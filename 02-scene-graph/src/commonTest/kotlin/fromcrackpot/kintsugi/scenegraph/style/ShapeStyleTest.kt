package fromcrackpot.kintsugi.scenegraph.style

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.*
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polyline
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Segment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
        // strokeWidth = 0.0 / size = 0.0: visible color, invisible shape. Callers must
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

    /* dash */

    @Test fun `dash defaults to unset on every per-shape constant`() {
        assertNull(ShapeStyle().dash)
        for (style in listOf(
            ShapeStyle.POINT, ShapeStyle.LINE, ShapeStyle.DIPOLE, ShapeStyle.CIRCLE,
            ShapeStyle.SEGMENT, ShapeStyle.POLYLINE, ShapeStyle.POLYGON,
        )) assertNull(style.dash, "$style")
    }

    @Test fun `dash is part of style equality`() {
        val dashed = ShapeStyle.CIRCLE.copy(dash = ShapeDash(listOf(3.0, 1.0)))
        assertNotEquals(ShapeStyle.CIRCLE, dashed)
        assertNotEquals(ShapeStyle.CIRCLE, ShapeStyle.CIRCLE.copy(dash = ShapeDash.SOLID))
        assertEquals(dashed, ShapeStyle.CIRCLE.copy(dash = ShapeDash(listOf(3.0, 1.0))))
    }

    @Test fun `dashFor - unset dash draws solid on a real shape`() {
        assertNull(ShapeStyle.CIRCLE.dashFor(Circle(0.0, 0.0, 1.0)))
        assertNull(ShapeStyle.LINE.dashFor(Line(0.0, 1.0, 0.0)))
        assertNull(ShapeStyle.POINT.dashFor(RoundPoint(0.0, 0.0)))
    }

    @Test fun `dashFor - unset dash takes the imaginary default on an imaginary shape`() {
        assertEquals(ImaginaryChrome.DASH, ShapeStyle.CIRCLE.dashFor(Circle(0.0, 0.0, -2.0, -1.0)))
        assertEquals(ImaginaryChrome.DASH, ShapeStyle.DIPOLE.dashFor(Dipole(1.0, 0.0, 1.0, 0.0, 0.0, 0.0)))
        assertEquals(ImaginaryChrome.DASH, ShapeStyle.POINT.dashFor(RoundPoint(10.0, 5.0, 1.0, 60.5)))
    }

    @Test fun `dashFor - unset dash takes the connector chrome on a real dipole`() {
        assertEquals(DipoleChrome.DASH, ShapeStyle.DIPOLE.dashFor(Dipole(1.0, 0.0, 0.0, 0.0, 0.0, 1.0)))
    }

    @Test fun `dashFor - an authored dash wins over both defaults`() {
        val authored = ShapeDash(listOf(1.0, 3.0), offset = 0.5)
        val imaginary = Circle(0.0, 0.0, -2.0, -1.0)
        val realDipole = Dipole(1.0, 0.0, 0.0, 0.0, 0.0, 1.0)
        assertEquals(authored, ShapeStyle.CIRCLE.copy(dash = authored).dashFor(imaginary))
        assertEquals(authored, ShapeStyle.DIPOLE.copy(dash = authored).dashFor(realDipole))
        assertEquals(authored, ShapeStyle.LINE.copy(dash = authored).dashFor(Line(0.0, 1.0, 0.0)))
    }

    @Test fun `dashFor - authored SOLID draws solid even when imaginary`() {
        assertNull(ShapeStyle.CIRCLE.copy(dash = ShapeDash.SOLID).dashFor(Circle(0.0, 0.0, -2.0, -1.0)))
        assertNull(ShapeStyle.DIPOLE.copy(dash = ShapeDash.SOLID).dashFor(Dipole(1.0, 0.0, 0.0, 0.0, 0.0, 1.0)))
    }

    /* DipoleChrome */

    @Test fun `DipoleChrome constants are the shared compose-svg contract`() {
        // Single source of truth consumed by both :compose drawDipole and :svg writeDipoleGroup.
        // Pinning the values makes a change a deliberate act, not silent drift.
        assertEquals(listOf(2.0, 1.0), DipoleChrome.DASH.intervals)
        assertEquals(0.0, DipoleChrome.DASH.offset)
        assertEquals(0.25, DipoleChrome.WIDTH_RATIO)
    }

    /* ImaginaryChrome */

    @Test fun `ImaginaryChrome constants are the shared compose-svg contract`() {
        assertEquals(listOf(4.0, 2.0), ImaginaryChrome.DASH.intervals)
        assertEquals(0.0, ImaginaryChrome.DASH.offset)
        assertEquals(0.25, ImaginaryChrome.RING_WIDTH_RATIO)
    }

    @Test fun `ImaginaryChrome applies to the four imaginary round shapes only`() {
        val imaginaryCircle = Circle(0.0, 0.0, -2.0, -1.0)
        val imaginaryDipole = Dipole(1.0, 0.0, 1.0, 0.0, 0.0, 0.0)
        val imaginaryPoint = RoundPoint(10.0, 5.0, 1.0, 60.5)
        assertTrue(ImaginaryChrome.appliesTo(imaginaryCircle))
        assertTrue(ImaginaryChrome.appliesTo(imaginaryDipole))
        assertTrue(ImaginaryChrome.appliesTo(imaginaryDipole.toSegment))
        assertTrue(ImaginaryChrome.appliesTo(imaginaryPoint))

        assertFalse(ImaginaryChrome.appliesTo(Circle(0.0, 0.0, 1.0)))
        assertFalse(ImaginaryChrome.appliesTo(Dipole(1.0, 0.0, 0.0, 0.0, 0.0, 1.0)))
        assertFalse(ImaginaryChrome.appliesTo(Segment(FlatPoint(0.0, 0.0), FlatPoint(1.0, 0.0))))
        assertFalse(ImaginaryChrome.appliesTo(RoundPoint(0.0, 0.0)))
        assertFalse(ImaginaryChrome.appliesTo(FlatPoint(0.0, 0.0)))
        assertFalse(ImaginaryChrome.appliesTo(Line(0.0, 1.0, 0.0)))
    }
}
