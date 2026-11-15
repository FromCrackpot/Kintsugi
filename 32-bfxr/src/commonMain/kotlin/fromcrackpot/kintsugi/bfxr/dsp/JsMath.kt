package fromcrackpot.kintsugi.bfxr.dsp

import fromcrackpot.kintsugi.bfxr.BfxrValue
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.math.withSign

internal const val RATE: Int = 44_100

// JS Math.round: ties toward +∞ and keeps −0, unlike Kotlin's half-even round().
internal fun jsRound(x: Double): Double {
    val f = floor(x)
    return if (x - f >= 0.5) (f + 1).withSign(x) else f
}

// JS `x|0`: truncates and wraps modulo 2³², NaN and ±∞ → 0; Kotlin's toInt() saturates instead.
internal fun toInt32(x: Double): Int = if (x.isFinite()) (x % 4294967296.0).toLong().toInt() else 0

// V8's Math.hypot: scaled by the larger magnitude, squares Kahan-summed; can differ from libm hypot by an ulp.
internal fun jsHypot(a: Double, b: Double): Double {
    if (a.isInfinite() || b.isInfinite()) return Double.POSITIVE_INFINITY
    if (a.isNaN() || b.isNaN()) return Double.NaN
    val max = max(abs(a), abs(b))
    if (max == 0.0) return 0.0
    var sum = 0.0
    var compensation = 0.0
    for (x in doubleArrayOf(abs(a), abs(b))) {
        val n = x / max
        val summand = n * n - compensation
        val preliminary = sum + summand
        compensation = (preliminary - sum) - summand
        sum = preliminary
    }
    return sqrt(sum) * max
}

internal fun clamp(value: Double, lo: Double, hi: Double): Double = max(lo, min(hi, value))

internal fun Map<String, BfxrValue>.value(name: String, fallback: Double, lo: Double = 0.0, hi: Double = 1.0): Double {
    val number = (this[name] as? BfxrValue.Number)?.value
    return if (number != null && number.isFinite()) clamp(number, lo, hi) else fallback
}
