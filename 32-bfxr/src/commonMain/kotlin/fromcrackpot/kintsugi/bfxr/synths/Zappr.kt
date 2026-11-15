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

internal object Zappr {
    private class Event(val time: Double, val life: Double, val frequency: Double, val gain: Double)

    fun render(p: Map<String, BfxrValue>): FloatArray {
        val rate = RATE.toDouble()
        val duration = p.value("duration", 1.2, 0.12, 5.0)
        val frames = jsRound(duration * rate).toInt()
        val out = FloatArray(frames)
        val arcs = jsRound(p.value("arcs", 7.0, 1.0, 24.0)).toInt()
        val voltage = p.value("voltage", 0.55)
        val branching = p.value("branching", 0.4)
        val crackle = p.value("crackle", 0.45)
        val hum = p.value("hum", 0.2)
        val spark = p.value("spark", 0.65)
        val spread = p.value("spread", 0.7)
        val decay = p.value("decay", 0.4)
        val random = Mulberry32(p.value("seed", 0.5))
        val tau = 2 * PI
        val humFrequency = 42 + voltage * 65
        val events = ArrayList<Event>()
        for (arc in 0 until arcs) {
            val position = if (arc == 0) 0.004 else (arc + random.next() * 0.65) / arcs * duration * (0.03 + spread * 0.86)
            val life = 0.008 + decay * 0.1
            val frequency = 400 + voltage * 4800 * (0.6 + random.next() * 0.8)
            events += Event(position, life, frequency, 0.55 + random.next() * 0.4)
            val branches = jsRound(branching * (2 + random.next() * 5)).toInt()
            for (branch in 0 until branches) {
                events += Event(
                    position + life * (0.3 + random.next() * 2),
                    life * (0.18 + random.next() * 0.55),
                    frequency * (0.5 + random.next() * 1.2),
                    branching * (0.15 + random.next() * 0.25),
                )
            }
        }
        var phase = 0.0
        var crackleEnv = 0.0
        var noiseLow = 0.0
        val crackleFall = exp(-1 / (rate * (0.001 + decay * 0.014)))
        for (i in 0 until frames) {
            val t = i / rate
            val noise = random.next() * 2 - 1
            noiseLow += 0.075 * (noise - noiseLow)
            phase += tau * humFrequency / rate
            if (phase > tau) phase -= tau
            if (random.next() < crackle * (12 + voltage * 130) / rate) crackleEnv = 0.15 + random.next() * 0.55
            crackleEnv *= crackleFall
            val field = hum * (sin(phase) + 0.28 * sin(phase * 3) + 0.15 * sin(phase * 7)) * 0.27
            out[i] = ((field + crackleEnv * (noise - noiseLow) * crackle) * min(1.0, t / 0.006) *
                max(0.0, 1 - t / duration).pow(0.2 + decay * 0.6)).toFloat()
        }
        for (event in events) {
            val start = jsRound(event.time * rate).toInt()
            val length = min((frames - start).toDouble(), ceil(event.life * rate * 6)).toInt()
            var arcPhase = random.next() * tau
            var low = 0.0
            for (j in 0 until length) {
                val age = j / rate
                val noise = random.next() * 2 - 1
                arcPhase += tau * event.frequency * (0.3 + 0.7 * exp(-age / event.life)) / rate
                if (arcPhase > tau) arcPhase -= tau
                low += 0.2 * (noise - low)
                val discharge = (0.4 + spark * 0.5) * (noise - low) +
                    sin(arcPhase + sin(arcPhase * 0.47) * voltage * 2) * (1 - spark * 0.65)
                val restrike = 0.65 + 0.35 * max(0.0, sin(tau * age * (75 + voltage * 290)))
                out[start + j] = (out[start + j] + discharge * event.gain * exp(-age / event.life) * restrike *
                    min(1.0, j / 12.0)).toFloat()
            }
        }
        return finish(out, p.value("masterVolume", 0.5))
    }
}
