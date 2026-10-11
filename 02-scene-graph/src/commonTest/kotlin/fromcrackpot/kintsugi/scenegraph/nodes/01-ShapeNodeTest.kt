package fromcrackpot.kintsugi.scenegraph.nodes

import fromcrackpot.kintsugi.cga.d2.products.meet
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polyline
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Segment
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.*
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Direction
import fromcrackpot.kintsugi.scenegraph.nodes.composites.PolygonNode
import fromcrackpot.kintsugi.scenegraph.nodes.composites.PolylineNode
import fromcrackpot.kintsugi.scenegraph.nodes.composites.SegmentNode
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.CircleNode
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.DipoleNode
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.FlatPointNode
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.LineNode
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.RoundPointNode
import fromcrackpot.kintsugi.scenegraph.style.ShapeColor
import fromcrackpot.kintsugi.scenegraph.style.ShapeStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertSame

class ShapeNodeTest {

    /* CTOR DEFAULTS — style seeded from the per-shape constants */

    @Test fun `flat point node gets POINT default style`() {
        assertEquals(ShapeStyle.POINT, FlatPointNode(FlatPoint(0.0, 0.0)).style)
    }

    @Test fun `round point node gets POINT default style`() {
        assertEquals(ShapeStyle.POINT, RoundPointNode(RoundPoint(0.0, 0.0)).style)
    }

    @Test fun `circle node gets CIRCLE default style`() {
        assertEquals(ShapeStyle.CIRCLE, CircleNode(Circle(0.0, 0.0, 1.0)).style)
    }

    @Test fun `line node gets LINE default style`() {
        assertEquals(ShapeStyle.LINE, LineNode(Line(0.0, 1.0, 0.0)).style)
    }

    @Test fun `dipole node gets DIPOLE default style`() {
        assertEquals(ShapeStyle.DIPOLE, DipoleNode(Dipole(1.0, 0.0, 0.0, 0.0, 0.0, 1.0)).style)
    }

    @Test fun `defaultFor dispatches on a leaf as its shape`() {
        val node = PolygonNode(Polygon.Rectangle(FlatPoint(0.0, 0.0), FlatPoint(2.0, 1.0)))
        assertEquals(ShapeStyle.POLYGON, ShapeStyle.defaultFor(node))
    }

    /* EXPLICIT OVERRIDE */

    @Test fun `explicit style overrides constructor default`() {
        val custom = ShapeStyle(fill = ShapeColor.rgb(0xFF, 0, 0))
        assertEquals(custom, FlatPointNode(FlatPoint(0.0, 0.0), style = custom).style)
    }

    @Test fun `ShapeStyle NONE is the idiom for explicitly invisible`() {
        assertEquals(ShapeStyle.NONE, FlatPointNode(FlatPoint(0.0, 0.0), style = ShapeStyle.NONE).style)
    }

    /* TAGS */

    @Test fun `tags default to empty`() {
        assertEquals(emptySet(), FlatPointNode(FlatPoint(0.0, 0.0)).tags)
    }

    @Test fun `explicit tags are carried as given`() {
        val node = FlatPointNode(FlatPoint(0.0, 0.0), tags = setOf("brick", "steel"))
        assertEquals(setOf("brick", "steel"), node.tags)
    }

    /* COPY CTOR — a node-typed argument preserves the decoration */

    @Test fun `flat point node copy keeps the source's style and tags`() {
        val source = FlatPointNode(FlatPoint(1.0, 2.0), ShapeStyle.POINT.copy(strokeWidth = 3.0), setOf("wall"))
        val copy = FlatPointNode(source)
        assertSame(source.style, copy.style)
        assertEquals(source, copy)
    }

    @Test fun `round point node copy keeps the source's style and tags`() {
        val source = RoundPointNode(RoundPoint(1.0, 2.0), ShapeStyle.POINT.copy(strokeWidth = 3.0), setOf("wall"))
        val copy = RoundPointNode(source)
        assertSame(source.style, copy.style)
        assertEquals(source, copy)
    }

    @Test fun `line node copy keeps the source's style and tags`() {
        val source = LineNode(Line(0.0, 1.0, -2.0), ShapeStyle.LINE.copy(strokeWidth = 3.0), setOf("wall"))
        val copy = LineNode(source)
        assertSame(source.style, copy.style)
        assertEquals(source, copy)
    }

    @Test fun `dipole node copy keeps the source's style and tags`() {
        val source = DipoleNode(Dipole(1.0, 0.0, 0.0, 0.0, 0.0, 1.0), ShapeStyle.DIPOLE.copy(strokeWidth = 3.0), setOf("wall"))
        val copy = DipoleNode(source)
        assertSame(source.style, copy.style)
        assertEquals(source, copy)
    }

    @Test fun `circle node copy keeps the source's style and tags`() {
        val source = CircleNode(Circle(0.0, 0.0, 1.0), ShapeStyle.CIRCLE.copy(strokeWidth = 3.0), setOf("wall"))
        val copy = CircleNode(source)
        assertSame(source.style, copy.style)
        assertEquals(source, copy)
    }

    @Test fun `segment node copy keeps the source's style and tags`() {
        val segment = Segment(FlatPoint(0.0, 0.0), FlatPoint(1.0, 0.0))
        val source = SegmentNode(segment, ShapeStyle.SEGMENT.copy(strokeWidth = 3.0), setOf("wall"))
        val copy = SegmentNode(source)
        assertSame(source.style, copy.style)
        assertEquals(source, copy)
    }

    @Test fun `polyline node copy keeps the source's style and tags`() {
        val polyline = Polyline(listOf(FlatPoint(0.0, 0.0), FlatPoint(1.0, 0.0), FlatPoint(1.0, 1.0)))
        val source = PolylineNode(polyline, ShapeStyle.POLYLINE.copy(strokeWidth = 3.0), setOf("wall"))
        val copy = PolylineNode(source)
        assertSame(source.style, copy.style)
        assertEquals(source, copy)
    }

    @Test fun `polygon node copy keeps the source's style and tags`() {
        val quad = Polygon.Rectangle(FlatPoint(0.0, 0.0), FlatPoint(2.0, 1.0))
        val source = PolygonNode(quad, ShapeStyle.POLYGON.copy(strokeWidth = 3.0), setOf("wall"))
        val copy = PolygonNode(source)
        assertSame(source.style, copy.style)
        assertEquals(source, copy)
    }

    @Test fun `copy with a new style keeps the source's tags`() {
        val source = PolygonNode(Polygon.Rectangle(FlatPoint(0.0, 0.0), FlatPoint(2.0, 1.0)), tags = setOf("brick"))
        val repainted = PolygonNode(source, ShapeStyle.NONE)
        assertEquals(ShapeStyle.NONE, repainted.style)
        assertEquals(setOf("brick"), repainted.tags)
    }

    @Test fun `copy with new tags keeps the source's style`() {
        val style = ShapeStyle.POLYGON.copy(strokeWidth = 3.0)
        val source = PolygonNode(Polygon.Rectangle(FlatPoint(0.0, 0.0), FlatPoint(2.0, 1.0)), style)
        val retagged = PolygonNode(source, tags = setOf("brick"))
        assertSame(style, retagged.style)
        assertEquals(setOf("brick"), retagged.tags)
    }

    @Test fun `a shape-typed argument selects the wrap constructor and its default style`() {
        val source = PolygonNode(
            Polygon.Rectangle(FlatPoint(0.0, 0.0), FlatPoint(2.0, 1.0)), ShapeStyle.NONE, setOf("brick"),
        )
        val shapeTyped: Polygon = source
        val rewrapped = PolygonNode(shapeTyped)
        assertEquals(ShapeStyle.POLYGON, rewrapped.style)
        assertEquals(emptySet(), rewrapped.tags)
    }

    /* TRANSPARENCY — a leaf is its shape */

    @Test fun `shape properties read directly off the leaf`() {
        val node = LineNode(Line(0.0, 1.0, -2.0))
        assertEquals(Direction(-1.0, 0.0), node.attitude)
        val quad = PolygonNode(Polygon.Rectangle(FlatPoint(0.0, 0.0), FlatPoint(4.0, 2.0)))
        assertEquals(FlatPoint(2.0, 1.0), quad.centroid)
    }

    @Test fun `a leaf participates in geometric operations as its shape — the result is plain`() {
        val a = LineNode(Line(1.0, 0.0, 0.0))
        val b = LineNode(Line(0.0, 1.0, 0.0))
        assertEquals(FlatPoint(0.0, 0.0), a meet b)
    }

    @Test fun `shape sheds the decoration to a plain shape`() {
        val node = LineNode(Line(0.0, 1.0, -2.0), ShapeStyle.LINE.copy(strokeWidth = 3.0))
        assertEquals(Line(0.0, 1.0, -2.0), node.shape)
    }

    /* IDENTITY — exact-class, style-inclusive */

    @Test fun `leaves are equal when geometry, style, and tags all match`() {
        val style = ShapeStyle.LINE.copy(strokeWidth = 3.0)
        val a = LineNode(Line(0.0, 1.0, -2.0), style, setOf("wall"))
        val b = LineNode(Line(0.0, 1.0, -2.0), style, setOf("wall"))
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test fun `leaves with the same geometry but different styles are unequal`() {
        val line = Line(0.0, 1.0, -2.0)
        assertNotEquals(LineNode(line), LineNode(line, ShapeStyle.LINE.copy(strokeWidth = 3.0)))
    }

    @Test fun `leaves with the same geometry but different tags are unequal`() {
        val line = Line(0.0, 1.0, -2.0)
        assertNotEquals(LineNode(line), LineNode(line, tags = setOf("wall")))
    }

    @Test fun `a leaf never equals the plain shape, in either direction`() {
        val line = Line(0.0, 1.0, -2.0)
        val node = LineNode(line)
        assertNotEquals<Any>(line, node)
        assertNotEquals<Any>(node, line)
    }

    @Test fun `leaves of different node classes are unequal`() {
        val flat = FlatPointNode(FlatPoint(1.0, 2.0))
        val round = RoundPointNode(RoundPoint(1.0, 2.0))
        assertNotEquals<Any>(flat, round)
    }
}
