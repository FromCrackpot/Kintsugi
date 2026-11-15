package fromcrackpot.kintsugi.cga.d2.shapes.primitives

import fromcrackpot.kintsugi.cga.d2.shapes.Shape
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Composite

/**
 * Marker for a shape that is itself a CGA element: [FlatPoint], [Line], [RoundPoint], [Dipole],
 * and [Circle].
 *
 * - Sealed, so a `when` over a `Primitive` is exhaustive without an `else`.
 */
public sealed interface Primitive : Shape
