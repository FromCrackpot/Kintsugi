package fromcrackpot.kintsugi.scenegraph.nodes.primitives

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Primitive
import fromcrackpot.kintsugi.scenegraph.nodes.ShapeNode

/**
 * Marker for a leaf decorating a CGA element.
 *
 * Implemented by the five blade leaves: [FlatPointNode], [LineNode], [RoundPointNode],
 * [DipoleNode], [CircleNode]. Sealed, so a `when` over a `PrimitiveNode` is exhaustive
 * without an `else` — a new blade leaf is a compile error at every dispatch site. The
 * marker parallels the shapes' own [Primitive] without being related to it — the two
 * hierarchies can't share a supertype, but every `PrimitiveNode` instance already is a
 * [Primitive] through the shape it subclasses.
 */
public sealed interface PrimitiveNode : ShapeNode
