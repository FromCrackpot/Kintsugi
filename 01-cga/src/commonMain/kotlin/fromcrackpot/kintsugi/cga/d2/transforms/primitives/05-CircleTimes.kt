package fromcrackpot.kintsugi.cga.d2.transforms.primitives

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Circle
import fromcrackpot.kintsugi.cga.d2.transforms.Flector
import fromcrackpot.kintsugi.cga.d2.transforms.Motor
import fromcrackpot.kintsugi.cga.d2.transforms.Versor
import fromcrackpot.kintsugi.cga.narrowed

// Versor action on Circle — all-components forms (not center+radius reconstruction), so
// inside-out and imaginary circles transform faithfully; |cw| is preserved, making the
// unitizing ctor a no-op pass. Derived by composing the trusted translation form with the pure
// rotation/reflection of the (cx, cy) pair — the residual translation applies *after* the
// rotation/reflection, exactly as the FlatPoint forms do. Sign convention pinned by the
// CircleTimesTest join rebuild (d * V) join (a * V) == (d join a) * V.

/**
 * `Circle * Motor => Circle`<br>
 * Returns this circle moved by [motor]'s rigid motion.
 *
 * The antiproduct sandwich `M ⟇ c ⟇ M̰` in expanded form, raw at full `Double` precision. The
 * center moves with the motion, the squared radius [Circle.radius2] is preserved — imaginary
 * values included — and the weight [Circle.cw] is untouched, so a classic circle stays classic
 * and an inside-out one stays inside-out. The result is the receiver's own class: the moved
 * geometry returns through [Circle.copy], preserving a subclass that overrides the hook.
 *
 * @receiver The [Circle] to move.
 * @param T The receiver's type, preserved in the result.
 * @param motor The rigid motion to apply.
 * @return The moved circle, as the receiver's class.
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
 * Returns this circle reflected by [flector]'s mirror motion — same radius, opposite
 * orientation.
 *
 * The antiproduct sandwich `−F ⟇ c ⟇ F̰` in expanded form, raw at full `Double` precision. The
 * center reflects across the mirror line and glides along it, the squared radius
 * [Circle.radius2] is preserved — imaginary values included — and the weight [Circle.cw]
 * negates: a classic circle reflects to an inside-out one and back, the algebra's record that
 * a mirror motion reverses orientation — first-class data, never an error. The result is the
 * receiver's own class: the moved geometry returns through [Circle.copy], preserving a
 * subclass that overrides the hook.
 *
 * @receiver The [Circle] to reflect.
 * @param T The receiver's type, preserved in the result.
 * @param flector The mirror motion to apply.
 * @return The reflected circle, as the receiver's class.
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
 * Returns this circle moved by [versor].
 *
 * The action for a versor typed only as [Versor]: it dispatches to the `Circle * Motor` and
 * `Circle * Flector` forms; a versor with a concrete static type resolves to those directly.
 *
 * @receiver The [Circle] to move.
 * @param T The receiver's type, preserved in the result.
 * @param versor The motion to apply.
 * @return The moved circle, as the receiver's class.
 */
public operator fun <T : Circle> T.times(versor: Versor): T = when (versor) {
    is Motor -> this * versor
    is Flector -> this * versor
}
