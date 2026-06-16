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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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

                    val hideMiniPlayerRoutes = setOf(
                        Routes.PLAYER,
                        Routes.SPLASH,
                        Routes.LOGIN,
                        Routes.REGISTER,
                        Routes.FORGOT_PASSWORD,
                        Routes.ONBOARDING
                    )

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
                            Column {
                                AnimatedVisibility(
                                    visible = currentRoute !in hideMiniPlayerRoutes && playerState.currentTitle.isNotBlank(),
                                    enter = slideInVertically { it } + fadeIn(),
                                    exit = slideOutVertically { it } + fadeOut()
                                ) {
                                    MiniPlayer(
                                        modifier = Modifier,
                                        onClick = { navController.navigate(Routes.PLAYER) },
                                        viewModel = playerViewModel
                                    )
                                }
                                if (showBottomNav) {
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
