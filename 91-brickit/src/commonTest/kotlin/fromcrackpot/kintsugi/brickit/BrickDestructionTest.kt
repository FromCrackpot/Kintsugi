package fromcrackpot.kintsugi.brickit

import fromcrackpot.kintsugi.brickit.entities.BALL_SPEED
import fromcrackpot.kintsugi.brickit.entities.Ball
import fromcrackpot.kintsugi.brickit.entities.Brick
import fromcrackpot.kintsugi.brickit.entities.BrickWall
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
import fromcrackpot.kintsugi.scenegraph.style.ShapeStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private const val DT = 1.0 / 120

class BrickDestructionTest {

    // Bricks pre-placement (init shifts the wall to BRICKS_Y = 120): gold spans x [-100, -40],
    // normal x [-30, 30], steel x [40, 100]; all land at y [105, 135].
    private fun world(): BrickIt = BrickIt(
        arena = PolygonNode(Polygon.Rectangle(FlatPoint(-400.0, -300.0), FlatPoint(400.0, 300.0))),
        bricks = BrickWall(
            listOf(
                Brick(
                    PolygonNode(
                        Polygon.Rectangle(FlatPoint(-100.0, -15.0), FlatPoint(-40.0, 15.0)),
                        tags = setOf("brick", "gold"),
                    ),
                ),
                Brick(PolygonNode(Polygon.Rectangle(FlatPoint(-30.0, -15.0), FlatPoint(30.0, 15.0)))),
                Brick(
                    PolygonNode(
                        Polygon.Rectangle(FlatPoint(40.0, -15.0), FlatPoint(100.0, 15.0)),
                        tags = setOf("brick", "steel"),
                    ),
                ),
            ),
        ),
        paddle = Paddle(PolygonNode(Polygon.Rectangle(FlatPoint(-50.0, -8.0), FlatPoint(50.0, 8.0)))),
        ball = Ball(PolygonNode(Polygon.Rectangle(FlatPoint(-8.0, -8.0), FlatPoint(8.0, 8.0))), ShapeStyle.POLYGON),
    )

    private fun step(world: BrickIt, dt: Double) = context(Step(dt, Input.NONE)) { world.update() }

    // Ball centroid to (x, 96.5): top edge 0.5 below the bricks' bottom face at y = 105, one
    // full step from contact; velocity straight up so only the aimed brick is hit.
    private fun BrickIt.aimBallUpAt(x: Double) {
        ball *= T(x - ball.centroid.px, 96.5 - ball.centroid.py)
        ball.velocity = Direction(0.0, BALL_SPEED)
    }

    @Test
    fun `a normal brick dies on contact — removed by identity, ball reflected`() {
        val world = world()
        val target = world.bricks.children.first { !it.steel && !it.gold }
        val steel = world.bricks.children.first { it.steel }
        world.aimBallUpAt(0.0)

        step(world, DT)

        val travel = BALL_SPEED * DT
        val depth = (96.5 + 8.0 + travel) - 105.0
        assertTrue(depth > 0.0, "scenario must penetrate; depth was $depth")
        assertTrue(world.bricks.children.none { it === target }, "the hit brick left the wall")
        assertTrue(world.bricks.children.any { it === steel }, "the unhit brick stayed")
        assertEquals(Direction(0.0, -BALL_SPEED), world.ball.velocity)
        // Time-exact continuation: overshoot mirrored, ball strictly below the wall by `depth`.
        assertEquals(FlatPoint(0.0, 96.5 + travel - 2.0 * depth), world.ball.centroid)
    }

    @Test
    fun `a steel brick deflects and survives`() {
        val world = world()
        val steel = world.bricks.children.first { it.steel }
        world.aimBallUpAt(70.0)

        step(world, DT)

        val travel = BALL_SPEED * DT
        val depth = (96.5 + 8.0 + travel) - 105.0
        assertTrue(world.bricks.children.any { it === steel }, "steel survived the hit")
        assertEquals(3, world.bricks.children.size, "nothing left the wall")
        assertEquals(Direction(0.0, -BALL_SPEED), world.ball.velocity)
        assertEquals(FlatPoint(70.0, 96.5 + travel - 2.0 * depth), world.ball.centroid)
    }

    @Test
    fun `a gold brick ends the step with the restart signal, before any bounce`() {
        val world = world()
        world.aimBallUpAt(-70.0)

        assertFailsWith<Restart>("the gold contact restarts the round") { step(world, DT) }

        assertEquals(3, world.bricks.children.size, "the discarded world's wall is untouched")
        assertEquals(Direction(0.0, BALL_SPEED), world.ball.velocity, "no bounce ran — the step ended at the signal")
    }
}
