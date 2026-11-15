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

internal object Breathr {
    fun render(p: Map<String, BfxrValue>): FloatArray {
        val rate = RATE.toDouble()
        val tau = PI * 2
        val duration = p.value("duration", 2.7, 0.15, 5.0)
        val out = FloatArray(jsRound(duration * rate).toInt())
        val volume = p.value("masterVolume", 0.5)
        if (volume == 0.0) return finish(out, 0.0)
        val single = jsRound(p.value("mode", 1.0, 0.0, 1.0)) == 0.0
        val inward = (1 - p.value("direction", 1.0, -1.0, 1.0)) / 2
        val random = Mulberry32(p.value("seed", 0.5))
        val cycles = if (single) 1.0 else jsRound(p.value("cycles", 1.0, 1.0, 10.0))
        val source = jsRound(p.value("source", 0.0, 0.0, 2.0)).toInt()
        val effort = p.value("effort", 0.55)
        val inhale = p.value("inhale", 0.42, 0.1, 0.9)
        val hold = p.value("hold", 0.04, 0.0, 0.7)
        val throat = p.value("throat", 0.5)
        val rasp = p.value("rasp", 0.1)
        val flutter = p.value("flutter", 0.15)
        val space = p.value("space", 0.1)
        val slot = out.size / cycles
        val inEnd = (1 - hold) * inhale * 0.9
        val outStart = inEnd + hold * 0.9 + 0.025
        val outEnd = 0.965
        val delay = jsRound(rate * (0.022 + space * 0.075)).toInt()
        val gain = 1.5 + effort * 1.5
        val variations = DoubleArray(cycles.toInt()) { 0.88 + random.next() * 0.24 }
        val radius = exp(-PI * 550 / rate)
        val r2 = radius * radius
        val bandGain = (1 - radius) * 1.6
        var low = 0.0
        var highpass = 0.0
        var y1 = 0.0
        var y2 = 0.0
        var previous = 0.0
        var previous2 = 0.0
        var wander = 0.0
        var phase = random.next() * tau
        var coefficient = 0.0
        var cutoff = 0.0
        var heldNoise = 0.0
        val heldPeriod = jsRound(5 + throat * 22).toInt()
        for (i in out.indices) {
            val position = (i % slot) / slot
            val cycle = min(cycles - 1, floor(i / slot)).toInt()
            val inhaling = position < inEnd
            val exhaling = position > outStart && position < outEnd
            val blend = if (single) inward else if (inhaling) 1.0 else 0.0
            val local = when {
                single -> i / max(1.0, out.size - 1.0)
                inhaling -> position / inEnd
                exhaling -> (position - outStart) / (outEnd - outStart)
                else -> 0.0
            }
            val arch = max(0.0, sin(PI * local)).pow(if (single) 0.7 + 0.1 * blend else if (inhaling) 0.8 else 0.7)
            val shape = when {
                single -> (1 - 0.36 * local) * (1 - blend) + (0.8 + 0.2 * local) * blend
                inhaling -> 0.8 + 0.2 * local
                else -> 1 - 0.36 * local
            }
            val airflow = arch * shape * variations[cycle]
            val noise = random.next() * 2 - 1
            wander += (noise - wander) * 0.0025
            if (source == 1 && i % heldPeriod == 0) heldNoise = if (noise > 0) 0.8 else -0.8
            val excitation = if (source == 1) heldNoise else noise
            if ((i and 31) == 0) {
                val airHz = if (single) {
                    (750 + effort * 1600) * (1 - blend) + (2400 + effort * 2700) * blend
                } else if (inhaling) {
                    2400 + effort * 2700
                } else {
                    750 + effort * 1600
                }
                val hz = airHz * (1 - throat * 0.38) * (0.75 + airflow * 0.25)
                cutoff = 1 - exp(-tau * hz / rate)
                coefficient = 2 * radius *
                    cos(tau * (if (single) 510 + 490 * blend else if (inhaling) 1000.0 else 510.0) * (1.35 - throat * 0.6) / rate)
            }
            low += (excitation - low) * cutoff
            highpass += (low - highpass) * (1 - exp(-tau * (if (single) 110 + 120 * blend else if (inhaling) 230.0 else 110.0) / rate))
            val turbulent = low - highpass
            val resonant = bandGain * (turbulent - previous2) + coefficient * y1 - r2 * y2
            y2 = y1
            y1 = resonant
            previous2 = previous
            previous = turbulent
            phase += tau * (24 + (1 - throat) * 44) * (1 + flutter * 0.3 * sin(tau * 2.3 * i / rate) + wander * 0.9) / rate
            val obstruction = if (source == 2) 0.72 + rasp * 0.27 else rasp * 0.22
            val flap = max(0.0, sin(phase)).pow(3)
            val airway = 1 - obstruction + obstruction * flap
            val tremor = max(0.2, 1 + wander * flutter * 6 + flutter * 0.07 * sin(tau * 7.3 * i / rate))
            var envelope = airflow * tremor
            if (source == 1) envelope = jsRound(envelope * 12) / 12
            val tissue = if (source == 2) (flap - 0.212) * rasp * 0.3 * arch else 0.0
            out[i] = (((turbulent * 0.85 + resonant * 0.8) * airway + tissue) * envelope * gain).toFloat()
            if (i >= delay) out[i] = (out[i] + out[i - delay] * space * 0.38).toFloat()
        }
        return finish(out, volume)
    }
}
