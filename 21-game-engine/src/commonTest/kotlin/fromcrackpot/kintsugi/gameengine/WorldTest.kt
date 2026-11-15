package fromcrackpot.kintsugi.gameengine

import fromcrackpot.kintsugi.cga.Angle
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.composites.split
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.transforms.T
import fromcrackpot.kintsugi.cga.d2.transforms.composites.times
import fromcrackpot.kintsugi.cga.deg
import fromcrackpot.kintsugi.cga.rad
import fromcrackpot.kintsugi.cga.round
import fromcrackpot.kintsugi.gameengine.entity.GroupEntity
import fromcrackpot.kintsugi.gameengine.entity.PolygonEntity
import fromcrackpot.kintsugi.gameengine.entity.traits.Explosive
import fromcrackpot.kintsugi.gameengine.entity.traits.Tilting
import fromcrackpot.kintsugi.gameengine.entity.traits.explode
import fromcrackpot.kintsugi.gameengine.entity.traits.tilt
import fromcrackpot.kintsugi.gameengine.input.Input
import fromcrackpot.kintsugi.scenegraph.SceneBounds
import fromcrackpot.kintsugi.scenegraph.nodes.GroupNode
import fromcrackpot.kintsugi.scenegraph.nodes.Node
import fromcrackpot.kintsugi.scenegraph.nodes.composites.PolygonNode
import fromcrackpot.kintsugi.scenegraph.nodes.flattenShapes
import fromcrackpot.kintsugi.scenegraph.style.ShapeColor
import fromcrackpot.kintsugi.scenegraph.style.ShapeStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

private val BOUNDS = SceneBounds(Polygon.Rectangle(FlatPoint(-100.0, -100.0), FlatPoint(100.0, 100.0)))

private fun quad(minX: Double, minY: Double, maxX: Double, maxY: Double): PolygonNode =
    PolygonNode(Polygon.Rectangle(FlatPoint(minX, minY), FlatPoint(maxX, maxY)))

private class Probe(node: PolygonNode) : PolygonEntity(node) {
    override fun rebuild(node: PolygonNode): Probe = Probe(node)
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

private class StagedWorld(sceneryNode: PolygonNode, firstNode: PolygonNode, secondNode: PolygonNode) : World(BOUNDS) {
    val scenery by staged(sceneryNode)
    var first by staged(Probe(firstNode))
    var second by staged(Probe(secondNode))

    context(step: Step)
    override fun update() {}
}

private class Tilter(node: PolygonNode) : PolygonEntity(node), Tilting {
    override var tiltTarget: Angle = 0.rad
    override val tiltRate: Angle get() = 360.deg
    override fun rebuild(node: PolygonNode): Tilter = Tilter(node).also { it.tiltTarget = tiltTarget }
}

private class KinematicWorld : World(BOUNDS) {
    var solo by staged(Tilter(quad(-5.0, -1.0, 5.0, 1.0)))
    var group by staged(GroupEntity(listOf(Tilter(quad(10.0, -1.0, 20.0, 1.0)))))
    var seenTilt: Angle? = null

    context(step: Step)
    override fun update() {
        seenTilt = solo.tilt
    }
}

private class Crate(node: PolygonNode, override val fragmentCount: Int) : PolygonEntity(node), Explosive {
    override fun rebuild(node: PolygonNode): Crate = Crate(node, fragmentCount)
}

private class ExplodingWorld(private val at: FlatPoint? = null) : World(BOUNDS) {
    val scenery by staged(quad(-1.0, -1.0, 1.0, 1.0))
    var pending: Crate? = null

    context(step: Step)
    override fun update() {
        val crate = pending ?: return
        if (at == null) crate.explode() else crate.explode(at)
        pending = null
    }
}

class WorldTest {

    /* SCENE */

    @Test
    fun `the default scene draws the staged nodes in staging order`() {
        val world = StagedWorld(quad(0.0, 0.0, 1.0, 1.0), quad(2.0, 0.0, 3.0, 1.0), quad(4.0, 0.0, 5.0, 1.0))
        assertEquals(listOf<Node>(world.scenery, world.first, world.second), world.root.children, "staging order is draw order")
        assertSame(BOUNDS, world.root.bounds, "the root carries the ctor bounds")
    }

    @Test
    fun `a staged var rebind is visible on the next root read`() {
        val world = StagedWorld(quad(0.0, 0.0, 1.0, 1.0), quad(2.0, 0.0, 3.0, 1.0), quad(4.0, 0.0, 5.0, 1.0))
        world.second *= T(10.0, 0.0)
        assertSame(world.second, world.root.children.last(), "the root projects the current binding")
        assertEquals(FlatPoint(14.5, 0.5), world.second.centroid, "the rebind moved the entity")
    }

    @Test
    fun `an overridden scene wins over staging order`() {
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

    /* STEP */

    @Test
    fun `step runs update under the step it is given`() {
        val world = TestWorld(quad(0.0, 0.0, 1.0, 1.0), emptyList(), quad(2.0, 0.0, 3.0, 1.0))
        world.step(0.25)
        world.step(0.0)
        assertEquals(listOf(0.25 to Input.NONE, 0.0 to Input.NONE), world.updates, "the hook sees each step as stepped")
    }

    /* KINEMATICS */

    @Test
    fun `step moves kinematic nodes before update - group members included`() {
        val world = KinematicWorld()
        world.solo.tiltTarget = 30.deg
        world.group.children.single().tiltTarget = 30.deg
        world.step(1.0 / 120)
        assertEquals(0.rad, round(world.seenTilt!! - 3.deg), "update saw the tilt the phase applied")
        assertEquals(0.rad, round(world.group.children.single().tilt - 3.deg), "the member turned inside its group")
    }

    @Test
    fun `a world at rest keeps its staged values`() {
        val world = KinematicWorld()
        val solo = world.solo
        val group = world.group
        world.step(1.0 / 120)
        assertSame(solo, world.solo, "nothing moved, nothing rebound")
        assertSame(group, world.group, "an untouched group is not rebuilt")
    }

    /* EXPLOSIONS */

    @Test
    fun `an explosion draws the entity's split over the scene - in its style and without its tags`() {
        val world = ExplodingWorld()
        val style = ShapeStyle.POLYGON.copy(fill = ShapeColor.YELLOW)
        val crate = Crate(PolygonNode(Polygon.Rectangle(FlatPoint(10.0, 0.0), FlatPoint(30.0, 10.0)), style, setOf("crate")), 2)
        world.pending = crate
        world.step(1.0 / 120)
        assertEquals(2, world.root.children.size, "the scenery, then the debris")
        assertSame(world.scenery, world.root.children.first())
        val expected = crate.split(2).map { PolygonNode(it, style) }
        assertEquals(expected, (world.root.children.last() as GroupNode).flattenShapes(), "drawn as split")
    }

    @Test
    fun `an explosion runs on the following steps and leaves the root once spent`() {
        val world = ExplodingWorld()
        world.pending = Crate(quad(10.0, 0.0, 30.0, 10.0), 2)
        world.step(1.0 / 120)
        world.step(1.0 / 120)
        val fragments = (world.root.children.last() as GroupNode).flattenShapes().map { it as PolygonNode }
        assertTrue(fragments.maxOf { it.centroid.px } > 25.0, "flown away from the entity's centroid")
        repeat(240) { world.step(1.0 / 120) }
        assertEquals(listOf<Node>(world.scenery), world.root.children, "every fragment dropped, no debris drawn")
    }

    @Test
    fun `a single fragment shrinks in place by default and flies from the given point`() {
        val node = quad(10.0, -5.0, 20.0, 5.0)
        val own = ExplodingWorld().apply { pending = Crate(node, 1) }
        val offset = ExplodingWorld(at = FlatPoint(0.0, 0.0)).apply { pending = Crate(node, 1) }
        repeat(3) { own.step(1.0 / 120); offset.step(1.0 / 120) }
        val stayed = (own.root.children.last() as GroupNode).flattenShapes().single() as PolygonNode
        val flown = (offset.root.children.last() as GroupNode).flattenShapes().single() as PolygonNode
        assertEquals(node.centroid, stayed.centroid, "about its own centroid it only fades")
        assertTrue(stayed.signedArea < node.signedArea, "and shrinks")
        assertTrue(flown.centroid.px > node.centroid.px, "about the origin it flies along +x")
    }

    @Test
    fun `an entity the split rejects fails the step`() {
        val world = ExplodingWorld()
        world.pending = Crate(quad(10.0, 0.0, 30.0, 10.0), 0)
        assertFailsWith<IllegalArgumentException> { world.step(1.0 / 120) }
    }
}
