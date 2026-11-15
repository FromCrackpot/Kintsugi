package fromcrackpot.kintsugi.cga.d2.operations.queries

import fromcrackpot.kintsugi.cga.d2.exceptions.NonConvexPolygonException
import fromcrackpot.kintsugi.cga.d2.operations.products.dot
import fromcrackpot.kintsugi.cga.d2.operations.products.times
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Segment
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Direction
import fromcrackpot.kintsugi.cga.round

/**
 * One polygon's penetration into a region it may not occupy: the violated face, how deep, and
 * the way out.
 *
 * - Moving the subject by [depth] along [normal] resolves it.
 * - Each answer holds one violated face. Several converge by resolving and querying again.
 *
 * @see onCollide
 * @see onEscape
 */
public class Penetration(
    /** The violated face. It is always the queried polygon's own edge, never the subject's. */
    public val edge: Segment,
    /** The raw Euclidean depth past [edge][Penetration.edge], positive at the `SCALE` quantum. */
    public val depth: Double,
    /** The correcting direction: unit length, pointing out of the region the subject may not occupy. */
    public val normal: Direction,
) {
    /** Returns the raw components at full stored precision. */
    override fun toString(): String = "Penetration(edge=$edge, depth=$depth, normal=$normal)"
}

/**
 * `Polygon onCollide Polygon => Penetration?`<br>
 * Returns the penetration where this polygon reaches into [collider]'s solid region, or `null`
 * when the two are clear. Touching counts as clear.
 *
 * - A classic [collider] is an obstacle. The answer is its shallowest push-out.
 * - An inside-out [collider] is a wall. The answer is the deepest protrusion.
 *
 * @receiver The subject polygon.
 * @param collider The polygon whose solid region the subject may not reach into.
 * @return The penetration, or `null` when clear at the `SCALE` quantum.
 * @throws NonConvexPolygonException when [collider] is not convex. Against a classic collider,
 *   also when the subject is not convex.
 * @see onEscape
 */
public infix fun Polygon.onCollide(collider: Polygon): Penetration? = penetrate(collider, out = false)

/**
 * `Polygon onEscape Polygon => Penetration?`<br>
 * Returns the penetration where this polygon reaches out of [boundary]'s solid region, or
 * `null` when it lies within. Touching counts as within.
 *
 * - A classic [boundary] is a container. The answer is the deepest protrusion.
 * - An inside-out [boundary]'s loop is off limits. The answer is the shallowest push-out.
 *
 * @receiver The subject polygon.
 * @param boundary The polygon whose solid region the subject may not leave.
 * @return The penetration, or `null` when within at the `SCALE` quantum.
 * @throws NonConvexPolygonException when [boundary] is not convex. Against an inside-out boundary,
 *   also when the subject is not convex.
 * @see onCollide
 */
public infix fun Polygon.onEscape(boundary: Polygon): Penetration? = penetrate(boundary, out = true)

// out ⟺ the violation is lying outside `region`'s solid (onEscape), else inside it (onCollide).
//   out  insideOut  strategy                    gate                 normal
//   F    F          ∃ push-out (min violation)  separatedByOwnFace   −carrier.normal
//   F    T          ∀ retreat  (max violation)  none                    −carrier.normal
//   T    F          ∀ retreat  (max violation)  none                    +carrier.normal
//   T    T          ∃ push-out (min violation)  separatedByOwnFace   +carrier.normal
// A carrier's normal points into its loop's solid region in both windings, so sign·(v dot carrier)
// is the valid-side clearance for either verb.
private fun Polygon.penetrate(region: Polygon, out: Boolean): Penetration? {
    val sign = if (out) 1.0 else -1.0
    val retreat = out xor region.isInsideOut
    if (!region.isConvex) {
        throw NonConvexPolygonException("${if (out) "boundary" else "collider"} is not convex: ${region.toStringRounded()}")
    }
    if (!retreat && !isConvex) throw NonConvexPolygonException("subject is not convex: ${toStringRounded()}")
    if (!retreat && separatedByOwnFace(region)) return null
    val objective = if (retreat) -1.0 else 1.0
    var pick = -1
    var best = Double.NEGATIVE_INFINITY

    region.edges.forEachIndexed { i, edge ->
        val carrier = edge.carrier
        val clearance = vertices.minOf { v -> sign * (v dot carrier) }
        val score = objective * clearance
        if (score > best) {
            best = score
            pick = i
        }
    }

    val depth = if (retreat) best else -best
    if (round(depth) <= 0.0) return null
    val edge = region.edges[pick]

    return Penetration(edge, depth, edge.carrier.normal * sign)
}

// The subject-face half of SAT, not redundant with the depth gate. A separation that only this
// loop's own faces witness leaves every `region` face violated, so depth alone would report a
// bogus penetration. Closed (≥ 0): a region vertex resting on a face is touching.
private fun Polygon.separatedByOwnFace(region: Polygon): Boolean {
    val s = if (isInsideOut) -1.0 else 1.0
    return edges.any { e -> round(region.vertices.minOf { v -> -s * (v dot e.carrier) }) >= 0.0 }
}

/**
 * `Penetration? { onPenetration } => Penetration?`<br>
 * Runs [onPenetration] on the penetration when there is one, and returns the receiver
 * unchanged either way.
 *
 * - Parenthesize the query: `(subject onCollide collider) { … }`. Bare, the lambda binds to `collider`.
 * - A `?:` after the call runs only when there was no penetration.
 *
 * @receiver A penetration query's answer.
 * @param onPenetration The handler, run once with the penetration. An exception it throws propagates.
 * @return This receiver: the penetration, or `null`.
 */
public operator fun Penetration?.invoke(onPenetration: (Penetration) -> Unit): Penetration? =
    this?.also(onPenetration)
