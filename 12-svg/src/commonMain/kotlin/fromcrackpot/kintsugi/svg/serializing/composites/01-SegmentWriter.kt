package fromcrackpot.kintsugi.svg.serializing.composites

import fromcrackpot.kintsugi.cga.d2.shapes.composites.Segment
import fromcrackpot.kintsugi.scenegraph.SceneBounds
import fromcrackpot.kintsugi.scenegraph.nodes.composites.SegmentNode
import fromcrackpot.kintsugi.scenegraph.style.ShapeStrokeCap
import fromcrackpot.kintsugi.svg.SVG_NS
import fromcrackpot.kintsugi.svg.cgaToSvg
import fromcrackpot.kintsugi.svg.kTagsAttr
import fromcrackpot.kintsugi.svg.serializing.colorAttr
import fromcrackpot.kintsugi.svg.serializing.formatSvgNumber
import fromcrackpot.kintsugi.svg.serializing.strokeCapAttr
import nl.adaptivity.xmlutil.XmlWriter

/**
 * Bounded segment → bare `<line>`. Endpoints are read directly off the unitized round-point
 * endpoints (`az = 1` by construction, so `.ax/.ay` are the Euclidean coords — same idiom as
 * dipole-endpoint rendering). No `k:type` attribute — bare `<line>` is the untagged default for
 * segments; `k:type="line"` is reserved for infinite CGA lines.
 */
internal fun XmlWriter.writeSegmentElement(shape: SegmentNode, viewBox: SceneBounds) {
    val segment: Segment = shape
    val style = shape.style

    val (sx1, sy1) = cgaToSvg(segment.start.ax, segment.start.ay, viewBox)
    val (sx2, sy2) = cgaToSvg(segment.end.ax, segment.end.ay, viewBox)

    startTag(SVG_NS, "line", "")
    attribute(null, "x1", null, sx1.formatSvgNumber())
    attribute(null, "y1", null, sy1.formatSvgNumber())
    attribute(null, "x2", null, sx2.formatSvgNumber())
    attribute(null, "y2", null, sy2.formatSvgNumber())
    colorAttr("stroke", style.stroke)
    attribute(null, "stroke-width", null, style.strokeWidth.formatSvgNumber())
    // Optional feature attribute: `butt` is the SVG default, so it's omitted (like k:flags).
    if (style.strokeCap != ShapeStrokeCap.BUTT) strokeCapAttr("stroke-linecap", style.strokeCap)
    kTagsAttr(shape.tags)
    endTag(SVG_NS, "line", "")
}
