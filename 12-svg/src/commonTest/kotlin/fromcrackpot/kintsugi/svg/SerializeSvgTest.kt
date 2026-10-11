package fromcrackpot.kintsugi.svg

import fromcrackpot.kintsugi.cga.d2.products.join
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Segment
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
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SerializeSvgTest {

    /* CANONICAL OUTPUT */

    @Test fun `empty root serializes to root svg with viewBox`() {
        val out = serializeSvg(GroupNode(bounds = SceneBounds(0.0, 0.0, 100.0, 100.0)))
        assertEquals(svg("0 0 100 100"), out.normalizeXml())
    }

    @Test fun `flat shape with default style emits ShapeStyle-default black fill explicitly`() {
        val out = serializeSvg(
            GroupNode(
                bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
                children = listOf(FlatPointNode(FlatPoint(-40.0, 40.0))),
            )
        )
        // FlatPointNode ctor default seeds style = ShapeStyle.POINT → fill=black, size=8.
        assertEquals(
            svg("0 0 100 100", """<rect k:type="flat-point" x="6" y="6" width="8" height="8" fill="#000000"/>"""),
            out.normalizeXml(),
        )
    }

    @Test fun `round shape with default style emits ShapeStyle-default black fill explicitly`() {
        val out = serializeSvg(
            GroupNode(
                bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
                children = listOf(RoundPointNode(RoundPoint(0.0, 0.0))),
            )
        )
        // Null round point at origin → conformal lift gives aw = (0² + 0²)/2 = 0; k:aw always emitted.
        // size=8 → r=4.
        assertEquals(
            svg("0 0 100 100", """<circle k:type="round-point" cx="50" cy="50" r="4" k:aw="0" fill="#000000"/>"""),
            out.normalizeXml(),
        )
    }

    @Test fun `non-null round point emits k aw`() {
        val out = serializeSvg(
            GroupNode(
                bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
                children = listOf(RoundPointNode(RoundPoint(10.0, 5.0, 1.0, 67.0), ShapeStyle(fill = ShapeColor.GREEN, size = 8.0))),
            )
        ).normalizeXml()
        // CGA (10,5) → SVG (60,45); radius² = 9 (non-null) → k:aw="67".
        assertEquals(
            svg("0 0 100 100", """<circle k:type="round-point" cx="60" cy="45" r="4" k:aw="67" fill="#00ff00"/>"""),
            out,
        )
    }

    @Test fun `shapes emit in source order`() {
        val out = serializeSvg(
            GroupNode(
                bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
                children = listOf(
                    RoundPointNode(RoundPoint(10.0, 10.0)),
                    FlatPointNode(FlatPoint(0.0, 0.0)),
                ),
            )
        ).normalizeXml()
        val roundIdx = out.indexOf("""<circle k:type="round-point" cx="60" cy="40"""")
        val flatIdx = out.indexOf("""<rect k:type="flat-point" x="46" y="46"""")
        assertTrue(roundIdx > 0 && flatIdx > roundIdx, "actual:\n$out")
    }

    @Test fun `points at infinity are silently skipped`() {
        val out = serializeSvg(
            GroupNode(
                bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
                children = listOf(FlatPointNode(FlatPoint(1.0, 0.0, 0.0))),
            )
        )
        assertEquals(svg("0 0 100 100"), out.normalizeXml())
    }

    @Test fun `round point at infinity is silently skipped`() {
        // az = 0 → at infinity; cannot encode as <circle>, skip+warn.
        val out = serializeSvg(
            GroupNode(
                bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
                children = listOf(RoundPointNode(RoundPoint(1.0, 0.0, 0.0, 0.0))),
            )
        )
        assertEquals(svg("0 0 100 100"), out.normalizeXml())
    }

    @Test fun `dipole with at-infinity endpoints is silently skipped`() {
        // Dipole at infinity (zero g-carrier, gz ≠ 0 — e.g. the meet of concentric circles):
        // its start/end round points are at infinity, so no <circle> children can be encoded.
        val out = serializeSvg(
            GroupNode(
                bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
                children = listOf(DipoleNode(Dipole(0.0, 0.0, 1.0, 0.0, 0.0, 0.0))),
            )
        )
        assertEquals(svg("0 0 100 100"), out.normalizeXml())
    }

    @Test fun `null bounds throws MissingViewBoxException`() {
        assertFailsWith<MissingViewBoxException> {
            serializeSvg(GroupNode(bounds = null))
        }
    }

    /* NUMBER FORMATTING */

    @Test fun `viewBox numbers strip trailing decimal`() {
        val out = serializeSvg(GroupNode(bounds = SceneBounds(-10.0, -20.0, 40.0, 60.0)))
        assertEquals(
            true,
            out.contains("""viewBox="-10 -20 40 60""""),
            "actual:\n${out.normalizeXml()}",
        )
    }

    /* COLOR */

    @Test fun `default style emits ShapeStyle default black fill explicitly`() {
        val out = serializeSvg(
            GroupNode(
                bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
                children = listOf(FlatPointNode(FlatPoint(0.0, 0.0))),
            )
        ).normalizeXml()
        // The new contract: every shape emits explicit fill, never relies on the SVG-viewer default.
        assertTrue(out.contains("""fill="#000000""""), "actual:\n$out")
    }

    @Test fun `explicit ShapeColor NONE fill serialises as fill='none'`() {
        val out = serializeSvg(
            GroupNode(
                bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
                children = listOf(FlatPointNode(FlatPoint(0.0, 0.0), ShapeStyle(fill = ShapeColor.NONE, size = 8.0))),
            )
        ).normalizeXml()
        assertTrue(out.contains("""fill="none""""), "actual:\n$out")
    }

    @Test fun `opaque color emits 6-digit hex`() {
        val out = serializeSvg(
            GroupNode(
                bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
                children = listOf(FlatPointNode(FlatPoint(0.0, 0.0), ShapeStyle(fill = ShapeColor.rgb(0xFF, 0x00, 0x00), size = 8.0))),
            )
        ).normalizeXml()
        assertTrue(out.contains("""fill="#ff0000""""), "actual:\n$out")
    }

    @Test fun `transparent color emits 8-digit hex`() {
        val out = serializeSvg(
            GroupNode(
                bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
                children = listOf(FlatPointNode(FlatPoint(0.0, 0.0), ShapeStyle(fill = ShapeColor.rgb(0x11, 0x22, 0x33, 0x44), size = 8.0))),
            )
        ).normalizeXml()
        assertTrue(out.contains("""fill="#11223344""""), "actual:\n$out")
    }

    @Test fun `zero-alpha color canonicalises to fill='none'`() {
        val out = serializeSvg(
            GroupNode(
                bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
                children = listOf(FlatPointNode(FlatPoint(0.0, 0.0), ShapeStyle(fill = ShapeColor.rgb(0x12, 0x34, 0x56, 0x00), size = 8.0))),
            )
        ).normalizeXml()
        // #12345600 has alpha=0 — collapses to ShapeColor.NONE which formats as "none".
        assertTrue(out.contains("""fill="none""""), "actual:\n$out")
        assertTrue(!out.contains("12345600"), "actual:\n$out")
    }

    /* CIRCLES */

    @Test fun `classic circle with default style emits explicit ShapeStyle default black stroke`() {
        val out = serializeSvg(
            GroupNode(
                bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
                children = listOf(CircleNode(Circle(0.0, 0.0, 10.0))),
            )
        )
        // ShapeStyle.defaultFor(Circle) → stroke=black, fill=none, stroke-width=1.
        assertEquals(
            svg("0 0 100 100", """<circle cx="50" cy="50" r="10" fill="none" stroke="#000000" stroke-width="1"/>"""),
            out.normalizeXml(),
        )
    }

    @Test fun `circle with fill stroke stroke-width emits canonical attribute order`() {
        val out = serializeSvg(
            GroupNode(
                bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
                children = listOf(
                    CircleNode(
                        Circle(0.0, 0.0, 10.0),
                        ShapeStyle(
                            fill = ShapeColor.rgb(0xFF, 0x00, 0x00),
                            stroke = ShapeColor.rgb(0x00, 0x00, 0xFF),
                            strokeWidth = 2.0,
                        ),
                    ),
                ),
            )
        ).normalizeXml()
        assertEquals(
            svg("0 0 100 100", """<circle cx="50" cy="50" r="10" fill="#ff0000" stroke="#0000ff" stroke-width="2"/>"""),
            out,
        )
    }

    @Test fun `imaginary circle is emitted with k flags imaginary`() {
        // radius² = cx² + cy² - 2·cz·cw; cz=-2, cw=-1 → -4 (imaginary, radius 2).
        val imaginary = Circle(0.0, 0.0, -2.0, -1.0)
        assertTrue(imaginary.isImaginary)
        val out = serializeSvg(
            GroupNode(
                bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
                children = listOf(CircleNode(imaginary)),
            )
        ).normalizeXml()
        assertEquals(
            svg("0 0 100 100", """<circle cx="50" cy="50" r="2" fill="none" stroke="#000000" stroke-width="1" k:flags="imaginary"/>"""),
            out,
        )
    }

    @Test fun `inside-out circle is emitted with k flags inside-out`() {
        // Classic Circle(0,0,10) has cw = -1. Negate every component → inside-out (cw = +1).
        val classic = Circle(0.0, 0.0, 10.0)
        val insideOut = Circle(-classic.cx, -classic.cy, -classic.cz, -classic.cw)
        assertTrue(insideOut.cw > 0)
        val out = serializeSvg(
            GroupNode(
                bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
                children = listOf(CircleNode(insideOut)),
            )
        ).normalizeXml()
        assertEquals(
            svg("0 0 100 100", """<circle cx="50" cy="50" r="10" fill="none" stroke="#000000" stroke-width="1" k:flags="inside-out"/>"""),
            out,
        )
    }

    /* LINES */

    @Test fun `horizontal Line serializes to tagged line element with endpoints on worldBorder`() {
        // y = 0 in CGA-space; worldBorder for 100x100 viewBox is r = sqrt(5000) ≈ 70.71 centered at CGA origin.
        val out = serializeSvg(
            GroupNode(
                bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
                children = listOf(LineNode(Line(0.0, 1.0, 0.0))),
            )
        ).normalizeXml()
        // Use a structural check so we don't pin exact rounding digits.
        assertTrue(out.contains("""<line k:type="line" """), "actual:\n$out")
        assertTrue(out.contains(""" y1="50""""), "actual:\n$out")
        assertTrue(out.contains(""" y2="50""""), "actual:\n$out")
    }

    @Test fun `styled Line emits k type then stroke and stroke-width in canonical attribute order`() {
        val out = serializeSvg(
            GroupNode(
                bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
                children = listOf(
                    LineNode(
                        Line(0.0, 1.0, 0.0), // y = 0
                        ShapeStyle(stroke = ShapeColor.rgb(0xFF, 0x00, 0x00), strokeWidth = 2.0),
                    ),
                ),
            )
        ).normalizeXml()
        // Endpoints are (±sqrt(5000), 0) — irrational, so don't pin x1/x2. Pin attribute order + y values.
        val pattern = Regex("""<line k:type="line" x1="[^"]+" y1="50" x2="[^"]+" y2="50" stroke="#ff0000" stroke-width="2"/>""")
        assertTrue(pattern.containsMatchIn(out), "actual:\n$out")
    }

    /* SEGMENTS */

    @Test fun `Segment serializes to a bare line with no k type`() {
        // CGA endpoints (-30, 30) and (30, -30) → SVG (20, 20) and (80, 80).
        val out = serializeSvg(
            GroupNode(
                bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
                children = listOf(
                    SegmentNode(
                        Segment(FlatPoint(-30.0, 30.0), FlatPoint(30.0, -30.0)),
                        ShapeStyle(stroke = ShapeColor.rgb(0xFF, 0x00, 0x00), strokeWidth = 2.0),
                    ),
                ),
            )
        ).normalizeXml()
        assertEquals(
            svg(
                "0 0 100 100",
                """<line x1="20" y1="20" x2="80" y2="80" stroke="#ff0000" stroke-width="2"/>""",
            ),
            out,
        )
    }

    @Test fun `default Segment emits ShapeStyle default black stroke explicitly`() {
        val out = serializeSvg(
            GroupNode(
                bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
                children = listOf(SegmentNode(Segment(FlatPoint(-30.0, 0.0), FlatPoint(30.0, 0.0)))),
            )
        ).normalizeXml()
        // ShapeStyle.defaultFor(Segment) == ShapeStyle.SEGMENT → stroke=black, stroke-width=1.
        assertEquals(
            svg(
                "0 0 100 100",
                """<line x1="20" y1="50" x2="80" y2="50" stroke="#000000" stroke-width="1"/>""",
            ),
            out,
        )
    }

    @Test fun `Segment with a round strokeCap emits stroke-linecap after stroke-width`() {
        val out = serializeSvg(
            GroupNode(
                bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
                children = listOf(
                    SegmentNode(
                        Segment(FlatPoint(-30.0, 30.0), FlatPoint(30.0, -30.0)),
                        ShapeStyle(stroke = ShapeColor.rgb(0xFF, 0x00, 0x00), strokeWidth = 2.0, strokeCap = ShapeStrokeCap.ROUND),
                    ),
                ),
            )
        ).normalizeXml()
        assertEquals(
            svg(
                "0 0 100 100",
                """<line x1="20" y1="20" x2="80" y2="80" stroke="#ff0000" stroke-width="2" stroke-linecap="round"/>""",
            ),
            out,
        )
    }

    @Test fun `Segment with butt strokeCap omits stroke-linecap`() {
        // Butt is the SVG default, so it's an optional feature attribute (like k:flags) — no attribute.
        val out = serializeSvg(
            GroupNode(
                bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
                children = listOf(
                    SegmentNode(
                        Segment(FlatPoint(-30.0, 30.0), FlatPoint(30.0, -30.0)),
                        ShapeStyle(stroke = ShapeColor.rgb(0xFF, 0x00, 0x00), strokeWidth = 2.0, strokeCap = ShapeStrokeCap.BUTT),
                    ),
                ),
            )
        ).normalizeXml()
        assertTrue(!out.contains("stroke-linecap"), "actual:\n$out")
    }

    @Test fun `Line at infinity is silently skipped`() {
        val out = serializeSvg(
            GroupNode(
                bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
                children = listOf(LineNode(Line(0.0, 0.0, 1.0))),
            )
        ).normalizeXml()
        assertTrue(!out.contains("<line"), "actual:\n$out")
    }

    @Test fun `fill emitted after k aw on round point`() {
        val out = serializeSvg(
            GroupNode(
                bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
                children = listOf(RoundPointNode(RoundPoint(0.0, 0.0), ShapeStyle(fill = ShapeColor.rgb(0, 0, 0xFF), size = 8.0))),
            )
        ).normalizeXml()
        // Canonical order: k:type cx cy r k:aw fill.
        assertEquals(
            svg("0 0 100 100", """<circle k:type="round-point" cx="50" cy="50" r="4" k:aw="0" fill="#0000ff"/>"""),
            out,
        )
    }

    /* DIPOLES */

    @Test fun `dipole emits g k type dipole with two round-points and a dashed chrome line`() {
        // Horizontal dipole at CGA origin, length 20. start = (-10, 0), end = (10, 0).
        val start = RoundPoint(-10.0, 0.0)
        val end = RoundPoint(10.0, 0.0)
        val dipole = start join end
        val out = serializeSvg(
            GroupNode(
                bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
                children = listOf(DipoleNode(dipole, ShapeStyle.DIPOLE)),
            )
        ).normalizeXml()
        // start and end land at SVG (40, 50) and (60, 50). aw rides on each child. The chrome line
        // tracks style.fill (the dipole's color, matching :compose's drawDipole), and its width is
        // size/4, derived the same way :compose computes it. So ShapeStyle.DIPOLE (fill=BLACK, size=8)
        // yields stroke="#000000" stroke-width="2". Canonical order: <g> then two circles then line.
        assertEquals(
            svg(
                "0 0 100 100",
                """<g k:type="dipole">""" +
                    """<circle k:type="round-point" cx="40" cy="50" r="4" k:aw="50" fill="#000000"/>""" +
                    """<circle k:type="round-point" cx="60" cy="50" r="4" k:aw="50" fill="#000000"/>""" +
                    """<line x1="40" y1="50" x2="60" y2="50" stroke="#000000" stroke-width="2" stroke-dasharray="2 1"/>""" +
                    """</g>""",
            ),
            out,
        )
    }

    @Test fun `imaginary dipole is emitted without k flags and round-trips via start join end`() {
        // Dipole(1, 0, 1, 0, 0, 0) has radius² = -1 (imaginary). No flag needed — start ∧ end
        // reconstructs the blade canonically thanks to the sqrt(|r²|) substitution preserving gz².
        val dipole = Dipole(1.0, 0.0, 1.0, 0.0, 0.0, 0.0)
        assertTrue(dipole.isImaginary)
        val out = serializeSvg(
            GroupNode(
                bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
                children = listOf(DipoleNode(dipole, ShapeStyle.DIPOLE)),
            )
        ).normalizeXml()
        assertTrue(out.contains("""<g k:type="dipole">"""), "actual:\n$out")
        assertTrue(!out.contains("k:flags"), "imaginary dipole must not emit k:flags; actual:\n$out")
    }

    /* GROUPS (GroupNode → <g>) */

    @Test fun `bare GroupNode emits a plain g with no k type`() {
        // Bare <g> is the GroupNode carrier — no k:type, mirrors bare-<circle>-is-Circle.
        val out = serializeSvg(
            GroupNode(
                bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
                children = listOf(
                    GroupNode(children = listOf(FlatPointNode(FlatPoint(0.0, 0.0)))),
                ),
            )
        ).normalizeXml()
        assertEquals(
            svg(
                "0 0 100 100",
                """<g><rect k:type="flat-point" x="46" y="46" width="8" height="8" fill="#000000"/></g>""",
            ),
            out,
        )
    }

    @Test fun `empty GroupNode emits an empty g element`() {
        val out = serializeSvg(
            GroupNode(
                bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
                children = listOf(GroupNode()),
            )
        ).normalizeXml()
        // xmlutil emits an empty element as <g></g> (or self-closing); both forms collapse here.
        assertTrue(
            out.contains("<g></g>") || out.contains("<g/>"),
            "expected an empty <g>, got:\n$out",
        )
    }

    @Test fun `nested GroupNodes emit nested g elements in source order`() {
        val out = serializeSvg(
            GroupNode(
                bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
                children = listOf(
                    GroupNode(
                        children = listOf(
                            GroupNode(children = listOf(FlatPointNode(FlatPoint(0.0, 0.0)))),
                        ),
                    ),
                ),
            )
        ).normalizeXml()
        assertEquals(
            svg(
                "0 0 100 100",
                """<g><g><rect k:type="flat-point" x="46" y="46" width="8" height="8" fill="#000000"/></g></g>""",
            ),
            out,
        )
    }

    @Test fun `nested GroupNode bounds is dropped on serialize`() {
        val out = serializeSvg(
            GroupNode(
                bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
                children = listOf(
                    GroupNode(
                        bounds = SceneBounds(0.0, 0.0, 50.0, 50.0),
                        children = listOf(FlatPointNode(FlatPoint(0.0, 0.0))),
                    ),
                ),
            )
        ).normalizeXml()
        val reloaded = parseSvg(out)
        val nested = reloaded.children.single() as GroupNode
        assertEquals(null, nested.bounds)
    }

    @Test fun `GroupNode with k type dipole shape still serializes as a dipole g, not a plain g`() {
        // DipoleNode sits at the same level as a nested GroupNode; make sure the writer's
        // node dispatch routes through writeDipoleGroup (k:type="dipole" carrier) and NOT through
        // writeGroupElement (the new plain-<g> path).
        val start = RoundPoint(-10.0, 0.0)
        val end = RoundPoint(10.0, 0.0)
        val out = serializeSvg(
            GroupNode(
                bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
                children = listOf(DipoleNode(start join end, ShapeStyle.DIPOLE)),
            )
        ).normalizeXml()
        assertTrue(out.contains("""<g k:type="dipole">"""), "actual:\n$out")
        assertTrue(!out.contains("<g>"), "Dipole carrier must not collide with plain-<g>, got:\n$out")
    }

    /* K:TAGS — optional feature attribute, emitted only when non-empty */

    @Test fun `tagged polygon emits k tags space-joined after style attrs`() {
        val out = serializeSvg(
            GroupNode(
                bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
                children = listOf(
                    PolygonNode(
                        Polygon(listOf(FlatPoint(-30.0, -30.0), FlatPoint(10.0, -30.0), FlatPoint(-30.0, 0.0))),
                        ShapeStyle.POLYGON,
                        tags = setOf("brick", "steel"),
                    ),
                ),
            )
        ).normalizeXml()
        assertTrue(out.contains("""k:tags="brick steel""""), "actual:\n$out")
    }

    @Test fun `untagged shape emits no k tags attribute`() {
        val out = serializeSvg(
            GroupNode(
                bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
                children = listOf(CircleNode(Circle(0.0, 0.0, 10.0))),
            )
        )
        assertTrue(!out.contains("k:tags"), "actual:\n$out")
    }

    @Test fun `tagged dipole emits k tags on the carrier g only`() {
        val start = RoundPoint(-10.0, 0.0)
        val end = RoundPoint(10.0, 0.0)
        val out = serializeSvg(
            GroupNode(
                bounds = SceneBounds(0.0, 0.0, 100.0, 100.0),
                children = listOf(DipoleNode(start join end, ShapeStyle.DIPOLE, tags = setOf("portal"))),
            )
        ).normalizeXml()
        assertTrue(out.contains("""<g k:type="dipole" k:tags="portal">"""), "actual:\n$out")
        // The synthesized round-point children must not repeat the tags.
        assertTrue(Regex("""k:tags""").findAll(out).count() == 1, "tags leaked onto children:\n$out")
    }
}

/** Build the canonical serialized document for the given viewBox + body, including the `xmlns:k` declaration. */
private fun svg(viewBox: String, body: String = ""): String =
    """<?xml version='1.0'?><svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="$viewBox">$body</svg>"""

/** Collapse whitespace between tags so tests aren't sensitive to xmlutil's pretty-print indenting. */
private fun String.normalizeXml(): String =
    Regex(">\\s+<").replace(this.trim(), "><")
