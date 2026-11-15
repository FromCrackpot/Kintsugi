package fromcrackpot.kintsugi.brickit

import fromcrackpot.kintsugi.brickit.entities.BALL_SPEED
import fromcrackpot.kintsugi.brickit.entities.FULL_CHARGE
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Direction
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.transforms.T
import fromcrackpot.kintsugi.cga.d2.transforms.composites.times
import fromcrackpot.kintsugi.gameengine.Restart
import fromcrackpot.kintsugi.gameengine.Sound
import fromcrackpot.kintsugi.scenegraph.nodes.composites.PolygonNode
import fromcrackpot.kintsugi.scenegraph.style.ShapeStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private const val DT = 1.0 / 120

class SoundTest {

    private val s = TEST_SOUNDS

    // ChargeTest's layout: paddle 100×16 at PADDLE_Y (faces at −222 and −238, line at −230), ball 16×16, floor at −300.
    // Bricks at y [105, 135]: gold x [-100, -40], normal x [-30, 30], steel x [40, 100] and [100, 160].
    private fun brick(left: Double, vararg tags: String): PolygonNode =
        PolygonNode(Polygon.Rectangle(FlatPoint(left, 105.0), FlatPoint(left + 60.0, 135.0)), tags = tags.toSet())

    private fun world(): BrickIt = BrickIt(
        BrickItAssets(
            wall = listOf(brick(-100.0, "brick", "gold"), brick(-30.0), brick(40.0, "brick", "steel"), brick(100.0, "brick", "steel")),
            paddle = PolygonNode(Polygon.Rectangle(FlatPoint(-50.0, -8.0), FlatPoint(50.0, 8.0))),
            ball = PolygonNode(Polygon.Rectangle(FlatPoint(-8.0, -8.0), FlatPoint(8.0, 8.0))),
            ballCharged = ShapeStyle.POLYGON,
            sounds = TEST_SOUNDS,
        ),
    )

    private fun BrickIt.charged() = apply { ball = ball.withCharge(FULL_CHARGE) }

    // One step from (x, y), returning what it played.
    private fun BrickIt.hit(velocity: Direction, x: Double, y: Double): List<Sound> {
        ball *= T(x - ball.x, y - ball.y)
        ball.velocity = velocity
        step(DT)
        return played
    }

    private fun BrickIt.hitUnderneath() = hit(Direction(0.0, BALL_SPEED), 0.0, -248.0)
    private fun BrickIt.hitOnTop() = hit(Direction(0.0, -BALL_SPEED), 0.0, -212.0)
    private fun BrickIt.hitTheSide() = hit(Direction(-300.0, 0.0), 60.0, -230.0)
    private fun BrickIt.hitBrickAt(x: Double) = hit(Direction(0.0, BALL_SPEED), x, 96.5)
    private fun BrickIt.climbPastTheLine() = hit(Direction(0.0, BALL_SPEED), 150.0, -232.0)

    // Back down beside the paddle, stepping until the floor turns the ball: that step's sounds.
    private fun BrickIt.fallToTheFloor(): List<Sound> {
        hit(Direction(0.0, -BALL_SPEED), 150.0, -228.0)
        while (ball.velocity.py < 0.0) step(DT)
        return played
    }

    /* PADDLE */

    @Test
    fun `every paddle face plays the paddle`() {
        assertEquals(listOf(s.paddle), world().hitOnTop())
        assertEquals(listOf(s.paddle), world().hitUnderneath())
        assertEquals(listOf(s.paddle, s.ballCharge), world().hitTheSide(), "a sidekick charges too")
    }

    @Test
    fun `a push by the paddle plays nothing`() {
        assertEquals(emptyList(), world().hit(Direction(0.0, -300.0), 0.0, -242.0))
    }

    @Test
    fun `the ball turning gold plays the charge-up once`() {
        val w = world()
        assertEquals(listOf(s.paddle), w.hitUnderneath())
        assertEquals(listOf(s.paddle, s.ballCharge), w.hitUnderneath())
        assertEquals(listOf(s.paddle), w.hitUnderneath(), "a full ball's dribble")
        assertEquals(listOf(s.paddle), w.hitTheSide(), "a full ball's sidekick")
    }

    /* ARENA */

    @Test
    fun `a side wall plays the wall`() {
        assertEquals(listOf(s.wall), world().hit(Direction(300.0, 0.0), 391.0, 0.0))
    }

    @Test
    fun `the floor plays the wall - and the discharge when it takes the charge`() {
        assertEquals(listOf(s.wall, s.ballDischarge), world().charged().apply { climbPastTheLine() }.fallToTheFloor())
        assertEquals(listOf(s.wall), world().apply { climbPastTheLine() }.fallToTheFloor(), "nothing to discharge")
    }

    @Test
    fun `a dribble's floor bounce plays the wall only`() {
        val w = world().apply { hitUnderneath(); hitUnderneath() }
        while (w.ball.velocity.py < 0.0) w.step(DT)
        assertTrue(w.ball.charged)
        assertEquals(listOf(s.wall), w.played)
    }

    /* BRICKS */

    @Test
    fun `a normal brick shatters - bounced off or flown through`() {
        assertEquals(listOf(s.brickNormalShatter), world().hitBrickAt(0.0))
        assertEquals(listOf(s.brickNormalShatter), world().charged().hitBrickAt(0.0))
    }

    @Test
    fun `steel clangs off an uncharged ball and shatters under a charged one`() {
        assertEquals(listOf(s.brickSteel), world().hitBrickAt(70.0))
        assertEquals(listOf(s.brickSteelShatter), world().charged().hitBrickAt(70.0))
    }

    @Test
    fun `a push off steel plays nothing`() {
        assertEquals(emptyList(), world().hit(Direction(0.0, -BALL_SPEED), 70.0, 101.0))
    }

    @Test
    fun `the gold plays the level completed as the round restarts`() {
        val w = world()
        assertFailsWith<Restart> { w.hitBrickAt(-70.0) }
        assertEquals(listOf(s.levelCompleted), w.played)
    }
}
