package fromcrackpot.kintsugi.scenegraph.nodes

/**
 * A node of the renderer-neutral scene graph — a sealed marker with exactly two kinds:
 * [ShapeNode], a styled leaf that is itself a CGA shape, and [GroupNode], an ordered
 * sub-tree.
 */
public sealed interface Node
