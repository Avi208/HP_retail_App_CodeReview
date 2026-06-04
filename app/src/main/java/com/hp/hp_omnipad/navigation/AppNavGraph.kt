package com.hp.hp_omnipad.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.hp.hp_omnipad.ui.home.HomeScreen
import com.hp.hp_omnipad.ui.splash.SplashScreen
import com.hp.hp_omnipad.ui.theme.ThemeViewModel

@Composable
fun AppNavGraph(
    navController: NavHostController,
    themeViewModel: ThemeViewModel
) {
    NavHost(
        navController = navController,
        startDestination = "splash",
        // ADDED: Smooth Fade for the Splash -> Home transition
        enterTransition = { fadeIn(tween(500)) },
        exitTransition = { fadeOut(tween(500)) }
    ) {
        // Splash Screen
        composable("splash") {
            SplashScreen(
                onNavigateToHome = {
                    navController.navigate("home") {
                        popUpTo("splash") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // Home Screen
        composable("home") {
            HomeScreen(
                themeViewModel = themeViewModel
            )
        }
    }
}