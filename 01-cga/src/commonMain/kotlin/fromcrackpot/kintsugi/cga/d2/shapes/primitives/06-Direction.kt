package fromcrackpot.kintsugi.cga.d2.shapes.primitives

import fromcrackpot.kintsugi.cga.Angle
import fromcrackpot.kintsugi.cga.d2.exceptions.FiniteDirectionException
import kotlin.math.atan2

/**
 * A free vector — magnitude and orientation with no position; the [FlatPoint] refinement
 * guaranteed to lie at infinity.
 *
 * The at-infinity flat point `dx·e41 + dy·e42`, its weight stored as exact `0.0` — not merely
 * a value that rounds to zero at the `SCALE` quantum — so even the raw arithmetic of the
 * operations cannot leak a position back in. A free vector doubles as a displacement: its
 * length the distance, its orientation the heading. The refinement adds no data, only the
 * guarantee: a direction and a plain [FlatPoint] carrying the same components are the same
 * value and compare equal, in both directions (see [FlatPoint.equals]). [FlatPoint.toDirection]
 * converts a computed flat point, admitting exactly the points at infinity.
 *
 * @constructor Builds the free vector `(dx, dy)`; the zero direction is legal.
 * @param dx The `e41` component: the vector's x-extent.
 * @param dy The `e42` component: the vector's y-extent.
 */
public class Direction(dx: Double, dy: Double) : FlatPoint(dx, dy, pz = 0.0) {

    /**
     * The heading of this free vector — its raw polar angle, measured counter-clockwise from
     * the positive X direction, in `(−180°, 180°]`.
     *
     * `atan2(py, px)`, raw at full `Double` precision and blind to the vector's length. The
     * negative-X heading answers `+π`, matching [Angle.canonical]'s range; the zero direction
     * answers `0.0`.
     */
    public val angle: Angle get() = Angle(atan2(py, px))

    /**
     * Returns this object rebuilt around the replacement geometry [flatPoint] — the hook the
     * shape-preserving operations return through, re-establishing the at-infinity guarantee.
     *
     * @param flatPoint The replacement geometry — a flat point at infinity.
     * @return The replacement geometry as a direction.
     * @throws FiniteDirectionException when [flatPoint] is finite — its weight does not round
     *   to zero at the `SCALE` quantum.
     */
    override fun copy(flatPoint: FlatPoint): Direction = flatPoint.toDirection
}

/**
 * `Direction + Direction => Direction`<br>
 * Returns the vector sum of the two free vectors: both displacements applied together.
 *
 * Component-wise addition, raw at full `Double` precision. Both operands carry the exact zero
 * weight, so the sum is at infinity by construction — no finite position can leak in. Opposite
 * operands cancel to the zero direction, which is legal.
 *
 * @receiver The left direction.
 * @param other The direction added to this one.
 * @return The component-wise sum, as a [Direction].
 */
public operator fun Direction.plus(other: Direction): Direction =
    Direction(px + other.px, py + other.py)

/**
 * `Direction - Direction => Direction`<br>
 * Returns the vector difference of the two free vectors: this displacement with [other] undone.
 *
 * Component-wise subtraction, raw at full `Double` precision. Both operands carry the exact
 * zero weight, so the difference is at infinity by construction — no finite position can leak
 * in. Equal operands cancel to the zero direction, which is legal.
 *
 * @receiver The left direction.
 * @param other The direction subtracted from this one.
 * @return The component-wise difference, as a [Direction].
 */
public operator fun Direction.minus(other: Direction): Direction =
    Direction(px - other.px, py - other.py)

/**
 * `-Direction => Direction`<br>
 * Returns the opposite free vector: the same length, reversed heading.
 *
 * Component-wise negation, raw at full `Double` precision. The operand carries the exact zero
 * weight, so the result is at infinity by construction — no finite position can leak in. The
 * zero direction is its own negation.
 *
 * @receiver The direction to negate.
 * @return The component-wise negation, as a [Direction].
 */
public operator fun Direction.unaryMinus(): Direction = Direction(-px, -py)
