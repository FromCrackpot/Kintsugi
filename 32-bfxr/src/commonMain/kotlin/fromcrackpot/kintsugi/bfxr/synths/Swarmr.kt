package fromcrackpot.kintsugi.bfxr.synths

import fromcrackpot.kintsugi.bfxr.BfxrValue
import fromcrackpot.kintsugi.bfxr.dsp.Mulberry32
import fromcrackpot.kintsugi.bfxr.dsp.RATE
import fromcrackpot.kintsugi.bfxr.dsp.finish
import fromcrackpot.kintsugi.bfxr.dsp.jsRound
import fromcrackpot.kintsugi.bfxr.dsp.value
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

internal object Swarmr {
    fun render(p: Map<String, BfxrValue>): FloatArray {
        val rate = RATE.toDouble()
        val duration = p.value("duration", 1.8, 0.25, 5.0)
        val frames = jsRound(duration * rate).toInt()
        val out = FloatArray(frames)
        val count = jsRound(p.value("count", 14.0, 3.0, 32.0)).toInt()
        val kind = jsRound(p.value("kind", 0.0, 0.0, 5.0)).toInt()
        val speed = p.value("speed", 0.5)
        val cohesion = p.value("cohesion", 0.3)
        val agitation = p.value("agitation", 0.25)
        val size = p.value("size", 0.45)
        val movement = p.value("movement", 0.5)
        val scatter = p.value("scatter", 0.2)
        val random = Mulberry32(p.value("seed", 0.5))
        val pi = PI
        val base = 90 * 2.0.pow((1 - size) * 3.8)
        val pulseRate = 2 + speed * 17
        val gain = 0.44 / sqrt(count.toDouble())
        val tau = PI * 2
        for (agent in 0 until count) {
            val start = floor(random.next() * scatter * frames * 0.42).toInt()
            val pulseOffset = random.next() * (1 - cohesion)
            val initialPhase = random.next() * tau
            val spread = (random.next() - 0.5) * (0.08 + 0.8 * (1 - cohesion))
            val detune = 2.0.pow(spread)
            val driftPhase = random.next() * tau
            val driftRate = 0.4 + random.next() * 2
            val center = 0.4 + random.next() * 0.2
            val voiceGain = gain * (0.8 + random.next() * 0.4)
            val pulseSpeed = pulseRate * (1 + (random.next() - 0.5) * (1 - cohesion) * 0.3)
            var phase = initialPhase
            var filteredNoise = 0.0
            var wingNoise = 0.0
            for (i in start until frames) {
                val t = i / rate
                val age = (i - start).toDouble() / (frames - start)
                val travel = 1 + movement * 0.55 * (1 - 2 * age)
                val jitter = 1 + agitation * 0.12 * sin(tau * driftRate * t + driftPhase)
                val beat = (t * pulseSpeed + pulseOffset) % 1
                val wing = sin(tau * beat)
                val positive = max(0.0, wing)
                val stroke = positive.pow(3) + 0.32 * max(0.0, -wing).pow(5)
                var frequency = base * detune * travel * jitter
                var envelope = 1.0
                when (kind) {
                    1 -> {
                        frequency *= 1.3 + 2.2 * (1 - beat)
                        envelope = positive.pow(6)
                    }
                    2 -> frequency *= 0.32
                    3 -> {
                        frequency *= 2.3
                        val arrivalBeat = (i - start) * pulseSpeed / rate
                        envelope = max(
                            if (beat < 0.2) (1 - beat / 0.2).pow(5) else 0.0,
                            if (arrivalBeat < 0.2) (1 - arrivalBeat / 0.2).pow(5) else 0.0,
                        )
                        val paired = beat - 0.24 - agitation * 0.035 * sin(tau * driftRate * t + driftPhase)
                        if (paired >= 0 && paired < 0.16) envelope += 0.62 * (1 - paired / 0.16).pow(4)
                    }
                    4 -> frequency *= 0.75
                    5 -> frequency *= 0.28
                }
                phase += tau * frequency / rate
                if (phase > tau) phase -= tau
                val noise = random.next() * 2 - 1
                filteredNoise += 0.08 * (noise - filteredNoise)
                wingNoise += (noise - wingNoise) * (0.16 + 0.32 * (1 - size))
                val turbulence = wingNoise - filteredNoise
                val airflow = 1 + agitation * 0.4 * sin(tau * (driftRate * 1.73) * t + initialPhase)
                val signal = when (kind) {
                    0 -> (sin(phase) + 0.22 * sin(phase * 2) + 0.12 * sin(phase * 3)) * (0.08 + 0.4 * stroke) +
                        (turbulence * 3.4 + filteredNoise * 0.6) * (0.12 + 1.4 * stroke) * airflow
                    1 -> (sin(phase) + 0.2 * sin(phase * 2)) * envelope
                    2 -> {
                        val blade = (0.5 + 0.5 * sin(phase * 3)).pow(5)
                        (0.62 * sin(phase) + 0.28 * sin(phase * 3) + 0.15 * sin(phase * 6)) * (0.75 + 0.25 * wing) +
                            (turbulence * 2.1 + noise * 0.12) * (0.25 + blade) * airflow
                    }
                    3 -> (noise * 0.65 + sin(phase) * 0.5) * envelope
                    4 -> sin(phase) * (0.75 + 0.25 * wing) + 0.12 * sin(phase * 2)
                    else -> (filteredNoise * 2.5 + sin(phase) * 0.25 + noise * 0.07) * (0.7 + 0.3 * wing)
                }
                val distance = (age - center) * 3
                val flyby = 1 / (1 + movement * distance * distance * 4)
                val window = if (kind == 3) {
                    min(1.0, min((i - start) / (rate * 0.002), (frames - 1 - i) / (rate * 0.005)))
                } else {
                    max(0.0, sin(pi * age)).pow(0.7)
                }
                out[i] = (out[i] + signal * window * flyby * voiceGain).toFloat()
            }
        }
        return finish(out, p.value("masterVolume", 0.5))
    }
}
