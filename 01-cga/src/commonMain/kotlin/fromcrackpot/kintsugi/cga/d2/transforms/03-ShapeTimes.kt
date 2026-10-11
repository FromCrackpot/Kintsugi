package fromcrackpot.kintsugi.cga.d2.transforms

import fromcrackpot.kintsugi.cga.d2.exceptions.DegeneratePolygonException
import fromcrackpot.kintsugi.cga.d2.exceptions.DegeneratePolylineException
import fromcrackpot.kintsugi.cga.d2.shapes.Shape
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Composite
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polyline
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Segment
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Circle
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Dipole
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Line
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Primitive
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.RoundPoint
import fromcrackpot.kintsugi.cga.d2.transforms.composites.times
import fromcrackpot.kintsugi.cga.d2.transforms.primitives.times

// The imported per-shape forms outrank this declaration in resolution — the arms delegate, never recurse.

/**
 * `Shape * Versor => Shape`<br>
 * Returns this shape moved by [versor].
 *
 * The action for a receiver typed only as [Shape]: it dispatches on the shape's kind to that
 * shape's own `* Versor` form, so one motion spelling covers every shape; a receiver with a
 * concrete static type resolves to the per-shape forms directly. The result is the receiver's
 * own class: the moved geometry returns through the shape's `copy` hook, preserving a subclass
 * that overrides it.
 *
 * @receiver The [Shape] to move.
 * @param T The receiver's type, preserved in the result.
 * @param versor The motion to apply.
 * @return The moved shape, as the receiver's class.
 * @throws DegeneratePolygonException when moving a [Polygon] lands two consecutive vertices
 *   rounded-coincident at the `SCALE` quantum.
 * @throws DegeneratePolylineException when moving a [Polyline] lands two consecutive vertices
 *   rounded-coincident at the `SCALE` quantum.
 */
public operator fun <T : Shape> T.times(versor: Versor): T = when (this) {
    is Primitive -> when (this) {
        is FlatPoint -> this * versor
        is RoundPoint -> this * versor
        is Line -> this * versor
        is Circle -> this * versor
        is Dipole -> this * versor
    }
    is Composite -> when (this) {
        is Segment -> this * versor
        is Polygon -> this * versor
        is Polyline -> this * versor
    }
    else -> error("Unknown Shape kind: ${this::class.simpleName}")
}
