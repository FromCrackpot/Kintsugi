package fromcrackpot.kintsugi.cga.d2.transforms.composites

import fromcrackpot.kintsugi.cga.d2.exceptions.DegeneratePolylineException
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polyline
import fromcrackpot.kintsugi.cga.d2.transforms.Flector
import fromcrackpot.kintsugi.cga.d2.transforms.Motor
import fromcrackpot.kintsugi.cga.d2.transforms.Versor
import fromcrackpot.kintsugi.cga.d2.transforms.primitives.times
import fromcrackpot.kintsugi.cga.narrowed

// Vertex finiteness is exact under the action (pz copies through), so the revalidating ctor's
// at-infinity throw is unreachable — only its rounded-coincidence check can fire.

/**
 * `Polyline * Motor => Polyline`<br>
 * Returns this polyline moved by [motor]'s rigid motion.
 *
 * Builds the polyline of the moved vertices, in order — every vertex carried by the same
 * motion, so the chain's segment lengths and angles are preserved, raw at full `Double`
 * precision. Consecutive vertices lying within a `SCALE` quantum of each other can land
 * rounded-coincident after the move, and the moved chain is then rejected. The result is the
 * receiver's own class: the moved geometry returns through [Polyline.copy], preserving a
 * subclass that overrides the hook.
 *
 * @receiver The [Polyline] to move.
 * @param T The receiver's type, preserved in the result.
 * @param motor The rigid motion to apply.
 * @return The moved polyline, as the receiver's class.
 * @throws DegeneratePolylineException when the move lands two consecutive vertices
 *   rounded-coincident at the `SCALE` quantum.
 */
public operator fun <T : Polyline> T.times(motor: Motor): T =
    narrowed(copy(Polyline(vertices.map { it * motor })))

/**
 * `Polyline * Flector => Polyline`<br>
 * Returns this polyline reflected by [flector]'s mirror motion.
 *
 * Builds the polyline of the reflected vertices, in order — every vertex carried by the same
 * motion, so the chain's segment lengths and angles are preserved, raw at full `Double`
 * precision. Consecutive vertices lying within a `SCALE` quantum of each other can land
 * rounded-coincident after the move, and the moved chain is then rejected. The result is the
 * receiver's own class: the moved geometry returns through [Polyline.copy], preserving a
 * subclass that overrides the hook.
 *
 * @receiver The [Polyline] to reflect.
 * @param T The receiver's type, preserved in the result.
 * @param flector The mirror motion to apply.
 * @return The reflected polyline, as the receiver's class.
 * @throws DegeneratePolylineException when the move lands two consecutive vertices
 *   rounded-coincident at the `SCALE` quantum.
 */
public operator fun <T : Polyline> T.times(flector: Flector): T =
    narrowed(copy(Polyline(vertices.map { it * flector })))

/**
 * `Polyline * Versor => Polyline`<br>
 * Returns this polyline moved by [versor].
 *
 * The action for a versor typed only as [Versor]: it dispatches to the `Polyline * Motor` and
 * `Polyline * Flector` forms; a versor with a concrete static type resolves to those directly.
 *
 * @receiver The [Polyline] to move.
 * @param T The receiver's type, preserved in the result.
 * @param versor The motion to apply.
 * @return The moved polyline, as the receiver's class.
 * @throws DegeneratePolylineException when the move lands two consecutive vertices
 *   rounded-coincident at the `SCALE` quantum.
 */
public operator fun <T : Polyline> T.times(versor: Versor): T = when (versor) {
    is Motor -> this * versor
    is Flector -> this * versor
}
