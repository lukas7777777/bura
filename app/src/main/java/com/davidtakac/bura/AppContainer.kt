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

package com.davidtakac.bura

import android.content.Context
import com.davidtakac.bura.common.UserAgentProvider
import com.davidtakac.bura.forecast.ForecastConverter
import com.davidtakac.bura.forecast.ForecastDataCacher
import com.davidtakac.bura.forecast.ForecastDataDownloader
import com.davidtakac.bura.forecast.ForecastRepository
// import com.davidtakac.bura.forecast.ForecastRepositoryImpl
import com.davidtakac.bura.graphs.EssentialGraphsViewModel
import com.davidtakac.bura.nowcast.data.NowcastRepository
import com.davidtakac.bura.nowcast.data.NowcastRepositoryImpl
import com.davidtakac.bura.nowcast.data.remote.NowcastApiService
import com.davidtakac.bura.place.Location
import com.davidtakac.bura.place.Place
import com.davidtakac.bura.place.picker.PlacePickerViewModel
import com.davidtakac.bura.place.saved.DeletePlace
import com.davidtakac.bura.place.saved.GetSavedPlaces
import com.davidtakac.bura.place.saved.SavedPlace
import com.davidtakac.bura.place.saved.SavedPlacesRepository
import com.davidtakac.bura.place.search.SearchPlaces
import com.davidtakac.bura.place.selected.SelectPlace
import com.davidtakac.bura.place.selected.SelectedPlaceRepository
import com.davidtakac.bura.settings.SelectedUnitsViewModel
import com.davidtakac.bura.summary.SummaryViewModel
import com.davidtakac.bura.units.SelectedUnitsRepository
import com.davidtakac.bura.units.Units
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.time.Clock

/**
 * Dependency Injection container for the application.
 * Provides instances of repositories, services, and ViewModels.
 */
interface AppContainer {
    val prefs: android.content.SharedPreferences
    val forecastRepository: ForecastRepository
    val selectedPlaceRepository: SelectedPlaceRepository
    val selectedUnitsRepository: SelectedUnitsRepository
    val savedPlacesRepository: SavedPlacesRepository
    val searchPlaces: SearchPlaces
    val selectPlace: SelectPlace
    val deletePlace: DeletePlace
    val getSavedPlaces: GetSavedPlaces
    val summaryViewModel: SummaryViewModel
    val essentialGraphsViewModel: EssentialGraphsViewModel
    val placePickerViewModel: PlacePickerViewModel
    val selectedUnitsViewModel: SelectedUnitsViewModel
    val userAgentProvider: UserAgentProvider

    // --- NEW: Provide NowcastRepository ---
    val nowcastRepository: NowcastRepository
}

/**
 * Default implementation of [AppContainer].
 */
class DefaultAppContainer(private val context: Context) : AppContainer {
    override val prefs: android.content.SharedPreferences by lazy {
        context.getSharedPreferences("bura_prefs", Context.MODE_PRIVATE)
    }

    // Use a SupervisorJob for the application scope to allow child jobs to fail independently
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // --- Existing Dependencies ---
    private val forecastDataDownloader: ForecastDataDownloader by lazy {
        ForecastDataDownloader(userAgentProvider)
    }
    private val forecastDataCacher: ForecastDataCacher by lazy {
        ForecastDataCacher(context.filesDir)
    }
    private val forecastConverter: ForecastConverter by lazy {
        ForecastConverter()
    }

    override val forecastRepository: ForecastRepository by lazy {
        ForecastRepository(
            cacher = forecastDataCacher,
            downloader = forecastDataDownloader,
            converter = forecastConverter
        )
    }

    override val selectedPlaceRepository: SelectedPlaceRepository by lazy {
        SelectedPlaceRepository(
            context.getSharedPreferences("selected_place", Context.MODE_PRIVATE),
            savedPlacesRepository
        )
    }

    override val selectedUnitsRepository: SelectedUnitsRepository by lazy {
        SelectedUnitsRepository(context.getSharedPreferences("selected_units", Context.MODE_PRIVATE))
    }

    override val savedPlacesRepository: SavedPlacesRepository by lazy {
        SavedPlacesRepository(context.filesDir)
    }

    override val searchPlaces: SearchPlaces by lazy {
        SearchPlaces(userAgentProvider)
    }

    override val selectPlace: SelectPlace by lazy {
        SelectPlace(selectedPlaceRepository, savedPlacesRepository)
    }

    override val deletePlace: DeletePlace by lazy {
        DeletePlace(savedPlacesRepository, forecastDataCacher)
    }

    override val getSavedPlaces: GetSavedPlaces by lazy {
        GetSavedPlaces(
            selectedUnitsRepository,
            selectedPlaceRepository,
            savedPlacesRepository,
            forecastRepository
        )
    }

    override val userAgentProvider: UserAgentProvider by lazy {
        UserAgentProvider(context)
    }

    // --- NEW: Nowcast Dependencies ---
    private val nowcastApiService: NowcastApiService by lazy {
        NowcastApiService(userAgentProvider)
    }

    override val nowcastRepository: NowcastRepository by lazy {
        NowcastRepositoryImpl(nowcastApiService)
    }

    // --- ViewModel Factories ---
    // These factories will now have access to the nowcastRepository
    override val summaryViewModel: SummaryViewModel by lazy {
        SummaryViewModel(
            selectedPlaceRepository,
            selectedUnitsRepository,
            forecastRepository,
            nowcastRepository
        )
    }

    override val essentialGraphsViewModel: EssentialGraphsViewModel by lazy {
        EssentialGraphsViewModel(
            selectedPlaceRepository,
            selectedUnitsRepository,
            forecastRepository
        )
    }

    override val placePickerViewModel: PlacePickerViewModel by lazy {
        PlacePickerViewModel(
            selectedPlaceRepository,
            selectPlace,
            getSavedPlaces,
            searchPlaces,
            deletePlace
        )
    }

    override val selectedUnitsViewModel: SelectedUnitsViewModel by lazy {
        SelectedUnitsViewModel(selectedUnitsRepository)
    }
}