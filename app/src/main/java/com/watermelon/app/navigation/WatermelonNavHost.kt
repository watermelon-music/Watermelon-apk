package com.watermelon.app.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.watermelon.app.R
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.watermelon.core.navigation.Routes
import com.watermelon.data.remote.radio.RadioStationDto
import com.watermelon.domain.model.Song
import com.watermelon.feature.auth.AuthViewModel
import com.watermelon.feature.auth.ForgotPasswordScreen
import com.watermelon.feature.auth.LoginScreen
import com.watermelon.feature.auth.RegisterScreen
import com.watermelon.feature.downloads.DownloadsScreen
import com.watermelon.feature.home.HomeScreen
import com.watermelon.feature.library.LibraryScreen
import com.watermelon.feature.player.PlayerScreen
import com.watermelon.feature.player.PlayerViewModel
import com.watermelon.feature.player.QueueScreen
import com.watermelon.feature.playlist.PlaylistDetailScreen
import com.watermelon.feature.search.SearchScreen
import com.watermelon.app.screens.AboutScreen
import com.watermelon.app.screens.CreatePlaylistScreen
import com.watermelon.app.screens.OnboardingScreen
import com.watermelon.app.screens.PremiumScreen
import com.watermelon.app.screens.ProfileScreen
import com.watermelon.app.screens.RadioScreen
import com.watermelon.feature.settings.SettingsScreen
import kotlinx.coroutines.delay

@Composable
fun WatermelonNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    playerViewModel: PlayerViewModel,
    startDestination: String = Routes.SPLASH,
    onOnboardingComplete: () -> Unit = {},
    onStartCheckout: (orderId: String, amountPaise: Int, planLabel: String) -> Unit = { _, _, _ -> }
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Routes.SPLASH) {
            val authViewModel: AuthViewModel = hiltViewModel()
            val isAuthenticated by authViewModel.isAuthenticated.collectAsState()

            LaunchedEffect(Unit) {
                delay(1500)
                val target = if (isAuthenticated) Routes.HOME else Routes.LOGIN
                navController.navigate(target) {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                }
            }
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.flash_logo),
                    contentDescription = "Watermelon",
                    modifier = Modifier.size(200.dp)
                )
            }
        }
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onGetStarted = {
                    onOnboardingComplete()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.LOGIN) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) },
                onNavigateToForgotPassword = { navController.navigate(Routes.FORGOT_PASSWORD) },
                onAuthSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onAuthSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(
                onNavigateToLogin = { navController.popBackStack() }
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                onSearchClick = { navController.navigate(Routes.SEARCH) },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                onSongClick = { song: Song ->
                    playerViewModel.playSong(song)
                    navController.navigate(Routes.PLAYER)
                }
            )
        }
        composable(Routes.RADIO) {
            RadioScreen(
                onPlayStation = { station: RadioStationDto ->
                    playerViewModel.loadAndPlay(
                        station.url ?: "",
                        station.name ?: "Unknown Station",
                        station.country ?: "",
                        station.favicon ?: ""
                    )
                }
            )
        }
        composable(Routes.PREMIUM) {
            PremiumScreen(
                onStartCheckout = onStartCheckout
            )
        }
        composable(Routes.SEARCH) {
            SearchScreen(
                onBackClick = { navController.popBackStack() },
                onSongClick = { song: Song ->
                    playerViewModel.playSong(song)
                    navController.navigate(Routes.PLAYER)
                }
            )
        }
        composable(Routes.LIBRARY) {
            LibraryScreen(
                onBackClick = { navController.popBackStack() },
                onPlaylistClick = { playlist ->
                    navController.navigate("playlist_detail/${playlist.id}")
                },
                onSongClick = { song: Song ->
                    playerViewModel.playSong(song)
                    navController.navigate(Routes.PLAYER)
                },
                onCreatePlaylist = { navController.navigate(Routes.CREATE_PLAYLIST) }
            )
        }
        composable(
            route = "playlist_detail/{playlistId}",
            arguments = listOf(navArgument("playlistId") { type = NavType.StringType })
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getString("playlistId") ?: ""
            PlaylistDetailScreen(
                playlistId = playlistId,
                onBackClick = { navController.popBackStack() },
                onSongClick = { song: Song ->
                    playerViewModel.playSong(song)
                    navController.navigate(Routes.PLAYER)
                }
            )
        }
        composable(Routes.PLAYER) {
            PlayerScreen(
                onBackClick = { navController.popBackStack() },
                onQueueClick = { navController.navigate(Routes.QUEUE) },
                viewModel = playerViewModel
            )
        }
        composable(Routes.QUEUE) {
            QueueScreen(
                onBackClick = { navController.popBackStack() },
                viewModel = playerViewModel
            )
        }
        composable(Routes.DOWNLOADS) {
            DownloadsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Routes.PROFILE) {
            ProfileScreen(
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onLogoutComplete = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
                onNavigateToProfile = { navController.navigate(Routes.PROFILE) },
                onNavigateToAbout = { navController.navigate(Routes.ABOUT) }
            )
        }
        composable(Routes.ABOUT) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.CREATE_PLAYLIST) {
            CreatePlaylistScreen(
                onBack = { navController.popBackStack() },
                onCreate = { name, desc ->
                    // TODO: persist playlist via ViewModel
                    navController.popBackStack()
                }
            )
        }
    }
}
