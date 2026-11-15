package fromcrackpot.kintsugi.svg.serializing.composites

import fromcrackpot.kintsugi.cga.d2.shapes.composites.Segment
import fromcrackpot.kintsugi.scenegraph.SceneBounds
import fromcrackpot.kintsugi.scenegraph.nodes.composites.SegmentNode
import fromcrackpot.kintsugi.scenegraph.style.ShapeStrokeCap
import fromcrackpot.kintsugi.svg.KATTR_FLAGS
import fromcrackpot.kintsugi.svg.KFLAG_IMAGINARY
import fromcrackpot.kintsugi.svg.SVG_NS
import fromcrackpot.kintsugi.svg.cgaToSvg
import fromcrackpot.kintsugi.svg.decodeSegment
import fromcrackpot.kintsugi.svg.kAttr
import fromcrackpot.kintsugi.svg.kTagsAttr
import fromcrackpot.kintsugi.svg.serializing.colorAttr
import fromcrackpot.kintsugi.svg.serializing.formatSvgNumber
import fromcrackpot.kintsugi.svg.serializing.strokeCapAttr
import fromcrackpot.kintsugi.svg.warn
import nl.adaptivity.xmlutil.XmlWriter

/**
 * Bounded segment → bare `<line>`.
 *
 * - Endpoints are read directly off the unitized round-point endpoints: `az = 1`, so `.ax/.ay` are
 *   the Euclidean coords (the dipole-endpoint rendering idiom).
 * - No `k:type`: bare `<line>` is the untagged default for segments, and `k:type="line"` is
 *   reserved for infinite CGA lines.
 * - An imaginary segment writes its real-valued endpoint pair plus `k:flags="imaginary"` (the
 *   circle's flag, from the rounded `isImaginary`).
 * - Skip+warn: a segment at infinity (no finite endpoints), and one whose endpoints coincide (it
 *   wouldn't reload, see [decodeSegment]).
 */
internal fun XmlWriter.writeSegmentElement(shape: SegmentNode, viewBox: SceneBounds) {
    val segment: Segment = shape
    val style = shape.style

    if (segment.toDipole.isAtInfinity) {
        warn("skipping Segment at infinity (no finite endpoints to encode): $segment")
        return
    }
    val (sx1, sy1) = cgaToSvg(segment.start.ax, segment.start.ay, viewBox)
    val (sx2, sy2) = cgaToSvg(segment.end.ax, segment.end.ay, viewBox)
    if (decodeSegment(sx1, sy1, sx2, sy2, viewBox) == null) {
        warn("skipping zero-length Segment (coincident endpoints would not reload): $segment")
        return
    }

    startTag(SVG_NS, "line", "")
    attribute(null, "x1", null, sx1.formatSvgNumber())
    attribute(null, "y1", null, sy1.formatSvgNumber())
    attribute(null, "x2", null, sx2.formatSvgNumber())
    attribute(null, "y2", null, sy2.formatSvgNumber())
    colorAttr("stroke", style.stroke)
    attribute(null, "stroke-width", null, style.strokeWidth.formatSvgNumber())
    // Optional feature attribute: `butt` is the SVG default, so it's omitted (like k:flags).
    if (style.strokeCap != ShapeStrokeCap.BUTT) strokeCapAttr("stroke-linecap", style.strokeCap)
    if (segment.isImaginary) kAttr(KATTR_FLAGS, KFLAG_IMAGINARY)
    kTagsAttr(shape.tags)
    endTag(SVG_NS, "line", "")
}
