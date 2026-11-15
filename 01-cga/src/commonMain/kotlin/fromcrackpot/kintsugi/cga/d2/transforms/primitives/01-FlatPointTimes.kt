package fromcrackpot.kintsugi.cga.d2.transforms.primitives

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.transforms.Dilator
import fromcrackpot.kintsugi.cga.d2.transforms.Flector
import fromcrackpot.kintsugi.cga.d2.transforms.Motor
import fromcrackpot.kintsugi.cga.d2.transforms.Versor
import fromcrackpot.kintsugi.cga.narrowed

// Versor action on FlatPoint: M ⟇ p ⟇ M̰ (proper), −F ⟇ p ⟇ F̰ (improper). The expanded forms are
// derived against the geometric antiproduct's Cayley table (see Versor.kt for the
// right-action `*` contract).

// Dilator action: D ⟇ u ⟇ D̰, rescaled by a power of the scale so the weight passes through.
// At scale = 1 it reduces to the Motor form with cos = 1, sin = 0.

/**
 * `FlatPoint * Motor => FlatPoint`<br>
 * Returns this flat point moved by [motor]'s rigid motion.
 *
 * - A direction (at infinity) ignores translation and turns with rotation.
 *
 * @receiver The flat point to move.
 * @param T The receiver's type, preserved in the result.
 * @param motor The rigid motion to apply.
 * @return The moved point.
 */
public operator fun <T : FlatPoint> T.times(motor: Motor): T =
    narrowed(
        copy(
            FlatPoint(
                px = motor.cos * this.px - motor.sin * this.py + motor.tx * this.pz, // e41
                py = motor.sin * this.px + motor.cos * this.py + motor.ty * this.pz, // e42
                pz = this.pz,                                                        // e43
            )
        )
    )

/**
 * `FlatPoint * Flector => FlatPoint`<br>
 * Returns this flat point reflected by [flector]'s mirror motion.
 *
 * - A direction at infinity reflects as a free vector. Mirror placement and glide do not apply.
 *
 * @receiver The flat point to reflect.
 * @param T The receiver's type, preserved in the result.
 * @param flector The mirror motion to apply.
 * @return The reflected point.
 */
public operator fun <T : FlatPoint> T.times(flector: Flector): T {
    val fx = flector.fx
    val fy = flector.fy
    return narrowed(
        copy(
            FlatPoint(
                px = (1.0 - 2.0 * fx * fx) * this.px - 2.0 * fx * fy * this.py + flector.tx * this.pz,  // e41
                py = -2.0 * fx * fy * this.px + (1.0 - 2.0 * fy * fy) * this.py + flector.ty * this.pz, // e42
                pz = this.pz,                                                                           // e43
            )
        )
    )
}

/**
 * `FlatPoint * Versor => FlatPoint`<br>
 * Returns this flat point moved by [versor], for a versor typed only as [Versor].
 *
 * @receiver The flat point to move.
 * @param T The receiver's type, preserved in the result.
 * @param versor The motion to apply.
 * @return The moved point.
 */
public operator fun <T : FlatPoint> T.times(versor: Versor): T = when (versor) {
    is Motor -> this * versor
    is Flector -> this * versor
}

/**
 * `FlatPoint * Dilator => FlatPoint`<br>
 * Returns this flat point dilated by [dilator].
 *
 * - A direction at infinity keeps its heading. Its length is multiplied by [dilator]'s scale.
 *
 * @receiver The flat point to dilate.
 * @param T The receiver's type, preserved in the result.
 * @param dilator The dilation to apply.
 * @return The dilated point.
 */
public operator fun <T : FlatPoint> T.times(dilator: Dilator): T =
    narrowed(
        copy(
            FlatPoint(
                px = dilator.scale * this.px + dilator.tx * this.pz, // e41
                py = dilator.scale * this.py + dilator.ty * this.pz, // e42
                pz = this.pz,                                        // e43
            )
        )
    )
