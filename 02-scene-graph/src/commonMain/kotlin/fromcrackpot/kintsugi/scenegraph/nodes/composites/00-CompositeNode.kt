package fromcrackpot.kintsugi.scenegraph.nodes.composites

import fromcrackpot.kintsugi.cga.d2.shapes.composites.Composite
import fromcrackpot.kintsugi.scenegraph.nodes.ShapeNode

/**
 * Marker for a leaf decorating a shape built from blades by composition: [SegmentNode],
 * [PolylineNode], and [PolygonNode].
 *
 * - Sealed, so a `when` over a `CompositeNode` is exhaustive without an `else`.
 * - Parallel to [Composite], not a subtype. Yet every instance is a [Composite] through its shape.
 */
public sealed interface CompositeNode : ShapeNode
