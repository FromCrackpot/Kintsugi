package fromcrackpot.kintsugi.bfxr.synths

import fromcrackpot.kintsugi.bfxr.BfxrValue
import fromcrackpot.kintsugi.bfxr.dsp.Mulberry32
import fromcrackpot.kintsugi.bfxr.dsp.RATE
import fromcrackpot.kintsugi.bfxr.dsp.finish
import fromcrackpot.kintsugi.bfxr.dsp.jsRound
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

internal object Machinr {
    fun render(params: Map<String, BfxrValue>): FloatArray {
        val p = params.mapValues { (_, value) -> (value as BfxrValue.Number).value }
        val size = p.getValue("size")
        val speed = p.getValue("speed")
        val load = p.getValue("load")
        val looseness = p.getValue("looseness")
        val roughness = p.getValue("roughness")
        val mechanism = p.getValue("mechanism")
        val rate = RATE.toDouble()
        val count = max(2.0, jsRound(p.getValue("duration") * rate)).toInt()
        val output = FloatArray(count)
        val random = Mulberry32(p.getValue("seed"))
        val tau = 2 * PI
        val scale = 2.0.pow((0.5 - size) * 2.6)
        val base = (35 + 230 * speed * speed) * scale * (1 - 0.32 * load)
        val toothRate = (5 + speed * 48) * (1 - load * 0.28)
        val attack = max(0.003, p.getValue("startTime")) * rate
        val release = max(0.006, p.getValue("stopTime")) * rate
        val ringFrequency = min(8500.0, (650 + 1100 * (1 - size)) * scale)
        val decay = exp(-1 / (rate * (0.007 + 0.035 * looseness)))
        val ringA = 2 * decay * cos(tau * ringFrequency / rate)
        val ringB = 2 * decay * cos(tau * ringFrequency * 0.63 / rate)
        var a1 = 0.0
        var a2 = 0.0
        var b1 = 0.0
        var b2 = 0.0
        var phase = 0.0
        var teeth = 0.0
        var slowNoise = 0.0
        var friction = 0.0
        var previousTooth = -1.0
        var turn = 0
        var contact = 0.0
        var eventStrength: Double
        val rotorOffset = random.next() * tau
        for (i in 0 until count) {
            val t = i / rate
            val progress = i.toDouble() / (count - 1)
            val engage = min(1.0, i / attack)
            val stop = min(1.0, (count - 1 - i) / release)
            val envelope = engage * stop
            val noise = random.next() * 2 - 1
            slowNoise += (noise - slowNoise) * 0.0015
            friction += (noise - friction) * (0.09 + 0.5 * roughness)
            var spin = (0.18 + 0.82 * engage) * (0.2 + 0.8 * stop)
            if (mechanism == 6.0) spin *= 1 - 0.7 * progress
            val wobble = 1 + roughness * (0.085 * sin(tau * 7.3 * t + rotorOffset) + slowNoise * 1.2)
            phase += tau * base * spin * wobble / rate
            var contactRate = toothRate
            if (mechanism == 3.0) contactRate = 1.3 + speed * 8
            if (mechanism == 4.0) contactRate = 7 + speed * 30
            if (mechanism == 7.0) contactRate = 4 + speed * 17
            teeth += contactRate * spin * wobble / rate
            val tooth = floor(teeth)
            var kick = 0.0
            if (tooth != previousTooth) {
                previousTooth = tooth
                turn++
                eventStrength = 0.45 + random.next() * 0.55
                kick = (0.15 + looseness * 0.7) * eventStrength
                if (mechanism == 3.0) kick = 0.75
                if (mechanism == 4.0) kick *= if (random.next() > roughness * 0.22) 1.0 else 0.1
                contact = eventStrength
            }
            contact *= exp(-1 / (rate * (0.007 + 0.025 * load)))
            if (mechanism == 2.0) {
                kick = if (i == floor(rate * 0.008).toInt() || i == floor(count * (0.19 + 0.12 * load)).toInt()) 1.9 else 0.0
            }
            if (mechanism == 7.0 && i == floor(count * 0.88).toInt()) kick += 3.2
            val resonantA = kick * 0.14 + ringA * a1 - decay * decay * a2
            val resonantB = kick * 0.11 + ringB * b1 - decay * decay * b2
            a2 = a1
            a1 = resonantA
            b2 = b1
            b1 = resonantB
            val rattle = if (turn % 2 != 0) resonantA else resonantB
            val rotor = sin(phase) + 0.35 * sin(2 * phase) + 0.12 * sin(5 * phase)
            val sample = when (mechanism) {
                1.0 -> 0.11 * rotor + 0.55 * rattle + friction * (0.05 + 0.18 * roughness) +
                    0.11 * load * sin(phase * 3.1 + 3 * sin(phase * 0.017)) * (0.5 + 0.5 * sin(tau * 1.7 * t))
                2.0 -> 1.05 * (resonantA + resonantB) + 0.08 * rotor * exp(-progress * 9) + friction * 0.13 * exp(-progress * 12)
                3.0 -> rattle * 1.35 + 0.014 * rotor + friction * contact * 0.18
                4.0 -> 0.28 * rotor * (0.3 + contact) + 0.4 * contact * friction +
                    0.21 * sin(phase * 0.5) * (0.4 + load) + 0.18 * rattle
                5.0 -> {
                    val position = 0.55 + 0.45 * sin(tau * (1.1 + speed * 3) * t)
                    (0.25 * sin(phase * (3.1 + load)) + 0.09 * sin(phase * 6.2)) * position +
                        0.05 * rattle + friction * (0.015 + 0.035 * roughness)
                }
                6.0 -> 0.13 * sin(phase * 1.9 + 2 * sin(teeth * 0.38)) + 0.53 * rattle +
                    0.08 * rotor + friction * (0.015 + 0.09 * roughness)
                7.0 -> {
                    val creak = sin(phase * 0.62 + 4 * sin(phase * 0.023))
                    0.27 * creak * (0.55 + 0.45 * sin(teeth * 1.4)) + 0.45 * rattle +
                        0.14 * sin(phase * 0.31) + friction * roughness * 0.1
                }
                else -> 0.24 * rotor + 0.09 * sin(phase * 8) * (0.25 + load) +
                    rattle * (0.1 + looseness * 0.3) + friction * (0.015 + roughness * 0.15)
            }
            output[i] = (sample * envelope).toFloat()
        }
        return finish(output, p.getValue("masterVolume"))
    }
}
