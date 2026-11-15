package fromcrackpot.kintsugi.cga.d2.transforms.primitives

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Line
import fromcrackpot.kintsugi.cga.d2.transforms.Dilator
import fromcrackpot.kintsugi.cga.d2.transforms.Flector
import fromcrackpot.kintsugi.cga.d2.transforms.Motor
import fromcrackpot.kintsugi.cga.d2.transforms.Versor
import fromcrackpot.kintsugi.cga.narrowed

// Versor action on Line: component-identical to the trusted Dipole g-part forms in DipoleTimes.kt.
//   - A dipole's carrier is the Line with the same (gx, gy, gz), and rigid motions fix e4, so the
//     sandwich commutes with the carrier: (d * V).carrier == d.carrier * V (LineTimesTest pins it).
//   - Cross-checked against the incidence adjoint p on l ⟺ (p * V) on (l * V).
//   - The flector form is the *negated* incidence solution, the grade-3 sign of an improper
//     sandwich. Don't "fix" the signs: the join-rebuild pins demand them.

// Dilator action: D ⟇ u ⟇ D̰, rescaled by a power of the scale so the weight passes through.
// At scale = 1 it reduces to the Motor form with cos = 1, sin = 0.

/**
 * `Line * Motor => Line`<br>
 * Returns this line moved by [motor]'s rigid motion.
 *
 * - Orientation is carried. A line at infinity stays fixed.
 *
 * @receiver The line to move.
 * @param T The receiver's type, preserved in the result.
 * @param motor The rigid motion to apply.
 * @return The moved line.
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
 * - The orientation mirrors. A line at infinity keeps its points, with [Line.gz] negated.
 *
 * @receiver The line to reflect.
 * @param T The receiver's type, preserved in the result.
 * @param flector The mirror motion to apply.
 * @return The reflected line.
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
 * Returns this line moved by [versor], for a versor typed only as [Versor].
 *
 * @receiver The line to move.
 * @param T The receiver's type, preserved in the result.
 * @param versor The motion to apply.
 * @return The moved line.
 */
public operator fun <T : Line> T.times(versor: Versor): T = when (versor) {
    is Motor -> this * versor
    is Flector -> this * versor
}

/**
 * `Line * Dilator => Line`<br>
 * Returns this line dilated by [dilator].
 *
 * - Orientation is carried. A line at infinity keeps its points, with [Line.gz] multiplied by the scale.
 *
 * @receiver The line to dilate.
 * @param T The receiver's type, preserved in the result.
 * @param dilator The dilation to apply.
 * @return The dilated line.
 */
public operator fun <T : Line> T.times(dilator: Dilator): T =
    narrowed(
        copy(
            Line(
                gx = this.gx,                                                               // e423
                gy = this.gy,                                                               // e431
                gz = dilator.scale * this.gz - dilator.tx * this.gx - dilator.ty * this.gy, // e412
            )
        )
    )
