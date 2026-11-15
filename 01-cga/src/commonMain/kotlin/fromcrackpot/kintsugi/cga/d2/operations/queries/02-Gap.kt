package fromcrackpot.kintsugi.cga.d2.operations.queries

import fromcrackpot.kintsugi.cga.d2.operations.projections.project
import fromcrackpot.kintsugi.cga.d2.operations.queries.measures.distanceTo
import fromcrackpot.kintsugi.cga.d2.operations.queries.predicates.intersects
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Segment
import fromcrackpot.kintsugi.cga.round

/**
 * `Polygon gap Polygon => Segment?`<br>
 * Returns the shortest segment from this polygon's boundary to [other]'s, or `null` when their
 * solid regions share any point. Touching counts as sharing.
 *
 * - An inside-out polygon's solid region is the outside of its loop, so two inside-out ones never have a gap.
 *
 * @receiver The polygon the segment starts on.
 * @param other The polygon the segment ends on.
 * @return The raw segment, `start` on this polygon and `end` on [other], or `null` when the regions meet.
 * @see intersects
 */
public infix fun Polygon.gap(other: Polygon): Segment? {
    if (this intersects other) return null
    val candidates = vertices.flatMap { v -> other.edges.map { e -> v to (v project e) } } +
        other.vertices.flatMap { v -> edges.map { e -> (v project e) to v } }
    val (start, end) = candidates.minBy { (a, b) -> a distanceTo b }
    // Not redundant with `intersects`. This is the wedge's own degenerate criterion, so the ctor
    // never meets a coincident pair.
    if (round(start distanceTo end) == 0.0) return null
    return Segment(start, end)
}
