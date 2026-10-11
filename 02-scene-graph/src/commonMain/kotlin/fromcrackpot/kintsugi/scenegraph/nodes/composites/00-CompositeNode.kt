package fromcrackpot.kintsugi.scenegraph.nodes.composites

import fromcrackpot.kintsugi.cga.d2.shapes.composites.Composite
import fromcrackpot.kintsugi.scenegraph.nodes.ShapeNode

/**
 * Marker for a leaf decorating a shape built from primitives.
 *
 * Implemented by [SegmentNode], [PolylineNode], and [PolygonNode]. Sealed, so a `when` over
 * a `CompositeNode` is exhaustive without an `else` — a new composite leaf is a compile
 * error at every dispatch site. The marker parallels the shapes' own [Composite] without
 * being related to it — the two hierarchies can't share a supertype, but every
 * `CompositeNode` instance already is a [Composite] through the shape it subclasses.
 */
public sealed interface CompositeNode : ShapeNode
