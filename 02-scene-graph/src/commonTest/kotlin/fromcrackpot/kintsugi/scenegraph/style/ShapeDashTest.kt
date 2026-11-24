package fromcrackpot.kintsugi.scenegraph.style

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ShapeDashTest {

    /* CTOR */

    @Test fun `even-length intervals are kept as given`() {
        val dash = ShapeDash(listOf(4.0, 2.0, 1.0, 2.0), offset = 1.5)
        assertEquals(listOf(4.0, 2.0, 1.0, 2.0), dash.intervals)
        assertEquals(1.5, dash.offset)
        assertFalse(dash.isSolid)
    }

    @Test fun `odd-length intervals repeat once to even length`() {
        assertEquals(listOf(2.0, 2.0), ShapeDash(listOf(2.0)).intervals)
        assertEquals(listOf(1.0, 2.0, 3.0, 1.0, 2.0, 3.0), ShapeDash(listOf(1.0, 2.0, 3.0)).intervals)
    }

    @Test fun `empty and all-zero intervals are solid with the offset dropped`() {
        assertTrue(ShapeDash(emptyList(), offset = 3.0).isSolid)
        assertEquals(0.0, ShapeDash(emptyList(), offset = 3.0).offset)
        assertEquals(ShapeDash.SOLID, ShapeDash(listOf(0.0, 0.0), offset = 3.0))
        assertTrue(ShapeDash.SOLID.isSolid)
    }

    @Test fun `zero runs inside a non-zero pattern are kept`() {
        assertEquals(listOf(0.0, 2.0), ShapeDash(listOf(0.0, 2.0)).intervals)
    }

    @Test fun `negative offset is legal and signed zero collapses`() {
        assertEquals(-1.0, ShapeDash(listOf(2.0, 1.0), offset = -1.0).offset)
        assertEquals(0.0.toBits(), ShapeDash(listOf(2.0, 1.0), offset = -0.0).offset.toBits())
    }

    @Test fun `negative or non-finite runs and a non-finite offset throw`() {
        assertFailsWith<IllegalArgumentException> { ShapeDash(listOf(2.0, -1.0)) }
        assertFailsWith<IllegalArgumentException> { ShapeDash(listOf(Double.NaN)) }
        assertFailsWith<IllegalArgumentException> { ShapeDash(listOf(Double.POSITIVE_INFINITY, 1.0)) }
        assertFailsWith<IllegalArgumentException> { ShapeDash(listOf(2.0, 1.0), offset = Double.NaN) }
    }

    @Test fun `the intervals list is a defensive copy`() {
        val source = mutableListOf(4.0, 2.0)
        val dash = ShapeDash(source)
        source[0] = 9.0
        assertEquals(listOf(4.0, 2.0), dash.intervals)
    }

    /* IDENTITY */

    @Test fun `patterns that draw the same compare equal`() {
        assertEquals(ShapeDash(listOf(2.0)), ShapeDash(listOf(2.0, 2.0)))
        assertEquals(ShapeDash(listOf(2.0)).hashCode(), ShapeDash(listOf(2.0, 2.0)).hashCode())
    }

    @Test fun `runs and offset both take part in equality`() {
        assertNotEquals(ShapeDash(listOf(4.0, 2.0)), ShapeDash(listOf(2.0, 4.0)))
        assertNotEquals(ShapeDash(listOf(4.0, 2.0)), ShapeDash(listOf(4.0, 2.0), offset = 1.0))
    }
}
