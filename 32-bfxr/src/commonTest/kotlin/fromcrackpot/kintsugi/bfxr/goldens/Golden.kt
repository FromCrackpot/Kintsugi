package fromcrackpot.kintsugi.bfxr.goldens

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

internal const val DIGEST_BLOCK = 256

internal class Golden(val name: String, val sound: String, val frames: Int, digest: String) {
    val digest: DoubleArray = digest.split(' ').map(String::toDouble).toDoubleArray()
}

internal fun digestOf(samples: FloatArray): DoubleArray {
    val values = ArrayList<Double>()
    for (start in samples.indices step DIGEST_BLOCK) {
        val end = minOf(samples.size, start + DIGEST_BLOCK)
        var peak = 0.0
        var sum = 0.0
        for (i in start until end) {
            val x = samples[i].toDouble()
            peak = max(peak, abs(x))
            sum += x * x
        }
        values += peak
        values += sqrt(sum / (end - start))
        values += samples[start].toDouble()
    }
    return values.toDoubleArray()
}
