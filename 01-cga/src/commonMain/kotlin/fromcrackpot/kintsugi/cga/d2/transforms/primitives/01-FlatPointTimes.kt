package fromcrackpot.kintsugi.cga.d2.transforms.primitives

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.transforms.Flector
import fromcrackpot.kintsugi.cga.d2.transforms.Motor
import fromcrackpot.kintsugi.cga.d2.transforms.Versor
import fromcrackpot.kintsugi.cga.narrowed

// Versor action on FlatPoint — M ⟇ p ⟇ M̰ (proper) / −F ⟇ p ⟇ F̰ (improper), expanded forms
// derived against the geometric antiproduct's Cayley table (see Versor.kt for the
// right-action `*` contract).

/**
 * `FlatPoint * Motor => FlatPoint`<br>
 * Returns this flat point moved by [motor]'s rigid motion.
 *
 * The antiproduct sandwich `M ⟇ p ⟇ M̰` in expanded form, raw at full `Double` precision. A
 * finite point comes back at its rotated or translated position, weight untouched. A point at
 * infinity — zero weight — is position-free: a translation fixes it, a rotation turns it as a
 * free vector. The result is the receiver's own class: the moved geometry returns through
 * [FlatPoint.copy], preserving a subclass that overrides the hook.
 *
 * @receiver The [FlatPoint] to move.
 * @param T The receiver's type, preserved in the result.
 * @param motor The rigid motion to apply.
 * @return The moved point, as the receiver's class.
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
 * The antiproduct sandwich `−F ⟇ p ⟇ F̰` in expanded form, raw at full `Double` precision. A
 * finite point reflects across the mirror line and glides along it, weight untouched. A point at infinity —
 * zero weight — reflects as a free vector: only the mirror's direction matters there; its
 * placement and the glide have no effect. The result is the receiver's own class: the moved
 * geometry returns through [FlatPoint.copy], preserving a subclass that overrides the hook.
 *
 * @receiver The [FlatPoint] to reflect.
 * @param T The receiver's type, preserved in the result.
 * @param flector The mirror motion to apply.
 * @return The reflected point, as the receiver's class.
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
 * Returns this flat point moved by [versor].
 *
 * The action for a versor typed only as [Versor]: it dispatches to the `FlatPoint * Motor` and
 * `FlatPoint * Flector` forms; a versor with a concrete static type resolves to those directly.
 *
 * @receiver The [FlatPoint] to move.
 * @param T The receiver's type, preserved in the result.
 * @param versor The motion to apply.
 * @return The moved point, as the receiver's class.
 */
public operator fun <T : FlatPoint> T.times(versor: Versor): T = when (versor) {
    is Motor -> this * versor
    is Flector -> this * versor
}
