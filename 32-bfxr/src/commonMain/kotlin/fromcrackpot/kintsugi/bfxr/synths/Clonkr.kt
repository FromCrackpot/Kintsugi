package fromcrackpot.kintsugi.bfxr.synths

import fromcrackpot.kintsugi.bfxr.BfxrValue
import fromcrackpot.kintsugi.bfxr.dsp.Mulberry32
import fromcrackpot.kintsugi.bfxr.dsp.RATE
import fromcrackpot.kintsugi.bfxr.dsp.finish
import fromcrackpot.kintsugi.bfxr.dsp.jsRound
import fromcrackpot.kintsugi.bfxr.dsp.value
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

internal object Clonkr {
    private class Material(val pitch: Double, val ring: Double, val brightness: Double, val ratios: DoubleArray)

    private class Mode(val cos: Double, val sin: Double, val weight: Double) {
        var real = 0.0
        var imaginary = 0.0
    }

    private val MATERIALS = listOf(
        Material(0.7, 0.34, 0.55, doubleArrayOf(1.0, 2.17, 3.04, 4.61, 5.43, 6.8, 8.7, 10.3)),
        Material(1.55, 1.25, 0.95, doubleArrayOf(1.0, 2.32, 4.25, 6.63, 9.38, 12.4, 15.8, 18.1)),
        Material(0.92, 1.6, 1.0, doubleArrayOf(1.0, 1.48, 2.06, 2.63, 3.52, 4.89, 6.28, 8.13)),
        Material(1.12, 0.58, 0.74, doubleArrayOf(1.0, 1.87, 3.22, 4.74, 6.18, 7.91, 10.2, 12.6)),
        Material(0.42, 0.16, 0.2, doubleArrayOf(1.0, 1.99, 3.02, 4.08, 5.19, 6.31, 7.46, 8.64)),
    )

    fun render(p: Map<String, BfxrValue>): FloatArray {
        val rate = RATE.toDouble()
        val random = Mulberry32(p.value("seed", 0.5))
        val material = MATERIALS[jsRound(p.value("material", 0.0, 0.0, 4.0)).toInt()]
        val action = jsRound(p.value("action", 0.0, 0.0, 2.0)).toInt()
        val size = p.value("size", 0.5)
        val hollow = p.value("hollowness", 0.35)
        val hardness = p.value("hardness", 0.65)
        val damping = p.value("damping", 0.35)
        val duration = p.value("duration", 0.7, 0.1, 2.0)
        val decay = 0.009 + (0.022 + duration * 0.44 * material.ring) * (1 - 0.92 * damping)
        val contactEnd = if (action == 0) 0.025 else duration
        val length = ceil((contactEnd + min(3.5, decay * 7)) * rate).toInt()
        val buffer = FloatArray(length)
        val excitation = FloatArray(length)
        val onset = jsRound(0.006 * rate).toInt()

        fun impact(time: Double, strength: Double) {
            val start = jsRound(time * rate).toInt()
            if (start >= length) return
            val width = max(1.0, jsRound((1 - hardness) * 0.0025 * rate)).toInt()
            var j = 0
            while (j < width && start + j < length) {
                val force = if (width == 1) 1.0 else sin(PI * (j + 0.5) / width) * PI / (2 * width)
                excitation[start + j] = (excitation[start + j] + force * strength).toFloat()
                j++
            }
            val noiseLength = jsRound((0.002 + (1 - hardness) * 0.009) * rate)
            j = 0
            while (j < noiseLength && start + j < length) {
                buffer[start + j] = (buffer[start + j] + (random.next() * 2 - 1) * exp(-j / (noiseLength * 0.2)) *
                    strength * (0.07 + hardness * 0.2)).toFloat()
                j++
            }
        }

        impact(onset / rate, 1.0)
        if (action == 1) {
            var friction = 0.0
            val end = jsRound(contactEnd * rate)
            var i = onset
            while (i < end) {
                val progress = (i - onset) / max(1.0, contactEnd * rate - onset)
                friction += ((random.next() * 2 - 1) - friction) * (0.08 + 0.7 * hardness)
                val grain = if (random.next() < (0.002 + hardness * 0.012)) (random.next() * 2 - 1) * 0.2 else 0.0
                excitation[i] = (excitation[i] + (friction * 0.022 + grain) * sin(PI * progress).pow(0.4)).toFloat()
                buffer[i] = (buffer[i] + friction * (0.08 + 0.12 * hardness) * sin(PI * progress)).toFloat()
                i++
            }
        } else if (action == 2) {
            val count = jsRound(4 + duration * 7 + hardness * 7).toInt()
            for (hit in 1 until count) {
                val progress = hit.toDouble() / count
                val time = 0.012 + (contactEnd - 0.025) * (progress + (random.next() - 0.5) * 0.5 / count)
                impact(time, (0.35 + random.next() * 0.65) * (1 - progress * 0.6))
            }
        }

        val base = 1400 * 2.0.pow(-size * 4.4) * material.pitch
        val modes = material.ratios.mapIndexed { index, ratio ->
            val detune = 1 + (random.next() - 0.5) * 0.018
            val frequency = min(rate * 0.43, base * ratio * detune)
            val angle = PI * 2 * frequency / rate
            val tau = decay / (1 + index * (0.08 + damping * 0.13))
            val radius = exp(-1 / (tau * rate))
            val weight = exp(-index * (0.28 + (1 - hardness) * 0.62 + (1 - material.brightness) * 0.4)) *
                (if (index == 0) 1 + hollow * 1.2 else 1 - hollow * 0.35)
            Mode(cos(angle) * radius, sin(angle) * radius, weight)
        }
        val scale = 1.4 / modes.fold(0.0) { sum, mode -> sum + mode.weight }
        for (i in onset until length) {
            var sample = 0.0
            for (mode in modes) {
                val real = mode.real * mode.cos - mode.imaginary * mode.sin + excitation[i]
                mode.imaginary = mode.real * mode.sin + mode.imaginary * mode.cos
                mode.real = real
                sample += real * mode.weight
            }
            buffer[i] = (buffer[i] + sample * scale).toFloat()
        }
        return finish(buffer, p.value("masterVolume", 0.5))
    }
}
