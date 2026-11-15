package fromcrackpot.kintsugi.cga.d2.exceptions

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Circle
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Dipole
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Line
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polyline
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Segment
import fromcrackpot.kintsugi.cga.d2.transforms.D
import fromcrackpot.kintsugi.cga.d2.transforms.Dilator
import fromcrackpot.kintsugi.cga.d2.transforms.Flector
import fromcrackpot.kintsugi.cga.d2.transforms.Motor

/**
 * The collapse of a would-be [Dipole] into a single flat point. There is no dipole to build.
 *
 * - Thrown by the [Dipole] constructor when `gx`, `gy`, and `gz` all round to zero at the `SCALE` quantum.
 */
public class DegenerateDipoleException(
    /** The flat point the failed blade amounts to. */
    public val point: FlatPoint,
) : Exception()

/**
 * The rejection of six components that form no dipole. They are a bivector, but not the wedge of
 * two points.
 *
 * - Thrown by the [Dipole] constructor when `gx·px + gy·py + gz·pz` doesn't round to zero once unitized.
 */
public class NonBladeDipoleException(
    /** The raw constraint residual `gx·px + gy·py + gz·pz` on the unitized components. */
    public val residual: Double,
) : Exception()

/**
 * The collapse of a would-be [Circle] into a line. Collinear geometry leaves no circle to build.
 *
 * - Thrown by the [Circle] constructor when the round weight `cw` rounds to zero at the `SCALE` quantum.
 */
public class DegenerateCircleException(
    /** The line the failed circle flattens onto. */
    public val line: Line,
) : Exception()

/**
 * The rejection of a vertex list that closes no polygon.
 *
 * - Thrown for < 3 vertices, a vertex at infinity, or consecutive vertices rounding equal, last→first included.
 * - Also thrown by [Polygon.Rectangle] for an at-infinity corner or an extent rounding to zero.
 */
public class DegeneratePolygonException(
    /** The human-readable diagnostic naming the offending vertex or count. */
    public val reason: String,
) : Exception()

/**
 * The rejection of a vertex list that chains into no polyline.
 *
 * - Thrown for < 2 vertices, a vertex at infinity, or consecutive vertices rounding equal.
 * - The last and first vertices are not compared.
 */
public class DegeneratePolylineException(
    /** The human-readable diagnostic naming the offending vertex or count. */
    public val reason: String,
) : Exception()

/**
 * The rejection of components that make no rigid motion. The would-be [Motor] has nothing to
 * unitize by.
 *
 * - Thrown by the [Motor] constructor when `√(mz² + mw²)` rounds to zero at the `SCALE` quantum.
 */
public class DegenerateMotorException(
    /** The raw `e41` component, for diagnostics. */
    public val mx: Double,
    /** The raw `e42` component, for diagnostics. */
    public val my: Double,
) : Exception()

/**
 * The rejection of components that make no mirror motion. The would-be [Flector]'s mirror line
 * lies at infinity.
 *
 * - Thrown by the [Flector] constructor when `√(fx² + fy²)` rounds to zero at the `SCALE` quantum.
 */
public class DegenerateFlectorException(
    /** The raw `e412` (line moment) component, for diagnostics. */
    public val fz: Double,
    /** The raw `e4` (glide) component, for diagnostics. */
    public val fw: Double,
) : Exception()

/**
 * The rejection of components that make no dilation. The would-be [Dilator] has nothing to
 * normalize by, or a scale that is not positive.
 *
 * - Thrown by the [Dilator] constructor when `dw + dz` rounds to zero at the `SCALE` quantum.
 * - Thrown by the [Dilator] constructor and by [D] when the scale rounds to zero or below.
 */
public class DegenerateDilatorException(
    /** The raw `e12` component, for diagnostics. */
    public val dz: Double,
    /** The raw `𝟙` component, for diagnostics. */
    public val dw: Double,
) : Exception()
