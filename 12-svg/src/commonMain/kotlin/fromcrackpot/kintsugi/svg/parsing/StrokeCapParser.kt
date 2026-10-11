package fromcrackpot.kintsugi.svg.parsing

import fromcrackpot.kintsugi.scenegraph.style.ShapeStrokeCap
import fromcrackpot.kintsugi.svg.warn
import nl.adaptivity.xmlutil.XmlReader

/**
 * Recognized: `butt`, `round`, `square` (SVG 1.1 `stroke-linecap` keywords, case-insensitive).
 * Anything else — including SVG 2's `inherit` — returns `null`, letting the caller fall back to the
 * inherited cascade / [SvgDefaults.STROKE_CAP]. Cosmetic property, so an unknown value is a
 * warn-and-fall-back (see [readStrokeCapAttr]), not a hard `Malformed…` failure.
 */
internal fun parseStrokeCap(raw: String): ShapeStrokeCap? = when (raw.trim().lowercase()) {
    "butt" -> ShapeStrokeCap.BUTT
    "round" -> ShapeStrokeCap.ROUND
    "square" -> ShapeStrokeCap.SQUARE
    else -> null
}

/**
 * Read a `stroke-linecap` attribute and decode it via [parseStrokeCap]. A present-but-unrecognized
 * value warns and yields `null` (tolerant fall-through to the cascade), unlike `readColorAttr`'s
 * throw-on-unknown posture — cap is cosmetic and a bad keyword shouldn't fail the whole parse.
 */
internal fun XmlReader.readStrokeCapAttr(name: String, location: String?): ShapeStrokeCap? =
    getAttributeValue(null, name)?.let { raw ->
        parseStrokeCap(raw) ?: run {
            warn("unrecognized $name=\"$raw\"${location?.let { " at $it" } ?: ""}; ignoring")
            null
        }
    }
