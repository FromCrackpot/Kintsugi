package fromcrackpot.kintsugi.cga.d2.transforms.primitives

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Circle
import fromcrackpot.kintsugi.cga.d2.transforms.Dilator
import fromcrackpot.kintsugi.cga.d2.transforms.Flector
import fromcrackpot.kintsugi.cga.d2.transforms.Motor
import fromcrackpot.kintsugi.cga.d2.transforms.Versor
import fromcrackpot.kintsugi.cga.narrowed

// Versor action on Circle, in all-components forms rather than a center+radius rebuild, so
// inside-out and imaginary circles transform faithfully. |cw| is preserved, so the unitizing ctor
// is a no-op pass. Derived as the trusted translation form composed with the pure
// rotation/reflection of the (cx, cy) pair. As in the FlatPoint forms, the residual translation
// applies *after* the rotation/reflection. Sign convention pinned by CircleTimesTest's join
// rebuild (d * V) join (a * V) == (d join a) * V.

// Dilator action: D ⟇ u ⟇ D̰, rescaled by a power of the scale so the weight passes through.
// At scale = 1 it reduces to the Motor form with cos = 1, sin = 0.

/**
 * `Circle * Motor => Circle`<br>
 * Returns this circle moved by [motor]'s rigid motion.
 *
 * - Radius and orientation are preserved.
 *
 * @receiver The circle to move.
 * @param T The receiver's type, preserved in the result.
 * @param motor The rigid motion to apply.
 * @return The moved circle.
 */
public operator fun <T : Circle> T.times(motor: Motor): T {
    val tx = motor.tx
    val ty = motor.ty
    val rx = motor.cos * this.cx - motor.sin * this.cy // rotated pair, pre-translation
    val ry = motor.sin * this.cx + motor.cos * this.cy
    return narrowed(
        copy(
            Circle(
                cx = rx - tx * this.cw,                                               // e423
                cy = ry - ty * this.cw,                                               // e431
                cz = this.cz - tx * rx - ty * ry
                        + ((tx * tx + ty * ty) / 2.0) * this.cw,                      // e412
                cw = this.cw,                                                         // e321
            )
        )
    )
}

/**
 * `Circle * Flector => Circle`<br>
 * Returns this circle reflected by [flector]'s mirror motion. The radius stays, the orientation flips.
 *
 * - A classic circle comes back inside-out, and vice versa.
 *
 * @receiver The circle to reflect.
 * @param T The receiver's type, preserved in the result.
 * @param flector The mirror motion to apply.
 * @return The reflected circle.
 */
public operator fun <T : Circle> T.times(flector: Flector): T {
    val fx = flector.fx
    val fy = flector.fy
    val tx = flector.tx
    val ty = flector.ty
    val rx = (1.0 - 2.0 * fx * fx) * this.cx - 2.0 * fx * fy * this.cy // reflected pair, pre-translation
    val ry = -2.0 * fx * fy * this.cx + (1.0 - 2.0 * fy * fy) * this.cy
    return narrowed(
        copy(
            Circle(
                cx = tx * this.cw - rx,                                               // e423
                cy = ty * this.cw - ry,                                               // e431
                cz = tx * rx + ty * ry - this.cz
                        - ((tx * tx + ty * ty) / 2.0) * this.cw,                      // e412
                cw = -this.cw,                                                        // e321
            )
        )
    )
}

/**
 * `Circle * Versor => Circle`<br>
 * Returns this circle moved by [versor], for a versor typed only as [Versor].
 *
 * @receiver The circle to move.
 * @param T The receiver's type, preserved in the result.
 * @param versor The motion to apply.
 * @return The moved circle.
 */
public operator fun <T : Circle> T.times(versor: Versor): T = when (versor) {
    is Motor -> this * versor
    is Flector -> this * versor
}

/**
 * `Circle * Dilator => Circle`<br>
 * Returns this circle dilated by [dilator].
 *
 * - Orientation is preserved, and the radius is multiplied by [dilator]'s scale.
 *
 * @receiver The circle to dilate.
 * @param T The receiver's type, preserved in the result.
 * @param dilator The dilation to apply.
 * @return The dilated circle.
 */
public operator fun <T : Circle> T.times(dilator: Dilator): T {
    val s = dilator.scale
    val tx = dilator.tx
    val ty = dilator.ty
    return narrowed(
        copy(
            Circle(
                cx = s * this.cx - tx * this.cw,                                      // e423
                cy = s * this.cy - ty * this.cw,                                      // e431
                cz = s * s * this.cz - s * (tx * this.cx + ty * this.cy)
                        + ((tx * tx + ty * ty) / 2.0) * this.cw,                      // e412
                cw = this.cw,                                                         // e321
            )
        )
    )
}
