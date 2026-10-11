package fromcrackpot.kintsugi.svg.parsing

import fromcrackpot.kintsugi.scenegraph.SceneBounds
import fromcrackpot.kintsugi.svg.MalformedViewBoxException
import fromcrackpot.kintsugi.svg.MissingViewBoxException
import fromcrackpot.kintsugi.svg.attrAsDouble
import nl.adaptivity.xmlutil.XmlReader

private val VIEWBOX_SEPARATOR = Regex("[\\s,]+")

/** Format: `"minX minY width height"`, comma or whitespace separated. */
internal fun parseViewBox(raw: String): SceneBounds? {
    val tokens = raw.trim().split(VIEWBOX_SEPARATOR)
    if (tokens.size != 4) return null
    val parsed = tokens.map { it.toDoubleOrNull() ?: return null }
    return try {
        SceneBounds(parsed[0], parsed[1], parsed[2], parsed[3])
    } catch (_: IllegalArgumentException) {
        null
    }
}

/**
 * Read the `viewBox` attribute off `<svg>` (or fall back to `width`/`height` if absent), throwing
 * domain exceptions at the parse boundary. Malformed `viewBox` → [MalformedViewBoxException];
 * missing entirely (and no width/height fallback) → [MissingViewBoxException].
 */
internal fun XmlReader.readViewBox(): SceneBounds {
    val raw = getAttributeValue(null, "viewBox")
    if (raw != null) return parseViewBox(raw) ?: throw MalformedViewBoxException(raw)

    val w = attrAsDouble("width")
    val h = attrAsDouble("height")
    if (w == null || h == null) throw MissingViewBoxException()
    return SceneBounds(0.0, 0.0, w, h)
}
