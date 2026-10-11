package fromcrackpot.kintsugi.cga.d2.products

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.narrowed

/**
 * `FlatPoint * Double => FlatPoint`<br>
 * Returns this flat point with all three components multiplied by [scalar].
 *
 * The multiplication is raw, at full `Double` precision. For a finite point the factor
 * cancels on re-unitization — the same position comes back, including under a negative
 * [scalar]. For a direction (at infinity, weight kept raw by the constructor) the magnitude
 * genuinely scales. A zero [scalar] collapses any operand to the zero flat point. The result
 * is the receiver's own class: the scaled geometry returns through [FlatPoint.copy],
 * preserving a subclass that overrides the hook.
 *
 * @receiver The [FlatPoint] to scale.
 * @param T The receiver's type, preserved in the result.
 * @param scalar The factor applied to every component.
 * @return The scaled flat point, as the receiver's class — the same position when finite, a
 *   scaled direction at infinity.
 */
public operator fun <T : FlatPoint> T.times(scalar: Double): T =
    narrowed(
        copy(
            FlatPoint(
                px * scalar, // e41
                py * scalar, // e42
                pz * scalar, // e43
            )
        )
    )
