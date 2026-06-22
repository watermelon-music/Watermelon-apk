package com.watermelon.data.repository

import com.watermelon.domain.model.Playlist
import com.watermelon.domain.model.Song
import com.watermelon.domain.repository.MusicRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepositoryImpl @Inject constructor() : MusicRepository {

    private val mockSongs = listOf(
        Song(
            id = "1",
            title = "Blinding Lights",
            artistId = "1",
            artistName = "The Weeknd",
            albumId = null,
            albumName = null,
            durationMs = 200000L,
            coverUrl = "https://picsum.photos/seed/s1/300/300",
            audioUrl = null,
            genre = "Pop",
            releaseDate = "2020"
        ),
        Song(
            id = "2",
            title = "Levitating",
            artistId = "2",
            artistName = "Dua Lipa",
            albumId = null,
            albumName = null,
            durationMs = 203000L,
            coverUrl = "https://picsum.photos/seed/s2/300/300",
            audioUrl = null,
            genre = "Pop",
            releaseDate = "2020"
        ),
        Song(
            id = "3",
            title = "God's Plan",
            artistId = "3",
            artistName = "Drake",
            albumId = null,
            albumName = null,
            durationMs = 198000L,
            coverUrl = "https://picsum.photos/seed/s3/300/300",
            audioUrl = null,
            genre = "Hip-Hop",
            releaseDate = "2018"
        ),
        Song(
            id = "4",
            title = "Save Your Tears",
            artistId = "1",
            artistName = "The Weeknd",
            albumId = null,
            albumName = null,
            durationMs = 215000L,
            coverUrl = "https://picsum.photos/seed/s4/300/300",
            audioUrl = null,
            genre = "Pop",
            releaseDate = "2020"
        ),
        Song(
            id = "5",
            title = "Don't Start Now",
            artistId = "2",
            artistName = "Dua Lipa",
            albumId = null,
            albumName = null,
            durationMs = 183000L,
            coverUrl = "https://picsum.photos/seed/s5/300/300",
            audioUrl = null,
            genre = "Pop",
            releaseDate = "2019"
        ),
        Song(
            id = "6",
            title = "One Dance",
            artistId = "3",
            artistName = "Drake",
            albumId = null,
            albumName = null,
            durationMs = 174000L,
            coverUrl = "https://picsum.photos/seed/s6/300/300",
            audioUrl = null,
            genre = "Hip-Hop",
            releaseDate = "2016"
        )
    )

    private val mockPlaylists = listOf(
        Playlist(
            id = "1",
            name = "Chill Vibes",
            description = "Relax and unwind",
            coverUrl = "https://picsum.photos/seed/p1/300/300",
            ownerId = "system"
        ),
        Playlist(
            id = "2",
            name = "Workout Energy",
            description = "Pump it up",
            coverUrl = "https://picsum.photos/seed/p2/300/300",
            ownerId = "system"
        ),
        Playlist(
            id = "3",
            name = "Late Night Drive",
            description = "Midnight cruisers",
            coverUrl = "https://picsum.photos/seed/p3/300/300",
            ownerId = "system"
        ),
        Playlist(
            id = "4",
            name = "Top Hits 2024",
            description = "Best of the year",
            coverUrl = "https://picsum.photos/seed/p4/300/300",
            ownerId = "system"
        )
    )

    override fun getRecentlyPlayed(): Flow<List<Song>> = flowOf(mockSongs.take(4))
    override fun getFavorites(): Flow<List<Song>> = flowOf(mockSongs.take(3))
    override fun getTrendingMusic(): Flow<List<Song>> = flowOf(mockSongs.shuffled())
    override fun getRecommendedPlaylists(): Flow<List<Playlist>> = flowOf(mockPlaylists)
}
