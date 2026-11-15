package fromcrackpot.kintsugi.gameengine.entity.kinematics

import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.round
import fromcrackpot.kintsugi.gameengine.Step
import fromcrackpot.kintsugi.gameengine.input.Input
import fromcrackpot.kintsugi.scenegraph.nodes.composites.PolygonNode
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

private const val DT = 1.0 / 120
private val BLAST = Blast()
private val ORIGIN = FlatPoint(0.0, 0.0)

private fun tile(x: Double, y: Double, side: Double = 10.0): PolygonNode =
    PolygonNode(Polygon.Rectangle(FlatPoint(x - side / 2, y - side / 2), FlatPoint(x + side / 2, y + side / 2)))

private fun burst(vararg tiles: PolygonNode, origin: FlatPoint = ORIGIN): Burst = Burst(tiles.toList(), origin, BLAST)

private fun Burst.advanced(dt: Double = DT): Burst = context(Step(dt, Input.NONE)) { blast.advance(this) }

private fun Burst.advanced(dts: List<Double>): Burst = dts.fold(this) { b, dt -> b.advanced(dt) }

private fun Burst.advancedFor(seconds: Double): Burst = advanced(List((seconds / DT).toInt()) { DT })

private fun Debris.advanced(): Debris = context(Step(DT, Input.NONE)) { law.advance(this) }

// The default blast's spread σ(t) = 1 + (K/γ)(1 − e^(−γt)), K = 20/s, γ = 1/s
private fun spread(t: Double): Double = 1 + 20.0 * (1 - exp(-t))

private fun size(fragment: PolygonNode): Double = sqrt(abs(fragment.signedArea))

private fun assertSameFragments(expected: Burst, actual: Burst, message: String) {
    assertEquals(expected.children.size, actual.children.size, "$message: fragment count")
    expected.children.zip(actual.children).forEachIndexed { i, (e, a) ->
        assertEquals(0.0, round(e.centroid.px - a.centroid.px), "$message: fragment $i x")
        assertEquals(0.0, round(e.centroid.py - a.centroid.py), "$message: fragment $i y")
        assertEquals(0.0, round(size(e) - size(a)), "$message: fragment $i size")
    }
}

class BlastTest {

    /* CTOR */

    @Test
    fun `a negative burst rate or a non-positive tuning value is refused`() {
        assertFailsWith<IllegalArgumentException> { Blast(burstRate = -1.0) }
        assertFailsWith<IllegalArgumentException> { Blast(drag = 0.0) }
        assertFailsWith<IllegalArgumentException> { Blast(erodeLength = 0.0) }
        assertFailsWith<IllegalArgumentException> { Blast(erodeTime = -1.0) }
        assertFailsWith<IllegalArgumentException> { Blast(minSize = 0.0) }
    }

    /* REST */

    @Test
    fun `a zero-length step leaves a burst as itself`() {
        val b = burst(tile(15.0, 0.0))
        assertSame(b, b.advanced(0.0))
    }

    @Test
    fun `an empty burst comes back as itself`() {
        val b = burst()
        assertSame(b, b.advanced())
    }

    @Test
    fun `a node that is no burst comes back as itself`() {
        val node = tile(15.0, 0.0)
        assertSame(node, context(Step(DT, Input.NONE)) { BLAST.advance(node) })
    }

    /* MOTION */

    @Test
    fun `a fragment's distance from the origin is scaled by the spread`() {
        val moved = burst(tile(15.0, 0.0), tile(0.0, -5.0)).advanced(0.1)
        assertEquals(0.0, round(moved.children[0].centroid.px - 15.0 * spread(0.1)), "flown along +x")
        assertEquals(0.0, round(moved.children[0].centroid.py), "on its ray from the origin")
        assertEquals(0.0, round(moved.children[1].centroid.py + 5.0 * spread(0.1)), "flown along −y")
    }

    @Test
    fun `a fragment shrinks with its travel and its age`() {
        val moved = burst(tile(15.0, 0.0)).advanced(0.1)
        val travel = (spread(0.1) - 1) * 15.0
        assertEquals(0.0, round(size(moved.children.single()) - 10.0 * exp(-travel / 40.0 - 0.1 / 0.5)))
    }

    @Test
    fun `the farther fragment flies faster and ends smaller`() {
        val moved = burst(tile(5.0, 0.0), tile(25.0, 0.0)).advanced(0.1)
        val (near, far) = moved.children
        assertTrue(far.centroid.px - 25.0 > near.centroid.px - 5.0, "the far one travelled farther")
        assertTrue(size(far) < size(near), "the far one is smaller")
    }

    @Test
    fun `one big step equals even and uneven splits`() {
        val b = burst(tile(15.0, 0.0), tile(-5.0, 10.0), origin = FlatPoint(2.0, -1.0))
        val whole = b.advanced(0.05)
        assertSameFragments(whole, b.advanced(List(5) { 0.01 }), "five even steps")
        assertSameFragments(whole, b.advanced(listOf(0.02, 0.005, 0.025)), "three uneven steps")
    }

    @Test
    fun `the age advances by the step - origin and blast carried - the receiver untouched`() {
        val origin = FlatPoint(2.0, -1.0)
        val b = burst(tile(15.0, 0.0), origin = origin)
        val moved = b.advanced(0.1)
        assertEquals(0.1, moved.age)
        assertSame(origin, moved.origin)
        assertSame(BLAST, moved.blast)
        assertEquals(0.0, b.age, "the receiver keeps its age")
        assertEquals(FlatPoint(15.0, 0.0), b.children.single().centroid, "the receiver keeps its fragment")
    }

    /* CULL */

    @Test
    fun `a fragment is dropped once its size reaches minSize`() {
        // size 0.6 on the origin: age alone shrinks it to 0.6·e^(−t/0.5), at 0.5 past t ≈ 0.091 s
        val b = burst(tile(0.0, 0.0, side = 0.6))
        assertEquals(1, b.advanced(0.05).children.size, "still above minSize")
        assertEquals(0, b.advanced(0.1).children.size, "dropped")
    }

    @Test
    fun `a fragment on the origin fades and is dropped in finite time`() {
        // size 10 → 0.5 by age alone: t = 0.5·ln 20 ≈ 1.5 s
        val b = burst(tile(0.0, 0.0))
        assertEquals(1, b.advancedFor(1.4).children.size, "still fading")
        assertEquals(0, b.advancedFor(1.6).children.size, "gone")
    }

    @Test
    fun `every tile of a 6×3 brick is dropped within a second`() {
        val columns = listOf(-25.0, -15.0, -5.0, 5.0, 15.0, 25.0)
        val tiles = listOf(-10.0, 0.0, 10.0).flatMap { y -> columns.map { x -> tile(x, y) } }
        assertEquals(0, burst(*tiles.toTypedArray()).advancedFor(1.0).children.size)
    }

    /* DEBRIS */

    @Test
    fun `a spent burst leaves the debris`() {
        val live = burst(tile(15.0, 0.0))
        val pruned = Debris(listOf(burst(), live)).advanced()
        assertEquals(1, pruned.children.size)
        assertSame(live, pruned.children.single())
    }

    @Test
    fun `debris with every burst in flight comes back as itself`() {
        val debris = Debris(listOf(burst(tile(15.0, 0.0))))
        assertSame(debris, debris.advanced())
    }

    @Test
    fun `empty debris comes back as itself`() {
        val debris = Debris()
        assertSame(debris, debris.advanced())
    }
}
