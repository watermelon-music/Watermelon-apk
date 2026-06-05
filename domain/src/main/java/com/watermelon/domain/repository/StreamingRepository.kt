package com.watermelon.domain.repository

import androidx.media3.common.Player

interface StreamingRepository {
    fun play(url: String)
    fun pause()
    fun resume()
    fun stop()
    fun seekTo(positionMs: Long)
    fun setVolume(volume: Float)
    fun isPlaying(): Boolean
    fun currentPosition(): Long
    fun duration(): Long
    fun addListener(listener: Player.Listener)
    fun removeListener(listener: Player.Listener)
}
