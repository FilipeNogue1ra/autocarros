package com.example.aveirobus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun Opcoes(
    paddingValues: PaddingValues = PaddingValues(0.dp),
    navController: NavController
) {
    val backgroundColor = Color(0xFFF8F0FF)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(paddingValues)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Círculo do perfil no topo
            Box(
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 32.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEBE3F5))
                    .clickable { /* Ação para perfil */ },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Perfil",
                    tint = Color(0xFF6750A4),
                    modifier = Modifier.size(24.dp)
                )
            }

            // Botões de opções
            OpcaoButton("Definições") {
                navController.navigate("definicoes")
            }
            Spacer(modifier = Modifier.height(16.dp))

            OpcaoButton("Pontos de venda") {
                // Navegar para pontos de venda
            }
            Spacer(modifier = Modifier.height(16.dp))

            OpcaoButton("Reportar Problema") {
                // Navegar para reportar problema
            }
            Spacer(modifier = Modifier.height(16.dp))

            OpcaoButton("Contactos") {
                // Navegar para contactos
            }
            Spacer(modifier = Modifier.height(16.dp))

            OpcaoButton("Termos e Condições") {
                // Navegar para termos e condições
            }
        }
    }
}

@Composable
fun OpcaoButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFEBE3F5),
            contentColor = Color.Black
        ),
        contentPadding = PaddingValues(horizontal = 16.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 2.dp
        )
    ) {
        Text(
            text = text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}