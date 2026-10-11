package fromcrackpot.kintsugi.scenegraph.style

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.*
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polyline
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Segment
import kotlin.test.Test
import kotlin.test.assertEquals

class ShapeStyleTest {

    /* NONE */

    @Test fun `NONE is the all-default no-paint style`() {
        assertEquals(ShapeStyle(), ShapeStyle.NONE)
        assertEquals(ShapeColor.NONE, ShapeStyle.NONE.fill)
        assertEquals(ShapeColor.NONE, ShapeStyle.NONE.stroke)
        assertEquals(0.0, ShapeStyle.NONE.strokeWidth)
        assertEquals(0.0, ShapeStyle.NONE.size)
        assertEquals(ShapeStrokeCap.BUTT, ShapeStyle.NONE.strokeCap)
    }

    /* strokeCap */

    @Test fun `strokeCap defaults to Butt`() {
        // Backward-compatible default: matches today's implicit behavior and SVG's `butt` initial value.
        assertEquals(ShapeStrokeCap.BUTT, ShapeStyle().strokeCap)
        assertEquals(ShapeStrokeCap.BUTT, ShapeStyle.SEGMENT.strokeCap)
        assertEquals(ShapeStrokeCap.BUTT, ShapeStyle.LINE.strokeCap)
    }

    @Test fun `strokeCap is an independent style field`() {
        val rounded = ShapeStyle.SEGMENT.copy(strokeCap = ShapeStrokeCap.ROUND)
        assertEquals(ShapeStrokeCap.ROUND, rounded.strokeCap)
        assertEquals(ShapeStyle.SEGMENT.stroke, rounded.stroke)
        assertEquals(ShapeStyle.SEGMENT.strokeWidth, rounded.strokeWidth)
    }

    /* CTOR */

    @Test fun `partial construction silently drops geometry to zero`() {
        // Documented gotcha: overriding only a paint field leaves the ctor defaults
        // strokeWidth = 0.0 / size = 0.0 — visible color, invisible shape. Callers must
        // include geometry or start from a per-shape constant via .copy().
        val partial = ShapeStyle(fill = ShapeColor.BLACK)
        assertEquals(0.0, partial.strokeWidth)
        assertEquals(0.0, partial.size)
    }

    /* defaultFor */

    @Test fun `flat point default is black filled square`() {
        assertEquals(ShapeStyle(fill = ShapeColor.BLACK, size = 8.0), ShapeStyle.defaultFor(FlatPoint(0.0, 0.0)))
    }

    @Test fun `round point default is black filled circle`() {
        assertEquals(ShapeStyle(fill = ShapeColor.BLACK, size = 8.0), ShapeStyle.defaultFor(RoundPoint(0.0, 0.0)))
    }

    @Test fun `circle default is black stroke with no fill`() {
        assertEquals(
            ShapeStyle(stroke = ShapeColor.BLACK, strokeWidth = 1.0),
            ShapeStyle.defaultFor(Circle(0.0, 0.0, 1.0)),
        )
    }

    @Test fun `line default is black stroke width 1`() {
        assertEquals(
            ShapeStyle(stroke = ShapeColor.BLACK, strokeWidth = 1.0),
            ShapeStyle.defaultFor(Line(0.0, 1.0, 0.0)),
        )
    }

    @Test fun `dipole default is black filled marker size 8`() {
        assertEquals(
            ShapeStyle(fill = ShapeColor.BLACK, size = 8.0),
            ShapeStyle.defaultFor(Dipole(1.0, 0.0, 0.0, 0.0, 0.0, 1.0)),
        )
    }

    @Test fun `segment default is black stroke width 1`() {
        assertEquals(
            ShapeStyle(stroke = ShapeColor.BLACK, strokeWidth = 1.0),
            ShapeStyle.defaultFor(Segment(FlatPoint(0.0, 0.0), FlatPoint(1.0, 0.0))),
        )
    }

    @Test fun `polygon default is black stroke width 1`() {
        assertEquals(
            ShapeStyle(stroke = ShapeColor.BLACK, strokeWidth = 1.0),
            ShapeStyle.defaultFor(Polygon.Triangle(FlatPoint(0.0, 0.0), FlatPoint(2.0, 0.0), FlatPoint(0.0, 1.0))),
        )
    }

    @Test fun `polyline default is black stroke width 1`() {
        assertEquals(
            ShapeStyle(stroke = ShapeColor.BLACK, strokeWidth = 1.0),
            ShapeStyle.defaultFor(Polyline(listOf(FlatPoint(0.0, 0.0), FlatPoint(2.0, 0.0)))),
        )
    }

    /* DipoleChrome */

    @Test fun `DipoleChrome constants are the shared compose-svg contract`() {
        // Single source of truth consumed by both :compose drawDipole and :svg writeDipoleGroup —
        // pinning the values makes a change a deliberate act, not silent drift.
        assertEquals(2.0, DipoleChrome.DASH_ON)
        assertEquals(1.0, DipoleChrome.DASH_OFF)
        assertEquals(0.25, DipoleChrome.WIDTH_RATIO)
    }
}
