package fromcrackpot.kintsugi.svg.serializing

import fromcrackpot.kintsugi.scenegraph.style.ShapeStrokeCap
import nl.adaptivity.xmlutil.XmlWriter

/** Canonical SVG `stroke-linecap` keyword. */
internal fun ShapeStrokeCap.formatSvg(): String = when (this) {
    ShapeStrokeCap.BUTT -> "butt"
    ShapeStrokeCap.ROUND -> "round"
    ShapeStrokeCap.SQUARE -> "square"
}

/**
 * Write a `stroke-linecap` attribute. Callers gate on non-`Butt` — `butt` is the universal SVG
 * default, so it's omitted. An optional feature attribute (emitted only when active), like `k:flags`.
 */
internal fun XmlWriter.strokeCapAttr(name: String, cap: ShapeStrokeCap) {
    attribute(null, name, null, cap.formatSvg())
}
