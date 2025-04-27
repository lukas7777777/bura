package com.davidtakac.bura.graphs.common
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.davidtakac.bura.graphs.common.GraphArgs
import com.davidtakac.bura.graphs.temperature.TemperatureGraph
import com.davidtakac.bura.graphs.pop.PopGraph
import com.davidtakac.bura.graphs.precipitation.PrecipitationGraph
import com.davidtakac.bura.precipitation.MixedPrecipitation
import com.davidtakac.bura.temperature.Temperature

@Composable
fun CombinedGraph(
    stateTemp: TemperatureGraph,
    absMinTemp: Temperature,
    absMaxTemp: Temperature,
    argsTemp: GraphArgs,
    statePop: PopGraph,
    argsPop: GraphArgs,
    statePrecip: PrecipitationGraph,
    maxPrecip: MixedPrecipitation,
    argsPrecip: GraphArgs,
    modifier: Modifier = Modifier
) {
    // Compute global min/max seconds-of-day from all three graphs
    val minTimestamp = listOfNotNull(
        stateTemp.points.minOfOrNull { it.time.value.toSecondOfDay() },
        statePop.points.minOfOrNull { it.time.value.toSecondOfDay() },
        statePrecip.points.minOfOrNull { it.time.value.toSecondOfDay() }
    ).minOrNull()?.toLong() ?: 0L
    val maxTimestamp = listOfNotNull(
        stateTemp.points.maxOfOrNull { it.time.value.toSecondOfDay() },
        statePop.points.maxOfOrNull { it.time.value.toSecondOfDay() },
        statePrecip.points.maxOfOrNull { it.time.value.toSecondOfDay() }
    ).maxOrNull()?.toLong() ?: 86399L
    Box(modifier) {
        PopGraph(
            state = statePop,
            args = argsPop,
            minTimestamp = minTimestamp,
            maxTimestamp = maxTimestamp,
            modifier = Modifier.fillMaxSize()
        )
        TemperatureGraph(
            state = stateTemp,
            absMinTemp = absMinTemp,
            absMaxTemp = absMaxTemp,
            args = argsTemp,
            minTimestamp = minTimestamp,
            maxTimestamp = maxTimestamp,
            modifier = Modifier.fillMaxSize()
        )
        PrecipitationGraph(
            state = statePrecip,
            max = maxPrecip,
            args = argsPrecip,
            minTimestamp = minTimestamp,
            maxTimestamp = maxTimestamp,
            modifier = Modifier.fillMaxSize()
        )
    }
}
