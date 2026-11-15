package fromcrackpot.kintsugi.bfxr.synths

import fromcrackpot.kintsugi.bfxr.BfxrValue
import fromcrackpot.kintsugi.bfxr.dsp.Mulberry32
import fromcrackpot.kintsugi.bfxr.dsp.RATE
import fromcrackpot.kintsugi.bfxr.dsp.finish
import fromcrackpot.kintsugi.bfxr.dsp.jsRound
import fromcrackpot.kintsugi.bfxr.dsp.value
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

internal object Pluckr {
    private class Matter(
        val life: Double,
        val average: Double,
        val filter: Double,
        val bright: Double,
        val excite: Double,
        val dispersion: Double,
        val stages: Int,
    )

    private class PluckString(
        val buffer: FloatArray,
        stages: Int,
        val fraction: Double,
        val feedback: Double,
        val start: Double,
    ) {
        var index = 0
        var previous = 0.0
        var low = 0.0
        var allpass = 0.0
        var allpassInput = 0.0
        val waveInputs = FloatArray(stages)
        val waveOutputs = FloatArray(stages)
    }

    private val MATTER = listOf(
        Matter(1.0, 0.5, 0.18, 0.8, 1.0, 0.0, 0),
        Matter(1.45, 0.14, 0.65, 0.35, 1.5, 0.2, 1),
        Matter(0.62, 0.55, 0.12, 0.5, 0.6, 0.6, 1),
        Matter(0.075, 0.65, 0.1, 0.28, 0.35, 0.4, 1),
        Matter(1.3, 0.06, 0.85, 0.15, 1.8, -0.72, 2),
        Matter(0.95, 0.25, 0.4, 0.5, 1.1, -0.45, 2),
    )

    private val NOTES = doubleArrayOf(0.0, 7.0, 12.0, 16.0, 19.0, 24.0, 28.0, 31.0)

    fun render(p: Map<String, BfxrValue>): FloatArray {
        val rate = RATE.toDouble()
        val duration = p.value("duration", 1.8, 0.15, 5.0)
        val out = FloatArray(jsRound(duration * rate).toInt())
        val volume = p.value("masterVolume", 0.5)
        if (volume == 0.0) return finish(out, 0.0)
        val random = Mulberry32(p.value("seed", 0.5))
        val count = jsRound(p.value("strings", 3.0, 1.0, 8.0)).toInt()
        val material = jsRound(p.value("material", 0.0, 0.0, 5.0)).toInt()
        val pitch = 55 * 2.0.pow(p.value("pitch", 0.5) * 4)
        val damping = p.value("damping", 0.25)
        val brightness = p.value("brightness", 0.6)
        val coupling = p.value("coupling", 0.15)
        val strum = p.value("strum", 0.2)
        val pluck = p.value("pluck", 0.3)
        val inharmonic = p.value("inharmonic", 0.05)
        val tremolo = p.value("tremolo", 0.0)
        val vibrato = p.value("vibrato", 0.0)
        val speed = p.value("tremoloRate", 4.0, 0.2, 12.0)
        val matter = MATTER[material]
        val gain = 0.65 / sqrt(count.toDouble())
        val strings = ArrayList<PluckString>(count)
        for (s in 0 until count) {
            val frequency = pitch * 2.0.pow(NOTES[s] / 12 + (random.next() - 0.5) * inharmonic * 0.07)
            val period = rate / frequency
            val omega = 2 * PI / period
            val a = matter.dispersion
            val dispersionDelay = matter.stages * 2 * atan2((1 - a) * sin(omega / 2), (1 + a) * cos(omega / 2)) / omega
            val desired = max(2.0, period - matter.average - dispersionDelay)
            val delay = max(2.0, floor(desired))
            val buffer = FloatArray(delay.toInt())
            var low = 0.0
            var mean = 0.0
            val location = 0.05 + pluck * 0.9
            for (i in buffer.indices) {
                low += (random.next() * 2 - 1 - low) * min(1.0, (0.05 + brightness * 0.9) * matter.excite)
                val x = i / delay
                val triangle = if (x < location) x / location else (1 - x) / (1 - location)
                buffer[i] = (low * 0.75 + (triangle - 0.5) * (1 - brightness) * 0.75).toFloat()
                mean += buffer[i]
            }
            mean /= delay
            for (i in buffer.indices) buffer[i] = (buffer[i] - mean).toFloat()
            strings += PluckString(
                buffer,
                matter.stages,
                fraction = (1 - (desired - delay)) / (1 + (desired - delay)),
                feedback = exp(-3 / (frequency * (0.07 + (1 - damping).pow(2) * 4) * matter.life)),
                start = jsRound(s * strum * min(rate * 0.11, out.size.toDouble() / max(1, count) * 0.65)),
            )
        }
        var bridge = 0.0
        for (i in out.indices) {
            var sum = 0.0
            var bridgeNext = 0.0
            val dispersion = matter.dispersion + (if (material == 5) 0.22 * sin(2 * PI * 1.7 * i / rate) else 0.0)
            for (string in strings) {
                if (i < string.start) continue
                val current = string.buffer[string.index].toDouble()
                val averaged = current * (1 - matter.average) + string.previous * matter.average
                string.previous = current
                string.low += (averaged - string.low) * (matter.filter + brightness * matter.bright)
                val input = string.low
                var dispersed = string.fraction * input + string.allpassInput - string.fraction * string.allpass
                string.allpassInput = input
                string.allpass = dispersed
                for (stage in 0 until matter.stages) {
                    val next = dispersion * dispersed + string.waveInputs[stage] - dispersion * string.waveOutputs[stage]
                    string.waveInputs[stage] = dispersed.toFloat()
                    string.waveOutputs[stage] = next.toFloat()
                    dispersed = next
                }
                string.buffer[string.index] =
                    (string.feedback * (dispersed * (1 - coupling * 0.025) + bridge * coupling * 0.025)).toFloat()
                string.index++
                if (string.index == string.buffer.size) string.index = 0
                sum += current
                bridgeNext += current
            }
            bridge = bridgeNext / count
            out[i] = (sum * gain * (if (vibrato > 0) 1.0 else 1 - tremolo * (0.5 - 0.5 * cos(2 * PI * speed * i / rate))))
                .toFloat()
        }
        if (vibrato > 0) {
            val dry = out.copyOf()
            val depth = (2.0.pow(vibrato * 0.75 / 12) - 1) * rate / (2 * PI * speed)
            for (i in out.indices) {
                val motion = 0.5 - 0.5 * cos(2 * PI * speed * i / rate)
                val position = max(0.0, i - depth * 2 * motion)
                val index = floor(position).toInt()
                val fraction = position - index
                out[i] = ((dry[index] * (1 - fraction) + dry[min(index + 1, dry.size - 1)] * fraction) * (1 - tremolo * motion))
                    .toFloat()
            }
        }
        return finish(out, volume)
    }
}
