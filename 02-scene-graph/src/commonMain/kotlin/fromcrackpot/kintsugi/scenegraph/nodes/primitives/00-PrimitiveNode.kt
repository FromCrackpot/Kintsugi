package fromcrackpot.kintsugi.scenegraph.nodes.primitives

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Primitive
import fromcrackpot.kintsugi.scenegraph.nodes.ShapeNode

/**
 * Marker for a leaf decorating a CGA element: [FlatPointNode], [LineNode], [RoundPointNode],
 * [DipoleNode], and [CircleNode].
 *
 * - Sealed, so a `when` over a `PrimitiveNode` is exhaustive without an `else`.
 * - Parallel to [Primitive], not a subtype. Yet every instance is a [Primitive] through its shape.
 */
public sealed interface PrimitiveNode : ShapeNode
