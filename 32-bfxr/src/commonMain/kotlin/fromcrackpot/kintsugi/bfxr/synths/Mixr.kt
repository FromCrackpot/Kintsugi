package fromcrackpot.kintsugi.bfxr.synths

import fromcrackpot.kintsugi.bfxr.BfxrValue
import fromcrackpot.kintsugi.bfxr.dsp.RATE
import fromcrackpot.kintsugi.bfxr.dsp.clamp
import fromcrackpot.kintsugi.bfxr.dsp.value
import fromcrackpot.kintsugi.bfxr.readSources
import fromcrackpot.kintsugi.bfxr.renderSamples
import kotlin.math.max
import kotlin.math.min

internal object Mixr {
    fun render(p: Map<String, BfxrValue>): FloatArray {
        val sources = readSources((p.getValue("sources") as BfxrValue.Text).value, "params.sources", mutableListOf())
        val seed = (p.getValue("seed") as BfxrValue.Number).value
        val pcm = sources.map { source -> source?.let { renderSamples(it.synth, it.params, it.renderSeed ?: seed) } }
        val balance = p.value("balance", 0.5)
        val volume = p.value("masterVolume", 0.5)
        val out = FloatArray(min(RATE * 12, max(4410, pcm.maxOfOrNull { it?.size ?: 0 } ?: 0)))
        val both = pcm.getOrNull(0) != null && pcm.getOrNull(1) != null
        for (slot in pcm.indices) {
            val samples = pcm[slot] ?: continue
            val gain = (if (both) (if (slot == 0) 1 - balance else balance) else 1.0) * volume * 2
            var i = 0
            while (i < samples.size && i < out.size) {
                out[i] = (out[i] + samples[i] * gain).toFloat()
                i++
            }
        }
        for (i in out.indices) {
            out[i] = if (out[i].isFinite()) clamp(out[i].toDouble(), -1.0, 1.0).toFloat() else 0f
            if (i > out.size - 128) out[i] = (out[i] * ((out.size - 1 - i) / 127.0)).toFloat()
        }
        return out
    }
}
