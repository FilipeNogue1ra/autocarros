package com.example.aveirobus.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
// import androidx.compose.foundation.layout.padding // Não é usado diretamente aqui se removido do NavHost
// import androidx.compose.ui.Modifier // Não é usado diretamente aqui se removido do NavHost
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.aveirobus.ui.screens.*

@Composable
fun NavigationGraph(
    navController: NavHostController,
    isLoggedIn: Boolean,
    onLoginSuccess: () -> Unit,
    onLoginFailure: () -> Unit,
    onLogout: () -> Unit,
    paddingValues: PaddingValues // Receber paddingValues do Scaffold
) {
    NavHost(
        navController = navController,
        startDestination = BottomNavItem.Autocarros.route
        // REMOVIDO: modifier = Modifier.padding(paddingValues)
        // O padding será aplicado individualmente a cada ecrã que o necessite.
    ) {
        composable(BottomNavItem.Autocarros.route) {
            // RouteSearchScreen precisa do paddingValues para o layout edge-to-edge do mapa
            // e para posicionar a sua UI sobreposta corretamente.
            RouteSearchScreen(paddingValues = paddingValues)
        }
        composable(BottomNavItem.Avisos.route) {
            // Avisos também recebe paddingValues para aplicar ao seu layout raiz.
            Avisos(paddingValues = paddingValues)
        }
        composable(BottomNavItem.AiChat.route) {
            // AiChat também recebe paddingValues.
            AiChat(paddingValues = paddingValues)
        }
        composable(BottomNavItem.Carteira.route) {
            // Se Carteira deve respeitar o padding do Scaffold, ela também deve aceitar paddingValues.
            // Se for de ecrã inteiro, não precisa. Assumindo que precisa:
            Carteira(paddingValues = paddingValues)
        }
        composable(BottomNavItem.Opcoes.route) {
            // Se Opcoes deve respeitar o padding do Scaffold, ela também deve aceitar paddingValues.
            // Assumindo que precisa:
            Opcoes(paddingValues = paddingValues)
        }

        // Ecrãs que NÃO usam o padding do Scaffold (são de ecrã inteiro)
        composable(TopNavItem.LoginScreen.route) {
            LoginScreen(onLoginSuccess, onLoginFailure) // Não passa paddingValues
        }
        composable("userProfile") {
            UserProfileScreen(onLogout) // Não passa paddingValues
        }
        composable("register") {
            RegisterScreen() // Não passa paddingValues
        }
    }
}
