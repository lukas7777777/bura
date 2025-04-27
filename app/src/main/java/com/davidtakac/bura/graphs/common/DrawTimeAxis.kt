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

package com.davidtakac.bura.graphs.common

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.LayoutDirection
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.LocalTime

enum class TimeMode {
    SECONDS_OF_DAY, // 0-86399
    EPOCH_MILLIS
}

fun DrawScope.drawTimeAxis(
    measurer: TextMeasurer,
    args: GraphArgs,
    hourFormatter: (Int) -> String,
    onStepDrawn: (
        i: Int,
        x: Float,
        calculateY: (percent: Double) -> YData,
        label: String?
    ) -> Unit
) {
    // 24-hour axis, ticks at each hour, labels at 6-hour intervals
    val hourFormatterHH = java.time.format.DateTimeFormatter.ofPattern("HH")
    for (i in 0..24) {
        if (i % 6 == 0) {
            val xPercent = if (layoutDirection == LayoutDirection.Ltr) i / 24f else 1 - (i / 24f)
            val xOffset = if (layoutDirection == LayoutDirection.Ltr) args.startGutter else args.endGutter - args.startGutter
            val plotWidth = size.width - args.endGutter - args.startGutter
            val x = xPercent * plotWidth + xOffset
            fun drawTimeHelperLine() {
                drawLine(
                    color = args.axisColor,
                    start = Offset(x, y = 0f),
                    end = Offset(x, y = size.height),
                    strokeWidth = args.axisWidth,
                    pathEffect = null
                )
            }
            drawTimeHelperLine()
            val hour = if (i == 24) 0 else i
            val label = hourFormatterHH.format(java.time.LocalTime.of(hour, 0))

            val measuredLabel = measurer.measure(label, style = args.axisTextStyle)
            val textTopLeftX =
                if (layoutDirection == LayoutDirection.Ltr) x + args.bottomAxisTextPaddingHorizontal
                else x - measuredLabel.size.width - args.bottomAxisTextPaddingHorizontal
            val textTopLeftXMin =
                if (layoutDirection == LayoutDirection.Ltr) args.startGutter + args.bottomAxisTextPaddingHorizontal
                else args.endGutter + args.bottomAxisTextPaddingHorizontal
            val textTopLeftXMax =
                if (layoutDirection == LayoutDirection.Ltr) size.width - args.endGutter - measuredLabel.size.width - args.bottomAxisTextPaddingHorizontal
                else size.width - args.startGutter - measuredLabel.size.width - args.bottomAxisTextPaddingHorizontal
            drawText(
                textLayoutResult = measuredLabel,
                color = args.axisColor,
                topLeft = Offset(
                    x = textTopLeftX.coerceIn(
                        minimumValue = textTopLeftXMin,
                        maximumValue = textTopLeftXMax
                    ),
                    y = size.height - args.bottomGutter + args.bottomAxisTextPaddingTop
                )
            )
            onStepDrawn(i, x, { percent ->
                val percentFromBottom = 1 - percent
                val plotHeight = size.height - args.topGutter - args.bottomGutter
                val yOffset = args.topGutter
                YData(
                    top = ((percentFromBottom * plotHeight) + yOffset).toFloat(),
                    bot = size.height - args.bottomGutter
                )
            }, label)
        }
    }
}

// Explicit-ticks version retained for special cases
fun DrawScope.drawTimeAxis(
    measurer: TextMeasurer,
    args: GraphArgs,
    ticks: List<Long>,
    labels: List<String>,
    timeMode: TimeMode,
    onStepDrawn: (
        i: Int,
        x: Float,
        calculateY: (percent: Double) -> YData,
        label: String
    ) -> Unit
) {
    require(ticks.size == labels.size) { "Ticks and labels must have the same size" }
    val n = ticks.size
    val minTimestamp = ticks.first()
    val maxTimestamp = ticks.last()
    for (i in 0 until n) {
        val tickTs = ticks[i]
        val label = labels[i]
        val xPercent = if (layoutDirection == LayoutDirection.Ltr) (tickTs - minTimestamp).toFloat() / (maxTimestamp - minTimestamp).toFloat() else 1 - (tickTs - minTimestamp).toFloat() / (maxTimestamp - minTimestamp).toFloat()
        val xOffset = if (layoutDirection == LayoutDirection.Ltr) args.startGutter else args.endGutter - args.startGutter
        val plotWidth = size.width - args.endGutter - args.startGutter
        val x = xPercent * plotWidth + xOffset
        fun drawTimeHelperLine(onEdge: Boolean) {
            drawLine(
                color = args.axisColor,
                start = Offset(x, y = if (onEdge) 0f else args.topGutter),
                end = Offset(x, y = size.height),
                strokeWidth = args.axisWidth,
                pathEffect = if (!onEdge) PathEffect.dashPathEffect(args.axisDashIntervals.toFloatArray()) else null
            )
        }
        drawTimeHelperLine(onEdge = i == 0 || i == n - 1)
        val measuredLabel = measurer.measure(label, style = args.axisTextStyle)
        val textTopLeftX =
            if (layoutDirection == LayoutDirection.Ltr) x + args.bottomAxisTextPaddingHorizontal
            else x - measuredLabel.size.width - args.bottomAxisTextPaddingHorizontal
        val textTopLeftXMin =
            if (layoutDirection == LayoutDirection.Ltr) args.startGutter + args.bottomAxisTextPaddingHorizontal
            else args.endGutter + args.bottomAxisTextPaddingHorizontal
        val textTopLeftXMax =
            if (layoutDirection == LayoutDirection.Ltr) size.width - args.endGutter - measuredLabel.size.width - args.bottomAxisTextPaddingHorizontal
            else size.width - args.startGutter - measuredLabel.size.width - args.bottomAxisTextPaddingHorizontal
        drawText(
            textLayoutResult = measuredLabel,
            color = args.axisColor,
            topLeft = Offset(
                x = textTopLeftX.coerceIn(
                    minimumValue = textTopLeftXMin,
                    maximumValue = textTopLeftXMax
                ),
                y = size.height - args.bottomGutter + args.bottomAxisTextPaddingTop
            )
        )
        onStepDrawn(i, x, { percent ->
            val percentFromBottom = 1 - percent
            val plotHeight = size.height - args.topGutter - args.bottomGutter
            val yOffset = args.topGutter
            YData(
                top = ((percentFromBottom * plotHeight) + yOffset).toFloat(),
                bot = size.height - args.bottomGutter
            )
        }, label)
    }
}

// Original drawTimeAxis retained for other use cases
fun DrawScope.drawTimeAxis(
    measurer: TextMeasurer,
    args: GraphArgs,
    steps: Int,
    minTimestamp: Long,
    maxTimestamp: Long,
    timeMode: TimeMode,
    onStepDrawn: (
        i: Int,
        x: Float,
        calculateY: (percent: Double) -> YData,
        label: String
    ) -> Unit
) {
    val zoneId = java.time.ZoneId.of("Europe/Berlin")
    val dateFormatter = when (timeMode) {
        TimeMode.EPOCH_MILLIS -> java.time.format.DateTimeFormatter.ofPattern("dd.MM. HH:mm")
        TimeMode.SECONDS_OF_DAY -> java.time.format.DateTimeFormatter.ofPattern("HH:mm")
    }
    for (i in 0..steps) {
        val xPercent = if (layoutDirection == LayoutDirection.Ltr) i / steps.toFloat() else 1 - (i / steps.toFloat())
        val xOffset = if (layoutDirection == LayoutDirection.Ltr) args.startGutter else args.endGutter - args.startGutter
        val plotWidth = size.width - args.endGutter - args.startGutter
        val x = xPercent * plotWidth + xOffset
        fun drawTimeHelperLine(onEdge: Boolean) {
            drawLine(
                color = args.axisColor,
                start = Offset(x, y = if (onEdge) 0f else args.topGutter),
                end = Offset(x, y = size.height),
                strokeWidth = args.axisWidth,
                pathEffect = if (!onEdge) PathEffect.dashPathEffect(args.axisDashIntervals.toFloatArray()) else null
            )
        }
        // Compute the timestamp for this tick
        val tickTs = minTimestamp + ((maxTimestamp - minTimestamp) * (i / steps.toFloat())).toLong()
        val label = when (timeMode) {
            TimeMode.EPOCH_MILLIS -> dateFormatter.format(
                java.time.Instant.ofEpochMilli(tickTs).atZone(zoneId)
            )
            TimeMode.SECONDS_OF_DAY -> dateFormatter.format(
                java.time.LocalTime.ofSecondOfDay(tickTs)
            )
        }
        // Draw helper line and label for every tick
        drawTimeHelperLine(onEdge = i == 0 || i == steps)
        val measuredLabel = measurer.measure(label, style = args.axisTextStyle)
        val textTopLeftX =
            if (layoutDirection == LayoutDirection.Ltr) x + args.bottomAxisTextPaddingHorizontal
            else x - measuredLabel.size.width - args.bottomAxisTextPaddingHorizontal
        val textTopLeftXMin =
            if (layoutDirection == LayoutDirection.Ltr) args.startGutter + args.bottomAxisTextPaddingHorizontal
            else args.endGutter + args.bottomAxisTextPaddingHorizontal
        val textTopLeftXMax =
            if (layoutDirection == LayoutDirection.Ltr) size.width - args.endGutter - measuredLabel.size.width - args.bottomAxisTextPaddingHorizontal
            else size.width - args.startGutter - measuredLabel.size.width - args.bottomAxisTextPaddingHorizontal
        drawText(
            textLayoutResult = measuredLabel,
            color = args.axisColor,
            topLeft = Offset(
                x = textTopLeftX.coerceIn(
                    minimumValue = textTopLeftXMin,
                    maximumValue = textTopLeftXMax
                ),
                y = size.height - args.bottomGutter + args.bottomAxisTextPaddingTop
            )
        )
        onStepDrawn(i, x, { percent ->
            val percentFromBottom = 1 - percent
            val plotHeight = size.height - args.topGutter - args.bottomGutter
            val yOffset = args.topGutter
            YData(
                top = ((percentFromBottom * plotHeight) + yOffset).toFloat(),
                bot = size.height - args.bottomGutter
            )
        }, label)
    }
}

data class YData(
    val top: Float,
    val bot: Float,
) {
    val height = bot - top
}