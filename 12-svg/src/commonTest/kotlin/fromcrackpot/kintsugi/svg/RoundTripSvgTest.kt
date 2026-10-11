package fromcrackpot.kintsugi.svg

import fromcrackpot.kintsugi.cga.d2.products.join
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Segment
import fromcrackpot.kintsugi.cga.d2.shapes.composites.unaryMinus
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.*
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
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RoundTripSvgTest {

    /*
     * Leaf node ctors seed `style` at construction with the per-shape default, so scenes built
     * with `XxxNode(shape)` already carry concrete style and are model-equal after a save cycle.
     * We still also assert the stronger idempotency invariant
     * `parse(serialize(parse(serialize(s)))) == parse(serialize(s))` for defensive coverage.
     */

    /* IDEMPOTENCY: SCENE → SVG → SCENE → SVG → SCENE */

    private fun assertIdempotent(original: GroupNode) {
        val once = parseSvg(serializeSvg(original))
        val twice = parseSvg(serializeSvg(once))
        assertEquals(once, twice)
    }

    @Test fun `empty scene round-trips`() {
        val original = GroupNode(bounds = SceneBounds(0.0, 0.0, 100.0, 100.0))
        assertEquals(original, parseSvg(serializeSvg(original)))
    }

    @Test fun `flat shape is idempotent after a save cycle`() {
        assertIdempotent(
            GroupNode(
                bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
                children = listOf(FlatPointNode(FlatPoint(-40.0, 40.0))),
            )
        )
    }

    @Test fun `round shape is idempotent after a save cycle`() {
        assertIdempotent(
            GroupNode(
                bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
                children = listOf(RoundPointNode(RoundPoint(0.0, 0.0))),
            )
        )
    }

    @Test fun `interleaved scene with offset viewBox is idempotent and preserves order`() {
        val original = GroupNode(
            bounds = SceneBounds(-10.0, -20.0, 40.0, 60.0),
            children = listOf(
                FlatPointNode(FlatPoint(0.0, 0.0)),
                RoundPointNode(RoundPoint(-3.0, 12.0)),
                FlatPointNode(FlatPoint(5.0, -7.0)),
            ),
        )
        assertIdempotent(original)
        val once = parseSvg(serializeSvg(original))
        // primitives survive intact, order preserved.
        assertEquals(
            listOf(FlatPoint(0.0, 0.0), RoundPoint(-3.0, 12.0), FlatPoint(5.0, -7.0)),
            once.children.map { (it as ShapeNode).shape },
        )
    }

    @Test fun `non-integer coordinates are idempotent`() {
        assertIdempotent(
            GroupNode(
                bounds = SceneBounds(0.0, 0.0, 200.0, 100.0),
                children = listOf(FlatPointNode(FlatPoint(1.2345, -6.789))),
            )
        )
    }

    @Test fun `flat-point render size round-trips through the rect geometry`() {
        val original = GroupNode(
            bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
            children = listOf(FlatPointNode(FlatPoint(0.0, 0.0), ShapeStyle(fill = ShapeColor.GREEN, size = 12.0))),
        )
        val once = parseSvg(serializeSvg(original))
        assertEquals(12.0, (once.children.single() as ShapeNode).style.size)
        assertEquals(original, once)
    }

    /* COLOR ROUND-TRIPS — explicit styles round-trip byte-for-byte at the model level */

    // The style a point fixture round-trips into — explicit fill + size from the geometry (size 8 == default).
    private fun pointRoundTripStyle(fill: ShapeColor) =
        ShapeStyle(fill = fill, size = ShapeStyle.defaultFor(FlatPoint(0.0, 0.0)).size)

    @Test fun `opaque color round-trips`() {
        val original = GroupNode(
            bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
            children = listOf(FlatPointNode(FlatPoint(0.0, 0.0), pointRoundTripStyle(ShapeColor.rgb(0xFF, 0x00, 0x00)))),
        )
        assertEquals(original, parseSvg(serializeSvg(original)))
    }

    @Test fun `alpha color round-trips`() {
        val original = GroupNode(
            bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
            children = listOf(FlatPointNode(FlatPoint(0.0, 0.0), pointRoundTripStyle(ShapeColor.rgb(0x11, 0x22, 0x33, 0x44)))),
        )
        assertEquals(original, parseSvg(serializeSvg(original)))
    }

    @Test fun `fill='none' round-trips as ShapeColor NONE`() {
        val original = GroupNode(
            bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
            children = listOf(FlatPointNode(FlatPoint(0.0, 0.0), pointRoundTripStyle(ShapeColor.NONE))),
        )
        assertEquals(original, parseSvg(serializeSvg(original)))
    }

    @Test fun `zero-alpha color canonicalises to NONE then round-trips`() {
        val original = GroupNode(
            bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
            children = listOf(FlatPointNode(FlatPoint(0.0, 0.0), ShapeStyle(fill = ShapeColor.rgb(0x12, 0x34, 0x56, 0x00), size = 8.0))),
        )
        val reloaded = parseSvg(serializeSvg(original))
        // The reloaded shape's fill is ShapeColor.NONE (canonical equivalence class), not the original #12345600.
        assertEquals(ShapeColor.NONE, (reloaded.children.single() as ShapeNode).style.fill)
    }

    /* CIRCLE ROUND-TRIPS */

    @Test fun `classic circle is idempotent`() {
        assertIdempotent(
            GroupNode(
                bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
                children = listOf(CircleNode(Circle(0.0, 0.0, 10.0))),
            )
        )
    }

    @Test fun `circle with fill stroke stroke-width round-trips byte-for-byte at model level`() {
        val original = GroupNode(
            bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
            children = listOf(
                CircleNode(
                    Circle(5.0, -3.0, 7.5),
                    ShapeStyle(
                        fill = ShapeColor.rgb(0xFF, 0x00, 0x00),
                        stroke = ShapeColor.rgb(0x00, 0x00, 0xFF),
                        strokeWidth = 2.0,
                    ),
                ),
            ),
        )
        assertEquals(original, parseSvg(serializeSvg(original)))
    }

    @Test fun `default-style circle round-trips to ShapeStyle defaults`() {
        val original = GroupNode(
            bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
            children = listOf(CircleNode(Circle(0.0, 0.0, 10.0))),
        )
        val once = parseSvg(serializeSvg(original))
        val expectedStyle = ShapeStyle.defaultFor(Circle(0.0, 0.0, 10.0))
        assertEquals(expectedStyle, (once.children.single() as ShapeNode).style)
    }

    @Test fun `imaginary circle is persisted via k flags and round-trips`() {
        val imaginary = Circle(5.0, -3.0, -19.0, -1.0) // radius² = -4
        assertTrue(imaginary.isImaginary)
        val original = GroupNode(
            bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
            children = listOf(CircleNode(imaginary)),
        )
        val once = parseSvg(serializeSvg(original))
        val rt = (once.children.single() as ShapeNode).shape as Circle
        assertEquals(imaginary, rt)
        assertTrue(rt.isImaginary, "imaginary-ness was lost on round-trip")
        assertIdempotent(original)
    }

    @Test fun `inside-out circle is persisted via k flags and round-trips`() {
        val insideOut = Circle(-3.0, -4.0, 0.0, 1.0) // cw = +1, radius 5
        assertTrue(insideOut.cw > 0)
        val original = GroupNode(
            bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
            children = listOf(CircleNode(insideOut)),
        )
        val once = parseSvg(serializeSvg(original))
        val rt = (once.children.single() as ShapeNode).shape as Circle
        assertEquals(insideOut, rt)
        assertTrue(rt.cw > 0, "orientation was lost on round-trip")
        assertIdempotent(original)
    }

    @Test fun `inside-out polygon is persisted via k flags and round-trips`() {
        val insideOut = -Polygon(
            listOf(FlatPoint(-30.0, -30.0), FlatPoint(10.0, -30.0), FlatPoint(-30.0, 0.0))
        )
        assertTrue(insideOut.isInsideOut)
        val original = GroupNode(
            bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
            children = listOf(PolygonNode(insideOut)),
        )
        val once = parseSvg(serializeSvg(original))
        val rt = (once.children.single() as ShapeNode).shape as Polygon
        assertEquals(insideOut, rt)
        assertTrue(rt.isInsideOut, "orientation was lost on round-trip")
        assertIdempotent(original)
    }

    /* ROUND-POINT ALGEBRAIC RADIUS ROUND-TRIPS (k:aw) */

    @Test fun `non-null round point persists aw and round-trips`() {
        val p = RoundPoint(10.0, 5.0, 1.0, 67.0) // radius² = 9
        val original = GroupNode(
            bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
            children = listOf(RoundPointNode(p, ShapeStyle(fill = ShapeColor.GREEN, size = 8.0))),
        )
        val once = parseSvg(serializeSvg(original))
        val rt = (once.children.single() as ShapeNode).shape as RoundPoint
        // RoundPoint.equals ignores aw, so assert the carried radius² explicitly.
        assertEquals(67.0, rt.aw)
        assertEquals(9.0, rt.radius2)
        assertEquals(original, once)
    }

    @Test fun `hand-authored round point without k aw parses, then re-emits with k aw`() {
        // Canonicalization: bare <circle k:type="round-point"> (no k:aw) parses via the 2-arg
        // conformal lift; on re-serialize, k:aw is emitted explicitly. Same posture as #F00 → #ff0000
        // and the always-emit style defaults. Round-trip is Double-exact after the first save.
        val first = serializeSvg(parseSvg(ONE_ROUND_POINT_SVG))
        assertTrue(first.contains("k:aw=\""), "expected canonicalized output to carry k:aw, got:\n$first")
        // Idempotent thereafter.
        assertEquals(first, serializeSvg(parseSvg(first)))
    }

    @Test fun `imaginary round point round-trips preserving imaginary-ness`() {
        val p = RoundPoint(10.0, 5.0, 1.0, 60.5) // radius² = -4
        assertTrue(p.isImaginary)
        val original = GroupNode(
            bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
            children = listOf(RoundPointNode(p, ShapeStyle(fill = ShapeColor.GREEN, size = 8.0))),
        )
        val once = parseSvg(serializeSvg(original))
        val rt = (once.children.single() as ShapeNode).shape as RoundPoint
        assertEquals(60.5, rt.aw)
        assertTrue(rt.isImaginary, "imaginary-ness was lost on round-trip")
        assertIdempotent(original)
    }

    /* LINE ROUND-TRIPS */

    @Test fun `horizontal Line is idempotent`() {
        assertIdempotent(
            GroupNode(
                bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
                children = listOf(LineNode(Line(0.0, 1.0, 0.0))),
            )
        )
    }

    @Test fun `diagonal Line via join is idempotent`() {
        assertIdempotent(
            GroupNode(
                bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
                children = listOf(LineNode(FlatPoint(-50.0, 50.0) join FlatPoint(50.0, -50.0))),
            )
        )
    }

    @Test fun `Line orientation survives parse-serialize-parse cycle`() {
        // The two sign-flipped twins must each round-trip exactly AND remain distinct from each
        // other after the cycle — catching any silent canonicalization in the serialize/parse path
        // that would symmetrize orientation (e.g. emitting endpoints in lexicographic order).
        val bounds = SceneBounds(0.0, 0.0, 100.0, 100.0)
        val forward = GroupNode(
            bounds = bounds,
            children = listOf(LineNode(FlatPoint(-50.0, 50.0) join FlatPoint(50.0, -50.0))),
        )
        val reverse = GroupNode(
            bounds = bounds,
            children = listOf(LineNode(FlatPoint(50.0, -50.0) join FlatPoint(-50.0, 50.0))),
        )

        val forwardRT = parseSvg(serializeSvg(forward))
        val reverseRT = parseSvg(serializeSvg(reverse))

        assertEquals(forward, forwardRT)
        assertEquals(reverse, reverseRT)

        val forwardLine = (forwardRT.children.single() as ShapeNode).shape as Line
        val reverseLine = (reverseRT.children.single() as ShapeNode).shape as Line
        assertNotEquals(forwardLine, reverseLine)
        assertEquals(forwardLine.canonical, reverseLine.canonical)
    }

    @Test fun `styled Line round-trips`() {
        val original = GroupNode(
            bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
            children = listOf(
                LineNode(
                    Line(1.0, 0.0, 0.0), // vertical line x = 0
                    ShapeStyle(stroke = ShapeColor.rgb(0xFF, 0xD7, 0x00), strokeWidth = 2.0),
                ),
            ),
        )
        assertEquals(original, parseSvg(serializeSvg(original)))
    }

    /* SEGMENT ROUND-TRIPS */

    @Test fun `horizontal Segment is idempotent`() {
        assertIdempotent(
            GroupNode(
                bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
                children = listOf(SegmentNode(Segment(FlatPoint(-30.0, 0.0), FlatPoint(30.0, 0.0)))),
            )
        )
    }

    @Test fun `skew Segment is idempotent`() {
        assertIdempotent(
            GroupNode(
                bounds = SceneBounds(-50.0, -50.0, 100.0, 100.0),
                children = listOf(SegmentNode(Segment(FlatPoint(1.0, 2.0), FlatPoint(4.0, -3.0)))),
            )
        )
    }

    @Test fun `Segment orientation survives parse-serialize-parse cycle`() {
        // start ∧ end and end ∧ start are sign-flipped twins on the underlying Dipole — both must
        // round-trip exactly AND stay distinct, catching any silent canonicalization in the codec.
        val bounds = SceneBounds(0.0, 0.0, 100.0, 100.0)
        val forward = GroupNode(
            bounds = bounds,
            children = listOf(SegmentNode(Segment(FlatPoint(-30.0, 30.0), FlatPoint(30.0, -30.0)))),
        )
        val reverse = GroupNode(
            bounds = bounds,
            children = listOf(SegmentNode(Segment(FlatPoint(30.0, -30.0), FlatPoint(-30.0, 30.0)))),
        )

        val forwardRT = parseSvg(serializeSvg(forward))
        val reverseRT = parseSvg(serializeSvg(reverse))

        assertEquals(forward, forwardRT)
        assertEquals(reverse, reverseRT)

        val forwardSegment = (forwardRT.children.single() as ShapeNode).shape as Segment
        val reverseSegment = (reverseRT.children.single() as ShapeNode).shape as Segment
        assertNotEquals(forwardSegment, reverseSegment)
        assertEquals(forwardSegment.canonical, reverseSegment.canonical)
    }

    @Test fun `styled Segment round-trips`() {
        val original = GroupNode(
            bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
            children = listOf(
                SegmentNode(
                    Segment(FlatPoint(-30.0, 30.0), FlatPoint(30.0, -30.0)),
                    ShapeStyle(stroke = ShapeColor.rgb(0xFF, 0x00, 0x00), strokeWidth = 2.0),
                ),
            ),
        )
        assertEquals(original, parseSvg(serializeSvg(original)))
    }

    @Test fun `round-capped Segment round-trips its strokeCap`() {
        val original = GroupNode(
            bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
            children = listOf(
                SegmentNode(
                    Segment(FlatPoint(-30.0, 30.0), FlatPoint(30.0, -30.0)),
                    ShapeStyle(stroke = ShapeColor.rgb(0xFF, 0x00, 0x00), strokeWidth = 2.0, strokeCap = ShapeStrokeCap.ROUND),
                ),
            ),
        )
        assertEquals(original, parseSvg(serializeSvg(original)))
    }

    @Test fun `square-capped Line round-trips its strokeCap`() {
        val original = GroupNode(
            bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
            children = listOf(
                LineNode(
                    Line(1.0, 0.0, 0.0), // vertical line x = 0
                    ShapeStyle(stroke = ShapeColor.rgb(0xFF, 0x00, 0x00), strokeWidth = 2.0, strokeCap = ShapeStrokeCap.SQUARE),
                ),
            ),
        )
        assertEquals(original, parseSvg(serializeSvg(original)))
    }

    /* RECTANGLE-POLYGON ROUND-TRIPS — rect is parse-only; rectangles live as Polygon quads */

    @Test fun `rectangle Polygon is idempotent`() {
        assertIdempotent(
            GroupNode(
                bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
                children = listOf(PolygonNode(Polygon.Rectangle(FlatPoint(-30.0, 0.0), FlatPoint(10.0, 20.0)))),
            )
        )
    }

    @Test fun `styled rectangle Polygon round-trips`() {
        val original = GroupNode(
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
        assertEquals(original, parseSvg(serializeSvg(original)))
    }

    @Test fun `non-integer rectangle Polygon coordinates are idempotent`() {
        assertIdempotent(
            GroupNode(
                bounds = SceneBounds(0.0, 0.0, 200.0, 100.0),
                children = listOf(
                    PolygonNode(Polygon.Rectangle(FlatPoint(1.2345, -6.789), FlatPoint(13.7345, 0.461))),
                ),
            )
        )
    }

    /* SVG → SCENE → SVG → SCENE (model-level idempotency) */

    @Test fun `parse-serialize-parse is idempotent for fixture inputs`() {
        listOf(
            EMPTY_SVG,
            ONE_FLAT_POINT_SVG,
            ONE_ROUND_POINT_SVG,
            FLAT_POINT_SIZE12_SVG,
            NON_NULL_ROUND_POINT_SVG,
            IMAGINARY_ROUND_POINT_SVG,
            MIXED_POINTS_SVG,
            OFFSET_VIEWBOX_SVG,
            NO_VIEWBOX_FALLBACK_SVG,
            WITH_COMMENTS_SVG,
            FILL_HEX6_SVG,
            FILL_HEX3_SHORTHAND_SVG,
            FILL_HEX8_SVG,
            FILL_HEX8_ZERO_ALPHA_SVG,
            FILL_RGB_FUNCTIONAL_SVG,
            FILL_RGBA_FUNCTIONAL_SVG,
            FILL_NONE_SVG,
            ROUND_BEFORE_FLAT_SVG,
            ONE_CIRCLE_SVG,
            CIRCLE_WITH_STYLE_SVG,
            IMAGINARY_CIRCLE_SVG,
            INSIDE_OUT_CIRCLE_SVG,
            LINE_DIAGONAL_SVG,
            LINE_DIAGONAL_STYLED_SVG,
            LINE_SEGMENT_SVG,
            LINE_SEGMENT_STYLED_SVG,
            LINE_SEGMENT_SWAPPED_SVG,
            LINE_SEGMENT_ROUNDCAP_SVG,
            LINE_DIAGONAL_SQUARECAP_SVG,
            ONE_DIPOLE_SVG,
            DIPOLE_NO_CHROME_SVG,
            DIPOLE_REVERSED_ORDER_SVG,
            ONE_RECTANGLE_SVG,
            BARE_RECT_SVG,
            NESTED_BARE_RECT_SVG,
            GROUP_FILL_CASCADE_RECT_SVG,
            NON_LEADING_FULL_BLEED_RECT_SVG,
            TWO_FULL_BLEED_RECTS_SVG,
        ).forEach { svg ->
            val once = parseSvg(svg)
            val twice = parseSvg(serializeSvg(once))
            assertEquals(once, twice, "idempotency broke for fixture:\n$svg")
        }
    }

    @Test fun `parse-serialize emits the k namespace declaration`() {
        val out = serializeSvg(parseSvg(ONE_FLAT_POINT_SVG))
        assertTrue("""xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1"""" in out, out)
        assertTrue("""k:type="flat-point"""" in out, out)
    }

    /* DIPOLE ROUND-TRIPS */

    @Test fun `real horizontal dipole is idempotent`() {
        assertIdempotent(
            GroupNode(
                bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
                children = listOf(DipoleNode(RoundPoint(-10.0, 0.0) join RoundPoint(10.0, 0.0))),
            )
        )
    }

    @Test fun `real skew dipole is idempotent`() {
        // Non-axis-aligned dipole built via join (Plücker constraint satisfied by construction).
        assertIdempotent(
            GroupNode(
                bounds = SceneBounds(-50.0, -50.0, 100.0, 100.0),
                children = listOf(DipoleNode(RoundPoint(1.0, 2.0) join RoundPoint(4.0, -3.0))),
            )
        )
    }

    @Test fun `dipole orientation survives parse-serialize-parse cycle`() {
        // start ∧ end and end ∧ start are sign-flipped twins — both must round-trip exactly AND stay distinct.
        val bounds = SceneBounds(0.0, 0.0, 100.0, 100.0)
        val forward = GroupNode(
            bounds = bounds,
            children = listOf(DipoleNode(RoundPoint(-10.0, 0.0) join RoundPoint(10.0, 0.0))),
        )
        val reverse = GroupNode(
            bounds = bounds,
            children = listOf(DipoleNode(RoundPoint(10.0, 0.0) join RoundPoint(-10.0, 0.0))),
        )

        val forwardRT = parseSvg(serializeSvg(forward))
        val reverseRT = parseSvg(serializeSvg(reverse))

        assertEquals(forward, forwardRT)
        assertEquals(reverse, reverseRT)

        val forwardDipole = (forwardRT.children.single() as ShapeNode).shape as Dipole
        val reverseDipole = (reverseRT.children.single() as ShapeNode).shape as Dipole
        assertNotEquals(forwardDipole, reverseDipole)
        assertEquals(forwardDipole.canonical, reverseDipole.canonical)
    }

    @Test fun `imaginary dipole round-trips preserving imaginary-ness`() {
        // Dipole(1, 0, 1, 0, 0, 0): radius² = pz² - gz² = 0 - 1 = -1. Verified analytically:
        // start ∧ end of the real-fallback endpoints reconstructs this exact blade because the
        // sqrt(|r²|) substitution preserves the sign of gz² through the wedge.
        val dipole = Dipole(1.0, 0.0, 1.0, 0.0, 0.0, 0.0)
        assertTrue(dipole.isImaginary)
        val original = GroupNode(
            bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
            children = listOf(DipoleNode(dipole)),
        )
        val once = parseSvg(serializeSvg(original))
        val rt = (once.children.single() as ShapeNode).shape as Dipole
        assertEquals(dipole, rt)
        assertTrue(rt.isImaginary, "imaginary-ness was lost on dipole round-trip")
        assertIdempotent(original)
    }

    @Test fun `off-center imaginary dipole round-trips preserving imaginary-ness`() {
        // Dipole(1, 0, 1, 0, 2, 0): radius² = 0 - 1 = -1, off-center (py ≠ 0).
        val dipole = Dipole(1.0, 0.0, 1.0, 0.0, 2.0, 0.0)
        assertTrue(dipole.isImaginary)
        val original = GroupNode(
            bounds = SceneBounds(-50.0, -50.0, 100.0, 100.0),
            children = listOf(DipoleNode(dipole)),
        )
        val once = parseSvg(serializeSvg(original))
        val rt = (once.children.single() as ShapeNode).shape as Dipole
        assertEquals(dipole, rt)
        assertTrue(rt.isImaginary, "imaginary-ness was lost on dipole round-trip")
    }

    @Test fun `styled dipole round-trips fill and size`() {
        val dipole = RoundPoint(-10.0, 0.0) join RoundPoint(10.0, 0.0)
        val original = GroupNode(
            bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
            children = listOf(
                DipoleNode(
                    dipole,
                    ShapeStyle(fill = ShapeColor.rgb(0xFF, 0x00, 0x00), size = 12.0),
                ),
            ),
        )
        val once = parseSvg(serializeSvg(original))
        val shape = once.children.single() as ShapeNode
        assertEquals(dipole, shape.shape)
        assertEquals(ShapeColor.rgb(0xFF, 0x00, 0x00), shape.style.fill)
        assertEquals(12.0, shape.style.size)
    }

    @Test fun `hex shorthand canonicalises to long form on round-trip`() {
        val once = parseSvg(FILL_HEX3_SHORTHAND_SVG)
        val emitted = serializeSvg(once)
        assertTrue(emitted.contains("""fill="#ff0000""""), "actual:\n$emitted")
    }

    @Test fun `bare circle picks up SVG defaults explicitly on parse and re-emits the same attrs`() {
        // <circle/> with no fill/stroke/stroke-width — SVG defaults fill=black, stroke=none, stroke-width=1.
        // After serialize, the output must include all three attributes explicitly.
        val once = parseSvg(ONE_CIRCLE_SVG)
        val emitted = serializeSvg(once)
        assertTrue(emitted.contains("""fill="#000000""""), "actual:\n$emitted")
        assertTrue(emitted.contains("""stroke="none""""), "actual:\n$emitted")
        assertTrue(emitted.contains("""stroke-width="1""""), "actual:\n$emitted")
    }

    /* FULL-BLEED RECT ROUND-TRIPS — backdrops are ordinary rectangle Polygons */

    @Test fun `full-bleed-rect fixture parse-serialize-parse is idempotent`() {
        listOf(BACKGROUND_BLACK_THEN_POINT_SVG, BACKGROUND_OFFSET_VIEWBOX_SVG, TWO_FULL_BLEED_RECTS_SVG).forEach { svg ->
            val once = parseSvg(svg)
            val twice = parseSvg(serializeSvg(once))
            assertEquals(once, twice, "idempotency broke for fixture:\n$svg")
        }
    }

    /* ROOT-CASCADE ROUND-TRIPS — root presentation attrs are resolved into shapes at parse time
     * and re-emerge as per-shape explicit attrs on serialize. Round-trip is canonical at the model
     * level (#F00 → #ff0000 posture), not byte-verbatim at the XML level.
     */

    @Test fun `root fill is absorbed into children and not re-emitted on the root tag`() {
        val emitted = serializeSvg(parseSvg(ROOT_FILL_BARE_CIRCLE_SVG))
        // The root <svg> tag loses fill on re-emit — only viewBox and namespaces ride there.
        assertTrue(
            !Regex("""<svg[^>]*\sfill=""").containsMatchIn(emitted),
            "root <svg> tag must not carry fill on re-emit, got:\n$emitted",
        )
        // The circle carries explicit fill matching the original root fill.
        assertTrue(
            emitted.contains("""fill="#ff0000""""),
            "expected per-shape fill=#ff0000 on re-emit, got:\n$emitted",
        )
    }

    /* GROUP ROUND-TRIPS — GroupNode ↔ <g> */

    @Test fun `empty nested group round-trips`() {
        val original = GroupNode(
            bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
            children = listOf(GroupNode()),
        )
        assertEquals(original, parseSvg(serializeSvg(original)))
    }

    @Test fun `single-shape nested group round-trips`() {
        val original = GroupNode(
            bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
            children = listOf(
                GroupNode(children = listOf(FlatPointNode(FlatPoint(0.0, 0.0)))),
            ),
        )
        assertEquals(original, parseSvg(serializeSvg(original)))
        assertIdempotent(original)
    }

    @Test fun `two-level nested groups round-trip preserving tree shape`() {
        val original = GroupNode(
            bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
            children = listOf(
                GroupNode(
                    children = listOf(
                        GroupNode(children = listOf(CircleNode(Circle(0.0, 0.0, 10.0)))),
                    ),
                ),
            ),
        )
        assertEquals(original, parseSvg(serializeSvg(original)))
        assertIdempotent(original)
    }

    @Test fun `sibling groups round-trip preserving source order`() {
        val original = GroupNode(
            bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
            children = listOf(
                GroupNode(children = listOf(CircleNode(Circle(-20.0, 0.0, 5.0)))),
                GroupNode(children = listOf(FlatPointNode(FlatPoint(20.0, 0.0)))),
            ),
        )
        val once = parseSvg(serializeSvg(original))
        assertEquals(original, once)
        // Order check (defensive against any silent sort).
        assertTrue(((once.children[0] as GroupNode).children.single() as ShapeNode).shape is Circle)
        assertTrue(((once.children[1] as GroupNode).children.single() as ShapeNode).shape is FlatPoint)
    }

    @Test fun `mixed top-level ShapeNode and GroupNode round-trip in source order`() {
        // Catches a regression where the writer flattens groups or reorders nodes relative to shapes.
        val original = GroupNode(
            bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
            children = listOf(
                FlatPointNode(FlatPoint(-20.0, 0.0)),
                GroupNode(children = listOf(CircleNode(Circle(0.0, 0.0, 5.0)))),
                FlatPointNode(FlatPoint(20.0, 0.0)),
            ),
        )
        assertEquals(original, parseSvg(serializeSvg(original)))
    }

    @Test fun `nested GroupNode bounds does not survive serialize - drops to null`() {
        val original = GroupNode(
            bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
            children = listOf(
                GroupNode(
                    bounds = SceneBounds(10.0, 10.0, 50.0, 50.0),
                    children = listOf(FlatPointNode(FlatPoint(0.0, 0.0))),
                ),
            ),
        )
        val once = parseSvg(serializeSvg(original))
        assertEquals(null, (once.children.single() as GroupNode).bounds)
        assertEquals(once, parseSvg(serializeSvg(once)))
    }

    @Test fun `group fixtures are model-level idempotent`() {
        listOf(
            NESTED_GROUP_ONE_FLAT_POINT_SVG,
            EMPTY_GROUP_SVG,
            NESTED_GROUPS_DEEP_SVG,
            MULTIPLE_GROUPS_SVG,
            GROUP_FILL_CASCADE_SVG,
            GROUP_FILL_OVERRIDDEN_BY_CHILD_SVG,
            GROUP_STROKE_CASCADE_SVG,
            GROUP_STROKE_LINECAP_CASCADE_SVG,
            NESTED_GROUP_FILL_CASCADE_SVG,
            NESTED_GROUP_FILL_OVERRIDE_SVG,
            DIPOLE_GROUP_FILL_CASCADE_SVG,
        ).forEach { svg ->
            val once = parseSvg(svg)
            val twice = parseSvg(serializeSvg(once))
            assertEquals(once, twice, "idempotency broke for fixture:\n$svg")
        }
    }

    @Test fun `full-bleed rect with stroke=none round-trips as a polygon`() {
        // A rectangle Polygon always re-emits its core attrs (stroke="none" stroke-width
        // included) and re-serializes as <polygon> — rect is parse-only.
        val once = parseSvg(LEADING_FULL_BLEED_RECT_STROKE_NONE_SVG)
        val emitted = serializeSvg(once)
        assertTrue(Regex("""<polygon[^>]*stroke="none"""").containsMatchIn(emitted), "actual:\n$emitted")
        // Idempotent thereafter.
        assertEquals(once, parseSvg(emitted))
    }

    @Test fun `k tags survive the round-trip on every carrying element`() {
        listOf(TAGGED_POLYGON_SVG, TAGGED_GROUP_SVG, TAGGED_DIPOLE_SVG).forEach { svg ->
            val once = parseSvg(svg)
            val twice = parseSvg(serializeSvg(once))
            assertEquals(once, twice, "k:tags idempotency broke for fixture:\n$svg")
        }
        // Spot-check the value survives, not just equality of trees.
        val polygon = parseSvg(serializeSvg(parseSvg(TAGGED_POLYGON_SVG)))
            .children.single() as ShapeNode
        assertEquals(setOf("brick", "Steel"), polygon.tags)
    }

    @Test fun `root cascade fixtures are model-level idempotent`() {
        listOf(
            ROOT_FILL_BARE_CIRCLE_SVG,
            ROOT_FILL_FLAT_POINT_SVG,
            ROOT_FILL_ROUND_POINT_SVG,
            ROOT_STROKE_LINE_SVG,
            ROOT_STROKE_CIRCLE_SVG,
            ROOT_FILL_SHAPE_OVERRIDE_SVG,
        ).forEach { svg ->
            val once = parseSvg(svg)
            val twice = parseSvg(serializeSvg(once))
            assertEquals(once, twice, "idempotency broke for fixture:\n$svg")
        }
    }
}
