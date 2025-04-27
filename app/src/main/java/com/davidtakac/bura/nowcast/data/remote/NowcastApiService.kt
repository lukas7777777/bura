package com.davidtakac.bura.nowcast.data.remote

import com.davidtakac.bura.common.UserAgentProvider // Assuming UserAgentProvider is available
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * Service class to fetch nowcast data from the custom API using HttpsURLConnection and org.json.
 */
class NowcastApiService(
    private val userAgentProvider: UserAgentProvider // Dependency for User-Agent header
) {

    private val API_BASE_URL = "https://weather.rufdo.de/api/nowcast" // Your custom API endpoint
    private val CONNECT_TIMEOUT_MS = 10_000 // 10 seconds
    private val READ_TIMEOUT_MS = 10_000 // 10 seconds

    /**
     * Fetches nowcast data for the given coordinates.
     * @param latitude The latitude of the location.
     * @param longitude The longitude of the location.
     * @return A [NowcastResponse] object if successful, or null if an error occurred.
     */
    suspend fun fetchNowcastData(latitude: Double, longitude: Double): NowcastResponse? =
        withContext(Dispatchers.IO) {
            val urlString = "$API_BASE_URL?lat=$latitude&lon=$longitude"
            val url = try {
                URL(urlString)
            } catch (e: Exception) {
                // Log the error or handle it appropriately
                e.printStackTrace()
                return@withContext null
            }

            val conn = try {
                url.openConnection() as HttpsURLConnection
            } catch (e: Exception) {
                // Log the error or handle it appropriately
                e.printStackTrace()
                return@withContext null
            }

            try {
                conn.requestMethod = "GET"
                conn.connectTimeout = CONNECT_TIMEOUT_MS
                conn.readTimeout = READ_TIMEOUT_MS
                // Reuse the app's User-Agent if available
                conn.setRequestProperty("User-Agent", userAgentProvider.userAgent)

                // Connect to the API
                conn.connect()

                // Check for successful HTTP response (status code 200)
                if (conn.responseCode != HttpsURLConnection.HTTP_OK) {
                    // Log the error response code
                    println("HTTP error code: ${conn.responseCode}")
                    return@withContext null
                }

                // Read the response body
                val jsonString = BufferedReader(InputStreamReader(conn.inputStream)).use(BufferedReader::readText)

                // Parse the JSON string using org.json
                parseNowcastResponse(jsonString)

            } catch (e: Exception) {
                // Log any exceptions during the network request or parsing
                e.printStackTrace()
                null
            } finally {
                // Disconnect the connection
                conn.disconnect()
            }
        }

    /**
     * Parses the JSON string into a [NowcastResponse] data class using org.json.
     * @param jsonString The JSON string to parse.
     * @return A [NowcastResponse] object if parsing is successful, or null otherwise.
     */
    private fun parseNowcastResponse(jsonString: String): NowcastResponse? {
        return try {
            val jsonObject = JSONObject(jsonString)

            val location = jsonObject.optString("location", null)
            val latitude = if (jsonObject.has("latitude")) jsonObject.optDouble("latitude") else null
            val longitude = if (jsonObject.has("longitude")) jsonObject.optDouble("longitude") else null
            val currentTimeLocal = jsonObject.optString("currentTimeLocal", null)

            val radarDataArray = jsonObject.optJSONArray("radarData")
            val radarDataList = radarDataArray?.let {
                val list = mutableListOf<NowcastDataPoint>()
                for (i in 0 until it.length()) {
                    val dataPointJson = it.optJSONObject(i)
                    if (dataPointJson != null) {
                        val timestamp = if (dataPointJson.has("timestamp")) dataPointJson.optLong("timestamp") else null
                        val pastRate = if (dataPointJson.has("pastRate")) dataPointJson.optDouble("pastRate") else null
                        val futureAvg1km = if (dataPointJson.has("futureAvg1km")) dataPointJson.optDouble("futureAvg1km") else null
                        val futureMinMax1kmArray = dataPointJson.optJSONArray("futureMinMax1km")
                        val futureMinMax1km = futureMinMax1kmArray?.let { arr ->
                            if (arr.length() == 2) listOf(arr.optDouble(0), arr.optDouble(1)) else null
                        }
                        val futureAvg6km = if (dataPointJson.has("futureAvg6km")) dataPointJson.optDouble("futureAvg6km") else null
                        val futureMinMax6kmArray = dataPointJson.optJSONArray("futureMinMax6km")
                        val futureMinMax6km = futureMinMax6kmArray?.let { arr ->
                            if (arr.length() == 2) listOf(arr.optDouble(0), arr.optDouble(1)) else null
                        }

                        list.add(
                            NowcastDataPoint(
                                timestamp = timestamp,
                                pastRate = pastRate,
                                futureAvg1km = futureAvg1km,
                                futureMinMax1km = futureMinMax1km,
                                futureAvg6km = futureAvg6km,
                                futureMinMax6km = futureMinMax6km
                            )
                        )
                    }
                }
                list
            }

            NowcastResponse(
                location = location,
                latitude = latitude,
                longitude = longitude,
                currentTimeLocal = currentTimeLocal,
                radarData = radarDataList
            )

        } catch (e: Exception) {
            // Log any JSON parsing errors
            e.printStackTrace()
            null
        }
    }
}
