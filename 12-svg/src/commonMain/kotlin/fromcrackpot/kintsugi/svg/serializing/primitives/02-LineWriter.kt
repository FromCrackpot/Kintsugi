package fromcrackpot.kintsugi.svg.serializing.primitives

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Circle
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Line
import fromcrackpot.kintsugi.cga.d2.products.meet
import fromcrackpot.kintsugi.scenegraph.SceneBounds
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.LineNode
import fromcrackpot.kintsugi.scenegraph.style.ShapeStrokeCap
import fromcrackpot.kintsugi.svg.KATTR_TYPE
import fromcrackpot.kintsugi.svg.KTYPE_LINE
import fromcrackpot.kintsugi.svg.SVG_NS
import fromcrackpot.kintsugi.svg.cgaToSvg
import fromcrackpot.kintsugi.svg.kAttr
import fromcrackpot.kintsugi.svg.kTagsAttr
import fromcrackpot.kintsugi.svg.serializing.colorAttr
import fromcrackpot.kintsugi.svg.serializing.formatSvgNumber
import fromcrackpot.kintsugi.svg.serializing.strokeCapAttr
import fromcrackpot.kintsugi.svg.warn
import nl.adaptivity.xmlutil.XmlWriter

/**
 * Unbounded line → `<line k:type="line">`. Endpoints are the worldBorder-clipped projection of the
 * algebraic line (`line meet worldBorder`); `k:type="line"` marks it as an infinite CGA Line so the
 * parser need not infer that from endpoint geometry. Degenerate/imaginary intersections skip+warn.
 */
internal fun XmlWriter.writeLineElement(shape: LineNode, viewBox: SceneBounds, worldBorder: Circle) {
    val line: Line = shape
    val style = shape.style

    if (line.isAtInfinity) {
        warn("skipping Line at infinity (no finite endpoints to render): $line")
        return
    }

    // `worldBorder ∨ line` (not `line ∨ worldBorder`) so the dipole's start, end land on the
    // line's a-side, b-side in that order — making the SVG export `(x1,y1) = start, (x2,y2) = end`
    // reparse to the same oriented Line blade. Same-grade antiwedge is antisymmetric, so swapping
    // the operands swaps start/end.
    val dipole = worldBorder meet line
    if (dipole.isImaginary) {
        warn("skipping Line outside viewBox (imaginary intersection with world border): $line")
        return
    }
    val (sx1, sy1) = cgaToSvg(dipole.start.ax, dipole.start.ay, viewBox)
    val (sx2, sy2) = cgaToSvg(dipole.end.ax, dipole.end.ay, viewBox)

    startTag(SVG_NS, "line", "")
    kAttr(KATTR_TYPE, KTYPE_LINE)
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
