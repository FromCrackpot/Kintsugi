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
import fromcrackpot.kintsugi.svg.kAttr
import fromcrackpot.kintsugi.svg.kTagsAttr
import fromcrackpot.kintsugi.svg.serializing.colorAttr
import fromcrackpot.kintsugi.svg.serializing.formatSvgNumber
import fromcrackpot.kintsugi.svg.warn
import nl.adaptivity.xmlutil.XmlWriter

/**
 * Dipole → `<g k:type="dipole">` with two `<circle k:type="round-point">` children carrying
 * `dipole.start` / `dipole.end` (in that order), plus an inline dashed `<line>` from start to end
 * as visual chrome. The parser ignores the `<line>` entirely; it's there so any SVG viewer renders
 * the dipole's segment.
 *
 * Chrome-line attributes track `style.fill` and `style.size`, NOT `style.stroke` /
 * `style.strokeWidth` — round-points only ever carry a fill (per round-point encoding), and the
 * `ShapeStyle.DIPOLE` default is fill-only, so the dipole's fill is its single "color." Stroke would
 * always resolve to `NONE`/`0` and the chrome line would be invisible in SVG viewers. Width and
 * dash values come from `:scene-graph`'s `DipoleChrome` — the single source of truth `:compose` also
 * consumes, so the same dipole reads the same way in both renderers.
 *
 * A dipole with an at-infinity endpoint cannot be encoded as a `<circle>` — skip+warn, matching
 * the round-point encoding posture for at-infinity round points.
 */
internal fun XmlWriter.writeDipoleGroup(shape: DipoleNode, viewBox: SceneBounds) {
    val dipole: Dipole = shape
    val style = shape.style

    if (dipole.start.isAtInfinity || dipole.end.isAtInfinity) {
        warn("skipping Dipole with endpoint at infinity (cannot encode as <g>): $dipole")
        return
    }

    startTag(SVG_NS, "g", "")
    kAttr(KATTR_TYPE, KTYPE_DIPOLE)
    // Tags ride the carrier <g> (the dipole's identity); the synthesized round-point children
    // carry none, mirroring the parser (child k:tags is ignored on read).
    kTagsAttr(shape.tags)

    writeRoundPointCircle(RoundPointNode(dipole.start, style), viewBox)
    writeRoundPointCircle(RoundPointNode(dipole.end, style), viewBox)

    // Dashed visual chrome — parser ignores this <line> entirely.
    val (sx1, sy1) = cgaToSvg(dipole.start.ax, dipole.start.ay, viewBox)
    val (sx2, sy2) = cgaToSvg(dipole.end.ax, dipole.end.ay, viewBox)
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
