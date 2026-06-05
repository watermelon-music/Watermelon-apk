package com.watermelon.data.repository

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.watermelon.domain.repository.StreamingRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreamingRepositoryImpl @Inject constructor(
    private val player: ExoPlayer
) : StreamingRepository {

    override fun play(url: String) {
        val mediaItem = MediaItem.fromUri(url)
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

    override fun addListener(listener: Player.Listener) {
        player.addListener(listener)
    }

    override fun removeListener(listener: Player.Listener) {
        player.removeListener(listener)
    }
}
