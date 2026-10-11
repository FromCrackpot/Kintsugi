package fromcrackpot.kintsugi.gameengine

import androidx.compose.ui.input.key.Key
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import kotlin.test.Test
import kotlin.test.assertEquals

private val LEFT = Key.DirectionLeft
private val RIGHT = Key.DirectionRight

private fun follow(speed: Double = 100.0) = FollowInputX(speed, left = LEFT, right = RIGHT)

// Bridge: establish the step context so test bodies keep the explicit (x, dt, input) spelling.
private fun FollowInputX.dx(x: Double, dt: Double, input: Input): Double =
    context(Step(dt, input)) { dx(x) }

private fun input(pointerX: Double? = null, keysDown: Set<Key> = emptySet()): Input = Input(
    pointers = if (pointerX == null) emptyList()
    else listOf(Pointer(FlatPoint(pointerX, 0.0), isDown = false, wasPressed = false, wasReleased = false)),
    keys = Keys(keysDown, emptySet(), emptySet()),
)

class FollowInputXTest {

    @Test
    fun `a moved pointer commands the x absolutely`() {
        val follow = follow()
        assertEquals(50.0, follow.dx(0.0, 0.1, input(pointerX = 50.0, keysDown = setOf(RIGHT))), 1e-9, "first pointer sample counts as moved and wins over the keys")
    }

    @Test
    fun `a still pointer yields to the key slide`() {
        val follow = follow()
        var x = 0.0
        x += follow.dx(x, 0.1, input(pointerX = 50.0, keysDown = setOf(RIGHT)))
        x += follow.dx(x, 0.1, input(pointerX = 50.0, keysDown = setOf(RIGHT)))
        assertEquals(60.0, x, 1e-9, "identical sampled x = still pointer, held key slides")
    }

    @Test
    fun `no pointer slides by the keys alone`() {
        val follow = follow()
        assertEquals(-50.0, follow.dx(0.0, 0.5, input(pointerX = null, keysDown = setOf(LEFT))), 1e-9, "axis * speed * dt from rest")
    }

    @Test
    fun `both keys held cancel to rest`() {
        val follow = follow()
        assertEquals(0.0, follow.dx(0.0, 0.5, input(pointerX = null, keysDown = setOf(LEFT, RIGHT))), 1e-9, "right and left cancel")
    }

    @Test
    fun `no keys and a still pointer hold position`() {
        val follow = follow()
        var x = 0.0
        x += follow.dx(x, 0.1, input(pointerX = 50.0))
        x += follow.dx(x, 0.1, input(pointerX = 50.0))
        assertEquals(50.0, x, 1e-9, "no command, no motion")
    }
}
