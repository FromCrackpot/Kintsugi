package fromcrackpot.kintsugi.cga.d2.operations.constraints

import fromcrackpot.kintsugi.cga.d2.exceptions.DegeneratePolygonException
import fromcrackpot.kintsugi.cga.d2.exceptions.NonConvexPolygonException
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.minus
import fromcrackpot.kintsugi.cga.d2.operations.products.times
import fromcrackpot.kintsugi.cga.d2.operations.queries.gap
import fromcrackpot.kintsugi.cga.d2.operations.queries.onCollide
import fromcrackpot.kintsugi.cga.d2.operations.queries.onEscape
import fromcrackpot.kintsugi.cga.d2.transforms.T
import fromcrackpot.kintsugi.cga.d2.transforms.composites.times

/**
 * `Polygon coerceOut Polygon => Polygon`<br>
 * Returns this polygon nudged clear of [collider]'s solid region, or `this` untouched when it
 * is already clear. Touching counts as clear.
 *
 * - A classic [collider] pushes the subject out; an inside-out one retreats it back inside.
 * - Each call resolves one penetration. Coerce again to clear several faces.
 *
 * @receiver The subject polygon.
 * @param P The receiver's type, preserved in the result.
 * @param collider The polygon whose solid region the subject may not reach into.
 * @return The nudged polygon, or `this` when there is no penetration.
 * @throws NonConvexPolygonException when [collider] is not convex. Against a classic collider,
 *   also when the subject is not convex.
 * @throws DegeneratePolygonException when the nudge makes two consecutive vertices round equal at
 *   the `SCALE` quantum.
 * @see coerceIn
 * @see onCollide
 */
public infix fun <P : Polygon> P.coerceOut(collider: Polygon): P =
    (this onCollide collider)?.let { this * T(it.normal * it.depth) } ?: this

/**
 * `Polygon coerceIn Polygon => Polygon`<br>
 * Returns this polygon nudged back inside [boundary]'s solid region, or `this` untouched when
 * it already lies inside. The boundary counts as inside.
 *
 * - A classic [boundary] keeps the subject in its interior; an inside-out one keeps it out of its loop.
 * - Each call resolves one penetration. Coerce again to clear several faces.
 *
 * @receiver The subject polygon.
 * @param P The receiver's type, preserved in the result.
 * @param boundary The polygon whose solid region the subject may not leave.
 * @return The nudged polygon, or `this` when the subject already lies inside.
 * @throws NonConvexPolygonException when [boundary] is not convex. Against an inside-out boundary,
 *   also when the subject is not convex.
 * @throws DegeneratePolygonException when the nudge makes two consecutive vertices round equal at
 *   the `SCALE` quantum.
 * @see coerceOut
 * @see onEscape
 */
public infix fun <P : Polygon> P.coerceIn(boundary: Polygon): P =
    (this onEscape boundary)?.let { this * T(it.normal * it.depth) } ?: this

/**
 * `Polygon coerceOn Polygon => Polygon`<br>
 * Returns this polygon translated until it touches [support], or `this` untouched when it already
 * touches.
 *
 * - A subject rests outside a classic [support]. Inside an inside-out one, it resolves one face per call.
 * - An overlapping subject is nudged clear first. It ends up touching, but not always at the nearest pose.
 *
 * @receiver The subject polygon.
 * @param P The receiver's type, preserved in the result.
 * @param support The polygon whose solid region the subject comes to rest against.
 * @return The translated polygon, or `this` when it already touches.
 * @throws NonConvexPolygonException when [support] is not convex. Against a classic support, also
 *   when the subject is not convex.
 * @throws DegeneratePolygonException when a translation makes two consecutive vertices round equal
 *   at the `SCALE` quantum.
 * @see coerceOut
 * @see gap
 */
public infix fun <P : Polygon> P.coerceOn(support: Polygon): P {
    val clear = this coerceOut support
    return (clear gap support)?.let { clear * T(it.end - it.start) } ?: clear
}
