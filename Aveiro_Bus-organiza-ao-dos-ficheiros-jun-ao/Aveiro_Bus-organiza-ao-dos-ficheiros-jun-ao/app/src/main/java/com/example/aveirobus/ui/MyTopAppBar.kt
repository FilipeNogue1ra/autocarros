@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.aveirobus.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTopAppBar(
    isLoggedIn: Boolean,
    onUserButtonClick: () -> Unit,
    currentRoute: String?,
    canNavigateBack: Boolean,
    navigateUp: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                "Aveiro Bus",
                style = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 20.sp
                )
            )
        },
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = navigateUp) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = onUserButtonClick) {
                Icon(
                    imageVector = if (isLoggedIn) Icons.Filled.AccountCircle else Icons.Filled.Person,
                    contentDescription = if (isLoggedIn) "Perfil do Utilizador" else "Login",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.Transparent, // FUNDO TRANSPARENTE
            scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f) // Opcional
        ),
        scrollBehavior = scrollBehavior
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF808080)
@Composable
fun MyTopAppBarPreviewTransparent() {
    MaterialTheme {
        MyTopAppBar(
            isLoggedIn = false,
            onUserButtonClick = {},
            currentRoute = "autocarros",
            canNavigateBack = false,
            navigateUp = {}
        )
    }
}
