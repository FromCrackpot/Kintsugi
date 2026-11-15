package fromcrackpot.kintsugi.bfxr

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Returns the sound a `.bfxr` document describes, checked against its synth's pinned version and parameters.
 *
 * - Stricter than bfxr2: an unknown key, or a value of the wrong shape or out of range, is an error, never ignored.
 * - An omitted parameter takes its default and an omitted `renderSeed` 0.5, as in bfxr2.
 *
 * @param text The JSON document: `synth_type`, `version`, `params`, and optionally `renderSeed` and `file_name`.
 * @return The checked sound.
 * @throws BfxrFormatException when [text] isn't JSON or fails the format, every problem listed with a did-you-mean
 *   hint for misspelt names.
 */
public fun parseBfxr(text: String): BfxrSound {
    val root = try {
        Json.parseToJsonElement(text)
    } catch (e: SerializationException) {
        throw BfxrFormatException(listOf("not valid JSON: ${e.message?.lineSequence()?.first()}"))
    }
    if (root !is JsonObject) throw BfxrFormatException(listOf("expected a JSON object at the top level"))

    val problems = mutableListOf<String>()
    for (key in root.keys - TOP_LEVEL_KEYS) problems += "$key: unknown key${didYouMean(key, TOP_LEVEL_KEYS)}"

    val synthId = root["synth_type"].string("synth_type", problems, required = true)
    val synth = synthId?.let { id ->
        BfxrSynth.entries.firstOrNull { it.id == id }
            ?: null.also { problems += "synth_type: unknown synth '$id'${didYouMean(id, BfxrSynth.entries.map { it.id })}" }
    }

    val version = root["version"].string("version", problems, required = true)
    if (synth != null && version != null && version != synth.version) {
        problems += "version: '$version' is not the supported ${synth.id} version '${synth.version}'"
    }

    root["file_name"].string("file_name", problems, required = false)

    val renderSeed = root["renderSeed"]?.let { element ->
        element.number("renderSeed", problems)?.also {
            if (it !in 0.0..1.0) problems += "renderSeed: $it lies outside [0, 1]"
        }
    } ?: 0.5

    val params = when (val element = root["params"]) {
        null -> emptyMap<String, BfxrValue>().also { problems += "params: missing" }
        !is JsonObject -> emptyMap<String, BfxrValue>().also { problems += "params: expected an object" }
        else -> if (synth != null) readParams(synth, element, "params", problems) else emptyMap()
    }

    if (problems.isNotEmpty() || synth == null) throw BfxrFormatException(problems)
    return BfxrSound(synth, params, renderSeed)
}

internal fun readParams(
    synth: BfxrSynth,
    element: JsonObject,
    path: String,
    problems: MutableList<String>,
): Map<String, BfxrValue> {
    val params = mutableMapOf<String, BfxrValue>()
    val names = synth.params.map { it.name }
    for ((name, value) in element) {
        val at = "$path.$name"
        val param = synth.params.firstOrNull { it.name == name }
        if (param == null) {
            problems += "$at: unknown ${synth.id} parameter${didYouMean(name, names)}"
            continue
        }
        val converted = value.toValue(param, at, problems) ?: continue
        val valueProblems = param.problemsWith(converted, at).ifEmpty { synth.problemsWith(name, converted, at) }
        if (valueProblems.isEmpty()) params[name] = converted else problems += valueProblems
    }
    return params
}

private val TOP_LEVEL_KEYS = setOf("synth_type", "version", "params", "renderSeed", "file_name")

private val JSON_NUMBER = Regex("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?")

internal fun JsonElement?.string(path: String, problems: MutableList<String>, required: Boolean): String? = when {
    this == null -> null.also { if (required) problems += "$path: missing" }
    this is JsonPrimitive && isString -> content
    else -> null.also { problems += "$path: expected a string" }
}

internal fun JsonElement.number(path: String, problems: MutableList<String>): Double? {
    val number = (this as? JsonPrimitive)
        ?.takeIf { !it.isString && it !is JsonNull && JSON_NUMBER.matches(it.content) }
        ?.content?.toDouble()
    return when {
        number == null -> null.also { problems += "$path: expected a number" }
        !number.isFinite() -> null.also { problems += "$path: expected a finite number" }
        else -> number
    }
}

private fun JsonElement.toValue(param: BfxrParam, path: String, problems: MutableList<String>): BfxrValue? =
    when (param) {
        is BfxrParam.Range, is BfxrParam.Choice -> number(path, problems)?.let(BfxrValue::Number)
        is BfxrParam.Text -> string(path, problems, required = true)?.let(BfxrValue::Text)
        is BfxrParam.Transition -> transition(param, path, problems)
    }

private val TRANSITION_KEYS = setOf("start", "end", "curve")

private fun JsonElement.transition(param: BfxrParam.Transition, path: String, problems: MutableList<String>): BfxrValue? {
    if (this !is JsonObject) return null.also { problems += "$path: expected an object with start, end and curve" }
    val before = problems.size
    for (key in keys - TRANSITION_KEYS) problems += "$path.$key: unknown key${didYouMean(key, TRANSITION_KEYS)}"
    val start = this["start"]?.number("$path.start", problems) ?: param.default.start
    val end = this["end"]?.number("$path.end", problems) ?: param.default.end
    val curve = this["curve"]?.let { it.string("$path.curve", problems, required = true) } ?: param.default.curve
    return if (problems.size == before) BfxrValue.Transition(start, end, curve) else null
}
