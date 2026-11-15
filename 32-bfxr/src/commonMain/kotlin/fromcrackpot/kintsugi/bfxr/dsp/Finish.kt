package fromcrackpot.kintsugi.bfxr.dsp

import kotlin.math.max
import kotlin.math.min
import kotlin.math.tanh

internal fun finish(buffer: FloatArray, volume: Double = 0.5, loop: Boolean = false): FloatArray {
    var mean = 0.0
    for (value in buffer) mean += if (value.isFinite()) value.toDouble() else 0.0
    mean /= max(1, buffer.size)
    val fadeLength = max(1, min(220, buffer.size / 2)).toDouble()
    val gain = clamp(volume, 0.0, 1.0)
    for (i in buffer.indices) {
        val sample = buffer[i]
        val value = if (sample.isFinite()) sample - mean else 0.0
        val fade = if (loop) 1.0 else min(1.0, min(i / fadeLength, (buffer.size - 1 - i) / fadeLength))
        buffer[i] = (tanh(value) * gain * 0.95 * fade).toFloat()
    }
    return buffer
}
