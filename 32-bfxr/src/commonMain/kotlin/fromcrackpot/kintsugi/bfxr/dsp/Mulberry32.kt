package fromcrackpot.kintsugi.bfxr.dsp

import kotlin.math.floor

internal class Mulberry32(seed: Double) {
    private var state: Int = floor(seed * 4294967295.0).toLong().toInt()

    fun next(): Double {
        state += 0x6D2B79F5
        var t = (state xor (state ushr 15)) * (1 or state)
        t = (t + (t xor (t ushr 7)) * (61 or t)) xor t
        return (t xor (t ushr 14)).toUInt().toDouble() / 4294967296.0
    }
}
