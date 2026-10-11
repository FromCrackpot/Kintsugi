package fromcrackpot.kintsugi.cga.d2.shapes.composites

import fromcrackpot.kintsugi.cga.d2.shapes.Shape
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Primitive

/**
 * Marker for a shape built from blades by composition — not itself a CGA element.
 *
 * Implemented by [Segment], [Polyline], and [Polygon]. A composite holds its [Primitive]s — a
 * wrapped blade whose accessors it forwards ([Segment]) or a vertex list with derived edges
 * ([Polyline], [Polygon]) — converting explicitly where a single algebra counterpart exists
 * ([Segment.toDipole]). Sealed, so a `when` over a `Composite` is exhaustive without an
 * `else` — a new composite is a compile error at every dispatch site.
 */
public sealed interface Composite : Shape

