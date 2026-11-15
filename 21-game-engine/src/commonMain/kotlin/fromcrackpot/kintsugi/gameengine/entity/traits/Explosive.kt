package fromcrackpot.kintsugi.gameengine.entity.traits

import fromcrackpot.kintsugi.cga.d2.exceptions.DegeneratePolygonException
import fromcrackpot.kintsugi.cga.d2.exceptions.InsideOutPolygonException
import fromcrackpot.kintsugi.cga.d2.exceptions.NonConvexPolygonException
import fromcrackpot.kintsugi.cga.d2.shapes.composites.split
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.gameengine.Step
import fromcrackpot.kintsugi.gameengine.World
import fromcrackpot.kintsugi.gameengine.entity.kinematics.Blast
import fromcrackpot.kintsugi.gameengine.entity.kinematics.Burst
import fromcrackpot.kintsugi.gameengine.entity.kinematics.Debris
import fromcrackpot.kintsugi.scenegraph.nodes.composites.PolygonNode

/**
 * The trait of bursting into fragments on [explode]: its polygon split into [fragmentCount] pieces
 * of equal area.
 *
 * - The pieces fly in the entity's own style; its tags stay behind with it.
 */
public interface Explosive {

    /** The number of pieces the entity bursts into, at least `1`. One piece is the whole outline. */
    public val fragmentCount: Int
}

/**
 * Explodes this entity: its polygon splits into [Explosive.fragmentCount] pieces that fly apart
 * from [at] under [blast], drawn over the scene until the last one is gone.
 *
 * - The entity itself stays where it is: remove it from the world's state as usual.
 * - Callable only while a [Step] is underway and the world is in context, as in its update.
 *
 * @receiver The entity to explode, a classic convex polygon.
 * @param E The entity's type.
 * @param at The point the fragments fly away from, the entity's centroid by default.
 * @param blast The explosion's law and tuning.
 * @throws IllegalArgumentException when [Explosive.fragmentCount] is below 1.
 * @throws InsideOutPolygonException when the entity is inside-out.
 * @throws NonConvexPolygonException when the entity is not convex.
 * @throws DegeneratePolygonException when the entity is too small for its fragments.
 */
context(world: World, _: Step)
public fun <E> E.explode(at: FlatPoint = centroid, blast: Blast = Blast()) where E : PolygonNode, E : Explosive {
    val fragments = split(fragmentCount).map { PolygonNode(it, style) }
    val debris = world.overlay as? Debris ?: Debris()
    world.overlay = Debris(debris.children + Burst(fragments, at, blast))
}
