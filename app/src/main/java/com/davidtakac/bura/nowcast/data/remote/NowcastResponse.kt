package com.davidtakac.bura.nowcast.data.remote

import kotlinx.serialization.Serializable

/**
 * Data class representing the overall response structure from the nowcast API endpoint,
 * focusing on the radar data for the graph.
 */
@Serializable
data class NowcastResponse(
    val location: String?,
    val latitude: Double?,
    val longitude: Double?,
    val currentTimeLocal: String?, // Using String initially, can be converted to Instant/LocalDateTime later
    val radarData: List<NowcastDataPoint>?
    // Removed 'accumulation' and 'currentWeather' based on user request
)
