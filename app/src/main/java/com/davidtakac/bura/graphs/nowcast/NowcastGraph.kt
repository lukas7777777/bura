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

package com.davidtakac.bura.graphs.nowcast

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.rememberTextMeasurer
import com.davidtakac.bura.graphs.common.GraphArgs
import com.davidtakac.bura.graphs.common.TimeMode
import com.davidtakac.bura.graphs.common.drawTimeAxis
import com.davidtakac.bura.graphs.common.drawVerticalAxis
import com.davidtakac.bura.nowcast.data.remote.NowcastResponse
import com.davidtakac.bura.nowcast.data.remote.NowcastDataPoint
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlin.math.pow
import kotlin.math.floor
import kotlin.math.log10
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas

/**
 * A graph component for displaying nowcast precipitation radar data.
 * This component visualizes both historical and forecasted precipitation data.
 * 
 * @param nowcastData The nowcast data containing radar information to display
 * @param modifier Modifier for styling and layout
 */
@Composable
fun NowcastGraph(
    nowcastData: NowcastResponse?,
    modifier: Modifier = Modifier
) {
    val args = GraphArgs.rememberPrecipitationArgs()
    val measurer = rememberTextMeasurer()
    val context = LocalContext.current
    val data = nowcastData?.radarData ?: emptyList()
    val hasData = data.isNotEmpty()
    val nowTimestamp = System.currentTimeMillis()
    val pastWindowMillis = 60 * 60 * 1000L // 60 min
    val futureWindowMillis = 120 * 60 * 1000L // 120 min
    val minTimestamp = nowTimestamp - pastWindowMillis
    val maxTimestamp = nowTimestamp + futureWindowMillis
    val timestamps = data.mapNotNull { it.timestamp }

    // Y-axis domain: 0 to max of all relevant values
    val allValues = data.flatMap {
        listOfNotNull(
            it.pastRate,
            it.futureAvg1km,
            it.futureAvg6km,
            it.futureMinMax1km?.getOrNull(0),
            it.futureMinMax1km?.getOrNull(1),
            it.futureMinMax6km?.getOrNull(0),
            it.futureMinMax6km?.getOrNull(1)
        )
    }
    val maxYRaw = allValues.maxOrNull()
    // Expand maxY to the next 'nice' round value (1, 2, 5, 10, 20, 50, 100, ...)
    fun roundUpNice(value: Double): Double {
        if (value <= 1.0) return 1.0
        val exponent = floor(log10(value)).toInt()
        val base = 10.0.pow(exponent)
        val mantissa = value / base
        val niceMantissa = when {
            mantissa <= 1.0 -> 1.0
            mantissa <= 2.0 -> 2.0
            mantissa <= 5.0 -> 5.0
            else -> 10.0
        }
        return niceMantissa * base
    }
    val maxY = roundUpNice(if (maxYRaw == null || maxYRaw.isNaN() || maxYRaw == 0.0) 1.0 else maxYRaw)

    // Assign surfaceColor here to avoid @Composable calls in Canvas
    val surfaceColor = androidx.compose.material3.MaterialTheme.colorScheme.surface

    androidx.compose.material3.Surface(
        color = surfaceColor,
        shape = androidx.compose.material3.MaterialTheme.shapes.large,
        modifier = modifier.padding(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
                // Fill background for Canvas
                drawRect(
                    color = surfaceColor,
                    size = size
                )
                val plotLeft = args.startGutter
                val plotRight = size.width - args.endGutter
                val plotTop = args.topGutter
                val plotBottom = size.height - args.bottomGutter
                val graphW = plotRight - plotLeft
                val graphH = plotBottom - plotTop

                // Helper functions
                fun getXForTimestamp(ts: Long): Float {
                    return if (maxTimestamp == minTimestamp) plotLeft
                    else plotLeft + ((ts - minTimestamp).toFloat() / (maxTimestamp - minTimestamp)) * graphW
                }
                fun getY(value: Double?): Float {
                    val unclamped = plotBottom - ((value ?: 0.0) / maxY * graphH).toFloat()
                    return unclamped.coerceIn(plotTop, plotBottom)
                }
                fun drawLineFor(key: (NowcastDataPoint) -> Double?, color: Color, stroke: Float = 3f, dashed: Boolean = false) {
                    val visibleData = data.filter { it.timestamp != null && it.timestamp in minTimestamp..maxTimestamp }
                    if (visibleData.isEmpty()) return
                    val path = androidx.compose.ui.graphics.Path()
                    var first = true
                    for (point in visibleData) {
                        val v = key(point)
                        if (v == null || v.isNaN()) continue
                        val x = getXForTimestamp(point.timestamp!!)
                        val y = getY(v)
                        if (first) {
                            path.moveTo(x, y)
                            first = false
                        } else {
                            path.lineTo(x, y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = color,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = stroke,
                            pathEffect = if (dashed) androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f)) else null
                        )
                    )
                }

                // Draw grid (dashed lines)
                val gridSteps = 5
                for (i in 0..gridSteps) {
                    val y = plotBottom - (i / gridSteps.toFloat()) * graphH
                    drawLine(
                        color = args.axisColor,
                        start = Offset(plotLeft, y),
                        end = Offset(plotRight, y),
                        strokeWidth = 1f,
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                    )
                }
                val xSteps = 6
                for (i in 0..xSteps) {
                    val x = plotLeft + (i / xSteps.toFloat()) * graphW
                    drawLine(
                        color = args.axisColor,
                        start = Offset(x, plotTop),
                        end = Offset(x, plotBottom),
                        strokeWidth = 1f,
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                    )
                }

                // Draw uncertainty areas (futureMinMax1km, futureMinMax6km)
                val area1kmPath = androidx.compose.ui.graphics.Path()
                val min1km = data.map { it.futureMinMax1km?.getOrNull(0) ?: Double.NaN }
                val max1km = data.map { it.futureMinMax1km?.getOrNull(1) ?: Double.NaN }
                if (min1km.any { !it.isNaN() } && max1km.any { !it.isNaN() }) {
                    if (data.isNotEmpty() && data[0].timestamp != null) {
                        area1kmPath.moveTo(getXForTimestamp(data[0].timestamp!!), getY(min1km[0]))
                        for (i in 1 until data.size) {
                            val ts = data[i].timestamp ?: continue
                            area1kmPath.lineTo(getXForTimestamp(ts), getY(min1km[i]))
                        }
                        for (i in (data.size - 1) downTo 0) {
                            val ts = data[i].timestamp ?: continue
                            area1kmPath.lineTo(getXForTimestamp(ts), getY(max1km[i]))
                        }
                    }
                    area1kmPath.close()
                    drawPath(
                        path = area1kmPath,
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color(0xFF4ade80).copy(alpha = 0.4f), Color(0xFF4ade80).copy(alpha = 0.1f))
                        ),
                        alpha = 1f
                    )
                }
                val area6kmPath = androidx.compose.ui.graphics.Path()
                val min6km = data.map { it.futureMinMax6km?.getOrNull(0) ?: Double.NaN }
                val max6km = data.map { it.futureMinMax6km?.getOrNull(1) ?: Double.NaN }
                if (min6km.any { !it.isNaN() } && max6km.any { !it.isNaN() }) {
                    if (data.isNotEmpty() && data[0].timestamp != null) {
                        area6kmPath.moveTo(getXForTimestamp(data[0].timestamp!!), getY(min6km[0]))
                        for (i in 1 until data.size) {
                            val ts = data[i].timestamp ?: continue
                            area6kmPath.lineTo(getXForTimestamp(ts), getY(min6km[i]))
                        }
                        for (i in (data.size - 1) downTo 0) {
                            val ts = data[i].timestamp ?: continue
                            area6kmPath.lineTo(getXForTimestamp(ts), getY(max6km[i]))
                        }
                    }
                    area6kmPath.close()
                    drawPath(
                        path = area6kmPath,
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color(0xFFfbbf24).copy(alpha = 0.4f), Color(0xFFfbbf24).copy(alpha = 0.1f))
                        ),
                        alpha = 1f
                    )
                }

                // Draw lines
                drawLineFor({ it.pastRate }, Color(0xFF2563eb), stroke = 4f)
                drawLineFor({ it.futureAvg1km }, Color(0xFF22c55e), stroke = 3f)
                drawLineFor({ it.futureAvg6km }, Color(0xFFf59e42), stroke = 3f, dashed = true)

                // Draw current time reference line
                if (nowTimestamp in minTimestamp..maxTimestamp) {
                    val rel = (nowTimestamp - minTimestamp).toFloat() / (maxTimestamp - minTimestamp).coerceAtLeast(1L)
                    val x = plotLeft + rel * graphW
                    drawLine(
                        color = Color.Red,
                        start = Offset(x, plotTop),
                        end = Offset(x, plotBottom),
                        strokeWidth = 2f,
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                    )
                }

                // Draw axes using utilities
                this.drawVerticalAxis(
                    steps = 5,
                    args = args,
                    measurer = measurer
                ) { step ->
                    val value = maxY * (step / 5.0)
                    if (value.isNaN()) "0.0 mm/h" else "%.1f mm/h".format(value)
                }
                val timeRangeMillis = maxTimestamp - minTimestamp
                val steps = when {
                    timeRangeMillis < 2 * 60 * 60 * 1000L -> 3 // <2h
                    timeRangeMillis < 6 * 60 * 60 * 1000L -> 4 // <6h
                    timeRangeMillis < 24 * 60 * 60 * 1000L -> 6 // <24h
                    else -> 8 // >24h
                }
                // Fix: Pass seconds-of-day for <24h, epoch millis for >=24h
                val isShortRange = timeRangeMillis < 24 * 60 * 60 * 1000L
                val zoneId = java.time.ZoneId.of("Europe/Berlin")
                val nowInstant = java.time.Instant.ofEpochMilli(nowTimestamp)
                val nowLocalDate = nowInstant.atZone(zoneId).toLocalDate()
                val minSecOfDay = if (isShortRange) java.time.Instant.ofEpochMilli(minTimestamp).atZone(zoneId).toLocalTime().toSecondOfDay().toLong() else minTimestamp
                val maxSecOfDay = if (isShortRange) java.time.Instant.ofEpochMilli(maxTimestamp).atZone(zoneId).toLocalTime().toSecondOfDay().toLong() else maxTimestamp
                this.drawTimeAxis(
                    measurer = measurer,
                    args = args,
                    steps = steps,
                    minTimestamp = minTimestamp,
                    maxTimestamp = maxTimestamp,
                    timeMode = TimeMode.EPOCH_MILLIS,
                    onStepDrawn = { _, _, _, _ -> }
                )
            }
            if (!hasData || data.all { it.timestamp == null || it.timestamp !in minTimestamp..maxTimestamp }) {
                androidx.compose.material3.Text(
                    text = "No data available",
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}



/**
 * Processes the raw nowcast data points for display on the graph.
 * 
 * @param dataPoints List of nowcast data points from the API
 * @return Processed data ready for visualization
 */
