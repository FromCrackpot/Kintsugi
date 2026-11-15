package fromcrackpot.kintsugi.gameengine.entity.kinetics

import fromcrackpot.kintsugi.cga.d2.exceptions.DegeneratePolygonException
import fromcrackpot.kintsugi.cga.d2.exceptions.DegeneratePolylineException
import fromcrackpot.kintsugi.cga.d2.operations.products.dot
import fromcrackpot.kintsugi.cga.d2.operations.products.times
import fromcrackpot.kintsugi.cga.d2.operations.queries.Penetration
import fromcrackpot.kintsugi.cga.d2.shapes.Shape
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Direction
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.minus
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.plus
import fromcrackpot.kintsugi.cga.d2.transforms.F
import fromcrackpot.kintsugi.cga.d2.transforms.T
import fromcrackpot.kintsugi.cga.d2.transforms.primitives.times
import fromcrackpot.kintsugi.cga.d2.transforms.times
import fromcrackpot.kintsugi.gameengine.entity.traits.Inertial

/**
 * One bounce: the kinetics resolving a [Penetration] as a perfectly elastic reflection off the
 * violated edge, relative to that edge's own motion.
 *
 * - Applied with `*`: `entity *= bounce(penetration)`. The collider is static unless given a velocity.
 */
public class Bounce(
    /** The resolved penetration: the violated edge, the depth past it, and the way out. */
    public val penetration: Penetration,
    /** The violated collider's velocity, in model units per second. Zero, a static collider, when omitted. */
    public val colliderVelocity: Direction = Direction(0.0, 0.0),
)

/**
 * Returns the [Bounce] resolving [penetration] against a collider moving at [colliderVelocity].
 *
 * @param penetration The penetration the bounce resolves.
 * @param colliderVelocity The collider's velocity. Zero, a static collider, by default.
 * @return The bounce.
 */
public fun bounce(penetration: Penetration, colliderVelocity: Direction = Direction(0.0, 0.0)): Bounce =
    Bounce(penetration, colliderVelocity)

/**
 * `Shape * Bounce => Shape`<br>
 * Returns this entity bounced off the violated edge. Its velocity is reflected when it approaches
 * the edge, and its geometry lands clear of the penetration either way.
 *
 * - Approach is decided relative to the collider: `(velocity - colliderVelocity) dot normal < 0`.
 * - A receding or grazing entity is only pushed clear, with its velocity untouched. It is never reflected twice.
 * - The receiver is untouched: only the returned copy carries the new velocity.
 *
 * @receiver The entity to bounce, a shape carrying the [Inertial] trait.
 * @param E The receiver's type, preserved in the result.
 * @param bounce The bounce to apply.
 * @return The bounced entity.
 * @throws DegeneratePolygonException when the landing makes two consecutive vertices of a
 *   polygon receiver round equal at the `SCALE` quantum.
 * @throws DegeneratePolylineException when the landing makes two consecutive vertices of a
 *   polyline receiver round equal at the `SCALE` quantum.
 */
public operator fun <E> E.times(bounce: Bounce): E where E : Shape, E : Inertial {
    val penetration = bounce.penetration
    val relative = velocity - bounce.colliderVelocity
    return if (relative dot penetration.normal < 0.0) {
        (// Corrected position: where the rigid bounce without penetration would put it
                this * T(penetration.normal * (2.0 * penetration.depth)))
            .also { // Relative velocity mirrored across the penetrated edge, collider velocity restored
                it.velocity = relative * F(penetration.edge.carrier) + bounce.colliderVelocity }
    } else {
        // Receding contact: resolve the penetration only, as a push, not a reflection
        this * T(penetration.normal * penetration.depth)
    }
}
