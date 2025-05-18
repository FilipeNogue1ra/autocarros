package com.example.aveirobus.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.aveirobus.ui.screens.*
import com.example.aveirobus.ui.viewmodels.UserPreferencesViewModel

@Composable
fun NavigationGraph(
    navController: NavHostController,
    isLoggedIn: Boolean,
    onLoginSuccess: () -> Unit,
    onLoginFailure: () -> Unit,
    onLogout: () -> Unit,
    paddingValues: PaddingValues,
    viewModel: UserPreferencesViewModel
) {
    NavHost(
        navController = navController,
        startDestination = BottomNavItem.Autocarros.route
    ) {
        composable(BottomNavItem.Autocarros.route) {
            RouteSearchScreen(
                paddingValues = paddingValues,
                navController = navController,
                viewModel = viewModel
            )
        }
        composable(BottomNavItem.Avisos.route) {
            Avisos(paddingValues = paddingValues)
        }
        composable(BottomNavItem.AiChat.route) {
            AiChat(paddingValues = paddingValues)
        }
        composable(BottomNavItem.Carteira.route) {
            Carteira(paddingValues = paddingValues)
        }
        composable(BottomNavItem.Opcoes.route) {
            Opcoes(
                paddingValues = paddingValues,
                navController = navController
            )
        }

        // Ecrãs que NÃO usam o padding do Scaffold
        composable(TopNavItem.LoginScreen.route) {
            LoginScreen(onLoginSuccess, onLoginFailure)
        }
        composable("userProfile") {
            UserProfileScreen(onLogout)
        }
        composable("register") {
            RegisterScreen()
        }

        // Nova rota para a tela de Definições
        composable("definicoes") {
            DefinicoesScreen(
                navController = navController,
                viewModel = viewModel
            )
        }
    }
}