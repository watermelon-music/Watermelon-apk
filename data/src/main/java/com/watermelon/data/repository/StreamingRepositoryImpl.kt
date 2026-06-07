package com.watermelon.data.repository

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.watermelon.domain.repository.StreamingRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreamingRepositoryImpl @Inject constructor(
    private val player: ExoPlayer
) : StreamingRepository {

    private val listeners = mutableListOf<StreamingRepository.Callback>()
    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            val isBuffering = playbackState == Player.STATE_BUFFERING
            listeners.forEach { it.onPlaybackStateChanged(isBuffering) }
            if (playbackState == Player.STATE_READY) {
                val dur = player.duration
                if (dur > 0) {
                    listeners.forEach { it.onDurationChanged(dur) }
                }
            }
            if (playbackState == Player.STATE_ENDED) {
                listeners.forEach { it.onPlaybackCompleted() }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            listeners.forEach { it.onIsPlayingChanged(isPlaying) }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            listeners.forEach { it.onPositionDiscontinuity() }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            val detail = "${error.errorCodeName}: ${error.localizedMessage ?: "Playback error"}"
            listeners.forEach { it.onPlaybackError(detail) }
        }
    }

    init {
        player.addListener(listener)
    }

    override fun play(url: String, title: String, artist: String, artworkUrl: String) {
        val metadata = MediaMetadata.Builder()
            .setTitle(title.takeIf { it.isNotBlank() })
            .setArtist(artist.takeIf { it.isNotBlank() })
            .setArtworkUri(artworkUrl.takeIf { it.isNotBlank() }?.let { Uri.parse(it) })
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(url)
            .setMediaMetadata(metadata)
            .build()

        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    override fun pause() = player.pause()
    override fun resume() = player.play()
    override fun stop() = player.stop()
    override fun seekTo(positionMs: Long) = player.seekTo(positionMs)

    override fun setVolume(volume: Float) {
        player.volume = volume
    }

    override fun isPlaying(): Boolean = player.isPlaying
    override fun currentPosition(): Long = player.currentPosition
    override fun duration(): Long = if (player.duration > 0) player.duration else 0L

    override fun addListener(callback: StreamingRepository.Callback) {
        listeners.add(callback)
    }

    override fun removeListener(callback: StreamingRepository.Callback) {
        listeners.remove(callback)
    }
}
