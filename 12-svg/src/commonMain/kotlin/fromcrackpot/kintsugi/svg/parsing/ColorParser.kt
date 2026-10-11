package fromcrackpot.kintsugi.svg.parsing

import fromcrackpot.kintsugi.scenegraph.style.ShapeColor
import fromcrackpot.kintsugi.svg.UnsupportedPaintException
import nl.adaptivity.xmlutil.XmlReader

/**
 * Recognized: `none` (→ `ShapeColor.NONE`), `#rgb`, `#rrggbb`, `#rgba`, `#rrggbbaa`,
 * `rgb(r,g,b)`, `rgba(r,g,b,a)` (a in 0..1).
 *
 * Canonicalization: any zero-alpha color (`#00000000`, `#12345600`, `rgba(255,0,0,0)`, …)
 * collapses to [ShapeColor.NONE] — the SVG model treats all fully-transparent paints as
 * equivalent to "no paint."
 */
internal fun parseColor(raw: String): ShapeColor? {
    val v = raw.trim().lowercase()
    if (v.isEmpty()) return null
    if (v == "none") return ShapeColor.NONE
    val parsed = parseHex(v) ?: parseRgbFunctional(v) ?: return null
    return if (parsed.a == 0) ShapeColor.NONE else parsed
}

private fun parseHex(v: String): ShapeColor? {
    if (!v.startsWith('#')) return null
    val hex = v.substring(1)
    return when (hex.length) {
        3 -> {
            val r = hex.nibble(0) ?: return null
            val g = hex.nibble(1) ?: return null
            val b = hex.nibble(2) ?: return null
            ShapeColor.rgb(r * 0x11, g * 0x11, b * 0x11)
        }
        4 -> {
            val r = hex.nibble(0) ?: return null
            val g = hex.nibble(1) ?: return null
            val b = hex.nibble(2) ?: return null
            val a = hex.nibble(3) ?: return null
            ShapeColor.rgb(r * 0x11, g * 0x11, b * 0x11, a * 0x11)
        }
        6 -> {
            val r = hex.byteAt(0) ?: return null
            val g = hex.byteAt(2) ?: return null
            val b = hex.byteAt(4) ?: return null
            ShapeColor.rgb(r, g, b)
        }
        8 -> {
            val r = hex.byteAt(0) ?: return null
            val g = hex.byteAt(2) ?: return null
            val b = hex.byteAt(4) ?: return null
            val a = hex.byteAt(6) ?: return null
            ShapeColor.rgb(r, g, b, a)
        }
        else -> null
    }
}

private fun parseRgbFunctional(v: String): ShapeColor? {
    val withAlpha = v.startsWith("rgba(")
    val opaque = v.startsWith("rgb(")
    if (!withAlpha && !opaque) return null
    if (!v.endsWith(')')) return null
    val inner = v.substring(v.indexOf('(') + 1, v.length - 1)
    val parts = inner.split(',').map { it.trim() }
    val expected = if (withAlpha) 4 else 3
    if (parts.size != expected) return null

    val r = parts[0].toIntOrNull()?.takeIf { it in 0..255 } ?: return null
    val g = parts[1].toIntOrNull()?.takeIf { it in 0..255 } ?: return null
    val b = parts[2].toIntOrNull()?.takeIf { it in 0..255 } ?: return null
    if (!withAlpha) return ShapeColor.rgb(r, g, b)

    val a = parts[3].toDoubleOrNull() ?: return null
    val alphaByte = (a.coerceIn(0.0, 1.0) * 255.0 + 0.5).toInt().coerceIn(0, 255)
    return ShapeColor.rgb(r, g, b, alphaByte)
}

private fun String.nibble(i: Int): Int? = this[i].digitToIntOrNull(16)

private fun String.byteAt(i: Int): Int? {
    val hi = nibble(i) ?: return null
    val lo = nibble(i + 1) ?: return null
    return (hi shl 4) or lo
}

/**
 * Read a paint attribute (`fill` or `stroke`) and decode it via [parseColor]. A null return from
 * [parseColor] becomes [UnsupportedPaintException] carrying the raw value and source location —
 * one exception covers both fill and stroke since color parsing is identical for both.
 */
internal fun XmlReader.readColorAttr(name: String, location: String?): ShapeColor? =
    getAttributeValue(null, name)?.let { raw ->
        parseColor(raw) ?: throw UnsupportedPaintException(raw, location)
    }
