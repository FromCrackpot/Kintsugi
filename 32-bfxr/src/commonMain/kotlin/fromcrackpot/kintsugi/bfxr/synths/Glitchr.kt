package fromcrackpot.kintsugi.bfxr.synths

import fromcrackpot.kintsugi.bfxr.BfxrValue
import fromcrackpot.kintsugi.bfxr.dsp.Mulberry32
import fromcrackpot.kintsugi.bfxr.dsp.RATE
import fromcrackpot.kintsugi.bfxr.dsp.finish
import fromcrackpot.kintsugi.bfxr.dsp.jsHypot
import fromcrackpot.kintsugi.bfxr.dsp.jsRound
import fromcrackpot.kintsugi.bfxr.dsp.toInt32
import fromcrackpot.kintsugi.bfxr.dsp.value
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

internal object Glitchr {
    private class Grain(val length: Double, var head: Double, val speed: Double) {
        var age = 0.0
    }

    private class Partial(var phase: Double, var frequency: Double, var gain: Double)

    private val NOTES = doubleArrayOf(1.0, 1.25, 0.75, 1.5, 1.125)

    fun render(p: Map<String, BfxrValue>): FloatArray {
        val tau = PI * 2
        val rate = RATE.toDouble()
        val duration = p.value("duration", 0.7, 0.08, 4.0)
        val out = FloatArray(jsRound(duration * rate).toInt())
        val random = Mulberry32(p.value("seed", 0.5))
        val mode = jsRound(p.value("mode", 0.0, 0.0, 7.0)).toInt()
        val base = 80 * 2.0.pow(p.value("pitch", 0.5) * 4)
        val fragment = p.value("fragment", 0.3)
        val repeat = p.value("repeat", 0.4)
        val chaos = p.value("chaos", 0.5)
        val dropout = p.value("dropout", 0.2)
        val crush = p.value("crush", 0.3)
        val rateLoss = p.value("rate", 0.3)
        val chunk = max(80.0, jsRound((0.009 + fragment * 0.18) * rate)).toInt()
        val hold = 1 + jsRound(rateLoss * 27).toInt()
        val steps = 2.0.pow(13 - jsRound(crush * 11))
        val source = FloatArray(jsRound(rate * 0.83).toInt())
        var sourceLow = 0.0
        for (i in source.indices) {
            val t = i / rate
            val note = floor(t / 0.137)
            val local = t - note * 0.137
            val f = base * NOTES[note.toInt() % NOTES.size]
            val noise = random.next() * 2 - 1
            sourceLow += (noise - sourceLow) * 0.12
            val pluck = (sin(tau * f * local) + 0.35 * sin(tau * f * 2.01 * local) + 0.18 * sin(tau * f * 3.97 * local)) *
                exp(-local * 18)
            val transient = (noise - sourceLow) * exp(-local * 120) * 0.25
            source[i] = ((pluck * 0.55 + transient + sourceLow * 0.24) * min(1.0, local / 0.001)).toFloat()
        }

        fun read(position: Double): Double {
            val size = source.size.toDouble()
            val wrapped = ((position % size) + size) % size
            val index = floor(wrapped).toInt()
            val mix = wrapped - index
            return source[index] * (1 - mix) + source[(index + 1) % source.size] * mix
        }

        var head = 0.0
        var anchor = 0.0
        var speed = 1.0
        var skip = false
        var sample = 0.0
        var previous = 0.0
        var dc = 0.0
        var scrubLow = 0.0
        var dataPhase = 0.0
        var dataBit = 0
        var dataCount = 0.0
        var shift = 1 + floor(random.next() * 65534).toInt()
        var bandLow = 0.0
        var bandMid = 0.0
        var bandHigh = 0.0
        var packetGain = doubleArrayOf(1.0, 1.0, 1.0, 1.0)
        val grains = ArrayList<Grain>()
        val frozen = List(6) { n -> Partial(random.next() * tau, base * (1 + n * 0.51), 0.0) }
        var grainCountdown = 0.0
        var grainOrigin = 0.0
        for (i in out.indices) {
            val t = i / rate
            val j = i % chunk
            if (j == 0) {
                val fresh = i == 0 || random.next() > repeat
                skip = i > 0 && random.next() < dropout * 0.86
                if (fresh) {
                    anchor = floor(random.next() * (source.size - chunk))
                    speed = 2.0.pow((random.next() - 0.5) * chaos * 2.4)
                    grainOrigin = anchor
                }
                if (mode == 0 || mode == 6) head = anchor
                if (mode == 2) {
                    if (fresh) head = anchor
                    speed = (if (random.next() < 0.55) -1 else 1) * (0.2 + random.next() * 2.8) * (0.4 + chaos)
                }
                packetGain = DoubleArray(packetGain.size) { 0.25 + random.next() * 1.2 }
                if (mode == 7 && fresh) {
                    for (partial in frozen) {
                        partial.frequency = min(9000.0, base * (0.5 + jsRound(random.next() * 11) / 4))
                        var real = 0.0
                        var imaginary = 0.0
                        for (k in 0 until 256) {
                            val captured = read(anchor + k)
                            val phase = tau * partial.frequency * k / rate
                            real += captured * cos(phase)
                            imaginary += captured * sin(phase)
                        }
                        partial.gain = 0.025 + min(0.22, jsHypot(real, imaginary) / 128)
                    }
                }
            }
            var value = 0.0
            when (mode) {
                0 -> {
                    value = read(head)
                    head += speed
                    if (head >= anchor + chunk) head = anchor
                    value *= min(1.0, min(j / 16.0, (chunk - j) / 16.0))
                }
                1 -> {
                    val captured = read(head)
                    head += speed * (1 + chaos * 0.45 * sin(tau * (8 + fragment * 25) * t))
                    bandLow += (captured - bandLow) * 0.018
                    bandMid += (captured - bandMid) * 0.11
                    bandHigh += (captured - bandHigh) * 0.43
                    val bands = doubleArrayOf(bandLow, bandMid - bandLow, bandHigh - bandMid, captured - bandHigh)
                    for (band in 0 until 4) {
                        val resolution = 2.0.pow(3 + band - (crush * 2))
                        val coded = jsRound(bands[band] * resolution) / resolution
                        value += coded * packetGain[band] * (0.7 + 0.3 * sin(tau * (11 + band * 7 + chaos * 13) * t + band))
                    }
                    value = value * 1.7 + sin(tau * base * 1.43 * t) * abs(bandMid - bandLow) * chaos * 0.6
                }
                2 -> {
                    head += speed * (1 + chaos * 0.8 * sin(tau * (1.5 + fragment * 5) * t))
                    val captured = read(head)
                    scrubLow += (captured - scrubLow) * min(0.95, 0.07 + abs(speed) * 0.19)
                    value = scrubLow * 1.5 + (random.next() * 2 - 1) * 0.018 * chaos
                }
                3 -> {
                    val decimation = 2 + jsRound(rateLoss * 95 + crush * 25 * (i.toDouble() / out.size)).toInt()
                    if (i % decimation == 0) {
                        val levels = 2.0.pow(8 - jsRound(crush * 6))
                        val captured = read(head)
                        var word = jsRound((captured + 1) * levels)
                        if (random.next() < chaos * 0.22) {
                            val bit = floor(random.next() * max(1.0, jsRound(crush * 7))).toInt()
                            word = (toInt32(word) xor (1 shl bit)).toDouble()
                        }
                        previous = word / levels - 1
                    }
                    head += 0.65 + speed * 0.45
                    value = previous * 0.85
                }
                4 -> {
                    val baud = jsRound(rate / (100 + fragment * 1700))
                    if (dataCount-- <= 0) {
                        shift = ((shift ushr 1) xor ((-(shift and 1)) and 0xB400)) and 65535
                        dataBit = shift and 1
                        dataCount = baud
                    }
                    dataPhase += tau * min(rate * 0.42, base * (if (dataBit != 0) 7.3 + chaos else 4.17)) / rate
                    val carrier = sin(dataPhase)
                    value = (if (carrier > 0) 1 else -1) * 0.30 + carrier * 0.22 + (dataBit * 2 - 1) * 0.08
                }
                5 -> {
                    if (grainCountdown-- <= 0) {
                        val length = max(100.0, jsRound(chunk * (0.6 + random.next() * 0.9)))
                        grains += Grain(
                            length,
                            grainOrigin + (random.next() - 0.5) * source.size * chaos,
                            2.0.pow((random.next() - 0.5) * chaos * 3) * (if (random.next() < chaos * 0.5) -1 else 1),
                        )
                        grainCountdown = max(30.0, jsRound(length / (2.5 + repeat * 2)))
                    }
                    for (n in grains.indices.reversed()) {
                        val grain = grains[n]
                        val window = 0.5 - 0.5 * cos(tau * grain.age / grain.length)
                        value += read(grain.head) * window * 0.8
                        grain.head += grain.speed
                        if (++grain.age >= grain.length) grains.removeAt(n)
                    }
                }
                6 -> {
                    val phrase = max(55.0, jsRound(chunk / (1 + jsRound(repeat * 5))))
                    val position = j % phrase
                    value = read(anchor + position * (0.65 + speed * 0.35)) *
                        min(1.0, min(position / 12, (phrase - position) / 12))
                    value *= 0.6 + 0.4 * exp(-position / (phrase * 0.25))
                }
                else -> {
                    for (partial in frozen) {
                        partial.phase += tau * partial.frequency * (1 + chaos * 0.012 * sin(tau * 3 * t)) / rate
                        value += sin(partial.phase) * partial.gain
                    }
                }
            }
            if (i % hold == 0) sample = jsRound(value * steps) / steps
            dc += (sample - dc) * 0.001
            out[i] = (if (skip) 0.0 else (sample - dc) * 0.85).toFloat()
        }
        return finish(out, p.value("masterVolume", 0.5))
    }
}
