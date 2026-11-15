package fromcrackpot.kintsugi.bfxr.synths

import fromcrackpot.kintsugi.bfxr.BfxrValue
import fromcrackpot.kintsugi.bfxr.dsp.Mulberry32
import fromcrackpot.kintsugi.bfxr.dsp.RATE
import fromcrackpot.kintsugi.bfxr.dsp.finish
import fromcrackpot.kintsugi.bfxr.dsp.jsRound
import fromcrackpot.kintsugi.bfxr.dsp.value
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

internal object Bouncr {
    private class Body(val pitch: Double, val decay: Double, val hard: Double, val ring: Double, val ratios: DoubleArray)

    private class Mode(val frequency: Double, val decay: Double, var gain: Double) {
        var phase = 0.0
    }

    private val OBJECTS = listOf(
        Body(0.55, 0.041, 0.22, 0.0, doubleArrayOf(1.0, 1.97, 3.17, 4.8)),
        Body(1.0, 0.030, 0.64, 0.0, doubleArrayOf(1.0, 2.43, 4.13, 6.27)),
        Body(2.5, 0.16, 1.0, 0.0, doubleArrayOf(1.0, 1.47, 2.71, 4.09)),
        Body(3.3, 0.10, 0.95, 0.0, doubleArrayOf(1.0, 2.76, 5.4, 8.13)),
        Body(0.72, 0.025, 0.86, 0.0, doubleArrayOf(1.0, 1.91, 3.47, 5.31)),
    )

    private val SURFACES = listOf(
        Body(260.0, 0.022, 0.95, 0.30, doubleArrayOf(1.0, 1.61, 2.83, 4.41)),
        Body(135.0, 0.075, 0.6, 0.72, doubleArrayOf(1.0, 2.18, 3.72, 5.41)),
        Body(330.0, 0.26, 0.93, 0.85, doubleArrayOf(1.0, 1.59, 2.32, 3.79)),
        Body(710.0, 0.18, 1.0, 0.70, doubleArrayOf(1.0, 1.71, 3.04, 4.93)),
        Body(78.0, 0.017, 0.16, 0.18, doubleArrayOf(1.0, 1.83, 2.81, 4.18)),
        Body(90.0, 0.016, 0.035, 0.15, doubleArrayOf(1.0, 2.11, 3.43, 4.72)),
    )

    fun render(p: Map<String, BfxrValue>): FloatArray {
        val tau = PI * 2
        val rate = RATE.toDouble()
        val duration = p.value("duration", 2.0, 0.2, 5.0)
        val out = FloatArray(jsRound(rate * duration).toInt())
        val random = Mulberry32(p.value("seed", 0.5))
        val material = jsRound(p.value("material", 0.0, 0.0, 4.0)).toInt()
        val surface = jsRound(p.value("surface", 0.0, 0.0, 5.0)).toInt()
        val count = jsRound(p.value("count", 1.0, 1.0, 20.0)).toInt()
        val bounce = p.value("bounce", 0.65)
        val gravity = p.value("gravity", 0.5)
        val size = p.value("size", 0.5)
        val hardness = p.value("hardness", 0.6)
        val spin = p.value("spin", 0.0)
        val force = p.value("force", 0.65)
        val tail = p.value("tail", 0.4)
        val body = OBJECTS[material]
        val target = SURFACES[surface]
        val stiffness = sqrt(body.hard * target.hard) * (0.18 + hardness * 0.82)
        val transfer = 0.15 + target.hard * 0.85
        val decayScale = (0.3 + tail * 2.7) * (0.75 + force * 0.5)
        val base = (95 + 790 * (1 - size).pow(2)) * body.pitch
        var time = 0.012
        var gap = min(duration * 0.42, 0.18 + 0.45 * (1 - gravity))
        for (hit in 0 until count) {
            val start = jsRound(time * rate).toInt()
            if (start >= out.size) break
            val strength = (0.18 + force * 0.82) * (0.53 + bounce * 0.43).pow(hit) * (0.94 + random.next() * 0.06)
            val contact = (0.0008 + (1 - stiffness) * 0.013) / (0.65 + force * 0.8)
            val objectDecay = body.decay * decayScale * (0.3 + transfer * 0.7) * (0.75 + bounce * 0.35)
            val surfaceDecay = target.decay * decayScale * (0.8 + size * 0.5)
            val detune = 1 + (random.next() - 0.5) * (0.025 + spin * 0.07)
            val modes = ArrayList<Mode>(8)
            for (mode in 0 until 4) {
                modes += Mode(
                    min(rate * 0.42, base * body.ratios[mode] * detune),
                    objectDecay / (1 + mode * (0.2 + (1 - hardness) * 0.4)),
                    transfer * 0.72 * (stiffness + 0.18).pow(mode * 0.48) / (1 + mode * 0.8),
                )
                modes += Mode(
                    min(rate * 0.42, target.pitch * (1.6 - size) * target.ratios[mode] * (0.97 + random.next() * 0.06)),
                    surfaceDecay / (1 + mode * 0.48),
                    target.ring * 0.72 * (stiffness + 0.12).pow(mode * 0.6) / (1 + mode),
                )
            }
            for (mode in modes) mode.gain /= 1 + (mode.frequency * contact * 0.4).pow(2)
            val end = min(out.size, start + ceil(max(contact * 10, max(objectDecay, surfaceDecay) * 7) * rate).toInt())
            var low = 0.0
            var rub = 0.0
            var absorbed = 0.0
            val tone = 1 - exp(-tau * (100 + stiffness * 11000) / rate)
            val absorption = 1 - exp(-tau * (100 + target.hard.pow(2) * 14000) / rate)
            for (i in start until end) {
                val t = (i - start) / rate
                val attack = min(1.0, t / (0.00025 + contact * 0.3))
                val noise = random.next() * 2 - 1
                low += (noise - low) * tone
                rub += (noise - rub) * 0.08
                var resonance = 0.0
                for (mode in modes) {
                    val bend = if (material == 0) 1 + (0.2 + force * 0.65) * exp(-t / (contact * 2.5)) else 1.0
                    mode.phase += tau * mode.frequency * bend / rate
                    resonance += sin(mode.phase) * mode.gain * exp(-t / mode.decay)
                }
                val contactNoise = low * (0.18 + stiffness * 0.6) * exp(-t / contact)
                val scatter = (if (surface == 4) 0.6 else if (surface == 0) 0.16 else 0.04) * low *
                    exp(-t / (0.014 + tail * 0.035))
                val scrape = rub * spin * 0.6 * exp(-t / (0.025 + spin * 0.08)) *
                    (0.65 + 0.35 * sin(tau * (110 + random.next() * 30) * t))
                absorbed += (resonance + contactNoise + scatter + scrape - absorbed) * absorption
                out[i] = (out[i] + absorbed * strength * attack).toFloat()
            }
            time += gap
            gap = max(0.006, gap * (0.4 + bounce * 0.54) * (1 - spin * 0.12))
        }
        return finish(out, p.value("masterVolume", 0.5))
    }
}
