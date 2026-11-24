package fromcrackpot.kintsugi.gameengine

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.pointer.PointerType
import fromcrackpot.kintsugi.audio.Pcm
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.scenegraph.SceneBounds
import fromcrackpot.kintsugi.scenegraph.nodes.GroupNode
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

private val LOOP_BOUNDS = SceneBounds(-100.0, -100.0, 200.0, 200.0)
private const val STEP = 1.0 / 120
private val MOVE = Key.DirectionRight
private val RESTART = Key.R
private val BEEP_KEY = Key.B
private val BEEP = Sound(Pcm(floatArrayOf(0.25f), 44_100))
private val FANFARE = Sound(Pcm(floatArrayOf(0.5f), 44_100))

private class LoopProbe : World(LOOP_BOUNDS) {
    val steps = mutableListOf<Step>()
    var marker by staged(GroupNode())

    context(step: Step)
    override fun update() {
        steps += step
        if (step.input.keys.isDown(MOVE)) marker = GroupNode()
        if (step.input.keys.isDown(BEEP_KEY)) BEEP.play()
        if (step.input.keys.wasPressed(RESTART)) {
            FANFARE.play()
            restart()
        }
    }
}

class FrameLoopTest {

    private val worlds = mutableListOf<LoopProbe>()
    private val flushes = mutableListOf<List<Sound>>()
    private val worldsAtFlush = mutableListOf<Int>()
    private fun newLoop(interruptible: Boolean = true) = FrameLoop(
        { LoopProbe().also { worlds += it } },
        tickRate = 120.0,
        maxFrameTime = 0.25,
        play = { flushes += it.toList(); worldsAtFlush += worlds.size },
        interruptible = interruptible,
    )
    private var loop = newLoop()
    private val world: LoopProbe get() = worlds.last()
    private val seconds: Double get() = world.steps.sumOf { it.dt }

    private fun at(millis: Long) {
        var outcome: Result<Unit>? = null
        suspend { loop.frame(millis * 1_000_000) }.startCoroutine(Continuation(EmptyCoroutineContext) { outcome = it })
        checkNotNull(outcome) { "the frame suspended" }.getOrThrow()
    }

    private fun tap(key: Key) {
        loop.latch.keyDown(key)
        loop.latch.keyUp(key)
    }

    /** Builds the world at 0 ms and anchors at 10 ms: the next frame steps. */
    private fun launch() {
        at(0)
        at(10)
    }

    /* LAUNCH */

    @Test
    fun `the first frame builds the world and publishes its root`() {
        at(0)
        assertEquals(1, worlds.size)
        assertSame(world.marker, loop.root.children.single())
        assertEquals(LOOP_BOUNDS, loop.root.bounds)
        assertTrue(world.steps.isEmpty(), "the building frame steps nothing")
    }

    @Test
    fun `the frame after the build only anchors`() {
        at(0)
        at(1_000)
        assertTrue(world.steps.isEmpty(), "the anchoring frame steps nothing")
        at(1_010)
        assertEquals(0.01, seconds, 1e-12, "the first steps spend the interval since the anchor")
    }

    /* STEP PLAN */

    @Test
    fun `a frame spends whole steps then one remainder`() {
        launch()
        at(30)
        assertEquals(listOf(STEP, STEP), world.steps.take(2).map { it.dt })
        assertEquals(3, world.steps.size)
        assertEquals(0.02 - 2 * STEP, world.steps[2].dt, 1e-12)
    }

    @Test
    fun `a stall simulates at most maxFrameTime`() {
        launch()
        at(5_000)
        assertEquals(0.25, seconds, 1e-9)
        assertTrue(world.steps.all { it.dt <= STEP })
    }

    /* EDGES */

    @Test
    fun `a press during a stall reaches the first step only`() {
        launch()
        loop.latch.keyDown(MOVE)
        at(110)
        val steps = world.steps
        assertTrue(steps.size > 1)
        assertTrue(steps.first().input.keys.wasPressed(MOVE))
        assertTrue(steps.drop(1).none { it.input.keys.wasPressed(MOVE) }, "later steps see level state only")
        assertTrue(steps.all { it.input.keys.isDown(MOVE) })
    }

    /* RESTART */

    @Test
    fun `a restart rebuilds within the frame and the next frame only anchors`() {
        launch()
        tap(RESTART)
        at(30)
        assertEquals(2, worlds.size, "the factory reruns inside the restarting frame")
        assertEquals(1, worlds[0].steps.size, "the throw ends the frame's steps")
        assertSame(world.marker, loop.root.children.single(), "the replacement's root is published at once")
        at(2_000)
        assertTrue(world.steps.isEmpty(), "the frame after the rebuild only anchors")
        at(2_010)
        assertEquals(0.01, seconds, 1e-12, "the rebuild's seconds are never simulated")
    }

    /* PUBLISH */

    @Test
    fun `a world at rest keeps the published root`() {
        launch()
        val atRest = loop.root
        at(30)
        assertSame(atRest, loop.root)
        loop.latch.keyDown(MOVE)
        at(50)
        assertNotSame(atRest, loop.root)
        assertSame(world.marker, loop.root.children.single())
    }

    /* SOUND */

    @Test
    fun `a frame plays its steps' distinct sounds once`() {
        launch()
        at(30)
        assertTrue(flushes.isEmpty(), "a silent frame plays nothing")
        loop.latch.keyDown(BEEP_KEY)
        at(50)
        assertEquals(6, world.steps.size, "the second frame's three steps each beeped")
        assertEquals(listOf(listOf(BEEP)), flushes)
    }

    @Test
    fun `a restart's sounds play before the replacement builds`() {
        launch()
        loop.latch.keyDown(BEEP_KEY)
        tap(RESTART)
        at(30)
        assertEquals(listOf(listOf(BEEP, FANFARE)), flushes)
        assertEquals(listOf(1), worldsAtFlush)
    }

    @Test
    fun `an interrupted frame plays nothing`() {
        launch()
        loop.latch.keyDown(BEEP_KEY)
        at(30)
        loop.interrupt()
        at(50)
        at(70)
        assertEquals(1, flushes.size)
    }

    /* INTERRUPTION */

    @Test
    fun `an interruption pauses until a press - which plays and never catches up`() {
        launch()
        loop.interrupt()
        at(30)
        at(5_000)
        assertTrue(world.steps.isEmpty(), "nothing steps while interrupted")
        assertTrue(loop.interrupted)
        loop.latch.press(1L, FlatPoint(0.0, 0.0), PointerType.Touch)
        at(5_010)
        assertFalse(loop.interrupted)
        assertEquals(0.01, seconds, 1e-12, "the interrupted seconds are never simulated")
        assertTrue(world.steps.first().input.pointer!!.wasPressed, "the resuming press reaches the game")
    }

    @Test
    fun `a key press resumes too`() {
        launch()
        loop.interrupt()
        at(30)
        tap(Key.Spacebar)
        at(40)
        assertEquals(0.01, seconds, 1e-12)
    }

    @Test
    fun `an interruption during the load still builds the world - then waits`() {
        loop.interrupt()
        at(0)
        at(10)
        at(20)
        assertEquals(1, worlds.size)
        assertTrue(world.steps.isEmpty())
    }

    @Test
    fun `a loop that is not interruptible steps straight through an interruption`() {
        loop = newLoop(interruptible = false)
        launch()
        loop.interrupt()
        assertFalse(loop.interrupted)
        at(30)
        assertEquals(0.02, seconds, 1e-12, "no press needed")
    }

    /* DEBUG KEYS: run with -PgameDebug, skipped without */

    @Test
    fun `a resuming press never ends a debug pause`() {
        if (!GAME_DEBUG) return
        launch()
        tap(Key.MoveHome)
        at(30)
        loop.interrupt()
        loop.latch.press(1L, FlatPoint(0.0, 0.0), PointerType.Touch)
        at(40)
        assertFalse(loop.interrupted)
        assertTrue(world.steps.isEmpty(), "still paused by the debug keys")
        tap(Key.MoveHome)
        at(50)
        assertEquals(0.01, seconds, 1e-12)
    }

    @Test
    fun `Home pauses and unpausing never catches up`() {
        if (!GAME_DEBUG) return
        launch()
        tap(Key.MoveHome)
        at(30)
        at(5_000)
        assertTrue(world.steps.isEmpty(), "nothing steps while paused")
        tap(Key.MoveHome)
        at(5_010)
        assertEquals(0.01, seconds, 1e-12, "the paused seconds are never simulated")
    }

    @Test
    fun `Page Down at the newest frame steps once`() {
        if (!GAME_DEBUG) return
        launch()
        tap(Key.MoveHome)
        at(30)
        tap(Key.PageDown)
        at(1_000)
        assertEquals(listOf(STEP), world.steps.map { it.dt })
        assertTrue(world.steps.single().input.keys.wasPressed(Key.PageDown), "the step reads the frame's edges")
    }

    @Test
    fun `Page Up shows the previous frame and Page Down walks back without stepping`() {
        if (!GAME_DEBUG) return
        launch()
        loop.latch.keyDown(MOVE)
        at(30)
        val previous = loop.root
        at(50)
        val newest = loop.root
        loop.latch.keyUp(MOVE)
        tap(Key.PageUp)
        at(70)
        assertSame(previous, loop.root)
        val stepped = world.steps.size
        tap(Key.PageDown)
        at(90)
        assertSame(newest, loop.root)
        assertEquals(stepped, world.steps.size, "walking the history steps nothing")
    }

    @Test
    fun `a debug step plays its sounds and a history walk plays none`() {
        if (!GAME_DEBUG) return
        launch()
        loop.latch.keyDown(MOVE)
        loop.latch.keyDown(BEEP_KEY)
        at(30)
        tap(Key.MoveHome)
        at(50)
        assertEquals(1, flushes.size, "the pause plays nothing")
        tap(Key.PageDown)
        at(70)
        assertEquals(2, flushes.size, "the debug step beeps")
        tap(Key.PageUp)
        at(90)
        tap(Key.PageDown)
        at(110)
        assertEquals(2, flushes.size, "walking the history plays nothing")
    }

    @Test
    fun `End toggles the FPS label`() {
        if (!GAME_DEBUG) return
        launch()
        tap(Key.MoveEnd)
        at(30)
        assertNull(loop.fpsLabel, "no label before the first full window")
        at(530)
        assertNotNull(loop.fpsLabel)
        tap(Key.MoveEnd)
        at(540)
        assertNull(loop.fpsLabel)
    }

    @Test
    fun `a restart during a debug step rebuilds and stays paused`() {
        if (!GAME_DEBUG) return
        launch()
        tap(Key.MoveHome)
        at(30)
        tap(Key.PageDown)
        tap(RESTART)
        at(50)
        assertEquals(2, worlds.size)
        at(2_000)
        assertTrue(world.steps.isEmpty(), "the frame after the rebuild only anchors")
        tap(Key.PageDown)
        at(2_010)
        assertEquals(listOf(STEP), world.steps.map { it.dt })
    }
}
