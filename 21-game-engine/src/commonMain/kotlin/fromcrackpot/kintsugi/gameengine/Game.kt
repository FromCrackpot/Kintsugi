package fromcrackpot.kintsugi.gameengine

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.compose.Scene
import fromcrackpot.kintsugi.compose.canvasToCga
import fromcrackpot.kintsugi.compose.fitScale
import fromcrackpot.kintsugi.scenegraph.SceneBounds
import fromcrackpot.kintsugi.scenegraph.nodes.GroupNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt

/**
 * Runs a game: [world] builds the first [World], then every display frame the loop advances it
 * step by step through [World.step], shows [World.root], redrawing only the children that changed,
 * and plays the sounds its steps [played][World.played].
 *
 * - Steps vary in length up to `1/tickRate`, zero included. Write game logic as a function of `dt`.
 * - A step ending in [restart] discards the world, and [world] builds a replacement. Its sounds still play.
 * - Built with the `gameDebug` Gradle property: Home pauses, Page Up/Down walk frames, End toggles an FPS counter.
 *
 * @param W The game's world type.
 * @param modifier The host's modifier chain for the game's surface. Any size it imposes overrides the
 *   scene's intrinsic size.
 * @param alignment Where the fitted scene sits on the surface when their aspects differ, centred by
 *   default. Pointers are read over the whole surface, letterbox included.
 * @param tickRate The base simulation frequency in Hz, 120 by default. `1/tickRate` is the
 *   largest `dt` a step receives.
 * @param maxFrameTime The most wall-clock seconds one frame may simulate, 0.25 by default. A
 *   longer stall loses the excess.
 * @param preferredFrameRate The display rate in Hz to ask for while the game is shown, `null` by default, leaving it
 *   to the platform. Android takes its lowest display mode at or above it, else its highest; desktop and web ignore
 *   it. It never paces the loop.
 * @param onFirstFrame Called once, on the frame after the first world's scene was drawn. A restart doesn't
 *   call it again.
 * @param onFailure Called with a throw from [world] or from a step, before it propagates and stops the game.
 *   Cancellation is not a failure.
 * @param paused Drawn over the scene, letterbox excluded, while the game is paused for losing focus or
 *   going to the background. It stays until the next press (pointer or key), which also plays.
 *   Nothing by default.
 * @param world Builds a world: the first at launch, and a replacement on each [restart]. It runs on a
 *   background thread where the platform has one, while the previous picture stays up. It may suspend
 *   freely, so asset loading awaits inside.
 * @throws IllegalArgumentException when [preferredFrameRate] is not positive.
 * @throws IllegalStateException when a scene holds a shape that is neither a CGA primitive nor
 *   a composite.
 */
@Composable
public fun <W : World> Game(
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    tickRate: Double = 120.0,
    maxFrameTime: Double = 0.25,
    preferredFrameRate: Double? = null,
    onFirstFrame: () -> Unit = {},
    onFailure: (Throwable) -> Unit = {},
    paused: @Composable BoxScope.() -> Unit = {},
    world: suspend () -> W,
) {
    require(preferredFrameRate == null || preferredFrameRate > 0.0) {
        "preferredFrameRate must be positive, was $preferredFrameRate"
    }

    PreferFrameRate(preferredFrameRate)

    val speaker = remember { Speaker() }
    DisposableEffect(speaker) { onDispose { speaker.close() } }
    val loop = remember {
        FrameLoop({ withContext(Dispatchers.Default) { world() } }, tickRate, maxFrameTime, speaker::play)
    }
    val latch = loop.latch
    // The world isn't observable state: the loop publishes its projection after each frame's steps.
    var root by remember { mutableStateOf(loop.root, referentialEqualityPolicy()) }
    var fpsLabel by remember { mutableStateOf<String?>(null) }
    var interrupted by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val firstDraw = remember { FirstDraw(loop.root) }

    // Read inside the pointer handler without restarting it when the bounds or the placement change.
    val bounds = rememberUpdatedState(root.bounds)
    val placement = rememberUpdatedState(alignment)
    val direction = rememberUpdatedState(LocalLayoutDirection.current)
    val firstFrame = rememberUpdatedState(onFirstFrame)
    val failure = rememberUpdatedState(onFailure)

    LaunchedEffect(Unit) {
        try {
            var reported = false
            while (true) {
                val now = withFrameNanos { it }
                if (firstDraw.done && !reported) {
                    reported = true
                    firstFrame.value()
                }
                loop.frame(now)
                root = loop.root
                fpsLabel = loop.fpsLabel
                interrupted = loop.interrupted
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            failure.value(e)
            throw e
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        loop.interrupt()
        interrupted = true
    }

    LaunchedEffect(Unit) { focus.requestFocus() }

    val overlay = if (GAME_DEBUG) Modifier.debugOverlay { fpsLabel } else Modifier

    // Scene's own intrinsic size: the fitted box measures it fixed, so it can't size the surface.
    val intrinsic = root.bounds?.let {
        Modifier.defaultMinSize(it.width.toFloat().dp, it.height.toFloat().dp)
    } ?: Modifier.defaultMinSize(400.dp, 300.dp)

    Box(
        modifier
            .then(intrinsic)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        for (change in event.changes) {
                            val position = surfaceToCga(
                                change.position, size, bounds.value, density, placement.value, direction.value,
                            )
                            val id = change.id.value
                            when {
                                change.changedToDownIgnoreConsumed() -> latch.press(id, position, change.type)
                                change.changedToUpIgnoreConsumed() -> latch.release(id, position, change.type)
                                else -> latch.move(id, position, change.type)
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
            }
            .then(overlay),
    ) {
        Box(Modifier.matchParentSize().fitted(root.bounds, alignment)) {
            Scene(
                root,
                Modifier.matchParentSize().drawWithContent {
                    drawContent()
                    if (root !== firstDraw.blank) firstDraw.done = true
                },
            )
            if (interrupted) Box(Modifier.matchParentSize(), content = paused)
        }
    }
}

// Plain fields, not state. The draw phase writes them, and a state write there would recompose.
private class FirstDraw(val blank: GroupNode) {
    var done = false
}

private fun Modifier.fitted(bounds: SceneBounds?, alignment: Alignment): Modifier = layout { measurable, constraints ->
    val surface = IntSize(constraints.maxWidth, constraints.maxHeight)
    val rect = sceneRect(surface, bounds, density, alignment, layoutDirection)
    val placeable = measurable.measure(Constraints.fixed(rect.width, rect.height))
    layout(surface.width, surface.height) { placeable.place(rect.topLeft) }
}

// The one placement both the fitted box and the pointer mapping read, so draw and touch can't drift.
internal fun sceneRect(
    surface: IntSize,
    bounds: SceneBounds?,
    density: Float,
    alignment: Alignment,
    direction: LayoutDirection,
): IntRect {
    if (bounds == null) return IntRect(IntOffset.Zero, surface)
    val scale = fitScale(surface.toSize(), bounds, density)
    val size = IntSize(
        (bounds.width * scale).roundToInt().coerceAtMost(surface.width),
        (bounds.height * scale).roundToInt().coerceAtMost(surface.height),
    )
    return IntRect(alignment.align(size, surface, direction), size)
}

internal fun surfaceToCga(
    position: Offset,
    surface: IntSize,
    bounds: SceneBounds?,
    density: Float,
    alignment: Alignment,
    direction: LayoutDirection,
): FlatPoint {
    val rect = sceneRect(surface, bounds, density, alignment, direction)
    val size = rect.size.toSize()
    return canvasToCga(position - rect.topLeft.toOffset(), size, fitScale(size, bounds, density))
}
