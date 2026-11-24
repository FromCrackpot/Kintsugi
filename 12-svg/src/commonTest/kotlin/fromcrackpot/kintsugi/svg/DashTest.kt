package fromcrackpot.kintsugi.svg

import fromcrackpot.kintsugi.cga.d2.operations.products.join
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polyline
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Segment
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Circle
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Dipole
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.RoundPoint
import fromcrackpot.kintsugi.scenegraph.SceneBounds
import fromcrackpot.kintsugi.scenegraph.nodes.GroupNode
import fromcrackpot.kintsugi.scenegraph.nodes.Node
import fromcrackpot.kintsugi.scenegraph.nodes.ShapeNode
import fromcrackpot.kintsugi.scenegraph.nodes.composites.PolygonNode
import fromcrackpot.kintsugi.scenegraph.nodes.composites.PolylineNode
import fromcrackpot.kintsugi.scenegraph.nodes.composites.SegmentNode
import fromcrackpot.kintsugi.scenegraph.nodes.flattenShapes
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.CircleNode
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.DipoleNode
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.FlatPointNode
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.LineNode
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.RoundPointNode
import fromcrackpot.kintsugi.scenegraph.style.ImaginaryChrome
import fromcrackpot.kintsugi.scenegraph.style.ShapeColor
import fromcrackpot.kintsugi.scenegraph.style.ShapeDash
import fromcrackpot.kintsugi.scenegraph.style.ShapeStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * `stroke-dasharray`/`stroke-dashoffset` and the `k:chrome` contract: an authored dash persists
 * as is, a derived default is written for viewers and listed in `k:chrome`, and the parser drops
 * every `k:chrome`-listed attribute so a default never turns into an authored value.
 */
class DashTest {

    private val bounds = SceneBounds(0.0, 0.0, 100.0, 100.0)
    private val imaginaryCircle = Circle(0.0, 0.0, -2.0, -1.0)
    private val imaginaryDipole = Dipole(1.0, 0.0, 1.0, 0.0, 0.0, 0.0)
    private val imaginaryPoint = RoundPoint(10.0, 5.0, 1.0, 60.5)
    private val authored = ShapeDash(listOf(3.0, 1.0), offset = 0.5)

    private fun scene(vararg nodes: Node) = GroupNode(children = nodes.toList(), bounds = bounds)
    private fun roundTrip(scene: GroupNode): GroupNode = parseSvg(serializeSvg(scene))
    private fun GroupNode.leaves(): List<ShapeNode> = flattenShapes()

    /* ROUND-TRIP: UNSET */

    @Test fun `unset dash survives save and load on every imaginary shape`() {
        val original = scene(
            CircleNode(imaginaryCircle),
            SegmentNode(imaginaryDipole.toSegment),
            DipoleNode(imaginaryDipole),
            RoundPointNode(imaginaryPoint),
        )
        val out = serializeSvg(original)
        assertTrue(out.contains("k:chrome"), "the derived default is written as chrome; actual:\n$out")

        val once = roundTrip(original)
        once.leaves().forEach { assertNull(it.style.dash, "derived default came back authored on $it") }
        assertEquals(original, once)
        assertEquals(once, roundTrip(once))
    }

    @Test fun `an unset dash stays unset when the shape later turns real`() {
        val reloaded = roundTrip(scene(CircleNode(imaginaryCircle))).leaves().single()
        val real = CircleNode(Circle(0.0, 0.0, 5.0), reloaded.style)
        val out = serializeSvg(scene(real))
        assertFalse(out.contains("stroke-dasharray"), "actual:\n$out")
        assertFalse(out.contains("k:chrome"), "actual:\n$out")
    }

    /* ROUND-TRIP: AUTHORED */

    @Test fun `authored dash wins on every imaginary shape and survives`() {
        val original = scene(
            CircleNode(imaginaryCircle, ShapeStyle.CIRCLE.copy(dash = authored)),
            SegmentNode(imaginaryDipole.toSegment, ShapeStyle.SEGMENT.copy(dash = authored)),
            DipoleNode(imaginaryDipole, ShapeStyle.DIPOLE.copy(dash = authored)),
            RoundPointNode(imaginaryPoint, ShapeStyle.POINT.copy(dash = authored)),
        )
        val out = serializeSvg(original)
        assertFalse(out.contains("stroke-dasharray=\"4 2\""), "the default must not be written; actual:\n$out")
        assertFalse(out.contains(Regex("k:chrome=\"[^\"]*stroke-dash")), "authored dash is never chrome; actual:\n$out")

        val once = roundTrip(original)
        once.leaves().forEach { assertEquals(authored, it.style.dash, "on $it") }
        assertEquals(original, once)
    }

    @Test fun `authored SOLID on an imaginary shape persists as none`() {
        val original = scene(CircleNode(imaginaryCircle, ShapeStyle.CIRCLE.copy(dash = ShapeDash.SOLID)))
        val out = serializeSvg(original)
        assertTrue(out.contains("stroke-dasharray=\"none\""), "actual:\n$out")
        assertFalse(out.contains("k:chrome"), "actual:\n$out")
        assertEquals(ShapeDash.SOLID, roundTrip(original).leaves().single().style.dash)
    }

    @Test fun `authored dash survives on every real shape`() {
        val original = scene(
            FlatPointNode(FlatPoint(-10.0, 10.0), ShapeStyle.POINT.copy(dash = authored)),
            LineNode(FlatPoint(0.0, 0.0) join FlatPoint(10.0, 5.0), ShapeStyle.LINE.copy(dash = authored)),
            RoundPointNode(RoundPoint(10.0, -10.0), ShapeStyle.POINT.copy(dash = authored)),
            DipoleNode(RoundPoint(-10.0, 0.0) join RoundPoint(10.0, 0.0), ShapeStyle.DIPOLE.copy(dash = authored)),
            CircleNode(Circle(0.0, 0.0, 20.0), ShapeStyle.CIRCLE.copy(dash = authored)),
            SegmentNode(Segment(FlatPoint(-5.0, -5.0), FlatPoint(5.0, -5.0)), ShapeStyle.SEGMENT.copy(dash = authored)),
            PolylineNode(
                Polyline(listOf(FlatPoint(0.0, 0.0), FlatPoint(10.0, 0.0), FlatPoint(10.0, 10.0))),
                ShapeStyle.POLYLINE.copy(dash = authored),
            ),
            PolygonNode(
                Polygon.Rectangle(FlatPoint(-20.0, -20.0), FlatPoint(-10.0, -15.0)),
                ShapeStyle.POLYGON.copy(dash = authored),
            ),
        )
        val once = roundTrip(original)
        once.leaves().forEach { assertEquals(authored, it.style.dash, "on $it") }
        assertEquals(original, once)
    }

    /* SERIALIZE */

    @Test fun `authored dash is written after the cap and before the flags`() {
        val out = serializeSvg(scene(CircleNode(imaginaryCircle, ShapeStyle.CIRCLE.copy(dash = authored))))
        assertTrue(
            out.contains(
                """<circle cx="50" cy="50" r="2" fill="none" stroke="#000000" stroke-width="1" """ +
                    """stroke-dasharray="3 1" stroke-dashoffset="0.5" k:flags="imaginary"/>"""
            ),
            "actual:\n$out",
        )
    }

    @Test fun `real dipole's authored dash rides the carrier g and the chrome line inherits it`() {
        val out = serializeSvg(
            scene(DipoleNode(RoundPoint(-10.0, 0.0) join RoundPoint(10.0, 0.0), ShapeStyle.DIPOLE.copy(dash = authored)))
        )
        assertTrue(out.contains("""<g k:type="dipole" stroke-dasharray="3 1" stroke-dashoffset="0.5">"""), "actual:\n$out")
        assertTrue(out.contains("""<line x1="40" y1="50" x2="60" y2="50" stroke="#000000" stroke-width="2"/>"""), "actual:\n$out")
    }

    @Test fun `imaginary round point writes a ring with the default dash as chrome`() {
        val out = serializeSvg(scene(RoundPointNode(imaginaryPoint, ShapeStyle.POINT.copy(fill = ShapeColor.rgb(0xff, 0, 0)))))
        assertTrue(
            out.contains(
                """<circle k:type="round-point" cx="60" cy="45" r="4" k:aw="60.5" fill="#ff0000" fill-opacity="0" """ +
                    """stroke="#ff0000" stroke-width="2" stroke-dasharray="4 2" """ +
                    """k:chrome="fill-opacity stroke stroke-width stroke-dasharray"/>"""
            ),
            "actual:\n$out",
        )
    }

    @Test fun `real shapes with no dash write no dash attributes`() {
        val out = serializeSvg(
            scene(
                CircleNode(Circle(0.0, 0.0, 20.0)),
                RoundPointNode(RoundPoint(10.0, -10.0)),
                SegmentNode(Segment(FlatPoint(-5.0, -5.0), FlatPoint(5.0, -5.0))),
            )
        )
        assertFalse(out.contains("stroke-dash"), "actual:\n$out")
        assertFalse(out.contains("k:chrome"), "actual:\n$out")
    }

    /* PARSE */

    @Test fun `stroke-dasharray and stroke-dashoffset inherit through g separately`() {
        val (inherited, overridden, none) = parseSvg(DASH_INHERITANCE_SVG).leaves()
        assertEquals(ShapeDash(listOf(4.0, 2.0), offset = 1.0), inherited.style.dash)
        assertEquals(ShapeDash(listOf(3.0, 3.0), offset = 1.0), overridden.style.dash)
        assertEquals(ShapeDash.SOLID, none.style.dash)
    }

    @Test fun `k chrome listed attributes are dropped on parse and the cascade applies`() {
        val circle = parseSvg(CHROME_DROPPED_SVG).leaves().single()
        assertEquals(ShapeColor.rgb(0, 0xff, 0), circle.style.stroke)
        assertEquals(3.0, circle.style.strokeWidth, "an unlisted attribute still reads")
        assertNull(circle.style.dash)
    }

    @Test fun `k chrome on a g drops the attribute from the inherited layer`() {
        assertEquals(ShapeDash(listOf(1.0, 1.0)), parseSvg(CHROME_ON_GROUP_SVG).leaves().single().style.dash)
    }

    @Test fun `unknown k chrome tokens are ignored`() {
        val circle = parseSvg(CHROME_UNKNOWN_TOKEN_SVG).leaves().single() as CircleNode
        assertEquals(10.0, circle.radius, 1e-9)
        assertEquals(ShapeDash(listOf(2.0, 1.0)), circle.style.dash)
    }

    @Test fun `unrecognized dash values fall back to the cascade`() {
        val (units, negative, badOffset, offsetOnly) = parseSvg(DASH_MALFORMED_SVG).leaves()
        assertEquals(ShapeDash(listOf(6.0, 3.0)), units.style.dash)
        assertEquals(ShapeDash(listOf(6.0, 3.0)), negative.style.dash)
        assertEquals(ShapeDash(listOf(6.0, 3.0)), badOffset.style.dash)
        assertNull(offsetOnly.style.dash)
    }

    @Test fun `the parsed imaginary default resolves to the shared constant`() {
        val circle = roundTrip(scene(CircleNode(imaginaryCircle))).leaves().single()
        assertEquals(ImaginaryChrome.DASH, circle.style.dashFor(circle as Circle))
    }
}
