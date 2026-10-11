package fromcrackpot.kintsugi.svg

import fromcrackpot.kintsugi.scenegraph.SceneBounds
import fromcrackpot.kintsugi.svg.parsing.parseViewBox
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ViewBoxTest {

    /*
    PARSE
     */

    @Test
    fun `parseViewBox accepts space-separated tokens`() {
        assertEquals(SceneBounds(0.0, 0.0, 100.0, 50.0), parseViewBox("0 0 100 50"))
    }

    @Test
    fun `parseViewBox accepts comma-separated tokens`() {
        assertEquals(SceneBounds(-10.0, -20.0, 30.0, 40.0), parseViewBox("-10, -20, 30, 40"))
    }

    @Test
    fun `parseViewBox accepts mixed whitespace and commas`() {
        assertEquals(SceneBounds(0.0, 0.0, 100.0, 50.0), parseViewBox("0,  0 100,50"))
    }

    @Test
    fun `parseViewBox returns null when token count is wrong`() {
        assertNull(parseViewBox("0 0 100"))
        assertNull(parseViewBox("0 0 100 50 60"))
    }

    @Test
    fun `parseViewBox returns null when a token is non-numeric`() {
        assertNull(parseViewBox("0 0 abc 50"))
    }

    @Test
    fun `parseViewBox returns null for invalid dimensions`() {
        assertNull(parseViewBox("0 0 0 50"))
        assertNull(parseViewBox("0 0 100 -1"))
    }
}
