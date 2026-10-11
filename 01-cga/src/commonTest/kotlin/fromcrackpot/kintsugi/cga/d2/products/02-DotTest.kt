package fromcrackpot.kintsugi.cga.d2.products

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Circle
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Line
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.RoundPoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DotTest {

    /*
    RoundPoint <dot> RoundPoint
    */

    @Test
    fun `self-dot of a euclidean round point is zero`() {
        // Euclidean point has radius² = 0, and self-dot = −radius².
        val p = RoundPoint(3.0, 4.0)
        assertEquals(0.0, p dot p)
    }

    @Test
    fun `two euclidean round points dot to minus half d-squared`() {
        // (0,0) and (3,4): d² = 25 → dot = −12.5
        assertEquals(-12.5, RoundPoint(0.0, 0.0) dot RoundPoint(3.0, 4.0))
    }

    @Test
    fun `round-round dot is symmetric`() {
        val a = RoundPoint(1.0, 2.0)
        val b = RoundPoint(-3.0, 5.0)
        assertEquals(a dot b, b dot a)
    }

    @Test
    fun `self-dot of a non-euclidean round point equals minus radius squared`() {
        // RoundPoint(1, 0, 1, 0): radius² = 2·1·0 − 1² − 0² = −1 (imaginary).
        // Self-dot = ax² + ay² − 2·az·aw = 1 + 0 − 0 = 1 = −radius². ✓
        val p = RoundPoint(1.0, 0.0, 1.0, 0.0)
        assertEquals(-p.radius2, p dot p)
    }

    /*
    FlatPoint <dot> FlatPoint
    */

    @Test
    fun `two finite flat points dot to minus half d-squared`() {
        // (0,0) and (3,4): d² = 25 → dot = −12.5
        assertEquals(-12.5, FlatPoint(0.0, 0.0) dot FlatPoint(3.0, 4.0))
    }

    @Test
    fun `self-dot of a finite flat point is zero`() {
        assertEquals(0.0, FlatPoint(7.0, -2.0) dot FlatPoint(7.0, -2.0))
    }

    @Test
    fun `flat-flat dot is symmetric`() {
        val a = FlatPoint(1.0, 2.0)
        val b = FlatPoint(-3.0, 5.0)
        assertEquals(a dot b, b dot a)
    }

    @Test
    fun `at-infinity flat points dot directionally (no distance meaning)`() {
        // At-infinity inputs degenerate to px·qx + py·qy.
        val a = FlatPoint(1.0, 0.0, pz = 0.0)
        val b = FlatPoint(2.0, 3.0, pz = 0.0)
        assertEquals(2.0, a dot b)
    }

    @Test
    fun `at-infinity dot finite flat point uses directional form`() {
        val atInf = FlatPoint(1.0, 0.0, pz = 0.0)
        val finite = FlatPoint(3.0, 4.0)
        assertEquals(3.0, atInf dot finite)
        assertEquals(3.0, finite dot atInf)   // either operand at infinity degenerates
    }

    @Test
    fun `flat-flat dot stays exact at large coordinates (difference-first form)`() {
        // Same (3, 4) separation as above but 10⁷ from the origin. The retired toRound
        // route computed aw ≈ 5·10¹³ before subtracting (ULP ~10⁻², above SCALE's 10⁻⁴
        // tolerance); the difference-first form's error scales with the separation.
        val a = FlatPoint(1e7, 0.0)
        val b = FlatPoint(1e7 + 3.0, 4.0)
        assertEquals(-12.5, a dot b)
    }

    /*
    FlatPoint <dot> Line
    */

    @Test
    fun `point on the line dots to zero`() {
        // y = 0 line; point (5, 0) lies on it.
        assertEquals(0.0, FlatPoint(5.0, 0.0) dot Line(0.0, 1.0, 0.0))
    }

    @Test
    fun `point off the line dots to nonzero`() {
        // y = 0 line; point (5, 3) is 3 above it.
        assertEquals(3.0, FlatPoint(5.0, 3.0) dot Line(0.0, 1.0, 0.0))
    }

    @Test
    fun `point dot line carries signed-distance information`() {
        // y = 0 line, points on opposite sides should have opposite-signed dots.
        val above = FlatPoint(0.0, 2.0) dot Line(0.0, 1.0, 0.0)
        val below = FlatPoint(0.0, -2.0) dot Line(0.0, 1.0, 0.0)
        assertEquals(-above, below)
    }

    /*
    RoundPoint <dot> Line
    */

    @Test
    fun `round point on the line dots to zero`() {
        // y = 0 line; point (5, 0) lies on it.
        assertEquals(0.0, RoundPoint(5.0, 0.0) dot Line(0.0, 1.0, 0.0))
    }

    @Test
    fun `round point off the line dots to the signed distance`() {
        // y = 0 line; point (5, 3) is 3 above it, (5, -3) is 3 below.
        assertEquals(3.0, RoundPoint(5.0, 3.0) dot Line(0.0, 1.0, 0.0))
        assertEquals(-3.0, RoundPoint(5.0, -3.0) dot Line(0.0, 1.0, 0.0))
    }

    @Test
    fun `round point dot line agrees with the flat form (aw drops out)`() {
        // A Line has no e321 component, so aw never participates — a non-lift aw
        // (here a circle center's semantic weight) must not perturb the result.
        val l = Line(3.0, 4.0, -2.0)
        val p = RoundPoint(1.5, -0.5)
        assertEquals(p.toFlat dot l, p dot l)
        val semanticAw = RoundPoint(1.5, -0.5, 1.0, 42.0)
        assertEquals(p dot l, semanticAw dot l)
    }

    @Test
    fun `at-infinity round point dot line uses directional form`() {
        // az = 0 kills the gz term: only the direction-vs-normal dot survives.
        val l = Line(0.0, 1.0, -7.0)                       // y = 7
        assertEquals(0.0, RoundPoint(1.0, 0.0, 0.0, 0.0) dot l)   // direction along the line
        assertEquals(1.0, RoundPoint(0.0, 1.0, 0.0, 0.0) dot l)   // direction along the normal
    }

    /*
    RoundPoint <dot> Circle
    */

    @Test
    fun `euclidean round point on classic circle dots to zero`() {
        // Unit circle at origin; point (1, 0) lies on it.
        val c = Circle(centerX = 0.0, centerY = 0.0, radius = 1.0)
        assertEquals(0.0, RoundPoint(1.0, 0.0) dot c)
    }

    @Test
    fun `euclidean round point inside classic circle has positive dot`() {
        // Unit circle at origin; (0.5, 0) is inside. Expected ½(r² − d²) = ½(1 − 0.25) = 0.375.
        val c = Circle(centerX = 0.0, centerY = 0.0, radius = 1.0)
        assertEquals(0.375, RoundPoint(0.5, 0.0) dot c)
    }

    @Test
    fun `euclidean round point outside classic circle has negative dot`() {
        // Unit circle at origin; (2, 0) is outside. Expected ½(1 − 4) = −1.5.
        val c = Circle(centerX = 0.0, centerY = 0.0, radius = 1.0)
        assertEquals(-1.5, RoundPoint(2.0, 0.0) dot c)
    }

    @Test
    fun `dot with inside-out circle is the negation of dot with classic`() {
        // Same geometric circle, opposite orientation: every component negates → dot negates.
        val classic = Circle(centerX = 0.0, centerY = 0.0, radius = 1.0)        // (0, 0, 0.5, -1)
        val insideOut = Circle(cx = 0.0, cy = 0.0, cz = -0.5, cw = 1.0)          // negated components
        val p = RoundPoint(0.5, 0.0)
        assertEquals(-(p dot classic), p dot insideOut)
    }

    /*
    FlatPoint <dot> Circle
    */

    @Test
    fun `finite flat point dot circle agrees with round-point dot`() {
        // FlatPoint(p) dot Circle should equal FlatPoint(p).toRound dot Circle for finite points.
        val c = Circle(centerX = 1.0, centerY = -2.0, radius = 3.0)
        val p = FlatPoint(0.5, 0.5)
        assertEquals(p.toRound dot c, p dot c)
    }

    @Test
    fun `finite flat point on circle dots to zero`() {
        val c = Circle(centerX = 0.0, centerY = 0.0, radius = 1.0)
        assertEquals(0.0, FlatPoint(0.0, 1.0) dot c)
    }

    @Test
    fun `at-infinity flat point dot circle uses directional form`() {
        // Branch hits px·cx + py·cy without the conformal aw·cw term.
        val c = Circle(centerX = 0.0, centerY = 0.0, radius = 1.0)   // cx=0, cy=0
        // Direction (1, 0): both cx and cy are 0 here → dot is 0.
        assertEquals(0.0, FlatPoint(1.0, 0.0, pz = 0.0) dot c)
    }

    /*
    Circle <dot> Circle
    */

    @Test
    fun `self-dot of a unit circle equals minus radius squared`() {
        val c = Circle(centerX = 0.0, centerY = 0.0, radius = 1.0)
        assertEquals(-1.0, c dot c)
        assertEquals(-c.radius2, c dot c)
    }

    @Test
    fun `two unitized classic circles dot to half d-squared minus sum of radii squared`() {
        // c1 at (0,0) r=1, c2 at (3,0) r=1: ½(9 − 1 − 1) = 3.5
        val c1 = Circle(centerX = 0.0, centerY = 0.0, radius = 1.0)
        val c2 = Circle(centerX = 3.0, centerY = 0.0, radius = 1.0)
        assertEquals(3.5, c1 dot c2)
    }

    @Test
    fun `circle-circle dot is symmetric`() {
        val c1 = Circle(centerX = 1.0, centerY = 2.0, radius = 0.5)
        val c2 = Circle(centerX = -1.0, centerY = 0.0, radius = 2.0)
        assertEquals(c1 dot c2, c2 dot c1)
    }

    @Test
    fun `concentric circles have negative dot`() {
        // Concentric: d = 0 → ½(0 − r₁² − r₂²) < 0
        val outer = Circle(centerX = 0.0, centerY = 0.0, radius = 2.0)
        val inner = Circle(centerX = 0.0, centerY = 0.0, radius = 1.0)
        assertTrue((outer dot inner) < 0)
    }
}