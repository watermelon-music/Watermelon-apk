package com.watermelon.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.watermelon.app.navigation.BottomNavBar
import com.watermelon.app.navigation.WatermelonNavHost
import com.watermelon.core.designsystem.theme.WatermelonTheme
import com.watermelon.core.navigation.Routes
import com.watermelon.feature.player.MiniPlayer
import com.watermelon.feature.player.PlayerViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            WatermelonTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val activity = LocalContext.current as ComponentActivity
                    val playerViewModel = hiltViewModel<PlayerViewModel>(activity)

                    val currentBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = currentBackStackEntry?.destination?.route

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
                        Routes.SEARCH,
                        Routes.LIBRARY,
                        Routes.SETTINGS
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
                            modifier = Modifier.padding(padding)
                        )
                    }
                }
            }
        }
    }
}
