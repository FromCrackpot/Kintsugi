package fromcrackpot.kintsugi.scratchpad

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ScenesTest {

    /* sceneFor */

    @Test
    fun `null resolves to the default scene`() {
        assertEquals(DEFAULT_SCENE, sceneFor(null).name)
    }

    @Test
    fun `blank resolves to the default scene`() {
        assertEquals(DEFAULT_SCENE, sceneFor("").name)
        assertEquals(DEFAULT_SCENE, sceneFor("   ").name)
    }

    @Test
    fun `lookup normalizes case and surrounding whitespace`() {
        assertEquals("logo", sceneFor("  LoGo  ").name)
    }

    @Test
    fun `unknown name falls back to the default scene`() {
        assertEquals(DEFAULT_SCENE, sceneFor("no-such-scene").name)
    }

    @Test
    fun `every registered scene resolves by its own name`() {
        for (scene in scenes) {
            assertEquals(scene.name, sceneFor(scene.name).name)
        }
    }

    /* SNAPSHOTS */

    @Test
    fun `every registered snapshot builds a bounded group without throwing`() {
        // A stale SMILEY_SVG or a degenerate scene primitive fails here, not at app launch.
        // Emptiness is not asserted — the empty scene is deliberately an empty prototyping slate.
        for (scene in scenes) {
            val group = runBlocking { scene.snapshot() }
            assertNotNull(group.bounds, "scene '${scene.name}' snapshot must carry bounds")
        }
    }
}
