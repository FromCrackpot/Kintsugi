package fromcrackpot.kintsugi.svg

import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Segment
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Circle
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Dipole
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.RoundPoint
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
import kotlin.test.assertTrue

class ParseSvgTest {

    // The style a point fixture (render size 8) round-trips into: explicit fill + size from the geometry.
    // Size 8 matches ShapeStyle.POINT.size, so these also read as the per-primitive default size.
    private fun pointStyle(fill: ShapeColor) =
        ShapeStyle(fill = fill, size = ShapeStyle.POINT.size)
    private val circleDefaultStyle = ShapeStyle(fill = ShapeColor.BLACK, stroke = ShapeColor.NONE, strokeWidth = 1.0)
    private val lineDefaultStyle = ShapeStyle(stroke = ShapeColor.NONE, strokeWidth = 1.0)

    /* VIEWBOX */

    @Test fun `parses standard viewBox`() {
        val root = parseSvg(EMPTY_SVG)
        assertEquals(SceneBounds(0.0, 0.0, 100.0, 100.0), root.bounds)
    }

    @Test fun `parses viewBox with comma separators and offset origin`() {
        val root = parseSvg(OFFSET_VIEWBOX_SVG)
        assertEquals(SceneBounds(-10.0, -20.0, 40.0, 60.0), root.bounds)
    }

    @Test fun `falls back to width and height when viewBox absent`() {
        val root = parseSvg(NO_VIEWBOX_FALLBACK_SVG)
        assertEquals(SceneBounds(0.0, 0.0, 50.0, 50.0), root.bounds)
    }

    /* FLAT POINTS */

    @Test fun `flat-point rect becomes FlatPoint at center with SVG-default fill`() {
        val root = parseSvg(ONE_FLAT_POINT_SVG)
        val p = FlatPoint(-40.0, 40.0)
        assertEquals(
            listOf(FlatPointNode(p, pointStyle(ShapeColor.BLACK))),
            root.children,
        )
    }

    @Test fun `flat-point rect side carries the render size`() {
        val root = parseSvg(FLAT_POINT_SIZE12_SVG)
        val shape = root.children.single() as ShapeNode
        assertTrue(shape.shape is FlatPoint)
        assertEquals(12.0, shape.style.size)
    }

    /* ROUND POINTS */

    @Test fun `round-point circle becomes RoundPoint at center with SVG-default fill`() {
        val root = parseSvg(ONE_ROUND_POINT_SVG)
        val p = RoundPoint(0.0, 0.0)
        assertEquals(
            listOf(RoundPointNode(p, pointStyle(ShapeColor.BLACK))),
            root.children,
        )
    }

    @Test fun `round-point r carries the render size as 2r`() {
        val root = parseSvg(ONE_ROUND_POINT_SVG)
        val shape = root.children.single() as ShapeNode
        // r = 4 → size = 8.
        assertEquals(8.0, shape.style.size)
    }

    @Test fun `round-point with k aw is a non-null point preserving radius squared`() {
        val root = parseSvg(NON_NULL_ROUND_POINT_SVG)
        val p = (root.children.single() as ShapeNode).shape as RoundPoint
        // CGA (10, 5), aw = 67 → radius² = 2·aw − (ax²+ay²) = 134 − 125 = 9.
        assertEquals(67.0, p.aw)
        assertEquals(9.0, p.radius2)
        assertTrue(!p.isImaginary)
    }

    @Test fun `round-point with small k aw is imaginary`() {
        val root = parseSvg(IMAGINARY_ROUND_POINT_SVG)
        val p = (root.children.single() as ShapeNode).shape as RoundPoint
        // aw = 60.5 → radius² = 121 − 125 = −4 < 0.
        assertEquals(-4.0, p.radius2)
        assertTrue(p.isImaginary)
    }

    @Test fun `round-point without k aw is a null point`() {
        val root = parseSvg(ONE_ROUND_POINT_SVG)
        val p = (root.children.single() as ShapeNode).shape as RoundPoint
        assertEquals(0.0, p.radius2)
    }

    /* MIXED + Y-FLIP + SOURCE ORDER */

    @Test fun `mixed scene preserves source order and applies Y-flip with centering`() {
        val root = parseSvg(MIXED_POINTS_SVG)
        // viewBox 0 0 200 100 → center is (100, 50)
        val a = FlatPoint(-100.0, 50.0)
        val b = FlatPoint(-50.0, 0.0)
        val c = RoundPoint(0.0, 0.0)
        val d = RoundPoint(100.0, -50.0)
        assertEquals(
            listOf(
                FlatPointNode(a, pointStyle(ShapeColor.BLACK)),
                FlatPointNode(b, pointStyle(ShapeColor.BLACK)),
                RoundPointNode(c, pointStyle(ShapeColor.BLACK)),
                RoundPointNode(d, pointStyle(ShapeColor.BLACK)),
            ),
            root.children,
        )
    }

    @Test fun `interleaved scene preserves source order`() {
        val root = parseSvg(ROUND_BEFORE_FLAT_SVG)
        assertTrue((root.children[0] as ShapeNode).shape is RoundPoint)
        assertTrue((root.children[1] as ShapeNode).shape is FlatPoint)
    }

    /* COMMENTS / WHITESPACE */

    @Test fun `comments and indentation between elements are ignored`() {
        val root = parseSvg(WITH_COMMENTS_SVG)
        assertEquals(2, root.children.size)
    }

    @Test fun `empty svg returns empty children list`() {
        val root = parseSvg(EMPTY_SVG)
        assertTrue(root.children.isEmpty())
    }

    /* COLOR — SVG defaults applied explicitly on parse */

    @Test fun `missing fill on rect materialises SVG default black`() {
        val root = parseSvg(ONE_FLAT_POINT_SVG)
        assertEquals(pointStyle(ShapeColor.BLACK), (root.children.single() as ShapeNode).style)
    }

    @Test fun `parsed FlatPoint style size matches the default-sized fixture`() {
        val root = parseSvg(ONE_FLAT_POINT_SVG)
        val shape = root.children.single() as ShapeNode
        assertEquals(ShapeStyle.defaultFor(shape.shape).size, shape.style.size)
    }

    @Test fun `parsed RoundPoint style size matches the default-sized fixture`() {
        val root = parseSvg(ONE_ROUND_POINT_SVG)
        val shape = root.children.single() as ShapeNode
        assertEquals(ShapeStyle.defaultFor(shape.shape).size, shape.style.size)
    }

    @Test fun `fill='none' on rect parses to ShapeColor NONE`() {
        val root = parseSvg(FILL_NONE_SVG)
        assertEquals(pointStyle(ShapeColor.NONE), (root.children.single() as ShapeNode).style)
    }

    @Test fun `parses 6-digit hex fill`() {
        val root = parseSvg(FILL_HEX6_SVG)
        assertEquals(pointStyle(ShapeColor.rgb(0xFF, 0x00, 0x00)), (root.children.single() as ShapeNode).style)
    }

    @Test fun `parses 3-digit hex shorthand and case-insensitive`() {
        val root = parseSvg(FILL_HEX3_SHORTHAND_SVG)
        assertEquals(pointStyle(ShapeColor.rgb(0xFF, 0x00, 0x00)), (root.children.single() as ShapeNode).style)
    }

    @Test fun `parses 8-digit hex with alpha`() {
        val root = parseSvg(FILL_HEX8_SVG)
        assertEquals(pointStyle(ShapeColor.rgb(0x11, 0x22, 0x33, 0x44)), (root.children.single() as ShapeNode).style)
    }

    @Test fun `parses 8-digit hex zero-alpha as ShapeColor NONE`() {
        val root = parseSvg(FILL_HEX8_ZERO_ALPHA_SVG)
        // Canonicalization: any zero-alpha color collapses to ShapeColor.NONE.
        assertEquals(pointStyle(ShapeColor.NONE), (root.children.single() as ShapeNode).style)
    }

    @Test fun `parses functional rgb`() {
        val root = parseSvg(FILL_RGB_FUNCTIONAL_SVG)
        assertEquals(pointStyle(ShapeColor.rgb(0, 128, 255)), (root.children.single() as ShapeNode).style)
    }

    @Test fun `parses functional rgba and rounds alpha`() {
        val root = parseSvg(FILL_RGBA_FUNCTIONAL_SVG)
        // 0.5 * 255 + 0.5 = 128
        assertEquals(pointStyle(ShapeColor.rgb(0xFF, 0x00, 0x00, 0x80)), (root.children.single() as ShapeNode).style)
    }

    /* CIRCLES */

    @Test fun `circle with cx cy r becomes Circle at center with SVG-default style`() {
        val root = parseSvg(ONE_CIRCLE_SVG)
        // viewBox 0 0 100 100 → SVG (50, 50) is CGA (0, 0). r=10. SVG defaults: fill=black, stroke=none, stroke-width=1.
        assertEquals(
            listOf(CircleNode(Circle(0.0, 0.0, 10.0), circleDefaultStyle)),
            root.children,
        )
    }

    @Test fun `circle with fill stroke and stroke-width parses style`() {
        val root = parseSvg(CIRCLE_WITH_STYLE_SVG)
        val shape = root.children.single() as ShapeNode
        assertEquals(Circle(0.0, 0.0, 10.0), shape.shape)
        assertEquals(
            ShapeStyle(
                fill = ShapeColor.rgb(0xFF, 0x00, 0x00),
                stroke = ShapeColor.rgb(0x00, 0x00, 0xFF),
                strokeWidth = 2.0,
            ),
            shape.style,
        )
    }

    @Test fun `circle with no stroke materialises SVG-default stroke=none stroke-width=1`() {
        val root = parseSvg(ONE_CIRCLE_SVG)
        val style = (root.children.single() as ShapeNode).style
        assertEquals(ShapeColor.NONE, style.stroke)
        assertEquals(1.0, style.strokeWidth)
    }

    @Test fun `imaginary flag parses to an imaginary circle`() {
        val root = parseSvg(IMAGINARY_CIRCLE_SVG)
        val c = (root.children.single() as ShapeNode).shape as Circle
        assertTrue(c.isImaginary)
        // r=10 with no centering offset (center at origin) → radius² = -100.
        assertEquals(-100.0, c.radius2)
    }

    @Test fun `inside-out flag parses to a cw-positive circle`() {
        val root = parseSvg(INSIDE_OUT_CIRCLE_SVG)
        val c = (root.children.single() as ShapeNode).shape as Circle
        assertTrue(c.cw > 0)
        assertTrue(!c.isImaginary)
        assertEquals(10.0, c.radius)
    }

    @Test fun `messy k flags are tolerant and unknown tokens ignored`() {
        val root = parseSvg(CIRCLE_FLAGS_MESSY_SVG)
        val c = (root.children.single() as ShapeNode).shape as Circle
        assertTrue(c.isImaginary)
        assertTrue(c.cw > 0)
    }

    @Test fun `unknown k type on circle falls back to a circle`() {
        val root = parseSvg(CIRCLE_UNKNOWN_KTYPE_SVG)
        assertTrue((root.children.single() as ShapeNode).shape is Circle)
    }

    /* LINES — SVG-default stroke=none means invisible, but the model carries the line */

    @Test fun `line with no stroke parses with SVG-default stroke=none`() {
        val root = parseSvg(LINE_DIAGONAL_SVG)
        assertEquals(lineDefaultStyle, (root.children.single() as ShapeNode).style)
    }

    /* FULL-BLEED RECTS — there is no background concept: a full-bleed rect is an ordinary rectangle Polygon */

    @Test fun `leading full-bleed fill-only rect parses as a rectangle Polygon child`() {
        // The old format lifted this into GroupNode.background; now it's just a backdrop-sized
        // rectangle Polygon in z-order, like the SVG rect trick it always encoded.
        val root = parseSvg(BACKGROUND_BLACK_THEN_POINT_SVG)
        assertEquals(2, root.children.size)
        val backdrop = root.children.first() as ShapeNode
        assertEquals(Polygon.Rectangle(FlatPoint(-50.0, -50.0), FlatPoint(50.0, 50.0)), backdrop.shape)
        assertEquals(ShapeColor.BLACK, backdrop.style.fill)
        assertTrue((root.children[1] as ShapeNode).shape is FlatPoint)
    }

    @Test fun `full-bleed rect with offset viewBox parses as a viewBox-covering rectangle Polygon`() {
        val root = parseSvg(BACKGROUND_OFFSET_VIEWBOX_SVG)
        val shape = root.children.single() as ShapeNode
        // CGA coords are viewBox-centered, so a full-bleed rect spans (-w/2, -h/2)..(w/2, h/2).
        assertEquals(Polygon.Rectangle(FlatPoint(-20.0, -30.0), FlatPoint(20.0, 30.0)), shape.shape)
        assertEquals(ShapeColor.rgb(0x12, 0x34, 0x56), shape.style.fill)
    }

    @Test fun `full-bleed rect with rx ry is malformed`() {
        // The untagged rectangle-Polygon default applies; the codec rejects rounded corners.
        kotlin.test.assertFailsWith<MalformedRectException> { parseSvg(LEADING_RECT_FULL_BLEED_WITH_RX_SVG) }
    }

    @Test fun `full-bleed rect with no fill attribute is a rectangle Polygon with the SVG-default fill`() {
        val root = parseSvg(LEADING_FULL_BLEED_RECT_NO_FILL_SVG)
        val shape = root.children.single() as ShapeNode
        assertTrue(shape.shape is Polygon)
        assertEquals(ShapeColor.BLACK, shape.style.fill)
    }

    @Test fun `full-bleed rect with stroke is a stroked rectangle Polygon`() {
        val root = parseSvg(LEADING_FULL_BLEED_RECT_WITH_STROKE_SVG)
        val shape = root.children.single() as ShapeNode
        assertTrue(shape.shape is Polygon)
        assertEquals(ShapeColor.rgb(0xFF, 0xFF, 0xFF), shape.style.stroke)
    }

    /* ROOT-CASCADE — root <svg fill/stroke/stroke-width> falls back to children whose own attr is absent. */

    @Test fun `root fill propagates to a bare circle`() {
        val root = parseSvg(ROOT_FILL_BARE_CIRCLE_SVG)
        val shape = root.children.single() as ShapeNode
        assertEquals(ShapeColor.rgb(0xFF, 0x00, 0x00), shape.style.fill)
    }

    @Test fun `root fill propagates to a flat-point rect`() {
        val root = parseSvg(ROOT_FILL_FLAT_POINT_SVG)
        val shape = root.children.single() as ShapeNode
        assertTrue(shape.shape is FlatPoint)
        assertEquals(ShapeColor.rgb(0xFF, 0x00, 0x00), shape.style.fill)
    }

    @Test fun `root fill propagates to a round-point circle`() {
        val root = parseSvg(ROOT_FILL_ROUND_POINT_SVG)
        val shape = root.children.single() as ShapeNode
        assertTrue(shape.shape is RoundPoint)
        assertEquals(ShapeColor.rgb(0xFF, 0x00, 0x00), shape.style.fill)
    }

    @Test fun `root stroke and stroke-width propagate to a tagged line`() {
        val root = parseSvg(ROOT_STROKE_LINE_SVG)
        val shape = root.children.single() as ShapeNode
        assertEquals(ShapeColor.rgb(0x00, 0x00, 0xFF), shape.style.stroke)
        assertEquals(3.0, shape.style.strokeWidth)
    }

    @Test fun `root stroke and stroke-width propagate to a bare circle`() {
        val root = parseSvg(ROOT_STROKE_CIRCLE_SVG)
        val shape = root.children.single() as ShapeNode
        assertEquals(ShapeColor.rgb(0x00, 0x00, 0xFF), shape.style.stroke)
        assertEquals(3.0, shape.style.strokeWidth)
    }

    @Test fun `per-shape fill overrides root fill`() {
        val root = parseSvg(ROOT_FILL_SHAPE_OVERRIDE_SVG)
        val shape = root.children.single() as ShapeNode
        // Own fill green wins over root fill red.
        assertEquals(ShapeColor.rgb(0x00, 0xFF, 0x00), shape.style.fill)
    }

    @Test fun `root fill cascades into a bare leading full-bleed rect`() {
        // The leading rect has no own fill; the root provides fill=red — a rectangle Polygon whose fill
        // cascades from the root, like any other bare shape.
        val root = parseSvg(ROOT_FILL_LEADING_BARE_RECT_SVG)
        val shape = root.children.single() as ShapeNode
        assertTrue(shape.shape is Polygon)
        assertEquals(ShapeColor.rgb(0xFF, 0x00, 0x00), shape.style.fill)
    }

    /* DIPOLES */

    @Test fun `g k type dipole with two round-point children becomes a single Dipole shape`() {
        val root = parseSvg(ONE_DIPOLE_SVG)
        // CGA (-10,0) join CGA (10,0) → Dipole(0,1,0,50,0,0); unitized, real, radius² = 100.
        val expected = Dipole(0.0, 1.0, 0.0, 50.0, 0.0, 0.0)
        val shape = root.children.single() as ShapeNode
        assertEquals(expected, shape.shape)
        // Style takes the first child's style — fill defaults to SVG black, size = 2·r = 8.
        assertEquals(pointStyle(ShapeColor.BLACK), shape.style)
    }

    @Test fun `dipole without the chrome line still parses`() {
        // Designer-authored dipoles may skip the dashed <line>; parser must accept.
        val root = parseSvg(DIPOLE_NO_CHROME_SVG)
        assertEquals(Dipole(0.0, 1.0, 0.0, 50.0, 0.0, 0.0), (root.children.single() as ShapeNode).shape)
    }

    @Test fun `swapping dipole children flips the blade sign`() {
        val forward = (parseSvg(ONE_DIPOLE_SVG).children.single() as ShapeNode).shape as Dipole
        val reversed = (parseSvg(DIPOLE_REVERSED_ORDER_SVG).children.single() as ShapeNode).shape as Dipole
        // Sign-flipped twins are NOT equal (orientation-sensitive), but share a canonical form.
        kotlin.test.assertNotEquals(forward, reversed)
        assertEquals(forward.canonical, reversed.canonical)
    }

    @Test fun `dipole with one round-point child is malformed`() {
        kotlin.test.assertFailsWith<MalformedDipoleException> { parseSvg(DIPOLE_ONE_CHILD_SVG) }
    }

    @Test fun `dipole with three round-point children is malformed`() {
        kotlin.test.assertFailsWith<MalformedDipoleException> { parseSvg(DIPOLE_THREE_CHILDREN_SVG) }
    }

    @Test fun `dipole containing a rect child is malformed`() {
        kotlin.test.assertFailsWith<MalformedDipoleException> { parseSvg(DIPOLE_REJECT_RECT_CHILD_SVG) }
    }

    @Test fun `dipole child circle without k type round-point is malformed`() {
        kotlin.test.assertFailsWith<MalformedDipoleException> { parseSvg(DIPOLE_NON_ROUND_POINT_CHILD_SVG) }
    }

    @Test fun `dipole with coincident endpoints is malformed`() {
        // RoundPoint join RoundPoint with start == end → DegenerateDipoleException → wrapped at the boundary.
        kotlin.test.assertFailsWith<MalformedDipoleException> { parseSvg(DIPOLE_COINCIDENT_SVG) }
    }

    @Test fun `g with unknown k type warns and falls back to a plain GroupNode`() {
        // Unknown k:type on <g> matches the <circle>/<line> pattern: warn but don't fail. Children
        // still parse normally; treating an unknown carrier as a plain group is the conservative
        // forward-compat option (Kintsugi vNext might introduce new <g k:type> carriers).
        val root = parseSvg(GROUP_UNKNOWN_KTYPE_SVG)
        val nested = root.children.single() as GroupNode
        assertTrue((nested.children.single() as ShapeNode).shape is RoundPoint)
    }

    /* GROUPS (GroupNode ↔ <g>) */

    @Test fun `bare g becomes a nested GroupNode containing its children`() {
        val root = parseSvg(NESTED_GROUP_ONE_FLAT_POINT_SVG)
        val nested = root.children.single() as GroupNode
        assertEquals(1, nested.children.size)
        assertTrue((nested.children.single() as ShapeNode).shape is FlatPoint)
    }

    @Test fun `empty g becomes a nested GroupNode with no children`() {
        val root = parseSvg(EMPTY_GROUP_SVG)
        val nested = root.children.single() as GroupNode
        assertTrue(nested.children.isEmpty())
        assertEquals(null, nested.bounds)
    }

    @Test fun `nested g elements form a deeper tree`() {
        val root = parseSvg(NESTED_GROUPS_DEEP_SVG)
        val outer = root.children.single() as GroupNode
        val inner = outer.children.single() as GroupNode
        assertTrue((inner.children.single() as ShapeNode).shape is Circle)
    }

    @Test fun `sibling g elements are preserved as separate GroupNodes`() {
        val root = parseSvg(MULTIPLE_GROUPS_SVG)
        assertEquals(2, root.children.size)
        val first = root.children[0] as GroupNode
        val second = root.children[1] as GroupNode
        assertTrue((first.children.single() as ShapeNode).shape is Circle)
        assertTrue((second.children.single() as ShapeNode).shape is Segment)
    }

    @Test fun `group fill cascades into a bare child circle`() {
        val root = parseSvg(GROUP_FILL_CASCADE_SVG)
        val nested = root.children.single() as GroupNode
        val circle = nested.children.single() as ShapeNode
        assertEquals(ShapeColor.rgb(0xFF, 0x00, 0x00), circle.style.fill)
    }

    @Test fun `child own-fill overrides group fill`() {
        val root = parseSvg(GROUP_FILL_OVERRIDDEN_BY_CHILD_SVG)
        val circle = (root.children.single() as GroupNode).children.single() as ShapeNode
        assertEquals(ShapeColor.rgb(0x00, 0xFF, 0x00), circle.style.fill)
    }

    @Test fun `group stroke and stroke-width cascade into a tagged line`() {
        val root = parseSvg(GROUP_STROKE_CASCADE_SVG)
        val line = (root.children.single() as GroupNode).children.single() as ShapeNode
        assertEquals(ShapeColor.rgb(0x00, 0x00, 0xFF), line.style.stroke)
        assertEquals(3.0, line.style.strokeWidth)
    }

    @Test fun `group fill cascades through two levels of nesting`() {
        val root = parseSvg(NESTED_GROUP_FILL_CASCADE_SVG)
        val outer = root.children.single() as GroupNode
        val inner = outer.children.single() as GroupNode
        val circle = inner.children.single() as ShapeNode
        assertEquals(ShapeColor.rgb(0xFF, 0x00, 0x00), circle.style.fill)
    }

    @Test fun `inner group fill overrides outer group fill for the inner subtree`() {
        val root = parseSvg(NESTED_GROUP_FILL_OVERRIDE_SVG)
        val outer = root.children.single() as GroupNode
        val inner = outer.children.single() as GroupNode
        val circle = inner.children.single() as ShapeNode
        assertEquals(ShapeColor.rgb(0x00, 0xFF, 0x00), circle.style.fill)
    }

    @Test fun `dipole carrier fill cascades into its round-point children`() {
        // Designer authoring `<g k:type="dipole" fill="#ff0000">` — the dipole carrier's own fill
        // overlays on top of the inherited cascade, same posture as bare `<g fill=...>`.
        val root = parseSvg(DIPOLE_GROUP_FILL_CASCADE_SVG)
        val shape = root.children.single() as ShapeNode
        assertTrue(shape.shape is Dipole)
        assertEquals(ShapeColor.rgb(0xFF, 0x00, 0x00), shape.style.fill)
    }

    @Test fun `full-bleed rect with stroke=none is a strokeless rectangle Polygon`() {
        // `stroke="none"` canonicalizes to ShapeColor.NONE on the rectangle Polygon's style.
        val root = parseSvg(LEADING_FULL_BLEED_RECT_STROKE_NONE_SVG)
        val shape = root.children.single() as ShapeNode
        assertTrue(shape.shape is Polygon)
        assertEquals(ShapeColor.NONE, shape.style.stroke)
    }

    @Test fun `designer-authored g attributes are silently dropped on parse`() {
        // id, inkscape:label, inkscape:groupmode are ignored; the group itself still parses normally.
        val root = parseSvg(GROUP_INKSCAPE_ATTRS_SVG)
        val nested = root.children.single() as GroupNode
        assertEquals(null, nested.bounds)
        assertTrue((nested.children.single() as ShapeNode).shape is Circle)
    }

    /* K:TAGS */

    @Test fun `k tags tokens are split tolerantly and preserved verbatim`() {
        // Open vocabulary: no lowercasing ("Steel" stays "Steel"), messy whitespace tolerated.
        val root = parseSvg(TAGGED_POLYGON_SVG)
        val shape = root.children.single() as ShapeNode
        assertEquals(setOf("brick", "Steel"), shape.tags)
    }

    @Test fun `group k tags union into leaves like the style cascade`() {
        val root = parseSvg(TAGGED_GROUP_SVG)
        val group = root.children.single() as GroupNode
        val polygon = group.children[0] as ShapeNode
        val circle = group.children[1] as ShapeNode
        assertEquals(setOf("brick", "steel"), polygon.tags)
        assertEquals(setOf("brick"), circle.tags)
    }

    @Test fun `dipole tags ride the carrier g and child k tags is ignored`() {
        val root = parseSvg(TAGGED_DIPOLE_SVG)
        val shape = root.children.single() as ShapeNode
        assertTrue(shape.shape is Dipole)
        assertEquals(setOf("portal"), shape.tags)
    }

    @Test fun `untagged shapes have empty tags`() {
        val root = parseSvg(ONE_FLAT_POINT_SVG)
        assertEquals(emptySet(), (root.children.single() as ShapeNode).tags)
    }
}
