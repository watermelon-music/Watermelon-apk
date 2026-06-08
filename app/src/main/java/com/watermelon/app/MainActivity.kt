package com.watermelon.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.watermelon.app.screens.buildRazorpayOptions
import dagger.hilt.android.AndroidEntryPoint
import com.razorpay.Checkout
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        Checkout.preload(applicationContext)
        setContent {
            val context = LocalContext.current
            val mode = remember { ThemeManager.get(context) }
            val isDark = when (mode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }
            WatermelonTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val activity = LocalContext.current as ComponentActivity
                    val playerViewModel = hiltViewModel<PlayerViewModel>(activity)

                    val currentBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = currentBackStackEntry?.destination?.route

                    val isPlaying by playerViewModel.uiState.collectAsState()
                    LaunchedEffect(isPlaying.isPlaying) {
                        val intent = Intent(activity, PlaybackService::class.java)
                        if (isPlaying.isPlaying) {
                            activity.startForegroundService(intent)
                        }
                        // Do NOT stop the service here — stopping it mid-transition
                        // kills playback between songs. Service stops in onTaskRemoved().
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
                                if (currentRoute !in hideMiniPlayerRoutes) {
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
                                                    popUpTo(Routes.HOME) {
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
                    // TODO: forward to PremiumViewModel
                } else {
                    val errorCode = data.getStringExtra("error_code") ?: "0"
                    val errorMsg = data.getStringExtra("error_description") ?: "Payment error"
                    Timber.e("Razorpay error: $errorCode - $errorMsg")
                    // TODO: forward error
                }
            }
        }
    }
}
