package com.example.aveirobus.data

import com.google.gson.annotations.SerializedName

data class DirectionsApiResponse(
    val routes: List<Route>?,
    val status: String
)

data class Route(
    val legs: List<Leg>?,
    val overview_polyline: Polyline?
)

data class Leg(
    val steps: List<Step>?,
    val distance: Distance?,
    val duration: Duration?,
    val start_address: String?,
    val end_address: String?
)

data class Step(
    val distance: Distance?,
    val duration: Duration?,
    val html_instructions: String?,
    val polyline: Polyline?,
    val travel_mode: String?, // e.g., "DRIVING", "TRANSIT", "WALKING"
    val transit_details: TransitDetails? // Relevant for transit steps
)

data class Distance(
    val text: String?,
    val value: Int?
)

data class Duration(
    val text: String?,
    val value: Int?
)

data class Polyline(
    val points: String?
)

data class TransitDetails(
    val arrival_stop: Stop?,
    val departure_stop: Stop?,
    val arrival_time: Time?,
    val departure_time: Time?,
    val headsign: String?,
    val line: Line?,
    val num_stops: Int?
)

data class Stop(
    val location: LatLng?,
    val name: String?
)

data class Time(
    val text: String?,
    val value: Long?,
    val time_zone: String?
)

data class Line(
    val name: String?,
    val short_name: String?,
    val vehicle: Vehicle?
    // Add more fields as needed, like color, text_color, agencies
)

data class Vehicle(
    val name: String?,
    val type: String? // e.g., "BUS"
    // Add more fields as needed, like icon, local_icon
)

data class LatLng(
    val lat: Double?,
    val lng: Double?
)