package fromcrackpot.kintsugi.cga.d2.queries.predicates

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Line
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Segment
import fromcrackpot.kintsugi.cga.d2.products.dot
import fromcrackpot.kintsugi.cga.round

/*
LINE
 */

/**
 * `Line intersects Segment => Boolean`<br>
 * Returns whether the line touches or crosses the segment.
 *
 * `Segment intersects Line` with the operands swapped — the identical rounded decision.
 *
 * @receiver The line to test.
 * @param segment The segment tested against.
 * @return `true` when the rounded straddle test passes.
 */
public infix fun Line.intersects(segment: Segment): Boolean = segment intersects this

/**
 * `Line intersects Polygon => Boolean`<br>
 * Returns whether the line reaches the polygon's solid region.
 *
 * `Polygon intersects Line` with the operands swapped — the identical rounded decision,
 * orientation-awareness included.
 *
 * @receiver The line to test.
 * @param polygon The polygon tested against.
 * @return `true` when [polygon] is inside-out, or any edge touches or crosses the line.
 */
public infix fun Line.intersects(polygon: Polygon): Boolean = polygon intersects this

/*
SEGMENT
 */

/**
 * `Segment intersects Line => Boolean`<br>
 * Returns whether the line touches or crosses the segment.
 *
 * A straddle test: the segment's endpoints must not lie strictly on one side of the line —
 * the two signed distances, each rounded at the `SCALE` quantum, must not multiply to a
 * positive value. Touching counts: an endpoint within the quantum of the line rounds to
 * zero, and a collinear segment intersects along its whole length.
 *
 * @receiver The segment to test.
 * @param line The line tested against.
 * @return `true` when the rounded straddle test passes.
 */
public infix fun Segment.intersects(line: Line): Boolean =
    round(start dot line) * round(end dot line) <= 0.0

/**
 * `Segment intersects Segment => Boolean`<br>
 * Returns whether the segments touch, cross, or overlap, endpoints included.
 *
 * True when the segments share at least one point, every gate rounded at the `SCALE`
 * quantum — touching within the quantum counts. Collinear segments intersect when they
 * overlap along the shared carrier.
 *
 * @receiver The first segment.
 * @param other The second segment.
 * @return `true` when the segments share at least one point.
 */
public infix fun Segment.intersects(other: Segment): Boolean =
    // Two branches covering each other's blind spots — don't collapse:
    //   X crossing (interior×interior): the `on` probes miss it, mutual straddle catches it.
    //   Collinear overlap: the `on` probes catch it; straddle false-positives on disjoint
    //   collinear segments (all four dots round to 0).
    if (other.start on carrier && other.end on carrier)
        // Interval overlap always contains an endpoint of one segment; the third probe
        // catches `this` nested strictly inside `other`.
        other.start on this || other.end on this || start on other
    else
        // One-sided straddle is not enough — far-apart segments can straddle one carrier.
        this intersects other.carrier && other intersects this.carrier

/**
 * `Segment intersects Polygon => Boolean`<br>
 * Returns whether the segment touches the polygon's boundary or lies within its solid region.
 *
 * True when the segment and the closed solid region share at least one point, every gate
 * rounded at the `SCALE` quantum: contact with any edge, or the segment entirely within.
 * The polygon's orientation decides its solid side — a classic polygon fills its loop, an
 * inside-out one ([Polygon.isInsideOut]) the closed complement.
 *
 * @receiver The segment to test.
 * @param polygon The polygon tested against.
 * @return `true` when the segment and the solid region share at least one point.
 */
public infix fun Segment.intersects(polygon: Polygon): Boolean =
    // Edge sweep for boundary contact; the interior probe catches the one case edges miss —
    // a segment entirely within the solid region. One endpoint suffices: with no boundary
    // crossing, the whole segment shares `start`'s side of the boundary.
    polygon.edges.any { this intersects it } || start in polygon

/*
POLYGON
 */

/**
 * `Polygon intersects Line => Boolean`<br>
 * Returns whether the line reaches the polygon's solid region.
 *
 * For a classic polygon, true when the line and the closed region share at least one point —
 * necessarily on the boundary, since an infinite line cannot lie inside a bounded region;
 * rounded at the `SCALE` quantum. An inside-out polygon ([Polygon.isInsideOut]) always
 * intersects: its solid region is unbounded in every direction, and an infinite line always
 * reaches it.
 *
 * @receiver The polygon to test.
 * @param line The line tested against.
 * @return `true` when this polygon is inside-out, or any edge touches or crosses the line.
 */
public infix fun Polygon.intersects(line: Line): Boolean =
    isInsideOut || edges.any { it intersects line }

/**
 * `Polygon intersects Segment => Boolean`<br>
 * Returns whether the segment touches the polygon's boundary or lies within its solid region.
 *
 * `Segment intersects Polygon` with the operands swapped — the identical rounded decision,
 * orientation-awareness included.
 *
 * @receiver The polygon tested against.
 * @param segment The segment to test.
 * @return `true` when the segment and the solid region share at least one point.
 */
public infix fun Polygon.intersects(segment: Segment): Boolean = segment intersects this

/**
 * `Polygon intersects Polygon => Boolean`<br>
 * Returns whether the two solid regions share any point — touching boundaries count.
 *
 * True when the closed solid regions overlap in any way, every gate rounded at the `SCALE`
 * quantum: crossing or touching boundaries, or one solid region entirely within the other.
 * Each operand's orientation decides its solid side — a classic polygon fills its loop, an
 * inside-out one ([Polygon.isInsideOut]) the closed complement. Two inside-out polygons
 * always intersect: both solid regions are unbounded, and far enough out every direction
 * belongs to both.
 *
 * @receiver The first polygon.
 * @param other The second polygon.
 * @return `true` when the solid regions share at least one point.
 */
public infix fun Polygon.intersects(other: Polygon): Boolean {
    // Two complements share the far field unconditionally.
    if (isInsideOut && other.isInsideOut) return true
    // Per-edge Segment×Polygon covers boundary crossings and `this ⊂ other` (any edge's
    // interior probe fires); the trailing probe covers the remaining `other ⊂ this`.
    return edges.any { it intersects other } || other.vertices.first() in this
}
