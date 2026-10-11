package fromcrackpot.kintsugi.cga.d2.queries

import fromcrackpot.kintsugi.cga.d2.projections.project
import fromcrackpot.kintsugi.cga.d2.queries.measures.distanceTo
import fromcrackpot.kintsugi.cga.d2.queries.predicates.intersects
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Segment
import fromcrackpot.kintsugi.cga.round

/**
 * `Polygon gap Polygon => Segment?`<br>
 * Returns the shortest segment from this polygon to [other] — from a point of one boundary to the nearest
 * point of the other's — or `null` when their solid regions share any point, touching included.
 *
 * The closest pair of the two boundaries as a [Segment] whose [Segment.start] lies on this polygon and
 * [Segment.end] on [other]; its length is the shortest distance between them, raw at full `Double`
 * precision. Existence is the closed region overlap `Polygon intersects Polygon`, rounded at the `SCALE`
 * quantum: two polygons whose solid regions overlap or touch have no gap — whether a vertex of one rests
 * on a face of the other or a face on a vertex — and a shortest distance that rounds to zero at the
 * quantum answers `null` all the same. Each operand is its oriented solid region: a classic polygon
 * fills its loop, an inside-out one ([Polygon.isInsideOut]) the closed complement, so a polygon within
 * an inside-out one pairs to the loop from inside, and two inside-out polygons never have a gap. No
 * convexity is required of either operand. When several pairs share the shortest distance — parallel
 * faces — one is picked arbitrarily; the displacement from [Segment.start] to [Segment.end] is the same
 * for all of them.
 *
 * @receiver The polygon the segment starts on.
 * @param other The polygon the segment ends on.
 * @return The raw shortest [Segment] from this polygon to [other], or `null` when the solid regions
 *   share at least one point.
 */
public infix fun Polygon.gap(other: Polygon): Segment? {
    if (this intersects other) return null
    val candidates = vertices.flatMap { v -> other.edges.map { e -> v to (v project e) } } +
        other.vertices.flatMap { v -> edges.map { e -> (v project e) to v } }
    val (start, end) = candidates.minBy { (a, b) -> a distanceTo b }
    // Not redundant with `intersects`: the wedge's own degenerate criterion — the ctor never meets a coincident pair.
    if (round(start distanceTo end) == 0.0) return null
    return Segment(start, end)
}
