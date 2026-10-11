package fromcrackpot.kintsugi.cga.d2.queries.predicates

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Circle
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Line
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.RoundPoint
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Segment
import fromcrackpot.kintsugi.cga.d2.products.dot
import fromcrackpot.kintsugi.cga.round
import kotlin.math.abs

/*
FLAT POINT
 */

/**
 * `FlatPoint on Line => Boolean`<br>
 * Returns whether the point lies on the line.
 *
 * True when `this dot line` — the signed Euclidean distance for finite operands — rounds to
 * zero at the `SCALE` quantum. The gate is all-linear, robust at the quantum. A direction
 * (at infinity) counts as on the line when it runs along it.
 *
 * @receiver The point to test.
 * @param line The line tested against.
 * @return `true` when the rounded signed distance is zero.
 */
public infix fun FlatPoint.on(line: Line): Boolean =
    round(this dot line) == 0.0

/**
 * `FlatPoint on Circle => Boolean`<br>
 * Returns whether the point lies on the circle.
 *
 * True when `this dot circle` — the raw probe `½·(r² − d²)` against a classic circle — rounds
 * to zero at the `SCALE` quantum. Orientation-blind: an inside-out circle negates the probe,
 * which leaves the zero test unchanged. Against an imaginary circle the raw probe is negative
 * everywhere. The probe is quadratic, reached through the conformal lift, so its residual
 * grows with the radius — a point placed within the quantum of a large circle's boundary may
 * still fail the test; callers wanting a "near" test need their own tolerance.
 *
 * @receiver The point to test.
 * @param circle The circle tested against.
 * @return `true` when the rounded probe is zero.
 */
public infix fun FlatPoint.on(circle: Circle): Boolean =
    round(this dot circle) == 0.0

/**
 * `FlatPoint on Segment => Boolean`<br>
 * Returns whether the point lies on the segment itself, endpoints included.
 *
 * Two gates, both all-linear and rounded at the `SCALE` quantum: the point must lie on the
 * segment's [Segment.carrier], and its rounded distance to the [Segment.cocarrier] — the
 * perpendicular bisector of the endpoints — must not exceed the rounded half-length
 * [Segment.radius].
 *
 * @receiver The point to test.
 * @param segment The segment tested against.
 * @return `true` when both rounded gates pass.
 */
public infix fun FlatPoint.on(segment: Segment): Boolean =
    (this on segment.carrier) && round(abs(this dot segment.cocarrier)) <= round(segment.radius)

/**
 * `FlatPoint on Polygon => Boolean`<br>
 * Returns whether the point lies on the polygon's boundary.
 *
 * A disjunction of `FlatPoint on Segment` probes over the polygon's [Polygon.edges], each
 * all-linear and rounded at the `SCALE` quantum.
 *
 * @receiver The point to test.
 * @param polygon The polygon whose boundary is tested against.
 * @return `true` when the point is on at least one edge.
 */
public infix fun FlatPoint.on(polygon: Polygon): Boolean =
    polygon.edges.any { this on it }

/*
ROUND POINT
 */

/**
 * `RoundPoint on Line => Boolean`<br>
 * Returns whether the point lies on the line.
 *
 * True when `this dot line` rounds to zero at the `SCALE` quantum — the same all-linear gate
 * as `FlatPoint on Line`; the dot never reads the round bulk [RoundPoint.aw], so the flat and
 * round forms agree exactly.
 *
 * @receiver The point to test.
 * @param line The line tested against.
 * @return `true` when the rounded signed distance is zero.
 */
public infix fun RoundPoint.on(line: Line): Boolean =
    round(this dot line) == 0.0

/**
 * `RoundPoint on Circle => Boolean`<br>
 * Returns whether the point lies on the circle.
 *
 * True when `this dot circle` rounds to zero at the `SCALE` quantum — the same
 * orientation-blind quadratic gate as `FlatPoint on Circle`, whose residual grows with the
 * radius. The pairing reads the stored round bulk [RoundPoint.aw], so the boundary reading
 * holds for null-lifted points; a semantic `aw` (a center, an attitude) feeds that data into
 * the probe instead.
 *
 * @receiver The point to test.
 * @param circle The circle tested against.
 * @return `true` when the rounded probe is zero.
 */
public infix fun RoundPoint.on(circle: Circle): Boolean =
    round(this dot circle) == 0.0

/**
 * `RoundPoint on Segment => Boolean`<br>
 * Returns whether the point lies on the segment itself, endpoints included.
 *
 * The same two all-linear gates as `FlatPoint on Segment`, rounded at the `SCALE` quantum;
 * neither dot reads the round bulk [RoundPoint.aw], so the flat and round forms agree exactly.
 *
 * @receiver The point to test.
 * @param segment The segment tested against.
 * @return `true` when both rounded gates pass.
 */
public infix fun RoundPoint.on(segment: Segment): Boolean =
    (this on segment.carrier) && round(abs(this dot segment.cocarrier)) <= round(segment.radius)

/**
 * `RoundPoint on Polygon => Boolean`<br>
 * Returns whether the point lies on the polygon's boundary.
 *
 * A disjunction of `RoundPoint on Segment` probes over the polygon's [Polygon.edges], each
 * all-linear and rounded at the `SCALE` quantum.
 *
 * @receiver The point to test.
 * @param polygon The polygon whose boundary is tested against.
 * @return `true` when the point is on at least one edge.
 */
public infix fun RoundPoint.on(polygon: Polygon): Boolean =
    polygon.edges.any { this on it }
