package com.watermelon.data.repository

import com.watermelon.data.local.dao.PlayHistoryDao
import com.watermelon.data.local.dao.UserActionDao
import com.watermelon.data.local.entity.PlayHistoryEntity
import com.watermelon.data.local.entity.UserActionEntity
import com.watermelon.domain.autoplay.RecommendationWeights
import com.watermelon.domain.autoplay.ScoredSong
import com.watermelon.domain.model.Song
import com.watermelon.domain.repository.MusicCatalogRepository
import com.watermelon.domain.repository.UserActionsRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.*
import org.junit.Test

class AutoplayRepositoryImplTest {

    // ------------------------------------------------------------------
    // Legacy weight defaults
    // ------------------------------------------------------------------

    @Test
    fun testWeights_haveDefaultValues() {
        val w = RecommendationWeights()
        assertThat(w.transitionFreq, `is`(30.0))
        assertEquals(20.0, w.likeSkipRatio, 0.001)
        assertEquals(15.0, w.skipPenalty, 0.001)
        assertEquals(0.5, w.recencyDecay, 0.001)
    }

    // ------------------------------------------------------------------
    // RecommendationEngine tests directly (AutoplayRepositoryImpl delegates to it)
    // ------------------------------------------------------------------

    private val catalogRepository: MusicCatalogRepository = mockk(relaxed = true)
    private val userActionsRepository: UserActionsRepository = mockk(relaxed = true)
    private val playHistoryDao: PlayHistoryDao = mockk(relaxed = true)
    private val userActionDao: UserActionDao = mockk(relaxed = true)

    private val engine = RecommendationEngineImpl(
        catalogRepository = catalogRepository,
        userActionsRepository = userActionsRepository,
        playHistoryDao = playHistoryDao,
        userActionDao = userActionDao
    )

    // ------------------------------------------------------------------
    // Title similarity tests
    // ------------------------------------------------------------------

    @Test
    fun `titleSimilarity identical titles returns 1_0`() {
        assertEquals(1.0, engine.titleSimilarity("happy nation", "happy nation"), 0.001)
    }

    @Test
    fun `titleSimilarity remix penalty - Happy Nation vs Happy Nation Remix should be above 0_7 and trigger penalty`() {
        val sim = engine.titleSimilarity("happy nation", "happy nation remix")
        assertTrue("Expected high similarity for remix variant: $sim", sim > 0.70)
    }

    @Test
    fun `titleSimilarity different songs should be low`() {
        val sim = engine.titleSimilarity("happy nation", "rhythm of the night")
        assertTrue("Expected low similarity: $sim", sim < 0.50)
    }

    @Test
    fun `titleSimilarity edited variants should be above 0_7`() {
        val sim = engine.titleSimilarity("around the world", "around the world radio edit")
        assertTrue("Expected high similarity for edit variant: $sim", sim > 0.70)
    }

    @Test
    fun `titleSimilarity slowed phonk variants should be above 0_7`() {
        val sim = engine.titleSimilarity("happy nation", "happy nation slowed + reverb phonk")
        assertTrue("Expected high similarity for slowed variant: $sim", sim > 0.70)
    }

    @Test
    fun `titleSimilarity completely different artist and title should be near 0`() {
        val sim = engine.titleSimilarity("happy nation", "mr vain")
        assertTrue("Expected near-zero similarity: $sim", sim < 0.30)
    }

    // ------------------------------------------------------------------
    // Diversity tests
    // ------------------------------------------------------------------

    @Test
    fun `applyDiversity limits same artist to 2`() {
        val songs = List(5) { index ->
            ScoredSong(
                song = Song(
                    id = "song_$index",
                    title = "Song $index",
                    artistId = "ace_of_base",
                    artistName = "Ace of Base",
                    albumId = "album1",
                    albumName = "The Sign",
                    durationMs = 300000,
                    coverUrl = null,
                    audioUrl = null,
                    genre = "Pop",
                    releaseDate = null
                ),
                score = 100.0 - index
            )
        }
        val result = engine.applyDiversity(songs, maxPerArtist = 2, maxPerAlbum = 5, targetCount = 5)
        val artistCounts = result.groupingBy { it.song.artistName }.eachCount()
        assertTrue("Max 2 per artist", (artistCounts["Ace of Base"] ?: 0) <= 2)
        assertEquals(2, result.size)
    }

    @Test
    fun `applyDiversity limits same album to 2`() {
        val songs = List(5) { index ->
            ScoredSong(
                song = Song(
                    id = "song_$index",
                    title = "Song $index",
                    artistId = "artist_$index",
                    artistName = "Artist $index",
                    albumId = "album_shared",
                    albumName = "Shared Album",
                    durationMs = 300000,
                    coverUrl = null,
                    audioUrl = null,
                    genre = "Pop",
                    releaseDate = null
                ),
                score = 100.0 - index
            )
        }
        val result = engine.applyDiversity(songs, maxPerArtist = 5, maxPerAlbum = 2, targetCount = 5)
        val albumCounts = result.groupingBy { it.song.albumName }.eachCount()
        assertTrue("Max 2 per album", (albumCounts["Shared Album"] ?: 0) <= 2)
        assertEquals(2, result.size)
    }

    @Test
    fun `applyDiversity respects targetCount when plenty of variety exists`() {
        val songs = List(20) { index ->
            ScoredSong(
                song = Song(
                    id = "song_$index",
                    title = "Song $index",
                    artistId = "artist_$index",
                    artistName = "Artist $index",
                    albumId = "album_$index",
                    albumName = "Album $index",
                    durationMs = 300000,
                    coverUrl = null,
                    audioUrl = null,
                    genre = "Pop",
                    releaseDate = null
                ),
                score = 100.0 - index
            )
        }
        val result = engine.applyDiversity(songs, maxPerArtist = 2, maxPerAlbum = 2, targetCount = 10)
        assertEquals(10, result.size)
    }

    // ------------------------------------------------------------------
    // Integration: generateQueue should reject title variants
    // ------------------------------------------------------------------

    @Test
    fun `generateQueue excludes title variants of current song`() = runTest {
        val current = Song(
            id = "happy_nation",
            title = "Happy Nation",
            artistId = "ace_of_base",
            artistName = "Ace of Base",
            albumId = null,
            albumName = null,
            durationMs = 300000,
            coverUrl = null,
            audioUrl = null,
            genre = "Pop",
            releaseDate = null
        )

        val artistSearchResults = listOf(
            current.copy(id = "happy_nation_remix", title = "Happy Nation Remix"),
            current.copy(id = "happy_nation_edit", title = "Happy Nation Radio Edit"),
            current.copy(id = "happy_nation_phonk", title = "Happy Nation Phonk"),
            current.copy(id = "rhythm_night", title = "Rhythm of the Night", artistName = "Corona", artistId = "corona"),
            current.copy(id = "what_is_love", title = "What Is Love", artistName = "Haddaway", artistId = "haddaway"),
            current.copy(id = "be_my_lover", title = "Be My Lover", artistName = "La Bouche", artistId = "la_bouche"),
            current.copy(id = "another_night", title = "Another Night", artistName = "Real McCoy", artistId = "real_mc_coy"),
            current.copy(id = "mr_vain", title = "Mr Vain", artistName = "Culture Beat", artistId = "culture_beat")
        )

        every { catalogRepository.search("Ace of Base") } returns flowOf(artistSearchResults)
        every { catalogRepository.getSongsByGenre("Pop") } returns flowOf(emptyList())
        every { catalogRepository.getTrendingMusic() } returns flowOf(emptyList())
        every { userActionsRepository.getFavorites() } returns flowOf(emptyList())
        every { playHistoryDao.getRecent() } returns flowOf(emptyList())
        every { userActionDao.getFavorites() } returns flowOf(emptyList())
        every { userActionDao.getSkips() } returns flowOf(emptyList())

        val queue = engine.generateQueue(current, excludeIds = emptySet(), count = 10)

        val ids = queue.map { it.id }
        assertFalse("Should NOT include remix", ids.contains("happy_nation_remix"))
        assertFalse("Should NOT include radio edit", ids.contains("happy_nation_edit"))
        assertFalse("Should NOT include phonk", ids.contains("happy_nation_phonk"))

        assertTrue("Should include Rhythm of the Night", ids.contains("rhythm_night"))
        assertTrue("Should include What Is Love", ids.contains("what_is_love"))
    }

    @Test
    fun `generateQueue enforces diversity in final output`() = runTest {
        val current = Song(
            id = "song_0",
            title = "Current Song",
            artistId = "artist_0",
            artistName = "Shared Artist",
            albumId = "album_0",
            albumName = "Shared Album",
            durationMs = 300000,
            coverUrl = null,
            audioUrl = null,
            genre = "Pop",
            releaseDate = null
        )

        val candidates = List(20) { index ->
            current.copy(
                id = "song_$index",
                title = "Song $index"
            )
        }

        every { catalogRepository.search(any()) } returns flowOf(candidates)
        every { catalogRepository.getSongsByGenre(any()) } returns flowOf(emptyList())
        every { catalogRepository.getTrendingMusic() } returns flowOf(emptyList())
        every { userActionsRepository.getFavorites() } returns flowOf(emptyList())
        every { playHistoryDao.getRecent() } returns flowOf(emptyList())
        every { userActionDao.getFavorites() } returns flowOf(emptyList())
        every { userActionDao.getSkips() } returns flowOf(emptyList())

        val queue = engine.generateQueue(current, excludeIds = emptySet(), count = 10)

        val artistCounts = queue.groupingBy { it.artistName }.eachCount()
        val albumCounts = queue.groupingBy { it.albumName }.eachCount()

        assertTrue("Max 2 songs from same artist", (artistCounts["Shared Artist"] ?: 0) <= 2)
        assertTrue("Max 2 songs from same album", (albumCounts["Shared Album"] ?: 0) <= 2)
    }

    @Test
    fun `generateQueue does not include excludeIds`() = runTest {
        val current = Song(
            id = "current",
            title = "Current",
            artistId = "a",
            artistName = "Artist A",
            albumId = null,
            albumName = null,
            durationMs = 300000,
            coverUrl = null,
            audioUrl = null,
            genre = "Pop",
            releaseDate = null
        )

        val candidates = listOf(
            current.copy(id = "song_1", title = "Song 1"),
            current.copy(id = "song_2", title = "Song 2"),
            current.copy(id = "song_3", title = "Song 3")
        )

        every { catalogRepository.search(any()) } returns flowOf(candidates)
        every { catalogRepository.getSongsByGenre(any()) } returns flowOf(emptyList())
        every { catalogRepository.getTrendingMusic() } returns flowOf(emptyList())
        every { userActionsRepository.getFavorites() } returns flowOf(emptyList())
        every { playHistoryDao.getRecent() } returns flowOf(emptyList())
        every { userActionDao.getFavorites() } returns flowOf(emptyList())
        every { userActionDao.getSkips() } returns flowOf(emptyList())

        val queue = engine.generateQueue(current, excludeIds = setOf("song_1", "song_2"), count = 10)
        val ids = queue.map { it.id }
        assertFalse(ids.contains("song_1"))
        assertFalse(ids.contains("song_2"))
        assertTrue(ids.contains("song_3"))
    }
}
