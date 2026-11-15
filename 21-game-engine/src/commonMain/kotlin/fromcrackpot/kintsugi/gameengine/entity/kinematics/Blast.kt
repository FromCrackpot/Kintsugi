package fromcrackpot.kintsugi.gameengine.entity.kinematics

import fromcrackpot.kintsugi.cga.d2.operations.queries.measures.distanceTo
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.transforms.D
import fromcrackpot.kintsugi.cga.d2.transforms.composites.times
import fromcrackpot.kintsugi.cga.d2.transforms.times
import fromcrackpot.kintsugi.cga.round
import fromcrackpot.kintsugi.gameengine.Step
import fromcrackpot.kintsugi.gameengine.entity.GroupEntity
import fromcrackpot.kintsugi.gameengine.entity.traits.Kinematic
import fromcrackpot.kintsugi.scenegraph.nodes.Node
import fromcrackpot.kintsugi.scenegraph.nodes.composites.PolygonNode
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sqrt

/**
 * The law of an explosion: fragments fly away from the burst's origin, fast at first and slowing to
 * rest, each shrinking with the distance it travels and with time until it is dropped.
 *
 * - A fragment's distance from the origin is scaled, so the farther ones fly faster and end up smaller.
 * - Every fragment is dropped in finite time, one sitting on the origin included.
 *
 * @constructor Builds the blast from its tuning.
 * @throws IllegalArgumentException when [burstRate] is negative, or another parameter is not positive.
 */
public class Blast(
    /** How fast the fragments' distances from the origin start growing, per second. */
    public val burstRate: Double = 20.0,
    /** How fast that growth dies away, per second: distances settle at `1 + burstRate / drag` times their start. */
    public val drag: Double = 1.0,
    /** The travel over which a fragment's size falls by a factor of `e`, travel alone. */
    public val erodeLength: Double = 40.0,
    /** The time over which a fragment's size falls by a factor of `e`, age alone, in seconds. */
    public val erodeTime: Double = 0.5,
    /** The size at or below which a fragment is dropped. A fragment's size is the square root of its area. */
    public val minSize: Double = 0.5,
) : Kinematics {

    init {
        require(burstRate >= 0.0) { "burstRate must not be negative, was $burstRate" }
        require(drag > 0.0) { "drag must be positive, was $drag" }
        require(erodeLength > 0.0) { "erodeLength must be positive, was $erodeLength" }
        require(erodeTime > 0.0) { "erodeTime must be positive, was $erodeTime" }
        require(round(minSize) > 0.0) { "minSize must be positive at the SCALE quantum, was $minSize" }
    }

    context(step: Step)
    override fun <E : Node> advance(node: E): E {
        // An explicit cast, not a smart cast to an intersection: that one miscompiles on wasmJs (invalid module).
        val burst = node as? Burst ?: return node
        if (step.dt == 0.0 || burst.children.isEmpty()) return node
        val age = burst.age + step.dt
        // σ(t + dt)/σ(t) telescopes, and so does each step's travel (ratio − 1)·r = (σ(t + dt) − σ(t))·r₀
        val ratio = spread(age) / spread(burst.age)
        val origin = burst.origin
        val fragments = burst.children.mapNotNull { fragment ->
            val center = fragment.centroid
            val erosion = exp(-(ratio - 1) * (center distanceTo origin) / erodeLength - step.dt / erodeTime)
            // Cull on the predicted size: building a polygon shrunk past the quantum would throw
            if (sqrt(abs(fragment.signedArea)) * erosion <= minSize) null
            else fragment * (D(center, erosion / ratio) * D(origin, ratio))
        }
        @Suppress("UNCHECKED_CAST")
        return Burst(fragments, origin, this, age) as E
    }

    private fun spread(age: Double): Double = 1 + burstRate / drag * (1 - exp(-drag * age))
}

internal class Burst(
    fragments: List<PolygonNode>,
    val origin: FlatPoint,
    val blast: Blast,
    val age: Double = 0.0,
) : GroupEntity<PolygonNode>(fragments), Kinematic {
    override val law: Kinematics get() = blast
    override fun rebuild(children: List<PolygonNode>): Burst = Burst(children, origin, blast, age)
}

internal class Debris(bursts: List<Burst> = emptyList()) : GroupEntity<Burst>(bursts), Kinematic {
    override val law: Kinematics get() = DropSpent
    override fun rebuild(children: List<Burst>): Debris = Debris(children)
}

private object DropSpent : Kinematics {
    context(step: Step)
    override fun <E : Node> advance(node: E): E {
        val debris = node as? Debris ?: return node
        if (debris.children.all { it.children.isNotEmpty() }) return node
        @Suppress("UNCHECKED_CAST")
        return Debris(debris.children.filter { it.children.isNotEmpty() }) as E
    }
}
