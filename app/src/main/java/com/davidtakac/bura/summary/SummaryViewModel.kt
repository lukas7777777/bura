/*
 * Copyright 2024 David Takač
 *
 * This file is part of Bura.
 *
 * Bura is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * Bura is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Bura. If not, see <https://www.gnu.org/licenses/>.
 */

package com.davidtakac.bura.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.davidtakac.bura.App
import com.davidtakac.bura.forecast.ForecastRepository
import com.davidtakac.bura.forecast.ForecastResult
import com.davidtakac.bura.nowcast.data.NowcastRepository
import com.davidtakac.bura.nowcast.data.NowcastResult
import com.davidtakac.bura.nowcast.data.remote.NowcastResponse
import com.davidtakac.bura.place.Location
import com.davidtakac.bura.place.selected.SelectedPlaceRepository
import com.davidtakac.bura.summary.daily.DailySummary
import com.davidtakac.bura.summary.daily.getDailySummary
import com.davidtakac.bura.summary.feelslike.FeelsLikeSummary
import com.davidtakac.bura.summary.feelslike.getFeelsLikeSummary
import com.davidtakac.bura.summary.hourly.HourSummary
import com.davidtakac.bura.summary.hourly.getHourlySummary
import com.davidtakac.bura.summary.humidity.HumiditySummary
import com.davidtakac.bura.summary.humidity.getHumiditySummary
import com.davidtakac.bura.summary.now.NowSummary
import com.davidtakac.bura.summary.now.getNowSummary
import com.davidtakac.bura.summary.precipitation.PrecipitationSummary
import com.davidtakac.bura.summary.precipitation.getPrecipitationSummary
import com.davidtakac.bura.summary.pressure.PressureSummary
import com.davidtakac.bura.summary.pressure.getPressureSummary
import com.davidtakac.bura.summary.sun.SunSummary
import com.davidtakac.bura.summary.sun.getSunSummary
import com.davidtakac.bura.summary.uvindex.UvIndexSummary
import com.davidtakac.bura.summary.uvindex.getUvIndexSummary
import com.davidtakac.bura.summary.visibility.VisibilitySummary
import com.davidtakac.bura.summary.visibility.getVisibilitySummary
import com.davidtakac.bura.summary.wind.WindSummary
import com.davidtakac.bura.summary.wind.getWindSummary
import com.davidtakac.bura.units.SelectedUnitsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant

class SummaryViewModel(
    private val placeRepo: SelectedPlaceRepository,
    private val unitsRepo: SelectedUnitsRepository,
    private val forecastRepo: ForecastRepository,
    private val nowcastRepository: NowcastRepository
) : ViewModel() {
    private val _state = MutableStateFlow<SummaryState>(SummaryState.Loading)
    val state = _state.asStateFlow()
      // StateFlow for nowcast data
    private val _nowcastDataState = MutableStateFlow<NowcastUiState>(NowcastUiState.Loading)
    val nowcastDataState = _nowcastDataState.asStateFlow()
    
    init {
        // Fetch data when the ViewModel is created
        getSummary()
    }

    fun getSummary() {
        viewModelScope.launch {
            if (_state.value !is SummaryState.Success) {
                _state.value = SummaryState.Loading
            }
            val location = placeRepo.getSelectedPlace()?.location
            if (location != null) {
                _state.value = getState()
                fetchNowcastData(location)
            } else {
                _state.value = SummaryState.NoSelectedPlace
                _nowcastDataState.value = NowcastUiState.Error("No place selected")
            }
        }
    }
    
    private suspend fun getState(): SummaryState {
        val location = placeRepo.getSelectedPlace()?.location ?: return SummaryState.NoSelectedPlace
        val coords = location.coordinates
        val units = unitsRepo.getSelectedUnits()
        val now = Instant.now().atZone(location.timeZone).toLocalDateTime()
        val forecast = forecastRepo.forecast(coords, units) ?: return SummaryState.FailedToDownload

        val nowSummary = getNowSummary(now, tempPeriod = forecast.temperature, feelsPeriod = forecast.feelsLike, forecast.condition)
        when (nowSummary) {
            ForecastResult.FailedToDownload -> return SummaryState.FailedToDownload
            ForecastResult.Outdated -> return SummaryState.Outdated
            is ForecastResult.Success -> Unit
        }

        val hourlySummary = getHourlySummary(now, forecast.temperature, forecast.pop, forecast.condition, forecast.sun)
        when (hourlySummary) {
            ForecastResult.FailedToDownload -> return SummaryState.FailedToDownload
            ForecastResult.Outdated -> return SummaryState.Outdated
            is ForecastResult.Success -> Unit
        }

        val dailySummary = getDailySummary(now, forecast.temperature, forecast.condition, forecast.pop)
        when (dailySummary) {
            ForecastResult.FailedToDownload -> return SummaryState.FailedToDownload
            ForecastResult.Outdated -> return SummaryState.Outdated
            is ForecastResult.Success -> Unit
        }

        val precipSummary = getPrecipitationSummary(now, forecast.precipitation)
        when (precipSummary) {
            ForecastResult.FailedToDownload -> return SummaryState.FailedToDownload
            ForecastResult.Outdated -> return SummaryState.Outdated
            is ForecastResult.Success -> Unit
        }

        val uvIndexSummary = getUvIndexSummary(now, forecast.uvIndex)
        when (uvIndexSummary) {
            ForecastResult.FailedToDownload -> return SummaryState.FailedToDownload
            ForecastResult.Outdated -> return SummaryState.Outdated
            is ForecastResult.Success -> Unit
        }

        val windSummary = getWindSummary(now, forecast.wind, forecast.gust)
        when (windSummary) {
            ForecastResult.FailedToDownload -> return SummaryState.FailedToDownload
            ForecastResult.Outdated -> return SummaryState.Outdated
            is ForecastResult.Success -> Unit
        }

        val pressureSummary = getPressureSummary(now, forecast.pressure)
        when (pressureSummary) {
            ForecastResult.FailedToDownload -> return SummaryState.FailedToDownload
            ForecastResult.Outdated -> return SummaryState.Outdated
            is ForecastResult.Success -> Unit
        }

        val humiditySummary = getHumiditySummary(now, forecast.humidity, forecast.dewPoint)
        when (humiditySummary) {
            ForecastResult.FailedToDownload -> return SummaryState.FailedToDownload
            ForecastResult.Outdated -> return SummaryState.Outdated
            is ForecastResult.Success -> Unit
        }

        val visSummary = getVisibilitySummary(now, forecast.visibility)
        when (visSummary) {
            ForecastResult.FailedToDownload -> return SummaryState.FailedToDownload
            ForecastResult.Outdated -> return SummaryState.Outdated
            is ForecastResult.Success -> Unit
        }

        val sunSummary = getSunSummary(now, forecast.sun, forecast.condition)
        when (sunSummary) {
            ForecastResult.FailedToDownload -> return SummaryState.FailedToDownload
            ForecastResult.Outdated -> return SummaryState.Outdated
            is ForecastResult.Success -> Unit
        }

        val feelsLikeSummary = getFeelsLikeSummary(now, tempPeriod = forecast.temperature, feelsPeriod = forecast.feelsLike)
        when (feelsLikeSummary) {
            ForecastResult.FailedToDownload -> return SummaryState.FailedToDownload
            ForecastResult.Outdated -> return SummaryState.Outdated
            is ForecastResult.Success -> Unit
        }        // Include nowcast data from the nowcast state if available
        val currentNowcastData = (_nowcastDataState.value as? NowcastUiState.Success)?.data

        return SummaryState.Success(
            now = nowSummary.data,
            hourly = hourlySummary.data,
            daily = dailySummary.data,
            precip = precipSummary.data,
            uvIndex = uvIndexSummary.data,
            wind = windSummary.data,
            pressure = pressureSummary.data,
            humidity = humiditySummary.data,
            vis = visSummary.data,
            sun = sunSummary.data,
            feelsLike = feelsLikeSummary.data,
            // Add the nowcast data from the separate state flow
            // Note: A more robust state merging strategy might be needed for production
            nowcastData = currentNowcastData
        )
    }    private fun fetchNowcastData(location: Location) {
        viewModelScope.launch {
            _nowcastDataState.value = NowcastUiState.Loading // Set loading state for nowcast
            when (val result = nowcastRepository.getNowcastData(location.coordinates.latitude, location.coordinates.longitude)) {
                is NowcastResult.Success -> {
                    // Update the nowcast specific state with the fetched data
                    _nowcastDataState.value = NowcastUiState.Success(result.data)
                    
                    // Update the main state if it's currently in a Success state
                    val currentState = _state.value
                    if (currentState is SummaryState.Success) {
                        // Create a new Success state by copying the existing one and adding the nowcast data
                        _state.value = currentState.copy(nowcastData = result.data)
                    }
                    // Note: If the main state isn't a Success state, we don't update it
                    // The nowcast data will be included when the main state transitions to Success
                }
                is NowcastResult.Error -> {
                    _nowcastDataState.value = NowcastUiState.Error(result.message)
                    // We don't update the main state on error, keeping the nowcast part optional
                }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val container = (checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as App).container
                return SummaryViewModel(
                    container.selectedPlaceRepository,
                    container.selectedUnitsRepository,
                    container.forecastRepository,
                    container.nowcastRepository
                ) as T
            }
        }
    }
}

sealed interface SummaryState {
    /**
     * Represents the successful state with all weather summary data loaded.
     */
    data class Success(
        val now: NowSummary,
        val hourly: List<HourSummary>,
        val daily: DailySummary,
        val precip: PrecipitationSummary,
        val uvIndex: UvIndexSummary,
        val wind: WindSummary,
        val pressure: PressureSummary,
        val humidity: HumiditySummary,
        val vis: VisibilitySummary,
        val sun: SunSummary,
        val feelsLike: FeelsLikeSummary,
        // --- NEW: Add nowcast data ---
        val nowcastData: NowcastResponse? = null // Field to hold the nowcast data
    ) : SummaryState

    /**
     * Represents the loading state while data is being fetched.
     */
    data object Loading : SummaryState

    /**
     * Represents a state where data failed to download.
     */
    data object FailedToDownload : SummaryState

    /**
     * Represents a state where the data is outdated and needs refreshing.
     */
    data object Outdated : SummaryState

    /**
     * Represents a state where no place has been selected by the user.
     */
    data object NoSelectedPlace : SummaryState
}

/**
 * Sealed class to represent the UI state specifically for Nowcast data
 */
sealed class NowcastUiState {
    data object Loading : NowcastUiState()
    data class Success(val data: NowcastResponse) : NowcastUiState()
    data class Error(val message: String) : NowcastUiState()
}