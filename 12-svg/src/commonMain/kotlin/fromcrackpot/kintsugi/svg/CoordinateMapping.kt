package fromcrackpot.kintsugi.svg

import fromcrackpot.kintsugi.scenegraph.SceneBounds

/*
 * CGA model space ↔ SVG user space. Model space is centered and Y-up (the right-handed frame of
 * the GA literature); SVG is top-left and Y-down. We keep the two spaces separate on purpose:
 * a Y-down frame is left-handed, silently negating every orientation-carrying quantity (wedge
 * signs, `meet` operand order, circle winding, dipole start/end), and centering keeps conformal
 * weights (`aw ~ ‖p‖²/2`) small where the null-basis machinery is best conditioned. These two
 * functions are `:svg`'s single conversion boundary; 1 CGA unit = 1 SVG user unit — only origin
 * and Y direction differ. The mapping is viewBox-relative: round-trips within a document are
 * exact, but moving shapes between documents requires re-mapping.
 */
internal fun svgToCga(svgX: Double, svgY: Double, vb: SceneBounds): Pair<Double, Double> =
    (svgX - vb.minX - vb.width / 2.0) to (vb.minY + vb.height / 2.0 - svgY)

internal fun cgaToSvg(cgaX: Double, cgaY: Double, vb: SceneBounds): Pair<Double, Double> =
    (cgaX + vb.minX + vb.width / 2.0) to (vb.minY + vb.height / 2.0 - cgaY)
