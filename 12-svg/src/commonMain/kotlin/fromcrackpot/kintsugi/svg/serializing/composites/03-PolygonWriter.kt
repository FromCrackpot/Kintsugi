package fromcrackpot.kintsugi.svg.serializing.composites

import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.scenegraph.SceneBounds
import fromcrackpot.kintsugi.scenegraph.nodes.composites.PolygonNode
import fromcrackpot.kintsugi.svg.KATTR_FLAGS
import fromcrackpot.kintsugi.svg.KFLAG_INSIDE_OUT
import fromcrackpot.kintsugi.svg.SVG_NS
import fromcrackpot.kintsugi.svg.cgaToSvg
import fromcrackpot.kintsugi.svg.kAttr
import fromcrackpot.kintsugi.svg.kTagsAttr
import fromcrackpot.kintsugi.svg.serializing.colorAttr
import fromcrackpot.kintsugi.svg.serializing.formatSvgNumber
import nl.adaptivity.xmlutil.XmlWriter

/**
 * Polygon → bare `<polygon>` (no `k:type`; designer-authored polygons "just work"). One
 * `x,y` pair per vertex in traversal order (order = winding = data); coordinates go through
 * the Y-flip per vertex. An inside-out polygon additionally emits `k:flags="inside-out"` —
 * flag and stored winding agree by invariant, so a re-parse reverses nothing and the
 * round-trip is idempotent. All other emitted attributes are core (mirroring `<rect>`);
 * `stroke-linecap` is never emitted (closed path, same rule as circle/rectangle). No skip
 * guards — the `:cga` ctor makes degenerate polygons unrepresentable.
 *
 * Rectangle `Polygon`s also land here — `<rect>` is parse-only in `/v1`; a rectangle
 * re-serializes as `<polygon>` like any other quad.
 */
internal fun XmlWriter.writePolygonElement(shape: PolygonNode, viewBox: SceneBounds) {
    val polygon: Polygon = shape
    val style = shape.style

    val points = polygon.vertices.joinToString(" ") { vertex ->
        val (svgX, svgY) = cgaToSvg(vertex.px, vertex.py, viewBox)
        "${svgX.formatSvgNumber()},${svgY.formatSvgNumber()}"
    }

    startTag(SVG_NS, "polygon", "")
    attribute(null, "points", null, points)
    colorAttr("fill", style.fill)
    colorAttr("stroke", style.stroke)
    attribute(null, "stroke-width", null, style.strokeWidth.formatSvgNumber())
    if (polygon.isInsideOut) kAttr(KATTR_FLAGS, KFLAG_INSIDE_OUT)
    kTagsAttr(shape.tags)
    endTag(SVG_NS, "polygon", "")
}
