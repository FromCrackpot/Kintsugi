package fromcrackpot.kintsugi.bfxr.synths

import fromcrackpot.kintsugi.bfxr.BfxrValue
import fromcrackpot.kintsugi.bfxr.dsp.Mulberry32
import fromcrackpot.kintsugi.bfxr.dsp.RATE
import fromcrackpot.kintsugi.bfxr.dsp.finish
import fromcrackpot.kintsugi.bfxr.dsp.jsRound
import fromcrackpot.kintsugi.bfxr.dsp.value
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

internal object Rustlr {
    private class Material(
        val cutoff: Double,
        val soft: Double,
        val highpass: Double,
        val crease: Double,
        val stick: Double,
        val flex: Double,
    )

    private val MATERIALS = listOf(
        Material(4200.0, 0.35, 0.65, 0.85, 0.5, 720.0),
        Material(950.0, 1.0, 0.2, 0.08, 0.15, 180.0),
        Material(1900.0, 0.65, 0.32, 0.38, 1.0, 330.0),
        Material(6400.0, 0.25, 0.72, 1.1, 0.6, 1150.0),
        Material(12500.0, 0.06, 0.92, 1.35, 0.3, 2600.0),
        Material(4500.0, 0.15, 0.55, 0.5, 0.2, 1500.0),
    )

    fun render(p: Map<String, BfxrValue>): FloatArray {
        val rate = RATE.toDouble()
        val duration = p.value("duration", 0.65, 0.08, 3.0)
        val frames = jsRound(duration * rate).toInt()
        val out = FloatArray(frames)
        val kind = jsRound(p.value("material", 0.0, 0.0, 5.0)).toInt()
        val material = MATERIALS[kind]
        val gesture = jsRound(p.value("gesture", 1.0, 0.0, 3.0)).toInt()
        val grain = p.value("grain", 0.45)
        val density = p.value("density", 0.5)
        val motion = p.value("motion", 0.0, -1.0, 1.0)
        val pressure = p.value("pressure", 0.5)
        val brightness = p.value("brightness", 0.5)
        val folds = jsRound(p.value("folds", 3.0, 1.0, 12.0)).toInt()
        val random = Mulberry32(p.value("seed", 0.5))
        val pi = PI
        val cutoff = material.cutoff * (0.22 + brightness * 1.18) / (1 + grain * 0.65) * (0.8 + pressure * 0.4)
        val filter = 1 - exp(-2 * pi * cutoff / rate)
        val slowFilter = 1 - exp(-2 * pi * max(60.0, cutoff * 0.13) / rate)
        val flexFilter = 1 - exp(-2 * pi * material.flex * (0.7 + pressure * 0.8) / rate)
        val grainFrames = max(8.0, jsRound((0.0007 + grain * grain * 0.023) * (1 + material.soft) * rate))
        val count = max(8.0, jsRound(duration * (48 + density * 780) / (1 + grain * 1.4))).toInt()
        val clusterCount = folds + 2
        val centers = DoubleArray(clusterCount) { (it + 0.2 + random.next() * 0.6) / clusterCount }
        val normalizer = 1 / sqrt(1 + count * grainFrames / frames * 0.12)
        val gain = (0.24 + pressure * 0.76) * normalizer * 2

        fun addGrain(start: Double, length: Double, strength: Double, crease: Boolean) {
            var low = 0.0
            var slow = 0.0
            var flex = 0.0
            var previousFlex = 0.0
            var j = 0
            while (j < length && start + j < frames) {
                val noise = random.next() * 2 - 1
                low += (noise - low) * filter
                slow += (low - slow) * slowFilter
                flex += (low - flex) * flexFilter
                val age = j / length
                val attack = if (crease) 0.025 + material.soft * 0.22 else 0.5
                val window = if (crease) {
                    min(1.0, age / attack) * exp(-age * (5 - material.soft * 2))
                } else {
                    sin(pi * age) * sin(pi * age)
                }
                val surface = low - slow * material.highpass
                val bending = (flex - previousFlex) * rate / (material.flex * 2 * pi)
                val index = (start + j).toInt()
                out[index] = (out[index] +
                    (surface + (if (crease) bending * (0.5 + material.soft) else 0.0)) * window * strength).toFloat()
                previousFlex = flex
                j++
            }
        }

        for (i in 0 until count) {
            val position = if (kind == 5) {
                ((i + 0.25 + random.next() * 0.5) / count).pow(1.35 - motion * 0.35)
            } else {
                val center = centers[floor(random.next() * clusterCount).toInt()]
                max(0.0, min(0.96, center + (random.next() + random.next() - 1) * (0.035 + density * 0.24)))
            }
            val length = max(6.0, jsRound(grainFrames * (0.45 + random.next() * 1.1)))
            val start = jsRound(position * (frames - length))
            addGrain(max(0.0, start), length, gain * (0.16 + random.next() * 0.3) * (if (kind == 5) 1.4 else 1.0), false)
        }
        for (fold in 0 until folds) {
            val position = (fold + 0.3 + random.next() * 0.45) / folds
            val length = max(8.0, jsRound((0.003 + grain * 0.022 + material.soft * 0.016) * rate * (0.65 + random.next() * 0.65)))
            val start = max(0.0, jsRound(position * (frames - length)))
            addGrain(start, length, gain * (0.42 + random.next() * 0.45) * material.crease, true)
        }
        val slipLoss = exp(-1 / ((0.0015 + material.soft * 0.017) * (0.6 + pressure) * rate))
        var friction = 0.0
        var frictionSlow = 0.0
        var shear = 0.0
        var slip = 0.0
        var motionLow = 0.0
        var breakaway = 0.7 + random.next() * 0.6
        for (i in 0 until frames) {
            val position = i.toDouble() / (frames - 1)
            val arch = max(0.0, sin(pi * position))
            friction += (random.next() * 2 - 1 - friction) * filter
            frictionSlow += (friction - frictionSlow) * slowFilter
            motionLow += (random.next() * 2 - 1 - motionLow) * 0.0012
            val speed = (0.3 + arch.pow(0.6)) * (0.7 + min(0.8, abs(motionLow) * 12))
            shear += speed * (30 + density * 130) / rate / (0.6 + pressure * material.stick * 2)
            if (shear > breakaway) {
                shear -= breakaway
                slip += 0.45 + pressure * material.stick * 1.5
                breakaway = 0.65 + random.next() * 0.8
            }
            slip *= slipLoss
            val bed = (friction - frictionSlow * material.highpass) * gain * (0.07 + density * 0.12) *
                (0.6 + material.soft * 1.3) * speed * (0.45 + material.stick * slip + shear * 0.35)
            val envelope = when (gesture) {
                0 -> arch.pow(0.35) * exp(-position * 4.5) * 2
                1 -> arch.pow(0.7) * (0.55 + 0.45 * abs(sin(pi * 2 * position)))
                2 -> arch.pow(0.45)
                else -> arch.pow(0.5) * (0.2 + 0.8 * abs(sin(pi * folds * position)))
            }
            val travel = exp(motion * (position - 0.5) * 5 - abs(motion) * 1.4)
            out[i] = ((out[i] + bed) * envelope * travel).toFloat()
        }
        return finish(out, p.value("masterVolume", 0.5))
    }
}
