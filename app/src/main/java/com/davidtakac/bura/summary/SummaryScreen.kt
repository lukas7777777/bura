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

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.davidtakac.bura.R
import com.davidtakac.bura.common.FailedToDownloadErrorScreen
import com.davidtakac.bura.common.NoSelectedPlaceErrorScreen
import com.davidtakac.bura.common.OutdatedErrorScreen
import com.davidtakac.bura.common.animateShimmerColorAsState
import com.davidtakac.bura.graphs.nowcast.NowcastGraph
import com.davidtakac.bura.place.Place
import com.davidtakac.bura.place.picker.PlacePickerSearchBar
import com.davidtakac.bura.place.picker.PlacePickerState
import com.davidtakac.bura.summary.daily.DailySummaryColumn
import com.davidtakac.bura.summary.daily.DailySummaryColumnSkeleton
import com.davidtakac.bura.summary.feelslike.FeelsLikeSummary
import com.davidtakac.bura.summary.hourly.HourSummaryLazyRow
import com.davidtakac.bura.summary.hourly.HourSummaryLazyRowSkeleton
import com.davidtakac.bura.summary.humidity.HumiditySummary
import com.davidtakac.bura.summary.now.NowSummary
import com.davidtakac.bura.summary.now.NowSummarySkeleton
import com.davidtakac.bura.summary.precipitation.PrecipitationSummary
import com.davidtakac.bura.summary.pressure.PressureSummary
import com.davidtakac.bura.summary.sun.SunSummary
import com.davidtakac.bura.summary.uvindex.UvIndexSummary
import com.davidtakac.bura.summary.visibility.VisibilitySummary
import com.davidtakac.bura.summary.wind.WindSummary
import java.time.LocalDate

@Composable
fun SummaryScreen(
    summaryState: SummaryState,
    nowcastState: NowcastUiState, // Added nowcast state parameter
    onHourlySectionClick: () -> Unit,
    onDayClick: (date: LocalDate) -> Unit,
    onSettingsButtonClick: () -> Unit,
    onPrecipitationClick: () -> Unit,

    pickerState: PlacePickerState,
    searchQuery: String,
    onSearchQueryChange: (query: String) -> Unit,
    onSearchQueryClearClick: () -> Unit,
    searchActive: Boolean,
    onSearchActiveChange: (Boolean) -> Unit,
    onSearch: (query: String) -> Unit,
    onPlaceClick: (Place) -> Unit,
    onPlaceDeleteClick: (Place) -> Unit,

    onTryAgainClick: () -> Unit,
    onSelectPlaceClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            PlacePickerSearchBar(
                state = pickerState,
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                onQueryClearClick = onSearchQueryClearClick,
                onSearchClick = onSearch,
                onPlaceClick = onPlaceClick,
                onPlaceDeleteClick = onPlaceDeleteClick,
                active = searchActive,
                onActiveChange = onSearchActiveChange,
                onSettingsClick = onSettingsButtonClick
            )
        }    ) { contentPadding ->
        Crossfade(
            targetState = summaryState,
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(top = 8.dp),
            label = "Grid crossfade"
        ) {
            when (it) {
                is SummaryState.Success -> SummaryGrid(
                    state = it,
                    nowcastState = nowcastState, // Pass nowcast state to SummaryGrid
                    onHourlyClick = onHourlySectionClick,
                    onDayClick = onDayClick,
                    onPrecipitationClick = onPrecipitationClick,
                    modifier = Modifier.fillMaxSize()
                )

                SummaryState.Loading -> SummaryScreenSkeleton(
                    modifier = Modifier.fillMaxSize()
                )

                SummaryState.FailedToDownload -> FailedToDownloadErrorScreen(
                    modifier = Modifier.fillMaxSize(),
                    onTryAgainClick = onTryAgainClick
                )

                SummaryState.Outdated -> OutdatedErrorScreen(
                    modifier = Modifier.fillMaxSize(),
                    onTryAgainClick = onTryAgainClick
                )

                SummaryState.NoSelectedPlace -> NoSelectedPlaceErrorScreen(
                    onSelectPlaceClick = onSelectPlaceClick,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun SummaryGrid(
    state: SummaryState.Success,
    nowcastState: NowcastUiState, // Add nowcast state parameter
    onHourlyClick: () -> Unit,
    onDayClick: (date: LocalDate) -> Unit,
    onPrecipitationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalStaggeredGrid(
        // Setting the horizontal padding to 16dp and adding empty FullLine items at the top
        // and bottom of the grid is a workaround for contentPadding. For some reason, setting it
        // causes the grid to crash with a 'position() should be called first' exception.
        modifier = modifier.padding(horizontal = 16.dp),
        verticalItemSpacing = 16.dp,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        columns = StaggeredGridCells.Fixed(2)
    ) {
        item(span = StaggeredGridItemSpan.FullLine) {}
        item(span = StaggeredGridItemSpan.FullLine) {
            NowSummary(
                state = state.now,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item(span = StaggeredGridItemSpan.FullLine) {
            HourSummaryLazyRow(
                state = state.hourly,
                onClick = onHourlyClick,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item(span = StaggeredGridItemSpan.FullLine) {
            DailySummaryColumn(
                state = state.daily,
                onDayClick = onDayClick,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // Add Nowcast Graph - This will show precipitation radar data
        item(span = StaggeredGridItemSpan.FullLine) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.nowcast_title), // You'll need to add this string resource
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.align(Alignment.TopStart)
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp), // Space for the title
                        contentAlignment = Alignment.Center
                    ) {

                        when(nowcastState) {
                            is NowcastUiState.Loading -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.align(Alignment.Center),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            is NowcastUiState.Success -> {
                                NowcastGraph(
                                    nowcastData = nowcastState.data,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                )
                            }
                            is NowcastUiState.Error -> {
                                Text(
                                    text = nowcastState.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
        item {
            PrecipitationSummary(
                state = state.precip,
                onClick = onPrecipitationClick,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            UvIndexSummary(
                state = state.uvIndex,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            WindSummary(
                state = state.wind,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            PressureSummary(
                state = state.pressure,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            HumiditySummary(
                state = state.humidity,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            VisibilitySummary(
                state = state.vis,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            SunSummary(
                state = state.sun,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            FeelsLikeSummary(
                state = state.feelsLike,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item(span = StaggeredGridItemSpan.FullLine) {
            Text(
                text = stringResource(id = R.string.credit_weather),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item(span = StaggeredGridItemSpan.FullLine) {}
    }
}

@Composable
private fun SummaryScreenSkeleton(modifier: Modifier = Modifier) {
    val shimmerColor = animateShimmerColorAsState()
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            NowSummarySkeleton(
                color = shimmerColor,
                withDate = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            HourSummaryLazyRowSkeleton(
                color = shimmerColor,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            DailySummaryColumnSkeleton(
                color = shimmerColor,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}