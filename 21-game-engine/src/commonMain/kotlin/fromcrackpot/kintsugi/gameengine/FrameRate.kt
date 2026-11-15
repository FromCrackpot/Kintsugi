package fromcrackpot.kintsugi.gameengine

import androidx.compose.runtime.Composable

@Composable
internal expect fun PreferFrameRate(rate: Double?)

internal fun pickRate(available: List<Double>, preferred: Double): Int {
    val reaching = available.indices.filter { available[it] >= preferred - RATE_SLACK }
    return if (reaching.isEmpty()) available.indices.maxBy { available[it] } else reaching.minBy { available[it] }
}

// panels report nominal rates as 120.00001 or 59.94
private const val RATE_SLACK = 0.5
