package fromcrackpot.kintsugi.cga.d2.queries

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Direction
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Segment
import fromcrackpot.kintsugi.cga.d2.products.dot
import fromcrackpot.kintsugi.cga.d2.products.times
import fromcrackpot.kintsugi.cga.round

/**
 * One polygon's resolved penetration into a region it may not occupy: the violated face, how
 * deep, and the way out.
 *
 * The single violation resolved by [collide] and [escape]; situations violating several faces
 * at once converge by resolving and querying again. Displacing the subject by [depth] along
 * [normal] resolves this penetration.
 */
public class Penetration(
    /** The violated face — always the queried polygon's own edge, never the subject's. */
    public val edge: Segment,
    /** The penetration depth past [edge] — a raw Euclidean distance, positive at the `SCALE` quantum. */
    public val depth: Double,
    /**
     * The correcting direction of the penetration — unit length, pointed out of the region the
     * subject may not occupy: the queried polygon's solid region for [collide], the complement
     * of it for [escape].
     */
    public val normal: Direction,
) {
    /** Returns the raw components — full stored precision, no rounding. */
    override fun toString(): String = "Penetration(edge=$edge, depth=$depth, normal=$normal)"
}

/**
 * `Polygon collide Polygon => Penetration?`<br>
 * Returns the penetration where this polygon reaches into [collider]'s solid region, or `null`
 * when the two are clear of each other, touching included.
 *
 * The collider's orientation decides which side is solid, and with it how the violated face is
 * picked:
 *
 * - A classic [collider] — counter-clockwise, [Polygon.isInsideOut] `false` — is an obstacle:
 *   its solid region is the loop's bounded interior. Clearing one face is enough to be out, so
 *   the answer is the face with the *shallowest* push-out — the nearest single-face way clear.
 * - An inside-out [collider] — clockwise — is a wall: its solid region is everything outside
 *   the loop, the bounded interior the allowed region. Every face must be cleared to be in, so
 *   the answer is the face with the *deepest* protrusion — resolving it retreats the subject
 *   the whole way back inside.
 *
 * [escape] is the dual query, related through negation: `subject collide -polygon` and
 * `subject escape polygon` agree on depth and normal and report the same face — as the negated
 * polygon's edge here, as the polygon's own edge there. [Penetration.depth] is the raw
 * Euclidean distance past the picked face; [Penetration.normal] is that face carrier's unit
 * normal pointed out of the solid region. Touching is not penetration: existence rounds at the
 * `SCALE` quantum, so a subject exactly on the solid region's boundary returns `null` — whether
 * a vertex of the subject rests on a face of the collider or a face of the subject on a vertex
 * of the collider.
 *
 * Sound for a convex loop on [collider] only — a concave loop yields an arbitrary face pick,
 * not an error. Against an inside-out collider the subject may be any polygon: it contributes
 * only its vertices, and within a convex allowed region a polygon lies inside exactly when its
 * vertices do. Against a classic collider the subject must be convex too — a concave subject
 * may be reported clear while its body still overlaps the collider — and faces for the pick
 * come from [collider] alone, so a deep corner-on-corner overlap may pick a face that is not
 * the shortest way out.
 *
 * @receiver The subject polygon to test against [collider]'s solid region.
 * @param collider The polygon whose oriented solid region the subject may not reach into.
 * @return The resolved penetration, or `null` when this polygon is clear of [collider]'s
 *   solid region at the `SCALE` quantum, boundary touches included.
 */
public infix fun Polygon.collide(collider: Polygon): Penetration? = penetrate(collider, out = false)

/**
 * `Polygon escape Polygon => Penetration?`<br>
 * Returns the penetration where this polygon reaches out of [boundary]'s solid region, or
 * `null` when it lies within, touching included.
 *
 * The boundary's orientation decides which side is solid, and with it how the violated face is
 * picked:
 *
 * - A classic [boundary] — counter-clockwise, [Polygon.isInsideOut] `false` — is a container:
 *   its solid region is the loop's bounded interior, which the subject may not leave. Every
 *   face must be satisfied to be in, so the answer is the face with the *deepest* protrusion —
 *   resolving it retreats the subject the whole way back inside.
 * - An inside-out [boundary] — clockwise — keeps the subject out of its loop: its solid region
 *   is everything outside the loop, and leaving it is reaching into the bounded interior.
 *   Clearing one face is enough to be out again, so the answer is the face with the
 *   *shallowest* push-out — the nearest single-face way clear.
 *
 * [collide] is the dual query, related through negation: `subject escape -polygon` and
 * `subject collide polygon` agree on depth and normal and report the same face — as the negated
 * polygon's edge here, as the polygon's own edge there. [Penetration.depth] is the raw
 * Euclidean distance past the picked face; [Penetration.normal] is that face carrier's unit
 * normal pointed back into the solid region. Touching is not penetration: existence rounds at
 * the `SCALE` quantum, so a subject exactly on the solid region's boundary returns `null` —
 * whether a vertex of the subject rests on a face of the boundary or a face of the subject on
 * a vertex of the boundary.
 *
 * Sound for a convex loop on [boundary] only — a concave loop yields an arbitrary face pick,
 * not an error. Against a classic boundary the subject may be any polygon: it contributes only
 * its vertices, and within a convex region a polygon lies inside exactly when its vertices do.
 * Against an inside-out boundary the subject must be convex too — a concave subject may be
 * reported clear while its body still reaches into the loop — and faces for the pick come from
 * [boundary] alone, so a deep corner-on-corner overlap may pick a face that is not the
 * shortest way out.
 *
 * @receiver The subject polygon to test against [boundary]'s solid region.
 * @param boundary The polygon whose oriented solid region the subject may not leave.
 * @return The resolved penetration, or `null` when this polygon lies within [boundary]'s
 *   solid region at the `SCALE` quantum, boundary touches included.
 */
public infix fun Polygon.escape(boundary: Polygon): Penetration? = penetrate(boundary, out = true)

// out ⟺ the violation is lying outside `region`'s solid (escape), else inside it (collide).
//   out  insideOut  strategy                    gate                 normal
//   F    F          ∃ push-out (min violation)  separatedByOwnFace   −carrier.normal
//   F    T          ∀ retreat  (max violation)  —                    −carrier.normal
//   T    F          ∀ retreat  (max violation)  —                    +carrier.normal
//   T    T          ∃ push-out (min violation)  separatedByOwnFace   +carrier.normal
// A carrier's normal points into its loop's solid region in both windings, so sign·(v dot carrier)
// is the valid-side clearance for either verb.
private fun Polygon.penetrate(region: Polygon, out: Boolean): Penetration? {
    val sign = if (out) 1.0 else -1.0
    val retreat = out xor region.isInsideOut
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

// The subject-face half of SAT — NOT redundant with the depth gate: a separation only this loop's
// own faces witness leaves every `region` face violated, so depth alone would report a bogus
// penetration. Closed (≥ 0): a region vertex resting on a face is touching, not penetration.
private fun Polygon.separatedByOwnFace(region: Polygon): Boolean {
    val s = if (isInsideOut) -1.0 else 1.0
    return edges.any { e -> round(region.vertices.minOf { v -> -s * (v dot e.carrier) }) >= 0.0 }
}

/**
 * `Penetration? { onPenetration } => Penetration?`<br>
 * Runs [onPenetration] on the penetration when there is one, and returns the receiver
 * unchanged either way.
 *
 * The handler spelling for a penetration query's nullable answer: the trailing lambda hangs
 * straight off the query, and the pass-through return keeps the answer live after the call —
 * `?:` behind it is an else-lane running only when there was no penetration, and a second
 * trailing lambda handles the same penetration again. An exception thrown by [onPenetration]
 * propagates unchanged. `(subject collide collider) { penetration -> … }` and
 * `(subject escape boundary) { penetration -> … }` spell the call — the parentheses are part
 * of the spelling: without them the lambda would attach to the query's right operand instead
 * of its answer.
 *
 * @receiver The penetration query's answer to handle.
 * @param onPenetration The handler, run once with the penetration when it exists.
 * @return This receiver: the penetration, or `null` when there was none.
 */
public operator fun Penetration?.invoke(onPenetration: (Penetration) -> Unit): Penetration? =
    this?.also(onPenetration)
