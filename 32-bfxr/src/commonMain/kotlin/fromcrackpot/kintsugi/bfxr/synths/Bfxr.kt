package fromcrackpot.kintsugi.bfxr.synths

import fromcrackpot.kintsugi.bfxr.BfxrParam
import fromcrackpot.kintsugi.bfxr.BfxrSynth
import fromcrackpot.kintsugi.bfxr.BfxrValue
import fromcrackpot.kintsugi.bfxr.dsp.AKWF_FMSYNTH_0012
import fromcrackpot.kintsugi.bfxr.dsp.AKWF_GRANULAR_0044
import fromcrackpot.kintsugi.bfxr.dsp.AKWF_HVOICE_0012
import fromcrackpot.kintsugi.bfxr.dsp.Mulberry32
import fromcrackpot.kintsugi.bfxr.dsp.RATE
import fromcrackpot.kintsugi.bfxr.dsp.clamp
import fromcrackpot.kintsugi.bfxr.dsp.toInt32
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

internal object Bfxr {
    private const val MIN_LENGTH = 0.18
    private const val LO_RES_NOISE_PERIOD = 8

    private val FREQUENCY_START = BfxrSynth.BFXR.range("frequency_start")
    private val MIN_FREQUENCY = BfxrSynth.BFXR.range("min_frequency_relative_to_starting_frequency")

    private fun BfxrSynth.range(name: String): BfxrParam.Range = params.first { it.name == name } as BfxrParam.Range

    private fun lerp(a: Double, b: Double, t: Double): Double = a + t * (b - a)

    fun render(params: Map<String, BfxrValue>, renderSeed: Double): FloatArray =
        Dsp(params.numbers(), Mulberry32(renderSeed)).generate()

    // ⇔ render(…) is empty: only a mute on sample 0 leaves no samples (the trim keeps ≥ 11).
    fun mutesAtOnce(params: Map<String, BfxrValue>): Boolean =
        Dsp(params.numbers(), Mulberry32(0.5)).generate(limit = 1).isEmpty()

    private fun Map<String, BfxrValue>.numbers(): Map<String, Double> =
        mapValues { (_, value) -> (value as BfxrValue.Number).value }

    private class Dsp(private val p: Map<String, Double>, private val random: Mulberry32) {
        private var attackTime = p.getValue("attackTime")
        private var sustainTime = p.getValue("sustainTime")
        private var decayTime = p.getValue("decayTime")

        private var frequencyPeriod = 0.0
        private var frequencyMaxPeriod = 0.0
        private var pitchJumpReached = false
        private var pitchJump2Reached = false
        private var masterVolume = 0.0
        private var waveType = 0
        private var sustainPunch = 0.0
        private var phase = 0.0
        private var minFrequency = 0.0
        private var muted = false
        private var overtones = 0.0
        private var overtoneFalloff = 0.0
        private var compressionFactor = 0.0
        private var filters = false
        private var vibratoPhase = 0.0
        private var vibratoSpeed = 0.0
        private var vibratoAmplitude = 0.0
        private var envelopeVolume = 0.0
        private var envelopeStage = 0
        private var envelopeTime = 0.0
        private var envelopeLength0 = 0.0
        private var envelopeLength1 = 0.0
        private var envelopeLength2 = 0.0
        private var attackLength = 0.0
        private var envelopeFullLength = 0.0
        private var bitcrushFreqSweep = 0.0
        private var bitcrushPhase = 0.0
        private var bitcrushLast = 0.0
        private var envelopeOverLength0 = 0.0
        private var envelopeOverLength1 = 0.0
        private var envelopeOverLength2 = 0.0
        private var flanger = false
        private var flangerDeltaOffset = 0.0
        private var flangerPos = 0
        private val flangerBuffer = FloatArray(1024)
        private val noiseBuffer = FloatArray(32)
        private val loResNoiseBuffer = FloatArray(32)
        private var oneBitNoiseState = 1 shl 14
        private var oneBitNoise = 0.0
        private var pitchJumpRepeatLength = 0.0
        private var pitchJumpAmount = 0.0
        private var pitchJump2Amount = 0.0
        private var pitchJumpCurrent = 0.0
        private var pitchJumpTimestamp = 0.0
        private var pitchJump2Timestamp = 0.0
        private var slide = 0.0
        private var frequencyAcceleration = 0.0
        private var flangerOffset = 0.0
        private var flangerInt = 0
        private var bitcrushFreq = 0.0
        private var squareDuty = 0.0
        private var dutySweep = 0.0
        private var lpFilterCutoff = 0.0
        private var lpFilterDeltaCutoff = 0.0
        private var lpFilterDamping = 0.0
        private var lpFilterOn = false
        private var lpFilterPos = 0.0
        private var lpFilterOldPos = 0.0
        private var lpFilterDeltaPos = 0.0
        private var hpFilterPos = 0.0
        private var hpFilterCutoff = 0.0
        private var hpFilterDeltaCutoff = 0.0
        private var paramResetPeriod = 0.0
        private var paramResetCurrent = 0.0

        init {
            reset(true)
        }

        private fun param(name: String): Double = p.getValue(name)

        private fun reset(total: Boolean) {
            val frequencyStart = param("frequency_start")
            frequencyPeriod = 100.0 / (frequencyStart * frequencyStart + 0.001)
            val minimumFrequency = param("min_frequency_relative_to_starting_frequency").pow(0.4) * frequencyStart
            frequencyMaxPeriod = 100.0 / (minimumFrequency * minimumFrequency + 0.001)
            pitchJumpReached = false
            pitchJump2Reached = false

            if (total) {
                masterVolume = param("masterVolume") * param("masterVolume")
                waveType = toInt32(param("waveType"))
                if (sustainTime < 0.01) sustainTime = 0.01
                val totalTime = attackTime + sustainTime + decayTime
                if (totalTime < MIN_LENGTH) {
                    val multiplier = MIN_LENGTH / totalTime
                    attackTime *= multiplier
                    sustainTime *= multiplier
                    decayTime *= multiplier
                }
                sustainPunch = param("sustainPunch")
                phase = 0.0
                minFrequency = param("min_frequency_relative_to_starting_frequency")
                muted = false
                overtones = param("overtones") * 10
                overtoneFalloff = param("overtoneFalloff")
                compressionFactor = 1 / (1 + 4 * param("compressionAmount"))
                filters = param("lpFilterCutoff") != 1.0 || param("hpFilterCutoff") != 0.0
                vibratoPhase = 0.0
                vibratoSpeed = param("vibratoSpeed") * param("vibratoSpeed") * 0.01
                vibratoAmplitude = param("vibratoDepth") * 0.5
                envelopeVolume = 0.0
                envelopeStage = 0
                envelopeTime = 0.0
                envelopeLength0 = attackTime * attackTime * 100000.0
                envelopeLength1 = sustainTime * sustainTime * 100000.0
                envelopeLength2 = decayTime * decayTime * 100000.0 + 10
                attackLength = envelopeLength0
                envelopeFullLength = envelopeLength0 + envelopeLength1 + envelopeLength2
                bitcrushFreqSweep = -param("bitCrushSweep") / envelopeFullLength
                bitcrushPhase = 0.0
                bitcrushLast = 0.0
                envelopeOverLength0 = 1.0 / envelopeLength0
                envelopeOverLength1 = 1.0 / envelopeLength1
                envelopeOverLength2 = 1.0 / envelopeLength2
                val flangerSweep = param("flangerSweep")
                flanger = param("flangerOffset") != 0.0 || flangerSweep != 0.0
                flangerDeltaOffset = flangerSweep * flangerSweep * flangerSweep * 0.2
                flangerPos = 0
                oneBitNoiseState = 1 shl 14
                oneBitNoise = 0.0
                flangerBuffer.fill(0f)
                for (i in 0 until 32) noiseBuffer[i] = (random.next() * 2.0 - 1.0).toFloat()
                for (i in 0 until 32) {
                    loResNoiseBuffer[i] =
                        if (i % LO_RES_NOISE_PERIOD == 0) (random.next() * 2.0 - 1.0).toFloat() else loResNoiseBuffer[i - 1]
                }
                pitchJumpRepeatLength =
                    lerp(envelopeFullLength, RATE / 50.0, param("pitch_jump_repeat_speed")) + 32
                val pitchJumpWindow = if (pitchJumpRepeatLength > 0) pitchJumpRepeatLength else envelopeFullLength
                pitchJumpAmount = pitchJumpFactor(param("pitch_jump_amount"))
                pitchJump2Amount = pitchJumpFactor(param("pitch_jump_2_amount"))
                pitchJumpCurrent = 0.0
                pitchJumpTimestamp = pitchJumpOnset(param("pitch_jump_onset_percent"), pitchJumpWindow)
                pitchJump2Timestamp = pitchJumpOnset(param("pitch_jump_onset2_percent"), pitchJumpWindow)

                if (waveType == 9) {
                    val startMid = (FREQUENCY_START.max + FREQUENCY_START.min) / 2
                    val minMid = (MIN_FREQUENCY.max + MIN_FREQUENCY.min) / 2
                    val deltaStart = (frequencyStart - FREQUENCY_START.min) / (FREQUENCY_START.max - FREQUENCY_START.min)
                    val minRelative = param("min_frequency_relative_to_starting_frequency")
                    val deltaMin = (minRelative - MIN_FREQUENCY.min) / (MIN_FREQUENCY.max - MIN_FREQUENCY.min)
                    val sf = startMid + deltaStart
                    val mf = minMid + deltaMin
                    frequencyPeriod = 100.0 / (sf * sf + 0.001)
                    frequencyMaxPeriod = 100.0 / (mf * mf + 0.001)
                }
            }

            val frequencySlide = param("frequency_slide")
            slide = 1.0 - frequencySlide * frequencySlide * frequencySlide * 0.01
            val acceleration = param("frequency_acceleration")
            frequencyAcceleration = -acceleration * acceleration * acceleration * 0.000001
            val offset = param("flangerOffset")
            flangerOffset = offset * offset * 1020.0
            if (offset < 0.0) flangerOffset = -flangerOffset
            bitcrushFreq = 1 - param("bitCrush").pow(1.0 / 3.0)
            squareDuty = 0.5 - param("squareDuty") * 0.5
            dutySweep = -param("dutySweep") * 0.00005
            val lpCutoff = param("lpFilterCutoff")
            lpFilterCutoff = lpCutoff * lpCutoff * lpCutoff * 0.1
            lpFilterDeltaCutoff = 1.0 + param("lpFilterCutoffSweep") * 0.0001
            val resonance = param("lpFilterResonance")
            lpFilterDamping = 5.0 / (1.0 + resonance * resonance * 20.0) * (0.01 + lpFilterCutoff)
            if (lpFilterDamping > 0.8) lpFilterDamping = 0.8
            lpFilterDamping = 1.0 - lpFilterDamping
            lpFilterOn = lpCutoff != 1.0
            lpFilterPos = 0.0
            lpFilterDeltaPos = 0.0
            hpFilterPos = 0.0
            val hpCutoff = param("hpFilterCutoff")
            hpFilterCutoff = hpCutoff * hpCutoff * 0.1
            hpFilterDeltaCutoff = 1.0 + param("hpFilterCutoffSweep") * 0.0003
            paramResetPeriod = lerp(envelopeFullLength, RATE / 10.0, param("repeatSpeed"))
            paramResetCurrent = 0.0
        }

        private fun pitchJumpFactor(amount: Double): Double =
            if (amount > 0.0) 1.0 - amount * amount * 0.9 else 1.0 + amount * amount * 10.0

        private fun pitchJumpOnset(percent: Double, window: Double): Double =
            if (percent == 1.0) 0.0 else percent * window + 32

        private fun sineApprox(phase: Double): Double {
            var pos = phase
            pos = if (pos > 0.5) (pos - 1.0) * 6.28318531 else pos * 6.28318531
            val s = if (pos < 0) 1.27323954 * pos + .405284735 * pos * pos else 1.27323954 * pos - 0.405284735 * pos * pos
            return if (s < 0) .225 * (s * -s - s) + s else .225 * (s * s - s) + s
        }

        private fun table(table: IntArray, tempPhase: Double, period: Int): Double =
            table[toInt32(tempPhase * 256 / period) % 256] / 32768.0 - 1

        fun generate(limit: Int = Int.MAX_VALUE): FloatArray {
            var buffer = FloatArray(envelopeFullLength.toInt())
            val length = envelopeFullLength
            var finished = false
            var lastNonZero = -1
            var i = 0
            while (i < length && i < limit) {
                if (finished) break

                if (paramResetPeriod != 0.0) {
                    paramResetCurrent++
                    if (paramResetCurrent >= paramResetPeriod) {
                        paramResetCurrent = 0.0
                        reset(false)
                    }
                }

                pitchJumpCurrent++
                if (pitchJumpCurrent >= pitchJumpRepeatLength) {
                    pitchJumpCurrent = 0.0
                    if (pitchJumpReached) {
                        frequencyPeriod /= pitchJumpAmount
                        pitchJumpReached = false
                    }
                    if (pitchJump2Reached) {
                        frequencyPeriod /= pitchJump2Amount
                        pitchJump2Reached = false
                    }
                }
                if (!pitchJumpReached && pitchJumpCurrent >= pitchJumpTimestamp) {
                    pitchJumpReached = true
                    frequencyPeriod *= pitchJumpAmount
                }
                if (!pitchJump2Reached && pitchJumpCurrent >= pitchJump2Timestamp) {
                    frequencyPeriod *= pitchJump2Amount
                    pitchJump2Reached = true
                }

                slide += frequencyAcceleration
                frequencyPeriod *= slide
                if (frequencyPeriod > frequencyMaxPeriod) {
                    frequencyPeriod = frequencyMaxPeriod
                    if (minFrequency > 0.0) muted = true
                }

                var periodTempDouble = frequencyPeriod
                if (vibratoAmplitude > 0.0) {
                    vibratoPhase += vibratoSpeed
                    periodTempDouble = frequencyPeriod * (1.0 + sin(vibratoPhase) * vibratoAmplitude)
                }
                var periodTemp = toInt32(periodTempDouble)
                if (periodTemp < 8) periodTemp = 8

                if (waveType == 0) {
                    squareDuty += dutySweep
                    if (squareDuty < 0.0) squareDuty = 0.001 else if (squareDuty > 0.5) squareDuty = 0.5
                }

                if (++envelopeTime > attackLength) {
                    envelopeTime = 0.0
                    when (++envelopeStage) {
                        1 -> attackLength = envelopeLength1
                        2 -> attackLength = envelopeLength2
                    }
                }
                when (envelopeStage) {
                    0 -> envelopeVolume = envelopeTime * envelopeOverLength0
                    1 -> envelopeVolume = 1.0 + (1.0 - envelopeTime * envelopeOverLength1) * 2.0 * sustainPunch
                    2 -> envelopeVolume = 1.0 - envelopeTime * envelopeOverLength2
                    3 -> {
                        envelopeVolume = 0.0
                        finished = true
                    }
                }

                if (flanger) {
                    flangerOffset += flangerDeltaOffset
                    flangerInt = toInt32(flangerOffset)
                    if (flangerInt < 0) flangerInt = -flangerInt else if (flangerInt > 1023) flangerInt = 1023
                }

                if (filters && hpFilterDeltaCutoff != 0.0) {
                    hpFilterCutoff *= hpFilterDeltaCutoff
                    if (hpFilterCutoff < 0.00001) hpFilterCutoff = 0.00001
                    else if (hpFilterCutoff > 0.1) hpFilterCutoff = 0.1
                }

                var superSample = 0.0
                for (j in 0 until 8) {
                    phase++
                    if (phase >= periodTemp) {
                        phase -= periodTemp
                        when (waveType) {
                            3 -> for (n in 0 until 32) noiseBuffer[n] = (random.next() * 2.0 - 1.0).toFloat()
                            6 -> for (n in 0 until 32) {
                                loResNoiseBuffer[n] = if (n % LO_RES_NOISE_PERIOD == 0) {
                                    (random.next() * 2.0 - 1.0).toFloat()
                                } else loResNoiseBuffer[n - 1]
                            }
                            9 -> {
                                val feedBit = ((oneBitNoiseState shr 1) and 1) xor (oneBitNoiseState and 1)
                                oneBitNoiseState = (oneBitNoiseState shr 1) or (feedBit shl 14)
                                oneBitNoise = (oneBitNoiseState.inv() and 1) - 0.5
                            }
                        }
                    }

                    var sample = 0.0
                    var overtoneStrength = 1.0
                    var k = 0
                    while (k <= overtones) {
                        val tempPhase = (phase * (k + 1)) % periodTemp
                        when (waveType) {
                            0 -> sample += overtoneStrength * (if (tempPhase / periodTemp < squareDuty) 0.5 else -0.5)
                            1 -> sample += overtoneStrength * (1.0 - (tempPhase / periodTemp) * 2.0)
                            2 -> sample += overtoneStrength * sineApprox(tempPhase / periodTemp)
                            3 -> sample += overtoneStrength * noiseBuffer[toInt32(tempPhase * 32 / periodTemp) % 32]
                            4 -> sample += overtoneStrength * (abs(1 - (tempPhase / periodTemp) * 2) - 1)
                            5 -> sample += overtoneStrength * table(AKWF_GRANULAR_0044, tempPhase, periodTemp)
                            6 -> sample += tan(PI * tempPhase / periodTemp) * overtoneStrength
                            7 -> {
                                var value = 0.75 * sineApprox(tempPhase / periodTemp)
                                value += 0.25 * sineApprox(((tempPhase * 20) % periodTemp) / periodTemp)
                                sample += overtoneStrength * value
                            }
                            8 -> {
                                val amp = tempPhase / periodTemp
                                sample += overtoneStrength * (abs(1 - amp * amp * 2) - 1)
                            }
                            9 -> sample += overtoneStrength * oneBitNoise
                            10 -> sample += overtoneStrength * table(AKWF_FMSYNTH_0012, tempPhase, periodTemp)
                            11 -> sample += overtoneStrength * table(AKWF_HVOICE_0012, tempPhase, periodTemp)
                        }
                        overtoneStrength *= (1 - overtoneFalloff)
                        k++
                    }

                    if (filters) {
                        lpFilterOldPos = lpFilterPos
                        lpFilterCutoff *= lpFilterDeltaCutoff
                        if (lpFilterCutoff < 0.0) lpFilterCutoff = 0.0 else if (lpFilterCutoff > 0.1) lpFilterCutoff = 0.1
                        if (lpFilterOn) {
                            lpFilterDeltaPos += (sample - lpFilterPos) * lpFilterCutoff
                            lpFilterDeltaPos *= lpFilterDamping
                        } else {
                            lpFilterPos = sample
                            lpFilterDeltaPos = 0.0
                        }
                        lpFilterPos += lpFilterDeltaPos
                        hpFilterPos += lpFilterPos - lpFilterOldPos
                        hpFilterPos *= 1.0 - hpFilterCutoff
                        sample = hpFilterPos
                    }

                    if (flanger) {
                        flangerBuffer[flangerPos and 1023] = sample.toFloat()
                        sample += flangerBuffer[(flangerPos - flangerInt + 1024) and 1023]
                        flangerPos = (flangerPos + 1) and 1023
                    }

                    superSample += sample
                }

                if (superSample > 8.0) superSample = 8.0 else if (superSample < -8.0) superSample = -8.0

                bitcrushPhase += bitcrushFreq
                if (bitcrushPhase > 1) {
                    bitcrushPhase = 0.0
                    bitcrushLast = superSample
                }
                val multiplier = lerp(1.0, 50 * bitcrushFreq, sqrt(bitcrushFreq))
                bitcrushFreq = max(min(bitcrushFreq + multiplier * bitcrushFreqSweep, 1.0), 0.00001)
                superSample = bitcrushLast

                superSample = masterVolume * envelopeVolume * superSample * 0.125
                superSample = if (superSample > 0) superSample.pow(compressionFactor)
                else -(-superSample).pow(compressionFactor)

                if (muted) {
                    buffer = buffer.copyOf(min(i, buffer.size))
                    break
                }
                if (abs(superSample) > 0.2e-2) lastNonZero = i
                if (i < buffer.size) buffer[i] = clamp(superSample, -1.0, 1.0).toFloat()
                i++
            }

            if (lastNonZero < buffer.size - 1) {
                lastNonZero = max(lastNonZero, 10)
                buffer = buffer.copyOf(min(lastNonZero + 1, buffer.size))
            }
            return buffer
        }
    }
}
