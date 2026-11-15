package fromcrackpot.kintsugi.cga.d2.operations.queries.predicates

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Circle
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.RoundPoint
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.operations.products.dot
import fromcrackpot.kintsugi.cga.round

/*
CIRCLE
 */

/**
 * `Circle contains FlatPoint => Boolean`<br>
 * Returns whether the point lies in the circle's interior or on the circle.
 *
 * - An inside-out circle's interior is the unbounded region outside it.
 * - Spelled `point in circle`. `point !in circle` is the strict outside.
 * - The test is quadratic, so it may misjudge near a large circle's boundary.
 *
 * @receiver The circle tested against.
 * @param point The point to test.
 * @return `true` when the probe rounds to non-negative at the `SCALE` quantum.
 */
public infix operator fun Circle.contains(point: FlatPoint): Boolean =
    round(point dot this) >= 0.0

/**
 * `Circle contains RoundPoint => Boolean`<br>
 * Returns whether the point lies in the circle's interior or on the circle.
 *
 * - An inside-out circle's interior is the unbounded region outside it.
 * - Spelled `point in circle`. `point !in circle` is the strict outside.
 * - A sized point, like a center, adds its own `aw`. Test its position through `toFlat`.
 *
 * @receiver The circle tested against.
 * @param point The point to test.
 * @return `true` when the probe rounds to non-negative at the `SCALE` quantum.
 */
public infix operator fun Circle.contains(point: RoundPoint): Boolean =
    round(point dot this) >= 0.0

/**
 * `Circle contains Circle => Boolean`<br>
 * Returns whether [other]'s region lies within this circle's region. Boundaries are included, so
 * internal tangency and identical circles count.
 *
 * - A classic circle bounds its disk, an inside-out one the region outside it.
 * - A classic circle never contains an inside-out one.
 * - Spelled `inner in outer`.
 *
 * @receiver The circle whose region must contain the other.
 * @param other The circle whose region must fit.
 * @return `true` when the region fits, rounded at the `SCALE` quantum.
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
 * - Uses the nonzero winding rule, so a pentagram's center is within.
 * - An inside-out polygon contains the complement of its loop, boundary included.
 * - Spelled `point in polygon`. `point !in polygon` is the strict outside.
 *
 * @receiver The polygon tested against.
 * @param point The point to test.
 * @return `true` when the point is on the boundary or within the solid region.
 */
public infix operator fun Polygon.contains(point: FlatPoint): Boolean {
    if (point on this) return true
    // Sunday's crossing-based winding number.
    // Asymmetric bounds (up: ay <= py < by, down: by <= py < ay) count a vertex crossing once
    // and skip horizontal edges. Side test: the raw signed distance to the edge carrier.
    // Raw throughout: rounding would double- or zero-count crossings near vertices.
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
 * Returns whether the point's position lies within the polygon's solid region, boundary
 * included.
 *
 * - An inside-out polygon contains the complement of its loop, boundary included.
 * - Spelled `point in polygon`. `point !in polygon` is the strict outside.
 *
 * @receiver The polygon tested against.
 * @param point The point to test.
 * @return `true` when the point is on the boundary or within the solid region.
 */
public infix operator fun Polygon.contains(point: RoundPoint): Boolean =
    contains(point.toFlat)
