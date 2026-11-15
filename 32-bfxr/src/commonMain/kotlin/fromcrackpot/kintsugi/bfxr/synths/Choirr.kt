package fromcrackpot.kintsugi.bfxr.synths

import fromcrackpot.kintsugi.bfxr.BfxrValue
import fromcrackpot.kintsugi.bfxr.dsp.Mulberry32
import fromcrackpot.kintsugi.bfxr.dsp.RATE
import fromcrackpot.kintsugi.bfxr.dsp.finish
import fromcrackpot.kintsugi.bfxr.dsp.jsRound
import fromcrackpot.kintsugi.bfxr.dsp.value
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

internal object Choirr {
    private val CHORDS = listOf(
        doubleArrayOf(0.0),
        doubleArrayOf(0.0, 4.0, 7.0, 12.0),
        doubleArrayOf(0.0, 3.0, 7.0, 12.0),
        doubleArrayOf(0.0, 7.0, 12.0, 19.0),
        doubleArrayOf(0.0, 1.0, 6.0, 10.0),
    )

    private val VOWELS = listOf(
        doubleArrayOf(350.0, 800.0, 2300.0),
        doubleArrayOf(750.0, 1150.0, 2600.0),
        doubleArrayOf(300.0, 2200.0, 3100.0),
    )

    fun render(p: Map<String, BfxrValue>): FloatArray {
        val rate = RATE.toDouble()
        val tau = PI * 2
        val duration = p.value("duration", 2.5, 0.15, 5.0)
        val out = FloatArray(jsRound(duration * rate).toInt())
        val volume = p.value("masterVolume", 0.5)
        if (volume == 0.0) return finish(out, 0.0)
        val random = Mulberry32(p.value("seed", 0.5))
        val voices = jsRound(p.value("voices", 6.0, 1.0, 12.0)).toInt()
        val pitch = 55 * 2.0.pow(p.value("pitch", 0.5) * 4)
        val vowel = p.value("vowel", 0.35)
        val detune = p.value("detune", 0.4)
        val swell = p.value("swell", 0.5)
        val breath = p.value("breath", 0.1)
        val motion = p.value("motion", 0.4)
        val harmony = jsRound(p.value("harmony", 1.0, 0.0, 4.0)).toInt()
        val vIndex = if (vowel < 0.5) 0 else 1
        val blend = if (vowel < 0.5) vowel * 2 else (vowel - 0.5) * 2
        val gain = 1.9 / sqrt(voices.toDouble())
        val radii = doubleArrayOf(exp(-PI * 85 / rate), exp(-PI * 120 / rate), exp(-PI * 190 / rate))
        val chord = CHORDS[harmony]
        for (voice in 0 until voices) {
            val note = chord[voice % chord.size]
            val frequency = pitch * 2.0.pow(note / 12 + (random.next() - 0.5) * detune * 0.08)
            val formantScale = 0.92 + random.next() * 0.16
            val coeff = DoubleArray(radii.size) { k ->
                2 * radii[k] *
                    cos(tau * (VOWELS[vIndex][k] * (1 - blend) + VOWELS[vIndex + 1][k] * blend) * formantScale / rate)
            }
            val y1 = DoubleArray(3)
            val y2 = DoubleArray(3)
            val squares = DoubleArray(radii.size) { radii[it] * radii[it] }
            val gains = DoubleArray(radii.size) { 1 - radii[it] }
            val initial = random.next() * tau
            val vibratoRate = 4.2 + random.next() * 1.8
            val onset = jsRound(random.next() * motion * min(out.size * 0.1, 3000.0)).toInt()
            var phase = random.next()
            var previous = 0.0
            var previous2 = 0.0
            for (i in onset until out.size) {
                val age = (i - onset).toDouble() / (out.size - onset)
                val t = i / rate
                val step = frequency * (1 + motion * 0.009 * sin(tau * vibratoRate * t + initial)) / rate
                phase += step
                if (phase >= 1) phase -= 1
                var source = 2 * phase - 1
                if (phase < step) {
                    val x = phase / step
                    source -= x + x - x * x - 1
                } else if (phase > 1 - step) {
                    val x = (phase - 1) / step
                    source -= x * x + x + x + 1
                }
                source = source * (1 - breath * 0.7) + (random.next() * 2 - 1) * breath * 0.35
                var resonant = 0.0
                for (band in 0 until 3) {
                    val sample = gains[band] * (source - previous2) + coeff[band] * y1[band] - squares[band] * y2[band]
                    y2[band] = y1[band]
                    y1[band] = sample
                    resonant += sample * (if (band == 0) 1.2 else if (band == 1) 0.8 else 0.5)
                }
                previous2 = previous
                previous = source
                val envelope = max(0.0, sin(PI * age)).pow(0.25 + swell * 2.7)
                out[i] = (out[i] + (resonant + sin(tau * phase) * 0.065) * envelope * gain *
                    (0.9 + motion * 0.1 * sin(tau * 0.7 * t + initial))).toFloat()
            }
        }
        return finish(out, volume)
    }
}
