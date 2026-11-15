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

internal object Squishr {
    private class Bubble(val start: Int, val length: Double, val frequency: Double, val strength: Double, val phase: Double)

    fun render(params: Map<String, BfxrValue>): FloatArray {
        val rate = RATE.toDouble()
        val random = Mulberry32(params.value("seed", 0.5))
        val texture = jsRound(params.value("texture", 0.0, 0.0, 5.0)).toInt()
        val viscosity = params.value("viscosity", 0.6)
        val stretch = params.value("stretch", 0.4)
        val pressure = params.value("pressure", 0.6)
        val wetness = params.value("wetness", 0.75)
        val bubbleSize = params.value("bubbleSize", 0.55)
        val duration = params.value("duration", 0.65, 0.1, 3.0)
        val release = 0.06 + viscosity * 0.16 + stretch * 0.13
        val length = ceil((duration + release) * rate).toInt()
        val buffer = FloatArray(length)
        val base = 1650 * 2.0.pow(-bubbleSize * 3.8)
        val amplitude = 0.45 + pressure * 0.7
        val onset = 0.008
        val events = ArrayList<Bubble>()

        fun addBubble(start: Double, strength: Double, pitch: Double = 1.0, life: Double = 1.0) {
            val seconds = (0.026 + viscosity * 0.065 + stretch * 0.13) * life
            events += Bubble(
                start = jsRound(start * rate).toInt(),
                length = jsRound(seconds * rate),
                frequency = base * pitch * (0.84 + random.next() * 0.32),
                strength = strength,
                phase = random.next() * 0.2,
            )
        }

        when (texture) {
            1 -> {
                val count = max(1.0, jsRound(duration * (2 + pressure * 7))).toInt()
                for (i in 0 until count) {
                    addBubble(onset + i * duration * 0.85 / count, 0.7 + random.next() * 0.3, 0.8 + random.next() * 0.4, 1.1)
                }
            }
            2 -> {
                addBubble(duration * 0.76, 1.2, 0.75, 1.5)
                addBubble(duration * 0.87, 0.55, 1.3, 0.7)
            }
            3 -> repeat(7) {
                addBubble(
                    onset + random.next() * duration * 0.34,
                    0.4 + random.next() * 0.45,
                    0.6 + random.next() * 1.1,
                    0.6 + random.next() * 0.6,
                )
            }
            4 -> {
                val count = max(1.0, jsRound(duration * (3 + pressure * 3))).toInt()
                for (i in 0 until count) {
                    val time = onset + i * duration * 0.82 / count
                    addBubble(time, 1.0, 0.62, 1.4)
                    addBubble(time + 0.035, 0.45, 1.35, 0.7)
                }
            }
            else -> {
                val count = max(2.0, jsRound(duration * (5 + pressure * 10))).toInt()
                for (i in 0 until count) {
                    addBubble(
                        onset + i * duration * 0.88 / count,
                        0.3 + random.next() * 0.5,
                        if (texture == 5) 0.7 else 0.5 + random.next() * 0.8,
                        if (texture == 5) 2.0 else 0.65 + random.next() * 0.65,
                    )
                }
            }
        }

        var lowNoise = 0.0
        var smoothNoise = 0.0
        var bodyPhase = 0.0
        var previous = 0.0
        val cutoff = 180 + (1 - viscosity) * 3800 + pressure * 500
        val filter = 1 - exp(-PI * 2 * cutoff / rate)
        for (i in 0 until length) {
            val time = i / rate
            val progress = min(1.0, time / duration)
            val tail = if (time <= duration) 1.0 else exp(-(time - duration) / (release * 0.2))
            val noise = random.next() * 2 - 1
            lowNoise += (noise - lowNoise) * filter
            smoothNoise += (lowNoise - smoothNoise) * filter
            val envelope = when (texture) {
                2 -> sin(PI * min(1.0, progress / 0.85)).pow(2)
                3 -> exp(-progress * (5 - viscosity * 2))
                4 -> (0.4 + 0.6 * sin(progress * PI * (3 + pressure * 3)).pow(2)) * (1 - progress * 0.6)
                else -> sin(PI * progress).pow(0.55)
            }
            val attack = min(1.0, time / 0.009)
            val bodyFrequency = base * (if (texture == 5) 0.27 else 0.16) *
                (1 + sin(time * (12 + pressure * 20)) * stretch * 0.3) *
                (if (texture == 2) 1.6 - progress else 1 - progress * stretch * 0.45)
            bodyPhase += PI * 2 * bodyFrequency / rate
            val body = sin(bodyPhase + sin(bodyPhase * 0.5) * stretch) *
                (if (texture == 5) 0.4 else 0.13) * (0.25 + viscosity * 0.75)
            val rasp = smoothNoise * (0.6 + wetness * 0.45) + (lowNoise - previous) * (1 - viscosity) * 0.25
            val noiseLevel = when (texture) {
                1 -> 0.015
                5 -> 0.14
                3 -> 1.1
                else -> 0.7
            }
            val bodyLevel = if (texture == 1) 0.03 else 1.0
            buffer[i] = ((rasp * noiseLevel + body * bodyLevel) * envelope * tail * amplitude * attack).toFloat()
            previous = lowNoise
        }

        for (event in events) {
            var phase = event.phase
            var j = 0
            while (j < event.length && event.start + j < length) {
                val progress = j / event.length
                val glide = when (texture) {
                    2, 4 -> 1.65 - progress * (1.1 + stretch * 0.3)
                    5 -> 1 + sin(progress * PI * (3 + stretch * 5)) * stretch * 0.6
                    else -> 0.65 + progress * (0.8 + pressure * 1.5) + sin(progress * PI) * stretch * 0.5
                }
                phase += PI * 2 * event.frequency * glide / rate
                val envelope = min(1.0, j / (rate * 0.0015)) * exp(-progress * (5 - viscosity * 2)) *
                    min(1.0, (1 - progress) * 15)
                val rounded = sin(phase) + sin(phase * 2) * (1 - viscosity) * 0.13
                buffer[event.start + j] = (buffer[event.start + j] +
                    rounded * envelope * event.strength * amplitude * (0.25 + wetness * 0.7)).toFloat()
                j++
            }
        }
        return finish(buffer, params.value("masterVolume", 0.5))
    }
}
