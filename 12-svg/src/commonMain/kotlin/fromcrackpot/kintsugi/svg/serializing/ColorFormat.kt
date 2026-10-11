package fromcrackpot.kintsugi.svg.serializing

import fromcrackpot.kintsugi.scenegraph.style.ShapeColor
import nl.adaptivity.xmlutil.XmlWriter

/**
 * Canonical SVG paint serialization. Any fully-transparent color (`alpha == 0`) collapses to
 * `"none"` — the canonical SVG spelling for "no paint." `#00000000` and `ShapeColor.NONE` are
 * the same equivalence class under data-class equality and both emit `"none"`. Opaque colors
 * emit 6-digit hex (`#rrggbb`); partial alpha emits 8-digit hex (`#rrggbbaa`).
 */
internal fun ShapeColor.formatHex(): String {
    if (a == 0) return "none"
    return buildString {
        append('#')
        appendHexByte(r)
        appendHexByte(g)
        appendHexByte(b)
        if (a != 0xFF) appendHexByte(a)
    }
}

private fun StringBuilder.appendHexByte(v: Int) {
    if (v < 0x10) append('0')
    append(v.toString(16))
}

/** Write an SVG paint attribute (e.g. `fill="#ff0000"`) using the canonical hex spelling. */
internal fun XmlWriter.colorAttr(name: String, color: ShapeColor) {
    attribute(null, name, null, color.formatHex())
}
