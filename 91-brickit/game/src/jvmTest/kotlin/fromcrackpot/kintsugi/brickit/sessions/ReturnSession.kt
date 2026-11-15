package fromcrackpot.kintsugi.brickit.sessions

import fromcrackpot.kintsugi.brickit.BrickIt
import fromcrackpot.kintsugi.brickit.harness.SessionLog
import fromcrackpot.kintsugi.brickit.harness.loadBrickIt
import fromcrackpot.kintsugi.cga.d2.shapes.composites.RectSide.BOTTOM
import fromcrackpot.kintsugi.cga.d2.shapes.composites.get
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Direction
import fromcrackpot.kintsugi.cga.d2.transforms.T
import fromcrackpot.kintsugi.cga.d2.transforms.composites.times
import fromcrackpot.kintsugi.gameengine.drive
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

// Paddle-return scenes on the live assets (paddle faces at −220/−240, ball half-size 7), a frame every step
// under build/sessions/return-*/; each row carries the tilt and the ball velocity so every contact is readable.
class ReturnSession {

    private fun scene(name: String, x: Double, y: Double, velocity: Direction, seconds: Double) {
        val log = SessionLog<BrickIt>(name, stride = 1) { t, w ->
            val tilt = w.paddle.edges[BOTTOM].attitude.angle.degrees
            """{"t":$t,"tilt":$tilt,"ball":[${w.ball.x},${w.ball.y}],"v":[${w.ball.velocity.px},${w.ball.velocity.py}],"paddle":${w.paddle.x}}"""
        }
        var maxTilt = 0.0
        drive(
            world = {
                loadBrickIt().also { w ->
                    w.ball *= T(x - w.ball.x, y - w.ball.y)
                    w.ball.velocity = velocity
                }
            },
            seconds = seconds,
            onStep = { t, w ->
                log.record(t, w)
                maxTilt = maxOf(maxTilt, abs(w.paddle.edges[BOTTOM].attitude.angle.degrees))
            },
        )
        assertTrue(maxTilt > 29.9, "$name: the servo reached max tilt (saw $maxTilt°)")
    }

    @Test
    fun `top hit halfway to the tip`() = scene("return-top-half", 30.0, -211.0, Direction(300.0, -300.0), 0.5)

    @Test
    fun `top hit at the tip against the drift`() = scene("return-top-tip", 62.0, -211.0, Direction(-300.0, -300.0), 0.5)

    @Test
    fun `dribble under the paddle`() = scene("return-dribble", 30.0, -249.0, Direction(300.0, 300.0), 1.5)

    @Test
    fun `swat at the tip`() = scene("return-tip-swat", 52.0, -249.0, Direction(300.0, 300.0), 0.5)
}
