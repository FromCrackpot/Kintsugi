package fromcrackpot.kintsugi.brickit

import fromcrackpot.kintsugi.brickit.entities.BALL_SPEED
import fromcrackpot.kintsugi.brickit.entities.FULL_CHARGE
import fromcrackpot.kintsugi.brickit.entities.Paddle
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Direction
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.transforms.T
import fromcrackpot.kintsugi.cga.d2.transforms.composites.times
import fromcrackpot.kintsugi.gameengine.Restart
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

    // Paddle 100×16 at PADDLE_Y → faces at −222 (top) and −238 (bottom), its line at −230; ball 16×16, floor at −300.
    // Bricks: gold spans x [-100, -40],
    // normal x [-30, 30], two steels x [40, 100] and [100, 160]; all at y [105, 135].
    private fun brick(left: Double, vararg tags: String): PolygonNode =
        PolygonNode(Polygon.Rectangle(FlatPoint(left, 105.0), FlatPoint(left + 60.0, 135.0)), tags = tags.toSet())

    private fun world(
        wall: List<PolygonNode> = listOf(brick(-100.0, "brick", "gold"), brick(-30.0), brick(40.0, "brick", "steel"), brick(100.0, "brick", "steel")),
    ): BrickIt = BrickIt(
        BrickItAssets(
            wall = wall,
            paddle = PolygonNode(Polygon.Rectangle(FlatPoint(-50.0, -8.0), FlatPoint(50.0, 8.0))),
            ball = PolygonNode(Polygon.Rectangle(FlatPoint(-8.0, -8.0), FlatPoint(8.0, 8.0)), PLAIN),
            ballCharged = CHARGED,
            sounds = TEST_SOUNDS,
        ),
    )

    private fun step(world: BrickIt) = world.step(DT)

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
    fun `the underside count stops at a full charge`() {
        val w = world().apply { hitUnderneath(); hitUnderneath(); hitUnderneath() }

        assertEquals(FULL_CHARGE, w.ball.charge)
    }

    @Test
    fun `a top-face catch keeps the charge`() {
        val w = world().apply { ball = ball.withCharge(FULL_CHARGE); hitOnTop() }

        assertTrue(w.ball.charged)
        assertEquals(CHARGED, w.ball.style)
    }

    @Test
    fun `a receding contact neither counts nor discharges`() {
        val under = world().apply { hit(Direction(0.0, -300.0), 0.0, -242.0) } // 1.5 into the underside, moving down and out
        assertEquals(0, under.ball.charge, "a push under the paddle is no rebound")

        val top = world().apply { ball = ball.withCharge(FULL_CHARGE); hit(Direction(0.0, 300.0), 20.0, -218.0) } // into the top face, moving up and out
        assertTrue(top.ball.charged, "a push off the top face keeps the charge")
    }

    @Test
    fun `the charge and both looks ride through the ball's rebuilds`() {
        val w = world().apply { hitUnderneath(); hitUnderneath() }

        repeat(5) { step(w) }
        assertTrue(w.ball.charged)
        assertEquals(CHARGED, w.ball.style)

        w.ball = w.ball.withCharge(0)
        repeat(5) { step(w) }
        assertEquals(PLAIN, w.ball.style, "a ball rebuilt while charged still knows its plain look")
    }

    /* THE PADDLE LINE */

    @Test
    fun `a partial count is lost above the paddle line - a full charge is not`() {
        val partial = world().apply { ball = ball.withCharge(1); hit(Direction(0.0, BALL_SPEED), 150.0, -232.0) } // beside the paddle, rising past its line
        assertEquals(0, partial.ball.charge)

        val full = world().apply { ball = ball.withCharge(FULL_CHARGE); hit(Direction(0.0, BALL_SPEED), 150.0, -232.0) }
        assertTrue(full.ball.charged)
    }

    // Beside the paddle, climbing past its line: the ball comes loose.
    private fun BrickIt.climbPastTheLine() = hit(Direction(0.0, BALL_SPEED), 150.0, -232.0)

    @Test
    fun `a ball climbing past the paddle line comes loose - a falling one doesn't`() {
        assertTrue(world().apply { climbPastTheLine() }.ball.loose)
        assertFalse(world().apply { hit(Direction(0.0, -BALL_SPEED), 150.0, -200.0) }.ball.loose)
    }

    @Test
    fun `the floor takes a loose ball's charge`() {
        val w = world().apply { ball = ball.withCharge(FULL_CHARGE); climbPastTheLine() }

        w.hit(Direction(0.0, -BALL_SPEED), 150.0, -228.0) // back from the arena, past the paddle
        repeat(18) { step(w) } // y = −291.3: the floor is next
        assertTrue(w.ball.charged, "charged until the floor")
        assertTrue(w.ball.loose, "the flag rides through the ball's rebuilds")

        repeat(2) { step(w) }
        assertTrue(w.ball.velocity.py > 0.0, "the floor bounced it")
        assertEquals(0, w.ball.charge)
        assertEquals(PLAIN, w.ball.style)
    }

    @Test
    fun `a dribble's floor bounce keeps the charge`() {
        val w = world().apply { climbPastTheLine(); hitUnderneath(); hitUnderneath() }
        assertFalse(w.ball.loose, "the underside plays the ball")

        repeat(20) { step(w) }

        assertTrue(w.ball.velocity.py > 0.0, "the floor bounced it")
        assertTrue(w.ball.charged)
    }

    @Test
    fun `a sidekick saves a loose ball - above the paddle line or past it`() {
        for (y in listOf(-224.0, -228.0)) { // the kick lands at y = −227.3, above the line, or −231.3, past it
            val w = world().apply { climbPastTheLine() }

            w.hit(Direction(-300.0, -BALL_SPEED), 60.0, y) // falling into the right face
            assertTrue(w.ball.charged, "the sidekick charges")
            assertFalse(w.ball.loose, "and plays the ball")

            repeat(22) { step(w) }
            assertTrue(w.ball.velocity.py > 0.0, "the floor bounced it")
            assertTrue(w.ball.charged, "the floor keeps a played ball's charge (kick from y = $y)")
        }
    }

    /* SPENDING */

    @Test
    fun `a charged ball flies through a normal brick`() {
        val w = world().apply { ball = ball.withCharge(FULL_CHARGE) }
        val target = w.bricks.children.first { !it.steel && !it.gold }

        w.hitBrickAt(0.0)

        assertTrue(w.bricks.children.none { it === target }, "the brick left the wall")
        assertEquals(Direction(0.0, BALL_SPEED), w.ball.velocity, "no bounce")
        assertEquals(FlatPoint(0.0, 96.5 + BALL_SPEED * DT), w.ball.centroid, "plain travel, no landing correction")
        assertTrue(w.ball.charged, "a normal brick costs no charge")
    }

    @Test
    fun `a charged ball breaks a steel brick - bounces off it and keeps its charge`() {
        val w = world().apply { ball = ball.withCharge(FULL_CHARGE) }
        val steel = w.bricks.children.first { it.steel }

        w.hitBrickAt(70.0)

        val travel = BALL_SPEED * DT
        val depth = (96.5 + 8.0 + travel) - 105.0
        assertTrue(w.bricks.children.none { it === steel }, "the steel brick left the wall")
        assertEquals(Direction(0.0, -BALL_SPEED), w.ball.velocity)
        assertEquals(FlatPoint(70.0, 96.5 + travel - 2.0 * depth), w.ball.centroid)
        assertTrue(w.ball.charged)
        assertEquals(CHARGED, w.ball.style)
    }

    @Test
    fun `one charge breaks steel after steel`() {
        val w = world().apply { ball = ball.withCharge(FULL_CHARGE) }

        w.hitBrickAt(100.0) // astride the two steels' seam
        assertEquals(1, w.bricks.children.count { it.steel }, "the bounce off the first clears the second")

        w.hitBrickAt(130.0)
        assertEquals(0, w.bricks.children.count { it.steel })
        assertTrue(w.ball.charged)
    }

    @Test
    fun `a gold brick restarts the round - charged ball or not`() {
        val w = world().apply { ball = ball.withCharge(FULL_CHARGE) }

        assertFailsWith<Restart> { w.hitBrickAt(-70.0) }
    }

    /* CONTACT ORDER */

    private fun brickAt(left: Double, bottom: Double, vararg tags: String): PolygonNode =
        PolygonNode(Polygon.Rectangle(FlatPoint(left, bottom), FlatPoint(left + 60.0, bottom + 30.0)), tags = tags.toSet())

    // Wall order, corner at (0, 120): the far steel above-left of it, its arms right of and below the far brick.
    private fun cornerWorld(vararg armTags: String): BrickIt =
        world(listOf(brickAt(-60.0, 120.0, "brick", "steel"), brickAt(0.0, 120.0, *armTags), brickAt(-60.0, 90.0, *armTags)))
            .apply { ball = ball.withCharge(FULL_CHARGE) }

    // The step lands the ball's top-left corner at (-1, 122): a sliver past x = 0, a rise past y = 120. Depth 1 < 2.
    private fun BrickIt.hitCorner() = hit(Direction(-300.0, BALL_SPEED), 7.0 + 300.0 * DT, 114.0 - BALL_SPEED * DT)

    @Test
    fun `a charged ball at a steel corner breaks both arms it hits - not the brick across it`() {
        val w = cornerWorld("brick", "steel")
        val far = w.bricks.children[0]

        w.hitCorner()

        assertEquals(listOf(far), w.bricks.children, "the deeper arm's bounce clears the far brick, not the other arm")
        assertEquals(Direction(300.0, -BALL_SPEED), w.ball.velocity, "both arms returned the ball")
        assertTrue(w.ball.charged)
    }

    @Test
    fun `a charged ball past normal arms still breaks the steel across the corner`() {
        val w = cornerWorld()
        val far = w.bricks.children[0]

        w.hitCorner()

        assertTrue(w.bricks.children.none { it === far }, "no arm stops a charged ball: the steel contact is real")
        assertTrue(w.ball.charged)
    }
}
