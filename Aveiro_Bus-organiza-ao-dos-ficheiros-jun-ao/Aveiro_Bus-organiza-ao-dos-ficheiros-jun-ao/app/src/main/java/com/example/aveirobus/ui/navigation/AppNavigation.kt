package com.example.aveirobus.ui.navigation

import android.util.Log
import androidx.compose.foundation.layout.PaddingValues // Import PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.aveirobus.ui.screens.AiChat
import com.example.aveirobus.ui.screens.Autocarros
import com.example.aveirobus.ui.screens.Avisos
import com.example.aveirobus.ui.screens.Carteira
import com.example.aveirobus.ui.screens.LoginScreen
import com.example.aveirobus.ui.screens.Opcoes
import com.example.aveirobus.ui.screens.UserProfileScreen
import com.example.aveirobus.ui.navigation.BottomNavItem
import com.example.aveirobus.ui.navigation.TopNavItem

@Composable
fun NavigationGraph(
    navController: NavHostController,
    isLoggedIn: Boolean,
    onLoginSuccess: () -> Unit,
    onLoginFailure: () -> Unit,
    onLogout: () -> Unit,
    paddingValues: PaddingValues // Added this parameter
) {
    NavHost(
        navController = navController,
        startDestination = BottomNavItem.Autocarros.route
        // No need to apply padding here if applying to individual screens
    ) {
        composable(BottomNavItem.Autocarros.route) {
            // Pass paddingValues to Autocarros if it uses Scaffold padding
            Autocarros()
        }
        composable(BottomNavItem.Avisos.route) {
            // Pass paddingValues to Avisos if it uses Scaffold padding
            Avisos(paddingValues = paddingValues)
        }
        composable(BottomNavItem.AiChat.route) {
            // Use the paddingValues parameter from NavigationGraph
            Log.d("NavigationGraph", "AiChat composable entered")
            AiChat(paddingValues = paddingValues) // Pass paddingValues here
        }
        composable(BottomNavItem.Carteira.route) {
            // Pass paddingValues to Carteira if it uses Scaffold padding
            Carteira()
        }
        composable(BottomNavItem.Opcoes.route) {
            // Pass paddingValues to Opcoes if it uses Scaffold padding
            Opcoes()
        }
        composable(TopNavItem.LoginScreen.route) {
            // Login screen likely doesn't need Scaffold padding if it's full screen
            LoginScreen(onLoginSuccess, onLoginFailure)
        }
        composable("userProfile") {
            // Pass paddingValues to UserProfileScreen if it uses Scaffold padding
            UserProfileScreen(onLogout)
        }
    }
}