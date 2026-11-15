package fromcrackpot.kintsugi.bfxr.synths

import fromcrackpot.kintsugi.bfxr.BfxrValue
import fromcrackpot.kintsugi.bfxr.dsp.Mulberry32
import fromcrackpot.kintsugi.bfxr.dsp.RATE
import fromcrackpot.kintsugi.bfxr.dsp.clamp
import fromcrackpot.kintsugi.bfxr.dsp.toInt32
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sqrt

internal object Footsteppr {
    fun render(params: Map<String, BfxrValue>, renderSeed: Double): FloatArray {
        val p = params.mapValues { (_, value) -> (value as BfxrValue.Number).value }
        val length = 0.1 + 0.7 * (1 - p.getValue("swiftness"))
        val pd = Pd((length * RATE).toInt(), Mulberry32(renderSeed))
        val envelope = resizeFn(
            addFns(
                resizeFn(step(p.getValue("heel")), 0.0, 1.0, 0.0, 0.3333),
                resizeFn(step(p.getValue("roll")), 0.0, 1.0, 0.125, 0.875),
                resizeFn(step(p.getValue("ball")), 0.0, 1.0, 0.6667, 1.0),
            ),
            0.0, 1.0, 0.0, length,
        )
        val envelopeSignal = pd.fn(envelope)
        var signal = when (p.getValue("terrain").toInt()) {
            1 -> pd.grass(envelopeSignal)
            2 -> pd.dirt(envelopeSignal)
            3 -> pd.gravel(envelopeSignal)
            4 -> pd.wood(envelopeSignal)
            else -> pd.snow(envelopeSignal)
        }
        signal = pd.mul(signal, pd.c(p.getValue("masterVolume")))
        signal = pd.clip(signal, pd.c(-1.0), pd.c(1.0))
        return pd.mul(signal, pd.c(4.0))
    }

    private fun step(n: Double): (Double) -> Double = { x ->
        if (x <= 0 || x >= 1) 0.0 else ((x * x * x) * n - x * n) * (1 - x) * (-1.5)
    }

    private fun resizeFn(fn: (Double) -> Double, a1: Double, a2: Double, b1: Double, b2: Double): (Double) -> Double =
        { y -> fn((y - b1) / (b2 - b1) * (a2 - a1) + a1) }

    private fun addFns(vararg fns: (Double) -> Double): (Double) -> Double =
        { x -> fns.fold(0.0) { acc, fn -> acc + fn(x) } }

    private fun Pd.dirt(envelope: FloatArray): FloatArray {
        val s32 = noise()
        val s31 = lop(s32, c(80.0))
        val s30 = mul(s31, c(70.0))
        val s29 = envelope
        val s28 = add(s29, c(0.3))
        val s27 = mul(s28, s30)
        val s26 = mul(s27, c(70.0))
        val s25 = add(s26, c(70.0))
        val s24 = osc(s25)
        val s23 = hip(s24, c(200.0))
        val s22 = clip(s23, c(-1.0), c(1.0))
        val s21 = mul(s22, c(0.04))
        val s18 = mul(s29, s29)
        val s14 = mul(s18, s18)
        val s6 = mul(s14, c(500.0))
        val s5 = add(s6, c(40.0))
        val s4 = osc(s5)
        val s3 = mul(s4, s14)
        val s2 = mul(s3, c(0.5))
        return add(s2, s21)
    }

    private fun Pd.grass(envelope: FloatArray): FloatArray {
        val s60 = noise()
        val s59 = lop(s60, c(16.0))
        val s58 = mul(s59, c(23800.0))
        val s57 = add(s58, c(3400.0))
        val s56 = clip(s57, c(2000.0), c(10000.0))
        val s54 = lop(s60, c(2000.0))
        val s52 = lop(s60, c(300.0))
        val s51 = div(s52, s54)
        val s50 = hip(s51, c(2500.0))
        val s43 = mul(s50, s50)
        val s29 = mul(s43, s43)
        val s28 = mul(s29, c(1e-05))
        val s27 = clip(s28, c(-0.9), c(0.9))
        val s26 = vcf(s27, s56, c(1.0))
        val s25 = hip(s26, c(900.0))
        val s24 = mul(s25, c(0.3))
        val s23 = envelope
        val s22 = mul(s23, s24)
        val s19 = mul(s23, s23)
        val s15 = mul(s19, s19)
        val s7 = mul(s15, c(600.0))
        val s6 = add(s7, c(30.0))
        val s5 = osc(s6)
        val s4 = clip(s5, c(0.0), c(0.5))
        val s3 = mul(s4, s15)
        val s2 = mul(s3, c(0.8))
        return add(s2, s22)
    }

    private fun Pd.gravel(envelope: FloatArray): FloatArray {
        val s27 = envelope
        val s26 = mul(s27, c(1000.0))
        val s25 = noise()
        val s24 = lop(s25, c(50.0))
        val s23 = mul(s24, c(50000.0))
        val s22 = add(s23, s26)
        val s21 = clip(s22, c(500.0), c(10000.0))
        val s19 = lop(s25, c(2000.0))
        val s17 = lop(s25, c(300.0))
        val s16 = div(s17, s19)
        val s15 = hip(s16, c(400.0))
        val s8 = mul(s15, s15)
        val s7 = mul(s8, c(0.01))
        val s6 = clip(s7, c(-0.9), c(0.9))
        val s5 = vcf(s6, s21, c(3.0))
        val s4 = hip(s5, c(200.0))
        val s3 = mul(s4, c(2.0))
        return mul(s27, s3)
    }

    private fun Pd.snow(envelope: FloatArray): FloatArray {
        val s29 = envelope
        val s27 = mul(s29, c(9000.0))
        val s26 = add(s27, c(700.0))
        val s25 = noise()
        val s24 = lop(s25, c(10.0))
        val s23 = mul(s24, c(17.0))
        val s19 = mul(s23, s23)
        val s18 = add(s19, c(0.5))
        val s16 = lop(s25, c(70.0))
        val s14 = lop(s25, c(50.0))
        val s13 = div(s14, s16)
        val s12 = noise()
        val s11 = lop(s12, c(900.0))
        val s9 = lop(s12, c(110.0))
        val s8 = div(s9, s11)
        val s7 = mul(s8, s13)
        val s6 = mul(s7, s18)
        val s5 = clip(s6, c(-1.0), c(1.0))
        val s4 = hip(s5, c(300.0))
        val s3 = vcf(s4, s26, c(0.5))
        val s2 = mul(s3, s29)
        return mul(s2, c(0.2))
    }

    private fun Pd.wood(envelope: FloatArray): FloatArray {
        val s29 = noise()
        val s28 = bp(s29, c(201.0), c(70.0))
        val s26 = bp(s29, c(189.0), c(90.0))
        val s24 = bp(s29, c(156.0), c(90.0))
        val s22 = bp(s29, c(123.0), c(20.0))
        val s21 = mul(polyadd(s22, s24, s26, s28), c(8.0))
        val s20 = envelope
        val s18 = mul(s20, s20)
        val s17 = mul(s18, c(2.0))
        val s16 = mul(s17, s21)
        val s15 = mul(s16, c(0.6))
        val s14 = noise()
        val s13 = bp(s14, c(154.0), c(90.0))
        val s11 = bp(s14, c(139.0), c(90.0))
        val s9 = bp(s14, c(134.0), c(90.0))
        val s7 = bp(s14, c(95.0), c(90.0))
        val s6 = mul(polyadd(s7, s9, s11, s13), c(6.0))
        val s4 = sqrt(s20)
        val s3 = mul(s4, s6)
        val s2 = mul(s3, c(0.5))
        return add(s2, s15)
    }

    private const val COSTABLESIZE = 2048
    private const val UNITBIT32 = 1572864.0
    private const val CONVERSION_FACTOR = 2 * PI / RATE

    private val COS_TABLE = FloatArray(COSTABLESIZE + 1).also { table ->
        for (i in 0 until COSTABLESIZE) table[i] = cos(2 * PI * i / COSTABLESIZE).toFloat()
        table[0] = 1f
        table[COSTABLESIZE] = 1f
        table[COSTABLESIZE / 4] = 0f
        table[3 * COSTABLESIZE / 4] = 0f
        table[COSTABLESIZE / 2] = -1f
    }

    private class Pd(private val length: Int, private val random: Mulberry32) {
        fun fn(fn: (Double) -> Double): FloatArray = FloatArray(length) { fn(it.toDouble() / RATE).toFloat() }

        fun noise(): FloatArray = FloatArray(length) { (random.next() * 2 - 1).toFloat() }

        fun c(value: Double): FloatArray = FloatArray(length) { value.toFloat() }

        fun clip(buffer: FloatArray, lo: FloatArray, hi: FloatArray): FloatArray =
            FloatArray(buffer.size) { clamp(buffer[it].toDouble(), lo[it].toDouble(), hi[it].toDouble()).toFloat() }

        fun mul(buffer: FloatArray, by: FloatArray): FloatArray =
            FloatArray(buffer.size) { (buffer[it].toDouble() * by[it].toDouble()).toFloat() }

        fun div(buffer: FloatArray, by: FloatArray): FloatArray =
            FloatArray(buffer.size) { (buffer[it].toDouble() / by[it].toDouble()).toFloat() }

        fun add(buffer: FloatArray, addend: FloatArray): FloatArray =
            FloatArray(buffer.size) { (buffer[it].toDouble() + addend[it].toDouble()).toFloat() }

        fun polyadd(vararg buffers: FloatArray): FloatArray {
            val result = FloatArray(buffers[0].size)
            for (i in result.indices) {
                result[i] = buffers[0][i]
                for (j in 1 until buffers.size) result[i] = (result[i].toDouble() + buffers[j][i].toDouble()).toFloat()
            }
            return result
        }

        fun sqrt(buffer: FloatArray): FloatArray = FloatArray(buffer.size) { sqrt(buffer[it].toDouble()).toFloat() }

        fun osc(freq: FloatArray): FloatArray {
            val result = FloatArray(freq.size)
            val conv = 2 * PI / RATE
            var phase = 0.0
            for (i in freq.indices) {
                phase += freq[i] * conv
                while (phase >= PI) phase -= 2 * PI
                while (phase < -PI) phase += 2 * PI
                val index = toInt32((phase + PI) / (2 * PI) * COSTABLESIZE)
                val idx1 = index % COSTABLESIZE
                val idx2 = (idx1 + 1) % COSTABLESIZE
                val frac = (index - idx1).toDouble()
                val f1 = COS_TABLE[idx1].toDouble()
                val f2 = COS_TABLE[idx2].toDouble()
                result[i] = (f1 + frac * (f2 - f1)).toFloat()
            }
            return result
        }

        fun lop(buffer: FloatArray, coefficient: FloatArray): FloatArray {
            val result = FloatArray(buffer.size)
            var last = buffer[0].toDouble()
            for (i in buffer.indices) {
                var coef = coefficient[i] * CONVERSION_FACTOR
                if (coef > 1) coef = 1.0
                if (coef < 0) coef = 0.0
                last = coef * buffer[i] + (1 - coef) * last
                result[i] = last.toFloat()
            }
            return result
        }

        fun hip(buffer: FloatArray, coefficient: FloatArray): FloatArray {
            val result = FloatArray(buffer.size)
            var last = buffer[0].toDouble()
            for (i in buffer.indices) {
                var coef = 1 - coefficient[i] * CONVERSION_FACTOR
                if (coef < 0) coef = 0.0 else if (coef > 1) coef = 1.0
                if (coef < 1) {
                    val normal = 0.5 * (1 + coef)
                    val cur = buffer[i] + coef * last
                    result[i] = (normal * (cur - last)).toFloat()
                    last = cur
                } else {
                    result[i] = buffer[i]
                }
            }
            return result
        }

        fun bp(buffer: FloatArray, centerFrequency: FloatArray, q: FloatArray): FloatArray {
            val result = FloatArray(buffer.size)
            var last = buffer[0].toDouble()
            var prev = buffer[0].toDouble()
            for (i in buffer.indices) {
                var f = centerFrequency[i].toDouble()
                var qi = q[i].toDouble()
                if (f < 0.001) f = 10.0
                if (qi < 0) qi = 0.0
                val omega = f * (2.0 * PI) / RATE
                var oneMinusR = if (qi < 0.001) 1.0 else omega / qi
                if (oneMinusR > 1.0) oneMinusR = 1.0
                val r = 1.0 - oneMinusR
                val coef1 = 2.0 * qcos(omega) * r
                val coef2 = -r * r
                val gain = 2 * oneMinusR * (oneMinusR + r * omega)
                val output = buffer[i] + coef1 * last + coef2 * prev
                result[i] = (gain * output).toFloat()
                prev = last
                last = output
            }
            return result
        }

        private fun qcos(f: Double): Double {
            if (f >= -(0.5 * PI) && f <= 0.5 * PI) {
                val g = f * f
                return (((g * g * g * (-1.0 / 720.0) + g * g * (1.0 / 24.0)) - g * 0.5) + 1)
            }
            return 0.0
        }

        fun vcf(buffer: FloatArray, resonantFrequency: FloatArray, q: FloatArray): FloatArray {
            val result = FloatArray(buffer.size)
            var re = 0.0
            var im = 0.0
            for (i in buffer.indices) {
                val qi = q[i].toDouble()
                val qinv = if (qi > 0) 1 / qi else 0.0
                val ampCorrect = 2 - 2 / (qi + 2)
                var cf = resonantFrequency[i] * CONVERSION_FACTOR
                if (cf < 0) cf = 0.0
                val cfIndex = cf * (COSTABLESIZE / 6.28318)
                var r = if (qinv > 0) 1 - cf * qinv else 0.0
                if (r < 0) r = 0.0
                val oneMinusR = 1 - r
                // upstream's tabfudge is a plain object, not a C union: its index reads stay 0 → tables 0 and 1536
                val frac = (cfIndex + UNITBIT32) - UNITBIT32
                var f1 = COS_TABLE[0].toDouble()
                var f2 = COS_TABLE[1].toDouble()
                val coefR = r * (f1 + frac * (f2 - f1))
                f1 = COS_TABLE[3 * COSTABLESIZE / 4].toDouble()
                f2 = COS_TABLE[3 * COSTABLESIZE / 4 + 1].toDouble()
                val coefI = r * (f1 + frac * (f2 - f1))
                val re2 = re
                re = ampCorrect * oneMinusR * buffer[i] + coefR * re2 - coefI * im
                im = coefI * re2 + coefR * im
                if (abs(re) < 1e-10) re = 0.0
                if (abs(im) < 1e-10) im = 0.0
                result[i] = re.toFloat()
            }
            return result
        }
    }
}
