package fromcrackpot.kintsugi.cga.d2.queries.predicates

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Circle
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.RoundPoint
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.products.dot
import fromcrackpot.kintsugi.cga.round

/*
CIRCLE
 */

/**
 * `Circle contains FlatPoint => Boolean`<br>
 * Returns whether the point lies in the circle's interior or on the circle.
 *
 * True when `point dot this` — positive inside a classic circle, negative outside — rounds
 * to non-negative at the `SCALE` quantum, so the boundary counts as contained.
 * Orientation-aware: an inside-out circle negates the probe, so its interior is the
 * unbounded region outside it, boundary included. Against an imaginary classic circle the
 * raw probe is negative everywhere. The probe is quadratic — the same precision caveat as
 * `FlatPoint on Circle`. An `operator fun`: `point in circle` spells containment,
 * `point !in circle` the strict outside — neither inside nor on the circle.
 *
 * @receiver The circle tested against.
 * @param point The point to test.
 * @return `true` when the rounded probe is non-negative.
 */
public infix operator fun Circle.contains(point: FlatPoint): Boolean =
    round(point dot this) >= 0.0

/**
 * `Circle contains RoundPoint => Boolean`<br>
 * Returns whether the point lies in the circle's interior or on the circle.
 *
 * True when `point dot this` rounds to non-negative at the `SCALE` quantum — the same
 * orientation-aware quadratic gate as `Circle contains FlatPoint`, whose residual grows with
 * the radius. The pairing reads the point's stored round bulk [RoundPoint.aw], so the
 * containment reading holds for null-lifted points; a semantic `aw` (a center, an attitude)
 * feeds that data into the probe instead. An `operator fun`: `point in circle` spells
 * containment, `point !in circle` the strict outside.
 *
 * @receiver The circle tested against.
 * @param point The point to test.
 * @return `true` when the rounded probe is non-negative.
 */
public infix operator fun Circle.contains(point: RoundPoint): Boolean =
    round(point dot this) >= 0.0

/**
 * `Circle contains Circle => Boolean`<br>
 * Returns whether the other circle's region lies within the region this circle bounds,
 * boundaries included — internal tangency and identical circles count.
 *
 * Orientation-aware on both sides: a classic circle bounds its disk, an inside-out circle
 * the unbounded region outside itself. For real circles with radii `r₁` (this) and `r₂`
 * ([other]) and centers `d` apart:
 *
 * | This       | Other      | Contains when |
 * |------------|------------|---------------|
 * | classic    | classic    | `d + r₂ ≤ r₁` — nested, the other no bigger than this one |
 * | inside-out | inside-out | `d + r₁ ≤ r₂` — nested, the other no smaller than this one |
 * | inside-out | classic    | `d ≥ r₁ + r₂` — the other's disk clears this one's excluded region |
 * | classic    | inside-out | never — an unbounded region cannot fit in a bounded one |
 *
 * The decision rounds at the `SCALE` quantum: one metric gate — `this dot other` plus the
 * product of the radii must round to non-positive — and, for same-orientation operands, a
 * rounded radius comparison picking the valid nesting. Imaginary operands are not rejected:
 * the same gates run on the raw magnitudes of their radii, with no region behind the answer.
 * An `operator fun`: `inner in outer` spells the containment, `inner !in outer` its failure —
 * the other circle's region reaching outside this one's.
 *
 * @receiver The circle whose region must contain the other.
 * @param other The circle whose region must fit.
 * @return `true` when the rounded gates pass.
 */
public infix operator fun Circle.contains(other: Circle): Boolean {
    if (other.cw > 0 && this.cw < 0) return false

    // delta + r₁·r₂ = ½(d² − (r₁−r₂)²) for same orientation (nesting gate),
    // ½((r₁+r₂)² − d²) for classic-in-inside-out (clearance gate).
    val delta = this dot other
    if (round(delta + this.radius * other.radius) > 0.0) return false

    if (this.cw * other.cw > 0) {
        return if (this.cw < 0) {
            round(other.radius - this.radius) <= 0.0
        } else {
            round(other.radius - this.radius) >= 0.0
        }
    }

    return true
}

/*
POLYGON
 */

/**
 * `Polygon contains FlatPoint => Boolean`<br>
 * Returns whether the point lies within the polygon's solid region, boundary included.
 *
 * Containment by the nonzero winding rule: the point sits within the loop when the polygon's
 * directed edges wind around it a net nonzero number of times, so self-touching and
 * self-crossing loops classify by winding — a pentagram's center is within.
 * Orientation-aware: a classic polygon contains the points its loop winds around; an
 * inside-out polygon ([Polygon.isInsideOut]) contains the complement — the points its loop
 * does *not* wind around — so `point !in -polygon` is the strict interior of `polygon`. The
 * boundary is probed first with `FlatPoint on Polygon`, rounded at the `SCALE` quantum, owns
 * the whole tolerance, and counts as contained in both orientations; off the boundary the
 * crossing walk runs raw. An `operator fun`: `point in polygon` spells containment,
 * `point !in polygon` the strict outside — neither within the solid region nor on the
 * boundary.
 *
 * @receiver The polygon tested against.
 * @param point The point to test.
 * @return `true` when the point is on the boundary, or the winding number matches the
 *   polygon's orientation — nonzero for classic, zero for inside-out.
 */
public infix operator fun Polygon.contains(point: FlatPoint): Boolean {
    if (point on this) return true
    // Sunday's crossing-based winding number. Asymmetric bounds (ay <= py < by up,
    // by <= py < ay down) count a vertex-level crossing exactly once and skip horizontal
    // edges for free; the side test is the raw signed distance against the edge carrier.
    // Raw throughout — rounding would double/zero-count crossings near vertices.
    var wn = 0
    for (edge in edges) {
        val ay = edge.start.ay
        val by = edge.end.ay
        if (ay <= point.py) {
            if (by > point.py && point dot edge.carrier > 0.0) wn++ // upward crossing, p left
        } else {
            if (by <= point.py && point dot edge.carrier < 0.0) wn-- // downward crossing, p right
        }
    }
    return if (isInsideOut) wn == 0 else wn != 0
}

/**
 * `Polygon contains RoundPoint => Boolean`<br>
 * Returns whether the point lies within the polygon's solid region, boundary included.
 *
 * `Polygon contains FlatPoint` on the point's flat projection [RoundPoint.toFlat]; neither
 * the boundary probes nor the winding walk read the round bulk, so the flat and round forms
 * agree exactly — orientation-awareness included. An `operator fun`: `point in polygon`
 * spells containment, `point !in polygon` the strict outside.
 *
 * @receiver The polygon tested against.
 * @param point The point to test.
 * @return `true` when the point is on the boundary, or the winding number matches the
 *   polygon's orientation — nonzero for classic, zero for inside-out.
 */
public infix operator fun Polygon.contains(point: RoundPoint): Boolean =
    contains(point.toFlat)
