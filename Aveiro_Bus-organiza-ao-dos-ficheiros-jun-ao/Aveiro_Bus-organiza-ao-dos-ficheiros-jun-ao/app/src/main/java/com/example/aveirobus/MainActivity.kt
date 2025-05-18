@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.aveirobus

import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.aveirobus.ui.theme.AveiroBusTheme
import com.example.aveirobus.ui.MyTopAppBar
import com.example.aveirobus.ui.navigation.BottomNavigationBar
import com.example.aveirobus.ui.navigation.NavigationGraph
import com.example.aveirobus.ui.navigation.BottomNavItem
import com.example.aveirobus.ui.navigation.TopNavItem
import com.example.aveirobus.ui.viewmodels.UserPreferencesViewModel
import com.example.aveirobus.ui.viewmodels.UserPreferencesViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Ativar Edge-to-Edge com a abordagem básica
        enableEdgeToEdge()

        super.onCreate(savedInstanceState)
        setContent {
            // Obter a instância do aplicativo
            val context = LocalContext.current
            val app = context.applicationContext as AveiroApplication

            // Inicializar o ViewModel de preferências do usuário
            val userPreferencesViewModel: UserPreferencesViewModel = viewModel(
                factory = UserPreferencesViewModelFactory(app.userPreferencesRepository)
            )

            // Observar as preferências do usuário
            val userPreferences by userPreferencesViewModel.userPreferencesFlow.collectAsState(initial = null)

            // Determinar o modo escuro
            val isDarkMode = userPreferences?.darkModeEnabled ?: isSystemInDarkTheme()

            // Configurar as barras do sistema baseado no tema usando WindowCompat
            DisposableEffect(isDarkMode) {
                val window = (context as ComponentActivity).window
                window.statusBarColor = AndroidColor.TRANSPARENT
                window.navigationBarColor = AndroidColor.TRANSPARENT

                // Configurar a aparência das barras do sistema
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.isAppearanceLightStatusBars = !isDarkMode
                insetsController.isAppearanceLightNavigationBars = !isDarkMode

                onDispose {}
            }

            AveiroBusTheme(darkTheme = isDarkMode) {
                var isLoggedIn by remember { mutableStateOf(false) }
                val navController = rememberNavController()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = navBackStackEntry?.destination?.route
                        val canNavigateBack = navController.previousBackStackEntry != null &&
                                currentRoute != BottomNavItem.Autocarros.route

                        MyTopAppBar(
                            isLoggedIn = isLoggedIn,
                            onUserButtonClick = {
                                if (isLoggedIn) {
                                    navController.navigate("userProfile") { launchSingleTop = true }
                                } else {
                                    navController.navigate(TopNavItem.LoginScreen.route) { launchSingleTop = true }
                                }
                            },
                            currentRoute = currentRoute,
                            canNavigateBack = canNavigateBack,
                            navigateUp = { navController.navigateUp() }
                        )
                    },
                    bottomBar = {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = navBackStackEntry?.destination?.route
                        val screensWithoutBottomBar = listOf(
                            TopNavItem.LoginScreen.route,
                            "userProfile",
                            "register"
                        )

                        if (currentRoute !in screensWithoutBottomBar) {
                            Surface(
                                color = Color.Transparent,
                            ) {
                                BottomNavigationBar(navController = navController)
                            }
                        }
                    },
                    containerColor = Color.Transparent
                ) { innerPadding ->
                    Log.d("MainActivity", "Scaffold innerPadding: $innerPadding")
                    NavigationGraph(
                        navController = navController,
                        isLoggedIn = isLoggedIn,
                        onLoginSuccess = {
                            isLoggedIn = true
                            navController.navigate(BottomNavItem.Autocarros.route) {
                                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onLoginFailure = { isLoggedIn = false },
                        onLogout = {
                            isLoggedIn = false
                            navController.navigate(BottomNavItem.Autocarros.route) {
                                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        paddingValues = innerPadding,
                        viewModel = userPreferencesViewModel
                    )
                }
            }
        }
    }
}