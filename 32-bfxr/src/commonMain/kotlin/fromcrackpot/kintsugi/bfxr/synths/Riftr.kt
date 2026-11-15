package fromcrackpot.kintsugi.bfxr.synths

import fromcrackpot.kintsugi.bfxr.BfxrValue
import fromcrackpot.kintsugi.bfxr.dsp.Mulberry32
import fromcrackpot.kintsugi.bfxr.dsp.RATE
import fromcrackpot.kintsugi.bfxr.dsp.finish
import fromcrackpot.kintsugi.bfxr.dsp.jsRound
import fromcrackpot.kintsugi.bfxr.dsp.value
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

internal object Riftr {
    private class Line(val buffer: FloatArray, val delay: Double, val depth: Double, var real: Double, var imag: Double, val c: Double, val s: Double) {
        var index = 0
        var filter = 0.0
        var read = 0.0
    }

    private class Allpass(val buffer: FloatArray) {
        var index = 0
    }

    private val RATIOS = doubleArrayOf(0.719, 1.0, 1.337, 1.731)

    fun render(p: Map<String, BfxrValue>): FloatArray {
        val rate = RATE.toDouble()
        val random = Mulberry32(p.value("seed", 0.5))
        val duration = p.value("duration", 1.8, 0.15, 6.0)
        val count = jsRound(duration * rate).toInt()
        val output = FloatArray(count)
        val excitation = jsRound(p.value("excitation", 0.0, 0.0, 4.0)).toInt()
        val volume = p.value("masterVolume", 0.5)
        if (volume == 0.0) return finish(output, 0.0)
        val pitch = p.value("pitch", 0.45)
        val bend = p.value("bend", -0.2, -1.0, 1.0)
        val space = p.value("space", 0.5)
        val feedback = p.value("feedback", 0.65) * 0.955
        val dispersion = p.value("dispersion", 0.55)
        val motion = p.value("motion", 0.3)
        val field = p.value("field", 0.7)
        val reverse = p.value("reverse", 0.0)
        val tau = 2 * PI
        val basePitch = 55 * 2.0.pow(pitch * 5.8)
        val pitchStep = 2.0.pow(bend * 3 / count)
        val baseDelay = (0.003 + space * space * 0.095) * rate
        val lines = RATIOS.mapIndexed { index, ratio ->
            val delay = max(7.0, baseDelay * ratio)
            val depth = motion * delay * 0.09
            val step = tau * (0.17 + motion * 2.3) * (1 + index * 0.13) / rate
            val phase = random.next() * tau
            Line(FloatArray(ceil(delay + depth + 3).toInt()), delay, depth, cos(phase), sin(phase), cos(step), sin(step))
        }
        val allpasses = doubleArrayOf(0.0023, 0.0051, 0.0113, 0.0197).map { seconds ->
            Allpass(FloatArray(max(1.0, jsRound(seconds * rate * (0.3 + space) * (0.1 + dispersion))).toInt()))
        }
        val apGain = 0.15 + dispersion * 0.59
        val damping = 0.48 + (1 - dispersion) * 0.4
        val pulseRadius = exp(-1 / (rate * (0.015 + space * 0.025)))
        val tearRadius = exp(-1 / (rate * (0.025 + duration * 0.16)))
        var phase = 0.0
        var increment = tau * basePitch / rate
        var pulse = 1.0
        var tear = 1.0
        var noiseLow = 0.0
        val phaseOffset = random.next() * tau
        var packet = 0.0
        var packetEnvelope = 0.0
        val packetStep = (8 + motion * 28) / rate
        val packetRadius = exp(-1 / (rate * 0.013))
        for (i in 0 until count) {
            val progress = i.toDouble() / (count - 1)
            phase += increment
            increment *= pitchStep
            val noise = random.next() * 2 - 1
            noiseLow += (noise - noiseLow) * 0.12
            val carrier = sin(phase + 0.55 * sin(phase * 1.417 + phaseOffset))
            val input = when (excitation) {
                1 -> {
                    val arc = max(0.0, sin(PI * min(1.0, progress / 0.72)))
                    (carrier * 0.65 + sin(phase * 0.498) * 0.28 + noiseLow * 0.12) * arc * arc
                }
                2 -> (carrier * 0.48 + sin(phase * 0.501) * 0.32) *
                    sin(PI * progress) * (0.65 + 0.35 * sin(tau * progress * (2 + motion * 7)))
                3 -> (noiseLow * 0.95 + carrier * 0.36 + sin(phase * 0.25) * 0.21) * tear
                4 -> {
                    packet += packetStep
                    if (packet >= 1 || i == 0) {
                        packet %= 1
                        packetEnvelope = 0.45 + random.next() * 0.5
                    }
                    val value = (sin(phase + floor(progress * 13) * 1.7) + noise * 0.12) * packetEnvelope * (1 - progress)
                    packetEnvelope *= packetRadius
                    value
                }
                else -> (carrier * 0.9 + sin(phase * 2.071) * 0.18 + noiseLow * 0.04) * pulse
            }
            pulse *= pulseRadius
            tear *= tearRadius
            var sum = 0.0
            for (line in lines) {
                val newReal = line.real * line.c - line.imag * line.s
                line.imag = line.real * line.s + line.imag * line.c
                line.real = newReal
                var position = line.index - line.delay - line.depth * line.imag
                if (position < 0) position += line.buffer.size
                val before = floor(position).toInt()
                val fraction = position - before
                val after = if (before + 1 == line.buffer.size) 0 else before + 1
                val sample = line.buffer[before] * (1 - fraction) + line.buffer[after] * fraction
                line.filter += (sample - line.filter) * damping
                line.read = line.filter
                sum += line.read
            }
            for (line in lines) {
                line.buffer[line.index] = (input * 0.5 + feedback * (line.read - sum * 0.5)).toFloat()
                if (++line.index == line.buffer.size) line.index = 0
            }
            var wet = sum * 0.65
            if (dispersion > 0) {
                for (stage in allpasses) {
                    val delayed = stage.buffer[stage.index]
                    val scattered = delayed - apGain * wet
                    stage.buffer[stage.index] = (wet + apGain * scattered).toFloat()
                    if (++stage.index == stage.buffer.size) stage.index = 0
                    wet = scattered
                }
            }
            output[i] = ((1 - field) * input + field * wet).toFloat()
        }
        if (reverse > 0) {
            for (i in 0 until count / 2) {
                val opposite = count - 1 - i
                val a = output[i].toDouble()
                val b = output[opposite].toDouble()
                output[i] = (a * (1 - reverse) + b * reverse).toFloat()
                output[opposite] = (b * (1 - reverse) + a * reverse).toFloat()
            }
        }
        val rendered = finish(output, volume)
        var peak = 0.0
        for (sample in rendered) peak = max(peak, abs(sample.toDouble()))
        if (peak > volume * 0.00001) {
            val gain = min(4.0, max(1.0, volume * 0.9 / peak))
            if (gain > 1) for (i in rendered.indices) rendered[i] = (rendered[i] * gain).toFloat()
        }
        return rendered
    }
}
