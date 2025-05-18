package com.example.aveirobus.ui.screens

import androidx.compose.foundation.layout.Box // ADICIONADA ESTA IMPORTAÇÃO
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun RegisterScreen() {
    // Box é um layout Composable que permite empilhar elementos ou posicioná-los
    // de forma relativa dentro dos seus limites.
    Box(
        modifier = Modifier.fillMaxSize(), // O Modifier preenche todo o espaço disponível
        contentAlignment = Alignment.Center // Alinha o conteúdo (o Text) ao centro do Box
    ) {
        Text(text = "Rigistar em ficheiro separado")
    }
}
