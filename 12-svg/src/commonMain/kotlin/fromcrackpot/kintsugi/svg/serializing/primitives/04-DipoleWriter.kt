package fromcrackpot.kintsugi.svg.serializing.primitives

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Dipole
import fromcrackpot.kintsugi.scenegraph.SceneBounds
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.DipoleNode
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.RoundPointNode
import fromcrackpot.kintsugi.scenegraph.style.DipoleChrome
import fromcrackpot.kintsugi.svg.KATTR_TYPE
import fromcrackpot.kintsugi.svg.KTYPE_DIPOLE
import fromcrackpot.kintsugi.svg.SVG_NS
import fromcrackpot.kintsugi.svg.cgaToSvg
import fromcrackpot.kintsugi.svg.decodeDipole
import fromcrackpot.kintsugi.svg.decodeRoundPoint
import fromcrackpot.kintsugi.svg.kAttr
import fromcrackpot.kintsugi.svg.kTagsAttr
import fromcrackpot.kintsugi.svg.serializing.colorAttr
import fromcrackpot.kintsugi.svg.serializing.formatSvgNumber
import fromcrackpot.kintsugi.svg.warn
import nl.adaptivity.xmlutil.XmlWriter

/**
 * Dipole → `<g k:type="dipole">` with two `<circle k:type="round-point">` children carrying
 * `dipole.start` / `dipole.end`, in that order.
 *
 * - An inline dashed `<line>` from start to end is visual chrome. The parser ignores it; it is there
 *   so any SVG viewer renders the dipole's segment.
 * - Chrome-line attributes track `style.fill` and `style.size`, NOT `style.stroke` /
 *   `style.strokeWidth`. Round points only carry a fill, and the `ShapeStyle.DIPOLE` default is
 *   fill-only, so stroke would resolve to `NONE`/`0` and hide the line. Width and dash values come
 *   from `:scene-graph`'s `DipoleChrome`, which `:compose` also consumes, so both renderers match.
 * - Skip+warn: a dipole with an at-infinity endpoint (no `<circle>` encoding, like round points at
 *   infinity), and a zero-radius dipole (a tangent meet), whose coincident children wouldn't reload
 *   ([decodeDipole]).
 */
internal fun XmlWriter.writeDipoleGroup(shape: DipoleNode, viewBox: SceneBounds) {
    val dipole: Dipole = shape
    val style = shape.style

    if (dipole.start.isAtInfinity || dipole.end.isAtInfinity) {
        warn("skipping Dipole with endpoint at infinity (cannot encode as <g>): $dipole")
        return
    }
    val (sx1, sy1) = cgaToSvg(dipole.start.ax, dipole.start.ay, viewBox)
    val (sx2, sy2) = cgaToSvg(dipole.end.ax, dipole.end.ay, viewBox)
    val reloaded = decodeDipole(
        decodeRoundPoint(sx1, sy1, dipole.start.aw, viewBox),
        decodeRoundPoint(sx2, sy2, dipole.end.aw, viewBox),
    )
    if (reloaded == null) {
        warn("skipping zero-radius Dipole (coincident endpoints would not reload): $dipole")
        return
    }

    startTag(SVG_NS, "g", "")
    kAttr(KATTR_TYPE, KTYPE_DIPOLE)
    // Tags ride the carrier <g> (the dipole's identity); the synthesized round-point children
    // carry none, mirroring the parser (child k:tags is ignored on read).
    kTagsAttr(shape.tags)

    writeRoundPointCircle(RoundPointNode(dipole.start, style), viewBox)
    writeRoundPointCircle(RoundPointNode(dipole.end, style), viewBox)

    // Dashed visual chrome: the parser ignores this <line> entirely.
    startTag(SVG_NS, "line", "")
    attribute(null, "x1", null, sx1.formatSvgNumber())
    attribute(null, "y1", null, sy1.formatSvgNumber())
    attribute(null, "x2", null, sx2.formatSvgNumber())
    attribute(null, "y2", null, sy2.formatSvgNumber())
    colorAttr("stroke", style.fill)
    attribute(null, "stroke-width", null, (style.size * DipoleChrome.WIDTH_RATIO).formatSvgNumber())
    attribute(null, "stroke-dasharray", null, "${DipoleChrome.DASH_ON.formatSvgNumber()} ${DipoleChrome.DASH_OFF.formatSvgNumber()}")
    endTag(SVG_NS, "line", "")

    endTag(SVG_NS, "g", "")
}
