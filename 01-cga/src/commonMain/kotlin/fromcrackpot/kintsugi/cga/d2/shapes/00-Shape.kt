package fromcrackpot.kintsugi.cga.d2.shapes

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Primitive
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Composite

/**
 * The umbrella marker for every 2D shape, [Primitive] and [Composite] alike.
 *
 * Every shape is either a [Primitive] — itself a CGA element — or a [Composite] built from
 * primitives. `Shape` itself is not sealed, so a `when` over it is never exhaustive; the two
 * kinds are, so exhaustive dispatch splits on them and nests a `when` over [Primitive] and
 * one over [Composite].
 */
public interface Shape
