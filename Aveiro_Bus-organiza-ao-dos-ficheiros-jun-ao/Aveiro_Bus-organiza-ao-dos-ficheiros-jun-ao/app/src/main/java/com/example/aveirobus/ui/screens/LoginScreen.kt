package com.example.aveirobus.ui.screens

import android.content.Context
import android.util.Log // <<< IMPORTAÇÃO ADICIONADA para Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
// import androidx.compose.foundation.layout.size // Não está a ser usado diretamente aqui
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.MaterialTheme // Importar MaterialTheme para usar as cores do tema
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.IOException // <<< IMPORTAÇÃO ADICIONADA para IOException
import java.io.InputStreamReader
// As importações kotlin.text.split e kotlin.text.trim não são necessárias explicitamente
// pois são funções de extensão padrão para String.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onLoginFailure: () -> Unit
){
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        var username by remember { mutableStateOf("") }
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary, // Usar cor do tema
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        var password by remember { mutableStateOf("") }
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary, // Usar cor do tema
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        var errorMessage by remember { mutableStateOf<String?>(null) }

        Button(onClick = {
            if (compareUserPassword(context, username, password)) {
                onLoginSuccess()
                errorMessage = null
            } else {
                onLoginFailure()
                errorMessage = "Erro: Username ou password inválidos."
            }
        }) {
            Text("Login")
        }

        errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error // Usar cor de erro do tema
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            // TODO: Implementar lógica de navegação para o ecrã de registo
            // Exemplo: navController.navigate("register")
            Log.d("LoginScreen", "Botão Register clicado")
        }) {
            Text("Register")
        }
    }
}

fun compareUserPassword(context: Context, username: String, password: String, fileName: String = "logins.txt"): Boolean {
    var found = false // Flag para controlar se o utilizador foi encontrado
    try {
        context.assets.open(fileName).use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                for (line in reader.lineSequence()) { // Usar lineSequence para poder retornar da função externa
                    val cleanLine = line.trim()
                    if (cleanLine.isNotBlank()) {
                        val parts = cleanLine.split(",")
                        Log.d("LoginUtil", "Linha lida: '$cleanLine', Parts: $parts")
                        if (parts.size == 2) {
                            val fileUser = parts[0].trim()
                            val filePassword = parts[1].trim()
                            if (username == fileUser && password == filePassword) {
                                Log.d("LoginUtil", "Match encontrado para user: $username")
                                found = true
                                break // Sai do loop for assim que encontrar o utilizador
                            }
                        } else {
                            Log.w("LoginUtil", "Linha mal formatada no ficheiro logins: '$cleanLine'")
                        }
                    }
                }
            }
        }
    } catch (e: FileNotFoundException) {
        Log.e("LoginUtil", "Ficheiro de logins não encontrado: $fileName", e)
        return false // Retorna false se o ficheiro não for encontrado
    } catch (e: IOException) {
        Log.e("LoginUtil", "Erro ao ler o ficheiro de logins: $fileName", e)
        return false // Retorna false em caso de erro de leitura
    } catch (e: Exception) {
        Log.e("LoginUtil", "Erro inesperado ao comparar password: ${e.message}", e)
        return false // Retorna false para outras exceções
    }
    Log.d("LoginUtil", "Resultado da comparação para $username: $found")
    return found // Retorna o estado da flag
}
