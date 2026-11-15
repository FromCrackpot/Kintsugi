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
import fromcrackpot.kintsugi.cga.d2.exceptions.InsideOutPolygonException
import fromcrackpot.kintsugi.cga.d2.exceptions.NonConvexPolygonException
import fromcrackpot.kintsugi.cga.d2.operations.queries.measures.distanceTo
import fromcrackpot.kintsugi.cga.assertNear
import fromcrackpot.kintsugi.cga.convexPolygon
import fromcrackpot.kintsugi.cga.deg
import fromcrackpot.kintsugi.cga.dilator
import fromcrackpot.kintsugi.cga.flatPoint
import fromcrackpot.kintsugi.cga.motor
import fromcrackpot.kintsugi.cga.sweep
import fromcrackpot.kintsugi.cga.versor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
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

    /* CONVEXITY */

    @Test
    fun `rectangles and triangles are convex in either winding`() {
        val rect = Polygon.Rectangle(FlatPoint(0.0, 0.0), FlatPoint(4.0, 2.0))
        val tri = Polygon.Triangle(FlatPoint(0.0, 0.0), FlatPoint(4.0, 0.0), FlatPoint(0.0, 3.0))
        assertTrue(rect.isConvex)
        assertTrue((-rect).isConvex)
        assertTrue(tri.isConvex)
        assertTrue((-tri).isConvex)
    }

    @Test
    fun `a collinear vertex keeps a polygon convex`() {
        val square = Polygon(listOf(FlatPoint(0.0, 0.0), FlatPoint(2.0, 0.0), FlatPoint(4.0, 0.0), FlatPoint(4.0, 4.0), FlatPoint(0.0, 4.0)))
        assertTrue(square.isConvex)
    }

    @Test
    fun `a sliver whose sharpest turn rounds to straight stays convex`() {
        val sliver = Polygon.Triangle(FlatPoint(0.0, 0.0), FlatPoint(1000.0, 0.0), FlatPoint(0.0, 0.01))
        assertTrue(sliver.isConvex)
    }

    @Test
    fun `a reflex vertex breaks convexity`() {
        val notch = Polygon(listOf(FlatPoint(0.0, 0.0), FlatPoint(10.0, 0.0), FlatPoint(10.0, 10.0), FlatPoint(5.0, 5.0), FlatPoint(0.0, 10.0)))
        assertFalse(notch.isConvex)
        assertFalse((-notch).isConvex)
    }

    @Test
    fun `a pentagram turns the same way at every vertex but winds twice - not convex`() {
        val pentagram = Polygon(List(5) { k ->
            val theta = PI / 2 + k * 4 * PI / 5
            FlatPoint(10.0 * cos(theta), 10.0 * sin(theta))
        })
        assertFalse(pentagram.isConvex)
    }

    @Test
    fun `a self-crossing loop is not convex`() {
        val bowtie = Polygon(listOf(FlatPoint(0.0, 0.0), FlatPoint(2.0, 2.0), FlatPoint(2.0, 0.0), FlatPoint(0.0, 2.0)))
        assertFalse(bowtie.isConvex)
    }

    @Test
    fun `a flat loop is not convex`() {
        val flat = Polygon.Triangle(FlatPoint(0.0, 0.0), FlatPoint(2.0, 0.0), FlatPoint(1.0, 0.0))
        assertFalse(flat.isConvex)
    }

    @Test
    fun `convexity survives negation - every versor and every dilator`() = sweep {
        val polygon = convexPolygon(flatPoint(), nextDouble(1.0, 300.0))
        assertTrue(polygon.isConvex, "$polygon")
        assertTrue((-polygon).isConvex, "negated $polygon")
        assertTrue((polygon * versor()).isConvex, "moved $polygon")
        assertTrue((polygon * dilator()).isConvex, "dilated $polygon")
    }

    /* SPLIT */

    private fun List<Polygon>.regions(): Set<Set<FlatPoint>> = map { it.vertices.toSet() }.toSet()

    private fun square(x: Double, y: Double, side: Double): Set<FlatPoint> =
        Polygon.Rectangle(FlatPoint(x, y), FlatPoint(x + side, y + side)).vertices.toSet()

    @Test
    fun `splitting in one answers the outline as a plain polygon`() {
        class Tri(polygon: Polygon) : Polygon(polygon.vertices) {
            override fun copy(polygon: Polygon): Polygon = Tri(polygon)
        }

        val tri = Tri(Polygon.Triangle(FlatPoint(0.0, 0.0), FlatPoint(4.0, 0.0), FlatPoint(0.0, 3.0)))
        val pieces = tri.split(1)
        assertEquals(listOf(Polygon(tri.vertices)), pieces)
        assertEquals(Polygon::class, pieces.single()::class)
    }

    @Test
    fun `the default split halves a rectangle across its longest edge`() {
        val pieces = Polygon.Rectangle(FlatPoint(0.0, 0.0), FlatPoint(60.0, 30.0)).split()
        assertEquals(setOf(square(0.0, 0.0, 30.0), square(30.0, 0.0, 30.0)), pieces.regions())
    }

    @Test
    fun `a 2-by-1 rectangle splits into eight as a 4-by-2 grid of squares`() {
        val pieces = Polygon.Rectangle(FlatPoint(0.0, 0.0), FlatPoint(60.0, 30.0)).split(8)
        val grid = (0 until 4).flatMap { i -> (0 until 2).map { j -> square(15.0 * i, 15.0 * j, 15.0) } }.toSet()
        assertEquals(grid, pieces.regions())
    }

    @Test
    fun `an isosceles triangle splits through its apex without a duplicate vertex`() {
        val pieces = Polygon.Triangle(FlatPoint(0.0, 0.0), FlatPoint(10.0, 0.0), FlatPoint(5.0, 8.0)).split()
        val left = setOf(FlatPoint(0.0, 0.0), FlatPoint(5.0, 0.0), FlatPoint(5.0, 8.0))
        val right = setOf(FlatPoint(5.0, 0.0), FlatPoint(10.0, 0.0), FlatPoint(5.0, 8.0))
        assertEquals(setOf(left, right), pieces.regions())
        assertTrue(pieces.all { it.vertices.size == 3 })
    }

    @Test
    fun `an odd count splits into equal areas`() {
        val rect = Polygon.Rectangle(FlatPoint(0.0, 0.0), FlatPoint(9.0, 4.0))
        val pieces = rect.split(3)
        assertEquals(3, pieces.size)
        pieces.forEach { assertEquals(12.0, it.signedArea, 1e-9) }
    }

    @Test
    fun `split rejects a count below one - an inside-out polygon and a non-convex one`() {
        val rect = Polygon.Rectangle(FlatPoint(0.0, 0.0), FlatPoint(4.0, 2.0))
        val notch = Polygon(listOf(FlatPoint(0.0, 0.0), FlatPoint(10.0, 0.0), FlatPoint(10.0, 10.0), FlatPoint(5.0, 5.0), FlatPoint(0.0, 10.0)))
        assertFailsWith<IllegalArgumentException> { rect.split(0) }
        assertFailsWith<InsideOutPolygonException> { (-rect).split() }
        assertFailsWith<NonConvexPolygonException> { notch.split() }
    }

    @Test
    fun `a polygon too small for its pieces collapses`() {
        val speck = Polygon.Rectangle(FlatPoint(0.0, 0.0), FlatPoint(1e-4, 1e-4))
        assertFailsWith<DegeneratePolygonException> { speck.split(8) }
    }

    @Test
    fun `split tiles a convex polygon with n convex counter-clockwise pieces of equal area`() = sweep {
        val polygon = convexPolygon(flatPoint(), nextDouble(10.0, 300.0))
        val n = nextInt(1, 17)
        val pieces = polygon.split(n)
        assertEquals(n, pieces.size, "piece count of $polygon")
        for (piece in pieces) {
            assertTrue(piece.isConvex && !piece.isInsideOut, "convex classic piece $piece of $polygon")
            assertNear(polygon.signedArea / n, piece.signedArea, "area of $piece of $polygon")
        }
        assertNear(polygon.signedArea, pieces.sumOf { it.signedArea }, "total area of $polygon")
    }

    @Test
    fun `split commutes with rigid motion`() = sweep {
        val polygon = convexPolygon(flatPoint(), nextDouble(10.0, 300.0))
        val n = nextInt(1, 17)
        val motion = motor()
        val moved = polygon.split(n).map { it * motion }
        for (piece in (polygon * motion).split(n)) {
            val twin = moved.minBy { it.centroid distanceTo piece.centroid }
            assertNear(0.0, twin.centroid distanceTo piece.centroid, "centroid of $piece")
            assertNear(twin.signedArea, piece.signedArea, "area of $piece")
        }
    }

    /* IDENTITY */

    @Test
    fun `equality is rounded and order-sensitive`() {
        val a = Polygon(listOf(FlatPoint(0.0, 0.0), FlatPoint(4.0, 0.0), FlatPoint(0.0, 3.0)))
        val b = Polygon(listOf(FlatPoint(0.0, 0.0), FlatPoint(4.0, 0.0), FlatPoint(0.0, 3.0)))
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        // Starting vertex and traversal direction are data: a rotated list is a different value.
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
        // each edge. The visual lookup is unmoved.
        val wall = -Polygon.Rectangle(FlatPoint(1.0, 2.0), FlatPoint(5.0, 5.0))
        assertEquals(Segment(FlatPoint(5.0, 2.0), FlatPoint(1.0, 2.0)), wall.edges[BOTTOM])
        assertEquals(Segment(FlatPoint(5.0, 5.0), FlatPoint(5.0, 2.0)), wall.edges[RIGHT])
        assertEquals(Segment(FlatPoint(1.0, 5.0), FlatPoint(5.0, 5.0)), wall.edges[TOP])
        assertEquals(Segment(FlatPoint(1.0, 2.0), FlatPoint(1.0, 5.0)), wall.edges[LEFT])
    }

    @Test
    fun `RectSide follows the visual sides through a mirror`() {
        // Reflecting across x = 0 keeps the list order but swaps the visually left and right
        // sides. No index arithmetic can recover this case, since the mirror flips the winding
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
