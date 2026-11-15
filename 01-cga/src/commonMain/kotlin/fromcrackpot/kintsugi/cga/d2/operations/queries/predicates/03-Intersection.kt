package fromcrackpot.kintsugi.cga.d2.operations.queries.predicates

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Line
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Segment
import fromcrackpot.kintsugi.cga.d2.operations.products.dot
import fromcrackpot.kintsugi.cga.round

/*
LINE
 */

/**
 * `Line intersects Segment => Boolean`<br>
 * Returns whether the line touches or crosses the segment, rounded at the `SCALE` quantum.
 *
 * @receiver The line to test.
 * @param segment The segment tested against.
 * @return `true` when they share a point.
 */
public infix fun Line.intersects(segment: Segment): Boolean = segment intersects this

/**
 * `Line intersects Polygon => Boolean`<br>
 * Returns whether the line reaches the polygon's solid region, rounded at the `SCALE` quantum.
 *
 * - Always `true` for an inside-out polygon, whose solid region is unbounded.
 *
 * @receiver The line to test.
 * @param polygon The polygon tested against.
 * @return `true` when they share a point.
 */
public infix fun Line.intersects(polygon: Polygon): Boolean = polygon intersects this

/*
SEGMENT
 */

/**
 * `Segment intersects Line => Boolean`<br>
 * Returns whether the line touches or crosses the segment, rounded at the `SCALE` quantum.
 *
 * - A collinear segment intersects along its whole length.
 *
 * @receiver The segment to test.
 * @param line The line tested against.
 * @return `true` when they share a point.
 */
public infix fun Segment.intersects(line: Line): Boolean =
    round(start dot line) * round(end dot line) <= 0.0

/**
 * `Segment intersects Segment => Boolean`<br>
 * Returns whether the segments touch, cross, or overlap, endpoints included, rounded at the
 * `SCALE` quantum.
 *
 * @receiver The first segment.
 * @param other The second segment.
 * @return `true` when the segments share a point.
 */
public infix fun Segment.intersects(other: Segment): Boolean =
    // Two branches cover each other's blind spots. Don't collapse them:
    //   X crossing (interior×interior): the `on` probes miss it, mutual straddle catches it.
    //   Collinear overlap: the `on` probes catch it; straddle false-positives on disjoint
    //   collinear segments (all four dots round to 0).
    if (other.start on carrier && other.end on carrier)
        // Interval overlap always contains an endpoint of one segment; the third probe
        // catches `this` nested strictly inside `other`.
        other.start on this || other.end on this || start on other
    else
        // One-sided straddle is not enough, since far-apart segments can straddle one carrier.
        this intersects other.carrier && other intersects this.carrier

/**
 * `Segment intersects Polygon => Boolean`<br>
 * Returns whether the segment touches the polygon's boundary or lies within its solid region.
 *
 * - An inside-out polygon's solid region is the closed complement of its loop.
 *
 * @receiver The segment to test.
 * @param polygon The polygon tested against.
 * @return `true` when they share a point, rounded at the `SCALE` quantum.
 */
public infix fun Segment.intersects(polygon: Polygon): Boolean =
    // Edge sweep for boundary contact. The interior probe catches the one case edges miss: a
    // segment entirely within the solid region. One endpoint suffices, since without a boundary
    // crossing the whole segment shares `start`'s side.
    polygon.edges.any { this intersects it } || start in polygon

/*
POLYGON
 */

/**
 * `Polygon intersects Line => Boolean`<br>
 * Returns whether the line reaches the polygon's solid region, rounded at the `SCALE` quantum.
 *
 * - Always `true` for an inside-out polygon, whose solid region is unbounded.
 *
 * @receiver The polygon to test.
 * @param line The line tested against.
 * @return `true` when they share a point.
 */
public infix fun Polygon.intersects(line: Line): Boolean =
    isInsideOut || edges.any { it intersects line }

/**
 * `Polygon intersects Segment => Boolean`<br>
 * Returns whether the segment touches the polygon's boundary or lies within its solid region.
 *
 * - An inside-out polygon's solid region is the closed complement of its loop.
 *
 * @receiver The polygon tested against.
 * @param segment The segment to test.
 * @return `true` when they share a point, rounded at the `SCALE` quantum.
 */
public infix fun Polygon.intersects(segment: Segment): Boolean = segment intersects this

/**
 * `Polygon intersects Polygon => Boolean`<br>
 * Returns whether the two solid regions share any point. Touching boundaries count.
 *
 * - An inside-out polygon's solid region is the outside of its loop, so two inside-out ones always intersect.
 *
 * @receiver The first polygon.
 * @param other The second polygon.
 * @return `true` when the solid regions share a point, rounded at the `SCALE` quantum.
 */
public infix fun Polygon.intersects(other: Polygon): Boolean {
    // Two complements share the far field unconditionally.
    if (isInsideOut && other.isInsideOut) return true
    // Per-edge Segment×Polygon covers boundary crossings and `this ⊂ other` (any edge's
    // interior probe fires); the trailing probe covers the remaining `other ⊂ this`.
    return edges.any { it intersects other } || other.vertices.first() in this
}
