package fromcrackpot.kintsugi.gameengine.entity

import fromcrackpot.kintsugi.cga.d2.operations.constraints.coerceIn
import fromcrackpot.kintsugi.cga.d2.operations.constraints.coerceOut
import fromcrackpot.kintsugi.cga.d2.operations.queries.onEscape
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Direction
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.transforms.T
import fromcrackpot.kintsugi.cga.d2.transforms.composites.times
import fromcrackpot.kintsugi.gameengine.entity.traits.Inertial
import fromcrackpot.kintsugi.scenegraph.nodes.composites.PolygonNode
import fromcrackpot.kintsugi.scenegraph.nodes.restyled
import fromcrackpot.kintsugi.scenegraph.nodes.retagged
import fromcrackpot.kintsugi.scenegraph.style.ShapeStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

private fun quad(minX: Double, minY: Double, maxX: Double, maxY: Double): PolygonNode =
    PolygonNode(Polygon.Rectangle(FlatPoint(minX, minY), FlatPoint(maxX, maxY)))

private class InertialProbe(node: PolygonNode) : PolygonEntity(node), Inertial {
    override var velocity: Direction = Direction(1.0, 0.0)
    override fun rebuild(node: PolygonNode): InertialProbe =
        InertialProbe(node).also { it.velocity = velocity }
}

private class WrongRebuild(node: PolygonNode) : PolygonEntity(node) {
    // Deliberately returns a different entity class. The narrowed check must catch it.
    override fun rebuild(node: PolygonNode): PolygonEntity = InertialProbe(node)
}

class EntityTest {

    /* READS */

    @Test
    fun `the entity is its node — geometry - style - and tags inherited`() {
        val node = PolygonNode(Polygon.Rectangle(FlatPoint(0.0, 0.0), FlatPoint(2.0, 2.0)), tags = setOf("probe"))
        val probe = InertialProbe(node)
        assertSame(node.vertices, probe.vertices, "the vertex list is shared by reference")
        assertSame(node.style, probe.style, "style comes from the injected node")
        assertSame(node.tags, probe.tags, "tags come from the injected node")
        assertEquals(node.centroid, probe.centroid, "geometry reads as the node's")
    }

    @Test
    fun `x and y read the centroid and track the rebinding`() {
        var probe = InertialProbe(quad(0.0, 0.0, 2.0, 2.0))
        assertEquals(probe.centroid.px, probe.x, "x is the centroid's x")
        assertEquals(probe.centroid.py, probe.y, "y is the centroid's y")
        probe *= T(10.0, -5.0)
        assertEquals(11.0, probe.x, "x reads the moved value's place")
        assertEquals(-4.0, probe.y, "y reads the moved value's place")
    }

    /* WRITES */

    @Test
    fun `times returns the entity's class — style - tags - and facts carried by rebuild`() {
        var probe = InertialProbe(
            PolygonNode(Polygon.Rectangle(FlatPoint(0.0, 0.0), FlatPoint(2.0, 2.0)), tags = setOf("probe"))
        )
        probe.velocity = Direction(-3.0, 4.0)
        val before = probe
        probe *= T(10.0, 0.0)
        assertNotSame(before, probe, "the rebind replaced the entity value")
        assertEquals(FlatPoint(11.0, 1.0), probe.centroid, "the entity moved")
        assertSame(before.style, probe.style, "style rides through")
        assertEquals(setOf("probe"), probe.tags, "tags ride through")
        assertEquals(Direction(-3.0, 4.0), probe.velocity, "the fact is carried by rebuild")
    }

    @Test
    fun `restyled and retagged return the entity's class — geometry and facts carried by rebuild`() {
        val probe = InertialProbe(quad(0.0, 0.0, 2.0, 2.0)).also { it.velocity = Direction(-3.0, 4.0) }

        val restyled: InertialProbe = probe.restyled(ShapeStyle.NONE)
        assertEquals(ShapeStyle.NONE, restyled.style, "the new paint")
        assertSame(probe.vertices, restyled.vertices, "the geometry rides through")
        assertEquals(Direction(-3.0, 4.0), restyled.velocity, "the fact is carried by rebuild")
        assertEquals(ShapeStyle.POLYGON, probe.style, "the receiver keeps its own paint")

        val retagged: InertialProbe = probe.retagged(setOf("probe"))
        assertEquals(setOf("probe"), retagged.tags, "the new tokens")
        assertSame(probe.style, retagged.style, "the paint rides through")
        assertEquals(Direction(-3.0, 4.0), retagged.velocity, "the fact is carried by rebuild")
    }

    @Test
    fun `coerceIn of a boundary nudges a protruding entity back inside`() {
        var probe = InertialProbe(quad(8.0, 0.0, 12.0, 2.0))
        probe = probe coerceIn Polygon.Rectangle(FlatPoint(-10.0, -10.0), FlatPoint(10.0, 10.0))
        assertEquals(FlatPoint(8.0, 1.0), probe.centroid, "nudged back inside by the depth")
    }

    @Test
    fun `coerceIn returns this by reference when nothing penetrates`() {
        val probe = InertialProbe(quad(0.0, 0.0, 2.0, 2.0))
        val coerced = probe coerceIn Polygon.Rectangle(FlatPoint(-10.0, -10.0), FlatPoint(10.0, 10.0))
        assertSame(probe, coerced, "no penetration, no rebuild")
    }

    @Test
    fun `coerceOut nudges an overlapping entity out along the shallowest push-out`() {
        var probe = InertialProbe(quad(0.0, 0.0, 2.0, 2.0))
        probe = probe coerceOut Polygon.Rectangle(FlatPoint(1.0, -10.0), FlatPoint(3.0, 10.0))
        assertEquals(FlatPoint(0.0, 1.0), probe.centroid, "pushed out left by the overlap")
    }

    /* QUERIES */

    @Test
    fun `an entity substitutes into Polygon-typed queries`() {
        val probe = InertialProbe(quad(8.0, 0.0, 12.0, 2.0))
        val boundary = Polygon.Rectangle(FlatPoint(-10.0, -10.0), FlatPoint(10.0, 10.0))
        val penetration = assertNotNull(probe onEscape boundary, "the protruding entity reports a penetration")
        assertEquals(2.0, penetration.depth, "raw depth of the protrusion")
    }

    /* FACTS */

    @Test
    fun `a Inertial fact reads and writes independently of the geometry`() {
        val probe = InertialProbe(quad(0.0, 0.0, 2.0, 2.0))
        val centroid = probe.centroid
        assertEquals(Direction(1.0, 0.0), probe.velocity, "the fact reads its initial value")
        probe.velocity = Direction(-3.0, 4.0)
        assertEquals(Direction(-3.0, 4.0), probe.velocity, "the fact rewrites freely")
        assertEquals(centroid, probe.centroid, "a fact write does not touch the geometry")
    }

    @Test
    fun `facts are excluded from the inherited structural equality`() {
        val a = InertialProbe(quad(0.0, 0.0, 2.0, 2.0))
        val b = InertialProbe(quad(0.0, 0.0, 2.0, 2.0))
        b.velocity = Direction(-3.0, 4.0)
        assertEquals(a, b, "equal geometry, style, and tags compare equal — facts don't take part")
    }

    /* REBUILD CONTRACT */

    @Test
    fun `a rebuild override returning the wrong class fails fast with a named error`() {
        val wrong = WrongRebuild(quad(0.0, 0.0, 2.0, 2.0))
        val failure = assertFailsWith<IllegalStateException> { wrong * T(1.0, 0.0) }
        val message = assertNotNull(failure.message)
        assertTrue("WrongRebuild.copy returned" in message, "the error names the offending class: $message")
        assertTrue("InertialProbe" in message, "the error names the returned class: $message")
    }
}
