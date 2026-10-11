package fromcrackpot.kintsugi.svg

import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polyline
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.scenegraph.*
import fromcrackpot.kintsugi.scenegraph.SceneBounds
import fromcrackpot.kintsugi.scenegraph.nodes.*
import fromcrackpot.kintsugi.scenegraph.nodes.GroupNode
import fromcrackpot.kintsugi.scenegraph.nodes.composites.*
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.*
import fromcrackpot.kintsugi.scenegraph.style.ShapeColor
import fromcrackpot.kintsugi.scenegraph.style.ShapeStrokeCap
import fromcrackpot.kintsugi.scenegraph.style.ShapeStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Bare `<polyline>` ↔ [Polyline] codec tests. `<polyline>` has no tagged reading — the bare
 * element is the untagged default, mirroring bare `<polygon>` → Polygon; an unknown `k:type`
 * warns and falls back. Unlike polygon, the open chain honors `stroke-linecap` (segment's rule).
 */
class PolylineTest {

    /* PARSE */

    @Test fun `bare polyline becomes a Polyline with Y-flipped vertices in source order`() {
        // viewBox 100×100: SVG (20,80)/(60,80)/(20,50) → CGA (−30,−30)/(10,−30)/(−30,0).
        val root = parseSvg(ONE_POLYLINE_SVG)
        val shape = root.children.single() as ShapeNode
        assertEquals(CHAIN, shape.shape)
    }

    @Test fun `points accepts whitespace-only separators`() {
        val shape = parseSvg(BARE_POLYLINE_SVG).children.single() as ShapeNode
        assertEquals(CHAIN, shape.shape)
    }

    @Test fun `unknown k type on polyline warns and falls back to a polyline`() {
        val shape = parseSvg(POLYLINE_UNKNOWN_KTYPE_SVG).children.single() as ShapeNode
        assertEquals(CHAIN, shape.shape)
    }

    @Test fun `two vertices parse - a single open edge is legal`() {
        // Unlike polygon's 3-vertex floor: an open chain needs only 2 vertices.
        val shape = parseSvg(POLYLINE_TWO_VERTICES_SVG).children.single() as ShapeNode
        assertEquals(Polyline(listOf(FlatPoint(-30.0, -30.0), FlatPoint(10.0, -30.0))), shape.shape)
    }

    @Test fun `closed spelling parses - first == last is legal and preserved as data`() {
        val shape = parseSvg(POLYLINE_CLOSED_SPELLING_SVG).children.single() as ShapeNode
        val chain = shape.shape as Polyline
        assertEquals(4, chain.vertices.size)
        assertEquals(chain.vertices.first(), chain.vertices.last())
    }

    /* STYLE */

    @Test fun `polyline with fill stroke and stroke-width parses style`() {
        val shape = parseSvg(ONE_POLYLINE_SVG).children.single() as ShapeNode
        assertEquals(
            ShapeStyle(
                fill = ShapeColor.NONE,
                stroke = ShapeColor.rgb(0x00, 0x00, 0xFF),
                strokeWidth = 2.0,
            ),
            shape.style,
        )
    }

    @Test fun `unstyled polyline materialises SVG defaults - fill black, stroke none, width 1`() {
        val shape = parseSvg(BARE_POLYLINE_SVG).children.single() as ShapeNode
        assertEquals(
            ShapeStyle(fill = ShapeColor.BLACK, stroke = ShapeColor.NONE, strokeWidth = 1.0),
            shape.style,
        )
    }

    @Test fun `stroke-linecap parses onto the style - open chain honors caps`() {
        val shape = parseSvg(POLYLINE_ROUND_CAP_SVG).children.single() as ShapeNode
        assertEquals(ShapeStrokeCap.ROUND, shape.style.strokeCap)
    }

    /* MALFORMED */

    @Test fun `polyline missing points throws MalformedPolylineException`() {
        assertFailsWith<MalformedPolylineException> { parseSvg(POLYLINE_NO_POINTS_SVG) }
    }

    @Test fun `odd coordinate count throws MalformedPolylineException`() {
        val ex = assertFailsWith<MalformedPolylineException> { parseSvg(POLYLINE_ODD_POINTS_SVG) }
        assertTrue(ex.message!!.contains("even"), "unexpected reason: ${ex.message}")
    }

    @Test fun `one vertex throws MalformedPolylineException - wraps the cga degenerate`() {
        assertFailsWith<MalformedPolylineException> { parseSvg(POLYLINE_ONE_VERTEX_SVG) }
    }

    @Test fun `non-numeric coordinate throws MalformedPolylineException`() {
        assertFailsWith<MalformedPolylineException> { parseSvg(POLYLINE_NON_NUMERIC_SVG) }
    }

    /* SERIALIZE */

    @Test fun `polyline serializes as a bare polyline element with core attributes`() {
        val group = GroupNode(
            children = listOf(
                PolylineNode(
                    CHAIN,
                    ShapeStyle(
                        fill = ShapeColor.NONE,
                        stroke = ShapeColor.rgb(0x00, 0x00, 0xFF),
                        strokeWidth = 2.0,
                    ),
                )
            ),
            bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
        )
        val svg = serializeSvg(group)
        assertTrue(
            svg.contains("""<polyline points="20,80 60,80 20,50" fill="none" stroke="#0000ff" stroke-width="2"/>"""),
            "unexpected serialization: $svg",
        )
    }

    @Test fun `non-butt linecap is emitted as the optional-feature attribute`() {
        val group = GroupNode(
            children = listOf(
                PolylineNode(
                    CHAIN,
                    ShapeStyle(
                        stroke = ShapeColor.rgb(0x00, 0x00, 0xFF),
                        strokeWidth = 4.0,
                        strokeCap = ShapeStrokeCap.ROUND,
                    ),
                )
            ),
            bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
        )
        val svg = serializeSvg(group)
        assertTrue(
            svg.contains("""stroke-linecap="round""""),
            "expected stroke-linecap=round, got: $svg",
        )
    }

    /* ROUND TRIP */

    @Test fun `polyline round-trips through serialize and parse`() {
        listOf(ONE_POLYLINE_SVG, POLYLINE_TWO_VERTICES_SVG, POLYLINE_CLOSED_SPELLING_SVG, POLYLINE_ROUND_CAP_SVG)
            .forEach { svg ->
                val group = parseSvg(svg)
                assertEquals(group, parseSvg(serializeSvg(group)), "round-trip broke for fixture:\n$svg")
            }
    }

    companion object {
        /** The fixtures' open chain in CGA coordinates (viewBox 100×100). */
        private val CHAIN = Polyline(
            listOf(FlatPoint(-30.0, -30.0), FlatPoint(10.0, -30.0), FlatPoint(-30.0, 0.0))
        )
    }
}
