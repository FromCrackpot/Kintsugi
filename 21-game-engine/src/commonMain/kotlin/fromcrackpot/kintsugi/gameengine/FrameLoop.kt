package fromcrackpot.kintsugi.gameengine

import androidx.compose.ui.input.key.Key
import fromcrackpot.kintsugi.gameengine.input.Input
import fromcrackpot.kintsugi.gameengine.input.InputLatch
import fromcrackpot.kintsugi.gameengine.input.Keys
import fromcrackpot.kintsugi.scenegraph.nodes.GroupNode

/**
 * The semi-fixed-timestep loop behind [Game], fed one absolute frame time per display frame: it
 * builds the world, steps it, publishes [root] only when the world draws differently, and hands
 * [play] the distinct sounds its steps played, once per frame.
 *
 * - A frame with no world builds one. A frame with no anchor only anchors, at launch and restart alike.
 * - [interrupt] pauses stepping until a frame brings a press. The debug pause is separate.
 * - A loop built with `interruptible = false` ignores [interrupt] and keeps stepping.
 */
internal class FrameLoop<W : World>(
    private val world: suspend () -> W,
    tickRate: Double,
    private val maxFrameTime: Double,
    private val play: (Collection<Sound>) -> Unit = {},
    private val interruptible: Boolean = true,
) {
    val latch = InputLatch()

    var root: GroupNode = GroupNode()
        private set

    var fpsLabel: String? = null
        private set

    var interrupted: Boolean = false
        private set

    private val stepSize = 1.0 / tickRate
    private var live: W? = null
    private var last: Long? = null
    private var paused = false
    private var showFps = false
    private val history = FrameHistory()
    private val stats = FrameStats()
    private val heard = LinkedHashSet<Sound>()

    suspend fun frame(now: Long) {
        val w = live ?: return rebuild()
        val anchor = last
        // FrameLoopTest: no catch-up after unpause. Paused frames advance the anchor too.
        last = now
        if (anchor == null) return
        val frameInput = latch.consumeEdges()
        if (interrupted && frameInput.pressesAnything()) interrupted = false

        if (GAME_DEBUG) {
            debugKeys(now - anchor, frameInput.keys)
            if (paused) {
                if (frameInput.keys.wasPressed(Key.PageDown)) {
                    val next = history.forward()
                    if (next != null) root = next else publishing(w) { w.advance(stepSize, frameInput) }
                }
                return
            }
        }
        if (interrupted) return

        publishing(w) {
            var acc = ((now - anchor) / 1e9).coerceAtMost(maxFrameTime)
            var input = frameInput
            while (acc >= stepSize) {
                w.advance(stepSize, input)
                input = latch.level()
                acc -= stepSize
            }
            w.advance(acc, input)
        }
    }

    private fun W.advance(dt: Double, input: Input) {
        step(dt, input)
        heard += played
    }

    fun interrupt() {
        if (interruptible) interrupted = true
    }

    private fun debugKeys(interval: Long, keys: Keys) {
        val label = stats.record(interval)
        if (keys.wasPressed(Key.MoveEnd)) showFps = !showFps
        fpsLabel = if (showFps) label ?: fpsLabel else null
        if (keys.wasPressed(Key.PageUp)) {
            paused = true
            history.back()?.let { root = it }
        } else if (keys.wasPressed(Key.MoveHome)) {
            paused = !paused
            if (!paused) root = history.toHead()
        }
    }

    private suspend inline fun publishing(w: W, steps: () -> Unit) {
        try {
            steps()
            flush()
            val next = w.root
            if (!next.drawsAs(root)) show(next)
        } catch (_: Restart) {
            heard += w.played
            flush()
            rebuild()
        }
    }

    private fun flush() {
        if (heard.isEmpty()) return
        play(heard.toList())
        heard.clear()
    }

    private suspend fun rebuild() {
        val w = world()
        live = w
        show(w.root)
        last = null // FrameLoopTest: no burst after restart
    }

    private fun show(next: GroupNode) {
        root = next
        if (GAME_DEBUG) history.push(next)
    }
}

private fun Input.pressesAnything(): Boolean = pointers.any { it.wasPressed } || keys.pressed.isNotEmpty()

private fun GroupNode.drawsAs(previous: GroupNode): Boolean =
    bounds == previous.bounds &&
        children.size == previous.children.size &&
        children.indices.all { children[it] === previous.children[it] }
