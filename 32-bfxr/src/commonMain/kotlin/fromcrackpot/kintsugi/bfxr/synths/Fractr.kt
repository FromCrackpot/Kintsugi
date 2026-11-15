package fromcrackpot.kintsugi.bfxr.synths

import fromcrackpot.kintsugi.bfxr.BfxrValue
import fromcrackpot.kintsugi.bfxr.dsp.Mulberry32
import fromcrackpot.kintsugi.bfxr.dsp.RATE
import fromcrackpot.kintsugi.bfxr.dsp.finish
import fromcrackpot.kintsugi.bfxr.dsp.jsRound
import fromcrackpot.kintsugi.bfxr.dsp.value
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

internal object Fractr {
    private class Material(
        val pitch: Double,
        val ring: Double,
        val noise: Double,
        val cutoff: Double,
        val grit: Double,
        val ratios: DoubleArray,
    )

    private class Contact(val sample: Int, val strength: Double)

    private class Mode(val c: Double, val s: Double)

    private val MATERIALS = listOf(
        Material(1.65, 0.22, 1.7, 11000.0, 0.28, doubleArrayOf(1.0, 2.71, 4.83)),
        Material(0.84, 0.15, 2.1, 6500.0, 0.6, doubleArrayOf(1.0, 1.91, 3.77)),
        Material(1.05, 1.75, 0.025, 10000.0, 0.0, doubleArrayOf(1.0, 1.505, 2.014)),
        Material(0.17, 0.08, 3.8, 1700.0, 1.0, doubleArrayOf(1.0, 1.63, 2.42)),
        Material(1.0, 0.6, 0.015, 10000.0, 0.0, doubleArrayOf(1.0, 2.0, 4.0)),
        Material(0.65, 0.26, 1.8, 7000.0, 0.42, doubleArrayOf(1.0, 2.39, 5.17)),
        Material(0.39, 0.1, 2.8, 3800.0, 0.45, doubleArrayOf(1.0, 1.82, 3.03)),
        Material(0.55, 0.015, 2.8, 5100.0, 0.65, doubleArrayOf(1.0, 1.77, 3.11)),
    )

    private val WIDTHS = doubleArrayOf(0.00012, 0.00065, 0.0002, 0.0022, 0.00015, 0.00023, 0.0011, 0.00022)
    private val MASSES = doubleArrayOf(0.06, 0.9, 0.08, 1.0, 0.02, 0.35, 1.3, 0.15)

    fun render(p: Map<String, BfxrValue>): FloatArray {
        val rate = RATE.toDouble()
        val random = Mulberry32(p.value("seed", 0.5))
        val duration = p.value("duration", 1.8, 0.15, 6.0)
        val output = FloatArray(jsRound(duration * rate).toInt())
        val materialIndex = jsRound(p.value("material", 0.0, 0.0, 7.0)).toInt()
        val material = MATERIALS[materialIndex]
        val tuned = materialIndex == 2 || materialIndex == 4
        val fragments = jsRound(p.value("fragments", 32.0, 3.0, 96.0)).toInt()
        val size = p.value("fragmentSize", 0.4)
        val spread = p.value("spread", 0.6)
        val stress = p.value("stress", 0.4)
        val fracture = p.value("fracture", 0.75)
        val decay = p.value("decay", 0.4)
        val gravity = p.value("gravity", 0.5)
        val bounce = p.value("bounce", 0.4)
        val cascadeTime = duration * (0.012 + spread * 0.72) * (1 - gravity * 0.6)
        val gain = 1.45 * p.value("shards", 0.35) / fragments.toDouble().pow(0.3)
        val tau = 2 * PI
        val bounceCount = jsRound(bounce * 4).toInt()

        for (shard in 0 until fragments) {
            val shardSize = max(0.0, min(1.0, size + (random.next() - 0.5) * 0.26))
            val progress = (shard + random.next() * 0.65) / fragments
            var time = 0.006 + cascadeTime * progress.pow(0.75 + 0.7 * gravity)
            if (materialIndex == 4) time = jsRound(time * 48) / 48 + 0.006
            val start = jsRound(time * rate).toInt()
            var frequency = 2700 * 2.0.pow(-shardSize * 4.5) * material.pitch * (0.65 + 0.7 * random.next())
            if (materialIndex == 4) frequency = 110 * 2.0.pow(jsRound(12 * log2(frequency / 110)) / 12)
            val ringTime = (0.006 + decay * 0.23) * material.ring * (0.8 + random.next() * 0.4)
            val contactTime =
                if (tuned) 0.0025 else 0.0018 + (0.004 + decay * 0.025) * (0.4 + shardSize) * (0.7 + material.grit)
            val noiseRadius = exp(-1 / (rate * contactTime))
            val crackRadius = exp(-1 / (rate * (0.0003 + shardSize * 0.00065)))
            val cutoff = material.cutoff * 2.0.pow(-shardSize * 1.6)
            val dustFilter = 1 - exp(-tau * cutoff / rate)
            val bodyFilter = 1 - exp(-tau * (130 + 1700 * (1 - shardSize) * material.pitch) / rate)
            val strength = gain * (0.55 + random.next() * 0.65) * (1 - progress * 0.3)
            val flight = (0.045 + 0.34 * (1 - gravity)) * (0.4 + 0.6 * shardSize)
            val contacts = ArrayList<Contact>(bounceCount + 1)
            contacts += Contact(start, strength)
            var nextTime = time
            var nextFlight = flight
            var nextStrength = strength
            for (j in 0 until bounceCount) {
                nextTime += nextFlight
                nextFlight *= 0.43 + bounce * 0.24
                nextStrength *= 0.27 + bounce * 0.42
                contacts += Contact(jsRound(nextTime * rate).toInt(), nextStrength)
            }
            val end = min(output.size, contacts.last().sample + jsRound(max(ringTime, contactTime) * 7 * rate).toInt())
            val modes = List(material.ratios.size) { index ->
                val angle = tau * min(11000.0, frequency * material.ratios[index]) / rate
                val radius = exp(-(1 + index * 0.55) / (ringTime * rate))
                Mode(cos(angle) * radius, sin(angle) * radius)
            }
            val a = modes[0]
            val b = modes[1]
            val c = modes[2]
            var ar = 0.0
            var ai = 0.0
            var br = 0.0
            var bi = 0.0
            var cr = 0.0
            var ci = 0.0
            var noiseEnvelope = 0.0
            var nextContact = 0
            var dust = 0.0
            var body = 0.0
            var crackEnvelope = 0.0
            var crackLeft = 0
            var nextCrack = 0
            var contactStrength = 0.0
            for (i in start until end) {
                if (nextContact < contacts.size && i == contacts[nextContact].sample) {
                    val hit = contacts[nextContact++].strength
                    val modeGain = if (tuned) 1.0 else 0.12
                    ar += hit * modeGain
                    br += hit * modeGain * 0.44
                    cr += hit * modeGain * 0.24
                    noiseEnvelope += hit * material.noise * (if (tuned) 1.0 else 0.58)
                    contactStrength = hit
                    crackLeft = if (tuned) 0 else if (nextContact == 1) 3 + jsRound(shardSize * 4).toInt() else 1
                    nextCrack = i
                }
                if (crackLeft > 0 && i == nextCrack) {
                    crackEnvelope += contactStrength * (0.5 + random.next() * 0.9)
                    nextCrack += 5 + jsRound(random.next() * (25 + shardSize * 100)).toInt()
                    crackLeft--
                }
                val an = ar * a.c - ai * a.s
                ai = ar * a.s + ai * a.c
                ar = an
                val bn = br * b.c - bi * b.s
                bi = br * b.s + bi * b.c
                br = bn
                val cn = cr * c.c - ci * c.s
                ci = cr * c.s + ci * c.c
                cr = cn
                val noise = random.next() * 2 - 1
                dust += (noise - dust) * dustFilter
                body += (noise - body) * bodyFilter
                val rough = dust + body * material.grit
                output[i] = (output[i] + ((ar + br + cr) * 0.52 + rough * noiseEnvelope +
                    (dust - body * 0.5) * crackEnvelope)).toFloat()
                noiseEnvelope *= noiseRadius
                crackEnvelope *= crackRadius
            }
        }

        val structuralRandom = Mulberry32(p.value("seed", 0.5) * 0.79 + 0.137)
        val splitTime = 0.011 + stress * 0.029
        val mass = MASSES[materialIndex]
        val width = WIDTHS[materialIndex] * (0.7 + size * 0.8)
        val splitGain = fracture * (if (tuned) 0.25 else if (materialIndex == 7) 2.2 else 3.4)
        val branches = when (materialIndex) {
            7 -> 34 + jsRound(size * 28).toInt()
            6 -> 5
            else -> 7 + jsRound(size * 9).toInt()
        }
        for (branch in 0 until branches) {
            val u = branch.toDouble() / branches
            val branchSpan = when (materialIndex) {
                7 -> 0.065 + stress * 0.07
                6 -> 0.017
                1 -> 0.045 + stress * 0.025
                else -> 0.022 + stress * 0.035
            }
            val delay =
                if (branch == 0) 0.0 else 0.001 + u.pow(1.6) * branchSpan * (0.75 + structuralRandom.next() * 0.5)
            val start = jsRound((splitTime + delay) * rate).toInt()
            val release = width * (0.7 + structuralRandom.next() * 0.9)
            val weight = splitGain * (if (branch == 0) 1.0 else 0.25 + 0.4 * (1 - u)) *
                (0.75 + structuralRandom.next() * 0.5)
            val length = min(output.size - start, jsRound((release * 9 + 0.009 * mass) * rate).toInt())
            var gritLow = 0.0
            var massGrit = 0.0
            val gritRate = 1 - exp(-tau * material.cutoff * 0.55 / rate)
            for (j in 0 until length) {
                val t = j / rate
                val q = t / release
                gritLow += gritRate * (structuralRandom.next() * 2 - 1 - gritLow)
                val tensile = (1 - q) * exp(-q)
                val tearing = gritLow * exp(-t / (release * 3)) * 0.55
                val bodyQ = t / (0.002 + mass * 0.004)
                massGrit += 0.12 * (structuralRandom.next() * 2 - 1 - massGrit)
                val bodyTone = ((1 - bodyQ) + massGrit * (if (materialIndex == 3 || materialIndex == 6) 6.0 else 1.2)) *
                    exp(-bodyQ) * mass * 0.95
                output[start + j] = (output[start + j] + weight * (tensile + tearing + bodyTone)).toFloat()
            }
        }
        if (!tuned && stress > 0) {
            var slow = 0.0
            var fast = 0.0
            val end = min(output.size, jsRound(splitTime * rate).toInt())
            for (i in jsRound(0.005 * rate).toInt() until end) {
                val u = i.toDouble() / end
                val noise = structuralRandom.next() * 2 - 1
                slow += 0.006 * (noise - slow)
                fast += 0.055 * (noise - fast)
                val stutter = (0.5 + 0.5 * sin(u * (36 + materialIndex * 9))).pow(5)
                output[i] = (output[i] + (fast - slow) * stutter * stress * u * 2.5).toFloat()
            }
        }
        return finish(output, p.value("masterVolume", 0.5))
    }
}
