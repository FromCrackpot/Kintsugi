package fromcrackpot.kintsugi.gameengine.dynamics

import fromcrackpot.kintsugi.cga.d2.exceptions.DegeneratePolygonException
import fromcrackpot.kintsugi.cga.d2.exceptions.DegeneratePolylineException
import fromcrackpot.kintsugi.cga.d2.products.dot
import fromcrackpot.kintsugi.cga.d2.products.times
import fromcrackpot.kintsugi.cga.d2.queries.Penetration
import fromcrackpot.kintsugi.cga.d2.shapes.Shape
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Direction
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.minus
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.plus
import fromcrackpot.kintsugi.cga.d2.transforms.F
import fromcrackpot.kintsugi.cga.d2.transforms.T
import fromcrackpot.kintsugi.cga.d2.transforms.primitives.times
import fromcrackpot.kintsugi.cga.d2.transforms.times
import fromcrackpot.kintsugi.gameengine.traits.Kinetic

/**
 * One bounce: the dynamics resolving a [Penetration] as a perfectly elastic reflection off the
 * violated edge, relative to that edge's own motion.
 *
 * Applied with `*` like any dynamics — `entity *= bounce(penetration)` — carrying the
 * penetration it resolves and the velocity of the collider whose edge was violated. The
 * default is the zero direction: a static collider.
 */
public class Bounce(
    /** The resolved penetration — the violated edge, the depth past it, and the way out. */
    public val penetration: Penetration,
    /**
     * The violated collider's velocity, in model units per second; the zero direction — a
     * static collider — when omitted.
     */
    public val colliderVelocity: Direction = Direction(0.0, 0.0),
)

/**
 * Returns the [Bounce] resolving [penetration] against a collider moving at [colliderVelocity].
 *
 * The lowercase verb is the dynamics tier's call-site spelling; the [Bounce] constructor is
 * equivalent.
 *
 * @param penetration The penetration the bounce resolves.
 * @param colliderVelocity The collider's velocity; defaults to the zero direction — a static
 *   collider.
 * @return The [Bounce] holding [penetration] and [colliderVelocity].
 */
public fun bounce(penetration: Penetration, colliderVelocity: Direction = Direction(0.0, 0.0)): Bounce =
    Bounce(penetration, colliderVelocity)

/**
 * `Shape * Bounce => Shape`<br>
 * Returns this entity bounced off the violated edge: velocity reflected when the entity
 * approaches the edge, geometry landed clear of the penetration either way.
 *
 * The receiver resolves [bounce]'s penetration relative to the collider's motion. Approach is
 * decided raw: the entity reflects only when its velocity relative to [Bounce.colliderVelocity]
 * runs against [Penetration.normal] — `(velocity - colliderVelocity) dot normal < 0`. On approach, the
 * geometry translates by twice [Penetration.depth] along the normal — the penetration happened
 * one depth ago along the normal and the reflected continuation lands one depth clear — and
 * the [Kinetic.velocity] of the result is the relative velocity mirrored across the violated
 * edge's carrier with the collider's velocity restored: the normal component of the relative
 * velocity flips, the tangential component rides through. Against the default zero-direction
 * collider this is exactly the world-frame reflection off a static edge.
 *
 * A receding or grazing contact — relative velocity with no component against the normal —
 * reflects nothing: the geometry translates by one [Penetration.depth] along the normal,
 * resolving the penetration, and the velocity rides through untouched. The relative gate is
 * what keeps the dynamics sound against a moving collider: motion of the collider manufactures
 * fresh contacts step after step, and each one resolves as a push — never as a second
 * reflection of an entity already moving away. The receiver itself is untouched in both lanes:
 * the velocity is written only on the returned copy, so a binding captured before the bounce
 * keeps its pre-bounce velocity, and the result is the receiver's own class through its `copy`
 * hook. `entity *= bounce(penetration)` spells the bounce — the rebind replaces the entity
 * with its bounced value.
 *
 * @receiver The entity to bounce — a [Shape] carrying the [Kinetic] trait.
 * @param E The receiver's type, preserved in the result.
 * @param bounce The bounce to apply.
 * @return The bounced entity, as the receiver's class.
 * @throws DegeneratePolygonException when the landing lands two consecutive vertices of a
 *   polygon receiver rounded-coincident at the `SCALE` quantum.
 * @throws DegeneratePolylineException when the landing lands two consecutive vertices of a
 *   polyline receiver rounded-coincident at the `SCALE` quantum.
 */
public operator fun <E> E.times(bounce: Bounce): E where E : Shape, E : Kinetic {
    val penetration = bounce.penetration
    val relative = velocity - bounce.colliderVelocity
    return if (relative dot penetration.normal < 0.0) {
        (// Corrected position: where the rigid bounce without penetration would put it
                this * T(penetration.normal * (2.0 * penetration.depth)))
            .also { // Relative velocity mirrored across the penetrated edge, collider velocity restored
                it.velocity = relative * F(penetration.edge.carrier) + bounce.colliderVelocity }
    } else {
        // Receding contact: resolve the penetration only — a push, not a reflection
        this * T(penetration.normal * penetration.depth)
    }
}
