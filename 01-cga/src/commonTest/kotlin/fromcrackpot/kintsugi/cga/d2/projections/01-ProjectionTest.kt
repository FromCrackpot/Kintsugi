package fromcrackpot.kintsugi.cga.d2.projections

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.*
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Segment
import fromcrackpot.kintsugi.cga.d2.exceptions.DegenerateDipoleException
import fromcrackpot.kintsugi.cga.d2.products.dot
import fromcrackpot.kintsugi.cga.d2.products.expand
import fromcrackpot.kintsugi.cga.d2.products.join
import fromcrackpot.kintsugi.cga.d2.products.meet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ProjectionTest {

    /*
    RoundPoint project Line => RoundPoint
    */

    @Test
    fun `point projects onto a line at its perpendicular foot sized by the squared distance`() {
        val a = RoundPoint(1.0, 3.0) project Line(0.0, 1.0, 0.0)
        assertEquals(RoundPoint(1.0, 0.0, 1.0, 5.0), a)
        assertEquals(9.0, a.radius2)
    }

    @Test
    fun `point on the line projects to itself`() {
        assertEquals(RoundPoint(3.0, 0.0), RoundPoint(3.0, 0.0) project Line(0.0, 1.0, 0.0))
    }

    @Test
    fun `projection onto a line matches the meet of the line with the expansion`() {
        // The compositional spelling b meet (a expand b); the line's self-pairing is 1.
        val a = RoundPoint(1.0, 3.0)
        val line = Line(0.0, 1.0, 0.0)
        assertEquals(line meet (a expand line), a project line)
    }

    /*
    RoundPoint project Circle => RoundPoint
    */

    @Test
    fun `point projects onto a circle incident with it on the radial line`() {
        val circle = Circle(centerX = 0.0, centerY = 0.0, radius = 1.0)
        val a = RoundPoint(3.0, 0.0) project circle
        assertEquals(RoundPoint(0.6, 0.0, 1.0, 0.5), a)
        assertEquals(0.0, a dot circle)
        assertEquals(0.64, a.radius2, 1e-12)
    }

    @Test
    fun `point on the circle projects to itself`() {
        val circle = Circle(centerX = 0.0, centerY = 0.0, radius = 1.0)
        assertEquals(RoundPoint(1.0, 0.0), RoundPoint(1.0, 0.0) project circle)
    }

    /*
    RoundPoint project Dipole => RoundPoint
    */

    @Test
    fun `point projects onto a dipole between the endpoints in squared-distance ratio`() {
        // From (0,1): distances² to (0,0) and (2,0) are 1 and 5 → division point (1/3, 0).
        val d = RoundPoint(0.0, 0.0) join RoundPoint(2.0, 0.0)
        assertEquals(RoundPoint(1.0 / 3.0, 0.0, 1.0, 1.0 / 3.0), RoundPoint(0.0, 1.0) project d)
    }

    @Test
    fun `collinear point projects between the endpoints in squared-distance ratio`() {
        // From (3,0): distances² 9 and 1 → division point (1.8, 0), inside the endpoints.
        val d = RoundPoint(0.0, 0.0) join RoundPoint(2.0, 0.0)
        assertEquals(RoundPoint(1.8, 0.0, 1.0, 1.8), RoundPoint(3.0, 0.0) project d)
    }

    /*
    FlatPoint project Line => FlatPoint
    */

    @Test
    fun `flat point projects onto a line at its perpendicular foot`() {
        assertEquals(FlatPoint(3.0, 2.0), FlatPoint(3.0, 4.0) project Line(0.0, 1.0, -2.0))
    }

    @Test
    fun `flat projection matches the meet of the line with the expansion`() {
        val p = FlatPoint(3.0, 4.0)
        val line = Line(0.0, 1.0, -2.0)
        assertEquals(line meet (p expand line), p project line)
    }

    @Test
    fun `direction projects onto a line as its component along it`() {
        // The classic vector projection: (1,2) onto a horizontal line keeps (1,0).
        val projected = Direction(1.0, 2.0) project Line(0.0, 1.0, -2.0)
        assertEquals(Direction(1.0, 0.0), projected)
    }

    /*
    FlatPoint project Circle => Dipole
    */

    @Test
    fun `flat point projects onto a circle as the radial crossing dipole`() {
        // The radial through (3,0) cuts the unit circle at (∓1, 0) — the join of that pair.
        val circle = Circle(centerX = 0.0, centerY = 0.0, radius = 1.0)
        val expected = RoundPoint(-1.0, 0.0) join RoundPoint(1.0, 0.0)
        assertEquals(expected, FlatPoint(3.0, 0.0) project circle)
    }

    @Test
    fun `flat point at the circle's center collapses the projection`() {
        val circle = Circle(centerX = 0.0, centerY = 0.0, radius = 1.0)
        assertFailsWith<DegenerateDipoleException> { FlatPoint(0.0, 0.0) project circle }
    }

    /*
    FlatPoint project Segment => FlatPoint
    */

    @Test
    fun `flat point projects onto a segment at its perpendicular foot when the foot lies within`() {
        val segment = Segment(FlatPoint(0.0, 0.0), FlatPoint(10.0, 0.0))
        assertEquals(FlatPoint(3.0, 0.0), FlatPoint(3.0, 4.0) project segment)
    }

    @Test
    fun `a foot beyond the start clamps to the start endpoint`() {
        val segment = Segment(FlatPoint(0.0, 0.0), FlatPoint(10.0, 0.0))
        assertEquals(FlatPoint(0.0, 0.0), FlatPoint(-5.0, 4.0) project segment)
    }

    @Test
    fun `a foot beyond the end clamps to the end endpoint`() {
        val segment = Segment(FlatPoint(0.0, 0.0), FlatPoint(10.0, 0.0))
        assertEquals(FlatPoint(10.0, 0.0), FlatPoint(15.0, 4.0) project segment)
    }

    @Test
    fun `point on the segment projects to itself`() {
        val segment = Segment(FlatPoint(0.0, 0.0), FlatPoint(10.0, 0.0))
        assertEquals(FlatPoint(4.0, 0.0), FlatPoint(4.0, 0.0) project segment)
    }

    /*
    Dipole project Line => Dipole
    */

    @Test
    fun `dipole projects onto a line as the cut of its endpoint circle`() {
        // {(1,1),(3,1)} onto y = 0: the circle through both centered on the line has r² = 2;
        // the cut dipole is centered at (2,0) with the same radius².
        val d = RoundPoint(1.0, 1.0) join RoundPoint(3.0, 1.0)
        val projected = d project Line(0.0, 1.0, 0.0)
        assertEquals(RoundPoint(2.0, 0.0, 1.0, 3.0), projected.center)
        assertEquals(2.0, projected.radius2)
    }

    @Test
    fun `dipole on the line projects to itself`() {
        val d = RoundPoint(0.0, 0.0) join RoundPoint(2.0, 0.0)
        assertEquals(d, d project Line(0.0, 1.0, 0.0))
    }

    @Test
    fun `carrier perpendicular to the line collapses to the shared foot payload`() {
        val d = RoundPoint(1.0, 0.0) join RoundPoint(1.0, 2.0)
        val ex = assertFailsWith<DegenerateDipoleException> { d project Line(0.0, 1.0, 0.0) }
        assertEquals(FlatPoint(1.0, 0.0), ex.point)
    }

    /*
    Dipole project Circle => Dipole
    */

    @Test
    fun `dipole with endpoints on the circle projects to itself`() {
        val d = RoundPoint(1.0, 0.0) join RoundPoint(1.0, 2.0)
        val circle = Circle(centerX = 1.0, centerY = 1.0, radius = 1.0)
        assertEquals(d, d project circle)
    }

    @Test
    fun `dipole projects onto a circle where its orthogonal endpoint circle meets it`() {
        // The compositional spelling b meet (a expand b), for a unit self-pairing target.
        val d = RoundPoint(0.0, 0.0) join RoundPoint(2.0, 0.0)
        val circle = Circle(centerX = 1.0, centerY = 2.0, radius = 1.0)
        assertEquals(circle meet (d expand circle), d project circle)
    }
}
