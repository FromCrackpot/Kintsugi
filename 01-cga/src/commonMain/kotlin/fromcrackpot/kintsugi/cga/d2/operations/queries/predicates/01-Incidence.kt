package fromcrackpot.kintsugi.cga.d2.operations.queries.predicates

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Circle
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Line
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.RoundPoint
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Segment
import fromcrackpot.kintsugi.cga.d2.operations.products.dot
import fromcrackpot.kintsugi.cga.round
import kotlin.math.abs

/*
FLAT POINT
 */

/**
 * `FlatPoint on Line => Boolean`<br>
 * Returns whether the point lies on the line, rounded at the `SCALE` quantum.
 *
 * - A direction (at infinity) is on the line when it runs along it.
 *
 * @receiver The point to test.
 * @param line The line tested against.
 * @return `true` when the signed distance rounds to zero.
 */
public infix fun FlatPoint.on(line: Line): Boolean =
    round(this dot line) == 0.0

/**
 * `FlatPoint on Circle => Boolean`<br>
 * Returns whether the point lies on the circle, rounded at the `SCALE` quantum. Orientation is ignored.
 *
 * - The test is quadratic, so it may fail near a large circle's boundary. Use your own tolerance for "near".
 *
 * @receiver The point to test.
 * @param circle The circle tested against.
 * @return `true` when the probe rounds to zero.
 */
public infix fun FlatPoint.on(circle: Circle): Boolean =
    round(this dot circle) == 0.0

/**
 * `FlatPoint on Segment => Boolean`<br>
 * Returns whether the point lies on the segment itself, endpoints included, rounded at the
 * `SCALE` quantum.
 *
 * @receiver The point to test.
 * @param segment The segment tested against.
 * @return `true` when the point is on the segment.
 */
public infix fun FlatPoint.on(segment: Segment): Boolean =
    (this on segment.carrier) && round(abs(this dot segment.cocarrier)) <= round(segment.radius)

/**
 * `FlatPoint on Polygon => Boolean`<br>
 * Returns whether the point lies on the polygon's boundary, rounded at the `SCALE` quantum.
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
 * Returns whether the point lies on the line, rounded at the `SCALE` quantum.
 *
 * @receiver The point to test.
 * @param line The line tested against.
 * @return `true` when the signed distance rounds to zero.
 */
public infix fun RoundPoint.on(line: Line): Boolean =
    round(this dot line) == 0.0

/**
 * `RoundPoint on Circle => Boolean`<br>
 * Returns whether the point lies on the circle, rounded at the `SCALE` quantum. Orientation is ignored.
 *
 * - The test is quadratic, so it may fail near a large circle's boundary. Use your own tolerance for "near".
 * - A sized point, like a center, adds its own `aw`. Test its position through `toFlat`.
 *
 * @receiver The point to test.
 * @param circle The circle tested against.
 * @return `true` when the probe rounds to zero.
 */
public infix fun RoundPoint.on(circle: Circle): Boolean =
    round(this dot circle) == 0.0

/**
 * `RoundPoint on Segment => Boolean`<br>
 * Returns whether the point lies on the segment itself, endpoints included, rounded at the
 * `SCALE` quantum.
 *
 * @receiver The point to test.
 * @param segment The segment tested against.
 * @return `true` when the point is on the segment.
 */
public infix fun RoundPoint.on(segment: Segment): Boolean =
    (this on segment.carrier) && round(abs(this dot segment.cocarrier)) <= round(segment.radius)

/**
 * `RoundPoint on Polygon => Boolean`<br>
 * Returns whether the point lies on the polygon's boundary, rounded at the `SCALE` quantum.
 *
 * @receiver The point to test.
 * @param polygon The polygon whose boundary is tested against.
 * @return `true` when the point is on at least one edge.
 */
public infix fun RoundPoint.on(polygon: Polygon): Boolean =
    polygon.edges.any { this on it }
