package fromcrackpot.kintsugi.brickit

import fromcrackpot.kintsugi.brickit.entities.BALL_SPEED
import fromcrackpot.kintsugi.brickit.entities.BRICK_FRAGMENTS
import fromcrackpot.kintsugi.brickit.entities.Ball
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.composites.split
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Direction
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.transforms.T
import fromcrackpot.kintsugi.cga.d2.transforms.composites.times
import fromcrackpot.kintsugi.gameengine.Restart
import fromcrackpot.kintsugi.scenegraph.nodes.GroupNode
import fromcrackpot.kintsugi.scenegraph.nodes.composites.PolygonNode
import fromcrackpot.kintsugi.scenegraph.nodes.flattenShapes
import fromcrackpot.kintsugi.scenegraph.style.ShapeColor
import fromcrackpot.kintsugi.scenegraph.style.ShapeStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private const val DT = 1.0 / 120

class BrickDestructionTest {

    // Bricks: gold spans x [-100, -40],
    // normal x [-30, 30], steel x [40, 100]; all at y [105, 135].
    private fun world(
        wall: List<PolygonNode> = listOf(
            PolygonNode(Polygon.Rectangle(FlatPoint(-100.0, 105.0), FlatPoint(-40.0, 135.0)), tags = setOf("brick", "gold")),
            PolygonNode(Polygon.Rectangle(FlatPoint(-30.0, 105.0), FlatPoint(30.0, 135.0)), ShapeStyle.POLYGON.copy(fill = ShapeColor.WHITE)),
            PolygonNode(Polygon.Rectangle(FlatPoint(40.0, 105.0), FlatPoint(100.0, 135.0)), tags = setOf("brick", "steel")),
        ),
    ): BrickIt = BrickIt(
        BrickItAssets(
            wall = wall,
            paddle = PolygonNode(Polygon.Rectangle(FlatPoint(-50.0, -8.0), FlatPoint(50.0, 8.0))),
            ball = PolygonNode(Polygon.Rectangle(FlatPoint(-8.0, -8.0), FlatPoint(8.0, 8.0))),
            ballCharged = ShapeStyle.POLYGON,
            sounds = TEST_SOUNDS,
        ),
    )

    private fun step(world: BrickIt, dt: Double) = world.step(dt)

    // Ball centroid to (x, 96.5): top edge 0.5 below the bricks' bottom face at y = 105, one
    // full step from contact; velocity straight up so only the aimed brick is hit.
    private fun BrickIt.aimBallUpAt(x: Double) {
        ball *= T(x - ball.centroid.px, 96.5 - ball.centroid.py)
        ball.velocity = Direction(0.0, BALL_SPEED)
    }

    @Test
    fun `a normal brick dies on contact — removed by identity - ball reflected`() {
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
    fun `a destroyed brick explodes into its split - in its style - drawn over the scene`() {
        val world = world()
        val target = world.bricks.children.first { !it.steel && !it.gold }
        world.aimBallUpAt(0.0)
        step(world, DT)
        val debris = world.root.children.last() as GroupNode
        val expected = target.split(BRICK_FRAGMENTS).map { PolygonNode(it, target.style) }
        assertEquals(expected, debris.flattenShapes(), "the pieces fly as split, in the brick's style")
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
    fun `a gold brick ends the step with the restart signal - before any bounce`() {
        val world = world()
        world.aimBallUpAt(-70.0)

        assertFailsWith<Restart>("the gold contact restarts the round") { step(world, DT) }

        assertEquals(3, world.bricks.children.size, "the discarded world's wall is untouched")
        assertEquals(Direction(0.0, BALL_SPEED), world.ball.velocity, "no bounce ran — the step ended at the signal")
    }

    /* CONTACT ORDER */

    private fun brickAt(left: Double, bottom: Double, vararg tags: String): PolygonNode =
        PolygonNode(Polygon.Rectangle(FlatPoint(left, bottom), FlatPoint(left + 60.0, bottom + 30.0)), tags = tags.toSet())

    // Wall order, corner at (0, 120): the far brick above-left of it, its arms right of and below the far brick.
    private fun cornerWorld(vararg farTags: String): BrickIt = world(
        listOf(
            brickAt(-60.0, 120.0, *farTags),
            brickAt(0.0, 120.0, "brick", "steel"),
            brickAt(-60.0, 90.0, "brick", "steel"),
        ),
    )

    // The step lands the ball's top-left corner at (-1, 122): a sliver past x = 0, a rise past y = 120. Depth 1 < 2.
    private fun BrickIt.landAcross(vx: Double) {
        ball *= T(7.0 - vx * DT - ball.centroid.px, 114.0 - BALL_SPEED * DT - ball.centroid.py)
        ball.velocity = Direction(vx, BALL_SPEED)
        step(this, DT)
    }

    @Test
    fun `an uncharged ball at a steel corner leaves the brick across it alive`() {
        val world = cornerWorld()

        world.landAcross(-300.0)

        assertEquals(3, world.bricks.children.size, "the far brick stayed")
        assertEquals(Direction(300.0, -BALL_SPEED), world.ball.velocity, "both steel faces returned the ball")
    }

    @Test
    fun `a gold brick behind a steel corner does not restart the round`() {
        val world = cornerWorld("brick", "gold")

        world.landAcross(-300.0)

        assertEquals(1, world.bricks.children.count { it.gold })
        assertEquals(Direction(300.0, -BALL_SPEED), world.ball.velocity)
    }

    @Test
    fun `a sliver over a neighbouring brick kills only the brick hit`() {
        val world = world(listOf(brickAt(-60.0, 120.0), brickAt(0.0, 120.0)))
        val neighbour = world.bricks.children[0]

        world.landAcross(0.0)

        assertEquals(listOf(neighbour), world.bricks.children, "the neighbour stayed")
        assertEquals(Direction(0.0, -BALL_SPEED), world.ball.velocity)
    }
}
