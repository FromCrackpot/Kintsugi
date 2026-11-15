package fromcrackpot.kintsugi.gameengine.entity.kinematics

import fromcrackpot.kintsugi.cga.Angle
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.deg
import fromcrackpot.kintsugi.cga.rad
import fromcrackpot.kintsugi.cga.round
import fromcrackpot.kintsugi.gameengine.Step
import fromcrackpot.kintsugi.gameengine.entity.PolygonEntity
import fromcrackpot.kintsugi.gameengine.entity.traits.Tilting
import fromcrackpot.kintsugi.gameengine.entity.traits.tilt
import fromcrackpot.kintsugi.gameengine.input.Input
import fromcrackpot.kintsugi.scenegraph.nodes.composites.PolygonNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

private const val DT = 1.0 / 120
private val RATE = 360.deg // 3° per 1/120 s step

private class Tilter(node: PolygonNode) : PolygonEntity(node), Tilting {
    override var tiltTarget: Angle = 0.rad
    override val tiltRate: Angle get() = RATE
    var label = ""
    override fun rebuild(node: PolygonNode): Tilter = Tilter(node).also { it.tiltTarget = tiltTarget; it.label = label }
}

private fun tilter(target: Angle = 0.rad): Tilter =
    Tilter(PolygonNode(Polygon.Rectangle(FlatPoint(-50.0, -8.0), FlatPoint(50.0, 8.0)))).also { it.tiltTarget = target }

private fun Tilter.advanced(dt: Double = DT): Tilter = context(Step(dt, Input.NONE)) { TiltServo.advance(this) }

private fun Tilter.advanced(dts: List<Double>): Tilter = dts.fold(this) { t, dt -> t.advanced(dt) }

private fun assertTilt(expected: Angle, actual: Tilter, message: String) =
    assertEquals(0.rad, round(expected - actual.tilt), "$message: expected $expected, was ${actual.tilt}")

class TiltServoTest {

    /* REST */

    @Test
    fun `an entity at rest comes back as itself`() {
        val t = tilter()
        assertSame(t, t.advanced(), "nothing moved, nothing rebuilt")
    }

    @Test
    fun `a zero-length step leaves an armed entity as itself`() {
        val t = tilter(30.deg)
        assertSame(t, t.advanced(0.0))
    }

    @Test
    fun `a node without the trait comes back as itself`() {
        val node = PolygonNode(Polygon.Rectangle(FlatPoint(0.0, 0.0), FlatPoint(1.0, 1.0)))
        assertSame(node, context(Step(DT, Input.NONE)) { TiltServo.advance(node) })
    }

    /* SWING */

    @Test
    fun `an armed tilt turns toward its target at the rate - about the centroid`() {
        val t = tilter(30.deg)
        val moved = t.advanced()
        assertTilt(3.deg, moved, "one step at 360°/s")
        assertEquals(t.centroid, moved.centroid, "turned about the centroid")
        assertEquals(30.deg, moved.tiltTarget, "still on the way out")
    }

    @Test
    fun `arriving mid-step turns back with the rest of the step and resets the target`() {
        val moved = tilter(30.deg).advanced(0.1) // 36° of travel: 30° out, 6° back
        assertTilt(24.deg, moved, "out and partly back")
        assertEquals(0.rad, moved.tiltTarget, "the target resets on arrival")
    }

    @Test
    fun `the swing ends level`() {
        val moved = tilter(-30.deg).advanced(List(40) { DT })
        assertTilt(0.rad, moved, "10 steps out, 10 back, then at rest")
        assertEquals(0.rad, moved.tiltTarget)
    }

    @Test
    fun `the swing is exact however the steps split`() {
        val whole = tilter(30.deg).advanced(0.15)
        val even = tilter(30.deg).advanced(List(30) { 0.005 })
        val uneven = tilter(30.deg).advanced(listOf(0.004, 0.07, 0.0, 0.031, 0.045))
        for ((name, split) in listOf("even" to even, "uneven" to uneven)) {
            assertTilt(whole.tilt, split, name)
            assertEquals(whole.tiltTarget, split.tiltTarget, "$name: same target")
        }
        assertTilt(6.deg, whole, "54° of travel: 30° out, 24° back")
    }

    @Test
    fun `a new target mid-swing turns toward it from where the entity is`() {
        val halfway = tilter(30.deg).advanced(List(5) { DT })
        assertTilt(15.deg, halfway, "five steps out")
        halfway.tiltTarget = -30.deg
        assertTilt(12.deg, halfway.advanced(), "heading for the new target")
    }

    /* PRESERVATION */

    @Test
    fun `the class and the facts ride through the turn`() {
        val t = tilter(30.deg).also { it.label = "probe" }
        val moved: Tilter = t.advanced()
        assertIs<Tilter>(moved)
        assertEquals("probe", moved.label, "the fact rode through rebuild")
        assertEquals(30.deg, t.tiltTarget, "the receiver is untouched")
        assertTilt(0.rad, t, "the receiver did not turn")
    }
}
