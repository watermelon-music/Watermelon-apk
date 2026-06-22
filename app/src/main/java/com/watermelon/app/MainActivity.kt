package com.watermelon.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.watermelon.app.navigation.BottomNavBar
import com.watermelon.app.navigation.WatermelonNavHost
import com.watermelon.app.config.KillSwitchConfig
import com.watermelon.core.designsystem.theme.ThemeManager
import com.watermelon.core.designsystem.theme.WatermelonTheme
import com.watermelon.core.navigation.Routes
import com.watermelon.feature.player.MiniPlayer
import com.watermelon.feature.player.PlayerViewModel
import com.watermelon.app.screens.PremiumViewModel
import com.watermelon.app.screens.buildRazorpayOptions
import dagger.hilt.android.AndroidEntryPoint
import com.razorpay.Checkout
import timber.log.Timber

import android.os.Build
import android.Manifest
import androidx.activity.result.contract.ActivityResultContracts
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.media.RingtoneManager
import android.media.AudioAttributes
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import android.provider.Settings
import android.net.Uri
import kotlinx.coroutines.launch

import com.watermelon.app.notifications.NotificationReceiver

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var authRepository: com.watermelon.domain.repository.AuthRepository

    @Inject
    lateinit var cachedSongDao: com.watermelon.data.local.dao.CachedSongDao

    private val premiumViewModel: PremiumViewModel by viewModels()
    private var pendingDeepLink by mutableStateOf<android.net.Uri?>(null)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Timber.i("Notification permission granted")
        } else {
            Timber.w("Notification permission denied — media controls may not appear")
        }
    }

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Timber.i("Storage permission granted — downloads will save to Music/watermelon/")
        } else {
            Timber.w("Storage permission denied — downloads will fall back to app-private storage")
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen() // Splash screen API
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        Checkout.preload(applicationContext)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        requestStoragePermission()
        checkBroadcasts()
        checkRemoteConfig()

        // Schedule first alarm if not already scheduled
        val alarmIntent = Intent(this, NotificationReceiver::class.java).apply {
            action = NotificationReceiver.ACTION_TRIGGER_ENGAGEMENT
        }
        val alreadyScheduled = PendingIntent.getBroadcast(
            this,
            100,
            alarmIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) != null
        if (!alreadyScheduled) {
            NotificationReceiver.scheduleNextAlarm(this)
            Timber.d("No engagement alarm found; scheduled initial alarm")
        }

        val prefs = getSharedPreferences("watermelon_prefs", MODE_PRIVATE)
        val hasSeenOnboarding = prefs.getBoolean("has_seen_onboarding", false)
        val startDestination = if (hasSeenOnboarding) Routes.SPLASH else Routes.ONBOARDING
        pendingDeepLink = intent?.data

        setContent {
            val context = LocalContext.current
            val mode = remember { ThemeManager.get(context) }

            // Updater States
            var updateInfo by remember { mutableStateOf<com.watermelon.app.updater.AppUpdater.UpdateInfo?>(null) }
            var downloadProgress by remember { mutableStateOf<Int?>(null) }
            var isDownloading by remember { mutableStateOf(false) }
            var showPermissionDialog by remember { mutableStateOf(false) }
            val updaterScope = rememberCoroutineScope()
            var downloadJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

            LaunchedEffect(Unit) {
                val currentVersion = BuildConfig.VERSION_NAME
                val info = com.watermelon.app.updater.AppUpdater.checkForUpdate(currentVersion)
                if (info != null) {
                    updateInfo = info
                    val prefs = context.getSharedPreferences("watermelon_prefs", MODE_PRIVATE)
                    val lastNotifiedVersion = prefs.getString("last_notified_version", "")
                    if (lastNotifiedVersion != info.version) {
                        showUpdateNotification(context, info.version, info.changelog)
                        prefs.edit().putString("last_notified_version", info.version).apply()
                    }
                }
            }

            fun startDownloadFlow(info: com.watermelon.app.updater.AppUpdater.UpdateInfo) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (!context.packageManager.canRequestPackageInstalls()) {
                        showPermissionDialog = true
                        return
                    }
                }
                isDownloading = true
                downloadProgress = 0
                downloadJob = updaterScope.launch {
                    val apkFile = com.watermelon.app.updater.AppUpdater.downloadApk(context, info.downloadUrl) { progress ->
                        downloadProgress = progress
                    }
                    isDownloading = false
                    downloadProgress = null
                    if (apkFile != null) {
                        com.watermelon.app.updater.AppUpdater.installApk(context, apkFile)
                    } else {
                        android.widget.Toast.makeText(context, "Update download failed", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }

            WatermelonTheme(themeMode = mode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val activity = LocalContext.current as MainActivity
                    val playerViewModel = hiltViewModel<PlayerViewModel>(activity)

                    LaunchedEffect(pendingDeepLink) {
                        pendingDeepLink?.let { uri ->
                            handleDeepLink(navController, uri, activity.authRepository, this)
                            pendingDeepLink = null
                        }
                    }

                    val currentBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = currentBackStackEntry?.destination?.route

                    val playerState by playerViewModel.uiState.collectAsStateWithLifecycle()
                    LaunchedEffect(playerState.isPlaying) {
                        val intent = Intent(activity, PlaybackService::class.java)
                        if (playerState.isPlaying) {
                            try {
                                activity.startForegroundService(intent)
                            } catch (e: Exception) {
                                Timber.e(e, "Failed to start foreground service")
                            }
                        }
                    }

                    val showBottomNav = currentRoute in setOf(
                        Routes.HOME,
                        Routes.RADIO,
                        Routes.SEARCH,
                        Routes.LIBRARY,
                        Routes.PREMIUM
                    )

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            if (showBottomNav) {
                                Column(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // A real track is loaded only when the player has a song id —
                                    // checking title/artist alone keeps stale strings around and was
                                    // the source of the blank overlay above the bottom nav.
                                    val isSongActive = playerState.currentSongId.isNotBlank() ||
                                        (playerState.isRadioStream && playerState.currentTitle.isNotBlank())
                                    AnimatedVisibility(
                                        visible = isSongActive && currentRoute != Routes.PLAYER,
                                        enter = slideInVertically(initialOffsetY = { it }) + expandVertically() + fadeIn(),
                                        exit = slideOutVertically(targetOffsetY = { it }) + shrinkVertically() + fadeOut()
                                    ) {
                                        MiniPlayer(
                                            onClick = { navController.navigate(Routes.PLAYER) },
                                            viewModel = playerViewModel
                                        )
                                    }
                                    BottomNavBar(
                                        currentRoute = currentRoute,
                                        onNavigate = { route ->
                                            if (currentRoute != route) {
                                                navController.navigate(route) {
                                                    popUpTo(navController.graph.getStartDestination()) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    ) { padding ->
                        WatermelonNavHost(
                            navController = navController,
                            playerViewModel = playerViewModel,
                            modifier = Modifier.padding(padding),
                            startDestination = startDestination,
                            onOnboardingComplete = {
                                prefs.edit().putBoolean("has_seen_onboarding", true).apply()
                            },
                            onStartCheckout = { orderId, amount, label ->
                                startRazorpayCheckout(orderId, amount, label)
                            }
                        )

                        // Render updater dialogs
                        updateInfo?.let { info ->
                            androidx.compose.ui.window.Dialog(
                                onDismissRequest = {
                                    if (!isDownloading) updateInfo = null
                                }
                            ) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    elevation = CardDefaults.cardElevation(8.dp)
                                ) {
                                    Column {
                                        // Gradient header
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    Brush.horizontalGradient(
                                                        listOf(
                                                            Color(0xFFDC2626),
                                                            Color(0xFFEF4444),
                                                            Color(0xFFF87171)
                                                        )
                                                    )
                                                )
                                                .padding(24.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = "🍉",
                                                    fontSize = 40.sp
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = "New Update Available!",
                                                    style = MaterialTheme.typography.titleLarge.copy(
                                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Surface(
                                                    color = Color.White.copy(alpha = 0.2f),
                                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
                                                ) {
                                                    Text(
                                                        text = "${BuildConfig.VERSION_NAME} → ${info.version}",
                                                        style = MaterialTheme.typography.labelLarge.copy(
                                                            color = Color.White,
                                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                                                        ),
                                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                                    )
                                                }
                                            }
                                        }

                                        // Content
                                        Column(
                                            modifier = Modifier.padding(20.dp)
                                        ) {
                                            if (info.changelog.isNotBlank()) {
                                                Text(
                                                    text = "What's New",
                                                    style = MaterialTheme.typography.titleSmall.copy(
                                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                                    ),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .heightIn(max = 140.dp)
                                                        .background(
                                                            MaterialTheme.colorScheme.surfaceVariant,
                                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                                                        )
                                                        .padding(14.dp)
                                                ) {
                                                    androidx.compose.foundation.lazy.LazyColumn {
                                                        item {
                                                            Text(
                                                                text = info.changelog,
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(16.dp))
                                            }

                                            // Download progress
                                            if (isDownloading) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "Downloading...",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = "${downloadProgress ?: 0}%",
                                                        style = MaterialTheme.typography.titleMedium.copy(
                                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                                            color = Color(0xFFDC2626)
                                                        )
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                                LinearProgressIndicator(
                                                    progress = { (downloadProgress ?: 0) / 100f },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(8.dp)
                                                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp)),
                                                    color = Color(0xFFDC2626),
                                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                                )
                                                Spacer(modifier = Modifier.height(16.dp))
                                            }

                                            // Action buttons
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                if (isDownloading) {
                                                    OutlinedButton(
                                                        onClick = {
                                                            downloadJob?.cancel()
                                                            isDownloading = false
                                                            downloadProgress = null
                                                        },
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(48.dp),
                                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                                                        border = androidx.compose.foundation.BorderStroke(
                                                            1.dp, MaterialTheme.colorScheme.error
                                                        )
                                                    ) {
                                                        Text("Cancel", color = MaterialTheme.colorScheme.error)
                                                    }
                                                } else {
                                                    OutlinedButton(
                                                        onClick = { updateInfo = null },
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(48.dp),
                                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
                                                    ) {
                                                        Text("Later")
                                                    }
                                                    Button(
                                                        onClick = { startDownloadFlow(info) },
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(48.dp),
                                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = Color(0xFFDC2626)
                                                        )
                                                    ) {
                                                        Text("Update Now")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (showPermissionDialog) {
                            AlertDialog(
                                onDismissRequest = { showPermissionDialog = false },
                                title = { Text("Permission Required") },
                                text = { Text("To install the update directly, please enable the 'Install unknown apps' permission for Watermelon in system settings.") },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            showPermissionDialog = false
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                                    data = Uri.parse("package:${context.packageName}")
                                                }
                                                context.startActivity(intent)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xDC, 0x26, 0x26))
                                    ) {
                                        Text("Settings")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showPermissionDialog = false }) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    fun startRazorpayCheckout(orderId: String, amountPaise: Int, planLabel: String) {
        try {
            val options = buildRazorpayOptions(orderId, amountPaise, "user@watermelon.app", planLabel)
            val checkout = Checkout()
            checkout.open(this, options)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Checkout.RZP_REQUEST_CODE) {
            if (data != null) {
                val paymentId = data.getStringExtra("razorpay_payment_id")
                val orderId = data.getStringExtra("razorpay_order_id")
                val signature = data.getStringExtra("razorpay_signature")
                if (paymentId != null && orderId != null && signature != null) {
                    premiumViewModel.onPaymentSuccess(paymentId, orderId, signature)
                } else {
                    val errorCode = data.getStringExtra("error_code") ?: "0"
                    val errorMsg = data.getStringExtra("error_description") ?: "Payment error"
                    Timber.e("Razorpay error: $errorCode - $errorMsg")
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        pendingDeepLink = intent?.data
    }

    private fun checkRemoteConfig() {
        lifecycleScope.launch(Dispatchers.IO) {
            runCatching {
                val cfg = authRepository.checkRemoteConfig()
                if (cfg != null) {
                    withContext(Dispatchers.Main) {
                        KillSwitchConfig.update(
                            disableYouTube = cfg.disableYouTube,
                            disableAudius = cfg.disableAudius,
                            disableJamendo = cfg.disableJamendo,
                            freeMaxPlaylists = cfg.freeMaxPlaylists,
                            maintenanceMode = cfg.maintenanceMode
                        )
                    }
                }
            }.onFailure { Timber.e(it, "Failed to check remote config") }
        }
    }

    private fun checkBroadcasts() {
        lifecycleScope.launch(Dispatchers.IO) {
            runCatching {
                val latest = authRepository.fetchLatestActiveBroadcast()
                if (latest != null) {
                    val prefs = getSharedPreferences("watermelon_prefs", MODE_PRIVATE)
                    val lastBroadcastId = prefs.getLong("last_broadcast_id", -1)
                    if (latest.id > lastBroadcastId) {
                        if (latest.message.startsWith("[REFRESH]")) {
                            cachedSongDao.clearAll()
                            withContext(Dispatchers.Main) {
                                recreate()
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                showBroadcastNotification(applicationContext, latest.message, latest.sender)
                            }
                        }
                        prefs.edit().putLong("last_broadcast_id", latest.id).apply()
                    }
                }
            }.onFailure { Timber.e(it, "Failed to check broadcasts") }
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

        val builder = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("${sender.takeIf { it.isNotBlank() } ?: "Watermelon"} 🍉")
            .setContentText(message)
            .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setSound(soundUri)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(998, builder.build())
    }

    private fun showUpdateNotification(context: Context, version: String, changelog: String) {
        val channelId = "watermelon_updates_v2"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val soundUri = Uri.parse("android.resource://${context.packageName}/${R.raw.watermelon_tone}")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "App Updates"
            val descriptionText = "Notifications for new Watermelon app releases"
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
            0, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("New Version Available! 🍉")
            .setContentText("Version $version is available. Check out the new updates!")
            .setStyle(androidx.core.app.NotificationCompat.BigTextStyle()
                .bigText("Version $version is now available.\n\nUpdates:\n$changelog"))
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setSound(soundUri)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(999, builder.build())
    }
}



private fun handleDeepLink(
    navController: androidx.navigation.NavHostController,
    uri: android.net.Uri,
    authRepository: com.watermelon.domain.repository.AuthRepository,
    scope: kotlinx.coroutines.CoroutineScope
) {
    when {
        uri.scheme == "watermelon" && uri.host == "confirm" -> {
            scope.launch {
                val verified = runCatching { authRepository.isEmailVerified() }.getOrDefault(false)
                val target = if (verified) Routes.HOME else Routes.LOGIN
                navController.navigate(target) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
        uri.scheme == "watermelon" && uri.host == "playlist" -> {
            val id = uri.lastPathSegment ?: return
            navController.navigate("playlist_detail/$id")
        }
        (uri.scheme == "https" && (uri.host == "watermelon.app" || uri.host == "watermelon-api-oxx2.onrender.com") && uri.path?.startsWith("/playlist/") == true) -> {
            val id = uri.pathSegments.getOrNull(1) ?: return
            navController.navigate("playlist_detail/$id")
        }
    }
}
