package com.example.aveirobus.data // Certifique-se de que este é o package correto

import com.google.gson.annotations.SerializedName

// --- Classes para Directions API ---

data class DirectionsApiResponse(
    val routes: List<Route>?,
    val status: String?,
    @SerializedName("error_message")
    val errorMessage: String?
)

data class Route(
    val summary: String?,
    val legs: List<Leg>?,
    @SerializedName("overview_polyline")
    val overviewPolyline: PolylineData?,
    val copyrights: String?,
    val warnings: List<String>?,
    val bounds: Viewport?
)

data class Leg(
    val steps: List<Step>?,
    val distance: ValueText?,
    val duration: ValueText?,
    @SerializedName("start_address")
    val startAddress: String?,
    @SerializedName("end_address")
    val endAddress: String?,
    @SerializedName("start_location")
    val startLocation: LocationPoint?,
    @SerializedName("end_location")
    val endLocation: LocationPoint?
)

data class Step(
    val distance: ValueText?,
    val duration: ValueText?,
    @SerializedName("html_instructions")
    val htmlInstructions: String?,
    val polyline: PolylineData?,
    @SerializedName("travel_mode")
    val travelMode: String?,
    @SerializedName("transit_details")
    val transitDetails: TransitDetails?
)

data class ValueText(
    val text: String?,
    val value: Long?
)

data class PolylineData(
    val points: String?
)

data class TransitDetails(
    @SerializedName("arrival_stop")
    val arrivalStop: StopPoint?,
    @SerializedName("departure_stop")
    val departureStop: StopPoint?,
    @SerializedName("arrival_time")
    val arrivalTime: TimeData?,
    @SerializedName("departure_time")
    val departureTime: TimeData?,
    val headsign: String?,
    val line: LineDetails?,
    @SerializedName("num_stops")
    val numStops: Int?
)

data class StopPoint(
    val location: LocationPoint?,
    val name: String?
)

data class TimeData(
    val text: String?,
    val value: Long?,
    @SerializedName("time_zone")
    val timeZone: String?
)

data class LineDetails(
    val name: String?,
    @SerializedName("short_name")
    val shortName: String?,
    val vehicle: VehicleDetails?
)

data class VehicleDetails(
    val name: String?,
    val type: String?,
    val icon: String?
)

data class LocationPoint(
    val lat: Double?,
    val lng: Double?
)

data class Viewport(
    val northeast: LocationPoint?,
    val southwest: LocationPoint?
)

// --- Novas Classes para Places Autocomplete API ---

data class PlacesAutocompleteResponse(
    val predictions: List<PlacePrediction>?,
    val status: String?,
    @SerializedName("error_message")
    val errorMessage: String?
)

data class PlacePrediction(
    val description: String?,
    @SerializedName("place_id")
    val placeId: String?,
    val reference: String?, // Deprecated
    val types: List<String>?,
    @SerializedName("matched_substrings")
    val matchedSubstrings: List<MatchedSubstring>?,
    val terms: List<Term>?,
    @SerializedName("structured_formatting")
    val structured_formatting: StructuredFormatting? // Nome corrigido para snake_case
)

data class MatchedSubstring(
    val length: Int?,
    val offset: Int?
)

data class Term(
    val offset: Int?,
    val value: String?
)

data class StructuredFormatting(
    @SerializedName("main_text")
    val mainText: String?,
    @SerializedName("secondary_text")
    val secondaryText: String?,
    @SerializedName("main_text_matched_substrings")
    val mainTextMatchedSubstrings: List<MatchedSubstring>?
)

data class Message(val text: String, val isUser: Boolean)

// Adicione a data class Message aqui se for usada em múltiplos ecrãs
// ou defina-a localmente nos ficheiros que a usam (AiChatScreen.kt).
// data class Message(val text: String, val isUser: Boolean)

