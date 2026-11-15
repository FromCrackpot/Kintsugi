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

internal object Birdr {
    private class Syllable(val start: Double, val active: Double, val shift: Double, val curvature: Double, val phase: Double)

    fun render(p: Map<String, BfxrValue>): FloatArray {
        val rate = RATE.toDouble()
        val tau = 2 * PI
        val duration = p.value("duration", 0.8, 0.15, 5.0)
        val output = FloatArray(jsRound(duration * rate).toInt())
        val volume = p.value("masterVolume", 0.5)
        if (volume == 0.0) return finish(output, 0.0)
        val random = Mulberry32(p.value("seed", 0.5))
        val voice = jsRound(p.value("voice", 0.0, 0.0, 4.0)).toInt()
        val count = jsRound(p.value("syllables", 3.0, 1.0, 16.0)).toInt()
        val gap = p.value("gap", 0.3, 0.0, 0.85)
        val pitch = 180 * 2.0.pow(p.value("pitch", 0.58) * 4.8)
        val sweep = p.value("sweep", -0.3, -1.0, 1.0)
        val arch = p.value("arch", 0.45, -1.0, 1.0)
        val trill = p.value("trill", 0.15)
        val trillRate = 5 + p.value("trill_rate", 0.4) * 65
        val duet = p.value("duet", 0.1)
        val rasp = p.value("rasp", 0.04)
        val breath = p.value("breath", 0.03)
        val rhythm = p.value("rhythm", 0.12)
        val variation = p.value("variation", 0.25)
        val weights = DoubleArray(count) { 1 + rhythm * (random.next() - 0.5) * 1.1 }
        val total = weights.fold(0.0) { a, b -> a + b }
        var offset = 0.0
        val syllables = weights.mapIndexed { index, weight ->
            val length = weight / total * output.size
            val result = Syllable(
                start = offset,
                active = max(1.0, length * (1 - gap)),
                shift = variation * ((if (index % 2 != 0) -0.6 else 0.15) + (random.next() - 0.5) * 0.2),
                curvature = 1 + variation * (random.next() - 0.5) * 0.5,
                phase = random.next() * tau,
            )
            offset += length
            result
        }
        var phase = random.next() * tau
        var second = random.next() * tau
        var noiseLow = 0.0
        var call = 0
        val radii = doubleArrayOf(exp(-PI * 260 / rate), exp(-PI * 480 / rate))
        val y1 = DoubleArray(2)
        val y2 = DoubleArray(2)
        val centers = if (voice == 3) doubleArrayOf(850.0, 1850.0) else doubleArrayOf(1550.0, 3200.0)
        val coeff = DoubleArray(centers.size) { 2 * radii[it] * cos(tau * centers[it] / rate) }
        for (i in output.indices) {
            while (call < count - 1 && i >= syllables[call + 1].start) call++
            val syllable = syllables[call]
            val local = i - syllable.start
            val u = min(1.0, local / syllable.active)
            val attack = max(35.0, syllable.active * (if (voice == 4) 0.18 else 0.065))
            val release = max(70.0, syllable.active * (if (voice == 3) 0.4 else 0.2))
            var envelope = max(0.0, min(1.0, min(local / attack, (syllable.active - local) / release)))
            envelope *= if (voice == 4) sin(PI * u).pow(0.6) else 0.88 + 0.12 * sin(PI * u)
            val tremor = sin(tau * trillRate * local / rate + syllable.phase)
            val contour = sweep * (u - 0.5) * 1.8 + arch * sin(PI * u) * syllable.curvature + syllable.shift
            val irregular = rasp * (0.035 * sin(phase * 0.47) + 0.022 * sin(second * 0.31))
            val frequency = min(10500.0, pitch * 2.0.pow(contour + trill * 0.2 * tremor + irregular))
            phase += tau * frequency / rate
            second += tau * min(11500.0, frequency * (1.12 + 0.15 * sin(PI * u) + duet * 0.35)) / rate
            val noise = random.next() * 2 - 1
            noiseLow += (noise - noiseLow) * 0.13
            val modulation = if (voice == 1) duet * 1.15 * sin(second) else 0.0
            var source = sin(phase + modulation)
            if (voice == 2) source = 0.7 * sin(phase + 0.8 * sin(phase)) + 0.23 * sin(phase * 2) + 0.12 * sin(phase * 3)
            if (voice == 3) source = 0.55 * sin(phase + 1.3 * sin(phase)) + 0.3 * sin(phase * 0.5) + rasp * noiseLow
            if (voice == 4) source = 0.88 * sin(phase) + 0.1 * sin(phase * 2)
            source += duet * 0.28 * sin(second) + rasp * 0.2 * sin(phase * 0.5) * (0.65 + 0.35 * sin(phase * 0.19))
            var resonant = 0.0
            for (band in 0 until 2) {
                val sample = (1 - radii[band]) * source + coeff[band] * y1[band] - radii[band].pow(2) * y2[band]
                y2[band] = y1[band]
                y1[band] = sample
                resonant += sample
            }
            if (voice == 2 || voice == 3) source = source * 0.62 + resonant * 0.85
            source = source * (1 - breath * 0.45) + breath * (noise - noiseLow) * 0.25
            val pulse = 1 - trill * 0.35 + trill * 0.35 * tremor
            output[i] = (source * envelope * pulse * 0.65).toFloat()
        }
        return finish(output, volume)
    }
}
