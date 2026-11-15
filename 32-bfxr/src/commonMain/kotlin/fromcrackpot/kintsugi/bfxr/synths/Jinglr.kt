package fromcrackpot.kintsugi.bfxr.synths

import fromcrackpot.kintsugi.bfxr.BfxrValue
import fromcrackpot.kintsugi.bfxr.PhraseNote
import fromcrackpot.kintsugi.bfxr.dsp.Mulberry32
import fromcrackpot.kintsugi.bfxr.dsp.RATE
import fromcrackpot.kintsugi.bfxr.dsp.finish
import fromcrackpot.kintsugi.bfxr.dsp.jsRound
import fromcrackpot.kintsugi.bfxr.dsp.value
import fromcrackpot.kintsugi.bfxr.readPhrase
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

internal object Jinglr {
    private val SCALES = listOf(
        intArrayOf(0, 2, 4, 5, 7, 9, 11),
        intArrayOf(0, 2, 3, 5, 7, 8, 10),
        intArrayOf(0, 2, 4, 7, 9),
        intArrayOf(0, 2, 3, 5, 7, 9, 10),
    )

    private val ATTACK_RANGES = listOf(
        0.001 to 0.012, 0.001 to 0.006, 0.001 to 0.009, 0.006 to 0.04,
        0.001 to 0.012, 0.012 to 0.06, 0.001 to 0.015, 0.025 to 0.12,
    )

    private class Event(val midi: Int?, val frequency: Double, val start: Double, val duration: Double)

    private class Voice(
        val instrument: Int,
        val seed: Int,
        val attack: Double,
        val release: Double,
        val damping: Double,
        val slope: Double,
        val color: Double,
        val hollow: Double,
        val filter: Double,
        val body: Double,
        val modulation: Double,
        val modRatio: Double,
        val spread: Double,
        val pick: Double,
    )

    private class Partial(val step: Double, var gain: Double, val falloff: Double, val offset: Double)

    private fun midi(degree: Int?, p: Map<String, BfxrValue>): Int? {
        if (degree == null) return null
        val scale = SCALES[jsRound(p.value("scale", 0.0, 0.0, 3.0)).toInt()]
        val octave = jsRound(p.value("octave", 4.0, 3.0, 6.0)).toInt()
        val key = jsRound(p.value("key", 0.0, 0.0, 11.0)).toInt()
        val index = (degree % scale.size + scale.size) % scale.size
        return (octave + 1) * 12 + key + floor(degree.toDouble() / scale.size).toInt() * 12 + scale[index]
    }

    private fun schedule(p: Map<String, BfxrValue>): Pair<List<Event>, Double> {
        val beat = 60 / p.value("tempo", 140.0, 60.0, 220.0)
        val swing = p.value("swing", 0.0, 0.0, 0.6)
        var cursor = 0.0
        val phrase: List<PhraseNote> = readPhrase((p["phrase"] as BfxrValue.Text).value, "phrase", mutableListOf())
        val events = phrase.mapIndexed { index, note ->
            val pair = index / 2 * 2
            val shift = if (pair + 1 < phrase.size) min(phrase[pair].beats, phrase[pair + 1].beats) * swing else 0.0
            val duration = (note.beats + (if (index % 2 != 0) -shift else shift)) * beat
            val midi = midi(note.degree, p)
            val event = Event(midi, if (midi == null) 0.0 else 440 * 2.0.pow((midi - 69) / 12.0), cursor, duration)
            cursor += duration
            event
        }
        return events to cursor
    }

    private fun voice(p: Map<String, BfxrValue>): Voice {
        val instrument = jsRound(p.value("instrument", 0.0, 0.0, 7.0)).toInt()
        val seed = jsRound(p.value("instrumentSeed", 42731.0, 0.0, 99999.0)).toInt()
        val random = Mulberry32((instrument * 100000.0 + seed + 1) / 800001)
        val (attackLow, attackHigh) = ATTACK_RANGES[instrument]
        return Voice(
            instrument = instrument,
            seed = seed,
            attack = attackLow + random.next() * (attackHigh - attackLow),
            release = 0.6 + random.next() * 0.85,
            damping = 0.45 + random.next() * 1.9,
            slope = 0.65 + random.next() * 1.5,
            color = 0.2 + random.next() * 0.75,
            hollow = random.next(),
            filter = 1.5 + random.next() * 9,
            body = 0.65 + random.next() * 0.35,
            modulation = 0.3 + random.next() * 3.5,
            modRatio = 1 + floor(random.next() * 4),
            spread = 0.001 + random.next() * 0.005,
            pick = 0.08 + random.next() * 0.4,
        )
    }

    fun render(p: Map<String, BfxrValue>): FloatArray {
        val (events, scoreDuration) = schedule(p)
        val rate = RATE.toDouble()
        val brightness = p.value("brightness", 0.55, 0.0, 1.0)
        val decay = p.value("decay", 0.45, 0.0, 1.0)
        val echo = p.value("echo", 0.12, 0.0, 0.8)
        val voice = voice(p)
        val instrument = voice.instrument
        val tail = (0.035 + decay * 0.26) * voice.release
        val delay = 60 / p.value("tempo", 140.0, 60.0, 220.0) * 0.75
        val echoTail = if (echo > 0) delay * 3 else 0.0
        val pcm = FloatArray(ceil((scoreDuration + tail + echoTail) * rate).toInt())
        val random = Mulberry32(((voice.seed * 7 + instrument * 100003 + 1) % 1000003) / 1000003.0)
        for (note in events) {
            if (note.midi == null) continue
            val start = jsRound(note.start * rate).toInt()
            val gate = note.duration * (0.52 + 0.38 * decay) * voice.body
            val length = ceil((gate + tail) * rate).toInt()
            val strength = 0.84 + random.next() * 0.16
            val phase = random.next() * PI * 2
            val partials = ArrayList<Partial>()
            fun addPartial(ratio: Double, gain: Double, falloff: Double, offset: Double = 0.0) {
                if (note.frequency * ratio < rate * 0.45) {
                    val step = PI * 2 * note.frequency * ratio / rate
                    partials += Partial(step, gain, exp(-falloff * voice.damping / rate), offset)
                }
            }
            when (instrument) {
                0 -> for (h in 1..10) {
                    val pick = if (h == 1) 1.0 else 0.25 + 0.75 * abs(sin(PI * h * voice.pick))
                    addPartial(
                        h.toDouble(),
                        pick * (0.12 + brightness * 0.85).pow(h - 1) / h.toDouble().pow(voice.slope),
                        (1.5 + (1 - decay) * 7) * sqrt(h.toDouble()),
                    )
                }
                1 -> doubleArrayOf(1.0, 2 + voice.color * 1.1, 3.8 + voice.hollow * 2.3, 7.5 + voice.color * 2.5)
                    .forEachIndexed { h, ratio ->
                        addPartial(
                            ratio,
                            if (h == 0) 0.8 else (0.1 + brightness * 0.85) / (h + 1).toDouble().pow(voice.slope),
                            (0.7 + (1 - decay) * 5) * (1 + h * 0.6),
                        )
                    }
                2 -> {
                    val duty = 0.12 + voice.hollow * 0.38
                    for (h in 1..16) addPartial(
                        h.toDouble(),
                        0.8 * sin(PI * h * duty) / sin(PI * duty) * (0.3 + brightness * 0.7).pow(h - 1) /
                            h.toDouble().pow(voice.slope),
                        0.45 + (1 - decay) * 2,
                    )
                }
                3 -> {
                    addPartial(1.0, 0.8, 0.35 + (1 - decay) * 1.5)
                    for (h in 2..5) addPartial(
                        h.toDouble(),
                        brightness * voice.color * 0.5 / (h - 1).toDouble().pow(voice.slope),
                        (0.35 + (1 - decay) * 1.5) * (1 + h * 0.15),
                    )
                }
                4 -> {
                    for (h in 1..8) addPartial(
                        h.toDouble(),
                        (if (h == 1) 0.85 else (0.2 + brightness * 0.7) * (if (h % 2 != 0) 0.55 else 1.0)) /
                            h.toDouble().pow(voice.slope),
                        (0.8 + (1 - decay) * 4) * (1 + h * 0.3),
                    )
                    addPartial(4 + voice.color * 0.04, brightness * voice.hollow * 0.35, 12 + (1 - decay) * 12)
                }
                5 -> for (h in 1..12) {
                    val resonance = 1 + voice.color * exp(-((h - (2 + voice.hollow * 4)) / 2).pow(2)) * 2
                    val even = if (h % 2 != 0) 1.0 else 0.1 + voice.hollow * 0.85
                    addPartial(
                        h.toDouble(),
                        0.65 * resonance * even * (0.35 + brightness * 0.65).pow(h - 1) / h.toDouble().pow(voice.slope),
                        0.2 + (1 - decay) * 0.8,
                    )
                }
                6 -> addPartial(1.0, 0.28, 0.5 + (1 - decay) * 3)
                else -> for (h in 1..10) {
                    val gain = 0.32 * (0.5 + brightness * 0.48).pow(h - 1) / h.toDouble().pow(voice.slope)
                    addPartial(h * (1 - voice.spread), gain, 0.15 + (1 - decay) * 0.6)
                    addPartial(h * (1 + voice.spread), gain, 0.15 + (1 - decay) * 0.6, voice.color * 2)
                }
            }
            val startPhase = if (instrument == 2) 0.0 else phase
            val attackLength = min(voice.attack, gate * 0.65) * rate
            var breath = 0.005 * brightness * (0.3 + voice.hollow)
            val breathFalloff = exp(-(0.5 + (1 - decay) * 2) * voice.damping / rate)
            val cutoff = min(rate * 0.44, note.frequency * (voice.filter + brightness * 9))
            val filter = 1 - exp(-PI * 2 * cutoff / rate)
            var filtered = 0.0
            val fundamental = PI * 2 * note.frequency / rate
            val fmIndex = min(
                voice.modulation * (0.15 + brightness),
                max(0.0, (rate * 0.4 / note.frequency - 1) / voice.modRatio),
            )
            var fmAmount = fmIndex
            var fmGain = 0.62
            val fmFalloff = exp(-(2 + (1 - decay) * 8) * voice.damping / rate)
            val fmGainFalloff = exp(-(0.6 + (1 - decay) * 3) * voice.damping / rate)
            var i = 0
            while (i < length && start + i < pcm.size) {
                val time = i / rate
                val attack = if (i < attackLength) i / attackLength else 1.0
                val release = if (time > gate) 1 - (time - gate) / tail else 1.0
                var sample = 0.0
                for (partial in partials) {
                    sample += sin(partial.step * i + startPhase + partial.offset) * partial.gain
                    partial.gain *= partial.falloff
                }
                if (instrument == 3 || instrument == 5) {
                    sample += (random.next() * 2 - 1) * breath
                    breath *= breathFalloff
                }
                if (instrument == 6) {
                    sample += sin(
                        fundamental * i + startPhase + (fmIndex * 0.18 + fmAmount) * sin(fundamental * i * voice.modRatio),
                    ) * fmGain
                    fmAmount *= fmFalloff
                    fmGain *= fmGainFalloff
                }
                filtered += (sample - filtered) * filter
                pcm[start + i] = (pcm[start + i] + filtered * attack * release * strength).toFloat()
                i++
            }
        }
        if (echo > 0) {
            val dry = pcm.copyOf()
            for (repeat in 1..3) {
                val offset = jsRound(delay * repeat * rate).toInt()
                val gain = (echo * 0.58).pow(repeat)
                var i = 0
                while (i + offset < pcm.size) {
                    pcm[i + offset] = (pcm[i + offset] + dry[i] * gain).toFloat()
                    i++
                }
            }
        }
        return finish(pcm, p.value("masterVolume", 0.5, 0.0, 1.0))
    }
}
