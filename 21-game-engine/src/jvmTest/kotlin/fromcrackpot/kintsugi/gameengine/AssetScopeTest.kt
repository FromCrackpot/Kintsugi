package fromcrackpot.kintsugi.gameengine

import fromcrackpot.kintsugi.scenegraph.nodes.composites.PolygonNode
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * JVM-only (needs `runBlocking`; kotlinx-coroutines-core rides in transitively via Compose).
 * Exercises the composed-load path of [AssetScope.loadGroup]: scan → sibling-relative fetch →
 * transitive re-scan → single parse. Parse semantics themselves are pinned in `:svg`'s
 * `UseExpansionTest`; here the subject is the fetch orchestration.
 */
class AssetScopeTest {

    private val brickTemplate = """<?xml version="1.0" encoding="UTF-8"?>
        <svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" width="40" height="20" viewBox="0 0 40 20">
          <rect x="0" y="0" width="40" height="20" k:tags="brick"/>
        </svg>"""

    private val gemTemplate = """<?xml version="1.0" encoding="UTF-8"?>
        <svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" width="20" height="10" viewBox="0 0 20 10">
          <rect x="0" y="0" width="20" height="10" k:tags="gem"/>
        </svg>"""

    private val compositeTemplate = """<?xml version="1.0" encoding="UTF-8"?>
        <svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" width="40" height="20" viewBox="0 0 40 20">
          <use href="gem.svg" x="10" y="5"/>
        </svg>"""

    private val wallLayout = """<?xml version="1.0" encoding="UTF-8"?>
        <svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 200 100">
          <use href="steel.svg" x="10" y="10" k:tags="steel"/>
          <use href="composite.svg" x="60" y="10"/>
        </svg>"""

    private fun scope(files: Map<String, String>) = AssetScope { path ->
        (files[path] ?: error("no such asset: $path (have: ${files.keys})")).encodeToByteArray()
    }

    /* COMPOSED LOAD */

    @Test fun `resolves templates relative to the layout's directory, transitively`() = runBlocking {
        val scope = scope(
            mapOf(
                "files/wall.svg" to wallLayout,
                "files/steel.svg" to brickTemplate,
                "files/composite.svg" to compositeTemplate,
                "files/gem.svg" to gemTemplate,
            ),
        )
        val bricks = scope.loadAll<PolygonNode>("files/wall.svg")
        assertEquals(2, bricks.size)
        assertEquals(setOf("brick", "steel"), bricks[0].tags)
        assertEquals(setOf("gem"), bricks[1].tags)
    }

    @Test fun `documents without refs skip the fetch pass`() = runBlocking {
        val scope = scope(mapOf("steel.svg" to brickTemplate))
        // Root-level path (no directory) resolves hrefs as bare names — and a ref-free document
        // never fetches at all: the fake would throw on any unexpected path.
        val brick = scope.load<PolygonNode>("steel.svg")
        assertEquals(setOf("brick"), brick.tags)
    }

    @Test fun `missing template file surfaces the asset path`() = runBlocking {
        val scope = scope(mapOf("files/wall.svg" to wallLayout))
        val e = assertFailsWith<IllegalStateException> { scope.loadGroup("files/wall.svg") }
        assertEquals(true, e.message?.contains("files/steel.svg"))
    }

    /* PATH ARITHMETIC */

    @Test fun `siblingPath joins against the document directory`() {
        assertEquals("files/steel.svg", siblingPath("files/wall.svg", "steel.svg"))
        assertEquals("a/b/steel.svg", siblingPath("a/b/wall.svg", "steel.svg"))
        assertEquals("steel.svg", siblingPath("wall.svg", "steel.svg"))
    }
}
