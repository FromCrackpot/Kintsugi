package fromcrackpot.kintsugi.brickit

import fromcrackpot.kintsugi.brickit.entities.Ball
import fromcrackpot.kintsugi.brickit.entities.BrickWall
import fromcrackpot.kintsugi.brickit.entities.Paddle
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Direction
import fromcrackpot.kintsugi.cga.d2.transforms.T
import fromcrackpot.kintsugi.cga.d2.transforms.composites.times
import fromcrackpot.kintsugi.gameengine.Input
import fromcrackpot.kintsugi.gameengine.Step
import fromcrackpot.kintsugi.scenegraph.nodes.composites.PolygonNode
import fromcrackpot.kintsugi.scenegraph.style.ShapeStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val DT = 1.0 / 120

class BallBounceTest {

    private fun world(): BrickIt = BrickIt(
        arena = PolygonNode(Polygon.Rectangle(FlatPoint(-400.0, -300.0), FlatPoint(400.0, 300.0))),
        bricks = BrickWall(emptyList()),
        paddle = Paddle(PolygonNode(Polygon.Rectangle(FlatPoint(-50.0, -8.0), FlatPoint(50.0, 8.0)))),
        ball = Ball(PolygonNode(Polygon.Rectangle(FlatPoint(-8.0, -8.0), FlatPoint(8.0, 8.0))), ShapeStyle.POLYGON),
    )

    private fun step(world: BrickIt, dt: Double) = context(Step(dt, Input.NONE)) { world.update() }

    // Ball centroid to (391.5, 0): right edge 0.5 inside the x = 400 wall, one full step from contact.
    private fun BrickIt.placeBallNearRightWall() {
        ball *= T(391.5 - ball.centroid.px, -ball.centroid.py)
    }

    @Test
    fun `wall hit reflects the velocity once and lands the ball strictly inside`() {
        val world = world()
        world.placeBallNearRightWall()
        val v = world.ball.velocity

        step(world, DT)

        val travel = v.px * DT
        val depth = (391.5 + 8.0 + travel) - 400.0
        assertTrue(depth > 0.0, "scenario must penetrate; depth was $depth")
        assertEquals(Direction(-v.px, v.py), world.ball.velocity)
        // Time-exact continuation: overshoot mirrored, ball strictly inside by `depth`.
        assertEquals(FlatPoint(391.5 + travel - 2.0 * depth, v.py * DT), world.ball.centroid)
    }

    @Test
    fun `zero-dt remainder after a hit does not re-reflect — the ratchet cannot arm`() {
        val world = world()
        world.placeBallNearRightWall()
        step(world, DT)
        val reflected = world.ball.velocity

        repeat(5) {
            step(world, 0.0) // the old double-flip trigger: a penetration surviving into a zero-dt step
            step(world, DT)
            assertEquals(reflected, world.ball.velocity)
            assertTrue(world.ball.vertices.all { it.px <= 400.0 }, "ball drifted past the wall")
        }
    }

    @Test
    fun `dt = 0 step is harmless`() {
        val world = world()
        // Entities are values — captured bindings are true snapshots, never mutated by later
        // steps; velocity is captured separately (equality is fact-exclusive).
        val ball = world.ball
        val ballVelocity = world.ball.velocity
        val paddle = world.paddle

        step(world, 0.0)

        assertEquals(ball, world.ball)
        assertEquals(ballVelocity, world.ball.velocity)
        assertEquals(paddle, world.paddle)
    }
}
