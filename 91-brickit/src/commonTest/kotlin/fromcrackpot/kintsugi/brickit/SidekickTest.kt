package fromcrackpot.kintsugi.brickit

import androidx.compose.ui.input.key.Key
import fromcrackpot.kintsugi.brickit.entities.Ball
import fromcrackpot.kintsugi.brickit.entities.BrickWall
import fromcrackpot.kintsugi.brickit.entities.PADDLE_SPEED
import fromcrackpot.kintsugi.brickit.entities.PADDLE_Y
import fromcrackpot.kintsugi.brickit.entities.Paddle
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Direction
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.transforms.T
import fromcrackpot.kintsugi.cga.d2.transforms.composites.times
import fromcrackpot.kintsugi.gameengine.Input
import fromcrackpot.kintsugi.gameengine.Step
import fromcrackpot.kintsugi.scenegraph.nodes.composites.PolygonNode
import fromcrackpot.kintsugi.scenegraph.style.ShapeStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

private const val DT = 1.0 / 120

class SidekickTest {

    // Paddle 100×16 at PADDLE_Y → faces at −222 (top) and −238 (bottom); ball 16×16.
    private fun world(): BrickIt = BrickIt(
        arena = PolygonNode(Polygon.Rectangle(FlatPoint(-400.0, -300.0), FlatPoint(400.0, 300.0))),
        bricks = BrickWall(emptyList()),
        paddle = Paddle(PolygonNode(Polygon.Rectangle(FlatPoint(-50.0, -8.0), FlatPoint(50.0, 8.0)))),
        ball = Ball(PolygonNode(Polygon.Rectangle(FlatPoint(-8.0, -8.0), FlatPoint(8.0, 8.0))), ShapeStyle.POLYGON),
    )

    private val pushRight = Input.of(keysDown = setOf(Key.DirectionRight))

    private fun step(world: BrickIt, input: Input = pushRight) = context(Step(DT, input)) { world.update() }

    private fun BrickIt.placeBall(x: Double, y: Double) {
        ball *= T(x - ball.x, y - ball.y)
    }

    @Test
    fun `a side push shoves the ball at the paddle's pace without reflecting it`() {
        val world = world()
        // Beside the paddle's right face (edge at 50), 2 units of gap; drifting right slower than the paddle.
        world.placeBall(60.0, PADDLE_Y)
        world.ball.velocity = Direction(100.0, 0.0)

        step(world)

        val travel = 100.0 * DT
        val depth = (50.0 + PADDLE_SPEED * DT) - (60.0 - 8.0 + travel)
        assertTrue(depth > 0.0, "scenario must penetrate; depth was $depth")
        assertEquals(Direction(100.0, 0.0), world.ball.velocity, "receding contact: the push lane keeps the velocity")
        assertEquals(FlatPoint(60.0 + travel + depth, PADDLE_Y), world.ball.centroid, "resolved by one depth along the normal")
    }

    @Test
    fun `a sustained push shoves step after step without re-reflecting — the ratchet cannot arm`() {
        val world = world()
        world.placeBall(60.0, PADDLE_Y)
        world.ball.velocity = Direction(100.0, 0.0)
        step(world)
        val shoved = world.ball.velocity

        repeat(5) {
            step(world)
            assertEquals(shoved, world.ball.velocity, "every fresh contact resolves as a push")
        }
    }

    @Test
    fun `a wall-pinned paddle pushes as a static obstacle`() {
        val world = world()
        // Paddle pinned at the right wall (right edge at 400); ball approaching its left face.
        world.paddle *= T(350.0 - world.paddle.x, 0.0)
        world.placeBall(294.0, PADDLE_Y)
        world.ball.velocity = Direction(100.0, 0.0)

        step(world)

        assertEquals(FlatPoint(350.0, PADDLE_Y), world.paddle.centroid, "the clamp cancels the push")
        assertEquals(Direction(-100.0, 0.0), world.ball.velocity, "plain static reflection — no paddle speed imparted")
    }

    @Test
    fun `a top-face return reads where the paddle is, never how fast it moves`() {
        // Same top-face hit; one paddle slides right underneath, the other already stands where the slide ends.
        val incoming = Direction(300.0, -300.0)
        val still = world().apply { paddle *= T(PADDLE_SPEED * DT, 0.0); placeBall(20.0, -212.0); ball.velocity = incoming }
        val sliding = world().apply { placeBall(20.0, -212.0); ball.velocity = incoming }

        step(still, Input.NONE)
        step(sliding)

        assertEquals(still.paddle.centroid, sliding.paddle.centroid, "scenario: both paddles meet the ball at the same place")
        assertNotEquals(incoming, still.ball.velocity, "scenario must bounce")
        assertEquals(still.ball.velocity, sliding.ball.velocity, "the hit point alone sets the return — no paddle speed imparted")
    }
}
