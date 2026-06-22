package com.watermelon.app

import androidx.media3.common.Player
import androidx.media3.common.ForwardingPlayer
import com.watermelon.domain.player.PlaybackCommandDispatcher

class QueueAwareForwardingPlayer(
    player: Player,
    private val dispatcher: PlaybackCommandDispatcher
) : ForwardingPlayer(player) {

    override fun seekToNext() {
        dispatcher.onNext?.invoke() ?: super.seekToNext()
    }

    override fun seekToNextMediaItem() {
        dispatcher.onNext?.invoke() ?: super.seekToNextMediaItem()
    }

    override fun seekToPrevious() {
        dispatcher.onPrevious?.invoke() ?: super.seekToPrevious()
    }

    override fun seekToPreviousMediaItem() {
        dispatcher.onPrevious?.invoke() ?: super.seekToPreviousMediaItem()
    }

    override fun getAvailableCommands(): Player.Commands {
        val base = super.getAvailableCommands()
        val builder = base.buildUpon()
        if (dispatcher.hasNext) {
            builder.add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
            builder.add(Player.COMMAND_SEEK_TO_NEXT)
        }
        if (dispatcher.hasPrevious) {
            builder.add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
            builder.add(Player.COMMAND_SEEK_TO_PREVIOUS)
        }
        return builder.build()
    }

    override fun hasNextMediaItem(): Boolean = dispatcher.hasNext

    override fun hasPreviousMediaItem(): Boolean = dispatcher.hasPrevious

    override fun isCommandAvailable(command: Int): Boolean {
        return when (command) {
            Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
            Player.COMMAND_SEEK_TO_NEXT -> dispatcher.hasNext
            Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
            Player.COMMAND_SEEK_TO_PREVIOUS -> dispatcher.hasPrevious
            else -> super.isCommandAvailable(command)
        }
    }
}