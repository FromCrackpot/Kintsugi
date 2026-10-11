package fromcrackpot.kintsugi.scenegraph.nodes

import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.transforms.T
import fromcrackpot.kintsugi.scenegraph.SceneBounds
import fromcrackpot.kintsugi.scenegraph.nodes.composites.PolygonNode
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.FlatPointNode
import fromcrackpot.kintsugi.scenegraph.style.ShapeColor
import fromcrackpot.kintsugi.scenegraph.style.ShapeStyle
import fromcrackpot.kintsugi.scenegraph.worldBorder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GroupNodeTest {

    /*
    DEFAULTS
     */

    @Test
    fun `default group has empty children and null bounds`() {
        val g = GroupNode()
        assertEquals(emptyList(), g.children)
        assertNull(g.bounds)
    }

    @Test
    fun `group preserves provided bounds`() {
        val b = SceneBounds(-50.0, -50.0, 100.0, 100.0)
        assertEquals(b, GroupNode(bounds = b).bounds)
    }

    @Test
    fun `vararg ctor builds the children in order, without bounds`() {
        val a = FlatPointNode(FlatPoint(1.0, 0.0))
        val b = FlatPointNode(FlatPoint(2.0, 0.0))
        val g = GroupNode(a, b)
        assertEquals(listOf<Node>(a, b), g.children)
        assertNull(g.bounds)
    }

    /*
    COPY HOOK
     */

    @Test
    fun `copy with children only carries the bounds through`() {
        val b = SceneBounds(0.0, 0.0, 100.0, 100.0)
        val g = GroupNode(children = listOf(FlatPointNode(FlatPoint(1.0, 0.0))), bounds = b)
        val rebuilt = g.copy(children = emptyList())
        assertEquals(emptyList(), rebuilt.children)
        assertEquals(b, rebuilt.bounds)
    }

    @Test
    fun `times returns a subclass through its copy override`() {
        val moved = Tinted(listOf(FlatPointNode(FlatPoint(1.0, 2.0)))) * T(3.0, 0.0)
        assertIs<Tinted>(moved)
        assertEquals(FlatPointNode(FlatPoint(4.0, 2.0)), moved.children.single())
    }

    @Test
    fun `times on a subclass without a copy override fails fast with a named error`() {
        val e = assertFailsWith<IllegalStateException> { Hollow(emptyList()) * T(1.0, 0.0) }
        assertTrue("Hollow" in e.message!!, "message names the offending class: ${e.message}")
        assertTrue("GroupNode" in e.message!!, "message names the returned class: ${e.message}")
    }

    /*
    EQUALITY
     */

    @Test
    fun `groups are equal when fields match`() {
        val b = SceneBounds(0.0, 0.0, 100.0, 100.0)
        val a = GroupNode(children = listOf(FlatPointNode(FlatPoint(1.0, 2.0))), bounds = b)
        val c = GroupNode(children = listOf(FlatPointNode(FlatPoint(1.0, 2.0))), bounds = b)
        assertEquals(a, c)
        assertEquals(a.hashCode(), c.hashCode())
    }

    @Test
    fun `groups are unequal when a leaf differs only in style`() {
        val quad = Polygon.Rectangle(FlatPoint(0.0, 0.0), FlatPoint(2.0, 1.0))
        val a = GroupNode(children = listOf(PolygonNode(quad)))
        val b = GroupNode(children = listOf(PolygonNode(quad, ShapeStyle.POLYGON.copy(fill = ShapeColor.GREEN))))
        assertNotEquals(a, b)
    }

    @Test
    fun `groups are unequal when bounds differ`() {
        val a = GroupNode(bounds = SceneBounds(0.0, 0.0, 100.0, 100.0))
        val b = GroupNode(bounds = SceneBounds(0.0, 0.0, 200.0, 100.0))
        assertNotEquals(a, b)
    }

    @Test
    fun `groups are unequal when one has bounds and the other does not`() {
        val a = GroupNode(bounds = SceneBounds(0.0, 0.0, 100.0, 100.0))
        val b = GroupNode(bounds = null)
        assertNotEquals(a, b)
    }

    @Test
    fun `a subclass instance and a plain group never compare equal, in either direction`() {
        val children = listOf<Node>(FlatPointNode(FlatPoint(1.0, 2.0)))
        val plain = GroupNode(children = children)
        val tinted = Tinted(children)
        assertNotEquals<GroupNode>(plain, tinted)
        assertNotEquals<GroupNode>(tinted, plain)
    }

    /*
    TRAVERSAL — flattenShapes
     */

    @Test
    fun `empty group yields nothing`() {
        assertEquals(emptyList(), GroupNode().flattenShapes())
    }

    @Test
    fun `traversal visits leaves in depth-first source order`() {
        val s1 = FlatPointNode(FlatPoint(1.0, 0.0))
        val s2 = FlatPointNode(FlatPoint(2.0, 0.0))
        val s3 = FlatPointNode(FlatPoint(3.0, 0.0))
        val root = GroupNode(
            children = listOf(
                s1,
                GroupNode(children = listOf(s2)),
                s3,
            ),
        )
        assertEquals(listOf(s1, s2, s3), root.flattenShapes())
    }

    /*
    BACKDROP
     */

    @Test
    fun `backdrop is a full-bleed origin-centered rectangle ignoring the bounds offset`() {
        // Deliberately offset bounds: the rect must stay origin-centered width × height,
        // matching the renderers' offset-ignoring convention (clip rect, worldBorder).
        val b = SceneBounds(10.0, 20.0, 400.0, 300.0)
        val node = backdrop(b).single<PolygonNode>()
        assertEquals(Polygon.Rectangle(FlatPoint(-200.0, -150.0), FlatPoint(200.0, 150.0)).vertices, node.vertices)
    }

    @Test
    fun `backdrop fills with the given color and no stroke, black by default`() {
        val b = SceneBounds(0.0, 0.0, 100.0, 100.0)
        val defaulted = backdrop(b).single<PolygonNode>()
        assertEquals(ShapeStyle.POLYGON.copy(fill = ShapeColor.BLACK, stroke = ShapeColor.NONE), defaulted.style)
        val tinted = backdrop(b, ShapeColor.GREEN).single<PolygonNode>()
        assertEquals(ShapeColor.GREEN, tinted.style.fill)
    }

    @Test
    fun `backdrop carries the bounds through`() {
        val b = SceneBounds(-50.0, -50.0, 100.0, 100.0)
        assertEquals(b, backdrop(b).bounds)
    }

    /*
    LOAD BOUNDARY — single<N>
     */

    @Test
    fun `single extracts the typed leaf with its style`() {
        val quad = Polygon.Rectangle(FlatPoint(0.0, 0.0), FlatPoint(4.0, 2.0))
        val style = ShapeStyle.POLYGON.copy(strokeWidth = 3.0)
        val group = GroupNode(children = listOf(PolygonNode(quad, style)))

        val node = group.single<PolygonNode>()

        assertEquals(quad.vertices, node.vertices)
        assertEquals(style, node.style)
    }

    @Test
    fun `single reaches through nested groups`() {
        val point = FlatPointNode(FlatPoint(1.0, 2.0))
        val group = GroupNode(
            children = listOf(GroupNode(children = listOf(point))),
        )
        assertEquals(point, group.single<FlatPointNode>())
    }

    @Test
    fun `single fails with found-vs-expected on a type mismatch`() {
        val group = GroupNode(children = listOf(FlatPointNode(FlatPoint(1.0, 2.0))))
        val e = assertFailsWith<IllegalStateException> { group.single<PolygonNode>() }
        assertTrue("FlatPointNode" in e.message!!, "message names the found type: ${e.message}")
        assertTrue("PolygonNode" in e.message!!, "message names the expected type: ${e.message}")
    }

    @Test
    fun `single fails when the tree does not hold exactly one leaf`() {
        assertFailsWith<NoSuchElementException> { GroupNode().single<FlatPointNode>() }
        val two = GroupNode(
            children = listOf(FlatPointNode(FlatPoint(1.0, 0.0)), FlatPointNode(FlatPoint(2.0, 0.0))),
        )
        assertFailsWith<IllegalArgumentException> { two.single<FlatPointNode>() }
    }
}

private class Tinted(children: List<Node>) : GroupNode(children) {
    override fun copy(children: List<Node>, bounds: SceneBounds?): Tinted = Tinted(children)
}

private class Hollow(children: List<Node>) : GroupNode(children)
