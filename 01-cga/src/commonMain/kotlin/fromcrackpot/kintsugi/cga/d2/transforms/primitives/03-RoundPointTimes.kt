package fromcrackpot.kintsugi.cga.d2.transforms.primitives

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.RoundPoint
import fromcrackpot.kintsugi.cga.d2.transforms.Dilator
import fromcrackpot.kintsugi.cga.d2.transforms.Flector
import fromcrackpot.kintsugi.cga.d2.transforms.Motor
import fromcrackpot.kintsugi.cga.d2.transforms.Versor
import fromcrackpot.kintsugi.cga.narrowed

// Dilator action: D ⟇ u ⟇ D̰, rescaled by a power of the scale so the weight passes through.
// At scale = 1 it reduces to the Motor form with cos = 1, sin = 0.

/**
 * `RoundPoint * Motor => RoundPoint`<br>
 * Returns this round point moved by [motor]'s rigid motion.
 *
 * - [RoundPoint.radius2] is preserved, so a sized point keeps its size.
 *
 * @receiver The round point to move.
 * @param T The receiver's type, preserved in the result.
 * @param motor The rigid motion to apply.
 * @return The moved point.
 */
public operator fun <T : RoundPoint> T.times(motor: Motor): T {
    val tx = motor.tx
    val ty = motor.ty
    val rx = motor.cos * this.ax - motor.sin * this.ay // rotated bulk, pre-translation
    val ry = motor.sin * this.ax + motor.cos * this.ay
    return narrowed(
        copy(
            RoundPoint(
                ax = rx + tx * this.az,                                               // e1
                ay = ry + ty * this.az,                                               // e2
                az = this.az,                                                         // e3
                aw = this.aw + tx * rx + ty * ry
                        + ((tx * tx + ty * ty) / 2.0) * this.az,                      // e4
            )
        )
    )
}

/**
 * `RoundPoint * Flector => RoundPoint`<br>
 * Returns this round point reflected by [flector]'s mirror motion.
 *
 * - [RoundPoint.radius2] is preserved, so a sized point keeps its size.
 *
 * @receiver The round point to reflect.
 * @param T The receiver's type, preserved in the result.
 * @param flector The mirror motion to apply.
 * @return The reflected point.
 */
public operator fun <T : RoundPoint> T.times(flector: Flector): T {
    val fx = flector.fx
    val fy = flector.fy
    val tx = flector.tx
    val ty = flector.ty
    val rx = (1.0 - 2.0 * fx * fx) * this.ax - 2.0 * fx * fy * this.ay // reflected bulk, pre-translation
    val ry = -2.0 * fx * fy * this.ax + (1.0 - 2.0 * fy * fy) * this.ay
    return narrowed(
        copy(
            RoundPoint(
                ax = rx + tx * this.az,                                               // e1
                ay = ry + ty * this.az,                                               // e2
                az = this.az,                                                         // e3
                aw = this.aw + tx * rx + ty * ry
                        + ((tx * tx + ty * ty) / 2.0) * this.az,                      // e4
            )
        )
    )
}

/**
 * `RoundPoint * Versor => RoundPoint`<br>
 * Returns this round point moved by [versor], for a versor typed only as [Versor].
 *
 * @receiver The round point to move.
 * @param T The receiver's type, preserved in the result.
 * @param versor The motion to apply.
 * @return The moved point.
 */
public operator fun <T : RoundPoint> T.times(versor: Versor): T = when (versor) {
    is Motor -> this * versor
    is Flector -> this * versor
}

/**
 * `RoundPoint * Dilator => RoundPoint`<br>
 * Returns this round point dilated by [dilator].
 *
 * - A sized point's radius is multiplied by [dilator]'s scale.
 *
 * @receiver The round point to dilate.
 * @param T The receiver's type, preserved in the result.
 * @param dilator The dilation to apply.
 * @return The dilated point.
 */
public operator fun <T : RoundPoint> T.times(dilator: Dilator): T {
    val s = dilator.scale
    val tx = dilator.tx
    val ty = dilator.ty
    return narrowed(
        copy(
            RoundPoint(
                ax = s * this.ax + tx * this.az,                                      // e1
                ay = s * this.ay + ty * this.az,                                      // e2
                az = this.az,                                                         // e3
                aw = s * s * this.aw + s * (tx * this.ax + ty * this.ay)
                        + ((tx * tx + ty * ty) / 2.0) * this.az,                      // e4
            )
        )
    )
}
