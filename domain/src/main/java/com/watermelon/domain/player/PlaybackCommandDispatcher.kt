package com.watermelon.domain.player

class PlaybackCommandDispatcher {
    @Volatile var onNext: (() -> Unit)? = null
    @Volatile var onPrevious: (() -> Unit)? = null
    @Volatile var onQueueStateChanged: (() -> Unit)? = null

    @Volatile
    var hasNext: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                onQueueStateChanged?.invoke()
            }
        }

    @Volatile
    var hasPrevious: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                onQueueStateChanged?.invoke()
            }
        }
}