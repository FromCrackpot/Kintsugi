package fromcrackpot.kintsugi.svg.serializing.primitives

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Dipole
import fromcrackpot.kintsugi.scenegraph.SceneBounds
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.DipoleNode
import fromcrackpot.kintsugi.scenegraph.style.DipoleChrome
import fromcrackpot.kintsugi.svg.KATTR_ATTITUDE
import fromcrackpot.kintsugi.svg.KATTR_TYPE
import fromcrackpot.kintsugi.svg.KTYPE_DIPOLE
import fromcrackpot.kintsugi.svg.SVG_NS
import fromcrackpot.kintsugi.svg.cgaToSvg
import fromcrackpot.kintsugi.svg.cgaToSvgDirection
import fromcrackpot.kintsugi.svg.decodeDipole
import fromcrackpot.kintsugi.svg.decodeRoundPoint
import fromcrackpot.kintsugi.svg.kAttr
import fromcrackpot.kintsugi.svg.kTagsAttr
import fromcrackpot.kintsugi.svg.serializing.colorAttr
import fromcrackpot.kintsugi.svg.serializing.dashAttrs
import fromcrackpot.kintsugi.svg.serializing.formatSvgNumber
import fromcrackpot.kintsugi.svg.warn
import nl.adaptivity.xmlutil.XmlWriter

/**
 * Dipole → `<g k:type="dipole">` with two `<circle k:type="round-point">` children carrying
 * `dipole.start` / `dipole.end`, in that order (the endpoint form).
 *
 * - An inline dashed `<line>` from start to end is visual chrome. The parser ignores it; it is there
 *   so any SVG viewer renders the dipole's segment.
 * - Chrome-line attributes track `style.fill` and `style.size`, NOT `style.stroke` /
 *   `style.strokeWidth`. Round points only carry a fill, and the `ShapeStyle.DIPOLE` default is
 *   fill-only, so stroke would resolve to `NONE`/`0` and hide the line. Width comes from
 *   `:scene-graph`'s `DipoleChrome`, which `:compose` also consumes, so both renderers match.
 * - Center form: endpoints that would not reload ([decodeDipole]), a zero-radius dipole such as a
 *   tangent meet. The `<g>` carries `k:attitude` (SVG frame) and one child, `dipole.center`, whose
 *   `k:aw` holds the radius². No chrome line: the connector has zero length.
 * - Dash: an authored one rides the carrier `<g>` (`none` included); the line and the rings carry
 *   none and inherit it in viewers. Unset, the line carries `style.dashFor` (chrome or imaginary)
 *   and an imaginary dipole's rings carry it in `k:chrome`.
 * - An imaginary dipole's children are rings ([writeRoundPointMarker]).
 * - Skip+warn: a dipole with an at-infinity endpoint (no `<circle>` encoding, like round points at
 *   infinity).
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
    val centerForm = decodeDipole(
        decodeRoundPoint(sx1, sy1, dipole.start.aw, viewBox),
        decodeRoundPoint(sx2, sy2, dipole.end.aw, viewBox),
    ) == null
    val authored = style.dash
    val derived = if (authored == null) style.dashFor(dipole) else null
    val ring = dipole.isImaginary
    val childDash = if (ring) derived else null

    startTag(SVG_NS, "g", "")
    kAttr(KATTR_TYPE, KTYPE_DIPOLE)
    if (centerForm) {
        val (ax, ay) = cgaToSvgDirection(dipole.attitude.ax, dipole.attitude.ay)
        kAttr(KATTR_ATTITUDE, "${ax.formatSvgNumber()} ${ay.formatSvgNumber()}")
    }
    authored?.let { dashAttrs(it, derived = false, mutableListOf()) }
    // Tags ride the carrier <g> (the dipole's identity); the synthesized round-point children
    // carry none, mirroring the parser (child k:tags is ignored on read).
    kTagsAttr(shape.tags)

    if (centerForm) {
        writeRoundPointMarker(dipole.center, style, viewBox, ring, childDash, dashDerived = true)
        endTag(SVG_NS, "g", "")
        return
    }
    writeRoundPointMarker(dipole.start, style, viewBox, ring, childDash, dashDerived = true)
    writeRoundPointMarker(dipole.end, style, viewBox, ring, childDash, dashDerived = true)

    // Dashed visual chrome: the parser ignores this <line> entirely.
    startTag(SVG_NS, "line", "")
    attribute(null, "x1", null, sx1.formatSvgNumber())
    attribute(null, "y1", null, sy1.formatSvgNumber())
    attribute(null, "x2", null, sx2.formatSvgNumber())
    attribute(null, "y2", null, sy2.formatSvgNumber())
    colorAttr("stroke", style.fill)
    attribute(null, "stroke-width", null, (style.size * DipoleChrome.WIDTH_RATIO).formatSvgNumber())
    derived?.let { dashAttrs(it, derived = false, mutableListOf()) }
    endTag(SVG_NS, "line", "")

    endTag(SVG_NS, "g", "")
}
