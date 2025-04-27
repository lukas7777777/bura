package com.davidtakac.bura.nowcast.data.remote

import kotlinx.serialization.Serializable

/**
 * Data class representing a single data point in the radarData list.
 */
@Serializable
data class NowcastDataPoint(
    val timestamp: Long?, // Timestamp in milliseconds since epoch
    val pastRate: Double?, // Precipitation rate for past data
    val futureAvg1km: Double?, // Average precipitation rate for future (1km radius)
    val futureMinMax1km: List<Double>?, // Min/Max precipitation rate range for future (1km radius)
    val futureAvg6km: Double?, // Average precipitation rate for future (6km radius)
    val futureMinMax6km: List<Double>? // Min/Max precipitation rate range for future (6km radius)
)
