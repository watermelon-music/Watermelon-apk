package com.watermelon.domain.model

sealed class SearchResult {
    data class SongItem(val song: Song) : SearchResult()
    data class PlaylistItem(val playlist: CommunityPlaylist) : SearchResult()
    data class ArtistItem(val name: String, val thumbnail: String, val songCount: Int) : SearchResult()
    data class AlbumItem(val title: String, val artist: String, val cover: String, val songCount: Int) : SearchResult()
    data class VideoItem(val song: Song) : SearchResult()
}