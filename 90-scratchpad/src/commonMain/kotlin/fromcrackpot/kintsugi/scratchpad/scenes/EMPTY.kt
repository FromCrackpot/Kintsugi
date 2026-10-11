package fromcrackpot.kintsugi.scratchpad.scenes

import fromcrackpot.kintsugi.scenegraph.SceneBounds
import fromcrackpot.kintsugi.scenegraph.nodes.GroupNode

/** Scene half-extent — frames the floor 480×480 around the CGA origin, like the smiley. */
private const val EXTENT = 240.0

/**
 * Empty prototyping scene: bounds only, zero shapes — a blank slate for the next experiment.
 * The breakout content that used to live here graduated into `:brickit`.
 */
fun emptyScene(): GroupNode = GroupNode(
    children = emptyList(),
    bounds = SceneBounds(-EXTENT, -EXTENT, 2 * EXTENT, 2 * EXTENT),
)
