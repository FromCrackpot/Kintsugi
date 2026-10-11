package fromcrackpot.kintsugi.svg

import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Circle
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
 * Untagged `<rect>` codec tests. Bare `<rect>` is **parse-only**: it decodes into a rectangle
 * [Polygon] (via `Polygon.Rectangle`) and re-serializes as `<polygon>` — deliberately lossy at
 * the element level, lossless at the CGA level. Only the flat-point reading of `<rect>` keeps
 * precedence (via its `k:type` tag).
 */
class RectangleTest {

    /* PARSE */

    @Test fun `bare rect becomes a rectangle Polygon in factory order`() {
        // viewBox 100×100: SVG top-left (20, 30), 40×20 → CGA bl = svgToCga(20, 50) = (−30, 0).
        // The vertex ORDER pinned here is the factory-order invariant (bl → br → tr → tl):
        // :brickit indexes edges[0]=bottom … [3]=left on SVG-loaded sprites,
        // so the <rect> parser must emit exactly Polygon.Rectangle's order.
        val root = parseSvg(ONE_RECTANGLE_SVG)
        val shape = root.children.single() as ShapeNode
        assertEquals(
            Polygon(
                listOf(
                    FlatPoint(-30.0, 0.0), FlatPoint(10.0, 0.0),
                    FlatPoint(10.0, 20.0), FlatPoint(-30.0, 20.0),
                )
            ),
            shape.shape,
        )
    }

    @Test fun `bare rect at the viewBox origin maps through the Y-flip`() {
        // SVG (0, 0), 20×10 → CGA bl = svgToCga(0, 10) = (−50, 40).
        val root = parseSvg(BARE_RECT_SVG)
        val shape = root.children.single() as ShapeNode
        assertEquals(Polygon.Rectangle(FlatPoint(-50.0, 40.0), FlatPoint(-30.0, 50.0)), shape.shape)
    }

    @Test fun `bare rect inside a nested g parses as a rectangle Polygon`() {
        val root = parseSvg(NESTED_BARE_RECT_SVG)
        val nested = root.children.single() as GroupNode
        val shape = nested.children.single() as ShapeNode
        assertEquals(Polygon.Rectangle(FlatPoint(-50.0, 40.0), FlatPoint(-30.0, 50.0)), shape.shape)
    }

    @Test fun `unknown k type on rect warns and falls back to a rectangle Polygon`() {
        val root = parseSvg(RECTANGLE_UNKNOWN_KTYPE_SVG)
        val shape = root.children.single() as ShapeNode
        assertEquals(Polygon.Rectangle(FlatPoint(-30.0, 0.0), FlatPoint(10.0, 20.0)), shape.shape)
    }

    @Test fun `rect with rx zero parses - zero rounding is square corners`() {
        val root = parseSvg(RECTANGLE_ZERO_RX_SVG)
        assertTrue((root.children.single() as ShapeNode).shape is Polygon)
    }

    /* STYLE */

    @Test fun `rect with fill stroke and stroke-width parses style`() {
        val shape = parseSvg(ONE_RECTANGLE_SVG).children.single() as ShapeNode
        assertEquals(
            ShapeStyle(
                fill = ShapeColor.rgb(0xFF, 0x00, 0x00),
                stroke = ShapeColor.rgb(0x00, 0x00, 0xFF),
                strokeWidth = 2.0,
            ),
            shape.style,
        )
    }

    @Test fun `unstyled rect materialises SVG defaults - fill black, stroke none, width 1`() {
        val shape = parseSvg(BARE_RECT_SVG).children.single() as ShapeNode
        assertEquals(
            ShapeStyle(fill = ShapeColor.BLACK, stroke = ShapeColor.NONE, strokeWidth = 1.0),
            shape.style,
        )
    }

    @Test fun `group fill cascades into a bare child rect`() {
        val nested = parseSvg(GROUP_FILL_CASCADE_RECT_SVG).children.single() as GroupNode
        val shape = nested.children.single() as ShapeNode
        assertEquals(ShapeColor.rgb(0x00, 0xFF, 0x00), shape.style.fill)
    }

    /* MALFORMED */

    @Test fun `bare rect missing any required geometry attribute throws MalformedRectException`() {
        listOf("x", "y", "width", "height").forEach { missing ->
            assertFailsWith<MalformedRectException>("expected throw for missing $missing") {
                parseSvg(rectSvgWithout(missing))
            }
        }
    }

    @Test fun `non-zero rx throws MalformedRectException - rounded corners unsupported`() {
        val ex = assertFailsWith<MalformedRectException> { parseSvg(RECTANGLE_RX_SVG) }
        assertTrue(ex.message!!.contains("rounded corners"), "unexpected reason: ${ex.message}")
    }

    @Test fun `non-zero ry throws MalformedRectException - rounded corners unsupported`() {
        assertFailsWith<MalformedRectException> { parseSvg(RECTANGLE_RY_SVG) }
    }

    @Test fun `zero width throws MalformedRectException wrapping the degenerate factory`() {
        assertFailsWith<MalformedRectException> { parseSvg(RECTANGLE_ZERO_WIDTH_SVG) }
    }

    @Test fun `negative height throws MalformedRectException - extents are not re-anchored`() {
        assertFailsWith<MalformedRectException> { parseSvg(RECTANGLE_NEGATIVE_HEIGHT_SVG) }
    }

    /* FULL-BLEED RECTS — no special reading; position and size carry no extra semantics */

    @Test fun `non-leading full-bleed fill-only rect is a rectangle Polygon`() {
        val root = parseSvg(NON_LEADING_FULL_BLEED_RECT_SVG)
        assertEquals(2, root.children.size)
        assertTrue((root.children[0] as ShapeNode).shape is Circle)
        assertEquals(
            Polygon.Rectangle(FlatPoint(-50.0, -50.0), FlatPoint(50.0, 50.0)),
            (root.children[1] as ShapeNode).shape,
        )
    }

    @Test fun `two full-bleed fill-only rects are two stacked rectangle Polygons in source order`() {
        val root = parseSvg(TWO_FULL_BLEED_RECTS_SVG)
        assertEquals(2, root.children.size)
        val first = root.children[0] as ShapeNode
        val second = root.children[1] as ShapeNode
        assertEquals(Polygon.Rectangle(FlatPoint(-50.0, -50.0), FlatPoint(50.0, 50.0)), first.shape)
        assertEquals(ShapeColor.BLACK, first.style.fill)
        assertEquals(Polygon.Rectangle(FlatPoint(-50.0, -50.0), FlatPoint(50.0, 50.0)), second.shape)
        assertEquals(ShapeColor.rgb(0xFF, 0x00, 0x00), second.style.fill)
    }

    /* SERIALIZE */

    @Test fun `rectangle Polygon serializes as a polygon element - rect is parse-only`() {
        val group = GroupNode(
            bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
            children = listOf(
                PolygonNode(
                    Polygon.Rectangle(FlatPoint(-30.0, 0.0), FlatPoint(10.0, 20.0)),
                    ShapeStyle(
                        fill = ShapeColor.rgb(0xFF, 0x00, 0x00),
                        stroke = ShapeColor.rgb(0x00, 0x00, 0xFF),
                        strokeWidth = 2.0,
                    ),
                ),
            ),
        )
        val emitted = serializeSvg(group)
        // bl → br → tr → tl through the Y-flip: (−30,0)→20,50 (10,0)→60,50 (10,20)→60,30 (−30,20)→20,30.
        assertTrue(
            emitted.contains("""<polygon points="20,50 60,50 60,30 20,30" fill="#ff0000" stroke="#0000ff" stroke-width="2"/>"""),
            "actual:\n$emitted",
        )
    }

    @Test fun `parsed rect round-trips as a stable polygon`() {
        // First save converts <rect> → <polygon> (deliberately lossy element-wise); the second
        // save is byte-stable and the model is identical at every step.
        val parsed = parseSvg(ONE_RECTANGLE_SVG)
        val once = serializeSvg(parsed)
        assertEquals(parsed, parseSvg(once))
        assertEquals(once, serializeSvg(parseSvg(once)))
    }

    @Test fun `full-bleed fill-only rectangle Polygon round-trips as itself`() {
        // The old format's collision corner (full-bleed rect re-parsing as a lifted background)
        // is gone with the background concept itself: a full-bleed rectangle Polygon is stable.
        val original = GroupNode(
            bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
            children = listOf(
                PolygonNode(
                    Polygon.Rectangle(FlatPoint(-50.0, -50.0), FlatPoint(50.0, 50.0)),
                    ShapeStyle(fill = ShapeColor.rgb(0x20, 0x20, 0x20), strokeWidth = 1.0),
                ),
            ),
        )
        assertEquals(original, parseSvg(serializeSvg(original)))
    }
}
