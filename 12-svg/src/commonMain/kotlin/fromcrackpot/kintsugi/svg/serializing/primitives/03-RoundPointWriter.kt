package fromcrackpot.kintsugi.svg.serializing.primitives

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.RoundPoint
import fromcrackpot.kintsugi.scenegraph.SceneBounds
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.RoundPointNode
import fromcrackpot.kintsugi.svg.*
import fromcrackpot.kintsugi.svg.serializing.colorAttr
import fromcrackpot.kintsugi.svg.serializing.formatSvgNumber
import nl.adaptivity.xmlutil.XmlWriter

/**
 * Round point → `<circle k:type="round-point">`. The element `r` is the render marker size
 * (`size/2`), NOT the algebraic radius. `k:aw` is **always emitted** — a core attribute under the
 * module's two-tier emission rule (core always emitted, optional-feature only when active); the parser tolerates
 * its absence on hand-authored input. A RoundPoint at infinity cannot be encoded — skip+warn.
 */
internal fun XmlWriter.writeRoundPointCircle(shape: RoundPointNode, viewBox: SceneBounds) {
    val p: RoundPoint = shape
    val style = shape.style

    if (p.isAtInfinity) {
        warn("skipping RoundPoint at infinity (cannot encode as <circle>): $p")
        return
    }

    val (svgCx, svgCy) = cgaToSvg(p.ax, p.ay, viewBox)

    startTag(SVG_NS, "circle", "")
    kAttr(KATTR_TYPE, KTYPE_ROUND_POINT)
    attribute(null, "cx", null, svgCx.formatSvgNumber())
    attribute(null, "cy", null, svgCy.formatSvgNumber())
    attribute(null, "r", null, (style.size / 2.0).formatSvgNumber())
    kAttr(KATTR_AW, p.aw.formatSvgNumber())
    colorAttr("fill", style.fill)
    kTagsAttr(shape.tags)
    endTag(SVG_NS, "circle", "")
}
