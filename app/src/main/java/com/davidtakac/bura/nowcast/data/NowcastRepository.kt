package com.davidtakac.bura.nowcast.data

import com.davidtakac.bura.nowcast.data.remote.NowcastApiService // Import the API service

/**
 * Repository interface for fetching nowcast data.
 * Abstracts the data source from the rest of the application.
 */
interface NowcastRepository {
    /**
     * Fetches nowcast data for the given latitude and longitude.
     * @param latitude The latitude of the location.
     * @param longitude The longitude of the location.
     * @return A [NowcastResult] indicating success or failure.
     */
    suspend fun getNowcastData(latitude: Double, longitude: Double): NowcastResult
}

/**
 * Implementation of the [NowcastRepository] that fetches data from [NowcastApiService].
 * @param apiService The API service responsible for making network requests.
 */
class NowcastRepositoryImpl(
    private val apiService: NowcastApiService
) : NowcastRepository {

    /**
     * Fetches nowcast data by calling the API service and wrapping the result in [NowcastResult].
     */
    override suspend fun getNowcastData(latitude: Double, longitude: Double): NowcastResult {
        return try {
            val response = apiService.fetchNowcastData(latitude, longitude)
            if (response != null) {
                NowcastResult.Success(response)
            } else {
                NowcastResult.Error("Failed to fetch nowcast data from API.")
            }
        } catch (e: Exception) {
            // Catch any unexpected exceptions during the process
            e.printStackTrace()
            NowcastResult.Error("An error occurred: ${e.message ?: "Unknown error"}")
        }
    }
}
