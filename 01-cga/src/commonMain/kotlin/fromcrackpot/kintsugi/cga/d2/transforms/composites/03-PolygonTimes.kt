package fromcrackpot.kintsugi.cga.d2.transforms.composites

import fromcrackpot.kintsugi.cga.d2.exceptions.DegeneratePolygonException
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.transforms.Flector
import fromcrackpot.kintsugi.cga.d2.transforms.Motor
import fromcrackpot.kintsugi.cga.d2.transforms.Versor
import fromcrackpot.kintsugi.cga.d2.transforms.primitives.times
import fromcrackpot.kintsugi.cga.narrowed

// Vertex finiteness is exact under the action (pz copies through), so the revalidating ctor's
// at-infinity throw is unreachable — only its rounded-coincidence check can fire.

/**
 * `Polygon * Motor => Polygon`<br>
 * Returns this polygon moved by [motor]'s rigid motion.
 *
 * Builds the polygon of the moved vertices, in order — every vertex carried by the same
 * motion, so edge lengths, angles, and the winding are preserved, raw at full `Double`
 * precision. Consecutive vertices lying within a `SCALE` quantum of each other can land
 * rounded-coincident after the move, and the moved loop is then rejected. The result is the
 * receiver's own class: the moved geometry returns through [Polygon.copy], preserving a
 * subclass that overrides the hook.
 *
 * @receiver The [Polygon] to move.
 * @param T The receiver's type, preserved in the result.
 * @param motor The rigid motion to apply.
 * @return The moved polygon, as the receiver's class.
 * @throws DegeneratePolygonException when the move lands two consecutive vertices
 *   rounded-coincident at the `SCALE` quantum.
 */
public operator fun <T : Polygon> T.times(motor: Motor): T =
    narrowed(copy(Polygon(vertices.map { it * motor })))

/**
 * `Polygon * Flector => Polygon`<br>
 * Returns this polygon reflected by [flector]'s mirror motion — same shape, reversed winding.
 *
 * Builds the polygon of the reflected vertices, in order — every vertex carried by the same
 * motion, so edge lengths and angles are preserved, raw at full `Double` precision. A mirror
 * motion reverses the winding: a counter-clockwise loop comes back clockwise — a valid
 * polygon, with [Polygon.signedArea]'s sign following the new winding. Consecutive vertices
 * lying within a `SCALE` quantum of each other can land rounded-coincident after the move,
 * and the moved loop is then rejected. The result is the receiver's own class: the moved
 * geometry returns through [Polygon.copy], preserving a subclass that overrides the hook.
 *
 * @receiver The [Polygon] to reflect.
 * @param T The receiver's type, preserved in the result.
 * @param flector The mirror motion to apply.
 * @return The reflected polygon, as the receiver's class.
 * @throws DegeneratePolygonException when the move lands two consecutive vertices
 *   rounded-coincident at the `SCALE` quantum.
 */
public operator fun <T : Polygon> T.times(flector: Flector): T =
    narrowed(copy(Polygon(vertices.map { it * flector })))

/**
 * `Polygon * Versor => Polygon`<br>
 * Returns this polygon moved by [versor].
 *
 * The action for a versor typed only as [Versor]: it dispatches to the `Polygon * Motor` and
 * `Polygon * Flector` forms; a versor with a concrete static type resolves to those directly.
 *
 * @receiver The [Polygon] to move.
 * @param T The receiver's type, preserved in the result.
 * @param versor The motion to apply.
 * @return The moved polygon, as the receiver's class.
 * @throws DegeneratePolygonException when the move lands two consecutive vertices
 *   rounded-coincident at the `SCALE` quantum.
 */
public operator fun <T : Polygon> T.times(versor: Versor): T = when (versor) {
    is Motor -> this * versor
    is Flector -> this * versor
}
