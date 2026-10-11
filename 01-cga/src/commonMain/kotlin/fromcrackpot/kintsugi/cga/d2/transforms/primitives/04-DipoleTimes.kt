package fromcrackpot.kintsugi.cga.d2.transforms.primitives

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Dipole
import fromcrackpot.kintsugi.cga.d2.transforms.Flector
import fromcrackpot.kintsugi.cga.d2.transforms.Motor
import fromcrackpot.kintsugi.cga.d2.transforms.Versor
import fromcrackpot.kintsugi.cga.narrowed

// Versor action on Dipole — via the outermorphism of the versor's grade-1 matrix (the RoundPoint
// action), exact for blades: Plücker is preserved, so the unitizing ctor's check passes. The two
// forms are derivation twins whose sole structural difference is det(R) = +1 (motor) vs −1
// (flector); each was verified against the rebuild identity d * V == (d.start * V) join (d.end * V).

/**
 * `Dipole * Motor => Dipole`<br>
 * Returns this dipole moved by [motor]'s rigid motion.
 *
 * The antiproduct sandwich `M ⟇ d ⟇ M̰` in expanded form, raw at full `Double` precision. The
 * result is the dipole of the moved endpoints, [Dipole.start] staying `start`, and the squared
 * radius [Dipole.radius2] is preserved, imaginary values included. An at-infinity dipole —
 * zero `gx` and `gy` — stays at infinity, `gz` copied exactly. The result is the receiver's
 * own class: the moved geometry returns through [Dipole.copy], preserving a subclass that
 * overrides the hook.
 *
 * @receiver The [Dipole] to move.
 * @param T The receiver's type, preserved in the result.
 * @param motor The rigid motion to apply.
 * @return The moved dipole, as the receiver's class.
 */
public operator fun <T : Dipole> T.times(motor: Motor): T {
    // p/q: the translation re-expressed in the rotated frame; w: its half-square norm.
    val cos = motor.cos
    val sin = motor.sin
    val tx = motor.tx
    val ty = motor.ty
    val p = tx * cos + ty * sin
    val q = ty * cos - tx * sin
    val w = (tx * tx + ty * ty) / 2.0
    return narrowed(
        copy(
            Dipole(
                gx = cos * this.gx - sin * this.gy,                     // e23
                gy = sin * this.gx + cos * this.gy,                     // e31
                gz = this.gz - p * this.gx - q * this.gy,               // e12

                px = cos * this.px - sin * this.py + tx * this.pz - ty * this.gz
                        + (sin * w + q * tx) * this.gx + (cos * w - p * tx) * this.gy, // e41
                py = sin * this.px + cos * this.py + ty * this.pz + tx * this.gz
                        + (q * ty - cos * w) * this.gx + (sin * w - p * ty) * this.gy, // e42
                pz = this.pz + q * this.gx - p * this.gy,               // e43
            )
        )
    )
}

/**
 * `Dipole * Flector => Dipole`<br>
 * Returns this dipole reflected by [flector]'s mirror motion.
 *
 * The antiproduct sandwich `−F ⟇ d ⟇ F̰` in expanded form, raw at full `Double` precision. The
 * result is the dipole of the reflected endpoints, [Dipole.start] staying `start`, and the
 * squared radius [Dipole.radius2] is preserved, imaginary values included. An at-infinity
 * dipole — zero `gx` and `gy` — stays at infinity, `gz` negated exactly. The result is the
 * receiver's own class: the moved geometry returns through [Dipole.copy], preserving a
 * subclass that overrides the hook.
 *
 * @receiver The [Dipole] to reflect.
 * @param T The receiver's type, preserved in the result.
 * @param flector The mirror motion to apply.
 * @return The reflected dipole, as the receiver's class.
 */
public operator fun <T : Dipole> T.times(flector: Flector): T {
    // (mwx, mwy): the translation re-expressed in the reflected frame; w: its half-square norm.
    val fx = flector.fx
    val fy = flector.fy
    val r11 = 1.0 - 2.0 * fx * fx
    val r12 = -2.0 * fx * fy // = r21 (reflection matrix is symmetric)
    val r22 = 1.0 - 2.0 * fy * fy
    val tx = flector.tx
    val ty = flector.ty
    val mwx = tx * r11 + ty * r12
    val mwy = tx * r12 + ty * r22
    val w = (tx * tx + ty * ty) / 2.0
    return narrowed(
        copy(
            Dipole(
                gx = this.gx * r22 - this.gy * r12,                              // e23
                gy = this.gy * r11 - this.gx * r12,                              // e31
                gz = this.gx * (r12 * ty - r22 * tx) + this.gy * (tx * r12 - ty * r11) - this.gz, // e12 (det = −1)

                px = this.px * r11 + this.py * r12 + this.pz * tx
                        - this.gx * (r12 * w - mwy * tx)
                        - this.gy * (tx * mwx - w * r11)
                        - this.gz * (r11 * mwy - mwx * r12),                     // e41
                py = this.px * r12 + this.py * r22 + this.pz * ty
                        - this.gx * (r22 * w - mwy * ty)
                        - this.gy * (ty * mwx - w * r12)
                        - this.gz * (r12 * mwy - mwx * r22),                     // e42
                pz = this.gx * mwy - this.gy * mwx + this.pz,                    // e43
            )
        )
    )
}

/**
 * `Dipole * Versor => Dipole`<br>
 * Returns this dipole moved by [versor].
 *
 * The action for a versor typed only as [Versor]: it dispatches to the `Dipole * Motor` and
 * `Dipole * Flector` forms; a versor with a concrete static type resolves to those directly.
 *
 * @receiver The [Dipole] to move.
 * @param T The receiver's type, preserved in the result.
 * @param versor The motion to apply.
 * @return The moved dipole, as the receiver's class.
 */
public operator fun <T : Dipole> T.times(versor: Versor): T = when (versor) {
    is Motor -> this * versor
    is Flector -> this * versor
}
