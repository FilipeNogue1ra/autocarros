@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class)
package com.example.aveirobus.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.aveirobus.data.*
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import kotlin.Result
import kotlin.collections.ArrayList
import androidx.compose.ui.unit.LayoutDirection

// Constants
private const val GOOGLE_API_KEY = "AIzaSyDdDNIdV5OVmv6zcEIhEHmmiS-BEzajNcU"
private val AVEIRO_LOCATION_CENTER = LatLng(40.64427, -8.64554)
// Reduzido para 10km para focar mais em Aveiro
private const val AUTOCOMPLETE_RADIUS_METERS = 10000

private enum class FocusedFieldAuto { NONE, ORIGIN, DESTINATION }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteSearchScreen(paddingValues: PaddingValues = PaddingValues(0.dp)) {
    var originText by remember { mutableStateOf("") }
    var destinationText by remember { mutableStateOf("") }
    var routesList by remember { mutableStateOf<List<Route>>(emptyList()) }
    var selectedRoute by remember { mutableStateOf<Route?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var isLoadingDirections by remember { mutableStateOf(false) }
    var isLoadingAutocomplete by remember { mutableStateOf(false) }
    var directionsErrorMessage by remember { mutableStateOf<String?>(null) }

    var originSuggestions by remember { mutableStateOf<List<PlacePrediction>>(emptyList()) }
    var destinationSuggestions by remember { mutableStateOf<List<PlacePrediction>>(emptyList()) }
    var focusedFieldAuto by remember { mutableStateOf(FocusedFieldAuto.NONE) }
    var autocompleteJob by remember { mutableStateOf<Job?>(null) }
    val focusManager = LocalFocusManager.current

    val context = LocalContext.current

    // Usar uma variável para controlar manualmente a visibilidade do BottomSheet
    var shouldShowBottomSheet by remember { mutableStateOf(false) }

    // Usar o rememberBottomSheetScaffoldState padrão, sem configuração personalizada
    val sheetState = rememberBottomSheetScaffoldState()

    val cameraPositionState: CameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(AVEIRO_LOCATION_CENTER, 13f)
    }

    var showMyLocation by remember { mutableStateOf(false) }
    val fusedLocationClient: FusedLocationProviderClient =
        remember { LocationServices.getFusedLocationProviderClient(context) }

    // Location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions: Map<String, Boolean> ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            showMyLocation = true
            coroutineScope.launch {
                try {
                    val locationResult: android.location.Location? =
                        fusedLocationClient.lastLocation.await()
                    locationResult?.let { loc: android.location.Location ->
                        val currentLatLng = LatLng(loc.latitude, loc.longitude)
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(
                                currentLatLng,
                                15f
                            )
                        )
                    }
                } catch (e: SecurityException) {
                    Log.e(
                        "Location",
                        "Permissão de localização não concedida (SecurityException).",
                        e
                    )
                } catch (e: Exception) {
                    Log.e("Location", "Erro ao obter localização.", e)
                }
            }
        } else {
            Log.d("Location", "Permissão de localização negada.")
        }
    }

    // Helper function must be defined inside @Composable, before its usage
    fun requestLocationPermissionsAndCenter() {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineLocationGranted || coarseLocationGranted) {
            showMyLocation = true
            coroutineScope.launch {
                try {
                    val locationResult: android.location.Location? =
                        fusedLocationClient.lastLocation.await()
                    locationResult?.let { loc: android.location.Location ->
                        val currentLatLng = LatLng(loc.latitude, loc.longitude)
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(
                                currentLatLng,
                                15f
                            )
                        )
                    } ?: Log.d("Location", "Última localização conhecida é nula.")
                } catch (e: SecurityException) {
                    Log.e("Location", "Permissão não concedida ao centrar.", e)
                } catch (e: Exception) {
                    Log.e("Location", "Erro ao centrar.", e)
                }
            }
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Substituir o LaunchedEffect pelo controle manual do estado do BottomSheet
    LaunchedEffect(routesList, selectedRoute, directionsErrorMessage, isLoadingDirections) {
        shouldShowBottomSheet = routesList.isNotEmpty() || selectedRoute != null ||
                directionsErrorMessage != null || isLoadingDirections

        if (shouldShowBottomSheet) {
            // Se houver conteúdo para mostrar, expandimos o sheet (isso é seguro)
            coroutineScope.launch {
                sheetState.bottomSheetState.expand()
            }
        }
        // Não tentamos chamar hide() aqui - isso é o que estava causando o erro
    }

    LaunchedEffect(Unit) {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (fineLocationGranted || coarseLocationGranted) {
            showMyLocation = true
        }
    }

    LaunchedEffect(originText, focusedFieldAuto) {
        if (originText.length > 2 && focusedFieldAuto == FocusedFieldAuto.ORIGIN) {
            autocompleteJob?.cancel()
            isLoadingAutocomplete = true
            autocompleteJob = coroutineScope.launch {
                delay(350)
                val result: Result<List<PlacePrediction>> =
                    fetchPlaceAutocompleteSuggestions(originText)
                isLoadingAutocomplete = false
                if (result.isSuccess) {
                    originSuggestions = result.getOrNull() ?: emptyList()
                } else {
                    Log.e("AutocompleteOrigin", "Erro: ${result.exceptionOrNull()?.message}")
                    originSuggestions = emptyList()
                }
            }
        } else if (focusedFieldAuto != FocusedFieldAuto.ORIGIN || originText.length <= 2) {
            if (focusedFieldAuto == FocusedFieldAuto.ORIGIN && originText.length <= 2) originSuggestions =
                emptyList()
        }
    }

    LaunchedEffect(destinationText, focusedFieldAuto) {
        if (destinationText.length > 2 && focusedFieldAuto == FocusedFieldAuto.DESTINATION) {
            autocompleteJob?.cancel()
            isLoadingAutocomplete = true
            autocompleteJob = coroutineScope.launch {
                delay(350)
                val result: Result<List<PlacePrediction>> =
                    fetchPlaceAutocompleteSuggestions(destinationText)
                isLoadingAutocomplete = false
                if (result.isSuccess) {
                    destinationSuggestions = result.getOrNull() ?: emptyList()
                } else {
                    Log.e("AutocompleteDest", "Erro: ${result.exceptionOrNull()?.message}")
                    destinationSuggestions = emptyList()
                }
            }
        } else if (focusedFieldAuto != FocusedFieldAuto.DESTINATION || destinationText.length <= 2) {
            if (focusedFieldAuto == FocusedFieldAuto.DESTINATION && destinationText.length <= 2) destinationSuggestions =
                emptyList()
        }
    }

    // UI
    BottomSheetScaffold(
        sheetContent = {
            Surface(
                modifier = Modifier.defaultMinSize(minHeight = 56.dp),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                tonalElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .width(40.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                            .align(Alignment.CenterHorizontally)
                    )

                    // Sheet Content
                    if (selectedRoute == null && routesList.isNotEmpty() && !isLoadingDirections) {
                        Text(
                            "Rotas encontradas:",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        LazyColumn(modifier = Modifier.fillMaxHeight(0.6f)) {
                            items(
                                items = routesList,
                                key = { route -> route.hashCode() }) { route ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            selectedRoute = route
                                            coroutineScope.launch {
                                                sheetState.bottomSheetState.expand()
                                            }
                                        },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            "Sumário: ${route.summary ?: "Rota"}",
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                        val firstLeg = route.legs?.firstOrNull()
                                        Text(
                                            "Duração: ${firstLeg?.duration?.text ?: "N/A"}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            "Distância: ${firstLeg?.distance?.text ?: "N/A"}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                            item { Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding() + 16.dp)) }
                        }
                    } else if (selectedRoute != null) {
                        val currentRoute = selectedRoute!!
                        Text(
                            "Itinerário Detalhado:",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        LazyColumn(modifier = Modifier.fillMaxHeight(0.6f)) {
                            currentRoute.legs?.forEachIndexed { legIndex, leg ->
                                item(key = "leg_header_$legIndex") {
                                    Text(
                                        "Parte ${legIndex + 1}: De ${leg.startAddress ?: "Início"} para ${leg.endAddress ?: "Fim"}",
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                                items(
                                    items = leg.steps ?: emptyList(),
                                    key = { step -> "step_${legIndex}_${step.hashCode()}" }) { step ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        elevation = CardDefaults.cardElevation(1.dp)
                                    ) {
                                        // Conteúdo do Card do step como antes...
                                    }
                                }
                            }
                            item { Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding() + 16.dp)) }
                        }
                        Button(
                            onClick = {
                                selectedRoute = null
                                coroutineScope.launch {
                                    if (routesList.isNotEmpty()) {
                                        sheetState.bottomSheetState.partialExpand()
                                    } else {
                                        // Em vez de chamar hide(), apenas atualizamos a variável
                                        shouldShowBottomSheet = false
                                    }
                                }
                            },
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .fillMaxWidth()
                        ) {
                            Text("Ver outras rotas")
                        }
                    } else if (isLoadingDirections) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (directionsErrorMessage != null) {
                        Text(
                            directionsErrorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                                .align(Alignment.CenterHorizontally)
                        )
                    } else {
                        Text(
                            "Insira a origem e o destino para ver as rotas de autocarro.",
                            modifier = Modifier.padding(16.dp)
                                .align(Alignment.CenterHorizontally)
                        )
                    }
                    Spacer(modifier = Modifier.navigationBarsPadding())
                }
            }
        },
        scaffoldState = sheetState,
        sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        // Usar a variável shouldShowBottomSheet para controlar a altura de pico
        sheetPeekHeight = if (shouldShowBottomSheet) 200.dp else 0.dp,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = showMyLocation),
                uiSettings = MapUiSettings(
                    myLocationButtonEnabled = false,
                    zoomControlsEnabled = true
                ),
                contentPadding = PaddingValues(
                    top = paddingValues.calculateTopPadding(),
                    start = paddingValues.calculateStartPadding(LayoutDirection.Ltr),
                    end = paddingValues.calculateEndPadding(LayoutDirection.Ltr),
                    bottom = paddingValues.calculateBottomPadding() +
                            if (shouldShowBottomSheet) 200.dp else 0.dp
                )
            ) {
                selectedRoute?.let { currentRoute ->
                    currentRoute.overviewPolyline?.points?.let { encodedPolyline: String ->
                        val decodedPath: List<LatLng> = decodePolyline(encodedPolyline)
                        if (decodedPath.isNotEmpty()) {
                            Polyline(
                                points = decodedPath,
                                color = Color.Blue,
                                width = 10f,
                                zIndex = 1f
                            )
                        }
                    }
                    currentRoute.legs?.forEach { leg: Leg ->
                        leg.startLocation?.let { startLoc: LocationPoint ->
                            if (startLoc.lat != null && startLoc.lng != null) {
                                Marker(
                                    state = rememberMarkerState(
                                        position = LatLng(
                                            startLoc.lat!!,
                                            startLoc.lng!!
                                        )
                                    ), title = "Início: ${leg.startAddress ?: ""}"
                                )
                            }
                        }
                        leg.endLocation?.let { endLoc: LocationPoint ->
                            if (endLoc.lat != null && endLoc.lng != null) {
                                Marker(
                                    state = rememberMarkerState(
                                        position = LatLng(
                                            endLoc.lat!!,
                                            endLoc.lng!!
                                        )
                                    ), title = "Fim: ${leg.endAddress ?: ""}"
                                )
                            }
                        }
                        leg.steps?.forEach { step: Step ->
                            step.polyline?.points?.let { stepEncodedPolyline: String ->
                                val stepPath: List<LatLng> = decodePolyline(stepEncodedPolyline)
                                if (stepPath.isNotEmpty()) {
                                    val pattern: List<PatternItem>? =
                                        if (step.travelMode == "WALKING") listOf(
                                            Dash(20f),
                                            Gap(10f)
                                        ) else null
                                    Polyline(
                                        points = stepPath,
                                        color = if (step.travelMode == "WALKING") Color.DarkGray else Color(
                                            0xFFE57373
                                        ),
                                        width = 7f, pattern = pattern, zIndex = 0.5f
                                    )
                                }
                            }
                            if (step.travelMode == "TRANSIT") {
                                step.transitDetails?.departureStop?.let { depStop: StopPoint ->
                                    depStop.location?.let { depLoc: LocationPoint ->
                                        if (depLoc.lat != null && depLoc.lng != null) {
                                            Marker(
                                                state = rememberMarkerState(
                                                    position = LatLng(
                                                        depLoc.lat!!,
                                                        depLoc.lng!!
                                                    )
                                                ),
                                                title = "Embarque: ${depStop.name ?: "Paragem"}",
                                                snippet = "Linha: ${step.transitDetails?.line?.shortName ?: step.transitDetails?.line?.name ?: ""}"
                                            )
                                        }
                                    }
                                }
                                step.transitDetails?.arrivalStop?.let { arrStop: StopPoint ->
                                    arrStop.location?.let { arrLoc: LocationPoint ->
                                        if (arrLoc.lat != null && arrLoc.lng != null) {
                                            Marker(
                                                state = rememberMarkerState(
                                                    position = LatLng(
                                                        arrLoc.lat!!,
                                                        arrLoc.lng!!
                                                    )
                                                ),
                                                title = "Desembarque: ${arrStop.name ?: "Paragem"}",
                                                snippet = "Linha: ${step.transitDetails?.line?.shortName ?: step.transitDetails?.line?.name ?: ""}"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // UI Inputs sobreposta ao Mapa - MOVIDO MAIS PARA CIMA
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // MODIFICADO: Reduzido o padding de topo para mover os campos para cima
                    .padding(top = paddingValues.calculateTopPadding() + 2.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        // MODIFICADO: Reduzido o padding para aproximar do topo
                        .padding(start = 16.dp, end = 16.dp, top = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(
                            alpha = 0.95f
                        )
                    )
                ) {
                    // MODIFICADO: Reduzido o padding interno para ganhar espaço
                    Column(modifier = Modifier.padding(12.dp)) {
                        OutlinedTextField(
                            value = originText,
                            onValueChange = {
                                originText = it; if (it.length <= 2) originSuggestions =
                                emptyList()
                            },
                            label = { Text("Ponto de partida") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged {
                                    if (it.isFocused) focusedFieldAuto = FocusedFieldAuto.ORIGIN
                                    else if (originSuggestions.isEmpty() && focusedFieldAuto == FocusedFieldAuto.ORIGIN && !it.isFocused) focusedFieldAuto =
                                        FocusedFieldAuto.NONE
                                },
                            trailingIcon = if (originText.isNotEmpty()) {
                                {
                                    IconButton(onClick = {
                                        originText = ""
                                        originSuggestions = emptyList()
                                        focusedFieldAuto = FocusedFieldAuto.NONE
                                    }) { Icon(Icons.Filled.Search, "Limpar") }
                                }
                            } else null,
                            singleLine = true
                        )
                        AnimatedVisibility(visible = originSuggestions.isNotEmpty() && focusedFieldAuto == FocusedFieldAuto.ORIGIN) {
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 150.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(
                                            alpha = 0.98f
                                        )
                                    )
                            ) {
                                items(originSuggestions) { suggestion: PlacePrediction ->
                                    Text(
                                        suggestion.description ?: "",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                originText = suggestion.description ?: ""
                                                originSuggestions = emptyList()
                                                focusedFieldAuto = FocusedFieldAuto.NONE
                                                focusManager.clearFocus()
                                            }
                                            .padding(16.dp)
                                    )
                                }
                            }
                        }
                        // MODIFICADO: Reduzido espaço entre os campos
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = destinationText,
                            onValueChange = {
                                destinationText =
                                    it; if (it.length <= 2) destinationSuggestions =
                                emptyList()
                            },
                            label = { Text("Ponto de destino") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged {
                                    if (it.isFocused) focusedFieldAuto =
                                        FocusedFieldAuto.DESTINATION
                                    else if (destinationSuggestions.isEmpty() && focusedFieldAuto == FocusedFieldAuto.DESTINATION && !it.isFocused) focusedFieldAuto =
                                        FocusedFieldAuto.NONE
                                },
                            trailingIcon = if (destinationText.isNotEmpty()) {
                                {
                                    IconButton(onClick = {
                                        destinationText = ""
                                        destinationSuggestions = emptyList()
                                        focusedFieldAuto = FocusedFieldAuto.NONE
                                    }) { Icon(Icons.Filled.Search, "Limpar") }
                                }
                            } else null,
                            singleLine = true
                        )
                        AnimatedVisibility(visible = destinationSuggestions.isNotEmpty() && focusedFieldAuto == FocusedFieldAuto.DESTINATION) {
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 150.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(
                                            alpha = 0.98f
                                        )
                                    )
                            ) {
                                items(destinationSuggestions) { suggestion: PlacePrediction ->
                                    Text(
                                        suggestion.description ?: "",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                destinationText = suggestion.description ?: ""
                                                destinationSuggestions = emptyList()
                                                focusedFieldAuto = FocusedFieldAuto.NONE
                                                focusManager.clearFocus()
                                            }
                                            .padding(16.dp)
                                    )
                                }
                            }
                        }
                        // MODIFICADO: Reduzido espaço antes do botão
                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                if (originText.isNotBlank() && destinationText.isNotBlank()) {
                                    isLoadingDirections = true
                                    directionsErrorMessage = null
                                    selectedRoute = null
                                    routesList = emptyList()
                                    focusManager.clearFocus()
                                    coroutineScope.launch {
                                        val result: Result<List<Route>> =
                                            fetchGoogleDirections(originText, destinationText)
                                        isLoadingDirections = false
                                        if (result.isSuccess) {
                                            val fetchedRoutes: List<Route> =
                                                result.getOrNull() ?: emptyList()
                                            routesList = fetchedRoutes
                                            if (fetchedRoutes.isEmpty()) directionsErrorMessage =
                                                "Nenhuma rota de autocarro encontrada."
                                            else { /* Lógica de animar câmara para a primeira rota */
                                            }
                                        } else {
                                            val exception: Throwable? = result.exceptionOrNull()
                                            directionsErrorMessage =
                                                "Erro: ${exception?.message ?: "Desconhecido"}"
                                        }
                                    }
                                } else directionsErrorMessage = "Preencha a origem e o destino."
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoadingDirections && !isLoadingAutocomplete
                        ) {
                            if (isLoadingDirections) CircularProgressIndicator(
                                Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            else Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Search,
                                    "Buscar"
                                ); Spacer(Modifier.width(8.dp)); Text("Buscar Rotas")
                            }
                        }
                    }
                }
                if (isLoadingAutocomplete) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            // FAB para centrar na localização do utilizador
            FloatingActionButton(
                onClick = { requestLocationPermissionsAndCenter() },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(paddingValues)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Icon(
                    Icons.Filled.MyLocation,
                    contentDescription = "Centrar na minha localização"
                )
            }
        }
    }
}

// Place Autocomplete implementation - MODIFICADO para priorizar Aveiro
suspend fun fetchPlaceAutocompleteSuggestions(input: String): Result<List<PlacePrediction>> {
    val apiKey = GOOGLE_API_KEY
    if (apiKey.isEmpty()) {
        Log.e("Autocomplete", "API Key não configurada para Places Autocomplete.")
        return Result.failure(IOException("Chave de API não configurada."))
    }
    val client = OkHttpClient()

    // CORREÇÃO: Simplificamos os parâmetros, mantendo apenas os essenciais e compatíveis
    val url = "https://maps.googleapis.com/maps/api/place/autocomplete/json" +
            "?input=$input" +
            "&language=pt-PT" +
            "&components=country:PT" +
            "&location=${AVEIRO_LOCATION_CENTER.latitude},${AVEIRO_LOCATION_CENTER.longitude}" +
            "&radius=10000" + // Raio reduzido para 10km
            "&key=$apiKey"

    // IMPORTANTE: Log para depuração - verificar a URL que está sendo chamada
    Log.d("Autocomplete", "Requesting URL: $url")
    val request = Request.Builder().url(url).build()

    return try {
        val responseBody: String? = withContext(Dispatchers.IO) {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorBody = response.body?.string()
                Log.e("Autocomplete", "API Error ${response.code}: $errorBody")
                throw IOException("Erro na API Places Autocomplete: ${response.code} - ${response.message}")
            }
            response.body?.string()
        }
        if (responseBody != null) {
            // ADICIONAL: Log para verificar o formato da resposta
            Log.d("Autocomplete", "Response: $responseBody")

            val gson = Gson()
            val apiResponse: PlacesAutocompleteResponse =
                gson.fromJson(responseBody, PlacesAutocompleteResponse::class.java)
            if (apiResponse.status == "OK" || apiResponse.status == "ZERO_RESULTS") {
                // ADICIONAL: Log para verificar quantas sugestões estão retornando
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

// Directions API implementation
suspend fun fetchGoogleDirections(origin: String, destination: String): Result<List<Route>> {
    val apiKey = GOOGLE_API_KEY
    if (apiKey.isEmpty()) {
        Log.e("Directions", "API Key não configurada para Google Directions.")
        return Result.failure(IOException("Chave de API não configurada."))
    }

    val client = OkHttpClient()
    val url = "https://maps.googleapis.com/maps/api/directions/json" +
            "?origin=$origin" +
            "&destination=$destination" +
            "&mode=transit" +
            "&transit_mode=bus" +
            "&language=pt-PT" +
            "&alternatives=true" +
            "&key=$apiKey"

    Log.d("Directions", "Requesting URL: $url")
    val request = Request.Builder().url(url).build()

    return try {
        val responseBody: String? = withContext(Dispatchers.IO) {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorBody = response.body?.string()
                Log.e("Directions", "API Error ${response.code}: $errorBody")
                throw IOException("Erro na API Directions: ${response.code} - ${response.message}")
            }
            response.body?.string()
        }

        if (responseBody != null) {
            val gson = Gson()
            val apiResponse: DirectionsApiResponse =
                gson.fromJson(responseBody, DirectionsApiResponse::class.java)

            if (apiResponse.status == "OK") {
                Result.success(apiResponse.routes ?: emptyList())
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

// Polyline decoding helper function
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
            b = encoded[index++].toInt() - 63
            result = result or (b and 0x1f shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lat += dlat

        shift = 0
        result = 0
        do {
            b = encoded[index++].toInt() - 63
            result = result or (b and 0x1f shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lng += dlng

        poly.add(LatLng(lat.toDouble() / 1E5, lng.toDouble() / 1E5))
    }
    return poly
}

// Preview moved outside the composable function
@Preview(showBackground = true, locale = "pt")
@Composable
fun RouteSearchScreenPreview() {
    MaterialTheme {
        RouteSearchScreen()
    }
}