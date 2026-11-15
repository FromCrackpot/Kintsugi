package fromcrackpot.kintsugi.gameengine.entity.kinematics

import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.transforms.R
import fromcrackpot.kintsugi.cga.d2.transforms.composites.times
import fromcrackpot.kintsugi.cga.rad
import fromcrackpot.kintsugi.cga.round
import fromcrackpot.kintsugi.gameengine.Step
import fromcrackpot.kintsugi.gameengine.entity.traits.Tilting
import fromcrackpot.kintsugi.scenegraph.nodes.Node
import kotlin.math.abs

/**
 * The law behind [Tilting]: turns the entity about its centroid toward its target at its rate,
 * then back to rest.
 *
 * - Exact however the steps split: arriving mid-step, it spends the rest of the step turning back.
 */
public object TiltServo : Kinematics {

    context(step: Step)
    override fun <E : Node> advance(node: E): E {
        // Explicit casts, not a smart cast to `E & Tilting & Polygon`: that one miscompiles on wasmJs (invalid module).
        val tilting = node as? Tilting ?: return node
        val polygon = node as? Polygon ?: return node
        val armed = round(tilting.tiltTarget) != 0.rad
        var budget = tilting.tiltRate.radians * step.dt
        val from = polygon.edges[0].attitude.angle

        val goal = if (armed) tilting.tiltTarget.radians else 0.0
        val gap = (goal.rad - from).canonical.radians
        var turn = gap.coerceIn(-budget, budget)
        budget -= abs(turn)
        val arrived = armed && round(gap - turn) == 0.0
        if (arrived) turn += (-(from + turn.rad)).canonical.radians.coerceIn(-budget, budget)

        if (!arrived && round(turn) == 0.0) return node
        val turned = polygon * R(polygon.centroid, turn.rad)
        if (arrived) (turned as Tilting).tiltTarget = 0.rad
        // `*` keeps the runtime class through the copy hook, class-checked by its narrowed.
        @Suppress("UNCHECKED_CAST")
        return turned as E
    }
}
