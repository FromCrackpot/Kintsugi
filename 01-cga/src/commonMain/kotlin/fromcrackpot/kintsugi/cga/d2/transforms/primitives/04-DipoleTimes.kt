package fromcrackpot.kintsugi.cga.d2.transforms.primitives

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Dipole
import fromcrackpot.kintsugi.cga.d2.transforms.Dilator
import fromcrackpot.kintsugi.cga.d2.transforms.Flector
import fromcrackpot.kintsugi.cga.d2.transforms.Motor
import fromcrackpot.kintsugi.cga.d2.transforms.Versor
import fromcrackpot.kintsugi.cga.narrowed

// Versor action on Dipole, via the outermorphism of the versor's grade-1 matrix (the RoundPoint
// action). Exact for blades: Plücker is preserved, so the unitizing ctor's check passes.
// The two forms are derivation twins, differing only in det(R): +1 (motor), −1 (flector).
// Each was verified against the rebuild identity d * V == (d.start * V) join (d.end * V).

// Dilator action: D ⟇ u ⟇ D̰, rescaled by a power of the scale so the weight passes through.
// At scale = 1 it reduces to the Motor form with cos = 1, sin = 0.

/**
 * `Dipole * Motor => Dipole`<br>
 * Returns this dipole moved by [motor]'s rigid motion.
 *
 * - [Dipole.start] stays `start`, and [Dipole.radius2] is preserved.
 *
 * @receiver The dipole to move.
 * @param T The receiver's type, preserved in the result.
 * @param motor The rigid motion to apply.
 * @return The moved dipole.
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
 * - [Dipole.start] stays `start`, and [Dipole.radius2] is preserved.
 *
 * @receiver The dipole to reflect.
 * @param T The receiver's type, preserved in the result.
 * @param flector The mirror motion to apply.
 * @return The reflected dipole.
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
 * Returns this dipole moved by [versor], for a versor typed only as [Versor].
 *
 * @receiver The dipole to move.
 * @param T The receiver's type, preserved in the result.
 * @param versor The motion to apply.
 * @return The moved dipole.
 */
public operator fun <T : Dipole> T.times(versor: Versor): T = when (versor) {
    is Motor -> this * versor
    is Flector -> this * versor
}

/**
 * `Dipole * Dilator => Dipole`<br>
 * Returns this dipole dilated by [dilator].
 *
 * - [Dipole.start] stays `start`, and the radius is multiplied by [dilator]'s scale.
 *
 * @receiver The dipole to dilate.
 * @param T The receiver's type, preserved in the result.
 * @param dilator The dilation to apply.
 * @return The dilated dipole.
 */
public operator fun <T : Dipole> T.times(dilator: Dilator): T {
    // w: the translation's half-square norm.
    val s = dilator.scale
    val tx = dilator.tx
    val ty = dilator.ty
    val w = (tx * tx + ty * ty) / 2.0
    return narrowed(
        copy(
            Dipole(
                gx = this.gx,                                           // e23
                gy = this.gy,                                           // e31
                gz = s * this.gz - tx * this.gx - ty * this.gy,         // e12

                px = s * s * this.px + s * (tx * this.pz - ty * this.gz)
                        + tx * ty * this.gx + (w - tx * tx) * this.gy,  // e41
                py = s * s * this.py + s * (ty * this.pz + tx * this.gz)
                        + (ty * ty - w) * this.gx - tx * ty * this.gy,  // e42
                pz = s * this.pz + ty * this.gx - tx * this.gy,         // e43
            )
        )
    )
}
