package fromcrackpot.kintsugi.brickit

import fromcrackpot.kintsugi.brickit.entities.BALL_SPEED
import fromcrackpot.kintsugi.brickit.entities.Ball
import fromcrackpot.kintsugi.brickit.entities.BrickWall
import fromcrackpot.kintsugi.brickit.entities.PADDLE_MAX_TILT
import fromcrackpot.kintsugi.brickit.entities.Paddle
import fromcrackpot.kintsugi.cga.Angle
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.composites.RectSide.BOTTOM
import fromcrackpot.kintsugi.cga.d2.shapes.composites.get
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Direction
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.transforms.R
import fromcrackpot.kintsugi.cga.d2.transforms.T
import fromcrackpot.kintsugi.cga.d2.transforms.composites.times
import fromcrackpot.kintsugi.cga.deg
import fromcrackpot.kintsugi.cga.rad
import fromcrackpot.kintsugi.cga.round
import fromcrackpot.kintsugi.gameengine.Input
import fromcrackpot.kintsugi.gameengine.Step
import fromcrackpot.kintsugi.scenegraph.nodes.composites.PolygonNode
import fromcrackpot.kintsugi.scenegraph.style.ShapeStyle
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.tan
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

private const val DT = 1.0 / 120
private const val EXTENT = 58.0 // ball half-width + paddle half-width: the hit offset's unit
private const val SWING_BOUND = 12 // steps; the full tilt range takes 10 at 120 Hz
private val SPEED = hypot(300.0, 300.0)
private val TIP_VX = BALL_SPEED * tan(PI / 3) // the tip leaves 30° off the face

class PaddleReturnTest {

    // Paddle 100×16 at PADDLE_Y → faces at −222 (top) and −238 (bottom); ball 16×16.
    private fun world(): BrickIt = BrickIt(
        arena = PolygonNode(Polygon.Rectangle(FlatPoint(-400.0, -300.0), FlatPoint(400.0, 300.0))),
        bricks = BrickWall(emptyList()),
        paddle = Paddle(PolygonNode(Polygon.Rectangle(FlatPoint(-50.0, -8.0), FlatPoint(50.0, 8.0)))),
        ball = Ball(PolygonNode(Polygon.Rectangle(FlatPoint(-8.0, -8.0), FlatPoint(8.0, 8.0))), ShapeStyle.POLYGON),
    )

    private fun step(world: BrickIt) = context(Step(DT, Input.NONE)) { world.update() }

    private fun BrickIt.tilt(): Angle = paddle.edges[BOTTOM].attitude.angle

    // One step from (x, y): the travel lands the ball in contact.
    private fun hit(velocity: Direction, x: Double, y: Double, prepare: BrickIt.() -> Unit = {}): BrickIt =
        world().apply {
            prepare()
            ball *= T(x - ball.x, y - ball.y)
            ball.velocity = velocity
            step(this)
        }

    // The start x whose travel lands the ball at `hitOffset` of the extent.
    private fun landingAt(hitOffset: Double, velocity: Direction): Double = hitOffset * EXTENT - velocity.px * DT

    private fun onTop(hitOffset: Double, velocity: Direction): BrickIt = hit(velocity, landingAt(hitOffset, velocity), -212.0)

    private fun underneath(hitOffset: Double, velocity: Direction): BrickIt = hit(velocity, landingAt(hitOffset, velocity), -248.0)

    /* RETURN */

    @Test
    fun `a top-face hit returns by the hit point, whatever the incoming heading`() {
        for (incoming in listOf(Direction(300.0, -300.0), Direction(-300.0, -300.0), Direction(0.0, -SPEED))) {
            val w = onTop(0.5, incoming)
            assertEquals(Direction(TIP_VX * 0.5, BALL_SPEED), w.ball.velocity, "halfway to the right tip (incoming $incoming)")
        }
    }

    @Test
    fun `the ball leaves at once, landed clear of the face`() {
        val w = onTop(0.5, Direction(300.0, -300.0))

        // Travel 2.5 → 0.5 deep into the level face; landed 2·depth back along the normal.
        assertEquals(FlatPoint(29.0, -213.5), w.ball.centroid)
    }

    @Test
    fun `the further from the centre the more sideways, mirrored left and right`() {
        val incoming = Direction(300.0, -300.0)

        assertEquals(Direction(TIP_VX * 0.25, BALL_SPEED), onTop(0.25, incoming).ball.velocity)
        assertEquals(Direction(TIP_VX * 0.75, BALL_SPEED), onTop(0.75, incoming).ball.velocity)
        assertEquals(Direction(TIP_VX * -0.25, BALL_SPEED), onTop(-0.25, incoming).ball.velocity)
        assertEquals(Direction(TIP_VX * -0.75, BALL_SPEED), onTop(-0.75, incoming).ball.velocity)
    }

    @Test
    fun `the vertical speed is constant, so the shallower the return the faster the ball`() {
        var speed = 0.0
        for (hitOffset in listOf(0.0, 0.25, 0.5, 0.75)) {
            val v = onTop(hitOffset, Direction(300.0, -300.0)).ball.velocity
            assertEquals(round(BALL_SPEED), round(v.py), "vertical speed (hit offset $hitOffset)")
            assertTrue(hypot(v.px, v.py) > speed, "faster than the steeper return before it (hit offset $hitOffset)")
            speed = hypot(v.px, v.py)
        }
    }

    @Test
    fun `a dead-centre hit returns straight up off a paddle that stays level`() {
        val w = onTop(0.0, Direction(300.0, -300.0))

        assertEquals(Direction(0.0, BALL_SPEED), w.ball.velocity)
        repeat(5) {
            step(w)
            assertEquals(0.rad, round(w.tilt()), "no side to dip")
        }
    }

    @Test
    fun `a bottom-face hit returns down and toward the centre`() {
        val incoming = Direction(300.0, 300.0)

        assertEquals(Direction(TIP_VX * -0.5, -BALL_SPEED), underneath(0.5, incoming).ball.velocity, "right half: down and leftward")
        assertEquals(Direction(TIP_VX * 0.5, -BALL_SPEED), underneath(-0.5, incoming).ball.velocity, "left half: down and rightward")
        assertEquals(Direction(0.0, -BALL_SPEED), underneath(0.0, incoming).ball.velocity, "centre: straight down")
    }

    @Test
    fun `a tilted paddle still returns by the hit point, not off its tilted axis`() {
        val w = hit(Direction(0.0, -SPEED), x = 0.0, y = -212.0) { paddle *= R(paddle.centroid, -10.deg) }

        assertNotEquals(0.rad, round(w.tilt()), "scenario: the paddle is still tilted at contact")
        assertEquals(Direction(0.0, BALL_SPEED), w.ball.velocity, "dead centre: straight up all the same")
    }

    /* TILT */

    @Test
    fun `the hit end dips through the servo, reaches max tilt, and the paddle levels off`() {
        val w = onTop(0.5, Direction(300.0, -300.0))
        val returned = w.ball.velocity

        assertEquals(0.rad, round(w.tilt()), "no snap: the paddle is still level on the contact step")
        assertEquals(-1.0, w.paddle.tiltTarget, "right-side hit: the right end will dip")

        var steps = 0
        while (round(w.tilt()) != round(PADDLE_MAX_TILT * -1.0)) {
            step(w)
            assertTrue(++steps <= SWING_BOUND, "the paddle never reached max tilt")
        }
        steps = 0
        while (round(w.tilt()) != 0.rad) {
            step(w)
            assertTrue(++steps <= SWING_BOUND + 1, "the paddle never levelled")
        }
        assertEquals(0.0, w.paddle.tiltTarget)
        assertEquals(returned, w.ball.velocity, "the swing never touched the ball again")
    }

    @Test
    fun `a swat pushes the ball along at most, never a second return`() {
        for (x in listOf(20.0, 40.0, 54.0)) {
            val w = hit(Direction(300.0, 300.0), x, -248.0)
            val returned = w.ball.velocity
            assertTrue(returned.px < 0.0 && returned.py < 0.0, "down and inward (x = $x)")

            var y = w.ball.y
            repeat(10) { // the whole dip; the floor is ≥ 14 steps away
                step(w)
                assertEquals(returned, w.ball.velocity, "the dipping end pushes, never re-returns (x = $x)")
                assertTrue(w.ball.y < y, "still heading down, under the paddle (x = $x)")
                y = w.ball.y
            }
        }
    }

    /* SIDES */

    @Test
    fun `a side hit is a plain bounce that arms the tilt`() {
        val w = hit(Direction(-300.0, 0.0), x = 60.0, y = -230.0)

        assertEquals(Direction(300.0, 0.0), w.ball.velocity, "plain reflection")
        assertEquals(-1.0, w.paddle.tiltTarget, "the servo will dip the right end")
    }

    @Test
    fun `a receding contact pushes without a return or a tilt`() {
        val w = hit(Direction(0.0, 300.0), x = 20.0, y = -218.0) // 4 units into the top face, moving up and out

        assertEquals(Direction(0.0, 300.0), w.ball.velocity, "push lane: velocity untouched")
        assertEquals(0.0, w.paddle.tiltTarget, "a receding contact never arms the tilt")
    }
}
