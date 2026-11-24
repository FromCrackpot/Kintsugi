package fromcrackpot.kintsugi.svg.serializing

import fromcrackpot.kintsugi.cga.d2.shapes.Shape
import fromcrackpot.kintsugi.scenegraph.style.ShapeDash
import fromcrackpot.kintsugi.scenegraph.style.ShapeStyle
import nl.adaptivity.xmlutil.XmlWriter

/** Canonical `stroke-dasharray` value: `none` when solid, else the runs space-joined. */
internal fun ShapeDash.formatDashArray(): String =
    if (isSolid) "none" else intervals.joinToString(" ") { it.formatSvgNumber() }

/**
 * Write [dash] as `stroke-dasharray`, plus `stroke-dashoffset` when non-zero. With [derived], the
 * written names join [chrome] for the element's `k:chrome`.
 */
internal fun XmlWriter.dashAttrs(dash: ShapeDash, derived: Boolean, chrome: MutableList<String>) {
    attribute(null, "stroke-dasharray", null, dash.formatDashArray())
    if (derived) chrome += "stroke-dasharray"
    if (dash.offset == 0.0) return
    attribute(null, "stroke-dashoffset", null, dash.offset.formatSvgNumber())
    if (derived) chrome += "stroke-dashoffset"
}

/**
 * The shape element's dash: the authored [ShapeStyle.dash] as is (`none` included), else the
 * derived default of [ShapeStyle.dashFor] listed in [chrome], else nothing.
 */
internal fun XmlWriter.strokeDashAttrs(style: ShapeStyle, shape: Shape, chrome: MutableList<String>) {
    val authored = style.dash
    if (authored != null) dashAttrs(authored, derived = false, chrome)
    else style.dashFor(shape)?.let { dashAttrs(it, derived = true, chrome) }
}
