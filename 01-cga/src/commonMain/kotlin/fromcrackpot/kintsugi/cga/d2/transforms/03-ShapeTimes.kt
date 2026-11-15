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

// The imported per-shape forms outrank this declaration in resolution, so the arms delegate and never recurse.

/**
 * `Shape * Versor => Shape`<br>
 * Returns this shape moved by [versor], for a receiver typed only as [Shape].
 *
 * @receiver The shape to move.
 * @param T The receiver's type, preserved in the result.
 * @param versor The motion to apply.
 * @return The moved shape.
 * @throws DegeneratePolygonException when moving a [Polygon] makes two consecutive vertices round
 *   equal at the `SCALE` quantum.
 * @throws DegeneratePolylineException when moving a [Polyline] makes two consecutive vertices round
 *   equal at the `SCALE` quantum.
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
