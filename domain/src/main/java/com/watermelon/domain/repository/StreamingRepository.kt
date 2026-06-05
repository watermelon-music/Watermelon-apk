package com.watermelon.domain.repository

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
    fun addListener(callback: Callback)
    fun removeListener(callback: Callback)

    interface Callback {
        fun onPlaybackStateChanged(isBuffering: Boolean)
        fun onIsPlayingChanged(isPlaying: Boolean)
        fun onPositionDiscontinuity()
    }
}
