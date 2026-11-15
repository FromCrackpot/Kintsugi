package fromcrackpot.kintsugi.cga.d2.transforms.composites

import fromcrackpot.kintsugi.cga.d2.exceptions.DegeneratePolylineException
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polyline
import fromcrackpot.kintsugi.cga.d2.transforms.Dilator
import fromcrackpot.kintsugi.cga.d2.transforms.Flector
import fromcrackpot.kintsugi.cga.d2.transforms.Motor
import fromcrackpot.kintsugi.cga.d2.transforms.Versor
import fromcrackpot.kintsugi.cga.d2.transforms.primitives.times
import fromcrackpot.kintsugi.cga.narrowed

// Vertex finiteness is exact under the action (pz copies through), so the revalidating ctor's
// at-infinity throw is unreachable. Only its rounded-coincidence check can fire.

/**
 * `Polyline * Motor => Polyline`<br>
 * Returns this polyline moved by [motor]'s rigid motion.
 *
 * @receiver The polyline to move.
 * @param T The receiver's type, preserved in the result.
 * @param motor The rigid motion to apply.
 * @return The moved polyline.
 * @throws DegeneratePolylineException when the move makes two consecutive vertices round
 *   equal at the `SCALE` quantum.
 */
public operator fun <T : Polyline> T.times(motor: Motor): T =
    narrowed(copy(Polyline(vertices.map { it * motor })))

/**
 * `Polyline * Flector => Polyline`<br>
 * Returns this polyline reflected by [flector]'s mirror motion.
 *
 * @receiver The polyline to reflect.
 * @param T The receiver's type, preserved in the result.
 * @param flector The mirror motion to apply.
 * @return The reflected polyline.
 * @throws DegeneratePolylineException when the move makes two consecutive vertices round
 *   equal at the `SCALE` quantum.
 */
public operator fun <T : Polyline> T.times(flector: Flector): T =
    narrowed(copy(Polyline(vertices.map { it * flector })))

/**
 * `Polyline * Versor => Polyline`<br>
 * Returns this polyline moved by [versor], for a versor typed only as [Versor].
 *
 * @receiver The polyline to move.
 * @param T The receiver's type, preserved in the result.
 * @param versor The motion to apply.
 * @return The moved polyline.
 * @throws DegeneratePolylineException when the move makes two consecutive vertices round
 *   equal at the `SCALE` quantum.
 */
public operator fun <T : Polyline> T.times(versor: Versor): T = when (versor) {
    is Motor -> this * versor
    is Flector -> this * versor
}

/**
 * `Polyline * Dilator => Polyline`<br>
 * Returns this polyline dilated by [dilator].
 *
 * @receiver The polyline to dilate.
 * @param T The receiver's type, preserved in the result.
 * @param dilator The dilation to apply.
 * @return The dilated polyline.
 * @throws DegeneratePolylineException when the dilation makes two consecutive vertices round
 *   equal at the `SCALE` quantum.
 */
public operator fun <T : Polyline> T.times(dilator: Dilator): T =
    narrowed(copy(Polyline(vertices.map { it * dilator })))
