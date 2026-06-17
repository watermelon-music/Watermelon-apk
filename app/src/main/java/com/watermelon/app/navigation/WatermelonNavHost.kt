package com.watermelon.app.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.watermelon.app.R
import com.watermelon.core.navigation.Routes
import com.watermelon.domain.model.RadioStation
import com.watermelon.domain.model.Song
import com.watermelon.feature.auth.AuthViewModel
import com.watermelon.feature.auth.EmailVerificationScreen
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
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.watermelon.feature.library.LibraryViewModel

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
        modifier = modifier,
        enterTransition = { fadeIn(tween(150)) },
        exitTransition = { fadeOut(tween(150)) },
        popEnterTransition = { fadeIn(tween(150)) },
        popExitTransition = { fadeOut(tween(150)) }
    ) {
        composable(Routes.SPLASH) {
            val authViewModel: AuthViewModel = hiltViewModel()
            val isAuthenticated by authViewModel.isAuthenticated.collectAsState()
            val hasNavigated = remember { mutableStateOf(false) }

            LaunchedEffect(isAuthenticated) {
                if (hasNavigated.value) return@LaunchedEffect
                if (isAuthenticated == null) {
                    delay(1500)
                    // Still null after wait — assume not authenticated
                    hasNavigated.value = true
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                    return@LaunchedEffect
                }
                delay(800) // minimum splash branding
                hasNavigated.value = true
                val auth = isAuthenticated
                if (auth == true) {
                    val verified = authViewModel.isEmailVerified()
                    val target = if (verified) Routes.HOME else Routes.VERIFY_EMAIL
                    navController.navigate(target) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                } else {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            }
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
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
            val authViewModel: AuthViewModel = hiltViewModel()
            val uiState by authViewModel.uiState.collectAsState()

            LaunchedEffect(uiState.needsEmailVerification) {
                if (uiState.needsEmailVerification) {
                    authViewModel.clearMessage()
                    val current = navController.currentDestination?.route
                    if (current != Routes.VERIFY_EMAIL) {
                        navController.navigate(Routes.VERIFY_EMAIL) {
                            current?.let { popUpTo(it) { inclusive = true } }
                        }
                    }
                }
            }

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
            val authViewModel: AuthViewModel = hiltViewModel()
            val uiState by authViewModel.uiState.collectAsState()

            LaunchedEffect(uiState.needsEmailVerification) {
                if (uiState.needsEmailVerification) {
                    authViewModel.clearMessage()
                    val current = navController.currentDestination?.route
                    if (current != Routes.VERIFY_EMAIL) {
                        navController.navigate(Routes.VERIFY_EMAIL) {
                            current?.let { popUpTo(it) { inclusive = true } }
                        }
                    }
                }
            }

            RegisterScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onAuthSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.VERIFY_EMAIL) {
            val authViewModel: AuthViewModel = hiltViewModel()
            EmailVerificationScreen(
                onVerified = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.VERIFY_EMAIL) { inclusive = true }
                    }
                },
                onBackToLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.VERIFY_EMAIL) { inclusive = true }
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
                onSongClick = { song: Song, songs: List<Song> ->
                    playerViewModel.playQueue(songs, songs.indexOf(song))
                    navController.navigate(Routes.PLAYER)
                },
                playerViewModel = playerViewModel,
                onPlayerClick = { navController.navigate(Routes.PLAYER) }
            )
        }
        composable(Routes.RADIO) {
            RadioScreen(
                onPlayStation = { station: RadioStation ->
                    playerViewModel.loadAndPlay(
                        station.url ?: "",
                        station.name ?: "Unknown Station",
                        station.country ?: "",
                        station.favicon ?: "",
                        isRadioStream = true
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
                onSongClick = { song: Song, index: Int, results: List<Song> ->
                    playerViewModel.playQueue(results, index)
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
                onSongClick = { song: Song, songs: List<Song> ->
                    playerViewModel.playQueue(songs, songs.indexOf(song).takeIf { it >= 0 } ?: 0)
                    navController.navigate(Routes.PLAYER)
                },
                onCreatePlaylist = { navController.navigate(Routes.CREATE_PLAYLIST) },
                onNavigateToPremium = { navController.navigate(Routes.PREMIUM) }
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
                onSongClick = { song: Song, songs: List<Song> ->
                    playerViewModel.playQueue(songs, songs.indexOf(song).takeIf { it >= 0 } ?: 0)
                    navController.navigate(Routes.PLAYER)
                },
                onPlayAllClick = { songs: List<Song> ->
                    playerViewModel.playQueue(songs, 0)
                    navController.navigate(Routes.PLAYER)
                },
                onShuffleClick = { songs: List<Song> ->
                    playerViewModel.playQueue(songs.shuffled(), 0)
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
                onNavigateToAbout = { navController.navigate(Routes.ABOUT) },
                onNavigateToPremium = { navController.navigate(Routes.PREMIUM) }
            )
        }
        composable(Routes.ABOUT) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.CREATE_PLAYLIST) {
            val libraryViewModel: LibraryViewModel = hiltViewModel(navController.getBackStackEntry(Routes.LIBRARY))
            CreatePlaylistScreen(
                onBack = { navController.popBackStack() },
                onCreate = { name ->
                    libraryViewModel.createPlaylist(name, null).getOrThrow()
                }
            )
        }
    }
}
