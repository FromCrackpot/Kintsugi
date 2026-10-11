package fromcrackpot.kintsugi.cga.d2

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Line

/**
 * The X axis — the line `y = 0`, running toward positive X.
 *
 * The [Line] `(0, −1, 0)`: its unit normal `(0, −1)` points toward negative Y, so the running
 * direction ([Line.attitude]) is `Direction(1, 0)` and the positive side — where the raw line
 * equation `gx·x + gy·y + gz` is positive — is the half-plane below the axis.
 */
public val AXIS_X: Line = Line(0.0, -1.0, 0.0)

/**
 * The Y axis — the line `x = 0`, running toward positive Y.
 *
 * The [Line] `(1, 0, 0)`: its unit normal `(1, 0)` points toward positive X, so the running
 * direction ([Line.attitude]) is `Direction(0, 1)` and the positive side — where the raw line
 * equation `gx·x + gy·y + gz` is positive — is the half-plane right of the axis.
 */
public val AXIS_Y: Line = Line(1.0, 0.0, 0.0)
