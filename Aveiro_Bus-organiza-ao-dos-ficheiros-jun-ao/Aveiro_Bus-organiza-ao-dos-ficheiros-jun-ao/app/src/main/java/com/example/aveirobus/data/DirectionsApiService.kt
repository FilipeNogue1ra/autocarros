package com.example.aveirobus.data

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URLEncoder

private const val GOOGLE_API_KEY = "AIzaSyDdDNIdV5OVmv6zcEIhEHmmiS-BEzajNcU"
private const val AVEIRO_LOCATION_CENTER_LAT = 40.64427
private const val AVEIRO_LOCATION_CENTER_LNG = -8.64554
private const val AUTOCOMPLETE_RADIUS_METERS = 10000

// Directions API implementation para transporte com acessibilidade
suspend fun fetchGoogleDirections(
    origin: String,
    destination: String,
    wheelchairAccessible: Boolean = false
): Result<List<Route>> {
    val apiKey = GOOGLE_API_KEY
    if (apiKey.isEmpty()) {
        Log.e("Directions", "API Key não configurada para Google Directions.")
        return Result.failure(IOException("Chave de API não configurada."))
    }

    val client = OkHttpClient()

    val encodedOrigin = URLEncoder.encode(origin, "UTF-8")
    val encodedDestination = URLEncoder.encode(destination, "UTF-8")

    // Parâmetros ajustados para garantir rotas de ônibus com acessibilidade
    var urlString = "https://maps.googleapis.com/maps/api/directions/json" +
            "?origin=$encodedOrigin" +
            "&destination=$encodedDestination" +
            "&mode=transit" +
            "&transit_mode=bus" +
            "&alternatives=true" +
            "&language=pt-PT"

    // Adicionar parâmetro de acessibilidade se necessário
    if (wheelchairAccessible) {
        urlString += "&wheelchair_accessible=true"  // Aqui você está modificando urlString
    }

    urlString += "&key=$apiKey"  // E aqui também

    Log.d("Directions", "Requesting URL: $urlString")
    val request = Request.Builder().url(urlString).build()

    return try {
        val responseBody = withContext(Dispatchers.IO) {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e("Directions", "API Error ${response.code}: ${response.message}")
                throw IOException("Erro na API Directions: ${response.code} - ${response.message}")
            }
            response.body?.string()
        }

        if (responseBody != null) {
            val gson = Gson()
            val apiResponse = gson.fromJson(responseBody, DirectionsApiResponse::class.java)

            if (apiResponse.status == "OK") {
                // Marcar rotas como acessíveis se a opção estiver ativada
                val routes = apiResponse.routes ?: emptyList()
                if (wheelchairAccessible) {
                    routes.forEach { route ->
                        route.wheelchairAccessible = true
                    }
                }
                Result.success(routes)
            } else {
                Log.e("Directions", "API Status not OK: ${apiResponse.status}, Error: ${apiResponse.errorMessage}")
                Result.failure(IOException("Erro da API Directions: ${apiResponse.status} - ${apiResponse.errorMessage ?: "Detalhes não disponíveis"}"))
            }
        } else {
            Log.e("Directions", "Response body is null")
            Result.failure(IOException("Resposta da API Directions vazia."))
        }
    } catch (e: Exception) {
        Log.e("Directions", "Exception during fetch or parse: ${e.message}", e)
        Result.failure(IOException("Falha ao buscar direções: ${e.message}", e))
    }
}

// Place Autocomplete implementation - Modificado para priorizar Aveiro
suspend fun fetchPlaceAutocompleteSuggestions(input: String): Result<List<PlacePrediction>> {
    val apiKey = GOOGLE_API_KEY
    if (apiKey.isEmpty()) {
        Log.e("Autocomplete", "API Key não configurada para Places Autocomplete.")
        return Result.failure(IOException("Chave de API não configurada."))
    }
    val client = OkHttpClient()

    val encodedInput = URLEncoder.encode(input, "UTF-8")
    val url = "https://maps.googleapis.com/maps/api/place/autocomplete/json" +
            "?input=$encodedInput" +
            "&language=pt-PT" +
            "&components=country:PT" +
            "&location=$AVEIRO_LOCATION_CENTER_LAT,$AVEIRO_LOCATION_CENTER_LNG" +
            "&radius=$AUTOCOMPLETE_RADIUS_METERS" +
            "&key=$apiKey"

    Log.d("Autocomplete", "Requesting URL: $url")
    val request = Request.Builder().url(url).build()

    return try {
        val responseBody = withContext(Dispatchers.IO) {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorBody = response.body?.string()
                Log.e("Autocomplete", "API Error ${response.code}: $errorBody")
                throw IOException("Erro na API Places Autocomplete: ${response.code} - ${response.message}")
            }
            response.body?.string()
        }

        if (responseBody != null) {
            Log.d("Autocomplete", "Response: $responseBody")

            val gson = Gson()
            val apiResponse = gson.fromJson(responseBody, PlacesAutocompleteResponse::class.java)

            if (apiResponse.status == "OK" || apiResponse.status == "ZERO_RESULTS") {
                Log.d("Autocomplete", "Encontradas ${apiResponse.predictions?.size ?: 0} sugestões")
                Result.success(apiResponse.predictions ?: emptyList())
            } else {
                Log.e(
                    "Autocomplete",
                    "API Status not OK: ${apiResponse.status}, Error: ${apiResponse.errorMessage}"
                )
                Result.failure(IOException("Erro da API Places Autocomplete: ${apiResponse.status} - ${apiResponse.errorMessage ?: "Detalhes não disponíveis"}"))
            }
        } else {
            Log.e("Autocomplete", "Response body is null")
            Result.failure(IOException("Resposta da API Places Autocomplete vazia."))
        }
    } catch (e: Exception) {
        Log.e("Autocomplete", "Exception during fetch or parse: ${e.message}", e)
        Result.failure(IOException("Falha ao buscar sugestões: ${e.message}", e))
    }
}