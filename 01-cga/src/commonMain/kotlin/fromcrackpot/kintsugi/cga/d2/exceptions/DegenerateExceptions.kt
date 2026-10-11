package fromcrackpot.kintsugi.cga.d2.exceptions

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Circle
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Dipole
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Line
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polyline
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Segment
import fromcrackpot.kintsugi.cga.d2.transforms.Flector
import fromcrackpot.kintsugi.cga.d2.transforms.Motor

/**
 * The collapse of a would-be [Dipole] into a single flat point — no dipole exists to build.
 *
 * Thrown by the [Dipole] constructor when the whole g-part (`gx`, `gy`, `gz`) rounds to zero
 * at the `SCALE` quantum: the remaining blade lies in the `e41`/`e42`/`e43` span and is the
 * flat point [point], not a dipole. An all-zero input carries the zero flat point.
 */
public class DegenerateDipoleException(
    /** The collapsed value — the flat point the failed blade amounts to. */
    public val point: FlatPoint,
) : Exception()

/**
 * The rejection of six components that form no dipole at all — a bivector that is not the
 * wedge of two points.
 *
 * Thrown by the [Dipole] constructor when the Plücker blade constraint
 * `gx·px + gy·py + gz·pz = 0` fails: a bivector violating it is not a factorizable wedge
 * `a ∧ b`, so no [Dipole.start]/[Dipole.end] pair could rebuild it. The residual is evaluated
 * on the unitized components and must round to zero at the `SCALE` quantum.
 */
public class NonBladeDipoleException(
    /** The constraint residual `gx·px + gy·py + gz·pz` on the unitized components — raw. */
    public val residual: Double,
) : Exception()

/**
 * The collapse of a would-be [Circle] into a line — collinear geometry, no circle to build.
 *
 * Thrown by the [Circle] constructor when the round weight `cw` rounds to zero at the `SCALE`
 * quantum: the remaining components are the [Line] the geometry flattens onto — three
 * collinear points trace a line, not a circle. An all-zero input carries the all-zero line.
 */
public class DegenerateCircleException(
    /** The collapsed value — the line the failed circle flattens onto. */
    public val line: Line,
) : Exception()

/**
 * The rejection of a vertex list that closes no polygon.
 *
 * Thrown by the [Polygon] constructor for fewer than 3 vertices, a vertex at infinity, or two
 * consecutive vertices — the last→first wrap included — coinciding at the `SCALE` quantum:
 * their shared edge would collapse, the same coincident-endpoint rule [Segment] enforces.
 * The [Polygon.Rectangle] factory throws it for an at-infinity corner or an extent that
 * rounds to zero.
 */
public class DegeneratePolygonException(
    /** The human-readable diagnostic naming the offending vertex or count. */
    public val reason: String,
) : Exception()

/**
 * The rejection of a vertex list that chains into no polyline.
 *
 * Thrown by the [Polyline] constructor for fewer than 2 vertices, a vertex at infinity, or
 * two consecutive vertices coinciding at the `SCALE` quantum — their shared edge would
 * collapse, the same coincident-endpoint rule [Segment] enforces. Unlike [Polygon] there is
 * no last→first wrap check: a chain may revisit points, the first and last included.
 */
public class DegeneratePolylineException(
    /** The human-readable diagnostic naming the offending vertex or count. */
    public val reason: String,
) : Exception()

/**
 * The rejection of components that make no rigid motion — a would-be [Motor] with nothing to
 * unitize by.
 *
 * Thrown by the [Motor] constructor when the flat weight norm `√(mz² + mw²)` rounds to zero
 * at the `SCALE` quantum: a zero-weight motor has no unitized representation, and its
 * remaining `e41`/`e42` part generates no rotation or translation.
 */
public class DegenerateMotorException(
    /** The offending `e41` component — raw, for diagnostics. */
    public val mx: Double,
    /** The offending `e42` component — raw, for diagnostics. */
    public val my: Double,
) : Exception()

/**
 * The rejection of components that make no mirror motion — a would-be [Flector] whose mirror
 * line lies at infinity.
 *
 * Thrown by the [Flector] constructor when the mirror's normal norm `√(fx² + fy²)` rounds to
 * zero at the `SCALE` quantum: the mirror line lies at infinity, and reflecting across it is
 * a point inversion — a proper motion, not the improper isometry a flector encodes — so no
 * unitized flector exists.
 */
public class DegenerateFlectorException(
    /** The offending `e412` (line moment) component — raw, for diagnostics. */
    public val fz: Double,
    /** The offending `e4` (glide) component — raw, for diagnostics. */
    public val fw: Double,
) : Exception()
