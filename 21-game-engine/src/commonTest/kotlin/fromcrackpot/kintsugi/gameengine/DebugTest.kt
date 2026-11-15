package fromcrackpot.kintsugi.gameengine

import fromcrackpot.kintsugi.scenegraph.nodes.GroupNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class DebugTest {

    private fun pushed(history: FrameHistory, count: Int): List<GroupNode> =
        List(count) { GroupNode().also(history::push) }

    /* FRAME HISTORY */

    @Test
    fun `back walks toward the oldest frame and stops there`() {
        val history = FrameHistory()
        val frames = pushed(history, 3)

        assertSame(frames[1], history.back())
        assertSame(frames[0], history.back())
        assertNull(history.back())
        assertSame(frames[1], history.forward(), "a refused back leaves the cursor on the oldest")
    }

    @Test
    fun `forward walks back to the newest frame and stops there`() {
        val history = FrameHistory()
        val frames = pushed(history, 3)
        history.back()
        history.back()

        assertSame(frames[1], history.forward())
        assertSame(frames[2], history.forward())
        assertNull(history.forward())
    }

    @Test
    fun `an empty or single-frame history has nowhere to go`() {
        val history = FrameHistory()
        assertNull(history.back())
        assertNull(history.forward())

        pushed(history, 1)
        assertNull(history.back())
        assertNull(history.forward())
    }

    @Test
    fun `toHead returns the newest frame from anywhere`() {
        val history = FrameHistory()
        val frames = pushed(history, 3)
        history.back()
        history.back()

        assertSame(frames[2], history.toHead())
        assertNull(history.forward(), "the cursor is back on the newest")
    }

    @Test
    fun `capacity drops the oldest frames`() {
        val history = FrameHistory(capacity = 2)
        val frames = pushed(history, 3)

        assertSame(frames[1], history.back())
        assertNull(history.back())
    }

    /* FRAME STATS */

    @Test
    fun `no label before the window fills`() {
        val stats = FrameStats(windowNanos = 100)
        assertNull(stats.record(50))
        assertNull(stats.record(49))
    }

    @Test
    fun `the label reads the rate and the worst frame of its window`() {
        val stats = FrameStats()
        repeat(29) { assertNull(stats.record(16_000_000)) }

        assertEquals("60 fps · 36 ms", stats.record(36_000_000))
    }

    @Test
    fun `each window starts fresh`() {
        val stats = FrameStats()
        assertEquals("2 fps · 500 ms", stats.record(500_000_000))

        repeat(49) { assertNull(stats.record(10_000_000)) }
        assertEquals("100 fps · 10 ms", stats.record(10_000_000))
    }
}
