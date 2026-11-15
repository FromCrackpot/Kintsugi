package fromcrackpot.kintsugi.bfxr

import fromcrackpot.kintsugi.bfxr.synths.Bfxr
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlin.math.floor

internal fun BfxrSynth.problemsWith(name: String, value: BfxrValue, path: String): List<String> = when {
    value !is BfxrValue.Text -> emptyList()
    this == BfxrSynth.JINGLR && name == "phrase" -> buildList { readPhrase(value.value, path, this) }
    this == BfxrSynth.MIXR && name == "sources" -> buildList { readSources(value.value, path, this) }
    else -> emptyList()
}

internal fun bfxrMuteProblem(params: Map<String, BfxrValue>, path: String): String? {
    if (!Bfxr.mutesAtOnce(params)) return null
    val name = "min_frequency_relative_to_starting_frequency"
    val value = (params.getValue(name) as BfxrValue.Number).value
    return "$path.$name: $value mutes the sound before its first sample; the pitch starts below it"
}

internal class MixrSource(val synth: BfxrSynth, val params: Map<String, BfxrValue>, val renderSeed: Double?)

private const val MAX_SOURCES = 2
private const val MAX_SOURCE_NAME = 60
private val SOURCE_KEYS = setOf("synth", "name", "params", "generator", "selectedGenerator", "renderSeed")

internal fun readSources(text: String, path: String, problems: MutableList<String>): List<MixrSource?> {
    val root = try {
        Json.parseToJsonElement(text)
    } catch (e: SerializationException) {
        problems += "$path: not valid JSON: ${e.message?.lineSequence()?.first()}"
        return emptyList()
    }
    if (root !is JsonArray) {
        problems += "$path: expected a JSON array of sources"
        return emptyList()
    }
    if (root.size > MAX_SOURCES) problems += "$path: ${root.size} sources, at most $MAX_SOURCES"
    return root.mapIndexed { index, element -> readSource(element, "$path[$index]", problems) }
}

private fun readSource(element: JsonElement, at: String, problems: MutableList<String>): MixrSource? {
    if (element is JsonNull) return null
    if (element !is JsonObject) {
        problems += "$at: expected a source object or null"
        return null
    }
    val before = problems.size
    for (key in element.keys - SOURCE_KEYS) problems += "$at.$key: unknown key${didYouMean(key, SOURCE_KEYS)}"
    val synth = element["synth"].string("$at.synth", problems, required = true)?.let { id ->
        val sources = BfxrSynth.entries - BfxrSynth.MIXR
        when (id) {
            BfxrSynth.MIXR.id -> null.also { problems += "$at.synth: a Mixr source can't be a Mixr" }
            else -> sources.firstOrNull { it.id == id }
                ?: null.also { problems += "$at.synth: unknown synth '$id'${didYouMean(id, sources.map { it.id })}" }
        }
    }
    element["name"].string("$at.name", problems, required = false)?.let { name ->
        if (name.length > MAX_SOURCE_NAME) problems += "$at.name: longer than $MAX_SOURCE_NAME characters"
    }
    element["generator"].string("$at.generator", problems, required = false)
    element["selectedGenerator"].string("$at.selectedGenerator", problems, required = false)
    val renderSeed = element["renderSeed"]?.number("$at.renderSeed", problems)?.also {
        if (it !in 0.0..1.0) problems += "$at.renderSeed: $it lies outside [0, 1]"
    }
    val params = when (val p = element["params"]) {
        null -> emptyMap()
        !is JsonObject -> emptyMap<String, BfxrValue>().also { problems += "$at.params: expected an object" }
        else -> if (synth != null) readParams(synth, p, "$at.params", problems) else emptyMap()
    }
    if (synth == null || problems.size > before) return null
    val full = synth.params.associate { it.name to (params[it.name] ?: it.default) }
    if (synth == BfxrSynth.BFXR) {
        bfxrMuteProblem(full, "$at.params")?.let {
            problems += it
            return null
        }
    }
    return MixrSource(synth, full, renderSeed)
}

internal class PhraseNote(val degree: Int?, val beats: Double)

private const val MAX_NOTES = 12
private val NOTE_KEYS = setOf("degree", "beats")

internal fun readPhrase(text: String, path: String, problems: MutableList<String>): List<PhraseNote> {
    val root = try {
        Json.parseToJsonElement(text)
    } catch (e: SerializationException) {
        problems += "$path: not valid JSON: ${e.message?.lineSequence()?.first()}"
        return emptyList()
    }
    if (root !is JsonArray) {
        problems += "$path: expected a JSON array of notes"
        return emptyList()
    }
    if (root.isEmpty()) problems += "$path: expected at least one note"
    if (root.size > MAX_NOTES) problems += "$path: ${root.size} notes, at most $MAX_NOTES"
    return root.mapIndexedNotNull { index, element ->
        val at = "$path[$index]"
        if (element !is JsonObject) {
            problems += "$at: expected an object with degree and beats"
            return@mapIndexedNotNull null
        }
        for (key in element.keys - NOTE_KEYS) problems += "$at.$key: unknown key${didYouMean(key, NOTE_KEYS)}"
        val degree = when (val d = element["degree"]) {
            null -> 0
            JsonNull -> null
            else -> d.number("$at.degree", problems)?.let { value ->
                when {
                    value != floor(value) -> null.also { problems += "$at.degree: $value is not an integer" }
                    value !in -7.0..14.0 -> null.also { problems += "$at.degree: $value lies outside [-7, 14]" }
                    else -> value.toInt()
                }
            }
        }
        val beats = element["beats"]?.let { b ->
            b.number("$at.beats", problems)?.also { value ->
                if (value !in 0.25..2.0) problems += "$at.beats: $value lies outside [0.25, 2]"
                else if (value * 4 != floor(value * 4)) problems += "$at.beats: $value is not a multiple of 0.25"
            }
        } ?: 0.5
        PhraseNote(degree, beats)
    }
}

internal fun BfxrParam.problemsWith(value: BfxrValue, path: String): List<String> = when (this) {
    is BfxrParam.Range -> when {
        value !is BfxrValue.Number -> listOf("$path: expected a number")
        !value.value.isFinite() -> listOf("$path: expected a finite number")
        value.value !in min..max -> listOf("$path: ${value.value} lies outside [$min, $max]")
        else -> emptyList()
    }

    is BfxrParam.Choice -> when {
        value !is BfxrValue.Number -> listOf("$path: expected a number")
        options.none { it.toDouble() == value.value } ->
            listOf("$path: ${value.value} is not one of ${options.sorted().joinToString(", ", "[", "]")}")
        else -> emptyList()
    }

    is BfxrParam.Transition -> when (value) {
        !is BfxrValue.Transition -> listOf("$path: expected an object with start, end and curve")
        else -> buildList {
            for ((end, number) in listOf("start" to value.start, "end" to value.end)) {
                if (!number.isFinite()) add("$path.$end: expected a finite number")
                else if (number !in min..max) add("$path.$end: $number lies outside [$min, $max]")
            }
            if (value.curve !in curves) add("$path.curve: '${value.curve}' is not one of ${curves.joinToString(", ")}")
        }
    }

    is BfxrParam.Text -> when {
        value !is BfxrValue.Text -> listOf("$path: expected a string")
        value.value.length > maxLength -> listOf("$path: longer than $maxLength characters")
        else -> emptyList()
    }
}

internal fun didYouMean(name: String, candidates: Iterable<String>): String {
    val key = name.lowercase()
    val best = candidates.minByOrNull { editDistance(key, it.lowercase()) } ?: return ""
    val distance = editDistance(key, best.lowercase())
    return if (distance <= maxOf(2, best.length / 3)) "; did you mean '$best'?" else ""
}

internal fun editDistance(a: String, b: String): Int {
    val d = Array(a.length + 1) { IntArray(b.length + 1) }
    for (i in 0..a.length) d[i][0] = i
    for (j in 0..b.length) d[0][j] = j
    for (i in 1..a.length) {
        for (j in 1..b.length) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            d[i][j] = minOf(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + cost)
            if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                d[i][j] = minOf(d[i][j], d[i - 2][j - 2] + 1)
            }
        }
    }
    return d[a.length][b.length]
}
