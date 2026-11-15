package fromcrackpot.kintsugi.cga.d2

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Line

/**
 * The X axis, the line `y = 0` running toward positive X.
 *
 * - Its normal points toward negative Y, so the positive side is the half-plane below the axis.
 */
public val AXIS_X: Line = Line(0.0, -1.0, 0.0)

/**
 * The Y axis, the line `x = 0` running toward positive Y.
 *
 * - Its normal points toward positive X, so the positive side is the half-plane right of the axis.
 */
public val AXIS_Y: Line = Line(1.0, 0.0, 0.0)
