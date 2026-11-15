package fromcrackpot.kintsugi.cga.d2.operations.products

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.narrowed

/**
 * `FlatPoint * Double => FlatPoint`<br>
 * Returns this flat point with all three components multiplied by [scalar], raw.
 *
 * - A zero [scalar] collapses any operand to the zero flat point.
 *
 * @receiver The flat point to scale.
 * @param T The receiver's type, preserved in the result.
 * @param scalar The factor applied to every component.
 * @return The scaled flat point. A finite one keeps its position; one at infinity scales as a direction.
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
