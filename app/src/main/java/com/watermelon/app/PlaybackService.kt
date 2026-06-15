package com.watermelon.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.ui.PlayerNotificationManager
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject lateinit var player: ExoPlayer
    private var mediaSession: MediaSession? = null
    private var notificationManager: PlayerNotificationManager? = null
    private var currentAccentColor: Int = 0
    private var sessionActivityPendingIntent: PendingIntent? = null
    private var colorListener: Player.Listener? = null
    private var playbackListener: Player.Listener? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()

        ensureNotificationChannel()

        sessionActivityPendingIntent = runCatching {
            TaskStackBuilder.create(this).run {
                addNextIntent(Intent(this@PlaybackService, MainActivity::class.java))
                getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            }
        }.getOrNull() ?: PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityPendingIntent ?: PendingIntent.getActivity(
                this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
            ))
            .build()

        setupCustomNotification()
        startColorExtractionListener()
        startPlaybackStateListener()
    }

    private fun setupCustomNotification() {
        val adapter = object : PlayerNotificationManager.MediaDescriptionAdapter {
            override fun getCurrentContentTitle(player: Player): CharSequence {
                return player.currentMediaItem?.mediaMetadata?.title ?: "Watermelon"
            }

            override fun createCurrentContentIntent(player: Player): PendingIntent? {
                return sessionActivityPendingIntent
            }

            override fun getCurrentContentText(player: Player): CharSequence {
                return player.currentMediaItem?.mediaMetadata?.artist ?: ""
            }

            override fun getCurrentLargeIcon(
                player: Player,
                callback: PlayerNotificationManager.BitmapCallback
            ): Bitmap? {
                val uri = player.currentMediaItem?.mediaMetadata?.artworkUri
                if (uri != null) {
                    loadBitmapAsync(uri) { bitmap ->
                        callback.onBitmap(bitmap)
                    }
                }
                return null
            }
        }

        val listener = object : PlayerNotificationManager.NotificationListener {
            override fun onNotificationPosted(
                notificationId: Int,
                notification: Notification,
                ongoing: Boolean
            ) {
                val styled = if (currentAccentColor != 0) {
                    NotificationCompat.Builder(this@PlaybackService, notification)
                        .setColor(currentAccentColor)
                        .setColorized(true)
                        .build()
                } else notification
                startForeground(notificationId, styled)
            }

            override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
                if (!player.isPlaying) {
                    stopSelf()
                }
            }
        }

        notificationManager = PlayerNotificationManager.Builder(
            this,
            NOTIFICATION_ID,
            PLAYBACK_CHANNEL_ID
        )
            .setMediaDescriptionAdapter(adapter)
            .setNotificationListener(listener)
            .setSmallIconResourceId(R.drawable.app_logo)
            .build()
            .apply {
                val token = mediaSession?.sessionCompatToken
                if (token != null) {
                    setMediaSessionToken(token)
                }
                setPlayer(player)
                setUseNextAction(true)
                setUsePreviousAction(true)
                setUsePlayPauseActions(true)
                setUseFastForwardAction(false)
                setUseRewindAction(false)
                setUseStopAction(false)
            }
    }

    private fun startColorExtractionListener() {
        currentAccentColor = 0xFFE53935.toInt()
        colorListener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                val uri = mediaItem?.mediaMetadata?.artworkUri
                if (uri != null) {
                    loadBitmapAsync(uri) { bitmap ->
                        extractColorAsync(bitmap)
                    }
                }
            }
        }
        colorListener?.let { player.addListener(it) }
    }

    private fun extractColorAsync(bitmap: Bitmap) {
        serviceScope.launch(Dispatchers.Default) {
            try {
                val palette = Palette.from(bitmap).generate()
                val color = palette.getVibrantColor(currentAccentColor)
                currentAccentColor = color
                // Force notification refresh on main thread
                launch(Dispatchers.Main) {
                    notificationManager?.invalidate()
                }
            } catch (e: Exception) {
                Timber.e(e, "Palette extraction failed")
            }
        }
    }

    private fun loadBitmapAsync(uri: android.net.Uri, onLoaded: (Bitmap) -> Unit) {
        val imageLoader = ImageLoader.Builder(this).build()
        val request = ImageRequest.Builder(this)
            .data(uri)
            .size(256, 256)
            .allowHardware(false)
            .target { result ->
                try {
                    val bitmap = result.toBitmap()
                    onLoaded(bitmap)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to load artwork bitmap")
                }
            }
            .build()
        imageLoader.enqueue(request)
    }



    private fun android.graphics.drawable.Drawable.toBitmap(): Bitmap {
        if (this is android.graphics.drawable.BitmapDrawable) return this.bitmap
        val bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)
        return bitmap
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!player.isPlaying) {
            player.stop()
            player.clearMediaItems()
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        playbackListener?.let { runCatching { player.removeListener(it) } }
        playbackListener = null
        colorListener?.let { runCatching { player.removeListener(it) } }
        colorListener = null
        serviceScope.cancel()
        notificationManager?.setPlayer(null)
        runCatching {
            player.stop()
            player.clearMediaItems()
        }
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                PLAYBACK_CHANNEL_ID,
                "Media playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setSound(null, null)
                enableLights(false)
                enableVibration(false)
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun startPlaybackStateListener() {
        playbackListener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (!isPlaying && player.mediaItemCount == 0) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_IDLE && player.mediaItemCount == 0) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
        playbackListener?.let { player.addListener(it) }
    }

    companion object {
        private const val PLAYBACK_CHANNEL_ID = "watermelon_playback_channel"
        private const val NOTIFICATION_ID = 1
    }
}
