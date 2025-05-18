package com.example.aveirobus

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.ui.graphics.vector.ImageVector // Import ImageVector

sealed class BottomNavItem(val route: String, val title: String, val icon: Any) {
    // Use R.drawable for your drawable resources
    object Autocarros : BottomNavItem("autocarros", "Autocarros", R.drawable.bus)
    object Avisos : BottomNavItem("avisos", "Avisos", Icons.Filled.Warning)
    object AiChat : BottomNavItem("aichat", "Ai Chat", R.drawable.message)
    object Carteira : BottomNavItem("carteira", "Carteira", R.drawable.wallet)
    object Opcoes : BottomNavItem("opcoes", "Opções", Icons.Filled.Menu)
}

sealed class TopNavItem(val route: String, val title: String, val icon: Any) {
    object LoginScreen : TopNavItem("login", "Login", Icons.Filled.AccountCircle)
}