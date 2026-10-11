package fromcrackpot.kintsugi.svg

import fromcrackpot.kintsugi.cga.d2.products.join
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Line
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.LineNode
import fromcrackpot.kintsugi.scenegraph.nodes.ShapeNode
import fromcrackpot.kintsugi.scenegraph.style.ShapeColor
import fromcrackpot.kintsugi.scenegraph.style.ShapeStrokeCap
import fromcrackpot.kintsugi.scenegraph.style.ShapeStyle
import kotlin.test.*

/**
 * Parser tests for SVG `<line k:type="line">` (infinite CGA `Line`). The bare/untagged form
 * decodes as a `Segment` — those tests live in [SegmentTest].
 */
class LineTest {

    /* PARSE — tagged line */

    @Test fun `tagged line becomes a Line`() {
        // viewBox 100x100 → corners (0,0) and (100,100) map to CGA (-50, 50) and (50, -50).
        val root = parseSvg(LINE_DIAGONAL_SVG)
        val expected = FlatPoint(-50.0, 50.0) join FlatPoint(50.0, -50.0)
        // SVG defaults applied at parse: stroke=none, stroke-width=1.
        val defaultStyle = ShapeStyle(stroke = ShapeColor.NONE, strokeWidth = 1.0)
        assertEquals(
            listOf(LineNode(Line(expected.gx, expected.gy, expected.gz), defaultStyle)),
            root.children,
        )
    }

    @Test fun `tagged line carries stroke and stroke-width style`() {
        val root = parseSvg(LINE_DIAGONAL_STYLED_SVG)
        val shape = root.children.single() as ShapeNode
        assertEquals(ShapeStyle(stroke = ShapeColor(0xFFFF0000UL), strokeWidth = 2.0), shape.style)
    }

    @Test fun `tagged line carries a square stroke-linecap`() {
        val shape = parseSvg(LINE_DIAGONAL_SQUARECAP_SVG).children.single() as ShapeNode
        assertEquals(
            ShapeStyle(stroke = ShapeColor(0xFFFF0000UL), strokeWidth = 2.0, strokeCap = ShapeStrokeCap.SQUARE),
            shape.style,
        )
    }

    @Test fun `tagged line with endpoints off the worldBorder is still a Line`() {
        // /v1 recognizes Lines by k:type, not geometry — this would have been skipped under the old heuristic.
        val root = parseSvg(LINE_TAGGED_OFFBORDER_SVG)
        assertTrue((root.children.single() as ShapeNode).shape is Line)
    }

    @Test fun `swapping x1y1 with x2y2 produces a sign-flipped Line`() {
        // The format defines orientation as XML-order → join-operand-order. Since the wedge is
        // anti-commutative, swapping (x1,y1) ↔ (x2,y2) must negate every blade component.
        val a = (parseSvg(LINE_DIAGONAL_SVG).children.single() as ShapeNode).shape as Line
        val b = (parseSvg(LINE_DIAGONAL_SWAPPED_SVG).children.single() as ShapeNode).shape as Line

        // Orientation-sensitive equals distinguishes them; canonical (unoriented) form matches.
        assertNotEquals(a, b)
        assertEquals(a.canonical, b.canonical)

        // Explicit anti-commutativity: each component pair sums to zero (sidesteps ±0 on gz).
        assertEquals(0.0, a.gx + b.gx)
        assertEquals(0.0, a.gy + b.gy)
        assertEquals(0.0, a.gz + b.gz)
    }

    /* PARSE — malformed (a tagged line must still validate) */

    @Test fun `tagged line missing any coordinate attribute throws MalformedLineException`() {
        for (missing in listOf("x1", "y1", "x2", "y2")) {
            assertFailsWith<MalformedLineException>("missing $missing must throw") {
                parseSvg(lineSvgWithout(missing, kType = "line"))
            }
        }
    }

    @Test fun `tagged line with coincident endpoints parses as a degenerate Line`() {
        // Letting the math speak: FlatPoint.join(self) yields a degenerate Line; downstream serialize
        // skips it via Line.isAtInfinity. No throw at the parse boundary — a CGA concern, not an SVG one.
        val root = parseSvg(LINE_COINCIDENT_SVG)
        assertEquals(1, root.children.size)
        val line = (root.children.single() as ShapeNode).shape as Line
        assertTrue(line.isAtInfinity, "expected degenerate join to land at infinity, got $line")
    }
}
