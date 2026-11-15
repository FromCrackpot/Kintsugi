package fromcrackpot.kintsugi.scratchpad

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SketchesTest {

    /* sketchFor */

    @Test
    fun `null resolves to the default sketch`() {
        assertEquals(DEFAULT_SKETCH, sketchFor(null).name)
    }

    @Test
    fun `blank resolves to the default sketch`() {
        assertEquals(DEFAULT_SKETCH, sketchFor("").name)
        assertEquals(DEFAULT_SKETCH, sketchFor("   ").name)
    }

    @Test
    fun `lookup normalizes case and surrounding whitespace`() {
        assertEquals("logo", sketchFor("  LoGo  ").name)
    }

    @Test
    fun `unknown name falls back to the default sketch`() {
        assertEquals(DEFAULT_SKETCH, sketchFor("no-such-sketch").name)
    }

    @Test
    fun `every registered sketch resolves by its own name`() {
        for (sketch in sketches) {
            assertEquals(sketch.name, sketchFor(sketch.name).name)
        }
    }

    /* SNAPSHOTS */

    @Test
    fun `every registered snapshot builds a bounded group without throwing`() {
        // A stale SMILEY_SVG or a degenerate scene primitive fails here, not at app launch.
        // Emptiness is not asserted: the empty sketch is deliberately an empty prototyping slate.
        for (sketch in sketches) {
            val group = runBlocking { sketch.snapshot() }
            assertNotNull(group.bounds, "sketch '${sketch.name}' snapshot must carry bounds")
        }
    }
}
