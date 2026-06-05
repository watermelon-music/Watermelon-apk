package com.watermelon.data.repository

import com.watermelon.domain.model.Playlist
import com.watermelon.domain.model.Song
import com.watermelon.domain.repository.MusicCatalogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Placeholder for an external online music API.
 * Replace with Retrofit calls to your own music server or licensed API.
 */
@Singleton
class MusicCatalogRepositoryImpl @Inject constructor() : MusicCatalogRepository {

    private val mockSongs = listOf(
        Song("1","Blinding Lights","1","The Weeknd",null,null,200000L,"https://picsum.photos/seed/s1/300/300","https://www.nasa.gov/wp-content/uploads/2015/01/590320main_ringtone_apollo11_countdown.mp3","Pop","2020"),
        Song("2","Levitating","2","Dua Lipa",null,null,203000L,"https://picsum.photos/seed/s2/300/300","https://archive.org/download/testmp3testfile/mpthreetest.mp3","Pop","2020"),
        Song("3","God's Plan","3","Drake",null,null,198000L,"https://picsum.photos/seed/s3/300/300","https://www.nasa.gov/wp-content/uploads/2015/01/590320main_ringtone_apollo11_countdown.mp3","Hip-Hop","2018"),
        Song("4","Save Your Tears","1","The Weeknd",null,null,215000L,"https://picsum.photos/seed/s4/300/300","https://archive.org/download/testmp3testfile/mpthreetest.mp3","Pop","2020"),
        Song("5","Don't Start Now","2","Dua Lipa",null,null,183000L,"https://picsum.photos/seed/s5/300/300","https://www.nasa.gov/wp-content/uploads/2015/01/590320main_ringtone_apollo11_countdown.mp3","Pop","2019"),
        Song("6","One Dance","3","Drake",null,null,174000L,"https://picsum.photos/seed/s6/300/300","https://archive.org/download/testmp3testfile/mpthreetest.mp3","Hip-Hop","2016")
    )

    private val mockPlaylists = listOf(
        Playlist("1","Chill Vibes","Relax and unwind","https://picsum.photos/seed/p1/300/300","system"),
        Playlist("2","Workout Energy","Pump it up","https://picsum.photos/seed/p2/300/300","system"),
        Playlist("3","Late Night Drive","Midnight cruisers","https://picsum.photos/seed/p3/300/300","system"),
        Playlist("4","Top Hits 2024","Best of the year","https://picsum.photos/seed/p4/300/300","system")
    )

    override fun getTrendingMusic(): Flow<List<Song>> = flowOf(mockSongs.shuffled())
    override fun getRecommendedPlaylists(): Flow<List<Playlist>> = flowOf(mockPlaylists)
}
