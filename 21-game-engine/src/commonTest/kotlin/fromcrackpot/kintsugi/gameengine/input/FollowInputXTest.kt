package fromcrackpot.kintsugi.gameengine.input

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.pointer.PointerType
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.gameengine.Step
import kotlin.test.Test
import kotlin.test.assertEquals

private val LEFT = Key.DirectionLeft
private val RIGHT = Key.DirectionRight

private fun follow(speed: Double = 100.0) = FollowInputX(speed, left = LEFT, right = RIGHT, dragGain = 2.0)

// Bridge: establish the step context so test bodies keep the explicit (x, dt, input) spelling.
private fun FollowInputX.dx(x: Double, dt: Double, input: Input): Double =
    context(Step(dt, input)) { dx(x) }

private fun input(pointerX: Double? = null, keysDown: Set<Key> = emptySet()): Input = Input(
    pointers = if (pointerX == null) emptyList()
    else listOf(Pointer.of(FlatPoint(pointerX, 0.0))),
    keys = Keys(keysDown, emptySet(), emptySet()),
)

private fun finger(x: Double, isDown: Boolean = true, wasPressed: Boolean = false, wasReleased: Boolean = false) =
    Pointer.of(FlatPoint(x, 0.0), isDown, wasPressed, wasReleased, PointerType.Touch)

private fun touches(vararg fingers: Pointer, keysDown: Set<Key> = emptySet()): Input =
    Input(fingers.toList(), Keys(keysDown, emptySet(), emptySet()))

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

    /* TOUCH */

    @Test
    fun `a finger drags relatively by the gain`() {
        val follow = follow()
        assertEquals(0.0, follow.dx(500.0, 0.1, touches(finger(50.0, wasPressed = true))), 1e-9, "landing grips, never jumps")
        assertEquals(20.0, follow.dx(500.0, 0.1, touches(finger(60.0))), 1e-9, "10 dragged × gain 2")
    }

    @Test
    fun `the newest finger takes over without a jump`() {
        val follow = follow()
        follow.dx(0.0, 0.1, touches(finger(50.0, wasPressed = true)))
        follow.dx(0.0, 0.1, touches(finger(60.0)))
        assertEquals(0.0, follow.dx(0.0, 0.1, touches(finger(60.0), finger(300.0, wasPressed = true))), 1e-9, "a landing re-grips")
        assertEquals(10.0, follow.dx(0.0, 0.1, touches(finger(80.0), finger(305.0))), 1e-9, "only the newest drags")
    }

    @Test
    fun `lifting the newest finger hands back to the older one without a jump`() {
        val follow = follow()
        follow.dx(0.0, 0.1, touches(finger(50.0, wasPressed = true), finger(300.0, wasPressed = true)))
        assertEquals(0.0, follow.dx(0.0, 0.1, touches(finger(50.0), finger(300.0, isDown = false, wasReleased = true))), 1e-9, "a lift re-grips")
        assertEquals(-20.0, follow.dx(0.0, 0.1, touches(finger(40.0))), 1e-9, "the older finger drags on")
    }

    @Test
    fun `a resting finger yields to the key slide`() {
        val follow = follow()
        follow.dx(0.0, 0.1, touches(finger(50.0, wasPressed = true)))
        assertEquals(10.0, follow.dx(0.0, 0.1, touches(finger(50.0), keysDown = setOf(RIGHT))), 1e-9, "a still finger commands nothing")
    }
}
