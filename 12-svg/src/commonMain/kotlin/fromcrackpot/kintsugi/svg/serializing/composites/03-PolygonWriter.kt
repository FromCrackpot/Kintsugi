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
 * Polygon → bare `<polygon>`, with no `k:type`, so designer-authored polygons "just work".
 *
 * - One `x,y` pair per vertex in traversal order (order = winding = data), each through the Y-flip.
 * - An inside-out polygon also emits `k:flags="inside-out"`. Flag and stored winding agree by
 *   invariant, so a re-parse reverses nothing and the round-trip is idempotent.
 * - Every other attribute is core (mirroring `<rect>`). `stroke-linecap` is never emitted, since
 *   the path is closed (like circle and rectangle).
 * - No skip guards: the `:cga` ctor makes degenerate polygons unrepresentable.
 * - Rectangle `Polygon`s land here too. `<rect>` is parse-only in `/v1`, so a rectangle
 *   re-serializes as `<polygon>` like any other quad.
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
