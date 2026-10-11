package fromcrackpot.kintsugi.gameengine

import androidx.compose.foundation.focusable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.toSize
import fromcrackpot.kintsugi.compose.Kanvas
import fromcrackpot.kintsugi.compose.canvasToCga
import fromcrackpot.kintsugi.compose.fitScale
import fromcrackpot.kintsugi.scenegraph.nodes.GroupNode

/**
 * Runs a game: [world] builds the first [World], then every display frame the loop advances it
 * step by step through [World.update] and draws [World.root] on screen.
 *
 * The loop is semi-fixed-timestep. Each display frame's wall-clock seconds — clamped to
 * [maxFrameTime] — are spent as whole `1/tickRate` steps plus one remainder step of whatever is
 * left; nothing carries over between frames and nothing is discarded, so the scene drawn is the
 * exact wall-clock-time state: rendering needs no interpolation, and [World.root] stays a pure
 * projection. [World.update] is the only place state advances, by a variable `dt` in
 * `[0, 1/tickRate]` seconds — the base step for every whole step of a frame, the frame's
 * remainder once per frame. A step of `dt = 0` occurs when a frame lands exactly on a step
 * boundary and must be harmless. Because steps vary in size, game logic must be a function of
 * `dt` — a timer counts seconds, decay is a per-second rate — never counts of steps.
 *
 * Each step receives sampled [Input], never events: pointers in CGA model units — converted
 * against the current scene's bounds — and the keyboard on Compose `Key` codes. Press and
 * release edges are latched so none is lost between frames and none fires twice within one
 * frame's burst of steps; the latching contract lives on [Input]. The composable requests focus
 * on launch, so key input arrives without any game-side focus code.
 *
 * [world] runs before the first step — the single creation site for every named node the state
 * and scene share, [AssetScope] loads and programmatic shapes alike — and again whenever a step
 * ends in [restart]: the world that called it is discarded, the rest of its frame with it, and
 * [world] builds the replacement, which draws from the next frame on and whose clock starts
 * there — the seconds the rebuild took are never simulated as catch-up. Input outlives a world:
 * a key held through a restart is still down in the replacement, with no fresh press edge. The
 * world [world] returns is fully formed: no step runs and no scene is read before it exists, so
 * games carry no nullable-asset tolerance; until the first returns, nothing draws, and until a
 * replacement returns, the discarded world's last frame stays on screen. The scene renders
 * through [Kanvas]: uniformly fitted to the surface, clipped to its bounds, letterboxed on
 * aspect mismatch. An exception thrown by [world] or [World.update] propagates unchanged —
 * [Restart] is the one the loop consumes.
 *
 * A build made with the `gameDebug` Gradle property carries a pause/step facility owned by the
 * loop: `P` toggles pause — no step runs while paused, the scene keeps drawing — and `O`, while
 * paused, advances exactly one `1/tickRate` step. Unpausing resumes from the current wall
 * clock; paused time is never simulated. Without the property the facility does not exist in
 * the binary and both keys reach the game untouched.
 *
 * @param W The game's world type.
 * @param modifier The host's modifier chain; sizing it imposes overrides the scene's intrinsic
 *   size.
 * @param tickRate The base simulation frequency in Hz; 120 by default. Its step `1/tickRate` is
 *   the largest `dt` [World.update] ever receives — the bound to size fast movers against.
 * @param maxFrameTime The most wall-clock seconds one frame may simulate; 0.25 by default. A
 *   longer stall — a debugger pause, a hidden tab — loses the excess time instead of spiraling
 *   into a catch-up storm.
 * @param world Builds a world — the first at launch, a replacement on each [restart]; suspends
 *   freely — asset loading awaits inside.
 * @throws IllegalStateException when a scene holds a shape that is neither a CGA primitive nor
 *   a composite — the unsealed `Shape` marker makes a foreign implementation constructible.
 */
@Composable
public fun <W : World> Game(
    modifier: Modifier = Modifier,
    tickRate: Double = 120.0,
    maxFrameTime: Double = 0.25,
    world: suspend () -> W,
) {
    var current by remember { mutableStateOf<W?>(null) }
    // The redraw driver: bumped once per display frame after the steps run, so each frame
    // recomposes and re-reads the scene projection — the world itself is not observable state.
    var frame by remember { mutableStateOf(0L) }
    val latch = remember { InputLatch() }
    val focus = remember { FocusRequester() }

    val root = remember(frame, current) { current?.root ?: GroupNode() }
    // Read inside the pointer handler without restarting it when the scene's bounds change.
    val bounds = rememberUpdatedState(root.bounds)

    LaunchedEffect(Unit) {
        var w = world()
        current = w

        val stepSize = 1.0 / tickRate
        var last = withFrameNanos { it }
        var paused = false

        suspend fun rebuild() {
            w = world()
            current = w
            // Not redundant with the loop's own anchor: without it the rebuild's seconds burst
            // into a catch-up.
            last = withFrameNanos { it }
        }

        while (true) {
            val now = withFrameNanos { it }
            var acc = ((now - last) / 1e9).coerceAtMost(maxFrameTime)
            last = now
            val frameInput = latch.consumeEdges()
            if (GAME_DEBUG) {
                if (frameInput.keys.wasPressed(Key.P)) paused = !paused
                if (paused) {
                    // `last` already advanced above — paused wall time is never simulated, so
                    // unpausing cannot burst into a catch-up.
                    if (frameInput.keys.wasPressed(Key.O)) {
                        try {
                            context(Step(stepSize, frameInput)) { w.update() }
                        } catch (_: Restart) {
                            rebuild()
                        }
                    }
                    frame++
                    continue
                }
            }
            try {
                var first = true
                while (acc >= stepSize) {
                    val input = if (first) frameInput else latch.level()
                    first = false
                    context(Step(stepSize, input)) { w.update() }
                    acc -= stepSize
                }
                context(Step(acc, if (first) frameInput else latch.level())) { w.update() }
            } catch (_: Restart) {
                rebuild()
            }
            frame++
        }
    }

    LaunchedEffect(Unit) { focus.requestFocus() }

    Kanvas(
        root,
        modifier
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val scale = fitScale(size.toSize(), bounds.value, density)
                        for (change in event.changes) {
                            val position = canvasToCga(change.position, size.toSize(), scale)
                            val id = change.id.value
                            when {
                                change.changedToDownIgnoreConsumed() -> latch.press(id, position)
                                change.changedToUpIgnoreConsumed() -> latch.release(
                                    id, position, ends = change.type != PointerType.Mouse,
                                )

                                else -> latch.move(id, position)
                            }
                        }
                    }
                }
            }
            .focusRequester(focus)
            .focusable()
            .onKeyEvent { event ->
                when (event.type) {
                    KeyEventType.KeyDown -> latch.keyDown(event.key)
                    KeyEventType.KeyUp -> latch.keyUp(event.key)
                }
                true
            },
    )
}
