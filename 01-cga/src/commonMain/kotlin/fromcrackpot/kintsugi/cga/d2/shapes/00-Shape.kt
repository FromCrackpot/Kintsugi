package fromcrackpot.kintsugi.cga.d2.shapes

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Primitive
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Composite

/**
 * The umbrella marker for every 2D shape, [Primitive] and [Composite] alike.
 *
 * - Not sealed: exhaustive dispatch splits on [Primitive] and [Composite] and nests a `when` over each.
 */
public interface Shape
