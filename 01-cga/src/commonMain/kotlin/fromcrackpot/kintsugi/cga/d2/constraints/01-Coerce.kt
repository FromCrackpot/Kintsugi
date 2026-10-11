package fromcrackpot.kintsugi.cga.d2.constraints

import fromcrackpot.kintsugi.cga.d2.exceptions.DegeneratePolygonException
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.minus
import fromcrackpot.kintsugi.cga.d2.products.times
import fromcrackpot.kintsugi.cga.d2.queries.collide
import fromcrackpot.kintsugi.cga.d2.queries.escape
import fromcrackpot.kintsugi.cga.d2.queries.gap
import fromcrackpot.kintsugi.cga.d2.transforms.T
import fromcrackpot.kintsugi.cga.d2.transforms.composites.times

/**
 * `Polygon coerceOut Polygon => Polygon`<br>
 * Returns this polygon nudged clear of [collider]'s solid region, or `this` untouched when it
 * is already clear, touching included.
 *
 * The penetration found by [collide] is resolved by the minimal position fix
 * `subject * T(normal * depth)`: a translation along the penetration's correcting normal by
 * its raw depth. The collider's orientation decides which side is solid — a classic [collider]
 * is an obstacle the subject is pushed out of along the shallowest push-out, an inside-out one
 * a wall the subject is retreated back inside from its deepest protrusion. One penetration is
 * resolved per call — a subject violating several of [collider]'s faces at once converges by
 * coercing again. The query contract is [collide]'s: sound for a convex loop on [collider]
 * only (against an inside-out collider the subject may be any polygon; against a classic one
 * it must be convex too), with existence rounded at the `SCALE` quantum — a subject exactly on
 * the solid region's boundary is already valid. The nudged result is the receiver's own
 * class — the translation returns through [Polygon.copy] — and a penetration-free subject
 * returns `this` itself. [coerceIn] is the dual constraint, related through negation:
 * `subject coerceOut -polygon` and `subject coerceIn polygon` land the subject identically. A
 * resolution other than the nudge is spelled on the query itself:
 * `(subject collide collider) { … }`.
 *
 * @receiver The subject polygon to keep clear of [collider]'s solid region.
 * @param P The receiver's type, preserved in the result.
 * @param collider The polygon whose oriented solid region the subject may not reach into.
 * @return The nudged polygon as the receiver's class, or `this` untouched when there is no
 *   penetration.
 * @throws DegeneratePolygonException when the nudge lands two consecutive vertices
 *   rounded-coincident at the `SCALE` quantum.
 */
public infix fun <P : Polygon> P.coerceOut(collider: Polygon): P =
    (this collide collider)?.let { this * T(it.normal * it.depth) } ?: this

/**
 * `Polygon coerceIn Polygon => Polygon`<br>
 * Returns this polygon nudged back inside [boundary]'s solid region, or `this` untouched when
 * it already lies inside, boundary included.
 *
 * The penetration found by [escape] is resolved by the minimal position fix
 * `subject * T(normal * depth)`: a translation along the penetration's correcting normal by
 * its raw depth. Which region "inside" names is the operand's own claim: a classic [boundary]
 * keeps the subject in its bounded interior, retreated back from its deepest protrusion; an
 * inside-out one keeps it in the closed complement of its loop, pushed out of the loop along
 * the shallowest push-out. One penetration is resolved per call — a subject violating several
 * of [boundary]'s faces at once converges by coercing again. The query contract is [escape]'s:
 * sound for a convex loop on [boundary] only (against a classic boundary the subject may be
 * any polygon; against an inside-out one it must be convex too), with existence rounded at the
 * `SCALE` quantum — a subject exactly on the solid region's boundary is already valid. The
 * nudged result is the receiver's own class — the translation returns through [Polygon.copy] —
 * and a valid subject returns `this` itself. [coerceOut] is the dual constraint, related
 * through negation: `subject coerceIn -polygon` and `subject coerceOut polygon` land the
 * subject identically. A resolution other than the nudge is spelled on the query itself:
 * `(subject escape boundary) { … }`.
 *
 * @receiver The subject polygon to keep inside [boundary]'s solid region.
 * @param P The receiver's type, preserved in the result.
 * @param boundary The polygon whose oriented solid region the subject may not leave.
 * @return The nudged polygon as the receiver's class, or `this` untouched when the subject
 *   already lies inside.
 * @throws DegeneratePolygonException when the nudge lands two consecutive vertices
 *   rounded-coincident at the `SCALE` quantum.
 */
public infix fun <P : Polygon> P.coerceIn(boundary: Polygon): P =
    (this escape boundary)?.let { this * T(it.normal * it.depth) } ?: this

/**
 * `Polygon coerceOn Polygon => Polygon`<br>
 * Returns this polygon translated along the shortest straight path until it touches [support], or `this`
 * untouched when it already touches.
 *
 * The equality constraint beside [coerceOut] and [coerceIn]: where those accept every pose that does not
 * violate their region, this one accepts exactly the touching poses. A subject reaching into [support]'s
 * solid region is resolved through [collide] by the minimal position fix `subject * T(normal * depth)` —
 * a translation along the penetration's correcting normal by its raw depth, landing exactly where
 * [coerceOut] lands; a subject clear of it is drawn in through [gap] by the translation closing its
 * shortest segment, `subject * T(end - start)`. A touching subject has neither — both existence gates
 * round at the `SCALE` quantum — and returns `this` itself. The support's orientation decides which side
 * is solid, and with it where the subject comes to rest: on a classic [support] it rests against the loop
 * from outside, on an inside-out one against the loop's inner wall. The overlapping half inherits
 * [collide]'s contract — sound for a convex loop on [support], the subject convex too against a classic
 * support, one penetration resolved per call, a subject violating several faces at once converging by
 * coercing again; the separated half asks no convexity of either operand. The result is the receiver's
 * own class — the translation returns through [Polygon.copy].
 *
 * @receiver The subject polygon to rest on [support].
 * @param P The receiver's type, preserved in the result.
 * @param support The polygon whose oriented solid region the subject comes to rest against.
 * @return The translated polygon as the receiver's class, or `this` untouched when it already touches.
 * @throws DegeneratePolygonException when the translation lands two consecutive vertices
 *   rounded-coincident at the `SCALE` quantum.
 */
public infix fun <P : Polygon> P.coerceOn(support: Polygon): P =
    (this collide support)?.let { this * T(it.normal * it.depth) }
        ?: (this gap support)?.let { this * T(it.end - it.start) }
        ?: this
