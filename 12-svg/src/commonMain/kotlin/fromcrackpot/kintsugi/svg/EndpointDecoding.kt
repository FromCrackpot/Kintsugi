package fromcrackpot.kintsugi.svg

import fromcrackpot.kintsugi.cga.d2.exceptions.DegenerateDipoleException
import fromcrackpot.kintsugi.cga.d2.operations.products.join
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Segment
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Dipole
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.RoundPoint
import fromcrackpot.kintsugi.scenegraph.SceneBounds

/*
 * What two encoded endpoints decode to. Parsers and writers share it, so the two directions can't
 * disagree on which endpoints coincide.
 * - Parser: a `null` is a malformed document. Writer: a `null` is a shape that would not reload.
 *   The segment writer skips it with a warning. The dipole writer switches to the center form.
 * Coincident = the wedge throws, or lands at infinity (closer than the quantum, off a line through the origin).
 */

/** The round point at SVG `(cx, cy)`: `k:aw` fed straight in, or the null lift when absent. */
internal fun decodeRoundPoint(cx: Double, cy: Double, aw: Double?, viewBox: SceneBounds): RoundPoint {
    val (x, y) = svgToCga(cx, cy, viewBox)
    return if (aw != null) RoundPoint(x, y, az = 1.0, aw = aw) else RoundPoint(x, y)
}

/** The segment between SVG `(x1, y1)` and `(x2, y2)`, or `null` when the endpoints coincide at the `SCALE` quantum. */
internal fun decodeSegment(x1: Double, y1: Double, x2: Double, y2: Double, viewBox: SceneBounds): Segment? {
    val (cx1, cy1) = svgToCga(x1, y1, viewBox)
    val (cx2, cy2) = svgToCga(x2, y2, viewBox)
    val segment = try {
        Segment(FlatPoint(cx1, cy1), FlatPoint(cx2, cy2))
    } catch (_: DegenerateDipoleException) {
        return null
    }
    return segment.takeUnless { it.toDipole.isAtInfinity }
}

/** The dipole `start ∧ end`, or `null` when the two round points coincide at the `SCALE` quantum. */
internal fun decodeDipole(start: RoundPoint, end: RoundPoint): Dipole? {
    val dipole = try {
        start join end
    } catch (_: DegenerateDipoleException) {
        return null
    }
    return dipole.takeUnless { it.isAtInfinity }
}
