package fromcrackpot.kintsugi.cga.d2.shapes.composites

import fromcrackpot.kintsugi.cga.d2.shapes.composites.RectSide.BOTTOM
import fromcrackpot.kintsugi.cga.d2.shapes.composites.RectSide.LEFT
import fromcrackpot.kintsugi.cga.d2.shapes.composites.RectSide.RIGHT
import fromcrackpot.kintsugi.cga.d2.shapes.composites.RectSide.TOP
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Direction
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Line
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.RoundPoint
import fromcrackpot.kintsugi.cga.d2.transforms.F
import fromcrackpot.kintsugi.cga.d2.transforms.R
import fromcrackpot.kintsugi.cga.d2.transforms.T
import fromcrackpot.kintsugi.cga.d2.transforms.composites.times
import fromcrackpot.kintsugi.cga.d2.exceptions.DegeneratePolygonException
import fromcrackpot.kintsugi.cga.deg
import kotlin.test.*

class PolygonTest {

    /* CTOR */

    @Test
    fun `fewer than 3 vertices throws`() {
        assertFailsWith<DegeneratePolygonException> {
            Polygon(listOf(FlatPoint(0.0, 0.0), FlatPoint(1.0, 0.0)))
        }
    }

    @Test
    fun `vertex at infinity throws`() {
        assertFailsWith<DegeneratePolygonException> {
            Polygon(listOf(FlatPoint(0.0, 0.0), Direction(1.0, 0.0), FlatPoint(0.0, 1.0)))
        }
    }

    @Test
    fun `coincident consecutive vertices throw`() {
        assertFailsWith<DegeneratePolygonException> {
            Polygon(listOf(FlatPoint(0.0, 0.0), FlatPoint(0.0, 0.0), FlatPoint(0.0, 1.0)))
        }
    }

    @Test
    fun `coincident last and first vertices throw - the loop closes implicitly`() {
        assertFailsWith<DegeneratePolygonException> {
            Polygon(listOf(FlatPoint(0.0, 0.0), FlatPoint(1.0, 0.0), FlatPoint(0.0, 0.0)))
        }
    }

    /* MATH */

    @Test
    fun `edges close the loop in vertex order`() {
        val triangle = Polygon(listOf(FlatPoint(0.0, 0.0), FlatPoint(4.0, 0.0), FlatPoint(0.0, 3.0)))
        assertEquals(3, triangle.edges.size)
        assertEquals(RoundPoint(0.0, 0.0), triangle.edges[0].start)
        assertEquals(RoundPoint(4.0, 0.0), triangle.edges[1].start)
        assertEquals(RoundPoint(0.0, 3.0), triangle.edges[2].start)
        assertEquals(RoundPoint(0.0, 0.0), triangle.edges[2].end)
    }

    @Test
    fun `adjacent edges share corner instances by reference`() {
        val triangle = Polygon(listOf(FlatPoint(0.0, 0.0), FlatPoint(4.0, 0.0), FlatPoint(0.0, 3.0)))
        assertSame(triangle.edges[0].end, triangle.edges[1].start)
        assertSame(triangle.edges[1].end, triangle.edges[2].start)
        assertSame(triangle.edges[2].end, triangle.edges[0].start) // wraps at the loop
    }

    @Test
    fun `signedArea is positive for CCW and negative for CW`() {
        val ccw = Polygon.Rectangle(FlatPoint(0.0, 0.0), FlatPoint(4.0, 3.0))
        assertEquals(12.0, ccw.signedArea, 1e-12)
        assertEquals(-12.0, Polygon(ccw.vertices.reversed()).signedArea, 1e-12)
    }

    @Test
    fun `centroid of a rectangle is its center`() {
        assertEquals(
            FlatPoint(2.0, 1.5),
            Polygon.Rectangle(FlatPoint(0.0, 0.0), FlatPoint(4.0, 3.0)).centroid,
        )
    }

    @Test
    fun `centroid of a triangle is the vertex mean`() {
        assertEquals(
            FlatPoint(1.0, 1.0),
            Polygon.Triangle(FlatPoint(0.0, 0.0), FlatPoint(3.0, 0.0), FlatPoint(0.0, 3.0)).centroid,
        )
    }

    @Test
    fun `centroid falls back to the vertex mean when the signed area cancels to zero`() {
        // Symmetric bowtie: the two lobes cancel algebraically.
        val bowtie = Polygon(
            listOf(FlatPoint(0.0, 0.0), FlatPoint(2.0, 2.0), FlatPoint(2.0, 0.0), FlatPoint(0.0, 2.0))
        )
        assertEquals(0.0, bowtie.signedArea, 1e-12)
        assertEquals(FlatPoint(1.0, 1.0), bowtie.centroid)
    }

    /* IDENTITY */

    @Test
    fun `equality is rounded and order-sensitive`() {
        val a = Polygon(listOf(FlatPoint(0.0, 0.0), FlatPoint(4.0, 0.0), FlatPoint(0.0, 3.0)))
        val b = Polygon(listOf(FlatPoint(0.0, 0.0), FlatPoint(4.0, 0.0), FlatPoint(0.0, 3.0)))
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        // Starting vertex and traversal direction are data — a rotated list is a different value.
        val shifted = Polygon(listOf(FlatPoint(4.0, 0.0), FlatPoint(0.0, 3.0), FlatPoint(0.0, 0.0)))
        assertNotEquals(a, shifted)
    }

    /* FACTORIES */

    @Test
    fun `Rectangle factory emits the CCW quad bl-br-tr-tl`() {
        // The factory-order invariant: every rectangle-producing path emits this exact order,
        // so consumers may rely on edges[0]=bottom, [1]=right, [2]=top, [3]=left.
        assertEquals(
            Polygon(
                listOf(
                    FlatPoint(1.0, 2.0), FlatPoint(5.0, 2.0),
                    FlatPoint(5.0, 5.0), FlatPoint(1.0, 5.0),
                )
            ),
            Polygon.Rectangle(FlatPoint(1.0, 2.0), FlatPoint(5.0, 5.0)),
        )
    }

    @Test
    fun `RectSide answers coincide with the factory-order edges on a fresh rectangle`() {
        val rect = Polygon.Rectangle(FlatPoint(1.0, 2.0), FlatPoint(5.0, 5.0))
        assertEquals(Segment(FlatPoint(1.0, 2.0), FlatPoint(5.0, 2.0)), rect.edges[BOTTOM])
        assertEquals(Segment(FlatPoint(5.0, 2.0), FlatPoint(5.0, 5.0)), rect.edges[RIGHT])
        assertEquals(Segment(FlatPoint(5.0, 5.0), FlatPoint(1.0, 5.0)), rect.edges[TOP])
        assertEquals(Segment(FlatPoint(1.0, 5.0), FlatPoint(1.0, 2.0)), rect.edges[LEFT])
    }

    @Test
    fun `RectSide follows the visual sides through negation`() {
        // The reversed walk shuffles the indices (side i lands at (2 − i) mod 4) and reverses
        // each edge — the visual lookup is unmoved.
        val wall = -Polygon.Rectangle(FlatPoint(1.0, 2.0), FlatPoint(5.0, 5.0))
        assertEquals(Segment(FlatPoint(5.0, 2.0), FlatPoint(1.0, 2.0)), wall.edges[BOTTOM])
        assertEquals(Segment(FlatPoint(5.0, 5.0), FlatPoint(5.0, 2.0)), wall.edges[RIGHT])
        assertEquals(Segment(FlatPoint(1.0, 5.0), FlatPoint(5.0, 5.0)), wall.edges[TOP])
        assertEquals(Segment(FlatPoint(1.0, 2.0), FlatPoint(1.0, 5.0)), wall.edges[LEFT])
    }

    @Test
    fun `RectSide follows the visual sides through a mirror`() {
        // Reflecting across x = 0 keeps the list order but swaps the visually left and right
        // sides — a case no index arithmetic can recover, since the mirror flips the winding
        // without reversing the list.
        val rect = Polygon.Rectangle(FlatPoint(1.0, 2.0), FlatPoint(5.0, 5.0))
        val mirrored = rect * F(Line(1.0, 0.0, 0.0))
        assertEquals(Segment(FlatPoint(-1.0, 2.0), FlatPoint(-5.0, 2.0)), mirrored.edges[BOTTOM])
        assertEquals(Segment(FlatPoint(-1.0, 5.0), FlatPoint(-1.0, 2.0)), mirrored.edges[RIGHT])
        assertEquals(Segment(FlatPoint(-5.0, 5.0), FlatPoint(-1.0, 5.0)), mirrored.edges[TOP])
        assertEquals(Segment(FlatPoint(-5.0, 2.0), FlatPoint(-5.0, 5.0)), mirrored.edges[LEFT])
    }

    @Test
    fun `RectSide is stable under a sub-45-degree tilt`() {
        val tilted = Polygon.Rectangle(FlatPoint(-2.0, -1.0), FlatPoint(2.0, 1.0)) *
                R(FlatPoint(0.0, 0.0), 30.deg)
        assertSame(tilted.edges[0], tilted.edges[BOTTOM])
        assertSame(tilted.edges[1], tilted.edges[RIGHT])
        assertSame(tilted.edges[2], tilted.edges[TOP])
        assertSame(tilted.edges[3], tilted.edges[LEFT])
    }

    @Test
    fun `Rectangle factory normalizes any opposite-corner pair`() {
        val bl = FlatPoint(1.0, 2.0)
        val br = FlatPoint(5.0, 2.0)
        val tr = FlatPoint(5.0, 5.0)
        val tl = FlatPoint(1.0, 5.0)
        val expected = Polygon.Rectangle(bl, tr)
        assertEquals(expected, Polygon.Rectangle(tr, bl))
        assertEquals(expected, Polygon.Rectangle(tl, br))
        assertEquals(expected, Polygon.Rectangle(br, tl))
    }

    @Test
    fun `Rectangle factory shares corner instances between adjacent edges`() {
        // Ported from the retired Rectangle class: adjacent edges share their corner
        // RoundPoint by reference (Segment's endpoint caching through the Polygon loop).
        val rect = Polygon.Rectangle(FlatPoint(1.0, 2.0), FlatPoint(5.0, 5.0))
        assertSame(rect.edges[0].end, rect.edges[1].start)
        assertSame(rect.edges[1].end, rect.edges[2].start)
        assertSame(rect.edges[2].end, rect.edges[3].start)
        assertSame(rect.edges[3].end, rect.edges[0].start) // wraps at the loop
    }

    @Test
    fun `Rectangle factory rejects zero extents`() {
        assertFailsWith<DegeneratePolygonException> {
            Polygon.Rectangle(FlatPoint(1.0, 2.0), FlatPoint(1.0, 5.0)) // shared x
        }
        assertFailsWith<DegeneratePolygonException> {
            Polygon.Rectangle(FlatPoint(1.0, 2.0), FlatPoint(5.0, 2.0)) // shared y
        }
    }

    @Test
    fun `Rectangle factory rejects a corner at infinity`() {
        assertFailsWith<DegeneratePolygonException> {
            Polygon.Rectangle(FlatPoint(1.0, 2.0), Direction(1.0, 1.0))
        }
    }

    @Test
    fun `Triangle factory delegates to the ctor`() {
        assertEquals(
            Polygon(listOf(FlatPoint(0.0, 0.0), FlatPoint(4.0, 0.0), FlatPoint(0.0, 3.0))),
            Polygon.Triangle(FlatPoint(0.0, 0.0), FlatPoint(4.0, 0.0), FlatPoint(0.0, 3.0)),
        )
    }

    /* TRANSFORMS */

    @Test
    fun `times a translation motor maps the vertex loop`() {
        val triangle = Polygon(listOf(FlatPoint(0.0, 0.0), FlatPoint(4.0, 0.0), FlatPoint(0.0, 3.0)))
        assertEquals(
            Polygon(listOf(FlatPoint(1.0, -2.0), FlatPoint(5.0, -2.0), FlatPoint(1.0, 1.0))),
            triangle * T(Direction(1.0, -2.0)),
        )
    }

    @Test
    fun `times a rotation motor maps the vertex loop`() {
        val square = Polygon.Rectangle(FlatPoint(0.0, 0.0), FlatPoint(2.0, 2.0))
        assertEquals(
            Polygon(
                listOf(
                    FlatPoint(0.0, 0.0), FlatPoint(0.0, 2.0),
                    FlatPoint(-2.0, 2.0), FlatPoint(-2.0, 0.0),
                )
            ),
            square * R(FlatPoint(0.0, 0.0), 90.deg),
        )
    }

    /* NEGATION */

    @Test
    fun `isInsideOut reads the winding`() {
        val ccw = Polygon(listOf(FlatPoint(0.0, 0.0), FlatPoint(4.0, 0.0), FlatPoint(0.0, 3.0)))
        val cw = Polygon(listOf(FlatPoint(0.0, 3.0), FlatPoint(4.0, 0.0), FlatPoint(0.0, 0.0)))
        assertFalse(ccw.isInsideOut)
        assertTrue(cw.isInsideOut)
    }

    @Test
    fun `a self-cancelling loop is not inside-out`() {
        // Symmetric bowtie: the shoelace terms cancel exactly to zero area.
        val bowtie = Polygon(
            listOf(FlatPoint(0.0, 0.0), FlatPoint(2.0, 2.0), FlatPoint(2.0, 0.0), FlatPoint(0.0, 2.0))
        )
        assertEquals(0.0, bowtie.signedArea)
        assertFalse(bowtie.isInsideOut)
    }

    @Test
    fun `negation reverses the vertex order and flips the winding`() {
        val a = FlatPoint(0.0, 0.0)
        val b = FlatPoint(4.0, 0.0)
        val c = FlatPoint(0.0, 3.0)
        val p = Polygon(listOf(a, b, c))
        val n = -p
        assertEquals(Polygon(listOf(c, b, a)), n)
        assertTrue(n.isInsideOut)
        assertEquals(-6.0, n.signedArea)
    }

    @Test
    fun `negation is an involution`() {
        val p = Polygon(listOf(FlatPoint(0.0, 0.0), FlatPoint(4.0, 0.0), FlatPoint(0.0, 3.0)))
        assertEquals(p, -(-p))
    }

    @Test
    fun `a polygon and its negation compare unequal`() {
        val p = Polygon(listOf(FlatPoint(0.0, 0.0), FlatPoint(4.0, 0.0), FlatPoint(0.0, 3.0)))
        assertNotEquals(p, -p)
    }

    @Test
    fun `negation preserves a subclass through its copy hook`() {
        class Tri(polygon: Polygon) : Polygon(polygon.vertices) {
            override fun copy(polygon: Polygon): Polygon = Tri(polygon)
        }

        val tri = Tri(Polygon(listOf(FlatPoint(0.0, 0.0), FlatPoint(4.0, 0.0), FlatPoint(0.0, 3.0))))
        assertIs<Tri>(-tri)
    }
}
