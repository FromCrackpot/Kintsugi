package fromcrackpot.kintsugi.brickit

import fromcrackpot.kintsugi.brickit.entities.BALL_SPEED
import fromcrackpot.kintsugi.brickit.entities.Ball
import fromcrackpot.kintsugi.brickit.entities.Brick
import fromcrackpot.kintsugi.brickit.entities.BrickWall
import fromcrackpot.kintsugi.brickit.entities.FULL_CHARGE
import fromcrackpot.kintsugi.brickit.entities.Paddle
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Direction
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.transforms.T
import fromcrackpot.kintsugi.cga.d2.transforms.composites.times
import fromcrackpot.kintsugi.gameengine.Input
import fromcrackpot.kintsugi.gameengine.Restart
import fromcrackpot.kintsugi.gameengine.Step
import fromcrackpot.kintsugi.scenegraph.nodes.composites.PolygonNode
import fromcrackpot.kintsugi.scenegraph.style.ShapeColor
import fromcrackpot.kintsugi.scenegraph.style.ShapeStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val DT = 1.0 / 120
private val PLAIN = ShapeStyle.POLYGON
private val CHARGED = ShapeStyle.POLYGON.copy(fill = ShapeColor.GREEN)

class ChargeTest {

    // Paddle 100×16 at PADDLE_Y → faces at −222 (top) and −238 (bottom); ball 16×16.
    // Bricks pre-placement (init shifts the wall to BRICKS_Y = 120): gold spans x [-100, -40],
    // normal x [-30, 30], two steels x [40, 100] and [100, 160]; all land at y [105, 135].
    private fun brick(left: Double, vararg tags: String): Brick =
        Brick(PolygonNode(Polygon.Rectangle(FlatPoint(left, -15.0), FlatPoint(left + 60.0, 15.0)), tags = tags.toSet()))

    private fun world(): BrickIt = BrickIt(
        arena = PolygonNode(Polygon.Rectangle(FlatPoint(-400.0, -300.0), FlatPoint(400.0, 300.0))),
        bricks = BrickWall(
            listOf(brick(-100.0, "brick", "gold"), brick(-30.0), brick(40.0, "brick", "steel"), brick(100.0, "brick", "steel")),
        ),
        paddle = Paddle(PolygonNode(Polygon.Rectangle(FlatPoint(-50.0, -8.0), FlatPoint(50.0, 8.0)))),
        ball = Ball(PolygonNode(Polygon.Rectangle(FlatPoint(-8.0, -8.0), FlatPoint(8.0, 8.0)), PLAIN), CHARGED),
    )

    private fun step(world: BrickIt) = context(Step(DT, Input.NONE)) { world.update() }

    // One step from (x, y): the travel lands the ball in contact.
    private fun BrickIt.hit(velocity: Direction, x: Double, y: Double) {
        ball *= T(x - ball.x, y - ball.y)
        ball.velocity = velocity
        step(this)
    }

    private fun BrickIt.hitUnderneath() = hit(Direction(0.0, BALL_SPEED), 0.0, -248.0)
    private fun BrickIt.hitOnTop() = hit(Direction(0.0, -BALL_SPEED), 0.0, -212.0)
    private fun BrickIt.hitTheSide() = hit(Direction(-300.0, 0.0), 60.0, -230.0)

    // Top edge 0.5 below the bricks' bottom face at y = 105, heading straight up.
    private fun BrickIt.hitBrickAt(x: Double) = hit(Direction(0.0, BALL_SPEED), x, 96.5)

    /* CHARGING */

    @Test
    fun `the second rebound under the paddle charges the ball`() {
        val w = world()

        w.hitUnderneath()
        assertEquals(1, w.ball.charge)
        assertFalse(w.ball.charged)
        assertEquals(PLAIN, w.ball.style)

        w.hitUnderneath()
        assertEquals(FULL_CHARGE, w.ball.charge)
        assertTrue(w.ball.charged)
        assertEquals(CHARGED, w.ball.style, "the charged ball wears the charged look")
    }

    @Test
    fun `a sidekick charges the ball at once`() {
        val w = world()

        w.hitTheSide()
        assertEquals(FULL_CHARGE, w.ball.charge)
        assertTrue(w.ball.charged)
        assertEquals(CHARGED, w.ball.style)

        w.hitTheSide()
        assertEquals(FULL_CHARGE, w.ball.charge, "a second sidekick adds nothing")
    }

    @Test
    fun `a top-face hit zeroes the count and the charge`() {
        val counting = world().apply { hitUnderneath(); hitOnTop() }
        assertEquals(0, counting.ball.charge, "the count starts over")

        val charged = world().apply { ball.charge = FULL_CHARGE; hitOnTop() }
        assertEquals(0, charged.ball.charge)
        assertEquals(PLAIN, charged.ball.style, "back to the plain look")
    }

    @Test
    fun `a receding contact neither counts nor discharges`() {
        val under = world().apply { hit(Direction(0.0, -300.0), 0.0, -242.0) } // 1.5 into the underside, moving down and out
        assertEquals(0, under.ball.charge, "a push under the paddle is no rebound")

        val top = world().apply { ball.charge = FULL_CHARGE; hit(Direction(0.0, 300.0), 20.0, -218.0) } // into the top face, moving up and out
        assertTrue(top.ball.charged, "a push off the top face keeps the charge")
    }

    @Test
    fun `the charge and both looks ride through the ball's moves`() {
        val w = world().apply { hitUnderneath(); hitUnderneath() }

        repeat(5) { step(w) }
        assertTrue(w.ball.charged)
        assertEquals(CHARGED, w.ball.style)

        w.hitOnTop()
        repeat(5) { step(w) }
        assertEquals(PLAIN, w.ball.style, "a ball rebuilt while charged still knows its plain look")
    }

    /* SPENDING */

    @Test
    fun `a charged ball flies through a normal brick`() {
        val w = world().apply { ball.charge = FULL_CHARGE }
        val target = w.bricks.children.first { !it.steel && !it.gold }

        w.hitBrickAt(0.0)

        assertTrue(w.bricks.children.none { it === target }, "the brick left the wall")
        assertEquals(Direction(0.0, BALL_SPEED), w.ball.velocity, "no bounce")
        assertEquals(FlatPoint(0.0, 96.5 + BALL_SPEED * DT), w.ball.centroid, "plain travel, no landing correction")
        assertTrue(w.ball.charged, "a normal brick costs no charge")
    }

    @Test
    fun `a charged ball breaks a steel brick, bounces off it and loses its charge`() {
        val w = world().apply { ball.charge = FULL_CHARGE }
        val steel = w.bricks.children.first { it.steel }

        w.hitBrickAt(70.0)

        val travel = BALL_SPEED * DT
        val depth = (96.5 + 8.0 + travel) - 105.0
        assertTrue(w.bricks.children.none { it === steel }, "the steel brick left the wall")
        assertEquals(Direction(0.0, -BALL_SPEED), w.ball.velocity)
        assertEquals(FlatPoint(70.0, 96.5 + travel - 2.0 * depth), w.ball.centroid)
        assertEquals(0, w.ball.charge)
        assertEquals(PLAIN, w.ball.style)
    }

    @Test
    fun `one charge breaks one steel brick`() {
        val w = world().apply { ball.charge = FULL_CHARGE }

        w.hitBrickAt(100.0) // astride the two steels' seam

        assertEquals(1, w.bricks.children.count { it.steel })
        assertEquals(0, w.ball.charge)
    }

    @Test
    fun `a steel brick leaves an uncharged ball's count alone`() {
        val w = world().apply { ball.charge = 1 }

        w.hitBrickAt(70.0)

        assertEquals(1, w.ball.charge, "bricks never reset the count")
        assertEquals(2, w.bricks.children.count { it.steel }, "steel survives an uncharged ball")
    }

    @Test
    fun `a gold brick restarts the round, charged ball or not`() {
        val w = world().apply { ball.charge = FULL_CHARGE }

        assertFailsWith<Restart> { w.hitBrickAt(-70.0) }
    }
}
