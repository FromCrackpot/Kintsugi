package fromcrackpot.kintsugi.cga.d2.shapes.primitives

import fromcrackpot.kintsugi.cga.d2.products.join
import fromcrackpot.kintsugi.cga.d2.products.meet
import fromcrackpot.kintsugi.cga.d2.queries.predicates.on
import fromcrackpot.kintsugi.cga.d2.exceptions.DegenerateCircleException
import fromcrackpot.kintsugi.cga.d2.exceptions.DegenerateDipoleException
import fromcrackpot.kintsugi.cga.d2.exceptions.NonBladeDipoleException
import kotlin.math.sqrt
import kotlin.test.*

class DipoleTest {

    /*
    CTOR
     */

    @Test
    fun `constructor unitizes by carrier norm sqrt(gx2+gy2)`() {
        // (gx, gy) = (3, 4), norm = 5 → divide all six components by 5.
        // Components satisfy the Plücker constraint: 3·0 + 4·10 + 10·(-4) = 0.
        val d = Dipole(3.0, 4.0, 10.0, 0.0, 10.0, -4.0)
        assertEquals(0.6, d.gx)
        assertEquals(0.8, d.gy)
        assertEquals(2.0, d.gz)
        assertEquals(0.0, d.px)
        assertEquals(2.0, d.py)
        assertEquals(-0.8, d.pz)
    }

    @Test
    fun `constructor with non-blade bivector throws NonBladeDipoleException carrying the residual`() {
        // gx·px + gy·py + gz·pz on the unitized components = (3·5 + 4·15 + 10·20) / 5² = 275/25 = 11.0
        val ex = assertFailsWith<NonBladeDipoleException> { Dipole(3.0, 4.0, 10.0, 5.0, 15.0, 20.0) }
        assertEquals(11.0, ex.residual)
    }

    @Test
    fun `constructor with zero carrier and non-blade components throws NonBladeDipoleException`() {
        // At infinity the Plücker residual reduces to gz·pz = 5·3 = 15 ≠ 0 — not a blade.
        val ex = assertFailsWith<NonBladeDipoleException> { Dipole(0.0, 0.0, 5.0, 1.0, 2.0, 3.0) }
        assertEquals(15.0, ex.residual)
    }

    @Test
    fun `constructor with zero carrier keeps an at-infinity blade raw`() {
        // Plücker: gz·pz = 5·0 = 0 — a valid dipole at infinity; no unitization possible, kept raw.
        val d = Dipole(0.0, 0.0, 5.0, 1.0, 2.0, 0.0)
        assertEquals(0.0, d.gx)
        assertEquals(0.0, d.gy)
        assertEquals(5.0, d.gz)
        assertEquals(1.0, d.px)
        assertEquals(2.0, d.py)
        assertEquals(0.0, d.pz)
        assertTrue(d.isAtInfinity)
    }

    @Test
    fun `constructor with whole g-part zero throws DegenerateDipoleException carrying the FlatPoint it collapsed to`() {
        // g = 0 leaves only the e41/e42/e43 span — the blade IS FlatPoint(1, 2, 3), not a dipole.
        val ex = assertFailsWith<DegenerateDipoleException> { Dipole(0.0, 0.0, 0.0, 1.0, 2.0, 3.0) }
        assertEquals(FlatPoint(1.0, 2.0, 3.0), ex.point)
    }

    @Test
    fun `constructor with all-zero components throws with zero point`() {
        val ex = assertFailsWith<DegenerateDipoleException> { Dipole(0.0, 0.0, 0.0, 0.0, 0.0, 0.0) }
        assertEquals(FlatPoint(0.0, 0.0, 0.0), ex.point)
    }

    /*
    MATH
     */

    // Reference vertical dipole: carrier is the y-axis (x=0), endpoints (0,0) and (0,4),
    // center (0,2), radius 2. Components: (gx,gy,gz)=(1,0,0), (px,py,pz)=(0,0,2).
    private val vertical = Dipole(1.0, 0.0, 0.0, 0.0, 0.0, 2.0)

    @Test
    fun `isAtInfinity is false for a finite dipole`() {
        assertFalse(vertical.isAtInfinity)
    }

    @Test
    fun `radius2 matches its component formula and radius is its sqrt`() {
        // r² = pz² - gz² - 2(gx·py - gy·px) = 4 - 0 - 0 = 4
        assertEquals(4.0, vertical.radius2)
        assertEquals(2.0, vertical.radius)
        assertFalse(vertical.isImaginary)
    }

    @Test
    fun `imaginary dipole exposes magnitude radius and isImaginary flag`() {
        // (gx,gy,gz,px,py,pz) = (1,0,1,0,0,0): r² = 0 - 1 - 0 = -1
        val imaginary = Dipole(1.0, 0.0, 1.0, 0.0, 0.0, 0.0)
        assertEquals(-1.0, imaginary.radius2)
        assertEquals(1.0, imaginary.radius)
        assertTrue(imaginary.isImaginary)
    }

    // Off-origin sibling of [vertical]: carrier x=3, endpoints (3,0) and (3,4), center (3,2),
    // radius 2. Components (1,0,-3, 6,-4.5,2) satisfy Plücker: 1·6 + 0·(-4.5) + (-3)·2 = 0.
    private val offset = Dipole(1.0, 0.0, -3.0, 6.0, -4.5, 2.0)

    @Test
    fun `norm is the flat bulk magnitude sqrt(px2+py2)`() {
        assertEquals(7.5, offset.norm) // sqrt(6² + 4.5²)
        // A dipole whose carrier passes through the origin has zero flat bulk.
        assertEquals(0.0, vertical.norm)
    }

    @Test
    fun `centerNorm is the distance of the center from the origin`() {
        // sqrt(gz² + pz²) for a unitized dipole: center (0,2) → 2, center (3,2) → sqrt(13)
        assertEquals(2.0, vertical.centerNorm)
        assertEquals(sqrt(13.0), offset.centerNorm)
    }

    @Test
    fun `center is the round point at the midpoint of the endpoints`() {
        // Round center: RoundPoint(0, 2, 1, 4). aw=4 carries radius²/2, NOT the naive lift (2).
        assertEquals(RoundPoint(0.0, 2.0, 1.0, 4.0), vertical.center)
        assertEquals(FlatPoint(0.0, 2.0), vertical.center.toFlat)
    }

    @Test
    fun `attitude is the carrier tangent (gy, -gx) as a round point at infinity`() {
        // Attitude: RoundPoint(gy, -gx, 0, -pz) = (0, -1, 0, -2)
        assertEquals(RoundPoint(0.0, -1.0, 0.0, -2.0), vertical.attitude)
    }

    @Test
    fun `start and end lie on the carrier at center minus or plus radius times attitude`() {
        // center=(0,2), radius=2, attitude=(0,-1) → start=(0,4) (center − r·att), end=(0,0) (center + r·att)
        assertEquals(RoundPoint(0.0, 4.0), vertical.start)
        assertEquals(RoundPoint(0.0, 0.0), vertical.end)
    }

    @Test
    fun `start joined with end reconstructs the dipole (Containers invariant)`() {
        // Axis-aligned: gy=0, gz=0, px=0, py=0 — the simple case.
        assertEquals(vertical, vertical.start join vertical.end)
        // Non-axis-aligned dipole-blade (built via join to guarantee the Plücker constraint
        // `gx·px + gy·py + gz·pz = 0` that the bare Dipole ctor doesn't enforce).
        val skew = RoundPoint(1.0, 2.0) join RoundPoint(4.0, -3.0)
        assertEquals(skew, skew.start join skew.end)
    }

    @Test
    fun `Containers invariant holds for imaginary dipoles too`() {
        // Disjoint circles meet at an imaginary dipole; start/end come out real-valued
        // (radius = sqrt(|radius²|)) but their wedge reconstructs the imaginary blade exactly.
        // :svg relies on this for dipole round-trip without an "imaginary" sidecar flag.
        val imaginary = Circle(0.0, 0.0, 1.0) meet Circle(3.0, 0.0, 1.0)
        assertTrue(imaginary.isImaginary)
        assertEquals(imaginary, imaginary.start join imaginary.end)
    }

    @Test
    fun `carrier is the line through the endpoints`() {
        // x = 0 line: Line(1, 0, 0)
        assertEquals(Line(1.0, 0.0, 0.0), vertical.carrier)
    }

    @Test
    fun `cocarrier is the line orthogonal to carrier through the center`() {
        // perpendicular to y-axis through (0,2): y = 2 line, Line(0, 1, -2)
        assertEquals(Line(0.0, 1.0, -2.0), vertical.cocarrier)
    }

    @Test
    fun `container is the circle having the endpoints as a diameter`() {
        // con(d) = Circle(gx·gz + gy·pz, gy·gz − gx·pz, gz² + gx·py − gy·px, gx² + gy²) = (0, −2, 0, 1).
        val container = vertical.container
        assertEquals(Circle(0.0, -2.0, 0.0, 1.0), container)
        // Same center and radius² as the dipole; both endpoints lie on it.
        assertEquals(vertical.center, container.center)
        assertEquals(vertical.radius2, container.radius2)
        assertTrue(vertical.start on container)
        assertTrue(vertical.end on container)
    }

    @Test
    fun `container of an imaginary dipole is an imaginary circle with the same radius2`() {
        val imaginary = Dipole(1.0, 0.0, 1.0, 0.0, 0.0, 0.0) // r² = −1
        val container = imaginary.container
        assertEquals(Circle(1.0, 0.0, 1.0, 1.0), container)
        assertEquals(imaginary.radius2, container.radius2)
        assertTrue(container.isImaginary)
    }

    @Test
    fun `container of a dipole at infinity throws DegenerateCircleException`() {
        // Zero round weight (gx = gy = 0) collapses the container's cw = gx²+gy²; the surviving
        // cz = gz² makes the carried line a line at infinity.
        val atInfinity = Dipole(0.0, 0.0, 5.0, 1.0, 2.0, 0.0)
        val ex = assertFailsWith<DegenerateCircleException> { atInfinity.container }
        assertEquals(Line(0.0, 0.0, 25.0), ex.line)
    }

    @Test
    fun `canonical of positive-gx dipole is itself`() {
        assertEquals(vertical, vertical.canonical)
    }

    @Test
    fun `canonical flips negative-gx dipole`() {
        val flipped = Dipole(-1.0, 0.0, 0.0, 0.0, 0.0, -2.0)
        assertEquals(vertical, flipped.canonical)
    }

    @Test
    fun `canonical is idempotent`() {
        val flipped = Dipole(-1.0, 0.0, 0.0, 0.0, 0.0, -2.0)
        assertEquals(flipped.canonical, flipped.canonical.canonical)
    }

    @Test
    fun `equal dipoles have equal canonicals`() {
        // Invariant: a == b ⇒ a.canonical == b.canonical.
        // Pick two dipoles that compare equal under FP-rounded equality.
        val a = Dipole(1.0, 0.0, 0.0, 0.0, 0.0, 2.0)
        val b = Dipole(1.0 + 1e-6, 0.0, 0.0, 0.0, 0.0, 2.0)
        assertEquals(a, b)
        assertEquals(a.canonical, b.canonical)
    }

    /*
    IDENTITY
     */

    @Test
    fun `equal coords yield equal dipoles`() {
        assertEquals(
            Dipole(1.0, 0.0, 0.0, 0.0, 0.0, 2.0),
            Dipole(1.0, 0.0, 0.0, 0.0, 0.0, 2.0),
        )
    }

    @Test
    fun `opposing-orientation dipoles are not equal`() {
        val a = Dipole(1.0, 0.0, 0.0, 0.0, 0.0, 2.0)
        val flipped = Dipole(-1.0, 0.0, 0.0, 0.0, 0.0, -2.0)
        assertNotEquals(a, flipped)
    }

    @Test
    fun `canonical equates opposing-orientation dipoles`() {
        val a = Dipole(1.0, 0.0, 0.0, 0.0, 0.0, 2.0)
        val flipped = Dipole(-1.0, 0.0, 0.0, 0.0, 0.0, -2.0)
        assertEquals(a.canonical, flipped.canonical)
    }

    @Test
    fun `equality tolerates differences within rounding scale`() {
        // SCALE = 10_000 → values agreeing to 1e-4 compare equal.
        val a = Dipole(1.0, 0.0, 0.0, 0.0, 0.0, 2.0)
        val b = Dipole(1.0, 0.0, 0.0, 0.0, 0.0, 2.0 + 1e-6)
        assertEquals(a, b)
    }

    @Test
    fun `equal dipoles have equal hashCode`() {
        assertEquals(
            Dipole(1.0, 0.0, 0.0, 0.0, 0.0, 2.0).hashCode(),
            Dipole(1.0, 0.0, 0.0, 0.0, 0.0, 2.0).hashCode(),
        )
    }

    @Test
    fun `dipole is not equal to null or different type`() {
        assertFalse(vertical.equals(null))
        assertFalse(vertical.equals("Dipole"))
    }

    @Test
    fun `equals is reflexive`() {
        assertTrue(vertical.equals(vertical))
    }

    /* NEGATION */

    @Test
    fun `negation exchanges the endpoints`() {
        val d = RoundPoint(1.0, 2.0) join RoundPoint(5.0, -1.0)
        val n = -d
        assertEquals(d.start, n.end)
        assertEquals(d.end, n.start)
        assertEquals(d.center, n.center)
    }

    @Test
    fun `negation flips the attitude`() {
        val d = RoundPoint(1.0, 2.0) join RoundPoint(5.0, -1.0)
        assertEquals(-(d.attitude), (-d).attitude)
    }

    @Test
    fun `negation is an involution`() {
        val d = RoundPoint(1.0, 2.0) join RoundPoint(5.0, -1.0)
        assertEquals(d, -(-d))
    }

    @Test
    fun `a dipole and its negation compare unequal`() {
        val d = RoundPoint(1.0, 2.0) join RoundPoint(5.0, -1.0)
        assertNotEquals(d, -d)
    }

    @Test
    fun `negation preserves the canonical representative`() {
        val d = RoundPoint(1.0, 2.0) join RoundPoint(5.0, -1.0)
        assertEquals(d.canonical, (-d).canonical)
    }

    @Test
    fun `negation of a dipole at infinity negates the raw components`() {
        val inf = Dipole(0.0, 0.0, 5.0, 1.0, 2.0, 0.0)
        assertEquals(Dipole(0.0, 0.0, -5.0, -1.0, -2.0, 0.0), -inf)
    }
}

