package fromcrackpot.kintsugi.gameengine

import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.transforms.T
import fromcrackpot.kintsugi.cga.d2.transforms.composites.times
import fromcrackpot.kintsugi.scenegraph.SceneBounds
import fromcrackpot.kintsugi.scenegraph.nodes.GroupNode
import fromcrackpot.kintsugi.scenegraph.nodes.Node
import fromcrackpot.kintsugi.scenegraph.nodes.composites.PolygonNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

private val BOUNDS = SceneBounds(Polygon.Rectangle(FlatPoint(-100.0, -100.0), FlatPoint(100.0, 100.0)))

private fun quad(minX: Double, minY: Double, maxX: Double, maxY: Double): PolygonNode =
    PolygonNode(Polygon.Rectangle(FlatPoint(minX, minY), FlatPoint(maxX, maxY)))

private class Probe(node: PolygonNode) : PolygonEntity(node) {
    override fun copy(polygon: Polygon): Probe = Probe(PolygonNode(polygon, style, tags))
}

private class TestWorld(
    sceneryNode: PolygonNode,
    memberNodes: List<PolygonNode>,
    soloNode: PolygonNode,
) : World(BOUNDS) {
    val scenery = sceneryNode
    val members = memberNodes.map { Probe(it) }.toMutableList()
    var solo = Probe(soloNode)
    var updates = mutableListOf<Pair<Double, Input>>()

    context(step: Step)
    override fun update() {
        updates += step.dt to step.input
    }

    override val scene: GroupNode get() = GroupNode(listOf(scenery) + members + solo)
}

class WorldTest {

    /* SCENE */

    @Test
    fun `scene order is root order, bounds carried`() {
        val world = TestWorld(
            quad(0.0, 0.0, 1.0, 1.0),
            listOf(quad(2.0, 0.0, 3.0, 1.0), quad(4.0, 0.0, 5.0, 1.0)),
            quad(6.0, 0.0, 7.0, 1.0),
        )
        val expected = listOf<Node>(world.scenery, world.members[0], world.members[1], world.solo)
        assertEquals(expected, world.root.children, "the root projects the scene's children in declaration order")
        assertSame(BOUNDS, world.root.bounds, "the root carries the ctor bounds")
    }

    @Test
    fun `a var rebind is visible on the next root read`() {
        val world = TestWorld(quad(0.0, 0.0, 1.0, 1.0), emptyList(), quad(0.0, 0.0, 1.0, 1.0))
        world.solo *= T(10.0, 0.0)
        assertSame(world.solo, world.root.children.last(), "the root projects the current binding")
        assertEquals(FlatPoint(10.5, 0.5), world.solo.centroid, "the rebind moved the entity")
    }

    @Test
    fun `a list removal is visible on the next root read`() {
        val world = TestWorld(
            quad(0.0, 0.0, 1.0, 1.0),
            listOf(quad(2.0, 0.0, 3.0, 1.0), quad(4.0, 0.0, 5.0, 1.0)),
            quad(6.0, 0.0, 7.0, 1.0),
        )
        val hit = world.members[0]
        world.members.removeAll { it === hit }
        assertEquals(
            listOf<Node>(world.scenery, world.members[0], world.solo),
            world.root.children,
            "the removed member is no longer drawn",
        )
    }

    @Test
    fun `the root is a pure projection — a fresh group per read`() {
        val world = TestWorld(quad(0.0, 0.0, 1.0, 1.0), emptyList(), quad(2.0, 0.0, 3.0, 1.0))
        assertEquals(world.root, world.root, "two reads project the same content")
        assertSame(world.root.children[0], world.root.children[0], "the same objects are projected")
    }

    /* UPDATE */

    @Test
    fun `update reads the step context the caller establishes`() {
        val world = TestWorld(quad(0.0, 0.0, 1.0, 1.0), emptyList(), quad(2.0, 0.0, 3.0, 1.0))
        context(Step(0.25, Input.NONE)) { world.update() }
        context(Step(0.0, Input.NONE)) { world.update() }
        assertEquals(listOf(0.25 to Input.NONE, 0.0 to Input.NONE), world.updates, "the hook sees each step as established")
    }
}
