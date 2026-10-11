package fromcrackpot.kintsugi.gameengine.dynamics

import fromcrackpot.kintsugi.cga.d2.queries.collide
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.composites.unaryMinus
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Direction
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.gameengine.PolygonEntity
import fromcrackpot.kintsugi.gameengine.traits.Kinetic
import fromcrackpot.kintsugi.scenegraph.nodes.composites.PolygonNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

private class KineticProbe(node: PolygonNode) : PolygonEntity(node), Kinetic {
    override var velocity: Direction = Direction(200.0, 200.0)
    override fun copy(polygon: Polygon): KineticProbe =
        KineticProbe(PolygonNode(polygon, style, tags)).also { it.velocity = velocity }
}

class BounceTest {

    private val wall = -Polygon.Rectangle(FlatPoint(-400.0, -300.0), FlatPoint(400.0, 300.0))

    // A 16×16 probe centered at (395, 0): its right edge protrudes 3 past the x = 400 wall.
    private fun protrudingProbe(): KineticProbe =
        KineticProbe(PolygonNode(Polygon.Rectangle(FlatPoint(387.0, -8.0), FlatPoint(403.0, 8.0)), tags = setOf("probe")))

    @Test
    fun `bounce reflects the velocity and lands the entity where the rigid bounce puts it`() {
        val probe = protrudingProbe()
        val penetration = assertNotNull(probe collide wall, "the scenario must penetrate")
        assertEquals(3.0, penetration.depth, "raw protrusion past the wall")

        val bounced = probe * bounce(penetration)

        assertEquals(Direction(-200.0, 200.0), bounced.velocity, "velocity mirrored across the vertical wall")
        assertEquals(FlatPoint(389.0, 0.0), bounced.centroid, "overshoot mirrored: 2·depth back along the normal")
        assertNull(bounced collide wall, "the landing is strictly inside — the penetration is retired")
    }

    @Test
    fun `the receiver keeps its pre-bounce velocity — the reflection lands only on the copy`() {
        val probe = protrudingProbe()
        val penetration = assertNotNull(probe collide wall)

        val bounced = probe * bounce(penetration)

        assertEquals(Direction(200.0, 200.0), probe.velocity, "the captured binding is a true snapshot")
        assertEquals(FlatPoint(395.0, 0.0), probe.centroid, "the receiver's geometry is untouched")
        assertEquals(Direction(-200.0, 200.0), bounced.velocity, "only the returned copy carries the reflection")
    }

    @Test
    fun `bounce returns the entity's class with style and tags carried`() {
        var probe = protrudingProbe()
        val before = probe
        val penetration = assertNotNull(probe collide wall)

        probe *= bounce(penetration)

        assertSame(before.style, probe.style, "style rides through the copy hook")
        assertEquals(setOf("probe"), probe.tags, "tags ride through the copy hook")
    }

    /* RELATIVE */

    private val obstacle = Polygon.Rectangle(FlatPoint(-50.0, -10.0), FlatPoint(50.0, 10.0))

    // A 16×16 probe centered at (55, 0): its left edge reaches 3 behind the obstacle's right face.
    private fun reachingProbe(velocity: Direction): KineticProbe =
        KineticProbe(PolygonNode(Polygon.Rectangle(FlatPoint(47.0, -8.0), FlatPoint(63.0, 8.0))))
            .also { it.velocity = velocity }

    @Test
    fun `a receding contact resolves the penetration without reflecting`() {
        val probe = protrudingProbe().also { it.velocity = Direction(-200.0, 200.0) }
        val penetration = assertNotNull(probe collide wall)

        val pushed = probe * bounce(penetration)

        assertEquals(Direction(-200.0, 200.0), pushed.velocity, "a receding velocity rides through untouched")
        assertEquals(FlatPoint(392.0, 0.0), pushed.centroid, "resolved by one depth along the normal")
        assertNull(pushed collide wall, "touching is not penetration — the contact is retired")
    }

    @Test
    fun `an overtaking collider knocks the entity ahead — the relative velocity reflects`() {
        val probe = reachingProbe(Direction(200.0, 0.0))
        val penetration = assertNotNull(probe collide obstacle, "the scenario must overlap")
        assertEquals(3.0, penetration.depth, "raw reach behind the right face")

        val bounced = probe * bounce(penetration, colliderVelocity = Direction(600.0, 0.0))

        assertEquals(Direction(1000.0, 0.0), bounced.velocity, "2·collider − velocity: knocked ahead of the collider")
        assertEquals(FlatPoint(61.0, 0.0), bounced.centroid, "overshoot mirrored: 2·depth out along the normal")
        assertEquals(Direction(200.0, 0.0), probe.velocity, "the receiver keeps its pre-bounce velocity")
        assertNull(bounced collide obstacle, "the landing clears the obstacle")
    }

    @Test
    fun `a collider retreating faster than the approach reflects nothing`() {
        val probe = reachingProbe(Direction(-100.0, 0.0))
        val penetration = assertNotNull(probe collide obstacle)

        val pushed = probe * bounce(penetration, colliderVelocity = Direction(-300.0, 0.0))

        assertEquals(Direction(-100.0, 0.0), pushed.velocity, "relative velocity recedes — no reflection")
        assertEquals(FlatPoint(58.0, 0.0), pushed.centroid, "resolved by one depth along the normal")
        assertNull(pushed collide obstacle, "touching is not penetration — the contact is retired")
    }
}
