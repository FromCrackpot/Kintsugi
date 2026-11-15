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
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

internal object Crittr {
    private val ANATOMIES = listOf(
        doubleArrayOf(550.0, 1260.0, 2550.0),
        doubleArrayOf(820.0, 2100.0, 3900.0),
        doubleArrayOf(360.0, 950.0, 1840.0),
        doubleArrayOf(1100.0, 2800.0, 4700.0),
        doubleArrayOf(430.0, 1500.0, 3200.0),
        doubleArrayOf(670.0, 1740.0, 3550.0),
        doubleArrayOf(430.0, 1150.0, 2450.0),
        doubleArrayOf(700.0, 1900.0, 3100.0),
    )

    private val DUTIES = doubleArrayOf(0.24, 0.12, 0.3, 0.07, 0.38, 0.1, 0.2, 0.28)

    fun render(p: Map<String, BfxrValue>): FloatArray {
        val rate = RATE.toDouble()
        val tau = PI * 2
        val duration = p.value("duration", 1.2, 0.15, 5.0)
        val output = FloatArray(jsRound(duration * rate).toInt())
        val volume = p.value("masterVolume", 0.5)
        if (volume == 0.0) return finish(output, 0.0)
        val random = Mulberry32(p.value("seed", 0.5))
        val voice = jsRound(p.value("voice", 0.0, 0.0, 7.0)).toInt()
        val pitch = 45 * 2.0.pow(5 * p.value("pitch", 0.45))
        val size = p.value("size", 0.45)
        val morph = p.value("morph", 0.45)
        val breath = p.value("breath", 0.15)
        val growl = p.value("growl", 0.2)
        val flutter = p.value("flutter", 0.2)
        val contour = p.value("contour", 0.25, -1.0, 1.0)
        val calls = jsRound(p.value("calls", 2.0, 1.0, 12.0))
        val slot = output.size / calls
        val active = slot * (1 - p.value("gap", 0.2, 0.0, 0.85))
        val attack = max(40.0, active * (if (voice == 6) 0.025 else if (voice == 7) 0.13 else 0.09))
        val release = max(80.0, active * 0.28)
        val scale = 2.0.pow((0.5 - size) * 2.8)
        val frequencies = DoubleArray(3) { min(10000.0, ANATOMIES[voice][it] * scale) }
        val radii = doubleArrayOf(exp(-PI * 100 / rate), exp(-PI * 160 / rate), exp(-PI * 230 / rate))
        val coefficients = DoubleArray(3)
        val y1 = DoubleArray(3)
        val y2 = DoubleArray(3)
        val radiiSquared = DoubleArray(3) { radii[it] * radii[it] }
        val gains = DoubleArray(3) { (1 - radii[it]) * 0.95 }
        val callMotion = DoubleArray(calls.toInt()) { random.next() * 2 - 1 }
        val flutterRate = 7 + flutter * 38
        val flutterStep = tau * flutterRate / rate
        val duty = DUTIES[voice]
        var phase = random.next() * tau
        var flutterPhase = random.next() * tau
        var previous = 0.0
        var previous2 = 0.0
        var noiseLow = 0.0
        for (i in output.indices) {
            val call = min(calls - 1, floor(i / slot))
            val local = i - call * slot
            val position = min(1.0, local / active)
            val envelope = max(0.0, min(1.0, min(local / attack, (active - local) / release)))
            val tremor = sin(flutterPhase)
            flutterPhase += flutterStep
            var bend = contour * ((position - 0.5) * 1.9 + callMotion[call.toInt()] * 0.3)
            if (voice == 6) bend += 0.5 * exp(-position * 13) - position * 0.3
            if (voice == 7) bend += 0.5 * sin(PI * min(1.0, position * 1.35)) - 0.3 * position
            phase += tau * pitch * exp(bend * 0.69314718056) * (1 + flutter * 0.045 * tremor) / rate
            val cycle = phase / tau - floor(phase / tau)
            val pulse = if (cycle < duty) 0.5 - 0.5 * cos(tau * cycle / duty) else 0.0
            val noise = random.next() * 2 - 1
            noiseLow += (noise - noiseLow) * 0.06
            var excitation = (pulse - duty * 0.5) * 3.4
            if (voice == 1) excitation += 0.22 * sin(phase * 2 + 0.8 * sin(phase))
            if (voice == 2) excitation *= 0.7 + 0.3 * sin(phase * 0.5)
            if (voice == 3) excitation *= 0.6 + 0.4 * sin(phase * 1.47)
            if (voice == 4) excitation = 0.48 * sin(phase) + excitation * 0.3
            if (voice == 5) excitation += 0.2 * sin(phase * 2.71)
            if (voice == 6) excitation = excitation * (0.85 + 0.15 * sin(phase * 0.5)) + noise * 0.65 * exp(-position * 20)
            if (voice == 7) excitation += 0.18 * sin(phase * 2) + 0.09 * sin(phase * 3)
            excitation = excitation * (1 - breath * 0.8) + noise * breath * 0.9
            if ((i and 31) == 0) {
                val opening = morph * (sin(PI * position) + 0.3 * callMotion[call.toInt()])
                coefficients[0] = 2 * radii[0] * cos(tau * min(11000.0, frequencies[0] * (1 + opening * 0.65)) / rate)
                coefficients[1] = 2 * radii[1] * cos(tau * min(11000.0, frequencies[1] * (1 - opening * 0.23)) / rate)
                coefficients[2] = 2 * radii[2] * cos(tau * min(11000.0, frequencies[2] * (1 + opening * 0.17)) / rate)
                if (voice == 6 || voice == 7) {
                    val mouth = if (voice == 6) exp(-position * 5) else sin(PI * position).pow(1.4)
                    coefficients[0] = 2 * radii[0] * cos(tau * min(11000.0, frequencies[0] * (0.58 + morph * mouth * 0.95)) / rate)
                    coefficients[1] = 2 * radii[1] * cos(tau * min(11000.0, frequencies[1] * (1 - morph * position * 0.58)) / rate)
                }
            }
            var resonant = 0.0
            for (band in 0 until 3) {
                val sample = gains[band] * (excitation - previous2) + coefficients[band] * y1[band] - radiiSquared[band] * y2[band]
                y2[band] = y1[band]
                y1[band] = sample
                resonant += sample * (if (band == 0) 1.15 else if (band == 1) 0.85 else 0.5)
            }
            previous2 = previous
            previous = excitation
            val subharmonic = growl * (0.28 * sin(phase * 0.5) + 0.1 * sin(phase / 3)) * (0.85 + noiseLow * 0.7)
            val throat = resonant * 1.65 + 0.12 * (1 - breath) * sin(phase) + subharmonic
            val trill = 1 - flutter * 0.42 + flutter * 0.42 * tremor
            val articulation = if (voice == 6) exp(-position * 4.5) else if (voice == 7) 0.75 + 0.25 * sin(PI * position) else 1.0
            output[i] = (throat * envelope * trill * articulation).toFloat()
        }
        return finish(output, volume)
    }
}
