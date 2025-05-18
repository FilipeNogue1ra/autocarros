@file:OptIn(ExperimentalMaterial3Api::class) // OptIn a nível de ficheiro para APIs experimentais do Material 3
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.ModalBottomSheetLayout
import androidx.compose.material3.ModalBottomSheetValue // <<< IMPORTAÇÃO ADICIONADA
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection // <<< IMPORTAÇÃO ADICIONADA
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

// Constantes
private const val GOOGLE_API_KEY = "AIzaSyDdDNIdV5OVmv6zcEIhEHmmiS-BEzajNcU"
private val AVEIRO_LOCATION_CENTER = LatLng(40.64427, -8.64554)
private const val AUTOCOMPLETE_RADIUS_METERS = 20000 // 20km

private enum class FocusedFieldAuto { NONE, ORIGIN, DESTINATION }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteSearchScreen(paddingValues: PaddingValues = PaddingValues(0.dp)) {
    var originText by remember { mutableStateOf("") }
    var destinationText by remember { mutableStateOf("") }
    var routesList by remember { mutableStateOf<List<com.example.aveirobus.data.Route>>(emptyList()) }
    var selectedRoute by remember { mutableStateOf<com.example.aveirobus.data.Route?>(null) }
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
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false, // Permite estado meio expandido
        initialValue = if (routesList.isEmpty() && selectedRoute == null && directionsErrorMessage == null) ModalBottomSheetValue.Hidden else ModalBottomSheetValue.PartiallyExpanded,)

    val cameraPositionState: CameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(AVEIRO_LOCATION_CENTER, 13f)
    }

    var showMyLocation by remember { mutableStateOf(false) }
    val fusedLocationClient: FusedLocationProviderClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    LaunchedEffect(routesList, selectedRoute, directionsErrorMessage, isLoadingDirections) {
        if (!isLoadingDirections) { // Só altera o estado do sheet se não estiver a carregar direções
            if (routesList.isNotEmpty() || selectedRoute != null || directionsErrorMessage != null) {
                // Se há algo para mostrar e o sheet está escondido (e não a meio de uma animação para esconder)
                if (sheetState.currentValue == ModalBottomSheetValue.Hidden && !sheetState.isVisible) {
                    coroutineScope.launch { sheetState.partialExpand() }
                }
            } else { // Sem rotas, sem seleção, sem erro E não a carregar
                // Se não há nada para mostrar e o sheet está visível
                if (sheetState.currentValue != ModalBottomSheetValue.Hidden && sheetState.isVisible) {
                    coroutineScope.launch { sheetState.hide() }
                }
            }
        }
    }

    ModalBottomSheetLayout(
        sheetContent = {
            Surface( // Envolver o conteúdo do sheet com um Surface para styling
                modifier = Modifier.defaultMinSize(minHeight = 56.dp), // Altura mínima para o drag handle
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f), // Fundo semi-transparente
                tonalElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    // "Pegador" (Drag Handle) visual
                    Box(
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .width(40.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                            .align(Alignment.CenterHorizontally)
                    )

                    // Lógica condicional para mostrar lista de rotas, detalhes, ou mensagens
                    if (selectedRoute == null && routesList.isNotEmpty() && !isLoadingDirections) {
                        Text("Rotas encontradas:", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                        LazyColumn(modifier = Modifier.fillMaxHeight(0.6f)) { // Limitar altura
                            items(items = routesList, key = { route -> route.hashCode() }) { route ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                                        selectedRoute = route
                                        coroutineScope.launch { sheetState.expand() } // Expandir ao selecionar
                                        // ... (lógica de animar câmara para a rota) ...
                                    },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Sumário: ${route.summary ?: "Rota"}", style = MaterialTheme.typography.titleLarge)
                                        val firstLeg = route.legs?.firstOrNull()
                                        Text("Duração: ${firstLeg?.duration?.text ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                                        Text("Distância: ${firstLeg?.distance?.text ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                            item { Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding() + 16.dp)) }
                        }
                    } else if (selectedRoute != null) {
                        val currentRoute = selectedRoute!!
                        Text("Itinerário Detalhado:", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                        LazyColumn(modifier = Modifier.fillMaxHeight(0.6f)) { // Limitar altura
                            currentRoute.legs?.forEachIndexed { legIndex, leg ->
                                item(key = "leg_header_$legIndex") {
                                    Text(
                                        "Parte ${legIndex + 1}: De ${leg.startAddress ?: "Início"} para ${leg.endAddress ?: "Fim"}",
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                                items(items = leg.steps ?: emptyList(), key = { step -> "step_${legIndex}_${step.hashCode()}" }) { step ->
                                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), elevation = CardDefaults.cardElevation(1.dp)) {
                                        // ... (Conteúdo do Card do step como antes) ...
                                    }
                                }
                            }
                            item { Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding() + 16.dp)) }
                        }
                        Button(
                            onClick = {
                                selectedRoute = null
                                coroutineScope.launch {
                                    if (routesList.isNotEmpty()) sheetState.partialExpand() else sheetState.hide()
                                }
                            },
                            modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth()
                        ) {
                            Text("Ver outras rotas")
                        }
                    } else if (isLoadingDirections) {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (directionsErrorMessage != null) {
                        Text(directionsErrorMessage!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally))
                    } else {
                        Text("Insira a origem e o destino para ver as rotas de autocarro.", modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally))
                    }
                    // Espaço para garantir que o conteúdo não fica por baixo da navegação do sistema se o sheet for muito alto
                    Spacer(modifier = Modifier.navigationBarsPadding()) // Importante para edge-to-edge
                }
            }
        },
        sheetState = sheetState,
        sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        // Altura inicial visível do painel. Ajustar conforme necessário.
        sheetPeekHeight = if (routesList.isNotEmpty() || selectedRoute != null || directionsErrorMessage != null || isLoadingDirections) 200.dp else 0.dp,
        // scrimColor = ModalBottomSheetDefaults.scrimColor.copy(alpha = 0.32f) // Opcional para escurecer o fundo
    ) {
        Box(modifier = Modifier.fillMaxSize()) { // Ocupa todo o espaço dado pelo ModalBottomSheetLayout
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = showMyLocation),
                uiSettings = MapUiSettings(myLocationButtonEnabled = false, zoomControlsEnabled = true),
                // Ajustar o padding do mapa para não ficar por baixo do sheet quando parcialmente visível
                // e também respeitar o paddingValues do Scaffold (barras de sistema)
                contentPadding = PaddingValues(
                    top = paddingValues.calculateTopPadding(),
                    start = paddingValues.calculateStartPadding(LayoutDirection.Ltr), // ou LocalLayoutDirection.current
                    end = paddingValues.calculateEndPadding(LayoutDirection.Ltr),
                    bottom = paddingValues.calculateBottomPadding() +
                            if (sheetState.targetValue != ModalBottomSheetValue.Hidden && sheetState.currentValue != ModalBottomSheetValue.Hidden) {
                                // Adiciona a altura do peek do sheet se ele estiver visível
                                // Isto é uma aproximação, pode precisar de ajuste mais fino
                                // ou usar a altura real do sheet se possível.
                                // Para simplificar, pode usar um valor fixo ou sheetPeekHeight.
                                200.dp // Exemplo: sheetPeekHeight
                            } else {
                                0.dp
                            }
                )
            ) {
                selectedRoute?.let { currentRoute ->
                    // ... (código de desenho de rotas e markers como antes) ...
                }
            }

            // UI de inputs sobreposta ao mapa, respeitando o paddingValues do Scaffold (para TopAppBar)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = paddingValues.calculateTopPadding()) // Só aplicar padding do topo aqui
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // ... (Campos de Origem, Destino, Autocomplete e Botão de Busca como antes) ...
                    }
                }
                if (isLoadingAutocomplete) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(horizontal=16.dp, vertical = 4.dp))
                }
            }

            // FAB para centrar na localização do utilizador
            FloatingActionButton(
                onClick = { requestLocationPermissionsAndCenter() },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(paddingValues) // Aplica o padding do Scaffold para não ficar por baixo da BottomNav
                    .padding(16.dp), // Padding adicional para o FAB
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Icon(Icons.Filled.MyLocation, contentDescription = "Centrar na minha localização")
            }
        } // Fim da Box do conteúdo principal
    }


    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions: Map<String, Boolean> ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            showMyLocation = true
            coroutineScope.launch {
                try {
                    val locationResult: android.location.Location? = fusedLocationClient.lastLocation.await()
                    locationResult?.let { loc: android.location.Location ->
                        val currentLatLng = LatLng(loc.latitude, loc.longitude)
                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                    }
                } catch (e: SecurityException) {
                    Log.e("Location", "Permissão de localização não concedida (SecurityException).", e)
                } catch (e: Exception) {
                    Log.e("Location", "Erro ao obter localização.", e)
                }
            }
        } else {
            Log.d("Location", "Permissão de localização negada.")
            // TODO: Mostrar mensagem ao utilizador sobre a necessidade da permissão (ex: Snackbar)
        }
    }

    fun requestLocationPermissionsAndCenter() {
        val fineLocationGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseLocationGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (fineLocationGranted || coarseLocationGranted) {
            showMyLocation = true
            coroutineScope.launch {
                try {
                    val locationResult: android.location.Location? = fusedLocationClient.lastLocation.await()
                    locationResult?.let { loc: android.location.Location ->
                        val currentLatLng = LatLng(loc.latitude, loc.longitude)
                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                    }  ?: Log.d("Location", "Última localização conhecida é nula.")
                } catch (e: SecurityException) {Log.e("Location", "Permissão não concedida ao centrar.", e)}
                catch (e: Exception) {Log.e("Location", "Erro ao centrar.", e)}
            }
        } else {
            locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    LaunchedEffect(Unit) {
        val fineLocationGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseLocationGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
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
                val result: Result<List<PlacePrediction>> = fetchPlaceAutocompleteSuggestions(originText)
                isLoadingAutocomplete = false
                if (result.isSuccess) {
                    originSuggestions = result.getOrNull() ?: emptyList()
                } else {
                    Log.e("AutocompleteOrigin", "Erro: ${result.exceptionOrNull()?.message}")
                    originSuggestions = emptyList()
                }
            }
        } else if (focusedFieldAuto != FocusedFieldAuto.ORIGIN || originText.length <= 2) {
            if(focusedFieldAuto == FocusedFieldAuto.ORIGIN && originText.length <=2 ) originSuggestions = emptyList()
        }
    }

    LaunchedEffect(destinationText, focusedFieldAuto) {
        if (destinationText.length > 2 && focusedFieldAuto == FocusedFieldAuto.DESTINATION) {
            autocompleteJob?.cancel()
            isLoadingAutocomplete = true
            autocompleteJob = coroutineScope.launch {
                delay(350)
                val result: Result<List<PlacePrediction>> = fetchPlaceAutocompleteSuggestions(destinationText)
                isLoadingAutocomplete = false
                if (result.isSuccess) {
                    destinationSuggestions = result.getOrNull() ?: emptyList()
                } else {
                    Log.e("AutocompleteDest", "Erro: ${result.exceptionOrNull()?.message}")
                    destinationSuggestions = emptyList()
                }
            }
        } else if (focusedFieldAuto != FocusedFieldAuto.DESTINATION || destinationText.length <= 2) {
            if(focusedFieldAuto == FocusedFieldAuto.DESTINATION && destinationText.length <= 2) destinationSuggestions = emptyList()
        }
    }

    Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = showMyLocation),
            uiSettings = MapUiSettings(myLocationButtonEnabled = false, zoomControlsEnabled = true)
        ) {
            selectedRoute?.let { currentRoute: com.example.aveirobus.data.Route ->
                currentRoute.overviewPolyline?.points?.let { encodedPolyline: String ->
                    val decodedPath: List<LatLng> = decodePolyline(encodedPolyline)
                    if (decodedPath.isNotEmpty()) {
                        Polyline(points = decodedPath, color = Color.Blue, width = 10f, zIndex = 1f)
                    }
                }
                currentRoute.legs?.forEach { leg: Leg ->
                    leg.startLocation?.let { startLoc: LocationPoint ->
                        if (startLoc.lat != null && startLoc.lng != null) {
                            Marker(state = rememberMarkerState(position = LatLng(startLoc.lat!!, startLoc.lng!!)), title = "Início: ${leg.startAddress ?: ""}")
                        }
                    }
                    leg.endLocation?.let { endLoc: LocationPoint ->
                        if (endLoc.lat != null && endLoc.lng != null) {
                            Marker(state = rememberMarkerState(position = LatLng(endLoc.lat!!, endLoc.lng!!)), title = "Fim: ${leg.endAddress ?: ""}")
                        }
                    }
                    leg.steps?.forEach { step: Step ->
                        step.polyline?.points?.let { stepEncodedPolyline: String ->
                            val stepPath: List<LatLng> = decodePolyline(stepEncodedPolyline)
                            if (stepPath.isNotEmpty()) {
                                val pattern: List<PatternItem>? = if (step.travelMode == "WALKING") listOf(Dash(20f), Gap(10f)) else null
                                Polyline(
                                    points = stepPath,
                                    color = if (step.travelMode == "WALKING") Color.DarkGray else Color(0xFFE57373),
                                    width = 7f, pattern = pattern, zIndex = 0.5f
                                )
                            }
                        }
                        if (step.travelMode == "TRANSIT") {
                            step.transitDetails?.departureStop?.location?.let { depLoc: LocationPoint ->
                                if (depLoc.lat != null && depLoc.lng != null) {
                                    Marker(
                                        state = rememberMarkerState(position = LatLng(depLoc.lat!!, depLoc.lng!!)),
                                        title = "Embarque: ${step.transitDetails.departureStop?.name ?: "Paragem"}",
                                        snippet = "Linha: ${step.transitDetails.line?.shortName ?: step.transitDetails.line?.name ?: ""}"
                                    )
                                }
                            }
                            step.transitDetails?.arrivalStop?.location?.let { arrLoc: LocationPoint ->
                                if (arrLoc.lat != null && arrLoc.lng != null) {
                                    Marker(
                                        state = rememberMarkerState(position = LatLng(arrLoc.lat!!, arrLoc.lng!!)),
                                        title = "Desembarque: ${step.transitDetails.arrivalStop?.name ?: "Paragem"}",
                                        snippet = "Linha: ${step.transitDetails.line?.shortName ?: step.transitDetails.line?.name ?: ""}"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // UI sobreposta ao mapa
        Column(modifier = Modifier.fillMaxSize()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp), // Padding para o Card de inputs
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)) // Fundo semi-transparente
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = originText,
                        onValueChange = { originText = it; if(it.length <=2) originSuggestions = emptyList() },
                        label = { Text("Ponto de partida") },
                        modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) focusedFieldAuto = FocusedFieldAuto.ORIGIN else if(originSuggestions.isEmpty() && focusedFieldAuto == FocusedFieldAuto.ORIGIN) focusedFieldAuto = FocusedFieldAuto.NONE },
                        trailingIcon = if (originText.isNotEmpty()) { { IconButton(onClick = { originText = ""; originSuggestions = emptyList(); focusedFieldAuto = FocusedFieldAuto.NONE }) { Icon(Icons.Filled.Clear, "Limpar") } } } else null,
                        singleLine = true
                    )
                    AnimatedVisibility(visible = originSuggestions.isNotEmpty() && focusedFieldAuto == FocusedFieldAuto.ORIGIN) {
                        LazyColumn(modifier = Modifier.heightIn(max = 150.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.98f))) {
                            items(originSuggestions) { suggestion : PlacePrediction ->
                                Text( suggestion.description ?: "", modifier = Modifier.fillMaxWidth().clickable {
                                    originText = suggestion.description ?: ""; originSuggestions = emptyList(); focusedFieldAuto = FocusedFieldAuto.NONE; focusManager.clearFocus()
                                }.padding(16.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = destinationText,
                        onValueChange = { destinationText = it; if(it.length <=2) destinationSuggestions = emptyList() },
                        label = { Text("Ponto de destino") },
                        modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) focusedFieldAuto = FocusedFieldAuto.DESTINATION else if(destinationSuggestions.isEmpty() && focusedFieldAuto == FocusedFieldAuto.DESTINATION) focusedFieldAuto = FocusedFieldAuto.NONE },
                        trailingIcon = if (destinationText.isNotEmpty()) { { IconButton(onClick = { destinationText = ""; destinationSuggestions = emptyList(); focusedFieldAuto = FocusedFieldAuto.NONE }) { Icon(Icons.Filled.Clear, "Limpar") } } } else null,
                        singleLine = true
                    )
                    AnimatedVisibility(visible = destinationSuggestions.isNotEmpty() && focusedFieldAuto == FocusedFieldAuto.DESTINATION) {
                        LazyColumn(modifier = Modifier.heightIn(max = 150.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.98f))) {
                            items(destinationSuggestions) { suggestion: PlacePrediction ->
                                Text( suggestion.description ?: "", modifier = Modifier.fillMaxWidth().clickable {
                                    destinationText = suggestion.description ?: ""; destinationSuggestions = emptyList(); focusedFieldAuto = FocusedFieldAuto.NONE; focusManager.clearFocus()
                                }.padding(16.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (originText.isNotBlank() && destinationText.isNotBlank()) {
                                isLoadingDirections = true; directionsErrorMessage = null; selectedRoute = null; routesList = emptyList(); focusManager.clearFocus()
                                coroutineScope.launch {
                                    val result: Result<List<com.example.aveirobus.data.Route>> = fetchGoogleDirections(originText, destinationText)
                                    isLoadingDirections = false
                                    if (result.isSuccess) {
                                        val fetchedRoutes: List<com.example.aveirobus.data.Route> = result.getOrNull() ?: emptyList()
                                        routesList = fetchedRoutes
                                        if (fetchedRoutes.isEmpty()) directionsErrorMessage = "Nenhuma rota de autocarro encontrada."
                                        else { /* Lógica de animar câmara para a primeira rota */ }
                                    } else {
                                        val exception: Throwable? = result.exceptionOrNull()
                                        directionsErrorMessage = "Erro: ${exception?.message ?: "Desconhecido"}"
                                    }
                                }
                            } else directionsErrorMessage = "Preencha a origem e o destino."
                        },
                        modifier = Modifier.fillMaxWidth(), enabled = !isLoadingDirections && !isLoadingAutocomplete
                    ) {
                        if (isLoadingDirections) CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        else Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Search, "Buscar"); Spacer(Modifier.width(8.dp)); Text("Buscar Rotas") }
                    }
                }
            }

            if (isLoadingAutocomplete) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(horizontal=16.dp, vertical = 4.dp))
            directionsErrorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
            Spacer(modifier = Modifier.height(8.dp))

            // Box para a lista de rotas ou detalhes, para que possa ter um fundo e scroll sobre o mapa
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (selectedRoute == null && routesList.isNotEmpty() && !isLoadingDirections) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.90f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)).padding(top = 8.dp)
                    ) {
                        item { Text("Rotas encontradas:", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp, start = 8.dp, end = 8.dp)) }
                        items(items = routesList, key = { route -> route.hashCode() }) { route : com.example.aveirobus.data.Route ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 8.dp).clickable { selectedRoute = route /* ... (lógica de animar câmara ao selecionar rota) ... */ },
                                elevation = CardDefaults.cardElevation(4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Sumário: ${route.summary ?: "Rota"}", style = MaterialTheme.typography.bodyLarge)
                                    val firstLeg: Leg? = route.legs?.firstOrNull()
                                    Text("Duração: ${firstLeg?.duration?.text ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                                    Text("Distância: ${firstLeg?.distance?.text ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) } // Espaço para o FAB não sobrepor
                    }
                } else if (selectedRoute != null) {
                    val currentRoute = selectedRoute!!
                    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.90f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)).padding(top = 8.dp)) {
                        Text("Itinerário Detalhado:", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 24.dp, end = 16.dp, bottom = 8.dp))
                        LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                            currentRoute.legs?.forEachIndexed { legIndex: Int, leg: Leg ->
                                item(key = "leg_header_$legIndex") { Text("Parte ${legIndex + 1}: De ${leg.startAddress ?: "Início"} para ${leg.endAddress ?: "Fim"}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp)) }
                                items(items = leg.steps ?: emptyList(), key = { step -> "step_${legIndex}_${step.hashCode()}" }) { step: Step ->
                                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 8.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text(step.htmlInstructions?.replace(Regex("<.*?>"), " ")?.replace("\\s+".toRegex(), " ")?.trim() ?: "Instrução", style = MaterialTheme.typography.bodyMedium)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()){
                                                Text("Duração: ${step.duration?.text ?: "N/A"}", style = MaterialTheme.typography.bodySmall)
                                                Text("Distância: ${step.distance?.text ?: "N/A"}", style = MaterialTheme.typography.bodySmall)
                                            }
                                            Text("Modo: ${step.travelMode ?: "N/A"}", style = MaterialTheme.typography.bodySmall)
                                            if (step.travelMode == "TRANSIT") {
                                                step.transitDetails?.let { transit: TransitDetails ->
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    val lineName = transit.line?.shortName ?: transit.line?.name ?: "N/A"
                                                    Text("Autocarro: Linha $lineName", style = MaterialTheme.typography.bodySmall)
                                                    Text("Veículo: ${transit.line?.vehicle?.name ?: "N/A"} (${transit.line?.vehicle?.type ?: ""})", style = MaterialTheme.typography.bodySmall)
                                                    Text("De: ${transit.departureStop?.name ?: "N/A"} (${transit.departureTime?.text ?: ""})", style = MaterialTheme.typography.bodySmall)
                                                    Text("Para: ${transit.arrivalStop?.name ?: "N/A"} (${transit.arrivalTime?.text ?: ""})", style = MaterialTheme.typography.bodySmall)
                                                    transit.numStops?.let { numStops -> Text("Paragens: $numStops", style = MaterialTheme.typography.bodySmall) }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            item(key = "itinerary_bottom_spacer_final") { Spacer(modifier = Modifier.height(16.dp)) }
                        }
                        Button(onClick = { selectedRoute = null }, modifier = Modifier.padding(16.dp).fillMaxWidth()) { Text("Ver outras rotas") }
                    }
                }
            } // Fim da Box para listas
            // O Spacer para o FAB foi movido para dentro da LazyColumn da lista de rotas,
            // ou pode ser tratado pelo padding da BottomBar no Scaffold.
        }

        // FAB para centrar na localização do utilizador
        FloatingActionButton(
            onClick = { requestLocationPermissionsAndCenter() },
            modifier = Modifier
                .align(Alignment.BottomStart) // Canto inferior esquerdo
                .padding(16.dp), // Padding para o FAB
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ) {
            Icon(Icons.Filled.MyLocation, contentDescription = "Centrar na minha localização")
        }
    }
}

// ----------------- FUNÇÕES AUXILIARES ABAIXO -----------------

fun decodePolyline(encoded: String?): List<LatLng> {
    if (encoded.isNullOrEmpty()) {
        Log.w("decodePolyline", "Input encoded polyline is null or empty.")
        return emptyList()
    }
    val poly = ArrayList<LatLng>()
    var index = 0
    val len = encoded.length
    var lat = 0
    var lng = 0
    try {
        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                if (index >= len) break
                val charCode = encoded[index++].code
                b = if (charCode >= 63) charCode - 63 else 0
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20 && index < len)
            val dlat = if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)
            lat += dlat
            shift = 0
            result = 0
            do {
                if (index >= len) break
                val charCode = encoded[index++].code
                b = if (charCode >= 63) charCode - 63 else 0
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20 && index < len)
            val dlng = if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)
            lng += dlng
            poly.add(LatLng(lat.toDouble() / 1E5, lng.toDouble() / 1E5))
        }
    } catch (e: Exception) {
        Log.e("decodePolyline", "Error decoding polyline: ${e.message}", e)
        return emptyList()
    }
    return poly
}

suspend fun fetchPlaceAutocompleteSuggestions(input: String): Result<List<PlacePrediction>> {
    val apiKey = GOOGLE_API_KEY
    if (apiKey.isEmpty()) {
        Log.e("Autocomplete", "API Key não configurada para Places Autocomplete.")
        return Result.failure(IOException("Chave de API não configurada."))
    }

    val client = OkHttpClient()
    // Alterado o parâmetro 'types' para focar em estabelecimentos
    val url = "https://maps.googleapis.com/maps/api/place/autocomplete/json?input=$input&language=pt-PT&components=country:PT&locationbias=circle:$AUTOCOMPLETE_RADIUS_METERS@${AVEIRO_LOCATION_CENTER.latitude},${AVEIRO_LOCATION_CENTER.longitude}&types=establishment&key=$apiKey"

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
            val gson = Gson()
            val apiResponse: PlacesAutocompleteResponse = gson.fromJson(responseBody, PlacesAutocompleteResponse::class.java)
            if (apiResponse.status == "OK" || apiResponse.status == "ZERO_RESULTS") {
                Result.success(apiResponse.predictions ?: emptyList())
            } else {
                Log.e("Autocomplete", "API Status not OK: ${apiResponse.status}, Error: ${apiResponse.errorMessage}")
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

suspend fun fetchGoogleDirections(origin: String, destination: String): Result<List<com.example.aveirobus.data.Route>> {
    val apiKey = GOOGLE_API_KEY
    if (apiKey.isEmpty()) {
        Log.e("fetchGoogleDirections", "API Key não configurada corretamente.")
        return Result.failure(IOException("Chave de API do Google Maps não configurada."))
    }

    val client = OkHttpClient()
    val url = "https://maps.googleapis.com/maps/api/directions/json?origin=$origin&destination=$destination&mode=transit&language=pt-PT&alternatives=true&key=$apiKey"

    Log.d("fetchGoogleDirections", "Requesting URL: $url")
    val request = Request.Builder().url(url).build()

    return try {
        val responseBody: String? = withContext(Dispatchers.IO) {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorBody = response.body?.string()
                Log.e("fetchGoogleDirections", "API Error ${response.code}: $errorBody")
                throw IOException("Erro na API Directions: ${response.code} - ${response.message}. Detalhes: $errorBody")
            }
            response.body?.string()
        }
        if (responseBody != null) {
            val gson = Gson()
            val apiResponse: com.example.aveirobus.data.DirectionsApiResponse = gson.fromJson(responseBody, com.example.aveirobus.data.DirectionsApiResponse::class.java)

            if (apiResponse.status == "OK") {
                Result.success(apiResponse.routes ?: emptyList<com.example.aveirobus.data.Route>())
            } else {
                val errorDetail = apiResponse.errorMessage ?: "Detalhes não disponíveis"
                Log.e("fetchGoogleDirections", "API Status not OK: ${apiResponse.status}, Error: $errorDetail")
                Result.failure(IOException("Erro da API Directions: ${apiResponse.status} - $errorDetail"))
            }
        } else {
            Log.e("fetchGoogleDirections", "Response body is null")
            Result.failure(IOException("Resposta da API vazia."))
        }
    } catch (e: Exception) {
        Log.e("fetchGoogleDirections", "Exception during fetch or parse: ${e.message}", e)
        Result.failure(IOException("Falha ao buscar ou processar rotas: ${e.message}", e))
    }
}

@Preview(showBackground = true, locale = "pt")
@Composable
fun RouteSearchScreenPreview() {
    MaterialTheme {
        RouteSearchScreen()
    }
}
