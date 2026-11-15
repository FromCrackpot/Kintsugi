package fromcrackpot.kintsugi.cga.d2.transforms.composites

import fromcrackpot.kintsugi.cga.d2.exceptions.DegeneratePolygonException
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.transforms.Dilator
import fromcrackpot.kintsugi.cga.d2.transforms.Flector
import fromcrackpot.kintsugi.cga.d2.transforms.Motor
import fromcrackpot.kintsugi.cga.d2.transforms.Versor
import fromcrackpot.kintsugi.cga.d2.transforms.primitives.times
import fromcrackpot.kintsugi.cga.narrowed

// Vertex finiteness is exact under the action (pz copies through), so the revalidating ctor's
// at-infinity throw is unreachable. Only its rounded-coincidence check can fire.

/**
 * `Polygon * Motor => Polygon`<br>
 * Returns this polygon moved by [motor]'s rigid motion.
 *
 * @receiver The polygon to move.
 * @param T The receiver's type, preserved in the result.
 * @param motor The rigid motion to apply.
 * @return The moved polygon.
 * @throws DegeneratePolygonException when the move makes two consecutive vertices round
 *   equal at the `SCALE` quantum.
 */
public operator fun <T : Polygon> T.times(motor: Motor): T =
    narrowed(copy(Polygon(vertices.map { it * motor })))

/**
 * `Polygon * Flector => Polygon`<br>
 * Returns this polygon reflected by [flector]'s mirror motion. The shape stays, the winding reverses.
 *
 * - A classic polygon comes back inside-out, and vice versa.
 *
 * @receiver The polygon to reflect.
 * @param T The receiver's type, preserved in the result.
 * @param flector The mirror motion to apply.
 * @return The reflected polygon.
 * @throws DegeneratePolygonException when the move makes two consecutive vertices round
 *   equal at the `SCALE` quantum.
 */
public operator fun <T : Polygon> T.times(flector: Flector): T =
    narrowed(copy(Polygon(vertices.map { it * flector })))

/**
 * `Polygon * Versor => Polygon`<br>
 * Returns this polygon moved by [versor], for a versor typed only as [Versor].
 *
 * @receiver The polygon to move.
 * @param T The receiver's type, preserved in the result.
 * @param versor The motion to apply.
 * @return The moved polygon.
 * @throws DegeneratePolygonException when the move makes two consecutive vertices round
 *   equal at the `SCALE` quantum.
 */
public operator fun <T : Polygon> T.times(versor: Versor): T = when (versor) {
    is Motor -> this * versor
    is Flector -> this * versor
}

/**
 * `Polygon * Dilator => Polygon`<br>
 * Returns this polygon dilated by [dilator].
 *
 * @receiver The polygon to dilate.
 * @param T The receiver's type, preserved in the result.
 * @param dilator The dilation to apply.
 * @return The dilated polygon.
 * @throws DegeneratePolygonException when the dilation makes two consecutive vertices round
 *   equal at the `SCALE` quantum.
 */
public operator fun <T : Polygon> T.times(dilator: Dilator): T =
    narrowed(copy(Polygon(vertices.map { it * dilator })))
