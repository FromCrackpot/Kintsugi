package fromcrackpot.kintsugi.bfxr.synths

import fromcrackpot.kintsugi.bfxr.BfxrValue
import fromcrackpot.kintsugi.bfxr.dsp.Mulberry32
import fromcrackpot.kintsugi.bfxr.dsp.RATE
import fromcrackpot.kintsugi.bfxr.dsp.finish
import fromcrackpot.kintsugi.bfxr.dsp.jsRound
import fromcrackpot.kintsugi.bfxr.dsp.value
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

internal object Signlr {
    fun render(p: Map<String, BfxrValue>): FloatArray {
        val rate = RATE.toDouble()
        val tau = 2 * PI
        val duration = p.value("duration", 1.4, 0.15, 5.0)
        val output = FloatArray(jsRound(duration * rate).toInt())
        val volume = p.value("masterVolume", 0.5)
        if (volume == 0.0) return finish(output, 0.0)
        val seed = p.value("seed", 0.5)
        val random = Mulberry32(seed)
        val encoding = jsRound(p.value("encoding", 0.0, 0.0, 4.0)).toInt()
        if (encoding == 4) return pings(p)
        val carrier = 90 * 2.0.pow(p.value("carrier", 0.55) * 5.7)
        val deviation = p.value("deviation", 0.35)
        val drift = p.value("drift", 0.0, -1.0, 1.0)
        val interference = p.value("interference", 0.08)
        val corruption = p.value("corruption", 0.08)
        val echo = p.value("echo", 0.15)
        val symbols = 4 + 176 * p.value("symbols", 0.4).pow(2)
        val symbolSamples = rate / symbols
        val packets = jsRound(p.value("packets", 3.0, 1.0, 12.0))
        val slot = output.size / packets
        val active = slot * (1 - p.value("gap", 0.25, 0.0, 0.85))
        val edge = min(rate * 0.005, active * 0.12)
        val symbolEdge = min(rate * 0.0007, symbolSamples * 0.06)
        val delayA = jsRound(rate * (0.073 + seed * 0.041)).toInt()
        val delayB = jsRound(delayA * 1.79).toInt()
        var phase = random.next() * tau
        var lastSymbol = -1.0
        var lastPacket = -1.0
        var bit = 1.0
        var symbolGain = 1.0
        var noiseLow = 0.0
        var radioLow = 0.0
        var phaseCode = 0.0
        var scramble = 0.0
        var carrierNow = carrier * 2.0.pow(-drift * 0.5)
        val driftStep = 2.0.pow(drift / output.size)
        val heterodyneStep = tau * (carrier * 1.013 + 37) / rate
        var heterodyne = random.next() * tau
        for (i in output.indices) {
            val packet = min(packets - 1, floor(i / slot))
            val local = i - packet * slot
            val symbol = floor(local / symbolSamples)
            val symbolPosition = local - symbol * symbolSamples
            val unit = symbolPosition / symbolSamples
            if (packet != lastPacket || symbol != lastSymbol) {
                lastPacket = packet
                lastSymbol = symbol
                bit = if (symbol < 4) (if ((symbol + packet) % 2 != 0.0) 1.0 else -1.0)
                else (if (random.next() < 0.5) -1.0 else 1.0)
                symbolGain = if (random.next() < corruption * 0.85) 0.025 else 1.0
                phaseCode = if (bit > 0) 0.0 else PI
                scramble = corruption * (random.next() * 2 - 1)
            }
            var frequency = carrierNow
            if (encoding == 0) frequency *= 2.0.pow(deviation * bit * 0.7 + scramble * 0.2)
            if (encoding == 2) frequency *= 2.0.pow(deviation * (1 - unit * 2) + scramble * 0.2)
            if (encoding == 3) frequency *= 1 + deviation * 0.3 * bit
            phase += tau * min(14000.0, max(30.0, frequency)) / rate
            carrierNow *= driftStep
            heterodyne += heterodyneStep
            val noise = random.next() * 2 - 1
            noiseLow += (noise - noiseLow) * 0.08
            val encoded = when (encoding) {
                1 -> sin(phase + phaseCode * (0.25 + deviation * 0.75))
                2 -> sin(phase) * exp(-unit * (5 + deviation * 5))
                3 -> sin(phase) * (0.62 + 0.38 * sin(phase * 0.037 + bit))
                else -> sin(phase) + 0.08 * deviation * sin(phase * 2)
            }
            val packetEnvelope = max(0.0, min(1.0, min(local / edge, (active - local) / edge)))
            val bitEnvelope = if (encoding == 2) {
                min(1.0, symbolPosition / symbolEdge)
            } else {
                0.88 + 0.12 * min(1.0, min(symbolPosition / symbolEdge, (symbolSamples - symbolPosition) / symbolEdge))
            }
            val radio = (noise - noiseLow) * 0.22 + sin(heterodyne) * 0.14
            val squelch = 0.4 + 0.6 * packetEnvelope
            var sample = encoded * 0.52 * symbolGain * packetEnvelope * bitEnvelope + radio * interference * squelch
            radioLow += (sample - radioLow) * 0.3
            if (encoding == 3) sample = radioLow
            if (i >= delayA) sample += output[i - delayA] * echo * 0.38
            if (i >= delayB) sample -= output[i - delayB] * echo * 0.17
            output[i] = sample.toFloat()
        }
        return finish(output, volume)
    }

    private fun pings(p: Map<String, BfxrValue>): FloatArray {
        val rate = RATE.toDouble()
        val tau = PI * 2
        val duration = p.value("duration", 1.4, .15, 5.0)
        val packets = jsRound(p.value("packets", 2.0, 1.0, 12.0))
        val out = FloatArray(jsRound(duration * rate).toInt())
        val rng = Mulberry32(p.value("seed", .5))
        val root = 170 * 2.0.pow(p.value("carrier", .55) * 4.1)
        val color = p.value("deviation", .15)
        val fall = p.value("gap", .5, 0.0, .85)
        val echo = p.value("echo", .4)
        val drift = p.value("drift", 0.0, -1.0, 1.0)
        val ratios = doubleArrayOf(1.0, 2.01 + rng.next() * .12, 3.1 + rng.next() * 1.7, 5.2 + rng.next() * 2.2)
        val weights = doubleArrayOf(.8, .06 + color * .28, .03 + color * .17, .02 + color * .1)
        val ring = .045 + (1 - fall) * .6
        val step = duration / packets
        val phases = DoubleArray(ratios.size) { rng.next() * tau }
        for (packet in 0 until packets.toInt()) {
            val start = jsRound(packet * step * rate).toInt()
            val frequency = root * 2.0.pow(drift * packet / max(1.0, packets - 1))
            var i = 0
            while (i < min(rate * (ring * 6), (out.size - start).toDouble())) {
                val t = i / rate
                val attack = 1 - exp(-t / .0007)
                var sample = 0.0
                for (h in ratios.indices) {
                    if (frequency * ratios[h] > 18000) continue
                    val phase = tau * frequency * ratios[h] * (t + color * .0008 * (1 - exp(-t / .006)))
                    sample += sin(phase + phases[h]) * weights[h] * exp(-t * (1 + h * 1.4) / ring)
                }
                sample *= attack * .7
                out[start + i] = (out[start + i] + sample).toFloat()
                for (repeat in 1..3) {
                    val target = start + i + jsRound(rate * (.062 + p.value("seed", .5) * .055) * repeat).toInt()
                    if (target < out.size) out[target] = (out[target] + sample * (echo * .52).pow(repeat)).toFloat()
                }
                i++
            }
        }
        return finish(out, p.value("masterVolume", .5))
    }
}
