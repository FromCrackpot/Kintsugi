package fromcrackpot.kintsugi.bfxr.synths

import fromcrackpot.kintsugi.bfxr.BfxrValue
import fromcrackpot.kintsugi.bfxr.dsp.Mulberry32
import fromcrackpot.kintsugi.bfxr.dsp.RATE
import fromcrackpot.kintsugi.bfxr.dsp.finish
import fromcrackpot.kintsugi.bfxr.dsp.jsRound
import fromcrackpot.kintsugi.bfxr.dsp.value
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

internal object Whooshr {
    fun render(p: Map<String, BfxrValue>): FloatArray {
        val rate = RATE.toDouble()
        val duration = p.value("duration", 0.6, 0.1, 5.0)
        val length = jsRound(rate * duration).toInt()
        val out = FloatArray(length)
        val random = Mulberry32(p.value("seed", 0.5))
        val size = p.value("size", 0.4)
        val air = p.value("air", 0.8)
        val whistle = p.value("whistle", 0.25)
        val movement = p.value("movement", 0.7)
        val focus = p.value("focus", 0.6)
        val flutter = p.value("flutter", 0.1)
        val tau = 2 * PI
        val base = 190 * 2.0.pow((1 - size) * 3.1)
        val offset = random.next() * tau
        val beat = 8 + random.next() * 10
        var low = 0.0
        var broad = 0.0
        var phase = offset
        for (i in 0 until length) {
            val u = i.toDouble() / (length - 1)
            val t = i / rate
            val position = (u - 0.5) * 2
            val approach = 1 - movement * 0.7 * position
            val envelope = sin(PI * u).pow(0.6 + focus * 5) / (1 + focus * position * position * 7)
            val gust = 1 - flutter * 0.65 + flutter * 0.65 * sin(tau * beat * t + offset) * sin(tau * beat * t + offset)
            val cutoff = min(12000.0, (350 + 5500 * (1 - size)) * approach)
            val noise = random.next() * 2 - 1
            low += (1 - exp(-tau * cutoff / rate)) * (noise - low)
            broad += (1 - exp(-tau * cutoff * 0.14 / rate)) * (noise - broad)
            phase += tau * base * approach * (1 + flutter * 0.025 * sin(tau * beat * t)) / rate
            if (phase > tau) phase -= tau
            out[i] = (((low - broad) * air * 2.7 + (sin(phase) + 0.12 * sin(phase * 2)) * whistle * 0.45) *
                envelope * gust).toFloat()
        }
        return finish(out, p.value("masterVolume", 0.5))
    }
}
