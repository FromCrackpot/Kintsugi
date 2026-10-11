package fromcrackpot.kintsugi.gameengine

import fromcrackpot.kintsugi.cga.d2.constraints.coerceIn
import fromcrackpot.kintsugi.cga.d2.constraints.coerceOut
import fromcrackpot.kintsugi.cga.d2.queries.escape
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Direction
import fromcrackpot.kintsugi.cga.d2.transforms.T
import fromcrackpot.kintsugi.cga.d2.transforms.composites.times
import fromcrackpot.kintsugi.gameengine.traits.Kinetic
import fromcrackpot.kintsugi.scenegraph.nodes.composites.PolygonNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

private fun quad(minX: Double, minY: Double, maxX: Double, maxY: Double): PolygonNode =
    PolygonNode(Polygon.Rectangle(FlatPoint(minX, minY), FlatPoint(maxX, maxY)))

private class KineticProbe(node: PolygonNode) : PolygonEntity(node), Kinetic {
    override var velocity: Direction = Direction(1.0, 0.0)
    override fun copy(polygon: Polygon): KineticProbe =
        KineticProbe(PolygonNode(polygon, style, tags)).also { it.velocity = velocity }
}

private class WrongCopy(node: PolygonNode) : PolygonEntity(node) {
    // Deliberately returns a different entity class — the narrowed check must catch it.
    override fun copy(polygon: Polygon): PolygonEntity =
        KineticProbe(PolygonNode(polygon, style, tags))
}

class EntityTest {

    /* READS */

    @Test
    fun `the entity is its node — geometry, style, and tags inherited`() {
        val node = PolygonNode(Polygon.Rectangle(FlatPoint(0.0, 0.0), FlatPoint(2.0, 2.0)), tags = setOf("probe"))
        val probe = KineticProbe(node)
        assertSame(node.vertices, probe.vertices, "the vertex list is shared by reference")
        assertSame(node.style, probe.style, "style comes from the injected node")
        assertSame(node.tags, probe.tags, "tags come from the injected node")
        assertEquals(node.centroid, probe.centroid, "geometry reads as the node's")
    }

    @Test
    fun `x and y read the centroid and track the rebinding`() {
        var probe = KineticProbe(quad(0.0, 0.0, 2.0, 2.0))
        assertEquals(probe.centroid.px, probe.x, "x is the centroid's x")
        assertEquals(probe.centroid.py, probe.y, "y is the centroid's y")
        probe *= T(10.0, -5.0)
        assertEquals(11.0, probe.x, "x reads the moved value's place")
        assertEquals(-4.0, probe.y, "y reads the moved value's place")
    }

    /* WRITES */

    @Test
    fun `times returns the entity's class — style, tags, and facts carried by copy`() {
        var probe = KineticProbe(
            PolygonNode(Polygon.Rectangle(FlatPoint(0.0, 0.0), FlatPoint(2.0, 2.0)), tags = setOf("probe"))
        )
        probe.velocity = Direction(-3.0, 4.0)
        val before = probe
        probe *= T(10.0, 0.0)
        assertNotSame(before, probe, "the rebind replaced the entity value")
        assertEquals(FlatPoint(11.0, 1.0), probe.centroid, "the entity moved")
        assertSame(before.style, probe.style, "style rides through")
        assertEquals(setOf("probe"), probe.tags, "tags ride through")
        assertEquals(Direction(-3.0, 4.0), probe.velocity, "the fact is carried by copy")
    }

    @Test
    fun `coerceIn of a boundary nudges a protruding entity back inside`() {
        var probe = KineticProbe(quad(8.0, 0.0, 12.0, 2.0))
        probe = probe coerceIn Polygon.Rectangle(FlatPoint(-10.0, -10.0), FlatPoint(10.0, 10.0))
        assertEquals(FlatPoint(8.0, 1.0), probe.centroid, "nudged back inside by the depth")
    }

    @Test
    fun `coerceIn returns this by reference when nothing penetrates`() {
        val probe = KineticProbe(quad(0.0, 0.0, 2.0, 2.0))
        val coerced = probe coerceIn Polygon.Rectangle(FlatPoint(-10.0, -10.0), FlatPoint(10.0, 10.0))
        assertSame(probe, coerced, "no penetration, no rebuild")
    }

    @Test
    fun `coerceOut nudges an overlapping entity out along the shallowest push-out`() {
        var probe = KineticProbe(quad(0.0, 0.0, 2.0, 2.0))
        probe = probe coerceOut Polygon.Rectangle(FlatPoint(1.0, -10.0), FlatPoint(3.0, 10.0))
        assertEquals(FlatPoint(0.0, 1.0), probe.centroid, "pushed out left by the overlap")
    }

    /* QUERIES */

    @Test
    fun `an entity substitutes into Polygon-typed queries`() {
        val probe = KineticProbe(quad(8.0, 0.0, 12.0, 2.0))
        val boundary = Polygon.Rectangle(FlatPoint(-10.0, -10.0), FlatPoint(10.0, 10.0))
        val penetration = assertNotNull(probe escape boundary, "the protruding entity reports a penetration")
        assertEquals(2.0, penetration.depth, "raw depth of the protrusion")
    }

    /* FACTS */

    @Test
    fun `a Kinetic fact reads and writes independently of the geometry`() {
        val probe = KineticProbe(quad(0.0, 0.0, 2.0, 2.0))
        val centroid = probe.centroid
        assertEquals(Direction(1.0, 0.0), probe.velocity, "the fact reads its initial value")
        probe.velocity = Direction(-3.0, 4.0)
        assertEquals(Direction(-3.0, 4.0), probe.velocity, "the fact rewrites freely")
        assertEquals(centroid, probe.centroid, "a fact write does not touch the geometry")
    }

    @Test
    fun `facts are excluded from the inherited structural equality`() {
        val a = KineticProbe(quad(0.0, 0.0, 2.0, 2.0))
        val b = KineticProbe(quad(0.0, 0.0, 2.0, 2.0))
        b.velocity = Direction(-3.0, 4.0)
        assertEquals(a, b, "equal geometry, style, and tags compare equal — facts don't take part")
    }

    /* COPY CONTRACT */

    @Test
    fun `a copy override returning the wrong class fails fast with a named error`() {
        val wrong = WrongCopy(quad(0.0, 0.0, 2.0, 2.0))
        val failure = assertFailsWith<IllegalStateException> { wrong * T(1.0, 0.0) }
        val message = assertNotNull(failure.message)
        assertTrue("WrongCopy" in message, "the error names the offending class: $message")
        assertTrue("KineticProbe" in message, "the error names the returned class: $message")
    }
}
