package fromcrackpot.kintsugi.svg.serializing.primitives

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.scenegraph.SceneBounds
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.FlatPointNode
import fromcrackpot.kintsugi.svg.KATTR_TYPE
import fromcrackpot.kintsugi.svg.KTYPE_FLAT_POINT
import fromcrackpot.kintsugi.svg.SVG_NS
import fromcrackpot.kintsugi.svg.cgaToSvg
import fromcrackpot.kintsugi.svg.kAttr
import fromcrackpot.kintsugi.svg.kTagsAttr
import fromcrackpot.kintsugi.svg.serializing.colorAttr
import fromcrackpot.kintsugi.svg.serializing.formatSvgNumber
import fromcrackpot.kintsugi.svg.warn
import nl.adaptivity.xmlutil.XmlWriter

/**
 * Flat point → `<rect k:type="flat-point">`. The rect is square, `width = height = size`
 * (the render marker size, `ShapeStyle.size`), centered on the CGA position. A FlatPoint at
 * infinity cannot be encoded (no finite SVG position) — skip+warn at the boundary.
 */
internal fun XmlWriter.writeFlatPointRect(shape: FlatPointNode, viewBox: SceneBounds) {
    val p: FlatPoint = shape
    val style = shape.style

    if (p.isAtInfinity) {
        warn("skipping FlatPoint at infinity (cannot encode as <rect>): $p")
        return
    }

    val (svgCx, svgCy) = cgaToSvg(p.px, p.py, viewBox)
    val s = style.size
    val x = svgCx - s / 2.0
    val y = svgCy - s / 2.0

    startTag(SVG_NS, "rect", "")
    kAttr(KATTR_TYPE, KTYPE_FLAT_POINT)
    attribute(null, "x", null, x.formatSvgNumber())
    attribute(null, "y", null, y.formatSvgNumber())
    attribute(null, "width", null, s.formatSvgNumber())
    attribute(null, "height", null, s.formatSvgNumber())
    colorAttr("fill", style.fill)
    kTagsAttr(shape.tags)
    endTag(SVG_NS, "rect", "")
}
