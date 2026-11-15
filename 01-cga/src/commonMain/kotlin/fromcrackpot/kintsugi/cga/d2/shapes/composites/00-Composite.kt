package fromcrackpot.kintsugi.cga.d2.shapes.composites

import fromcrackpot.kintsugi.cga.d2.shapes.Shape
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Primitive

/**
 * Marker for a shape built from blades by composition: [Segment], [Polyline] and [Polygon]. It is
 * not itself a CGA element.
 *
 * - Sealed, so a `when` over a `Composite` is exhaustive without an `else`.
 */
public sealed interface Composite : Shape

