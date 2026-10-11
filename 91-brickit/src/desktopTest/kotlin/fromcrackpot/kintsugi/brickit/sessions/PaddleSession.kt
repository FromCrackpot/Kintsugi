package fromcrackpot.kintsugi.brickit.sessions

import androidx.compose.ui.input.key.Key
import fromcrackpot.kintsugi.brickit.ARENA
import fromcrackpot.kintsugi.brickit.BrickIt
import fromcrackpot.kintsugi.brickit.harness.SessionLog
import fromcrackpot.kintsugi.brickit.harness.loadBrickIt
import fromcrackpot.kintsugi.cga.d2.queries.escape
import fromcrackpot.kintsugi.cga.d2.shapes.composites.RectSide.BOTTOM
import fromcrackpot.kintsugi.cga.d2.shapes.composites.get
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.round
import fromcrackpot.kintsugi.gameengine.Controls
import fromcrackpot.kintsugi.gameengine.drive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PaddleSession {

    @Test
    fun `scripted session - key slides clamp at the wall, the pointer overrides`() {
        val halfWidth = loadBrickIt().paddle.edges[BOTTOM].radius
        val rightLimit = 400.0 - halfWidth
        val log = SessionLog<BrickIt>("paddle") { elapsed, w ->
            """{"t":$elapsed,"paddle":${w.paddle.x},"ball":[${w.ball.x},${w.ball.y}],"bricks":${w.bricks.children.size}}"""
        }

        var maxX = Double.NEGATIVE_INFINITY
        var lastPointerX = 0.0
        val world = drive(::loadBrickIt, seconds = 4.0, onStep = { elapsed, w ->
            log.record(elapsed, w)
            maxX = maxOf(maxX, w.paddle.x)
            assertNull(w.paddle escape ARENA, "paddle stays inside the arena") // tilted at the wall, its x may exceed the level limit
        }) { elapsed, _ ->
            when {
                elapsed < 2.0 -> Controls(keys = setOf(Key.DirectionRight))
                elapsed < 3.0 -> Controls(keys = setOf(Key.DirectionLeft))
                else -> {
                    lastPointerX = -350.0 + 100.0 * (elapsed - 3.0)
                    Controls(pointer = FlatPoint(lastPointerX, 0.0))
                }
            }
        }

        assertTrue(round(maxX) >= rightLimit, "the right wall was reached")
        assertEquals(round(lastPointerX), round(world.paddle.x), "the paddle follows the pointer 1:1")
    }
}
