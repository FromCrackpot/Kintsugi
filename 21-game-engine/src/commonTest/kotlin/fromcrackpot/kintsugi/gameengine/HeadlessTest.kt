package fromcrackpot.kintsugi.gameengine

import androidx.compose.ui.input.key.Key
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.scenegraph.SceneBounds
import fromcrackpot.kintsugi.scenegraph.nodes.GroupNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

private val RIGHT = Key.DirectionRight
private val LEFT = Key.DirectionLeft

private class ProbeWorld(private val restartAt: Int = 0) : World(SceneBounds(-100.0, -100.0, 200.0, 200.0)) {
    val inputs = mutableListOf<Input>()
    val dts = mutableListOf<Double>()
    var x = 0.0

    context(step: Step)
    override fun update() {
        inputs += step.input
        dts += step.dt
        if (step.input.keys.isDown(RIGHT)) x += step.dt
        if (dts.size == restartAt) restart()
    }

    override val scene: GroupNode get() = GroupNode()
}

class HeadlessTest {

    /* DRIVER */

    @Test
    fun `session runs seconds times tickRate steps of the fixed quantum`() {
        val world = drive({ ProbeWorld() }, seconds = 1.0, tickRate = 10)
        assertEquals(10, world.dts.size, "1 s at 10 Hz is 10 steps")
        assertTrue(world.dts.all { it == 0.1 }, "every step is the 1/tickRate quantum")
    }

    @Test
    fun `controls sees start-of-step elapsed and onStep end-of-step elapsed`() {
        val policyTimes = mutableListOf<Double>()
        val observedTimes = mutableListOf<Double>()
        drive(
            { ProbeWorld() }, seconds = 0.3, tickRate = 10,
            onStep = { elapsed, _ -> observedTimes += elapsed },
            controls = { elapsed, _ -> policyTimes += elapsed; Controls.NONE },
        )
        assertEquals(listOf(0.0, 0.1, 0.2), policyTimes, "policy runs before each step")
        assertEquals(listOf(0.1, 0.2, 0.30000000000000004), observedTimes, "observer runs after each step")
    }

    @Test
    fun `drive returns the world it built`() {
        val world = ProbeWorld()
        assertSame(world, drive({ world }, seconds = 0.1, tickRate = 10), "the built world is the returned world")
    }

    /* KEY EDGES */

    @Test
    fun `a held key presses on its first step only and stays down`() {
        val world = drive({ ProbeWorld() }, seconds = 0.4, tickRate = 10) { _, _ -> Controls(keys = setOf(RIGHT)) }
        assertTrue(world.inputs[0].keys.wasPressed(RIGHT), "held from t=0 edges on step 0")
        assertTrue(world.inputs.drop(1).none { it.keys.wasPressed(RIGHT) }, "no repeat edges while held")
        assertTrue(world.inputs.all { it.keys.isDown(RIGHT) }, "level state persists")
    }

    @Test
    fun `a dropped key releases on exactly the step it leaves the set`() {
        val world = drive({ ProbeWorld() }, seconds = 0.4, tickRate = 10) { elapsed, _ ->
            if (elapsed < 0.2) Controls(keys = setOf(RIGHT)) else Controls.NONE
        }
        assertEquals(
            listOf(false, false, true, false),
            world.inputs.map { it.keys.wasReleased(RIGHT) },
            "one release edge, on the first key-less step",
        )
        assertEquals(
            listOf(true, true, false, false),
            world.inputs.map { it.keys.isDown(RIGHT) },
            "level state drops with the set",
        )
    }

    @Test
    fun `swapping keys edges both in the same step`() {
        val world = drive({ ProbeWorld() }, seconds = 0.2, tickRate = 10) { elapsed, _ ->
            Controls(keys = if (elapsed < 0.1) setOf(RIGHT) else setOf(LEFT))
        }
        val swap = world.inputs[1].keys
        assertTrue(swap.wasPressed(LEFT), "the entering key presses")
        assertTrue(swap.wasReleased(RIGHT), "the leaving key releases")
    }

    /* POINTER EDGES */

    @Test
    fun `pointer down and up transitions edge exactly once`() {
        val at = FlatPoint(5.0, 5.0)
        val world = drive({ ProbeWorld() }, seconds = 0.4, tickRate = 10) { elapsed, _ ->
            Controls(pointer = at, pointerDown = elapsed in 0.1..0.25)
        }
        assertEquals(
            listOf(false, true, false, false),
            world.inputs.map { it.pointer!!.wasPressed },
            "one press edge, on the down transition",
        )
        assertEquals(
            listOf(false, false, false, true),
            world.inputs.map { it.pointer!!.wasReleased },
            "one release edge, on the up transition",
        )
        assertEquals(
            listOf(false, true, true, false),
            world.inputs.map { it.pointer!!.isDown },
            "level state spans the hold",
        )
    }

    @Test
    fun `a pointer vanishing while down releases at its last position`() {
        val at = FlatPoint(3.0, -4.0)
        val world = drive({ ProbeWorld() }, seconds = 0.2, tickRate = 10) { elapsed, _ ->
            if (elapsed < 0.1) Controls(pointer = at, pointerDown = true) else Controls.NONE
        }
        val ghost = world.inputs[1].pointer!!
        assertTrue(ghost.wasReleased, "the vanish yields the release edge")
        assertFalse(ghost.isDown, "the ghost pointer is up")
        assertEquals(at, ghost.position, "the release lands at the last position")
    }

    @Test
    fun `a pointer vanishing while up just disappears`() {
        val world = drive({ ProbeWorld() }, seconds = 0.2, tickRate = 10) { elapsed, _ ->
            if (elapsed < 0.1) Controls(pointer = FlatPoint(1.0, 1.0)) else Controls.NONE
        }
        assertNull(world.inputs[1].pointer, "an up pointer owes no release edge")
    }

    /* CLOSED LOOP */

    @Test
    fun `the policy reads the state the session mutates`() {
        val world = drive({ ProbeWorld() }, seconds = 1.0, tickRate = 10) { _, w ->
            if (w.x < 0.25) Controls(keys = setOf(RIGHT)) else Controls.NONE
        }
        assertEquals(0.30000000000000004, world.x, "the bot stops commanding once its goal is reached")
    }

    /* RESTART */

    @Test
    fun `a restart rebuilds the world from the factory and the session continues on it`() {
        val built = mutableListOf<ProbeWorld>()
        val seen = mutableListOf<ProbeWorld>()
        val world = drive(
            { ProbeWorld(restartAt = 3).also { built += it } },
            seconds = 0.5, tickRate = 10,
            onStep = { _, w -> seen += w },
        )
        assertEquals(2, built.size, "the factory ran at the start and once more on the restart")
        assertSame(built[1], world, "the returned world is the rebuilt one")
        assertEquals(3, built[0].dts.size, "the first world stepped through the restarting step")
        assertEquals(2, built[1].dts.size, "the rebuilt world took the remaining steps")
        assertEquals(
            listOf(built[0], built[0], built[1], built[1], built[1]),
            seen,
            "the observer sees the rebuilt world from the restarting step on",
        )
    }

    @Test
    fun `a key held through a restart stays down without a fresh press edge`() {
        val world = drive({ ProbeWorld(restartAt = 3) }, seconds = 0.5, tickRate = 10) { _, _ ->
            Controls(keys = setOf(RIGHT))
        }
        assertEquals(2, world.dts.size, "the rebuilt world took the steps after the restart")
        assertTrue(world.inputs.all { it.keys.isDown(RIGHT) }, "level state persists across the swap")
        assertTrue(world.inputs.none { it.keys.wasPressed(RIGHT) }, "the hold edged before the swap, never again")
    }

    /* FACTORIES */

    @Test
    fun `Input of builds the claimed snapshot`() {
        val input = Input.of(
            pointers = listOf(Pointer.of(FlatPoint(2.0, 3.0), isDown = true, wasPressed = true)),
            keysDown = setOf(RIGHT),
            keysPressed = setOf(RIGHT),
            keysReleased = setOf(LEFT),
        )
        assertTrue(input.keys.isDown(RIGHT), "fabricated level state reads back")
        assertTrue(input.keys.wasPressed(RIGHT), "fabricated press edge reads back")
        assertTrue(input.keys.wasReleased(LEFT), "fabricated release edge reads back")
        val pointer = input.pointer!!
        assertEquals(FlatPoint(2.0, 3.0), pointer.position, "fabricated position reads back")
        assertTrue(pointer.isDown && pointer.wasPressed && !pointer.wasReleased, "fabricated pointer facts read back")
    }
}
