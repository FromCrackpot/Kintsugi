package fromcrackpot.kintsugi.cga.d2.transforms.primitives

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.RoundPoint
import fromcrackpot.kintsugi.cga.d2.transforms.Flector
import fromcrackpot.kintsugi.cga.d2.transforms.Motor
import fromcrackpot.kintsugi.cga.d2.transforms.Versor
import fromcrackpot.kintsugi.cga.narrowed

/**
 * `RoundPoint * Motor => RoundPoint`<br>
 * Returns this round point moved by [motor]'s rigid motion.
 *
 * The antiproduct sandwich `M ⟇ a ⟇ M̰` in expanded form, raw at full `Double` precision. The
 * position moves, the weight [RoundPoint.az] is untouched, and the round bulk follows so the
 * squared radius [RoundPoint.radius2] is preserved, imaginary values included: the null lift
 * of a position moves to the lift of the moved position, and a semantic [RoundPoint.aw] — a
 * center, an attitude — keeps its radius reading. A point at infinity — zero weight — keeps
 * that weight, its direction turned by the rotation. The result is the receiver's own class:
 * the moved geometry returns through [RoundPoint.copy], preserving a subclass that overrides
 * the hook.
 *
 * @receiver The [RoundPoint] to move.
 * @param T The receiver's type, preserved in the result.
 * @param motor The rigid motion to apply.
 * @return The moved point, as the receiver's class.
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
 * The antiproduct sandwich `−F ⟇ a ⟇ F̰` in expanded form, raw at full `Double` precision. The
 * position reflects across the mirror line and glides along it, the weight [RoundPoint.az] is
 * untouched, and the round bulk follows so the squared radius [RoundPoint.radius2] is
 * preserved, imaginary values included: the null lift of a position moves to the lift of the
 * reflected position, and a semantic [RoundPoint.aw] — a center, an attitude — keeps its
 * radius reading. A point at infinity — zero weight — keeps that weight, its direction
 * mirrored. The result is the receiver's own class: the moved geometry returns through
 * [RoundPoint.copy], preserving a subclass that overrides the hook.
 *
 * @receiver The [RoundPoint] to reflect.
 * @param T The receiver's type, preserved in the result.
 * @param flector The mirror motion to apply.
 * @return The reflected point, as the receiver's class.
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
 * Returns this round point moved by [versor].
 *
 * The action for a versor typed only as [Versor]: it dispatches to the `RoundPoint * Motor`
 * and `RoundPoint * Flector` forms; a versor with a concrete static type resolves to those
 * directly.
 *
 * @receiver The [RoundPoint] to move.
 * @param T The receiver's type, preserved in the result.
 * @param versor The motion to apply.
 * @return The moved point, as the receiver's class.
 */
public operator fun <T : RoundPoint> T.times(versor: Versor): T = when (versor) {
    is Motor -> this * versor
    is Flector -> this * versor
}
