package com.example.aveirobus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.aveirobus.ui.viewmodels.UserPreferencesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefinicoesScreen(
    navController: NavController,
    viewModel: UserPreferencesViewModel,
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val userPreferencesFlow = viewModel.userPreferencesFlow.collectAsState(initial = null)
    val preferences = userPreferencesFlow.value

    val isDarkMode = preferences?.darkModeEnabled ?: false
    val language = preferences?.language ?: "pt"
    val isWheelchairAccessible = preferences?.wheelchairAccessibilityEnabled ?: false

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
            // Cabeçalho com botão de voltar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Voltar",
                        tint = Color(0xFF6750A4)
                    )
                }

                // Círculo do perfil
                Box(
                    modifier = Modifier
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
            }

            // Modo Escuro
            SettingsToggleItem(
                title = "Modo Escuro",
                isChecked = isDarkMode,
                onCheckedChange = { viewModel.updateDarkMode(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Idioma (Dropdown)
            LanguageSelector(
                selectedLanguage = language,
                onLanguageSelected = { viewModel.updateLanguage(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Acessibilidade para Cadeira de Rodas
            SettingsToggleItem(
                title = "Acessibilidade",
                isChecked = isWheelchairAccessible,
                onCheckedChange = { viewModel.updateWheelchairAccessibility(it) }
            )
        }
    }
}

@Composable
fun SettingsToggleItem(
    title: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFEBE3F5),
            contentColor = Color.Black
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Switch(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF6750A4),
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color.Gray.copy(alpha = 0.5f)
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelector(
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val languages = listOf("Português", "English", "Español")
    val languageCodes = mapOf("Português" to "pt", "English" to "en", "Español" to "es")
    val selectedName = languageCodes.entries.find { it.value == selectedLanguage }?.key ?: "Português"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFEBE3F5),
            contentColor = Color.Black
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Idioma",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                Box(
                    modifier = Modifier
                        .menuAnchor()
                        .background(Color(0xFFD9D9D9), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .clickable { expanded = true }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = selectedName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Selecionar idioma"
                        )
                    }
                }

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    languages.forEach { language ->
                        DropdownMenuItem(
                            text = { Text(language) },
                            onClick = {
                                onLanguageSelected(languageCodes[language] ?: "pt")
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}