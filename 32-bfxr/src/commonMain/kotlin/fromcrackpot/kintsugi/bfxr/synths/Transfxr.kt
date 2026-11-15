package fromcrackpot.kintsugi.bfxr.synths

import fromcrackpot.kintsugi.bfxr.BfxrValue
import fromcrackpot.kintsugi.bfxr.dsp.AKWF_FMSYNTH_0012
import fromcrackpot.kintsugi.bfxr.dsp.AKWF_GRANULAR_0044
import fromcrackpot.kintsugi.bfxr.dsp.AKWF_HVOICE_0012
import fromcrackpot.kintsugi.bfxr.dsp.RATE
import fromcrackpot.kintsugi.bfxr.dsp.jsRound
import fromcrackpot.kintsugi.bfxr.dsp.toInt32
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tan
import kotlin.math.tanh

internal object Transfxr {
    private val WAVEFORMS = intArrayOf(2, 4, 1, 0, 8, 6, 7, 3, 11, 9, 5, 10)
    private val LINEAR: (Double) -> Double = { t -> t }

    private val CURVES: Map<String, (Double) -> Double> = mapOf(
        "Linear" to LINEAR,
        "Ease In" to { t -> t * t },
        "Ease Out" to { t -> 1 - (1 - t) * (1 - t) },
        "Smooth" to { t -> t * t * (3 - 2 * t) },
        "Triangle" to { t -> 1 - abs(2 * t - 1) },
        "Pulse" to { t -> (1 - cos(2 * PI * t)) / 2 },
        "Bounce" to ::bounce,
        "Steps" to { t -> min(1.0, floor(t * 5) / 4) },
    )

    private fun bounce(x: Double): Double {
        var t = x
        if (t < 1 / 2.75) return 7.5625 * t * t
        if (t < 2 / 2.75) {
            t -= 1.5 / 2.75
            return 7.5625 * t * t + 0.75
        }
        if (t < 2.5 / 2.75) {
            t -= 2.25 / 2.75
            return 7.5625 * t * t + 0.9375
        }
        t -= 2.625 / 2.75
        return 7.5625 * t * t + 0.984375
    }

    private fun frequency(value: Double): Double = 40 * 2.0.pow(value * 7)

    private fun cutoff(value: Double): Double = 100 * 160.0.pow(value)

    private fun transition(value: BfxrValue.Transition): (Double) -> Double {
        val curve = CURVES[value.curve] ?: LINEAR
        return { t -> value.start + (value.end - value.start) * curve(t) }
    }

    private fun Map<String, BfxrValue>.number(name: String): Double = (getValue(name) as BfxrValue.Number).value

    private fun Map<String, BfxrValue>.transition(name: String): BfxrValue.Transition =
        getValue(name) as BfxrValue.Transition

    private fun waveformOf(index: Double): Waveform = Waveform(WAVEFORMS.getOrElse(index.toInt()) { 2 })

    fun render(p: Map<String, BfxrValue>): FloatArray {
        val rate = RATE.toDouble()
        val duration = p.number("duration")
        val count = max(2.0, jsRound(duration * rate)).toInt()
        val delay = jsRound(min(0.24, max(0.075, duration * 0.23)) * rate).toInt()
        val feedback = p.number("echo") * 0.65
        val repeats = if (feedback > 0) ceil(ln(0.0001) / ln(feedback)).toInt() else 0
        val tail = repeats * delay
        val output = FloatArray(count + tail)
        val controls = listOf("pitch", "tone", "vibrato", "level")
        val envelopes = controls.map { transition(p.transition(it)) }
        val values = DoubleArray(controls.size) { p.transition(controls[it]).start }
        val attack = max(0.003, p.number("attack")) * rate
        val release = max(0.006, p.number("release")) * rate
        val damping = 1 / (0.707 + p.number("resonance") * 5)
        val waveform = waveformOf(p.number("waveType"))
        val waveTo = p.number("waveTo")
        val destination = if (waveTo >= 0 && waveTo == floor(waveTo)) waveformOf(waveTo) else null
        val morphValue = p.transition("morph")
        val morph = transition(morphValue)
        var blend = morphValue.start
        var phase = 0.0
        var ic1 = 0.0
        var ic2 = 0.0
        for (i in 0 until count) {
            val t = i.toDouble() / (count - 1)
            if (destination != null) blend += (morph(t) - blend) * .012
            for (j in values.indices) values[j] += (envelopes[j](t) - values[j]) * 0.012
            val pitch = values[0]
            val tone = values[1]
            val vibrato = values[2]
            val level = values[3]
            val frequency = frequency(pitch) * 2.0.pow(sin(i / rate * PI * 16) * vibrato * 0.16)
            val step = frequency / (rate * 2)
            val g = tan(PI * cutoff(tone) / (rate * 2))
            val a1 = 1 / (1 + g * (g + damping))
            var sample = 0.0
            repeat(2) {
                val source = waveform(phase, step)
                val osc = if (destination != null) source * (1 - blend) + destination(phase, step) * blend else source
                phase = (phase + step) % 1
                val v1 = a1 * (ic1 + g * (osc - ic2))
                val v2 = ic2 + g * v1
                ic1 = 2 * v1 - ic1
                ic2 = 2 * v2 - ic2
                sample += v2 * 0.5
            }
            val fade = min(1.0, i / attack) * min(1.0, (count - 1 - i) / release)
            output[i] = (tanh(sample * 1.4) * level * fade).toFloat()
        }
        for (i in output.indices) {
            if (i >= delay) output[i] = (output[i] + output[i - delay] * feedback).toFloat()
        }
        val volume = p.number("masterVolume")
        for (i in output.indices) {
            val fade = if (tail != 0) min(1.0, (output.size - 1 - i) / 256.0) else 1.0
            output[i] = (tanh(output[i].toDouble()) * volume * 0.95 * fade).toFloat()
        }
        return output
    }

    private fun blep(phase: Double, step: Double): Double {
        if (phase < step) {
            val t = phase / step
            return t + t - t * t - 1
        }
        if (phase > 1 - step) {
            val t = (phase - 1) / step
            return t * t + t + t + 1
        }
        return 0.0
    }

    private class Waveform(private val type: Int) {
        private var state = toInt32(jsRound(0.5 * 4294967295.0)).let { if (it == 0) 1 else it }
        private var previous = 1.0
        private var cell = -1.0
        private var white = 0.0
        private var bits = ((state xor 0x4a35) and 0x7fff).let { if (it == 0) 1 else it }
        private var bit = 0.5
        private val table = when (type) {
            5 -> AKWF_GRANULAR_0044
            10 -> AKWF_FMSYNTH_0012
            11 -> AKWF_HVOICE_0012
            else -> null
        }

        private fun random(): Double {
            state = state xor (state shl 13)
            state = state xor (state ushr 17)
            state = state xor (state shl 5)
            return state.toUInt().toDouble() / 2147483648.0 - 1
        }

        operator fun invoke(phase: Double, step: Double): Double {
            if (phase < previous) {
                cell = -1.0
                val feed = ((bits shr 1) and 1) xor (bits and 1)
                bits = (bits shr 1) or (feed shl 14)
                bit = (bits.inv() and 1) - 0.5
            }
            previous = phase
            if (table != null) {
                val pos = phase * 256
                val i = floor(pos).toInt()
                return (table[i] + (table[(i + 1) % 256] - table[i]) * (pos - i)) / 32768 - 1
            }
            return when (type) {
                0 -> (if (phase < 0.5) 1.0 else -1.0) + blep(phase, step) - blep((phase + 0.5) % 1, step)
                1 -> 2 * phase - 1 - blep(phase, step)
                3 -> {
                    val next = floor(phase * 32)
                    if (next != cell) {
                        cell = next
                        white = random()
                    }
                    white
                }
                4 -> 1 - 4 * abs(phase - 0.5)
                6 -> max(-3.0, min(3.0, tan(PI * phase))) / 3
                7 -> .75 * sin(phase * 2 * PI) + (if (step * 20 < .5) .25 * sin(phase * 40 * PI) else 0.0)
                8 -> 2 * (abs(1 - 2 * phase * phase) - .609475708)
                9 -> bit
                else -> sin(phase * 2 * PI)
            }
        }
    }
}
