package fromcrackpot.kintsugi.bfxr.synths

import fromcrackpot.kintsugi.bfxr.BfxrValue
import fromcrackpot.kintsugi.bfxr.dsp.Mulberry32
import fromcrackpot.kintsugi.bfxr.dsp.RATE
import fromcrackpot.kintsugi.bfxr.dsp.finish
import fromcrackpot.kintsugi.bfxr.dsp.jsRound
import fromcrackpot.kintsugi.bfxr.dsp.value
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

internal object Boomr {
    private class Type(val front: Double, val gas: Double, val body: Double, val decay: Double, val brightness: Double)

    private class Event(val time: Double, val gain: Double)

    private val TYPES = listOf(
        Type(1.0, 1.0, 1.0, 1.0, 1.0),
        Type(0.55, 1.3, 0.8, 1.75, 0.65),
        Type(1.5, 0.2, 1.6, 1.5, 0.08),
        Type(1.1, 0.65, 1.8, 1.45, 0.4),
        Type(0.7, 0.65, 1.5, 0.8, 0.5),
        Type(0.6, 0.5, 0.22, 0.2, 1.5),
        Type(0.35, 1.6, 0.3, 2.0, 2.5),
        Type(0.9, 0.15, 2.2, 0.55, 0.15),
    )

    fun render(p: Map<String, BfxrValue>): FloatArray {
        val rate = RATE.toDouble()
        val duration = p.value("duration", 1.5, 0.12, 5.0)
        val frames = jsRound(rate * duration).toInt()
        val out = FloatArray(frames)
        val size = p.value("size", 0.5)
        val pressure = p.value("pressure", 0.7)
        val blast = p.value("blast", 0.65)
        val debris = p.value("debris", 0.35)
        val spread = p.value("spread", 0.5)
        val tail = p.value("tail", 0.45)
        val muffle = p.value("muffle", 0.15)
        val mechanism = jsRound(p.value("mechanism", 0.0, 0.0, 7.0)).toInt()
        val space = p.value("space", 0.25)
        val random = Mulberry32(p.value("seed", 0.5))
        val tau = 2 * PI

        fun coefficient(hz: Double): Double = 1 - exp(-tau * hz / rate)

        val type = TYPES[mechanism]
        val gas = p.value("gas", 0.25)
        val aftershock = p.value("aftershock", 0.25)
        val rubbleSize = p.value("rubbleSize", 0.5)
        val events = arrayListOf(Event(if (mechanism == 7) 0.08 + size * 0.07 else 0.007, 1.0))
        if (mechanism == 4) {
            for (j in 0 until 5) events += Event(duration * (0.12 + j * 0.12) * (0.8 + random.next() * 0.3), 0.75 - j * 0.09)
        }
        if (mechanism == 2) {
            for (j in 0 until 3) events += Event(0.06 + (0.1 + size * 0.18) * 1.42.pow(j), 0.44 * 0.63.pow(j))
        }
        val pressureDecay = (0.024 + size * 0.18) * type.decay
        val gasDecay = (0.022 + duration * (0.045 + tail * 0.28)) * type.decay
        val frontTime = (0.0014 + size * 0.01) * (if (mechanism == 2) 2.5 else if (mechanism == 5) 0.45 else 1.0)
        val lowRate = coefficient(40 + 120 * (1 - size))
        val midRate = coefficient(200 + 650 * (1 - size))
        val dcRate = coefficient(15.0)
        val motionRate = coefficient(4 + 10 * (1 - size))
        for (event in events) {
            val start = jsRound(event.time * rate).toInt()
            var low = 0.0
            var mid = 0.0
            var high = 0.0
            var dc = 0.0
            var drift = 0.0
            var wind = 0.0
            var gasLow = 0.0
            var gasDC = 0.0
            var shellPhase = 0.0
            for (i in start until frames) {
                val t = (i - start) / rate
                val n = random.next() * 2 - 1
                val n2 = random.next() * 2 - 1
                low += lowRate * (n - low)
                mid += midRate * (n - mid)
                dc += dcRate * (low - dc)
                drift += motionRate * (n2 - drift)
                gasLow += lowRate * (n2 - gasLow)
                gasDC += dcRate * (gasLow - gasDC)
                val cooling = exp(-t / (gasDecay * 0.65))
                val highRate = coefficient(140 + (650 + 8500 * (1 - size).pow(2)) * type.brightness * cooling)
                high += highRate * (n2 - high)
                wind += coefficient(85 + 260 * cooling) * (n2 - wind)
                val q = t / frontTime
                val front = (1 - q) * exp(-q)
                val pressureBody = (low - dc) * 5.5 * exp(-t / pressureDecay)
                val gasAttack = if (mechanism == 1) 1 - exp(-t / (0.026 + size * 0.08)) else 1 - exp(-t / 0.002)
                val billow = 0.8 + abs(drift) * 6
                val plume = ((high - wind) * 0.65 + wind * 3.2) * exp(-t / gasDecay) * gasAttack * billow
                val rollingSource = if (mechanism == 1) gasLow - gasDC else low - dc
                val roll = rollingSource * 4.2 * exp(-t / (0.05 + duration * (0.08 + tail * 0.25))) * (1 - exp(-t / 0.028))
                var transmission = 0.0
                if (mechanism == 1) {
                    shellPhase += tau * (95 + 170 * (1 - size)) * (1 - 0.14 * (1 - exp(-t / 0.04))) / rate
                    transmission = (sin(shellPhase) + 0.23 * sin(shellPhase * 2.37)) * exp(-t / (0.018 + size * 0.045)) * 0.2
                } else if (mechanism == 2) {
                    transmission = (1 - t / (frontTime * 2)) * exp(-t / (frontTime * 2)) * 0.9
                } else if (mechanism == 3) {
                    transmission = (mid - low) * 5.5 * exp(-t / (0.08 + size * 0.2)) * (1 - exp(-t / 0.012))
                }
                out[i] = (out[i] + event.gain * (pressure * (front * type.front + pressureBody * type.body + transmission) +
                    blast * type.gas * plume + tail * type.body * roll) * 0.8).toFloat()
            }
        }
        val gasRandom = Mulberry32(p.value("seed", 0.5) * 0.71 + 0.193)
        val jetDelay = if (mechanism == 6) 0.015 else if (mechanism == 1) 0.045 else 0.07 + size * 0.045
        val jetLife = (0.1 + duration * (0.12 + tail * 0.3)) *
            (if (mechanism == 6) 1.5 else if (mechanism == 2) 0.35 else if (mechanism == 5) 0.12 else 1.0)
        val gasLowRate = coefficient(50 + 80 * (1 - size))
        val gasHighRate = coefficient(if (mechanism == 2) 170.0 else 700 + 4700 * (1 - size).pow(0.65))
        var jetLow = 0.0
        var jetHigh = 0.0
        var jetMotion = 0.0
        for (i in 0 until frames) {
            val t = i / rate - jetDelay
            val n = gasRandom.next() * 2 - 1
            jetLow += gasLowRate * (n - jetLow)
            jetHigh += gasHighRate * (n - jetHigh)
            jetMotion += coefficient(13.0) * (gasRandom.next() * 2 - 1 - jetMotion)
            if (t >= 0 && gas > 0) {
                val opening = 1 - exp(-t / (0.02 + size * 0.035))
                val envelope = opening * exp(-t / jetLife)
                val billow = 0.48 + abs(jetMotion) * 9 + 0.25 * sin(t * (11 + size * 9)).pow(2)
                out[i] = (out[i] + gas * (if (mechanism == 5) 0.2 else 1.0) * envelope * billow *
                    ((jetHigh - jetLow) * 1.9 + jetLow * 2.2)).toFloat()
            }
            if (mechanism == 7 && t + jetDelay < events[0].time) {
                val u = (t + jetDelay) / events[0].time
                out[i] = (out[i] + (jetHigh - jetLow) * blast * u.pow(3) * 0.65).toFloat()
            }
        }
        val shockRandom = Mulberry32(p.value("seed", 0.5) * 0.61 + 0.317)
        val shockCount = 2 + jsRound(aftershock * 4).toInt()
        var shock = 0
        while (shock < shockCount && aftershock > 0) {
            val arrival = 0.12 + duration * (0.04 + shock * 0.115) * (0.8 + shockRandom.next() * 0.4)
            val start = jsRound(arrival * rate).toInt()
            val life = (0.025 + size * 0.1) * (0.7 + shockRandom.next() * 0.6)
            val strength = aftershock * 0.7.pow(shock) * (0.75 + size * 0.8) * (if (mechanism == 5) 0.06 else 1.0)
            var low = 0.0
            var dc = 0.0
            val lp = coefficient(55 + 90 * (1 - size))
            val end = min(frames.toDouble(), start + ceil(life * 8 * rate))
            var i = start
            while (i < end) {
                val t = (i - start) / rate
                val q = t / (0.006 + size * 0.016)
                low += lp * (shockRandom.next() * 2 - 1 - low)
                dc += coefficient(12.0) * (low - dc)
                out[i] = (out[i] + strength * ((1 - q) * exp(-q) * 0.8 +
                    (low - dc) * 7 * (1 - exp(-t / 0.009)) * exp(-t / life))).toFloat()
                i++
            }
            shock++
        }
        val pieces = jsRound(debris * 48).toInt()
        for (piece in 0 until pieces) {
            val start = floor((0.018 + rubbleSize * 0.055 + random.next() * duration * (0.04 + spread * 0.8)) * rate).toInt()
            val chunk = random.next()
            val life = (0.002 + chunk * 0.025) * (0.25 + rubbleSize * 2.5)
            val gain = debris * (0.1 + random.next() * 0.3) * (0.7 + rubbleSize)
            val length = min((frames - start).toDouble(), ceil(life * rate * 7)).toInt()
            val damping = exp(-1 / (life * rate))
            val dustRate = coefficient((1200 + random.next() * 8500) * (1 - rubbleSize * 0.94).pow(2))
            val bodyRate = coefficient(45 + random.next() * 600 * (1 - rubbleSize * 0.85).pow(2))
            var dust = 0.0
            var body = 0.0
            var envelope = 1.0
            for (j in 0 until length) {
                val noise = random.next() * 2 - 1
                dust += dustRate * (noise - dust)
                body += bodyRate * (noise - body)
                val q = (j / rate) / (0.0004 + rubbleSize * 0.009)
                out[start + j] = (out[start + j] + (dust * (1 - rubbleSize * 0.7) + body * (0.5 + rubbleSize * 5) +
                    (1 - q) * exp(-q) * rubbleSize) * envelope * gain).toFloat()
                envelope *= damping
            }
        }
        if (space > 0) {
            val delays = doubleArrayOf(0.071, 0.113, 0.173).map { t ->
                FloatArray(max(1.0, jsRound(t * (0.6 + space * 1.8) * rate)).toInt())
            }
            val heads = IntArray(3)
            val smoothed = DoubleArray(3)
            for (i in 0 until frames) {
                val input = out[i].toDouble()
                var echo = 0.0
                for (j in 0 until 3) {
                    val buffer = delays[j]
                    val head = heads[j]
                    val sample = buffer[head]
                    smoothed[j] += 0.08 * (sample - smoothed[j])
                    buffer[head] = (input * 0.36 + smoothed[j] * (0.2 + space * 0.48)).toFloat()
                    echo += smoothed[j]
                    heads[j] = (head + 1) % buffer.size
                }
                out[i] = (out[i] + echo * space * 0.7).toFloat()
            }
        }
        val finalRate = coefficient(80 + 15000 * (1 - muffle).pow(3))
        var filtered = 0.0
        for (i in 0 until frames) {
            filtered += finalRate * (out[i] - filtered)
            out[i] = filtered.toFloat()
        }
        return finish(out, p.value("masterVolume", 0.5))
    }
}
