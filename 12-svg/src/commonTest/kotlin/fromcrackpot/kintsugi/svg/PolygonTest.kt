package fromcrackpot.kintsugi.svg

import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.composites.unaryMinus
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.scenegraph.*
import fromcrackpot.kintsugi.scenegraph.SceneBounds
import fromcrackpot.kintsugi.scenegraph.nodes.*
import fromcrackpot.kintsugi.scenegraph.nodes.GroupNode
import fromcrackpot.kintsugi.scenegraph.nodes.composites.*
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.*
import fromcrackpot.kintsugi.scenegraph.style.ShapeColor
import fromcrackpot.kintsugi.scenegraph.style.ShapeStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Bare `<polygon>` ↔ [Polygon] codec tests. `<polygon>` has no tagged reading — the bare element
 * is the untagged default, mirroring bare `<rect>` → rectangle Polygon; an unknown `k:type` warns
 * and falls back.
 */
class PolygonTest {

    /* PARSE */

    @Test fun `bare polygon becomes a Polygon with Y-flipped vertices in source order`() {
        // viewBox 100×100: SVG (20,80)/(60,80)/(20,50) → CGA (−30,−30)/(10,−30)/(−30,0).
        val root = parseSvg(ONE_POLYGON_SVG)
        val shape = root.children.single() as ShapeNode
        assertEquals(TRIANGLE, shape.shape)
    }

    @Test fun `points accepts whitespace-only separators`() {
        val shape = parseSvg(BARE_POLYGON_SVG).children.single() as ShapeNode
        assertEquals(TRIANGLE, shape.shape)
    }

    @Test fun `unknown k type on polygon warns and falls back to a polygon`() {
        val shape = parseSvg(POLYGON_UNKNOWN_KTYPE_SVG).children.single() as ShapeNode
        assertEquals(TRIANGLE, shape.shape)
    }

    /* STYLE */

    @Test fun `polygon with fill stroke and stroke-width parses style`() {
        val shape = parseSvg(ONE_POLYGON_SVG).children.single() as ShapeNode
        assertEquals(
            ShapeStyle(
                fill = ShapeColor.rgb(0xFF, 0x00, 0x00),
                stroke = ShapeColor.rgb(0x00, 0x00, 0xFF),
                strokeWidth = 2.0,
            ),
            shape.style,
        )
    }

    @Test fun `unstyled polygon materialises SVG defaults - fill black, stroke none, width 1`() {
        val shape = parseSvg(BARE_POLYGON_SVG).children.single() as ShapeNode
        assertEquals(
            ShapeStyle(fill = ShapeColor.BLACK, stroke = ShapeColor.NONE, strokeWidth = 1.0),
            shape.style,
        )
    }

    /* MALFORMED */

    @Test fun `polygon missing points throws MalformedPolygonException`() {
        assertFailsWith<MalformedPolygonException> { parseSvg(POLYGON_NO_POINTS_SVG) }
    }

    @Test fun `odd coordinate count throws MalformedPolygonException`() {
        val ex = assertFailsWith<MalformedPolygonException> { parseSvg(POLYGON_ODD_POINTS_SVG) }
        assertTrue(ex.message!!.contains("even"), "unexpected reason: ${ex.message}")
    }

    @Test fun `two vertices throw MalformedPolygonException - wraps the cga degenerate`() {
        assertFailsWith<MalformedPolygonException> { parseSvg(POLYGON_TWO_VERTICES_SVG) }
    }

    @Test fun `non-numeric coordinate throws MalformedPolygonException`() {
        assertFailsWith<MalformedPolygonException> { parseSvg(POLYGON_NON_NUMERIC_SVG) }
    }

    /* SERIALIZE */

    @Test fun `polygon serializes as a bare polygon element with core attributes`() {
        val group = GroupNode(
            children = listOf(
                PolygonNode(
                    TRIANGLE,
                    ShapeStyle(
                        fill = ShapeColor.rgb(0xFF, 0x00, 0x00),
                        stroke = ShapeColor.rgb(0x00, 0x00, 0xFF),
                        strokeWidth = 2.0,
                    ),
                )
            ),
            bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
        )
        val svg = serializeSvg(group)
        assertTrue(
            svg.contains("""<polygon points="20,80 60,80 20,50" fill="#ff0000" stroke="#0000ff" stroke-width="2"/>"""),
            "unexpected serialization: $svg",
        )
    }

    /* ROUND TRIP */

    @Test fun `polygon round-trips through serialize and parse`() {
        val group = parseSvg(ONE_POLYGON_SVG)
        assertEquals(group, parseSvg(serializeSvg(group)))
    }

    /* ORIENTATION (k:flags="inside-out") */

    @Test fun `inside-out flag parses to an inside-out polygon in authored order`() {
        val shape = parseSvg(INSIDE_OUT_POLYGON_SVG).children.single() as ShapeNode
        assertEquals(WALL_TRIANGLE, shape.shape)
    }

    @Test fun `inside-out flag reverses a counter-clockwise walk - flag is intent, winding is truth`() {
        val shape = parseSvg(MISWOUND_INSIDE_OUT_POLYGON_SVG).children.single() as ShapeNode
        assertEquals(WALL_TRIANGLE, shape.shape)
    }

    @Test fun `unflagged clockwise walk is canonicalized to classic`() {
        val shape = parseSvg(MISWOUND_CLASSIC_POLYGON_SVG).children.single() as ShapeNode
        assertEquals(TRIANGLE, shape.shape)
    }

    @Test fun `inside-out rect parses to an inside-out rectangle polygon`() {
        val shape = parseSvg(INSIDE_OUT_RECT_SVG).children.single() as ShapeNode
        assertEquals(-Polygon.Rectangle(FlatPoint(-20.0, -10.0), FlatPoint(20.0, 10.0)), shape.shape)
    }

    @Test fun `inside-out polygon serializes with k flags inside-out`() {
        val group = GroupNode(
            children = listOf(PolygonNode(WALL_TRIANGLE)),
            bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
        )
        val svg = serializeSvg(group)
        assertTrue(
            svg.contains(
                """<polygon points="20,50 60,80 20,80" fill="none" stroke="#000000" stroke-width="1" k:flags="inside-out"/>"""
            ),
            "unexpected serialization: $svg",
        )
    }

    @Test fun `inside-out polygon round-trips through serialize and parse`() {
        val group = parseSvg(INSIDE_OUT_POLYGON_SVG)
        val once = parseSvg(serializeSvg(group))
        assertEquals(group, once)
    }

    companion object {
        /** The fixtures' triangle in CGA coordinates (viewBox 100×100). */
        private val TRIANGLE = Polygon(
            listOf(FlatPoint(-30.0, -30.0), FlatPoint(10.0, -30.0), FlatPoint(-30.0, 0.0))
        )

        /** The same loop inside out — the walk the flagged fixtures decode to. */
        private val WALL_TRIANGLE = -TRIANGLE
    }
}
