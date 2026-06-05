package com.watermelon.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.watermelon.core.navigation.Routes

@Composable
fun WatermelonNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Routes.SPLASH
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Routes.SPLASH) {
            // TODO: SplashScreen
        }
        composable(Routes.ONBOARDING) {
            // TODO: OnboardingScreen
        }
        composable(Routes.LOGIN) {
            // TODO: LoginScreen
        }
        composable(Routes.REGISTER) {
            // TODO: RegisterScreen
        }
        composable(Routes.FORGOT_PASSWORD) {
            // TODO: ForgotPasswordScreen
        }
        composable(Routes.HOME) {
            // TODO: HomeScreen
        }
        composable(Routes.SEARCH) {
            // TODO: SearchScreen
        }
        composable(Routes.LIBRARY) {
            // TODO: LibraryScreen
        }
        composable(Routes.PLAYLIST_DETAIL) {
            // TODO: PlaylistDetailsScreen
        }
        composable(Routes.CREATE_PLAYLIST) {
            // TODO: CreatePlaylistScreen
        }
        composable(Routes.PLAYER) {
            // TODO: PlayerScreen
        }
        composable(Routes.QUEUE) {
            // TODO: QueueScreen
        }
        composable(Routes.DOWNLOADS) {
            // TODO: DownloadsScreen
        }
        composable(Routes.PROFILE) {
            // TODO: ProfileScreen
        }
        composable(Routes.SETTINGS) {
            // TODO: SettingsScreen
        }
        composable(Routes.ABOUT) {
            // TODO: AboutScreen
        }
    }
}
