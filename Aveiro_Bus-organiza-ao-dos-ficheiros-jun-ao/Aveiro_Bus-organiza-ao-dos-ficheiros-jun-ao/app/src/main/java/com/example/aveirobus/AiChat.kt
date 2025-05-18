package com.example.aveirobus

import android.util.Log // Import Log for logging
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers // Import Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext // Import withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import kotlin.compareTo
import kotlin.toString

data class Message(val text: String, val isUser: Boolean)

@Composable
fun AiChat(
    // Accept paddingValues if you are using this within a Scaffold
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    Log.d("AiChat", "AiChat composable entered") // Log when AiChat is entered

    var questionText by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<Message>() }
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) } // State to track loading
    var error: String? by remember { mutableStateOf(null) } // State to track errors

    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues) // Apply padding from Scaffold
            .padding(horizontal = 16.dp) // Add horizontal padding
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp), // Add vertical padding to list
            reverseLayout = true,
            verticalArrangement = Arrangement.spacedBy(8.dp) // Add spacing between messages
        ) {
            items(messages.reversed()) { message ->
                MessageBubble(message = message) // MessageBubble will also have logging
            }
        }

        // Show a loading indicator
        if (isLoading) {
            Log.d("AiChat", "Showing loading indicator") // Log when loading is true
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp) // Add horizontal padding to progress bar
            )
        }

        // Display error message if present
        error?.let { errorMessage ->
            Log.e("AiChat", "Displaying error: $errorMessage") // Log when error is displayed
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error, // Use theme error color
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp), // Add vertical padding to input row
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = questionText,
                onValueChange = {
                    questionText = it
                    Log.d("AiChat", "questionText updated: $questionText") // Log questionText changes
                    error = null // Clear error when user starts typing
                },
                label = { Text("Pergunta-me qualquer coisa...") },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp), // Add padding to the end of the TextField
                shape = RoundedCornerShape(24.dp), // Slightly rounder shape
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        Log.d("AiChat", "Keyboard Send action triggered") // Log keyboard action
                        if (questionText.isNotBlank() && !isLoading) {
                            // Trigger send action
                            val userMessage = questionText
                            messages.add(Message(userMessage, isUser = true))
                            Log.d("AiChat", "User message added to list: ${messages.last().text}") // Log user message added
                            questionText = ""
                            isLoading = true // Set loading to true
                            error = null // Clear any previous errors

                            coroutineScope.launch {
                                // Switch to IO dispatcher for the network call
                                val reply = withContext(Dispatchers.IO) {
                                    sendToGemini(userMessage) // Logging inside sendToGemini
                                }
                                // Switch back to Main dispatcher to update UI
                                withContext(Dispatchers.Main) {
                                    messages.add(Message(reply, isUser = false))
                                    Log.d("AiChat", "API reply added to list: ${messages.last().text}") // Log API reply added
                                    isLoading = false // Set loading to false
                                }
                            }
                            keyboardController?.hide()
                        } else {
                            Log.d("AiChat", "Keyboard Send: questionText is blank or isLoading")
                        }
                    }
                ),
                singleLine = true // Prevent multiline input for a chat box
            )
            Button(
                onClick = {
                    Log.d("AiChat", "Send button clicked") // Log button click
                    if (questionText.isNotBlank() && !isLoading) {
                        // Trigger send action
                        val userMessage = questionText
                        messages.add(Message(userMessage, isUser = true))
                        Log.d("AiChat", "User message added to list: ${messages.last().text}") // Log user message added
                        questionText = ""
                        isLoading = true // Set loading to true
                        error = null // Clear any previous errors

                        coroutineScope.launch {
                            // Switch to IO dispatcher for the network call
                            val reply = withContext(Dispatchers.IO) {
                                sendToGemini(userMessage) // Logging inside sendToGemini
                            }
                            // Switch back to Main dispatcher to update UI
                            withContext(Dispatchers.Main) {
                                messages.add(Message(reply, isUser = false))
                                Log.d("AiChat", "API reply added to list: ${messages.last().text}") // Log API reply added
                                isLoading = false // Set loading to false
                            }
                        }
                        keyboardController?.hide()
                    } else {
                        Log.d("AiChat", "Button Click: questionText is blank or isLoading")
                    }
                },
                enabled = !isLoading && questionText.isNotBlank() // Enable button only when not loading and text is not blank
            ) {
                Icon(imageVector = Icons.Default.Send, contentDescription = "Send")
            }
        }
    }
}

@Composable
fun MessageBubble(message: Message) {
    Log.d("MessageBubble", "MessageBubble called for: ${message.text}") // Log when MessageBubble is called

    val backgroundColor = if (message.isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart // Align messages

    Box(modifier = Modifier.fillMaxWidth()) { // Use Box to align messages
        Card(
            modifier = Modifier
                .align(alignment) // Apply alignment
                .padding(horizontal = 8.dp, vertical = 4.dp) // Add padding around the bubble
                .widthIn(max = 300.dp), // Limit bubble width
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            shape = RoundedCornerShape(12.dp) // Rounder corners for bubbles
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(10.dp), // Inner padding for text
                style = MaterialTheme.typography.bodyMedium // Use body medium style
            )
        }
    }
}

suspend fun sendToGemini(question: String): String {
    Log.d("GeminiAPI", "Calling Gemini API with question: $question")
    val client = OkHttpClient()

    // Construct the JSON body using JSONArray and JSONObject
    val partsArray = JSONArray().apply {
        put(JSONObject().apply {
            put("text", question)
        })
    }

    val contentsArray = JSONArray().apply {
        put(JSONObject().apply {
            put("parts", partsArray)
        })
    }

    val json = JSONObject().apply {
        put("contents", contentsArray)
    }

    val body = RequestBody.create(
        "application/json".toMediaTypeOrNull(),
        json.toString()
    )

    val apiKey = "AIzaSyA-peiYyjeY2xFJbzYmXhbEJTU25oMsoQU" // Substitui pela tua chave Gemini
    val request = Request.Builder()
        // CHANGED HERE: v1beta -> v1
        .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent?key=$apiKey")
        .post(body)
        .build()

    return try {
        // Using withContext to ensure network call is on a background thread
        // and to make the OkHttp call suspendable in a cleaner way.
        // Though OkHttp's execute() is blocking, and you're in a suspend fun,
        // explicitly using Dispatchers.IO is good practice.
        withContext(Dispatchers.IO) {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            Log.d("GeminiAPI", "Raw API Response Body: $responseBody") // Log the raw response

            if (!response.isSuccessful) {
                Log.e("GeminiAPI", "API Call not successful: ${response.code} - $responseBody")
                // Try to parse error if possible, otherwise return generic error
                responseBody?.let {
                    try {
                        val jsonError = JSONObject(it)
                        if (jsonError.has("error")) {
                            val errorObject = jsonError.getJSONObject("error")
                            val errorMessage = errorObject.optString("message", "Unknown API error from body")
                            return@withContext "API Error: $errorMessage"
                        }
                    } catch (e: JSONException) {
                        // Not a JSON error, or malformed
                        Log.e("GeminiAPI", "Error parsing API response: ${e.message}")
                    }
                }
                return@withContext "API Error: ${response.code} - ${response.message}"
            }

            val jsonResponse = JSONObject(responseBody)

            if (jsonResponse.has("candidates")) {
                val candidates = jsonResponse.getJSONArray("candidates")
                if (candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    if (firstCandidate.has("content")) {
                        val content = firstCandidate.getJSONObject("content")
                        val parts = content.getJSONArray("parts")
                        if (parts.length() > 0) {
                            val reply = parts.getJSONObject(0).getString("text").trim()
                            Log.d("GeminiAPI", "Parsed Reply: $reply")
                            reply
                        } else {
                            Log.w("GeminiAPI", "API response has candidates but no parts in the first candidate.")
                            "Received response without content."
                        }
                    } else {
                        // Handle cases where a candidate might not have content, e.g., safety reasons
                        val finishReason = firstCandidate.optString("finishReason", "UNKNOWN")
                        Log.w("GeminiAPI", "Candidate found but no content. Finish Reason: $finishReason")
                        if (finishReason == "SAFETY") {
                            "The response was blocked due to safety reasons."
                        } else {
                            "Candidate found but no content parts."
                        }
                    }
                } else {
                    Log.w("GeminiAPI", "API response has no candidates in the array.")
                    "Received an empty response from the API."
                }
            } else if (jsonResponse.has("error")) {
                val errorObject = jsonResponse.getJSONObject("error")
                val errorMessage = errorObject.optString("message", "Unknown API error")
                val errorCode = errorObject.optInt("code", -1)
                Log.e("GeminiAPI", "Gemini API returned error: Code $errorCode, Message: $errorMessage")
                "API Error: $errorMessage"
            } else {
                Log.e("GeminiAPI", "API response does not contain 'candidates' or 'error' key. Response: $responseBody")
                "Unexpected API response format."
            }
        }
    } catch (e: Exception) {
        Log.e("GeminiAPI", "Error during API call or parsing", e)
        "Erro ao contactar a API Gemini: ${e.message}"
    }
}