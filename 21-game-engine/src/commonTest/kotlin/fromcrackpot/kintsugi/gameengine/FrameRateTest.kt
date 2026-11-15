package fromcrackpot.kintsugi.gameengine

import kotlin.test.Test
import kotlin.test.assertEquals

class FrameRateTest {

    /* PICK RATE */

    @Test
    fun `an offered rate is picked exactly`() {
        assertEquals(1, pickRate(listOf(60.0, 90.0, 120.0), 90.0))
    }

    @Test
    fun `between two rates the higher one is picked`() {
        assertEquals(1, pickRate(listOf(60.0, 120.0), 90.0))
    }

    @Test
    fun `below every rate the lowest one is picked`() {
        assertEquals(0, pickRate(listOf(60.0, 120.0), 24.0))
    }

    @Test
    fun `above every rate the highest one is picked`() {
        assertEquals(2, pickRate(listOf(60.0, 90.0, 120.0), 144.0))
    }

    @Test
    fun `a nominal rate reported slightly off still counts as reached`() {
        assertEquals(1, pickRate(listOf(90.0, 120.00001), 120.0), "reported above")
        assertEquals(0, pickRate(listOf(59.94, 120.0), 60.0), "reported below")
    }

    @Test
    fun `the order of the offered rates does not matter`() {
        assertEquals(2, pickRate(listOf(120.0, 24.0, 60.0, 90.0, 30.0), 60.0))
        assertEquals(0, pickRate(listOf(120.0, 24.0, 60.0, 90.0, 30.0), 200.0))
    }
}
