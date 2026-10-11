package fromcrackpot.kintsugi.gameengine

import androidx.compose.ui.input.key.Key
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InputLatchTest {

    private val p = FlatPoint(1.0, 2.0)
    private val mouse = 0L

    /* POINTER EDGES */

    @Test
    fun `click shorter than a frame is not lost - both edges latched`() {
        val latch = InputLatch()
        latch.press(mouse, p)
        latch.release(mouse, p, ends = false)

        val input = latch.consumeEdges()
        val pointer = input.pointer!!
        assertFalse(pointer.isDown)
        assertTrue(pointer.wasPressed)
        assertTrue(pointer.wasReleased)
    }

    @Test
    fun `edges are consumed once - second read is level only`() {
        val latch = InputLatch()
        latch.press(mouse, p)

        assertTrue(latch.consumeEdges().pointer!!.wasPressed)
        val second = latch.consumeEdges().pointer!!
        assertFalse(second.wasPressed)
        assertTrue(second.isDown, "level state survives edge consumption")
    }

    @Test
    fun `level snapshot carries no edges but keeps position and down state`() {
        val latch = InputLatch()
        latch.press(mouse, p)

        val input = latch.level()
        val pointer = input.pointer!!
        assertTrue(pointer.isDown)
        assertFalse(pointer.wasPressed)
        assertEquals(p, pointer.position)

        assertTrue(latch.consumeEdges().pointer!!.wasPressed, "level() must not consume the latch")
    }

    @Test
    fun `mouse persists after release as hover - touch is dropped after its release edge`() {
        val latch = InputLatch()
        latch.press(mouse, p)
        latch.release(mouse, p, ends = false)
        latch.press(7L, p)
        latch.release(7L, p, ends = true)

        assertEquals(2, latch.consumeEdges().pointers.size, "both report their release edge")
        assertEquals(1, latch.consumeEdges().pointers.size, "touch dropped, mouse hovers on")
    }

    @Test
    fun `move creates a hovering pointer and updates position`() {
        val latch = InputLatch()
        latch.move(mouse, p)

        val pointer = latch.consumeEdges().pointer!!
        assertFalse(pointer.isDown)
        assertFalse(pointer.wasPressed)
        assertEquals(p, pointer.position)

        val q = FlatPoint(3.0, 4.0)
        latch.move(mouse, q)
        assertEquals(q, latch.level().pointer!!.position)
    }

    /* KEY EDGES */

    @Test
    fun `key press and release inside one frame latch both edges`() {
        val latch = InputLatch()
        latch.keyDown(Key.Spacebar)
        latch.keyUp(Key.Spacebar)

        val keys = latch.consumeEdges().keys
        assertFalse(keys.isDown(Key.Spacebar))
        assertTrue(keys.wasPressed(Key.Spacebar))
        assertTrue(keys.wasReleased(Key.Spacebar))

        assertFalse(latch.consumeEdges().keys.wasPressed(Key.Spacebar), "edges cleared on consume")
    }

    @Test
    fun `OS auto-repeat does not re-latch a press`() {
        val latch = InputLatch()
        latch.keyDown(Key.A)
        latch.consumeEdges()

        latch.keyDown(Key.A) // auto-repeat while held
        val keys = latch.consumeEdges().keys
        assertTrue(keys.isDown(Key.A))
        assertFalse(keys.wasPressed(Key.A))
    }

    @Test
    fun `level keys snapshot has no edges`() {
        val latch = InputLatch()
        latch.keyDown(Key.A)

        val keys = latch.level().keys
        assertTrue(keys.isDown(Key.A))
        assertFalse(keys.wasPressed(Key.A))
    }
}
