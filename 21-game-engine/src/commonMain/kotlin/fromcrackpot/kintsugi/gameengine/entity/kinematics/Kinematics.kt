package fromcrackpot.kintsugi.gameengine.entity.kinematics

import fromcrackpot.kintsugi.gameengine.Step
import fromcrackpot.kintsugi.gameengine.entity.traits.Kinematic
import fromcrackpot.kintsugi.scenegraph.nodes.Node

/**
 * A law of motion over time: the engine advances a node by it once per step.
 *
 * - An entity reaches its law through its [Kinematic] trait's [Kinematic.law].
 * - A node at rest must come back as itself, so a world at rest draws nothing new.
 */
public interface Kinematics {

    /**
     * Returns [node] advanced by the current step under this law, or [node] itself when it doesn't move.
     *
     * - Callable only while a [Step] is underway.
     *
     * @param E The node's type, preserved in the result.
     * @param node The node to advance; a node the law doesn't apply to comes back as itself.
     * @return The advanced node, or [node] itself.
     */
    context(step: Step)
    public fun <E : Node> advance(node: E): E
}
