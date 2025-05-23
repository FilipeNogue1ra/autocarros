@file:OptIn(ExperimentalMaterial3Api::class)
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
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.aveirobus.data.*
import com.example.aveirobus.ui.viewmodels.UserPreferencesViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.IOException
import java.net.URLEncoder


// Constants
private const val GOOGLE_API_KEY = "AIzaSyDdDNIdV5OVmv6zcEIhEHmmiS-BEzajNcU"
private val AVEIRO_LOCATION_CENTER = LatLng(40.64427, -8.64554)
private const val AUTOCOMPLETE_RADIUS_METERS = 10000

private enum class FocusedFieldAuto { NONE, ORIGIN, DESTINATION }
private enum class TravelStepType { WALKING, BUS, TRANSFER }

@Composable
fun RouteSearchScreen(
    paddingValues: PaddingValues = PaddingValues(0.dp),
    navController: NavController,
    viewModel: UserPreferencesViewModel
) {
    var originText by remember { mutableStateOf("") }
    var destinationText by remember { mutableStateOf("") }
    var routesList by remember { mutableStateOf<List<Route>>(emptyList()) }
    var selectedRoute by remember { mutableStateOf<Route?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var isLoadingDirections by remember { mutableStateOf(false) }
    var isLoadingAutocomplete by remember { mutableStateOf(false) }
    var directionsErrorMessage by remember { mutableStateOf<String?>(null) }

    // Obter preferências do usuário para acessibilidade
    val userPreferencesState = viewModel.userPreferencesFlow.collectAsState(initial = null)
    val userPreferences = userPreferencesState.value
    val isWheelchairAccessible = userPreferences?.wheelchairAccessibilityEnabled ?: false

    var originSuggestions by remember { mutableStateOf<List<PlacePrediction>>(emptyList()) }
    var destinationSuggestions by remember { mutableStateOf<List<PlacePrediction>>(emptyList()) }
    var focusedFieldAuto by remember { mutableStateOf(FocusedFieldAuto.NONE) }
    var autocompleteJob by remember { mutableStateOf<Job?>(null) }
    val focusManager = LocalFocusManager.current

    val context = LocalContext.current

    // Estado para controlar quais partes da rota devem ser destacadas
    var highlightBusStops by remember { mutableStateOf(true) }
    var highlightWalkingPath by remember { mutableStateOf(true) }
    var highlightBusPath by remember { mutableStateOf(true) }

    // Controle do BottomSheet
    var shouldShowBottomSheet by remember { mutableStateOf(false) }
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
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            showMyLocation = true
            coroutineScope.launch {
                try {
                    val locationResult = fusedLocationClient.lastLocation.await()
                    locationResult?.let { loc ->
                        val currentLatLng = LatLng(loc.latitude, loc.longitude)
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f),
                            1000
                        )
                    }
                } catch (e: SecurityException) {
                    Log.e("Location", "Permissão de localização não concedida (SecurityException).", e)
                } catch (e: Exception) {
                    Log.e("Location", "Erro ao obter localização.", e)
                }
            }
        } else {
            Log.d("Location", "Permissão de localização negada.")
        }
    }

    // Função para buscar rotas levando em consideração a acessibilidade
    fun fetchAccessibleRoutes(origin: String, destination: String) {
        isLoadingDirections = true
        directionsErrorMessage = null
        selectedRoute = null
        routesList = emptyList()
        focusManager.clearFocus()

        coroutineScope.launch {
            val result = fetchGoogleDirections(origin, destination, isWheelchairAccessible)
            isLoadingDirections = false

            if (result.isSuccess) {
                val fetchedRoutes = result.getOrNull() ?: emptyList()

                // Filtrar apenas rotas que incluem transporte público (ônibus)
                val busRoutes = fetchedRoutes.filter { route ->
                    route.legs?.any { leg ->
                        leg.steps?.any { step ->
                            step.travelMode == "TRANSIT" &&
                                    (step.transitDetails?.line?.vehicle?.type == "BUS" ||
                                            step.transitDetails?.line?.vehicle?.name?.contains("Bus", ignoreCase = true) == true)
                        } == true
                    } == true
                }

                routesList = busRoutes

                if (busRoutes.isEmpty()) {
                    if (isWheelchairAccessible) {
                        directionsErrorMessage = "Nenhuma rota acessível para cadeira de rodas encontrada."
                    } else if (fetchedRoutes.isEmpty()) {
                        directionsErrorMessage = "Nenhuma rota encontrada."
                    } else {
                        directionsErrorMessage = "Nenhuma rota de autocarro disponível entre estes pontos."
                    }
                }
            } else {
                val exception = result.exceptionOrNull()
                directionsErrorMessage = "Erro: ${exception?.message ?: "Desconhecido"}"
            }
        }
    }

    // Helper function para solicitar permissão de localização
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
                    val locationResult = fusedLocationClient.lastLocation.await()
                    locationResult?.let { loc ->
                        val currentLatLng = LatLng(loc.latitude, loc.longitude)
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f),
                            1000
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

    // Função para destacar a rota selecionada no mapa
    fun centerRouteOnMap(route: Route) {
        route.overviewPolyline?.points?.let { encodedPolyline ->
            val decodedPath = decodePolyline(encodedPolyline)
            if (decodedPath.isNotEmpty()) {
                val bounds = LatLngBounds.builder()
                decodedPath.forEach { bounds.include(it) }

                coroutineScope.launch {
                    try {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngBounds(bounds.build(), 100),
                            1000
                        )
                    } catch (e: Exception) {
                        Log.e("RouteSearch", "Erro ao animar câmera: ${e.message}")
                    }
                }
            }
        }
    }

    // Efeitos colaterais
    LaunchedEffect(routesList, selectedRoute, directionsErrorMessage, isLoadingDirections) {
        shouldShowBottomSheet = routesList.isNotEmpty() || selectedRoute != null ||
                directionsErrorMessage != null || isLoadingDirections

        if (shouldShowBottomSheet) {
            coroutineScope.launch {
                sheetState.bottomSheetState.expand()
            }
        }
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

    // Autocompletar origem
    LaunchedEffect(originText, focusedFieldAuto) {
        if (originText.length > 2 && focusedFieldAuto == FocusedFieldAuto.ORIGIN) {
            autocompleteJob?.cancel()
            isLoadingAutocomplete = true
            autocompleteJob = launch {
                delay(350)
                val result = fetchPlaceAutocompleteSuggestions(originText)
                isLoadingAutocomplete = false
                if (result.isSuccess) {
                    originSuggestions = result.getOrNull() ?: emptyList()
                } else {
                    Log.e("AutocompleteOrigin", "Erro: ${result.exceptionOrNull()?.message}")
                    originSuggestions = emptyList()
                }
            }
        } else if (focusedFieldAuto != FocusedFieldAuto.ORIGIN || originText.length <= 2) {
            if (focusedFieldAuto == FocusedFieldAuto.ORIGIN && originText.length <= 2)
                originSuggestions = emptyList()
        }
    }

    // Autocompletar destino
    LaunchedEffect(destinationText, focusedFieldAuto) {
        if (destinationText.length > 2 && focusedFieldAuto == FocusedFieldAuto.DESTINATION) {
            autocompleteJob?.cancel()
            isLoadingAutocomplete = true
            autocompleteJob = launch {
                delay(350)
                val result = fetchPlaceAutocompleteSuggestions(destinationText)
                isLoadingAutocomplete = false
                if (result.isSuccess) {
                    destinationSuggestions = result.getOrNull() ?: emptyList()
                } else {
                    Log.e("AutocompleteDest", "Erro: ${result.exceptionOrNull()?.message}")
                    destinationSuggestions = emptyList()
                }
            }
        } else if (focusedFieldAuto != FocusedFieldAuto.DESTINATION || destinationText.length <= 2) {
            if (focusedFieldAuto == FocusedFieldAuto.DESTINATION && destinationText.length <= 2)
                destinationSuggestions = emptyList()
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

                    // Sheet Content - Lista de Rotas
                    if (selectedRoute == null && routesList.isNotEmpty() && !isLoadingDirections) {
                        Text(
                            "Rotas de Autocarro Disponíveis:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Mostrar indicação de acessibilidade se ativada
                        if (isWheelchairAccessible) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFE3F2FD)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsBus,
                                        contentDescription = null,
                                        tint = Color(0xFF1976D2)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Mostrando apenas rotas acessíveis para cadeira de rodas",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF1976D2)
                                    )
                                }
                            }
                        }

                        LazyColumn(modifier = Modifier.fillMaxHeight(0.6f)) {
                            items(
                                items = routesList,
                                key = { route -> route.hashCode() }
                            ) { route ->
                                BusRouteCard(
                                    route = route,
                                    isWheelchairAccessible = isWheelchairAccessible,
                                    onClick = {
                                        selectedRoute = route
                                        centerRouteOnMap(route)
                                        coroutineScope.launch {
                                            sheetState.bottomSheetState.expand()
                                        }
                                    }
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding() + 16.dp))
                            }
                        }
                    } else if (selectedRoute != null) {
                        val currentRoute = selectedRoute!!

                        // Cabeçalho com botão para voltar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    selectedRoute = null
                                    coroutineScope.launch {
                                        if (routesList.isNotEmpty()) {
                                            sheetState.bottomSheetState.partialExpand()
                                        } else {
                                            shouldShowBottomSheet = false
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsBus,
                                    contentDescription = "Voltar para lista de rotas"
                                )
                            }

                            Text(
                                "Detalhes da Rota de Autocarro",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        // Resumo da rota
                        currentRoute.legs?.firstOrNull()?.let { firstLeg ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "De: ${firstLeg.startAddress ?: "Origem"}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Para: ${firstLeg.endAddress ?: "Destino"}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                "Duração:",
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                            Text(
                                                firstLeg.duration?.text ?: "N/A",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Column {
                                            Text(
                                                "Distância:",
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                            Text(
                                                firstLeg.distance?.text ?: "N/A",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Filtros para visualização
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            FilterChip(
                                selected = highlightWalkingPath,
                                onClick = { highlightWalkingPath = !highlightWalkingPath },
                                label = { Text("Caminhos a Pé") },
                                leadingIcon = if (highlightWalkingPath) {
                                    { Icon(Icons.Default.MyLocation, contentDescription = null) }
                                } else null
                            )

                            FilterChip(
                                selected = highlightBusPath,
                                onClick = { highlightBusPath = !highlightBusPath },
                                label = { Text("Rota do Autocarro") },
                                leadingIcon = if (highlightBusPath) {
                                    { Icon(Icons.Default.DirectionsBus, contentDescription = null) }
                                } else null
                            )

                            FilterChip(
                                selected = highlightBusStops,
                                onClick = { highlightBusStops = !highlightBusStops },
                                label = { Text("Paragens") },
                                leadingIcon = if (highlightBusStops) {
                                    { Icon(Icons.Default.DirectionsBus, contentDescription = null) }
                                } else null
                            )
                        }

                        Text(
                            "Itinerário Detalhado:",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )

                        LazyColumn(modifier = Modifier.fillMaxHeight(0.5f)) {
                            currentRoute.legs?.forEachIndexed { legIndex, leg ->
                                leg.steps?.forEachIndexed { stepIndex, step ->
                                    item(key = "step_${legIndex}_${stepIndex}") {
                                        RouteStepItem(step = step)
                                    }
                                }
                            }
                            item { Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding() + 16.dp)) }
                        }
                    } else if (isLoadingDirections) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("A procurar rotas de autocarro...")
                            }
                        }
                    } else if (directionsErrorMessage != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                directionsErrorMessage!!,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Insira a origem e o destino para ver as rotas de autocarro disponíveis.",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.navigationBarsPadding())
                }
            }
        },
        scaffoldState = sheetState,
        sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
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
                    // Desenhar o poliline geral da rota para referência
                    if (highlightBusPath) {
                        currentRoute.overviewPolyline?.points?.let { encodedPolyline: String ->
                            val decodedPath: List<LatLng> = decodePolyline(encodedPolyline)
                            if (decodedPath.isNotEmpty()) {
                                Polyline(
                                    points = decodedPath,
                                    color = Color.Gray.copy(alpha = 0.3f),
                                    width = 5f,
                                    zIndex = 0.5f
                                )
                            }
                        }
                    }

                    // Processar os detalhes de cada perna da rota
                    currentRoute.legs?.forEach { leg: Leg ->
                        leg.steps?.forEach { step: Step ->
                            when (step.travelMode) {
                                "WALKING" -> {
                                    if (highlightWalkingPath) {
                                        step.polyline?.points?.let { stepEncodedPolyline: String ->
                                            val stepPath: List<LatLng> = decodePolyline(stepEncodedPolyline)
                                            if (stepPath.isNotEmpty()) {
                                                Polyline(
                                                    points = stepPath,
                                                    color = Color(0xFF4CAF50), // Verde para caminhada
                                                    width = 8f,
                                                    pattern = listOf(Dash(20f), Gap(10f)),
                                                    zIndex = 1f
                                                )
                                            }
                                        }
                                    }
                                }
                                "TRANSIT" -> {
                                    if (highlightBusPath) {
                                        // Destacar o percurso do ônibus
                                        step.polyline?.points?.let { stepEncodedPolyline: String ->
                                            val stepPath: List<LatLng> = decodePolyline(stepEncodedPolyline)
                                            if (stepPath.isNotEmpty()) {
                                                Polyline(
                                                    points = stepPath,
                                                    color = Color(0xFF1976D2), // Azul para ônibus
                                                    width = 10f,
                                                    zIndex = 2f
                                                )
                                            }
                                        }
                                    }

                                    if (highlightBusStops) {
                                        // Marcar ponto de embarque com ícone de ônibus
                                        step.transitDetails?.departureStop?.let { depStop: StopPoint ->
                                            depStop.location?.let { depLoc: LocationPoint ->
                                                if (depLoc.lat != null && depLoc.lng != null) {
                                                    Marker(
                                                        state = rememberMarkerState(
                                                            position = LatLng(
                                                                depLoc.lat,
                                                                depLoc.lng
                                                            )
                                                        ),
                                                        title = "Embarque: ${depStop.name ?: "Paragem"}",
                                                        snippet = "Linha: ${step.transitDetails.line?.shortName ?: step.transitDetails.line?.name ?: ""}",
                                                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                                                    )
                                                }
                                            }
                                        }

                                        // Marcar ponto de desembarque com ícone de ônibus
                                        step.transitDetails?.arrivalStop?.let { arrStop: StopPoint ->
                                            arrStop.location?.let { arrLoc: LocationPoint ->
                                                if (arrLoc.lat != null && arrLoc.lng != null) {
                                                    Marker(
                                                        state = rememberMarkerState(
                                                            position = LatLng(
                                                                arrLoc.lat,
                                                                arrLoc.lng
                                                            )
                                                        ),
                                                        title = "Desembarque: ${arrStop.name ?: "Paragem"}",
                                                        snippet = "Linha: ${step.transitDetails.line?.shortName ?: step.transitDetails.line?.name ?: ""}",
                                                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                                                    )
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

            // UI Inputs sobreposta ao Mapa
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = paddingValues.calculateTopPadding() + 2.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .shadow(8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = originText,
                            onValueChange = {
                                originText = it
                                if (it.length <= 2) originSuggestions = emptyList()
                            },
                            label = { Text("Ponto de partida") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged {
                                    if (it.isFocused) focusedFieldAuto = FocusedFieldAuto.ORIGIN
                                    else if (originSuggestions.isEmpty() && focusedFieldAuto == FocusedFieldAuto.ORIGIN && !it.isFocused)
                                        focusedFieldAuto = FocusedFieldAuto.NONE
                                },
                            trailingIcon = if (originText.isNotEmpty()) {
                                {
                                    IconButton(onClick = {
                                        originText = ""
                                        originSuggestions = emptyList()
                                        focusedFieldAuto = FocusedFieldAuto.NONE
                                    }) { Icon(Icons.Default.Search, "Limpar") }
                                }
                            } else null,
                            singleLine = true
                        )

                        AnimatedVisibility(visible = originSuggestions.isNotEmpty() && focusedFieldAuto == FocusedFieldAuto.ORIGIN) {
                            LazyColumn(
                                modifier = Modifier
                                    .heightIn(max = 150.dp)
                                    .background(MaterialTheme.colorScheme.surface)
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

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = destinationText,
                            onValueChange = {
                                destinationText = it
                                if (it.length <= 2) destinationSuggestions = emptyList()
                            },
                            label = { Text("Ponto de destino") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged {
                                    if (it.isFocused) focusedFieldAuto = FocusedFieldAuto.DESTINATION
                                    else if (destinationSuggestions.isEmpty() && focusedFieldAuto == FocusedFieldAuto.DESTINATION && !it.isFocused)
                                        focusedFieldAuto = FocusedFieldAuto.NONE
                                },
                            trailingIcon = if (destinationText.isNotEmpty()) {
                                {
                                    IconButton(onClick = {
                                        destinationText = ""
                                        destinationSuggestions = emptyList()
                                        focusedFieldAuto = FocusedFieldAuto.NONE
                                    }) { Icon(Icons.Default.Search, "Limpar") }
                                }
                            } else null,
                            singleLine = true
                        )

                        AnimatedVisibility(visible = destinationSuggestions.isNotEmpty() && focusedFieldAuto == FocusedFieldAuto.DESTINATION) {
                            LazyColumn(
                                modifier = Modifier
                                    .heightIn(max = 150.dp)
                                    .background(MaterialTheme.colorScheme.surface)
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

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (originText.isNotBlank() && destinationText.isNotBlank()) {
                                    fetchAccessibleRoutes(originText, destinationText)
                                } else {
                                    directionsErrorMessage = "Preencha a origem e o destino."
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoadingDirections && !isLoadingAutocomplete
                        ) {
                            if (isLoadingDirections) {
                                CircularProgressIndicator(
                                    Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.DirectionsBus, "Buscar")
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        if (isWheelchairAccessible) "Buscar Autocarros Acessíveis"
                                        else "Buscar Autocarros"
                                    )
                                }
                            }
                        }

                        // Indicador visual de acessibilidade ativada
                        if (isWheelchairAccessible) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsBus,
                                    contentDescription = null,
                                    tint = Color(0xFF1976D2),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Modo de acessibilidade ativado",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF1976D2)
                                )
                            }
                        }
                    }
                }

                if (isLoadingAutocomplete) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
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
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    Icons.Default.MyLocation,
                    contentDescription = "Centrar na minha localização"
                )
            }

            // FAB para acesso rápido às definições
            FloatingActionButton(
                onClick = { navController.navigate("opcoes") },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(paddingValues)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsBus,
                    contentDescription = "Opções"
                )
            }
        }
    }
}

@Composable
fun BusRouteCard(
    route: Route,
    isWheelchairAccessible: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val firstLeg = route.legs?.firstOrNull()

            // Número da rota ou linha principal (se disponível)
            val mainBusLine = firstLeg?.steps?.find {
                it.travelMode == "TRANSIT" && it.transitDetails?.line != null
            }?.transitDetails?.line

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsBus,
                    contentDescription = null,
                    tint = if (isWheelchairAccessible) Color(0xFF1976D2) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (mainBusLine?.shortName != null) {
                            "Linha ${mainBusLine.shortName}"
                        } else if (mainBusLine?.name != null) {
                            mainBusLine.name ?: "Autocarro"
                        } else {
                            "Rota via autocarro"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = route.summary ?: "Rota de Autocarro",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Badge de acessibilidade se necessário
                if (isWheelchairAccessible) {
                    Icon(
                        imageVector = Icons.Default.DirectionsBus,
                        contentDescription = "Acessível para cadeira de rodas",
                        tint = Color(0xFF1976D2),
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color(0xFFE3F2FD), CircleShape)
                            .padding(4.dp)
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Duração",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        firstLeg?.duration?.text ?: "N/A",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column {
                    Text(
                        "Distância",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        firstLeg?.distance?.text ?: "N/A",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Preço estimado (opcional)
                Column {
                    Text(
                        "Preço estimado",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        "€?", // Valor fixo
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Número de transferências
            val transfers = route.legs?.sumOf { leg ->
                leg.steps?.count { it.travelMode == "TRANSIT" } ?: 0
            }?.minus(1) ?: 0

            if (transfers > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$transfers transferência${if (transfers > 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

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

@Composable
fun RouteStepItem(step: Step) {
    val stepType = when (step.travelMode) {
        "WALKING" -> TravelStepType.WALKING
        "TRANSIT" -> TravelStepType.BUS
        else -> TravelStepType.TRANSFER
    }

    val (backgroundColor, contentColor) = when (stepType) {
        TravelStepType.WALKING -> Pair(Color(0xFFE8F5E9), Color(0xFF2E7D32))
        TravelStepType.BUS -> Pair(Color(0xFFE3F2FD), Color(0xFF1565C0))
        TravelStepType.TRANSFER -> Pair(Color(0xFFFFFDE7), Color(0xFFFF8F00))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ícone baseado no tipo de passo
            when (stepType) {
                TravelStepType.WALKING -> {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Caminhar",
                        tint = contentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                TravelStepType.BUS -> {
                    Icon(
                        imageVector = Icons.Default.DirectionsBus,
                        contentDescription = "Autocarro",
                        tint = contentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                TravelStepType.TRANSFER -> {
                    Icon(
                        imageVector = Icons.Default.DirectionsBus,
                        contentDescription = "Transferência",
                        tint = contentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                when (stepType) {
                    TravelStepType.WALKING -> {
                        Text(
                            text = "Caminhar ${step.distance?.text ?: ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = contentColor
                        )
                        Text(
                            text = step.htmlInstructions?.replace("<[^>]*>".toRegex(), "") ?: "Siga a pé",
                            style = MaterialTheme.typography.bodySmall,
                            color = contentColor.copy(alpha = 0.8f)
                        )
                    }
                    TravelStepType.BUS -> {
                        val busLine = step.transitDetails?.line
                        Text(
                            text = if (busLine?.shortName != null) {
                                "Autocarro ${busLine.shortName}"
                            } else if (busLine?.name != null) {
                                busLine.name ?: "Autocarro"
                            } else {
                                "Autocarro"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = contentColor
                        )
                        Text(
                            text = "De: ${step.transitDetails?.departureStop?.name ?: "?"} → Para: ${step.transitDetails?.arrivalStop?.name ?: "?"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = contentColor.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "Duração: ${step.duration?.text ?: "?"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = contentColor.copy(alpha = 0.8f)
                        )
                    }
                    TravelStepType.TRANSFER -> {
                        Text(
                            text = "Transferência",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = contentColor
                        )
                        Text(
                            text = step.htmlInstructions?.replace("<[^>]*>".toRegex(), "") ?: "Faça a transferência",
                            style = MaterialTheme.typography.bodySmall,
                            color = contentColor.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

// API functions - implementation for fetchPlaceAutocompleteSuggestions and fetchGoogleDirections
// are defined elsewhere