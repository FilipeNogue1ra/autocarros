package com.example.aveirobus.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
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
import java.io.InputStreamReader
import kotlin.text.split
import kotlin.text.trim

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onLoginFailure: () -> Unit
){
    val context = LocalContext.current
    // Use a single Column to manage the layout of all elements
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center // Center the content vertically
    ) {
        // Username field
        var username by remember { mutableStateOf("") }
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true, // Ensure the text field is a single line
            shape = RoundedCornerShape(18.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color.Blue,
                unfocusedBorderColor = Color.Gray,
                cursorColor = Color.Blue,
                focusedLabelColor = Color.Blue,
                unfocusedLabelColor = Color.Gray
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password field
        var password by remember { mutableStateOf("") }
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(), // Hide password characters
            singleLine = true, // Ensure the text field is a single line
            shape = RoundedCornerShape(18.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color.Blue,
                unfocusedBorderColor = Color.Gray,
                cursorColor = Color.Blue,
                focusedLabelColor = Color.Blue,
                unfocusedLabelColor = Color.Gray
            )
        )

        Spacer(modifier = Modifier.height(24.dp)) // More space before the button

        // Error message state
        var errorMessage by remember { mutableStateOf<String?>(null) }

        // Login Button
        Button(onClick = {
            if (compareUserPassword(context, username, password)) {
                onLoginSuccess() // Call the lambda to navigate
                errorMessage = null // Clear any previous error message
            } else {
                onLoginFailure() // Call the new lambda
                errorMessage = "Error: Invalid username or password."
            }
        }) {
            Text("Login")
        }

        // Display error message if any
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage!!,
                color = Color.Red
            )
        }

        Spacer(modifier = Modifier.height(16.dp)) // More space before the button

        // Register Button
        Button(onClick = {
            // Handle registration logic here
        }) {
            Text("Register")
            }
    }
}

fun compareUserPassword(context: Context, username: String, password: String, fileName: String = "logins.txt"): Boolean {
    try {
        val inputStream = context.assets.open(fileName)
        val reader = BufferedReader(InputStreamReader(inputStream))
        var found = false
        reader.forEachLine { line ->
            val cleanLine = line.trim()
            val parts = cleanLine.split(",")
            println("Parts: $parts")
            if (parts.size == 2) {
                val fileUser = parts[0].trim()
                val filePassword = parts[1].trim()

                if (username == fileUser && password == filePassword) {
                    found = true
                    return@forEachLine // This is a labeled return, exiting only the lambda
                }
            }
        }
        reader.close()
        return found
    } catch (e: FileNotFoundException) {
        println("Error: ${e.message}")
        return false
    } catch (e: Exception) {
        println("An unexpected error occurred: ${e.message}")
        return false
    }
}