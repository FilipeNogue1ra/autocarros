@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.aveirobus // Certifique-se que o package name está correto

import android.graphics.Color as AndroidColor // Alias para evitar conflito com androidx.compose.ui.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle          // Importação para SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge       // Importação para enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme // Importação para isSystemInDarkTheme()
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.aveirobus.ui.theme.AveiroBusTheme
import com.example.aveirobus.ui.MyTopAppBar
import com.example.aveirobus.ui.navigation.BottomNavigationBar
import com.example.aveirobus.ui.navigation.NavigationGraph
import com.example.aveirobus.ui.navigation.BottomNavItem
import com.example.aveirobus.ui.navigation.TopNavItem

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // ATIVAR EDGE-TO-EDGE ANTES DE setContent
        // A chamada simplificada enableEdgeToEdge() usa os padrões para transparência
        // e ajusta automaticamente a cor dos ícones da barra de status/navegação.
        enableEdgeToEdge()

        super.onCreate(savedInstanceState)
        setContent {
            AveiroBusTheme {
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
                                // Para um efeito de "vidro fosco" ou semi-transparente, pode usar:
                                // color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.90f),
                                // Para totalmente transparente para ver o mapa por baixo:
                                color = Color.Transparent,
                            ) {
                                BottomNavigationBar(navController = navController)
                            }
                        }
                    },
                    containerColor = Color.Transparent // Container do Scaffold transparente
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
                        paddingValues = innerPadding
                    )
                }
            }
        }
    }
}
