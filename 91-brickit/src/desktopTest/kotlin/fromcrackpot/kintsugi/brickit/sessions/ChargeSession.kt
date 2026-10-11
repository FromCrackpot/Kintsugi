package fromcrackpot.kintsugi.brickit.sessions

import fromcrackpot.kintsugi.brickit.BrickIt
import fromcrackpot.kintsugi.brickit.entities.FULL_CHARGE
import fromcrackpot.kintsugi.brickit.harness.SessionLog
import fromcrackpot.kintsugi.brickit.harness.loadBrickIt
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Direction
import fromcrackpot.kintsugi.cga.d2.transforms.T
import fromcrackpot.kintsugi.cga.d2.transforms.composites.times
import fromcrackpot.kintsugi.gameengine.drive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Charge scenes on the live assets (wall at y [0, 240], columns 60 wide from x = −270), under
// build/sessions/charge-*/; each row carries the charge and the brick count, the frames the look.
class ChargeSession {

    private fun scene(name: String, x: Double, y: Double, velocity: Direction, charge: Int, seconds: Double): BrickIt {
        val log = SessionLog<BrickIt>(name, stride = 2) { t, w ->
            """{"t":$t,"charge":${w.ball.charge},"bricks":${w.bricks.children.size},"ball":[${w.ball.x},${w.ball.y}],"v":[${w.ball.velocity.px},${w.ball.velocity.py}]}"""
        }
        return drive(
            world = {
                loadBrickIt().also { w ->
                    w.ball *= T(x - w.ball.x, y - w.ball.y)
                    w.ball.velocity = velocity
                    w.ball.charge = charge
                }
            },
            seconds = seconds,
            onStep = log::record,
        )
    }

    @Test
    fun `a dribble under the paddle charges the ball`() {
        val w = scene("charge-dribble", 30.0, -249.0, Direction(300.0, 300.0), charge = 0, seconds = 0.6)

        assertTrue(w.ball.charged, "charged after the dribble (charge ${w.ball.charge})")
    }

    @Test
    fun `a charged ball clears a column of normal bricks`() {
        val before = loadBrickIt().bricks.children.size
        val w = scene("charge-normals", -120.0, -100.0, Direction(0.0, 400.0), charge = FULL_CHARGE, seconds = 1.2)

        assertEquals(before - 8, w.bricks.children.size, "the whole column left the wall")
        assertTrue(w.ball.charged, "normal bricks cost no charge")
    }

    @Test
    fun `a charged ball breaks one steel brick of the gold's vault and comes back plain`() {
        val before = loadBrickIt().bricks.children.count { it.steel }
        val w = scene("charge-steel", 0.0, -100.0, Direction(0.0, 400.0), charge = FULL_CHARGE, seconds = 0.5)

        assertEquals(before - 1, w.bricks.children.count { it.steel })
        assertEquals(0, w.ball.charge)
    }
}
