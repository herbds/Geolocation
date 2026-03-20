package com.project.geolocation.network

import kotlinx.serialization.Serializable

@Serializable
data class DestinationResponse(
    val success: Boolean,
    val user_id: String,
    val destinations: List<Destination>,
    val count: Int
)

@Serializable
data class Destination(
    val id: Int,
    val latitude: Double,
    val longitude: Double,
    val status: String,
    val created_at: String,
    val order_index: Int = 0,
    val building_name: String = "",
    val route_id: Int = 0
)

data class PendingDestination(
    val latitude: Double,
    val longitude: Double,
    val timestamp: String,
    val source: String,
    val buildingName: String = ""
)