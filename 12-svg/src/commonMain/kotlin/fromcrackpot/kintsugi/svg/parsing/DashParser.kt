package fromcrackpot.kintsugi.svg.parsing

import fromcrackpot.kintsugi.scenegraph.style.ShapeDash
import fromcrackpot.kintsugi.svg.presentationAttr
import fromcrackpot.kintsugi.svg.warn
import nl.adaptivity.xmlutil.XmlReader

/** SVG list separators: whitespace and/or commas. */
private val LIST_SEPARATORS = Regex("[\\s,]+")

/**
 * Recognized `stroke-dasharray`: `none` (case-insensitive) → empty list (authored solid), or plain
 * non-negative numbers in user units. Anything else (units, percentages, negatives, `inherit`)
 * returns `null`.
 */
internal fun parseDashArray(raw: String): List<Double>? {
    val v = raw.trim()
    if (v.equals("none", ignoreCase = true)) return emptyList()
    val tokens = v.split(LIST_SEPARATORS).filter { it.isNotEmpty() }
    if (tokens.isEmpty()) return null
    return tokens.map { token ->
        token.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 } ?: return null
    }
}

/** Recognized `stroke-dashoffset`: one finite plain number, negatives included. */
internal fun parseDashOffset(raw: String): Double? =
    raw.trim().toDoubleOrNull()?.takeIf { it.isFinite() }

/**
 * Read `stroke-dasharray` via [parseDashArray]. Cosmetic like `stroke-linecap`, so an unrecognized
 * value warns and yields `null` (falls through to the cascade).
 */
internal fun XmlReader.readDashArrayAttr(location: String?): List<Double>? =
    presentationAttr("stroke-dasharray")?.let { raw ->
        parseDashArray(raw) ?: run {
            warn("unrecognized stroke-dasharray=\"$raw\"${location?.let { " at $it" } ?: ""}; ignoring")
            null
        }
    }

/** Read `stroke-dashoffset` via [parseDashOffset], with [readDashArrayAttr]'s tolerance. */
internal fun XmlReader.readDashOffsetAttr(location: String?): Double? =
    presentationAttr("stroke-dashoffset")?.let { raw ->
        parseDashOffset(raw) ?: run {
            warn("unrecognized stroke-dashoffset=\"$raw\"${location?.let { " at $it" } ?: ""}; ignoring")
            null
        }
    }

/**
 * The element's resolved dash: own attrs over [inherited], each property nearest-wins.
 * No dasharray anywhere → `null` (unset), an offset alone included.
 */
internal fun XmlReader.readDash(inherited: InheritedStyle, location: String?): ShapeDash? {
    val array = readDashArrayAttr(location) ?: inherited.dashArray ?: return null
    return ShapeDash(array, readDashOffsetAttr(location) ?: inherited.dashOffset ?: 0.0)
}
