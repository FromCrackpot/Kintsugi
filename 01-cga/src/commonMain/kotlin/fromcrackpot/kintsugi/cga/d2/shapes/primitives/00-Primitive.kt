package fromcrackpot.kintsugi.cga.d2.shapes.primitives

import fromcrackpot.kintsugi.cga.d2.shapes.Shape
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Composite

/**
 * Marker for a shape that is itself a CGA element.
 *
 * Implemented by the five 2D blades: [FlatPoint], [Line], [RoundPoint], [Dipole], [Circle].
 * Sealed, so a `when` over a `Primitive` is exhaustive without an `else` — a new blade is a
 * compile error at every dispatch site. [Composite]s, the other kind of [Shape], are built
 * from primitives by composition and carry no blade components of their own.
 */
public sealed interface Primitive : Shape
