package com.watermelon.data.repository

import com.watermelon.data.remote.youtube.NewPipeInitializer
import com.watermelon.domain.model.Playlist
import com.watermelon.domain.model.Song
import com.watermelon.domain.repository.MusicCatalogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicCatalogRepositoryImpl @Inject constructor(
    initializer: NewPipeInitializer
) : MusicCatalogRepository {

    private val youtube by lazy { org.schabi.newpipe.extractor.ServiceList.YouTube }

    private val mockPlaylists = listOf(
        Playlist("1","Chill Vibes","Relax and unwind","https://picsum.photos/seed/p1/300/300","system"),
        Playlist("2","Workout Energy","Pump it up","https://picsum.photos/seed/p2/300/300","system"),
        Playlist("3","Late Night Drive","Midnight cruisers","https://picsum.photos/seed/p3/300/300","system"),
        Playlist("4","Top Hits 2024","Best of the year","https://picsum.photos/seed/p4/300/300","system")
    )

    override fun getTrendingMusic(): Flow<List<Song>> = flow {
        val songs = withContext(Dispatchers.IO) {
            runCatching {
                val kioskList = youtube.getKioskList()
                val extractor = kioskList.getDefaultKioskExtractor()
                extractor.fetchPage()
                extractor.initialPage.items
                    .filterIsInstance<StreamInfoItem>()
                    .take(20)
                    .map { it.toSong() }
            }.getOrElse { getFallbackSongs() }
        }
        emit(songs)
    }

    override fun getRecommendedPlaylists(): Flow<List<Playlist>> = flowOf(mockPlaylists)

    override fun search(query: String): Flow<List<Song>> = flow {
        val songs = withContext(Dispatchers.IO) {
            if (query.isBlank()) {
                emptyList()
            } else {
                runCatching {
                    val extractor = youtube.getSearchExtractor(query)
                    extractor.fetchPage()
                    extractor.initialPage.items
                        .filterIsInstance<StreamInfoItem>()
                        .take(20)
                        .map { it.toSong() }
                }.getOrElse {
                    getFallbackSongs().filter { s ->
                        s.title.contains(query, ignoreCase = true) ||
                        s.artistName.contains(query, ignoreCase = true)
                    }
                }
            }
        }
        emit(songs)
    }

    private fun StreamInfoItem.toSong(): Song {
        val fullUrl = if (url.startsWith("http")) url else "https://www.youtube.com$url"
        val videoId = extractVideoId(fullUrl)
        return Song(
            id = videoId,
            title = name,
            artistId = uploaderName?.hashCode()?.toString() ?: videoId,
            artistName = uploaderName ?: "Unknown Artist",
            albumId = null,
            albumName = null,
            durationMs = if (duration > 0) (duration * 1000L) else 0L,
            coverUrl = thumbnailUrl,
            audioUrl = fullUrl,
            genre = "",
            releaseDate = ""
        )
    }

    private fun extractVideoId(url: String): String {
        return when {
            url.contains("v=") -> url.substringAfter("v=").substringBefore("&")
            url.contains("youtu.be/") -> url.substringAfter("youtu.be/")
            else -> url.hashCode().toString()
        }
    }

    private fun getFallbackSongs(): List<Song> = listOf(
        Song("dQw4w9WgXcQ","Never Gonna Give You Up","rick","Rick Astley",null,null,213000L,"https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg","https://www.youtube.com/watch?v=dQw4w9WgXcQ","Pop","1987"),
        Song("9bZkp7q19f0","Gangnam Style","psy","PSY",null,null,253000L,"https://i.ytimg.com/vi/9bZkp7q19f0/hqdefault.jpg","https://www.youtube.com/watch?v=9bZkp7q19f0","K-Pop","2012"),
        Song("kJQP7kiw5Fk","Despacito","luis","Luis Fonsi",null,null,229000L,"https://i.ytimg.com/vi/kJQP7kiw5Fk/hqdefault.jpg","https://www.youtube.com/watch?v=kJQP7kiw5Fk","Latin","2017"),
        Song("JGwWNGJdvx8","Shape of You","ed","Ed Sheeran",null,null,234000L,"https://i.ytimg.com/vi/JGwWNGJdvx8/hqdefault.jpg","https://www.youtube.com/watch?v=JGwWNGJdvx8","Pop","2017"),
        Song("RgKAFK5djSk","See You Again","wiz","Wiz Khalifa",null,null,230000L,"https://i.ytimg.com/vi/RgKAFK5djSk/hqdefault.jpg","https://www.youtube.com/watch?v=RgKAFK5djSk","Hip-Hop","2015"),
        Song("OPf0YbXqDm0","Uptown Funk","mark","Mark Ronson",null,null,270000L,"https://i.ytimg.com/vi/OPf0YbXqDm0/hqdefault.jpg","https://www.youtube.com/watch?v=OPf0YbXqDm0","Funk","2014"),
        Song("CevxZvSJLk8","Roar","katy","Katy Perry",null,null,231000L,"https://i.ytimg.com/vi/CevxZvSJLk8/hqdefault.jpg","https://www.youtube.com/watch?v=CevxZvSJLk8","Pop","2013"),
        Song("pRpeEdMmmQ0","Waka Waka","shakira","Shakira",null,null,220000L,"https://i.ytimg.com/vi/pRpeEdMmmQ0/hqdefault.jpg","https://www.youtube.com/watch?v=pRpeEdMmmQ0","Pop","2010")
    )
}
