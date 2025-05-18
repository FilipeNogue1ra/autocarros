package com.example.aveirobus.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text // Using Material 2 Text for preview compatibility if needed
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
// import com.example.aveirobus.data.DirectionsApiResponse // Import the data classes - Already imported via Route
import com.example.aveirobus.data.DirectionsApiResponse
import com.example.aveirobus.data.Route // Import Route
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds // Import LatLngBounds
import com.google.maps.android.compose.Marker // Import Marker
import com.google.maps.android.compose.rememberMarkerState // Import rememberMarkerState
import com.google.maps.android.compose.* // Import Google Maps Compose
import com.google.gson.Gson // Import Gson
import kotlinx.coroutines.launch
import okhttp3.*
import java.io.IOException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteSearchScreen() {
    var originText by remember { mutableStateOf("") }
    var destinationText by remember { mutableStateOf("") }
    var routes by remember { mutableStateOf<List<Route>>(emptyList()) }
    var selectedRoute by remember { mutableStateOf<Route?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(0.0, 0.0), 10f) // Initial camera position (can be changed later)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        OutlinedTextField(
            value = originText,
            onValueChange = { originText = it },
            label = { Text("Origem") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = destinationText,
            onValueChange = { destinationText = it },
            label = { Text("Destino") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                coroutineScope.launch {
                    routes = fetchRoutes(originText, destinationText)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Buscar Rotas")
            }
        if (routes.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Rotas encontradas:")
            LazyColumn(modifier = Modifier.weight(1f)) { // Make the list take available space
                items(routes) { route ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                selectedRoute = route
                                route.overviewPolyline?.points?.let { polylinePoints ->
                                    val latLngBounds = LatLngBounds.builder()
                                    decodePolyline(polylinePoints).forEach { latLng ->
                                        latLngBounds.include(latLng)
                                    }
                                    coroutineScope.launch {
                                        cameraPositionState.animate(
                                            update = CameraUpdateFactory.newLatLngBounds(latLngBounds.build(), 100), durationMs = 1000)
                                    }
                                }
                            },
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Duração: ${route.legs.firstOrNull()?.duration?.text ?: "N/A"}")
                            Text("Distância: ${route.legs.firstOrNull()?.distance?.text ?: "N/A"}")
                            // You might want to add more details here, like route summary
                            route.summary?.let {
                                Text("Sumário: $it", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }

        // Display the map
        selectedRoute?.let { route ->
            GoogleMap(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp), // Adjust height as needed
                cameraPositionState = cameraPositionState
            ) {
                // Draw the polyline for the selected route
                // You might need to decode the polyline string to LatLng coordinates
                route.overviewPolyline?.points?.let { polylinePoints ->
                     Polyline(points = decodePolyline(polylinePoints)) // Assuming you have a decodePolyline function
                }
                route.legs.firstOrNull()?.startLocation?.let { startLocation ->
                    Marker(
                        state = rememberMarkerState(position = LatLng(startLocation.lat, startLocation.lng)),
                        title = "Origem"
                    )
                }

                route.legs.lastOrNull()?.endLocation?.let { endLocation ->
                    Marker(
                        state = rememberMarkerState(position = LatLng(endLocation.lat, endLocation.lng)),
                        title = "Destino"
                    )
                }

                // Add markers for bus stops
                route.legs.forEach { leg ->
                    leg.steps.forEach { step ->
                        if (step.travelMode == "TRANSIT") {
                            step.transitDetails?.departureStop?.location?.let { departureLocation ->
                                Marker(
                                    state = rememberMarkerState(position = LatLng(departureLocation.lat, departureLocation.lng)),
                                    title = "Embarque: ${step.transitDetails.departureStop.name ?: ""}"
                                )
                            }
                            step.transitDetails?.arrivalStop?.location?.let { arrivalLocation ->
                                Marker(
                                    state = rememberMarkerState(position = LatLng(arrivalLocation.lat, arrivalLocation.lng)),
                                    title = "Desembarque: ${step.transitDetails.arrivalStop.name ?: ""}"
                                )
                            }
                        }
                    }
                }
            }

        // Display the steps of the selected route
        selectedRoute?.let { route ->
            Spacer(modifier = Modifier.height(16.dp))
            Text("Passos da rota:")
            LazyColumn(modifier = Modifier.weight(1f)) { // Give steps list some weight
                route.legs.forEach { leg ->
                    items(leg.steps) { step ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                // Display step instructions (HTML text needs to be handled)
                                Text(step.htmlInstructions?.replace(Regex("<.*?>"), "") ?: "Instrução desconhecida") // Remove HTML tags
                                step.duration?.text?.let {
                                    Text("Duração: $it", style = MaterialTheme.typography.bodySmall)
                                }
                                step.distance?.text?.let {
                                    Text("Distância: $it", style = MaterialTheme.typography.bodySmall)
                                }
                                // Display transit details if available
                                if (step.travelMode == "TRANSIT") {
                                    step.transitDetails?.let { transit ->
                                        Text("Linha: ${transit.line?.name ?: "N/A"}", style = MaterialTheme.typography.bodySmall)
                                        Text("De: ${transit.departureStop?.name ?: "N/A"}", style = MaterialTheme.typography.bodySmall)
                                        Text("Para: ${transit.arrivalStop?.name ?: "N/A"}", style = MaterialTheme.typography.bodySmall)
                                        Text("Paragens: ${transit.numStops}", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    }
}

// Dummy decodePolyline function - Replace with actual implementation
fun decodePolyline(encoded: String): List<LatLng> {
    val poly = ArrayList<LatLng>()
    var index = 0
    val len = encoded.length
    var lat = 0
    var lng = 0

    while (index < len) {
        var b: Int
        var shift = 0
        var result = 0
        do {
            b = encoded[index++].code - 63
            result = result or (b and 0x1f shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlat = if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)
        lat += dlat

        shift = 0
        result = 0
        do {
            b = encoded[index++].code - 63
            result = result or (b and 0x1f shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlng = if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)
        lng += dlng

        val p = LatLng(lat.toDouble() / 1E5, lng.toDouble() / 1E5)
        poly.add(p)
    }
    return poly
}


suspend fun fetchRoutes(origin: String, destination: String): List<Route> {
    val apiKey = "YOUR_API_KEY" // Replace with how you access your API key
    val client = OkHttpClient()
    val url = "https://maps.googleapis.com/maps/api/directions/json?origin=$origin&destination=$destination&key=$apiKey"

    val request = Request.Builder()
        .url(url)
        .build()

    return try {
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: return emptyList()
        val gson = Gson()
        val apiResponse = gson.fromJson(responseBody, DirectionsApiResponse::class.java)
        apiResponse.routes ?: emptyList()
    } catch (e: IOException) {
        e.printStackTrace()
        emptyList()
    }
}