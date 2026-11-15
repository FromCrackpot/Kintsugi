package fromcrackpot.kintsugi.cga.d2.operations.queries.measures

import fromcrackpot.kintsugi.cga.Angle
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Line
import kotlin.math.atan2


/*
LINE
 */

/**
 * `Line angleTo Line => Angle`<br>
 * Returns the signed angle turning this line's direction onto [other]'s. It is counter-clockwise
 * positive, in `(−π, π]`.
 *
 * - Reversing either line offsets the answer by a half turn.
 * - A line at infinity has no direction, so the answer degenerates to the zero angle.
 *
 * @receiver The line the angle turns from.
 * @param other The line the angle turns onto.
 * @return The raw signed angle.
 */
public infix fun Line.angleTo(other: Line): Angle = Angle(
    atan2(
        this.gx * other.gy - this.gy * other.gx + 0.0,
        this.gx * other.gx + this.gy * other.gy + 0.0
    )
)
