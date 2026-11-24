package fromcrackpot.kintsugi.svg.serializing.primitives

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.RoundPoint
import fromcrackpot.kintsugi.scenegraph.SceneBounds
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.RoundPointNode
import fromcrackpot.kintsugi.scenegraph.style.ImaginaryChrome
import fromcrackpot.kintsugi.scenegraph.style.ShapeDash
import fromcrackpot.kintsugi.scenegraph.style.ShapeStyle
import fromcrackpot.kintsugi.svg.*
import fromcrackpot.kintsugi.svg.serializing.colorAttr
import fromcrackpot.kintsugi.svg.serializing.dashAttrs
import fromcrackpot.kintsugi.svg.serializing.formatSvgNumber
import nl.adaptivity.xmlutil.XmlWriter

/**
 * Round point → `<circle k:type="round-point">`.
 *
 * - The element `r` is the render marker size (`size/2`), NOT the algebraic radius.
 * - `k:aw` is **always emitted**, a core attribute under the module's two-tier emission rule (core
 *   always, optional features only when active). The parser tolerates its absence on hand-authored input.
 * - An imaginary round point draws as a ring (see [writeRoundPointMarker]); its dash is the authored
 *   one, else `ImaginaryChrome.DASH` in `k:chrome`. A real one writes only an authored dash.
 * - A RoundPoint at infinity cannot be encoded, so it skips with a warning.
 */
internal fun XmlWriter.writeRoundPointCircle(shape: RoundPointNode, viewBox: SceneBounds) {
    val p: RoundPoint = shape
    val style = shape.style

    if (p.isAtInfinity) {
        warn("skipping RoundPoint at infinity (cannot encode as <circle>): $p")
        return
    }
    writeRoundPointMarker(
        p, style, viewBox,
        ring = p.isImaginary,
        dash = style.dash ?: style.dashFor(p),
        dashDerived = style.dash == null,
        tags = shape.tags,
    )
}

/**
 * The `<circle k:type="round-point">` element, shared with the dipole writer's two children.
 *
 * - [ring]: the imaginary marker. `fill` stays the color data; the ring is `fill-opacity="0"`,
 *   `stroke` = fill, `stroke-width` = size × `ImaginaryChrome.RING_WIDTH_RATIO`, all in `k:chrome`.
 * - [dash]: written as `stroke-dasharray`/`stroke-dashoffset`, listed in `k:chrome` when [dashDerived].
 *
 * Order: `k:type cx cy r k:aw fill [ring] [dash] [k:chrome] [k:tags]`.
 */
internal fun XmlWriter.writeRoundPointMarker(
    p: RoundPoint,
    style: ShapeStyle,
    viewBox: SceneBounds,
    ring: Boolean,
    dash: ShapeDash?,
    dashDerived: Boolean,
    tags: Set<String> = emptySet(),
) {
    val (svgCx, svgCy) = cgaToSvg(p.ax, p.ay, viewBox)
    val chrome = mutableListOf<String>()

    startTag(SVG_NS, "circle", "")
    kAttr(KATTR_TYPE, KTYPE_ROUND_POINT)
    attribute(null, "cx", null, svgCx.formatSvgNumber())
    attribute(null, "cy", null, svgCy.formatSvgNumber())
    attribute(null, "r", null, (style.size / 2.0).formatSvgNumber())
    kAttr(KATTR_AW, p.aw.formatSvgNumber())
    colorAttr("fill", style.fill)
    if (ring) {
        attribute(null, "fill-opacity", null, "0")
        colorAttr("stroke", style.fill)
        attribute(null, "stroke-width", null, (style.size * ImaginaryChrome.RING_WIDTH_RATIO).formatSvgNumber())
        chrome += listOf("fill-opacity", "stroke", "stroke-width")
    }
    dash?.let { dashAttrs(it, dashDerived, chrome) }
    kChromeAttr(chrome)
    kTagsAttr(tags)
    endTag(SVG_NS, "circle", "")
}
