package fromcrackpot.kintsugi.cga.d2.transforms.primitives

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Line
import fromcrackpot.kintsugi.cga.d2.transforms.Flector
import fromcrackpot.kintsugi.cga.d2.transforms.Motor
import fromcrackpot.kintsugi.cga.d2.transforms.Versor
import fromcrackpot.kintsugi.cga.narrowed

// Versor action on Line — component-identical to the trusted Dipole g-part forms in
// DipoleTimes.kt: a dipole's carrier is the Line with the same (gx, gy, gz), and the point at
// infinity e4 is fixed by rigid motions, so the sandwich commutes with the carrier —
// (d * V).carrier == d.carrier * V (LineTimesTest pins it). Cross-checked against the incidence
// adjoint (p on l ⟺ (p * V) on (l * V)); the flector form is the *negated* incidence solution —
// the grade-3 sign an improper sandwich produces. Don't "fix" the signs — the join-rebuild
// pins demand them.

/**
 * `Line * Motor => Line`<br>
 * Returns this line moved by [motor]'s rigid motion.
 *
 * The antiproduct sandwich `M ⟇ l ⟇ M̰` in expanded form, raw at full `Double` precision: the
 * normal turns with the rotation, the moment follows the translation, and orientation is
 * carried — the moved line runs through the moved points of the original in the same sense. A
 * line at infinity — zero normal — is fixed, weight included: a rigid motion leaves the
 * horizon alone. The result is the receiver's own class: the moved geometry returns through
 * [Line.copy], preserving a subclass that overrides the hook.
 *
 * @receiver The [Line] to move.
 * @param T The receiver's type, preserved in the result.
 * @param motor The rigid motion to apply.
 * @return The moved line, as the receiver's class.
 */
public operator fun <T : Line> T.times(motor: Motor): T {
    val cos = motor.cos
    val sin = motor.sin
    val p = motor.tx * cos + motor.ty * sin
    val q = motor.ty * cos - motor.tx * sin
    return narrowed(
        copy(
            Line(
                gx = cos * this.gx - sin * this.gy,       // e423
                gy = sin * this.gx + cos * this.gy,       // e431
                gz = this.gz - p * this.gx - q * this.gy, // e412
            )
        )
    )
}

/**
 * `Line * Flector => Line`<br>
 * Returns this line reflected by [flector]'s mirror motion.
 *
 * The antiproduct sandwich `−F ⟇ l ⟇ F̰` in expanded form, raw at full `Double` precision: the
 * moved line runs through the reflected points of the original, and its orientation is the
 * genuinely mirrored one — running direction and normal both reflect. A line at infinity —
 * zero normal — keeps its point set and comes back with [Line.gz] negated: the horizon is
 * fixed, its orientation reversed by the improper motion. The result is the receiver's own
 * class: the moved geometry returns through [Line.copy], preserving a subclass that overrides
 * the hook.
 *
 * @receiver The [Line] to reflect.
 * @param T The receiver's type, preserved in the result.
 * @param flector The mirror motion to apply.
 * @return The reflected line, as the receiver's class.
 */
public operator fun <T : Line> T.times(flector: Flector): T {
    val fx = flector.fx
    val fy = flector.fy
    val r11 = 1.0 - 2.0 * fx * fx
    val r12 = -2.0 * fx * fy // = r21 (reflection matrix is symmetric)
    val r22 = 1.0 - 2.0 * fy * fy
    val tx = flector.tx
    val ty = flector.ty
    return narrowed(
        copy(
            Line(
                gx = this.gx * r22 - this.gy * r12,                                               // e423
                gy = this.gy * r11 - this.gx * r12,                                               // e431
                gz = this.gx * (r12 * ty - r22 * tx) + this.gy * (tx * r12 - ty * r11) - this.gz, // e412
            )
        )
    )
}

/**
 * `Line * Versor => Line`<br>
 * Returns this line moved by [versor].
 *
 * The action for a versor typed only as [Versor]: it dispatches to the `Line * Motor` and
 * `Line * Flector` forms; a versor with a concrete static type resolves to those directly.
 *
 * @receiver The [Line] to move.
 * @param T The receiver's type, preserved in the result.
 * @param versor The motion to apply.
 * @return The moved line, as the receiver's class.
 */
public operator fun <T : Line> T.times(versor: Versor): T = when (versor) {
    is Motor -> this * versor
    is Flector -> this * versor
}
