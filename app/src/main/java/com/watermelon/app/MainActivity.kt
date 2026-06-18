package com.watermelon.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import android.provider.Settings
import android.net.Uri
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
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
                    val activity = LocalContext.current as ComponentActivity
                    val playerViewModel = hiltViewModel<PlayerViewModel>(activity)

                    LaunchedEffect(pendingDeepLink) {
                        pendingDeepLink?.let { uri ->
                            handleDeepLink(navController, uri)
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
                                    val isSongActive = playerState.currentTitle.isNotBlank() || playerState.currentArtist.isNotBlank()
                                    AnimatedVisibility(
                                        visible = isSongActive && currentRoute != Routes.PLAYER,
                                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                                    ) {
                                        MiniPlayer(
                                            onClick = { navController.navigate(Routes.PLAYER) }
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
                            AlertDialog(
                                onDismissRequest = {
                                    if (!isDownloading) {
                                        updateInfo = null
                                    }
                                },
                                title = {
                                    Text("Update Available 🍉", style = MaterialTheme.typography.titleLarge)
                                },
                                text = {
                                    Column {
                                        Text("Version ${info.version} is now available. You are currently on version ${BuildConfig.VERSION_NAME}.", style = MaterialTheme.typography.bodyLarge)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        if (info.changelog.isNotBlank()) {
                                            Text("What's New:", style = MaterialTheme.typography.titleSmall)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(120.dp)
                                                    .background(MaterialTheme.colorScheme.surfaceVariant, shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                                                    .padding(8.dp)
                                            ) {
                                                androidx.compose.foundation.lazy.LazyColumn {
                                                    item {
                                                        Text(info.changelog, style = MaterialTheme.typography.bodyMedium)
                                                    }
                                                }
                                            }
                                        }
                                        if (isDownloading) {
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text("Downloading: ${downloadProgress ?: 0}%", style = MaterialTheme.typography.bodyMedium)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            LinearProgressIndicator(
                                                progress = { (downloadProgress ?: 0) / 100f },
                                                modifier = Modifier.fillMaxWidth(),
                                                color = Color(0xDC, 0x26, 0x26)
                                            )
                                        }
                                    }
                                },
                                confirmButton = {
                                    if (isDownloading) {
                                        Button(
                                            onClick = {
                                                downloadJob?.cancel()
                                                isDownloading = false
                                                downloadProgress = null
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                        ) {
                                            Text("Cancel")
                                        }
                                    } else {
                                        Button(
                                            onClick = { startDownloadFlow(info) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xDC, 0x26, 0x26))
                                        ) {
                                            Text("Update Now")
                                        }
                                    }
                                },
                                dismissButton = {
                                    if (!isDownloading) {
                                        TextButton(onClick = { updateInfo = null }) {
                                            Text("Later")
                                        }
                                    }
                                }
                            )
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
}

private fun handleDeepLink(
    navController: androidx.navigation.NavHostController,
    uri: android.net.Uri
) {
    when {
        uri.scheme == "watermelon" && uri.host == "playlist" -> {
            val id = uri.lastPathSegment ?: return
            navController.navigate("playlist_detail/$id")
        }
        uri.scheme == "https" && uri.host == "watermelon.app" && uri.path?.startsWith("/playlist/") == true -> {
            val id = uri.pathSegments.getOrNull(1) ?: return
            navController.navigate("playlist_detail/$id")
        }
    }
}
