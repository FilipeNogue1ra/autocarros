package com.example.aveirobus.ui.screens

import android.util.Log
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
import androidx.compose.runtime.snapshots.SnapshotStateList // <<< IMPORTAÇÃO ADICIONADA
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
// Importar a data class Message do seu ficheiro de modelos de dados
import com.example.aveirobus.data.Message // <<< VERIFIQUE SE ESTA IMPORTAÇÃO ESTÁ CORRETA E SE Message ESTÁ DEFINIDA EM data/DirectionsModels.kt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import kotlin.Result

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChat(
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    Log.d("AiChatScreen", "AiChat composable entered")

    var questionText by remember { mutableStateOf("") }
    // Especificar explicitamente o tipo para mutableStateListOf pode ajudar
    // 'Message' deve ser resolvido pela importação.
    // 'SnapshotStateList' deve ser resolvido pela importação.
    val messages: SnapshotStateList<Message> = remember { mutableStateListOf<Message>() }
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            reverseLayout = true,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages, key = { message -> message.hashCode() }) { message: Message ->
                MessageBubble(message = message)
            }
        }

        if (isLoading) {
            Log.d("AiChatScreen", "Showing loading indicator")
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        errorText?.let { currentErrorMessage: String ->
            Log.e("AiChatScreen", "Displaying error: $currentErrorMessage")
            Text(
                text = currentErrorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = questionText,
                onValueChange = {
                    questionText = it
                    Log.d("AiChatScreen", "questionText updated: $questionText")
                    errorText = null
                },
                label = { Text("Pergunta-me qualquer coisa...") },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        Log.d("AiChatScreen", "Keyboard Send action triggered")
                        if (questionText.isNotBlank() && !isLoading) {
                            val userMessageText = questionText
                            val userMessage = Message(userMessageText, isUser = true) // Criação da Message
                            messages.add(0, userMessage) // Adiciona Message à lista
                            Log.d("AiChatScreen", "User message added: ${messages.firstOrNull()?.text}")
                            questionText = ""
                            isLoading = true
                            errorText = null

                            coroutineScope.launch {
                                val result: Result<String> = sendToGeminiApi(userMessageText)
                                withContext(Dispatchers.Main) {
                                    if (result.isSuccess) {
                                        val apiReply = result.getOrThrow()
                                        messages.add(0, Message(apiReply, isUser = false)) // Criação da Message
                                        Log.d("AiChatScreen", "API reply added: ${messages.firstOrNull()?.text}")
                                    } else {
                                        errorText = result.exceptionOrNull()?.message ?: "Erro desconhecido da API Gemini"
                                        Log.e("AiChatScreen", "API Error: $errorText")
                                    }
                                    isLoading = false
                                }
                            }
                            keyboardController?.hide()
                        } else {
                            Log.d("AiChatScreen", "Keyboard Send: questionText is blank or isLoading")
                        }
                    }
                ),
                singleLine = true
            )
            Button(
                onClick = {
                    Log.d("AiChatScreen", "Send button clicked")
                    if (questionText.isNotBlank() && !isLoading) {
                        val userMessageText = questionText
                        val userMessage = Message(userMessageText, isUser = true) // Criação da Message
                        messages.add(0, userMessage) // Adiciona Message à lista
                        Log.d("AiChatScreen", "User message added: ${messages.firstOrNull()?.text}")
                        questionText = ""
                        isLoading = true
                        errorText = null

                        coroutineScope.launch {
                            val result: Result<String> = sendToGeminiApi(userMessageText)
                            withContext(Dispatchers.Main) {
                                if (result.isSuccess) {
                                    val apiReply = result.getOrThrow()
                                    messages.add(0, Message(apiReply, isUser = false)) // Criação da Message
                                    Log.d("AiChatScreen", "API reply added: ${messages.firstOrNull()?.text}")
                                } else {
                                    errorText = result.exceptionOrNull()?.message ?: "Erro desconhecido da API Gemini"
                                    Log.e("AiChatScreen", "API Error: $errorText")
                                }
                                isLoading = false
                            }
                        }
                        keyboardController?.hide()
                    } else {
                        Log.d("AiChatScreen", "Button Click: questionText is blank or isLoading")
                    }
                },
                enabled = !isLoading && questionText.isNotBlank(),
                modifier = Modifier.height(IntrinsicSize.Max)
            ) {
                Icon(imageVector = Icons.Default.Send, contentDescription = "Send")
            }
        }
    }
}

@Composable
fun MessageBubble(message: Message) {
    Log.d("MessageBubble", "MessageBubble called for: ${message.text}")
    val backgroundColor = if (message.isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Card(
            modifier = Modifier
                .align(alignment)
                .widthIn(max = 300.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            shape = RoundedCornerShape(
                topStart = if (message.isUser) 12.dp else 4.dp,
                topEnd = if (message.isUser) 4.dp else 12.dp,
                bottomStart = 12.dp,
                bottomEnd = 12.dp
            )
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

suspend fun sendToGeminiApi(question: String): Result<String> {
    Log.d("GeminiAPI", "Chamando API Gemini com a pergunta: $question")
    val apiKey = "AIzaSyDdDNIdV5OVmv6zcEIhEHmmiS-BEzajNcU" // Chave API Fornecida

    if (apiKey.isEmpty()) {
        Log.e("GeminiAPI", "Chave da API Gemini não configurada.")
        return Result.failure(IOException("Erro: Chave da API Gemini não configurada."))
    }

    val client = OkHttpClient()
    val jsonMediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
    val partsArray = JSONArray().put(JSONObject().put("text", question))
    val contentsArray = JSONArray().put(JSONObject().put("parts", partsArray))
    val requestJsonBody = JSONObject().put("contents", contentsArray).toString()
    val body = RequestBody.create(jsonMediaType, requestJsonBody)

    val request = Request.Builder()
        .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent?key=$apiKey")
        .post(body)
        .build()

    return try {
        val responseString: String? = withContext(Dispatchers.IO) {
            val response = client.newCall(request).execute()
            val responseBodyStringLocal = response.body?.string()
            Log.d("GeminiAPI", "Resposta Bruta da API: $responseBodyStringLocal")

            if (!response.isSuccessful) {
                Log.e("GeminiAPI", "Chamada à API não foi bem-sucedida: ${response.code} - $responseBodyStringLocal")
                val errorMsg = responseBodyStringLocal?.let {
                    try { JSONObject(it).optJSONObject("error")?.optString("message", "Erro desconhecido da API.") }
                    catch (e: JSONException) { "Formato de erro inesperado." }
                } ?: "Erro ${response.code} - ${response.message}"
                throw IOException("Erro da API Gemini: $errorMsg")
            }
            responseBodyStringLocal
        }

        if (responseString == null) {
            Log.e("GeminiAPI", "Corpo da resposta vazio.")
            return Result.failure(IOException("Corpo da resposta vazio."))
        }

        val jsonResponse = JSONObject(responseString)
        val candidates = jsonResponse.optJSONArray("candidates")

        if (candidates != null && candidates.length() > 0) {
            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            if (parts != null && parts.length() > 0) {
                val reply = parts.getJSONObject(0).optString("text", "").trim()
                Log.d("GeminiAPI", "Resposta Analisada: $reply")
                if (reply.isNotEmpty()) Result.success(reply) else Result.success("Recebi uma resposta, mas sem texto.")
            } else {
                val finishReason = firstCandidate.optString("finishReason", "DESCONHECIDO")
                Log.w("GeminiAPI", "Candidato sem partes de conteúdo. Razão de finalização: $finishReason")
                val replyText = if (finishReason == "SAFETY") "A resposta foi bloqueada por razões de segurança."
                else "Resposta recebida sem conteúdo utilizável (Razão: $finishReason)."
                Result.success(replyText)
            }
        } else if (jsonResponse.has("error")) {
            val errorObject = jsonResponse.getJSONObject("error")
            val geminiErrorMessage = errorObject.optString("message", "Erro desconhecido da API Gemini")
            Log.e("GeminiAPI", "API Gemini retornou erro: $geminiErrorMessage")
            Result.failure(IOException("Erro da API Gemini: $geminiErrorMessage"))
        } else {
            Log.w("GeminiAPI", "Formato de resposta inesperado: $responseString")
            Result.failure(IOException("Formato de resposta inesperado da API."))
        }
    } catch (e: Exception) {
        Log.e("GeminiAPI", "Erro durante chamada à API ou parsing", e)
        Result.failure(IOException("Erro ao contactar a API Gemini: ${e.message}", e))
    }
}

@Preview(showBackground = true)
@Composable
fun AiChatScreenPreview() {
    MaterialTheme {
        AiChat()
    }
}
