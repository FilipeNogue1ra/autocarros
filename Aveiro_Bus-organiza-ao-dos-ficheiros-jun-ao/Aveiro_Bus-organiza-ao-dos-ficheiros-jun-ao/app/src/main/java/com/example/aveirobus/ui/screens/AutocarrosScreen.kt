package com.example.aveirobus.ui.screens


import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search // Import the Search icon
import androidx.compose.material.icons.filled.LocationOn // Import MyLocation icon
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch // Import for coroutineScope.launch
import androidx.compose.material3.FabPosition // Import FabPosition


data class LinhaAutocarro(
    val nome: String,
    val cor: Color,
    val percurso: List<com.google.android.gms.maps.model.LatLng>,
    val paragens: List<Paragem>
)

data class Paragem(
    val nome: String,
    val localizacao: com.google.android.gms.maps.model.LatLng,
    val horarios: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Autocarros() {
    val context = LocalContext.current
    val fusedLocationClient: FusedLocationProviderClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    var lastKnownLocation by remember { mutableStateOf<Location?>(null) }
    var locationPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            locationPermissionGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (locationPermissionGranted) {
                getLastKnownLocation(fusedLocationClient) { location ->
                    lastKnownLocation = location
                }
            }
        }
    )

    LaunchedEffect(locationPermissionGranted) {
        if (locationPermissionGranted) {
            getLastKnownLocation(fusedLocationClient) { location ->
                lastKnownLocation = location
            }
        } else {
            // Correctly launching the permission request with the array of permissions
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }


    val cameraPositionState = rememberCameraPositionState {
        position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(com.google.android.gms.maps.model.LatLng(40.6405, -8.6538), 13f) // Aveiro
    }

    val linhas = remember {
        listOf(
            LinhaAutocarro(
                nome = "Linha Azul",
                cor = Color.Blue,
                percurso = listOf(
                    com.google.android.gms.maps.model.LatLng(40.6405, -8.6538),
                    com.google.android.gms.maps.model.LatLng(40.6420, -8.6500)
                ),
                paragens = listOf(
                    Paragem("Paragem 1",
                        com.google.android.gms.maps.model.LatLng(40.6405, -8.6538), listOf("08:00", "09:00")),
                    Paragem("Paragem 2",
                        com.google.android.gms.maps.model.LatLng(40.6420, -8.6500), listOf("08:10", "09:10"))
                )
            )
            // Add more lines here
        )
    }

    var paragemSelecionada by remember { mutableStateOf<Paragem?>(null) }
    var selectedMarkerScreenPosition by remember { mutableStateOf<IntOffset?>(null) }

    var boxSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }
    var boxPosition by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    val density = LocalDensity.current
    // Calculated outside the onClick lambda
    val yOffsetPx = with(density) { 50.dp.toPx() }

    val coroutineScope = rememberCoroutineScope() // Get a CoroutineScope

    // State for the start and destination input fields
    var startLocation by remember { mutableStateOf("") }
    var destinationLocation by remember { mutableStateOf("") }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    lastKnownLocation?.let { location ->
                        coroutineScope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(location.latitude, location.longitude), 17f
                                )
                            )
                        }
                    }
                },
                modifier = Modifier.offset(y = (-70).dp) // desloca o botão 60dp para cima
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = "Ir para a minha localização")
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        // Add padding to accommodate a potential bottom bar or navigation
        content = { paddingValues -> // This provides padding based on scaffold structure
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // Apply padding from Scaffold to prevent content being under potential bottom bar
                    .padding(paddingValues)
                    .onGloballyPositioned { coordinates ->
                        boxSize = coordinates.size.toSize()
                        boxPosition = coordinates.positionInRoot()
                    }
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(isMyLocationEnabled = locationPermissionGranted),
                    uiSettings = MapUiSettings(myLocationButtonEnabled = false, zoomControlsEnabled = false),
                    onMapClick = {
                        paragemSelecionada = null
                        selectedMarkerScreenPosition = null
                    }
                ) {
                    linhas.forEach { linha ->
                        Polyline(
                            points = linha.percurso,
                            color = linha.cor,
                            width = 8f
                        )

                        linha.paragens.forEach { paragem ->
                            Marker(
                                state = MarkerState(position = paragem.localizacao),
                                title = paragem.nome,
                                snippet = "Toque para ver horários",
                                onClick = {
                                    paragemSelecionada = paragem
                                    // Use the pre-calculated yOffsetPx here
                                    selectedMarkerScreenPosition = IntOffset(
                                        x = (boxSize.width / 2).toInt(),
                                        y = (boxSize.height / 2 - yOffsetPx).toInt()
                                    )
                                    true
                                }
                            )
                        }
                    }
                }

                // Input fields and search button at the top
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                        .offset(y = 50.dp) // Adjusted offset slightly to accommodate the background
                        // Added semi-transparent white background
                        .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(12.dp) // Added padding inside the background
                        .fillMaxWidth()
                ) {
                    // Start Location Input
                    OutlinedTextField(
                        value = startLocation,
                        onValueChange = { startLocation = it },
                        label = { Text("Ponto de Partida") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp) // Make input field smaller
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Destination Input and Search Button in a Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = destinationLocation,
                            onValueChange = { destinationLocation = it },
                            label = { Text("Destino") },
                            modifier = Modifier
                                .weight(3f) // Increased weight to make it wider (e.g., 3 times the button's implicit weight)
                                .height(56.dp) // Make input field smaller
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                // TODO: Implement bus route search logic here
                                println("Searching route from $startLocation to $destinationLocation")
                            },
                            // Removed fillMaxWidth to let it sit next to the text field
                            // modifier = Modifier.fillMaxWidth()
                        ) {
                            // Replaced Text with Icon for the search symbol
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Pesquisar" // Accessible label for the icon
                            )
                        }
                    }
                }

                // Display the popup when a station is selected and we have an estimated screen position
                paragemSelecionada?.let { paragem ->
                    selectedMarkerScreenPosition?.let { screenPosition ->
                        Column(
                            modifier = Modifier
                                .offset { screenPosition }
                                .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(8.dp)) // Slightly less transparent for the popup
                                .padding(12.dp)
                                .widthIn(max = 250.dp)
                        ) {
                            Text(text = "Paragem: ${paragem.nome}", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            paragem.horarios.forEach {
                                Text("Horário: $it", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    )
}

// Helper function to get the last known location
// This is a basic implementation and might not always return a location immediately.
// For more robust location updates, consider using LocationCallback.
private fun getLastKnownLocation(
    fusedLocationClient: FusedLocationProviderClient,
    onLocation: (Location?) -> Unit
) {
    try {
        fusedLocationClient.lastLocation.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onLocation(task.result)
            } else {
                onLocation(null)
            }
        }
    } catch (e: SecurityException) {
        // Handle the case where location permissions are not granted
        onLocation(null)
    }
}