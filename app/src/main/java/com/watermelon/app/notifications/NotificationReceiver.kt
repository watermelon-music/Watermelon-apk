package com.watermelon.app.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.watermelon.app.MainActivity
import com.watermelon.app.R
import com.watermelon.domain.repository.AuthRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.random.Random

@AndroidEntryPoint
class NotificationReceiver : BroadcastReceiver() {

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Timber.d("NotificationReceiver received action: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED || action == ACTION_INITIALIZE) {
            scheduleNextAlarm(context)
        } else if (action == ACTION_TRIGGER_ENGAGEMENT || action == "com.watermelon.app.TRIGGER_ENGAGEMENT_NOTIFICATION") {
            val pendingResult = goAsync()
            showRandomEngagementNotification(context, pendingResult)
            checkBroadcasts(context)
            scheduleNextAlarm(context)
        }
    }

    private fun showRandomEngagementNotification(context: Context, pendingResult: PendingResult? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val user = runCatching { authRepository.refreshUser() }.getOrNull()
                val name = user?.displayName ?: user?.username

                val channelId = "watermelon_engagement"
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val soundUri = Uri.parse("android.resource://${context.packageName}/${R.raw.watermelon_tone}")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val channelName = "Daily Recommendations"
                    val descriptionText = "Personalized trending songs and updates"
                    val importance = NotificationManager.IMPORTANCE_HIGH
                    val channel = NotificationChannel(channelId, channelName, importance).apply {
                        description = descriptionText
                        enableLights(true)
                        enableVibration(true)
                        val audioAttributes = AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .build()
                        setSound(soundUri, audioAttributes)
                    }
                    notificationManager.createNotificationChannel(channel)
                }

                val index = Random.nextInt(engagementTitles.size)
                val rawTitle = engagementTitles[index]
                val title = if (!name.isNullOrBlank()) {
                    String.format(rawTitle, name)
                } else {
                    rawTitle.replace("Hey %s, ", "").replaceFirstChar { it.uppercase() }
                }
                val message = engagementMessages[index]

                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    2,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val builder = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(android.R.drawable.ic_menu_search)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setSound(soundUri)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)

                notificationManager.notify(997, builder.build())
            } catch (e: Exception) {
                Timber.e(e, "Error showing engagement notification")
            } finally {
                pendingResult?.finish()
            }
        }
    }

    private fun checkBroadcasts(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val latest = authRepository.fetchLatestActiveBroadcast()
                if (latest != null) {
                    val prefs = context.getSharedPreferences("watermelon_prefs", Context.MODE_PRIVATE)
                    val lastBroadcastId = prefs.getLong("last_broadcast_id", -1)
                    if (latest.id > lastBroadcastId) {
                        showBroadcastNotification(context, latest.message, latest.sender)
                        prefs.edit().putLong("last_broadcast_id", latest.id).apply()
                    }
                }
            }.onFailure { Timber.e(it, "Failed to check broadcasts in background") }
        }
    }

    private fun showBroadcastNotification(context: Context, message: String, sender: String = "Watermelon") {
        val channelId = "watermelon_broadcasts_v2"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val soundUri = Uri.parse("android.resource://${context.packageName}/${R.raw.watermelon_tone}")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "App Announcements"
            val descriptionText = "Important alerts from the developer"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
                enableLights(true)
                enableVibration(true)
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()
                setSound(soundUri, audioAttributes)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("${sender.takeIf { it.isNotBlank() } ?: "Watermelon"} 🍉")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(soundUri)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(998, builder.build())
    }

    companion object {
        const val ACTION_TRIGGER_ENGAGEMENT = "com.watermelon.app.TRIGGER_ENGAGEMENT"
        const val ACTION_INITIALIZE = "com.watermelon.app.INITIALIZE_ALARM"

        private val engagementTitles = listOf(
            "Hey %s, hungry for music? 🍉",
            "Hey %s, trending songs near you! 🎧",
            "Hey %s, hot track counter is full! 🔥",
            "Hey %s, craving some beats? 🍕",
            "Hey %s, don't let your music get cold! 🍉",
            "Hey %s, late night cravings? 🌙",
            "Hey %s, special recipe from the chef: 🧑‍🍳",
            "Hey %s, soul food is served! 🍉"
        )

        private val engagementMessages = listOf(
            "Fresh trending songs are waiting at your doorstep! Tap to listen now.",
            "Discover the hottest tracks in your area right now.",
            "Listen to the new songs added to our trending list today.",
            "Delicious new tunes cooked just for you are ready to play!",
            "Discover today's top charting hits on Watermelon.",
            "We have curated the perfect playlist for your mood tonight.",
            "New tracks are trending in your area. Open the app to taste them!",
            "Play the latest hits on Watermelon and feed your soul."
        )

        fun scheduleNextAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = ACTION_TRIGGER_ENGAGEMENT
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                100,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val intervalsHours = listOf(6, 8, 12, 24)
            val randomHours = intervalsHours.random()
            val triggerMs = System.currentTimeMillis() + randomHours * 60 * 60 * 1000L

            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMs,
                pendingIntent
            )
            Timber.d("Scheduled next engagement alarm in $randomHours hours")
        }
    }
}
