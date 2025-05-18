package com.example.aveirobus.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding // Importar Modifier.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp // Para o PaddingValues padrão (opcional)

@Composable
fun Carteira(paddingValues: PaddingValues = PaddingValues(0.dp)) { // Aceita paddingValues, com um valor padrão
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues), // Aplicar o padding recebido do Scaffold
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Conteúdo do Ecrã Carteira")
    }
}
