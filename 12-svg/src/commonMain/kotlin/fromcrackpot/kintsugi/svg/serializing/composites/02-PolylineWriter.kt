package fromcrackpot.kintsugi.svg.serializing.composites

import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polyline
import fromcrackpot.kintsugi.scenegraph.SceneBounds
import fromcrackpot.kintsugi.scenegraph.nodes.composites.PolylineNode
import fromcrackpot.kintsugi.scenegraph.style.ShapeStrokeCap
import fromcrackpot.kintsugi.svg.SVG_NS
import fromcrackpot.kintsugi.svg.cgaToSvg
import fromcrackpot.kintsugi.svg.kChromeAttr
import fromcrackpot.kintsugi.svg.kTagsAttr
import fromcrackpot.kintsugi.svg.serializing.colorAttr
import fromcrackpot.kintsugi.svg.serializing.formatSvgNumber
import fromcrackpot.kintsugi.svg.serializing.strokeCapAttr
import fromcrackpot.kintsugi.svg.serializing.strokeDashAttrs
import nl.adaptivity.xmlutil.XmlWriter

/**
 * Polyline → bare `<polyline>`, with no `k:type`, so designer-authored polylines "just work".
 *
 * - One `x,y` pair per vertex in traversal order (order = data), each through the Y-flip.
 * - Core attributes are always emitted (`points fill stroke stroke-width`). `stroke-linecap` is an
 *   optional feature, emitted only when non-`butt`: the open stroke follows the segment's rule,
 *   unlike the closed polygon, which never emits it.
 * - No skip guards: the `:cga` ctor makes degenerate polylines unrepresentable.
 */
internal fun XmlWriter.writePolylineElement(shape: PolylineNode, viewBox: SceneBounds) {
    val polyline: Polyline = shape
    val style = shape.style

    val points = polyline.vertices.joinToString(" ") { vertex ->
        val (svgX, svgY) = cgaToSvg(vertex.px, vertex.py, viewBox)
        "${svgX.formatSvgNumber()},${svgY.formatSvgNumber()}"
    }

    startTag(SVG_NS, "polyline", "")
    attribute(null, "points", null, points)
    colorAttr("fill", style.fill)
    colorAttr("stroke", style.stroke)
    attribute(null, "stroke-width", null, style.strokeWidth.formatSvgNumber())
    if (style.strokeCap != ShapeStrokeCap.BUTT) strokeCapAttr("stroke-linecap", style.strokeCap)
    val chrome = mutableListOf<String>()
    strokeDashAttrs(style, polyline, chrome)
    kChromeAttr(chrome)
    kTagsAttr(shape.tags)
    endTag(SVG_NS, "polyline", "")
}
