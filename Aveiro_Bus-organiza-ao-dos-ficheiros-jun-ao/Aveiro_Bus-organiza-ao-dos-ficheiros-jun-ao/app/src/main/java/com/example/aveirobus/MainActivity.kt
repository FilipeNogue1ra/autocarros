package com.example.aveirobus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.aveirobus.ui.theme.AveiroBusTheme // Import your theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // enableEdgeToEdge() // Consider if you need this when using Scaffold with default insets

        setContent {
            AveiroBusTheme { // Apply your app's theme
                var isLoggedIn by remember { mutableStateOf(false) } // State variable for login status
                val navController = rememberNavController() // Create a NavController

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        // Get the current back stack entry to determine the current route
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = navBackStackEntry?.destination?.route

                        // Determine if back navigation is possible
                        // Generally, you can navigate back if there's a previous entry
                        // and you are not on the starting destination of your graph.
                        val canNavigateBack = navController.previousBackStackEntry != null &&
                                             currentRoute != navController.graph.startDestinationRoute // Assuming graph.startDestinationRoute is your main screen route

                        MyTopAppBar(
                            isLoggedIn = isLoggedIn,
                            onUserButtonClick = {
                                if (isLoggedIn) {
                                    navController.navigate("userProfile") // Use the correct user profile route
                                } else {
                                    navController.navigate(TopNavItem.LoginScreen.route) // Use the correct login route
                                }
                            },
                            // Pass the necessary information and action to MyTopAppBar
                            currentRoute = currentRoute,
                            canNavigateBack = canNavigateBack,
                            navigateUp = { navController.popBackStack() } // This function triggers going back
                        )
                    },
                    bottomBar = { BottomNavigationBar(navController = navController) }
                ) { paddingValues -> // This lambda receives the padding values from Scaffold
                    println("Scaffold content called with padding: $paddingValues") // Log to see padding values
                    NavigationGraph(
                        navController = navController,
                        isLoggedIn = isLoggedIn,
                        onLoginSuccess = {
                            println("Login success")
                            isLoggedIn = true
                            // Clear back stack and navigate to a main screen after login
                            navController.popBackStack(navController.graph.startDestinationId, inclusive = false)
                            navController.navigate(BottomNavItem.Autocarros.route) // Navigate to a starting screen
                        },
                        onLoginFailure = {
                            println("Login failure")
                            // Consider showing an error message instead of just waiting and navigating back
                            // You might want to stay on the login screen and show an error message
                            // If you must navigate back after a delay (which is unusual UX):
                            // You'd need to use a coroutine for a delay in Compose, not Thread.sleep
                            // Coroutine example (requires adding lifecycle-runtime-compose dependency):
                            /*
                            CoroutineScope(Dispatchers.Main).launch {
                                delay(5000) // Delay for 5 seconds
                                navController.popBackStack()
                                navController.navigate(TopNavItem.LoginScreen.route)
                            }
                            */
                            // For now, removing the problematic Thread.sleep and immediate navigation
                            isLoggedIn = false // Ensure isLoggedIn is false on failure
                            // navController.popBackStack() // Consider if you want to pop back on failure
                            // navController.navigate(TopNavItem.LoginScreen.route) // Consider if you want to navigate back
                        },
                        onLogout = {
                            println("Logout")
                            isLoggedIn = false
                            // Clear back stack and navigate to a main screen after logout
                            navController.popBackStack(navController.graph.startDestinationId, inclusive = false)
                            navController.navigate(BottomNavItem.Autocarros.route) // Navigate to a starting screen
                        },
                        paddingValues = paddingValues // Pass the padding values to your NavigationGraph
                    )
                }
            }
        }
    }
}