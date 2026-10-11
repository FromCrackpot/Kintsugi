package fromcrackpot.kintsugi.scenegraph.nodes

import fromcrackpot.kintsugi.cga.d2.products.join
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polyline
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Segment
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Circle
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Direction
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Line
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.RoundPoint
import fromcrackpot.kintsugi.cga.d2.transforms.F
import fromcrackpot.kintsugi.cga.d2.transforms.T
import fromcrackpot.kintsugi.cga.d2.transforms.Versor
import fromcrackpot.kintsugi.cga.d2.transforms.composites.times
import fromcrackpot.kintsugi.cga.d2.transforms.primitives.times
import fromcrackpot.kintsugi.scenegraph.SceneBounds
import fromcrackpot.kintsugi.scenegraph.nodes.composites.PolygonNode
import fromcrackpot.kintsugi.scenegraph.nodes.composites.PolylineNode
import fromcrackpot.kintsugi.scenegraph.nodes.composites.SegmentNode
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.CircleNode
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.DipoleNode
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.FlatPointNode
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.LineNode
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.RoundPointNode
import fromcrackpot.kintsugi.scenegraph.style.ShapeStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertSame

class NodeTest {

    /*
    LEAF — node * Versor
     */

    @Test
    fun `transforms the geometry and keeps the style untouched`() {
        val style = ShapeStyle.POINT.copy(size = 12.0)
        val moved = FlatPointNode(FlatPoint(1.0, 2.0), style) * T(3.0, 4.0)
        assertEquals(FlatPointNode(FlatPoint(4.0, 6.0), style), moved)
    }

    @Test
    fun `transforms every leaf type, node-preserving`() {
        val t = T(3.0, 4.0)
        // The declared types pin the generic copy-routed returns — each leaf comes back as itself.
        val flat: FlatPointNode = FlatPointNode(FlatPoint(1.0, 2.0)) * t
        assertEquals(FlatPointNode(FlatPoint(4.0, 6.0)), flat)
        val round: RoundPointNode = RoundPointNode(RoundPoint(1.0, 2.0)) * t
        assertEquals(RoundPointNode(RoundPoint(4.0, 6.0)), round)
        val line: LineNode = LineNode(Line(1.0, 0.0, -2.0)) * t
        assertEquals(LineNode(Line(1.0, 0.0, -5.0)), line)
        val circle: CircleNode = CircleNode(Circle(1.0, 2.0, 5.0)) * t
        assertEquals(CircleNode(Circle(4.0, 6.0, 5.0)), circle)
        val dipole: DipoleNode = DipoleNode(RoundPoint(0.0, 0.0) join RoundPoint(2.0, 0.0)) * t
        assertEquals(DipoleNode(RoundPoint(3.0, 4.0) join RoundPoint(5.0, 4.0)), dipole)
        val segment: SegmentNode = SegmentNode(Segment(FlatPoint(0.0, 0.0), FlatPoint(2.0, 0.0))) * t
        assertEquals(SegmentNode(Segment(FlatPoint(3.0, 4.0), FlatPoint(5.0, 4.0))), segment)
        val polygon: PolygonNode = PolygonNode(Polygon.Rectangle(FlatPoint(1.0, 2.0), FlatPoint(4.0, 6.0))) * t
        assertEquals(PolygonNode(Polygon.Rectangle(FlatPoint(4.0, 6.0), FlatPoint(7.0, 10.0))), polygon)
        val polyline: PolylineNode =
            PolylineNode(Polyline(listOf(FlatPoint(0.0, 0.0), FlatPoint(2.0, 0.0), FlatPoint(2.0, 2.0)))) * t
        assertEquals(
            PolylineNode(Polyline(listOf(FlatPoint(3.0, 4.0), FlatPoint(5.0, 4.0), FlatPoint(5.0, 6.0)))),
            polyline,
        )
    }

    @Test
    fun `preserves tags alongside the style`() {
        val tagged = FlatPointNode(FlatPoint(1.0, 2.0), tags = setOf("brick", "steel"))
        val moved = tagged * T(3.0, 4.0)
        assertEquals(FlatPointNode(FlatPoint(4.0, 6.0), tagged.style, setOf("brick", "steel")), moved)
    }

    @Test
    fun `applies a flector — reflection across a line`() {
        val mirror = F(Line(1.0, 0.0, 0.0))   // the line x = 0
        val moved = FlatPointNode(FlatPoint(1.0, 2.0)) * mirror
        assertEquals(FlatPointNode(FlatPoint(-1.0, 2.0)), moved)
    }

    @Test
    fun `applies a stored Versor through the generic dispatch`() {
        val motion: Versor = T(3.0, 4.0)
        val moved: FlatPointNode = FlatPointNode(FlatPoint(1.0, 2.0)) * motion
        assertEquals(FlatPointNode(FlatPoint(4.0, 6.0)), moved)
    }

    @Test
    fun `moves a leaf typed only as ShapeNode through the family dispatch`() {
        val node: ShapeNode = FlatPointNode(FlatPoint(1.0, 2.0))
        val moved: ShapeNode = node * T(3.0, 4.0)
        assertEquals(FlatPointNode(FlatPoint(4.0, 6.0)), moved)
    }

    /*
    LEAF — node.canonical
     */

    @Test
    fun `canonicalizes node-preserving — style and tags ride along`() {
        val style = ShapeStyle.POINT.copy(size = 12.0)
        val c = LineNode(Line(-1.0, 0.0, 2.0), style, setOf("wall")).canonical
        // The runtime class pins the copy-routed rebuild — the leaf comes back as itself.
        assertIs<LineNode>(c)
        assertEquals(LineNode(Line(1.0, 0.0, -2.0), style, setOf("wall")), c)
    }

    @Test
    fun `an already-canonical leaf returns this`() {
        val line = LineNode(Line(1.0, 0.0, -2.0))
        assertSame(line, line.canonical)
        val circle = CircleNode(Circle(1.0, 2.0, 5.0))   // classic orientation is canonical
        assertSame(circle, circle.canonical)
    }

    @Test
    fun `canonicalizes every canonical-bearing leaf type, node-preserving and idempotent`() {
        // The runtime classes pin the copy-routed rebuild — each leaf comes back as itself.
        val line = LineNode(Line(-1.0, 0.0, 2.0)).canonical
        assertIs<LineNode>(line)
        assertSame(line, line.canonical)
        val dipole = DipoleNode(RoundPoint(0.0, 0.0) join RoundPoint(2.0, 0.0)).canonical
        assertIs<DipoleNode>(dipole)
        assertSame(dipole, dipole.canonical)
        val circle = CircleNode(Circle(1.0, 2.0, 5.0)).canonical
        assertIs<CircleNode>(circle)
        assertSame(circle, circle.canonical)
        val segment = SegmentNode(Segment(FlatPoint(0.0, 0.0), FlatPoint(2.0, 0.0))).canonical
        assertIs<SegmentNode>(segment)
        assertSame(segment, segment.canonical)
    }

    @Test
    fun `orientation twins with equal style compare equal through canonical`() {
        val lineA = LineNode(Line(1.0, 0.0, -2.0))
        val lineB = LineNode(Line(-1.0, 0.0, 2.0))
        assertNotEquals(lineA, lineB)
        assertEquals(lineA.canonical, lineB.canonical)

        val dipoleA = DipoleNode(RoundPoint(0.0, 0.0) join RoundPoint(2.0, 0.0))
        val dipoleB = DipoleNode(RoundPoint(2.0, 0.0) join RoundPoint(0.0, 0.0))
        assertNotEquals(dipoleA, dipoleB)
        assertEquals(dipoleA.canonical, dipoleB.canonical)

        val segmentA = SegmentNode(Segment(FlatPoint(0.0, 0.0), FlatPoint(2.0, 0.0)))
        val segmentB = SegmentNode(Segment(FlatPoint(2.0, 0.0), FlatPoint(0.0, 0.0)))
        assertNotEquals(segmentA, segmentB)
        assertEquals(segmentA.canonical, segmentB.canonical)
    }

    @Test
    fun `orientation twins with different styles stay unequal through canonical`() {
        val a = LineNode(Line(1.0, 0.0, -2.0))
        val b = LineNode(Line(-1.0, 0.0, 2.0), ShapeStyle.NONE)
        assertNotEquals(a.canonical, b.canonical)
    }

    @Test
    fun `shape-canonical crosses the node-plain divide`() {
        val node = LineNode(Line(-1.0, 0.0, 2.0))
        assertEquals(Line(-1.0, 0.0, 2.0).canonical, (node.shape as Line).canonical)
    }

    /*
    TREE — GroupNode * Versor
     */

    @Test
    fun `transforms every leaf, preserving structure and source order`() {
        val bounds = SceneBounds(-50.0, -50.0, 100.0, 100.0)
        val root = GroupNode(
            children = listOf(
                FlatPointNode(FlatPoint(1.0, 0.0)),
                GroupNode(children = listOf(FlatPointNode(FlatPoint(2.0, 0.0)))),
                FlatPointNode(FlatPoint(3.0, 0.0)),
            ),
            bounds = bounds,
        )

        val moved = root * T(0.0, 5.0)

        val expected = GroupNode(
            children = listOf(
                FlatPointNode(FlatPoint(1.0, 5.0)),
                GroupNode(children = listOf(FlatPointNode(FlatPoint(2.0, 5.0)))),
                FlatPointNode(FlatPoint(3.0, 5.0)),
            ),
            bounds = bounds,
        )
        assertEquals(expected, moved)
    }

    @Test
    fun `reflects a whole tree across a line`() {
        val root = GroupNode(
            children = listOf(
                FlatPointNode(FlatPoint(1.0, 0.0)),
                GroupNode(children = listOf(FlatPointNode(FlatPoint(2.0, 3.0)))),
            ),
        )
        val moved = root * F(Line(1.0, 0.0, 0.0))   // mirror x = 0
        val expected = GroupNode(
            children = listOf(
                FlatPointNode(FlatPoint(-1.0, 0.0)),
                GroupNode(children = listOf(FlatPointNode(FlatPoint(-2.0, 3.0)))),
            ),
        )
        assertEquals(expected, moved)
    }

    @Test
    fun `bounds pass through unchanged — content moves within the same viewport`() {
        val bounds = SceneBounds(0.0, 0.0, 80.0, 40.0)
        val moved = GroupNode(bounds = bounds) * T(100.0, 100.0)
        assertEquals(bounds, moved.bounds)
    }

    @Test
    fun `nested group nodes stay group nodes`() {
        val root = GroupNode(
            children = listOf(GroupNode(children = listOf(FlatPointNode(FlatPoint(0.0, 0.0))))),
        )
        val moved = root * T(1.0, 1.0)
        assertIs<GroupNode>(moved.children.single())
    }

    @Test
    fun `zero translation is the identity`() {
        val root = GroupNode(
            children = listOf(
                CircleNode(Circle(1.0, 2.0, 5.0)),
                PolygonNode(Polygon.Rectangle(FlatPoint(0.0, 0.0), FlatPoint(2.0, 1.0))),
            ),
            bounds = SceneBounds(-10.0, -10.0, 20.0, 20.0),
        )
        assertEquals(root, root * T(Direction(0.0, 0.0)))
    }
}
