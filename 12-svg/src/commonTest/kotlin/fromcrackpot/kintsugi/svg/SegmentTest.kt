package fromcrackpot.kintsugi.svg

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Segment
import fromcrackpot.kintsugi.scenegraph.*
import fromcrackpot.kintsugi.scenegraph.nodes.*
import fromcrackpot.kintsugi.scenegraph.nodes.composites.*
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.*
import fromcrackpot.kintsugi.scenegraph.style.ShapeColor
import fromcrackpot.kintsugi.scenegraph.style.ShapeStrokeCap
import fromcrackpot.kintsugi.scenegraph.style.ShapeStyle
import kotlin.test.*

/**
 * Parser tests for the bare `<line>` SVG element — the untagged default for `Segment` in /v1.
 * The infinite-line counterpart (`<line k:type="line">`) is covered by [LineTest].
 */
class SegmentTest {

    /* PARSE — bare segment */

    @Test fun `bare line becomes a Segment`() {
        // viewBox 100x100 → SVG (20,20) and (80,80) map to CGA (-30, 30) and (30, -30).
        val root = parseSvg(LINE_SEGMENT_SVG)
        val expected = Segment(FlatPoint(-30.0, 30.0), FlatPoint(30.0, -30.0))
        // SVG defaults applied at parse: stroke=none, stroke-width=1.
        val defaultStyle = ShapeStyle(stroke = ShapeColor.NONE, strokeWidth = 1.0)
        assertEquals(
            listOf(SegmentNode(expected, defaultStyle)),
            root.children,
        )
    }

    @Test fun `bare line carries stroke and stroke-width style`() {
        val root = parseSvg(LINE_SEGMENT_STYLED_SVG)
        val shape = root.children.single() as ShapeNode
        assertEquals(ShapeStyle(stroke = ShapeColor(0xFFFF0000UL), strokeWidth = 2.0), shape.style)
    }

    @Test fun `bare line without stroke-linecap defaults to Butt`() {
        val shape = parseSvg(LINE_SEGMENT_STYLED_SVG).children.single() as ShapeNode
        assertEquals(ShapeStrokeCap.BUTT, shape.style.strokeCap)
    }

    @Test fun `bare line carries a round stroke-linecap`() {
        val shape = parseSvg(LINE_SEGMENT_ROUNDCAP_SVG).children.single() as ShapeNode
        assertEquals(
            ShapeStyle(stroke = ShapeColor(0xFFFF0000UL), strokeWidth = 2.0, strokeCap = ShapeStrokeCap.ROUND),
            shape.style,
        )
    }

    @Test fun `stroke-linecap cascades from an enclosing group`() {
        // <g stroke-linecap="round"> reaches a bare segment with no cap of its own — same
        // per-attribute fallback chain as stroke/stroke-width.
        val group = parseSvg(GROUP_STROKE_LINECAP_CASCADE_SVG).children.single() as GroupNode
        val shape = group.children.single() as ShapeNode
        assertEquals(ShapeStrokeCap.ROUND, shape.style.strokeCap)
    }

    @Test fun `line with unknown k type falls back to a Segment`() {
        val root = parseSvg(LINE_UNKNOWN_KTYPE_SVG)
        val shape = root.children.single() as ShapeNode
        assertTrue(shape.shape is Segment, "expected Segment, got ${shape.shape::class.simpleName}")
    }

    @Test fun `swapping x1y1 with x2y2 produces a sign-flipped Segment`() {
        // Orientation contract: XML endpoint order → start ∧ end. The wedge is anti-commutative
        // at the underlying Dipole level, so swapped endpoints negate every blade component.
        val a = (parseSvg(LINE_SEGMENT_SVG).children.single() as ShapeNode).shape as Segment
        val b = (parseSvg(LINE_SEGMENT_SWAPPED_SVG).children.single() as ShapeNode).shape as Segment

        assertNotEquals(a, b)
        assertEquals(a.canonical, b.canonical)

        assertEquals(0.0, a.gx + b.gx)
        assertEquals(0.0, a.gy + b.gy)
        assertEquals(0.0, a.gz + b.gz)
    }

    /* PARSE — malformed */

    @Test fun `bare line missing any coordinate attribute throws MalformedSegmentException`() {
        for (missing in listOf("x1", "y1", "x2", "y2")) {
            assertFailsWith<MalformedSegmentException>("missing $missing must throw") {
                parseSvg(lineSvgWithout(missing, kType = null))
            }
        }
    }

    @Test fun `bare line with coincident endpoints throws MalformedSegmentException`() {
        // :cga's Dipole ctor throws DegenerateDipoleException when the carrier norm collapses to
        // zero; the SVG codec wraps it so :svg's parse boundary only ever surfaces SvgFormatExceptions.
        assertFailsWith<MalformedSegmentException> { parseSvg(LINE_SEGMENT_COINCIDENT_SVG) }
    }
}
