package fromcrackpot.kintsugi.svg

/**
 * Inline SVG strings used by the test files. Living here (not on disk under resources/)
 * keeps tests pure-commonMain without the JVM/wasmJs resource-access discrepancy.
 *
 * Kintsugi /v1 format: flat points are `<rect k:type="flat-point">` (square, side = render size),
 * round points are `<circle k:type="round-point">` (`r` = render size / 2), circles are plain
 * `<circle>`, and unbounded lines are `<line k:type="line">`. Point fixtures use render size 8
 * (matching `ShapeStyle.POINT.size`) so parsed `ShapeStyle.size` lands on the default.
 */

internal const val EMPTY_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100"/>
"""

/** Single FlatPoint centered at SVG (10, 10), render size 8 → rect (6,6,8,8). viewBox 100x100 → CGA (-40, 40). */
internal const val ONE_FLAT_POINT_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <rect k:type="flat-point" x="6" y="6" width="8" height="8"/>
</svg>
"""

/** Single RoundPoint at SVG (50, 50) (= CGA origin), render size 8 → r = 4. */
internal const val ONE_ROUND_POINT_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <circle k:type="round-point" cx="50" cy="50" r="4"/>
</svg>
"""

/** A flat-point rect whose side is 12 (non-default) — proves render size rides in the geometry. */
internal const val FLAT_POINT_SIZE12_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <rect k:type="flat-point" x="44" y="44" width="12" height="12"/>
</svg>
"""

/** Non-null round point at CGA (10, 5) with k:aw=67 (radius² = 9). viewBox 100x100 → SVG (60, 45). */
internal const val NON_NULL_ROUND_POINT_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <circle k:type="round-point" cx="60" cy="45" r="4" k:aw="67"/>
</svg>
"""

/** Imaginary round point at CGA (10, 5) with k:aw=60.5 (radius² = -4). */
internal const val IMAGINARY_ROUND_POINT_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <circle k:type="round-point" cx="60" cy="45" r="4" k:aw="60.5"/>
</svg>
"""

internal const val MIXED_POINTS_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 200 100">
  <rect k:type="flat-point" x="-4" y="-4" width="8" height="8"/>
  <rect k:type="flat-point" x="46" y="46" width="8" height="8"/>
  <circle k:type="round-point" cx="100" cy="50" r="4"/>
  <circle k:type="round-point" cx="200" cy="100" r="4"/>
</svg>
"""

/** viewBox with non-zero origin and comma separators. */
internal const val OFFSET_VIEWBOX_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="-10, -20, 40, 60">
  <rect k:type="flat-point" x="-4" y="-4" width="8" height="8"/>
</svg>
"""

/** Falls back to width/height when viewBox absent. */
internal const val NO_VIEWBOX_FALLBACK_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" width="50" height="50">
  <rect k:type="flat-point" x="21" y="21" width="8" height="8"/>
</svg>
"""

internal const val NO_VIEWBOX_NO_DIMENSIONS_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1">
  <rect k:type="flat-point" x="0" y="0" width="8" height="8"/>
</svg>
"""

/** A bare `<rect>` (no k:type) — the untagged default: a Rectangle. */
internal const val BARE_RECT_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <rect x="0" y="0" width="20" height="10"/>
</svg>
"""

/** A flat-point rect that is not square — malformed (a point has a single render size). */
internal const val NON_SQUARE_FLAT_POINT_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <rect k:type="flat-point" x="0" y="0" width="20" height="10"/>
</svg>
"""

/** Single Circle centered in a 100x100 viewBox, radius 10. CGA: center (0, 0), radius 10. */
internal const val ONE_CIRCLE_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <circle cx="50" cy="50" r="10"/>
</svg>
"""

/** Circle with fill, stroke, and stroke-width. */
internal const val CIRCLE_WITH_STYLE_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <circle cx="50" cy="50" r="10" fill="#ff0000" stroke="#0000ff" stroke-width="2"/>
</svg>
"""

/** Imaginary circle (radius² < 0) carried via k:flags. */
internal const val IMAGINARY_CIRCLE_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <circle cx="50" cy="50" r="10" k:flags="imaginary"/>
</svg>
"""

/** Inside-out circle (cw > 0) carried via k:flags. */
internal const val INSIDE_OUT_CIRCLE_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <circle cx="50" cy="50" r="10" k:flags="inside-out"/>
</svg>
"""

/** Both flags, with extra whitespace and an unknown token to exercise tolerant parsing. */
internal const val CIRCLE_FLAGS_MESSY_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <circle cx="50" cy="50" r="10" k:flags="  imaginary   bogus inside-out "/>
</svg>
"""

/** Unknown k:type on a circle — warns and falls back to a circle. */
internal const val CIRCLE_UNKNOWN_KTYPE_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <circle k:type="hyperbola" cx="50" cy="50" r="10"/>
</svg>
"""

internal const val CIRCLE_MISSING_R_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <circle cx="50" cy="50"/>
</svg>
"""

internal const val CIRCLE_ZERO_R_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <circle cx="50" cy="50" r="0"/>
</svg>
"""

/** Bare `<g>` containing a single flat-point — the GroupNode carrier (no `k:type` on the `<g>`). */
internal const val NESTED_GROUP_ONE_FLAT_POINT_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <g>
    <rect k:type="flat-point" x="0" y="0" width="8" height="8"/>
  </g>
</svg>
"""

/** Empty `<g></g>` — GroupNode with no children. */
internal const val EMPTY_GROUP_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <g></g>
</svg>
"""

/** Two levels of nesting — `<g><g><circle .../></g></g>`. */
internal const val NESTED_GROUPS_DEEP_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <g>
    <g>
      <circle cx="50" cy="50" r="10"/>
    </g>
  </g>
</svg>
"""

/** Two sibling groups, each holding one shape. */
internal const val MULTIPLE_GROUPS_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <g>
    <circle cx="30" cy="50" r="5"/>
  </g>
  <g>
    <line x1="40" y1="40" x2="60" y2="60"/>
  </g>
</svg>
"""

/** `<g>` with an unknown `k:type` — warns and falls back to a plain GroupNode. */
internal const val GROUP_UNKNOWN_KTYPE_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <g k:type="quadrupole">
    <circle k:type="round-point" cx="50" cy="50" r="4"/>
  </g>
</svg>
"""

/** Group-level `fill="#ff0000"` falls back to the bare child circle. */
internal const val GROUP_FILL_CASCADE_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <g fill="#ff0000">
    <circle cx="50" cy="50" r="10"/>
  </g>
</svg>
"""

/** Group-level fill overridden by own-attr on the child. */
internal const val GROUP_FILL_OVERRIDDEN_BY_CHILD_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <g fill="#ff0000">
    <circle cx="50" cy="50" r="10" fill="#00ff00"/>
  </g>
</svg>
"""

/** Group stroke/stroke-width cascading to a bare tagged line. */
internal const val GROUP_STROKE_CASCADE_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <g stroke="#0000ff" stroke-width="3">
    <line k:type="line" x1="0" y1="0" x2="100" y2="100"/>
  </g>
</svg>
"""

/** Group fill cascade reaches across two levels of nesting. */
internal const val NESTED_GROUP_FILL_CASCADE_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <g fill="#ff0000">
    <g>
      <circle cx="50" cy="50" r="10"/>
    </g>
  </g>
</svg>
"""

/** Inner `<g fill="#00ff00">` overrides outer `<g fill="#ff0000">` for its children. */
internal const val NESTED_GROUP_FILL_OVERRIDE_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <g fill="#ff0000">
    <g fill="#00ff00">
      <circle cx="50" cy="50" r="10"/>
    </g>
  </g>
</svg>
"""

/** Designer-authored Inkscape-style attrs on a `<g>` are silently dropped on parse. */
internal const val GROUP_INKSCAPE_ATTRS_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <g id="layer1" inkscape:label="Main" inkscape:groupmode="layer" xmlns:inkscape="http://www.inkscape.org/namespaces/inkscape">
    <circle cx="50" cy="50" r="10"/>
  </g>
</svg>
"""

internal const val PATH_UNSUPPORTED_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <path d="M 0 0 L 50 50"/>
</svg>
"""

internal const val MALFORMED_VIEWBOX_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100">
  <rect k:type="flat-point" x="0" y="0" width="8" height="8"/>
</svg>
"""

internal const val WITH_COMMENTS_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <!-- a comment between shapes -->
  <rect k:type="flat-point" x="6" y="6" width="8" height="8"/>
  <!-- another -->
  <circle k:type="round-point" cx="50" cy="50" r="4"/>
</svg>
"""

/* LINE FIXTURES — viewBox 100x100, worldBorder is the circle through the four corners. */

/** Tagged unbounded line (corner to corner). */
internal const val LINE_DIAGONAL_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <line k:type="line" x1="0" y1="0" x2="100" y2="100"/>
</svg>
"""

internal const val LINE_DIAGONAL_STYLED_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <line k:type="line" x1="0" y1="0" x2="100" y2="100" stroke="#ff0000" stroke-width="2"/>
</svg>
"""

/** Mirror of [LINE_DIAGONAL_SVG] with (x1,y1) ↔ (x2,y2) swapped — the sign-flipped orientation. */
internal const val LINE_DIAGONAL_SWAPPED_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <line k:type="line" x1="100" y1="100" x2="0" y2="0"/>
</svg>
"""

/** A tagged line whose endpoints are NOT on the worldBorder — still a Line in /v1 (recognized by k:type). */
internal const val LINE_TAGGED_OFFBORDER_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <line k:type="line" x1="20" y1="20" x2="80" y2="80"/>
</svg>
"""

/** Bare `<line>` (no k:type) — the untagged default for segments. */
internal const val LINE_SEGMENT_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <line x1="20" y1="20" x2="80" y2="80"/>
</svg>
"""

/** Bare `<line>` (no k:type) with explicit stroke/stroke-width — styled segment. */
internal const val LINE_SEGMENT_STYLED_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <line x1="20" y1="20" x2="80" y2="80" stroke="#ff0000" stroke-width="2"/>
</svg>
"""

/** Unknown k:type on a line — warns and falls back to a segment. */
internal const val LINE_UNKNOWN_KTYPE_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <line k:type="ray" x1="0" y1="0" x2="100" y2="100"/>
</svg>
"""

/** Bare segment with mirrored endpoints — sign-flipped orientation twin of [LINE_SEGMENT_SVG]. */
internal const val LINE_SEGMENT_SWAPPED_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <line x1="80" y1="80" x2="20" y2="20"/>
</svg>
"""

/** Bare `<line>` (segment) with a round stroke-linecap — the attribute rides after stroke-width. */
internal const val LINE_SEGMENT_ROUNDCAP_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <line x1="20" y1="20" x2="80" y2="80" stroke="#ff0000" stroke-width="2" stroke-linecap="round"/>
</svg>
"""

/** Tagged unbounded line with a square stroke-linecap. */
internal const val LINE_DIAGONAL_SQUARECAP_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <line k:type="line" x1="0" y1="0" x2="100" y2="100" stroke="#ff0000" stroke-width="2" stroke-linecap="square"/>
</svg>
"""

/** `<g stroke-linecap="round">` cascades the cap onto a bare segment lacking its own. */
internal const val GROUP_STROKE_LINECAP_CASCADE_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <g stroke-linecap="round">
    <line x1="20" y1="20" x2="80" y2="80" stroke="#ff0000" stroke-width="2"/>
  </g>
</svg>
"""

/**
 * A `<line>` with one coordinate attribute dropped — the malformed-attribute matrix for
 * [LineTest]/[SegmentTest]. [kType] = `"line"` builds the tagged infinite-line form; `null`
 * builds the bare segment form.
 */
internal fun lineSvgWithout(missing: String, kType: String?): String {
    val attrs = listOf("x1" to "0", "y1" to "0", "x2" to "100", "y2" to "100")
        .filter { it.first != missing }
        .joinToString(" ") { (name, value) -> """$name="$value"""" }
    val tag = if (kType != null) """k:type="$kType" $attrs""" else attrs
    return """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <line $tag/>
</svg>
"""
}

/** Bare segment with coincident endpoints — collapses underlying Dipole's carrier norm to zero. */
internal const val LINE_SEGMENT_COINCIDENT_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <line x1="50" y1="50" x2="50" y2="50"/>
</svg>
"""

internal const val LINE_COINCIDENT_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <line k:type="line" x1="0" y1="0" x2="0" y2="0"/>
</svg>
"""

/* COLOR FIXTURES — flat-point rects centered at SVG (50,50) = CGA origin, render size 8. */

internal const val FILL_HEX6_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <rect k:type="flat-point" x="46" y="46" width="8" height="8" fill="#ff0000"/>
</svg>
"""

internal const val FILL_HEX3_SHORTHAND_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <rect k:type="flat-point" x="46" y="46" width="8" height="8" fill="#F00"/>
</svg>
"""

internal const val FILL_HEX8_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <rect k:type="flat-point" x="46" y="46" width="8" height="8" fill="#11223344"/>
</svg>
"""

/** Zero-alpha color — should canonicalize to ShapeColor.NONE on parse and "none" on serialize. */
internal const val FILL_HEX8_ZERO_ALPHA_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <rect k:type="flat-point" x="46" y="46" width="8" height="8" fill="#12345600"/>
</svg>
"""

internal const val FILL_RGB_FUNCTIONAL_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <rect k:type="flat-point" x="46" y="46" width="8" height="8" fill="rgb(0, 128, 255)"/>
</svg>
"""

internal const val FILL_RGBA_FUNCTIONAL_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <rect k:type="flat-point" x="46" y="46" width="8" height="8" fill="rgba(255, 0, 0, 0.5)"/>
</svg>
"""

internal const val FILL_NAMED_RED_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <rect k:type="flat-point" x="46" y="46" width="8" height="8" fill="red"/>
</svg>
"""

internal const val FILL_NONE_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <rect k:type="flat-point" x="46" y="46" width="8" height="8" fill="none"/>
</svg>
"""

internal const val FILL_CURRENT_COLOR_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <rect k:type="flat-point" x="46" y="46" width="8" height="8" fill="currentColor"/>
</svg>
"""

/** Round-point circle appears before flat-point rect in source order. */
internal const val ROUND_BEFORE_FLAT_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <circle k:type="round-point" cx="50" cy="50" r="4"/>
  <rect k:type="flat-point" x="6" y="6" width="8" height="8"/>
</svg>
"""

/* FULL-BLEED RECT FIXTURES — no background concept: a full-bleed rect is an ordinary Rectangle */

/** Leading full-bleed fill-only rect with the same dims as the viewBox — an ordinary Rectangle (backdrop by z-order). */
internal const val BACKGROUND_BLACK_THEN_POINT_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <rect x="0" y="0" width="100" height="100" fill="#000000"/>
  <rect k:type="flat-point" x="46" y="46" width="8" height="8"/>
</svg>
"""

/** Leading rect dims match an offset viewBox exactly. */
internal const val BACKGROUND_OFFSET_VIEWBOX_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="-10 -20 40 60">
  <rect x="-10" y="-20" width="40" height="60" fill="#123456"/>
</svg>
"""

/** Full-bleed rect with `rx`/`ry` — the untagged Rectangle default applies; the codec rejects rounded corners — malformed. */
internal const val LEADING_RECT_FULL_BLEED_WITH_RX_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <rect x="0" y="0" width="100" height="100" rx="0.5" ry="0.5" fill="#000000"/>
</svg>
"""

/** Full-bleed rect with no fill — a Rectangle with the SVG-default fill (black). */
internal const val LEADING_FULL_BLEED_RECT_NO_FILL_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <rect x="0" y="0" width="100" height="100"/>
</svg>
"""

/** Full-bleed rect carrying a stroke — a stroked Rectangle. */
internal const val LEADING_FULL_BLEED_RECT_WITH_STROKE_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <rect x="0" y="0" width="100" height="100" fill="#000000" stroke="#ffffff"/>
</svg>
"""

/** Tagged round-point `<circle>` missing the required `r` attribute. */
internal const val ROUND_POINT_MISSING_R_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <circle k:type="round-point" cx="50" cy="50"/>
</svg>
"""

/* ROOT-CASCADE FIXTURES — presentation attrs on <svg> fall back to each child whose own attr is absent. */

/** Root fill applies to a bare circle (no own fill). */
internal const val ROOT_FILL_BARE_CIRCLE_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100" fill="#ff0000">
  <circle cx="50" cy="50" r="10"/>
</svg>
"""

/** Root fill applies to a flat-point rect (no own fill). */
internal const val ROOT_FILL_FLAT_POINT_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100" fill="#ff0000">
  <rect k:type="flat-point" x="46" y="46" width="8" height="8"/>
</svg>
"""

/** Root fill applies to a round-point circle (no own fill). */
internal const val ROOT_FILL_ROUND_POINT_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100" fill="#ff0000">
  <circle k:type="round-point" cx="50" cy="50" r="4"/>
</svg>
"""

/** Root stroke/stroke-width applies to a tagged line (no own stroke attrs). */
internal const val ROOT_STROKE_LINE_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100" stroke="#0000ff" stroke-width="3">
  <line k:type="line" x1="0" y1="0" x2="100" y2="100"/>
</svg>
"""

/** Root stroke/stroke-width applies to a bare circle (no own stroke attrs). */
internal const val ROOT_STROKE_CIRCLE_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100" stroke="#0000ff" stroke-width="3">
  <circle cx="50" cy="50" r="10"/>
</svg>
"""

/** Per-shape fill overrides root fill. */
internal const val ROOT_FILL_SHAPE_OVERRIDE_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100" fill="#ff0000">
  <circle cx="50" cy="50" r="10" fill="#00ff00"/>
</svg>
"""

/** Root fill is set and a leading bare full-bleed rect has no fill of its own — a Rectangle whose fill cascades from the root. */
internal const val ROOT_FILL_LEADING_BARE_RECT_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100" fill="#ff0000">
  <rect x="0" y="0" width="100" height="100"/>
</svg>
"""

/* DIPOLE FIXTURES — viewBox 100x100, SVG (50,50) = CGA origin, render size 8 → r=4 on each child. */

/** Horizontal dipole through SVG (40,50) and (60,50) = CGA (-10,0) and (10,0); with dashed chrome. */
internal const val ONE_DIPOLE_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <g k:type="dipole">
    <circle k:type="round-point" cx="40" cy="50" r="4"/>
    <circle k:type="round-point" cx="60" cy="50" r="4"/>
    <line x1="40" y1="50" x2="60" y2="50" stroke-dasharray="2 1"/>
  </g>
</svg>
"""

/** Same dipole but the chrome `<line>` is omitted — designer-authored case must still parse. */
internal const val DIPOLE_NO_CHROME_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <g k:type="dipole">
    <circle k:type="round-point" cx="40" cy="50" r="4"/>
    <circle k:type="round-point" cx="60" cy="50" r="4"/>
  </g>
</svg>
"""

/** Child order swapped — orientation contract says this is the sign-flipped dipole. */
internal const val DIPOLE_REVERSED_ORDER_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <g k:type="dipole">
    <circle k:type="round-point" cx="60" cy="50" r="4"/>
    <circle k:type="round-point" cx="40" cy="50" r="4"/>
  </g>
</svg>
"""

/** Dipole with only one round-point child — malformed. */
internal const val DIPOLE_ONE_CHILD_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <g k:type="dipole">
    <circle k:type="round-point" cx="40" cy="50" r="4"/>
  </g>
</svg>
"""

/** Dipole with three round-point children — malformed (more than two). */
internal const val DIPOLE_THREE_CHILDREN_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <g k:type="dipole">
    <circle k:type="round-point" cx="40" cy="50" r="4"/>
    <circle k:type="round-point" cx="60" cy="50" r="4"/>
    <circle k:type="round-point" cx="50" cy="60" r="4"/>
  </g>
</svg>
"""

/** Dipole containing a non-circle, non-line child — malformed. */
internal const val DIPOLE_REJECT_RECT_CHILD_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <g k:type="dipole">
    <circle k:type="round-point" cx="40" cy="50" r="4"/>
    <rect k:type="flat-point" x="56" y="46" width="8" height="8"/>
  </g>
</svg>
"""

/** Dipole with a child <circle> that isn't tagged round-point — malformed. */
internal const val DIPOLE_NON_ROUND_POINT_CHILD_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <g k:type="dipole">
    <circle cx="40" cy="50" r="4"/>
    <circle k:type="round-point" cx="60" cy="50" r="4"/>
  </g>
</svg>
"""

/** Coincident round-point children → DegenerateDipoleException → MalformedDipoleException. */
internal const val DIPOLE_COINCIDENT_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <g k:type="dipole">
    <circle k:type="round-point" cx="50" cy="50" r="4"/>
    <circle k:type="round-point" cx="50" cy="50" r="4"/>
  </g>
</svg>
"""

/** <g> with an unknown k:type — UnsupportedSvgElementException (only k:type="dipole" is recognized). */
internal const val DIPOLE_UNKNOWN_GROUP_KTYPE_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <g k:type="quadrupole">
    <circle k:type="round-point" cx="40" cy="50" r="4"/>
    <circle k:type="round-point" cx="60" cy="50" r="4"/>
  </g>
</svg>
"""

/** Dipole carrier with its own `fill` — cascades into the round-point children (same posture as bare `<g fill=...>`). */
internal const val DIPOLE_GROUP_FILL_CASCADE_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <g k:type="dipole" fill="#ff0000">
    <circle k:type="round-point" cx="40" cy="50" r="4"/>
    <circle k:type="round-point" cx="60" cy="50" r="4"/>
  </g>
</svg>
"""

/** Full-bleed rect with explicit `stroke="none"` — a strokeless Rectangle (`none` canonicalizes to ShapeColor.NONE). */
internal const val LEADING_FULL_BLEED_RECT_STROKE_NONE_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <rect x="0" y="0" width="100" height="100" fill="#000000" stroke="none"/>
</svg>
"""

/** Untagged `<rect>` inside a nested `<g>` — a Rectangle, same as at root. */
internal const val NESTED_BARE_RECT_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <g>
    <rect x="0" y="0" width="20" height="10"/>
  </g>
</svg>
"""

/* RECTANGLE FIXTURES — viewBox 100x100; svgToCga(x, y) = (x − 50, 50 − y); CGA anchor = bottom-left. */

/** Styled rectangle: SVG top-left (20, 30), 40×20 → CGA anchor (−30, 0), width 40, height 20. */
internal const val ONE_RECTANGLE_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <rect x="20" y="30" width="40" height="20" fill="#ff0000" stroke="#0000ff" stroke-width="2"/>
</svg>
"""

/** Rectangle with an unknown k:type — warns and falls back to the untagged Rectangle default. */
internal const val RECTANGLE_UNKNOWN_KTYPE_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <rect k:type="squircle" x="20" y="30" width="40" height="20"/>
</svg>
"""

/** Rectangle whose fill cascades from the enclosing `<g>`. */
internal const val GROUP_FILL_CASCADE_RECT_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <g fill="#00ff00">
    <rect x="20" y="30" width="40" height="20"/>
  </g>
</svg>
"""

/** Builds a rect fixture with one required geometry attribute omitted — MalformedRectException per attr. */
internal fun rectSvgWithout(missing: String): String {
    val attrs = listOf("x" to "20", "y" to "30", "width" to "40", "height" to "20")
        .filter { (name, _) -> name != missing }
        .joinToString(" ") { (name, value) -> "$name=\"$value\"" }
    return """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <rect $attrs/>
</svg>
"""
}

/** Non-full-bleed rect with rounded corners (rx only) — unsupported geometry, malformed. */
internal const val RECTANGLE_RX_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <rect x="20" y="30" width="40" height="20" rx="3"/>
</svg>
"""

/** Non-full-bleed rect with rounded corners (ry only) — unsupported geometry, malformed. */
internal const val RECTANGLE_RY_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <rect x="20" y="30" width="40" height="20" ry="3"/>
</svg>
"""

/** Rect with rx="0" — zero rounding is semantically square corners; parses as a Rectangle. */
internal const val RECTANGLE_ZERO_RX_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <rect x="20" y="30" width="40" height="20" rx="0"/>
</svg>
"""

/** Zero width — Degenerate→Malformed at the codec boundary. */
internal const val RECTANGLE_ZERO_WIDTH_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <rect x="20" y="30" width="0" height="20"/>
</svg>
"""

/** Negative height — Degenerate→Malformed at the codec boundary (extents are not re-anchored). */
internal const val RECTANGLE_NEGATIVE_HEIGHT_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <rect x="20" y="30" width="40" height="-20"/>
</svg>
"""

/** Full-bleed fill-only rect that is not leading (a shape precedes it) — a Rectangle; position carries no special meaning. */
internal const val NON_LEADING_FULL_BLEED_RECT_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <circle cx="50" cy="50" r="10"/>
  <rect x="0" y="0" width="100" height="100" fill="#000000"/>
</svg>
"""

/** Two full-bleed fill-only rects — two stacked Rectangles, in source order. */
internal const val TWO_FULL_BLEED_RECTS_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <rect x="0" y="0" width="100" height="100" fill="#000000"/>
  <rect x="0" y="0" width="100" height="100" fill="#ff0000"/>
</svg>
"""

/** Bare triangle polygon. viewBox 100×100: SVG (20,80)/(60,80)/(20,50) → CGA (−30,−30)/(10,−30)/(−30,0). */
internal const val ONE_POLYGON_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <polygon points="20,80 60,80 20,50" fill="#ff0000" stroke="#0000ff" stroke-width="2"/>
</svg>
"""

/** Same triangle, whitespace-only separators and no styling — SVG defaults materialize. */
internal const val BARE_POLYGON_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <polygon points="20 80 60 80 20 50"/>
</svg>
"""

/** Polygon with an unknown k:type — warns and falls back to a polygon. */
internal const val POLYGON_UNKNOWN_KTYPE_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <polygon k:type="mystery" points="20,80 60,80 20,50"/>
</svg>
"""

/** Polygon missing the required points attribute. */
internal const val POLYGON_NO_POINTS_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <polygon fill="#ff0000"/>
</svg>
"""

/** Polygon with an odd coordinate count. */
internal const val POLYGON_ODD_POINTS_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <polygon points="20,80 60,80 20"/>
</svg>
"""

/** Polygon with only two vertices — degenerate in :cga, malformed at the parse boundary. */
internal const val POLYGON_TWO_VERTICES_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <polygon points="20,80 60,80"/>
</svg>
"""

/** Polygon with a non-numeric coordinate. */
internal const val POLYGON_NON_NUMERIC_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <polygon points="20,80 60,oops 20,50"/>
</svg>
"""

/** Inside-out triangle: flag and model winding agree (the walk maps model-clockwise) — the canonical serialized form. */
internal const val INSIDE_OUT_POLYGON_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <polygon points="20,50 60,80 20,80" k:flags="inside-out"/>
</svg>
"""

/** Inside-out flag on a walk that maps model-counter-clockwise — the parser reverses it (flag is intent). */
internal const val MISWOUND_INSIDE_OUT_POLYGON_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <polygon points="20,80 60,80 20,50" k:flags="inside-out"/>
</svg>
"""

/** No flag on a walk that maps model-clockwise — the parser reverses it to classic. */
internal const val MISWOUND_CLASSIC_POLYGON_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <polygon points="20,50 60,80 20,80"/>
</svg>
"""

/** Inside-out rectangle via `<rect>` + flag: the factory quad, reversed to the wall reading. */
internal const val INSIDE_OUT_RECT_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <rect x="30" y="40" width="40" height="20" k:flags="inside-out"/>
</svg>
"""

/* POLYLINE FIXTURES — bare <polyline> is the untagged (and only) reading: an open Polyline chain */

/** Bare open-chain polyline. viewBox 100×100: SVG (20,80)/(60,80)/(20,50) → CGA (−30,−30)/(10,−30)/(−30,0). */
internal const val ONE_POLYLINE_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <polyline points="20,80 60,80 20,50" fill="none" stroke="#0000ff" stroke-width="2"/>
</svg>
"""

/** Same chain, whitespace-only separators and no styling — SVG defaults materialize. */
internal const val BARE_POLYLINE_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <polyline points="20 80 60 80 20 50"/>
</svg>
"""

/** Polyline with an unknown k:type — warns and falls back to a polyline. */
internal const val POLYLINE_UNKNOWN_KTYPE_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <polyline k:type="mystery" points="20,80 60,80 20,50"/>
</svg>
"""

/** Two-vertex polyline — a single open edge, legal (unlike polygon's 3-vertex floor). */
internal const val POLYLINE_TWO_VERTICES_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <polyline points="20,80 60,80"/>
</svg>
"""

/** Closed-spelling polyline (first == last) — legal for the open chain, preserved as data. */
internal const val POLYLINE_CLOSED_SPELLING_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <polyline points="20,80 60,80 20,50 20,80"/>
</svg>
"""

/** Polyline with a non-butt linecap — the optional-feature attribute round-trips. */
internal const val POLYLINE_ROUND_CAP_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <polyline points="20,80 60,80 20,50" stroke="#0000ff" stroke-width="4" stroke-linecap="round"/>
</svg>
"""

/** Polyline missing the required points attribute. */
internal const val POLYLINE_NO_POINTS_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <polyline stroke="#ff0000"/>
</svg>
"""

/** Polyline with an odd coordinate count. */
internal const val POLYLINE_ODD_POINTS_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <polyline points="20,80 60,80 20"/>
</svg>
"""

/** Polyline with only one vertex — degenerate in :cga, malformed at the parse boundary. */
internal const val POLYLINE_ONE_VERTEX_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <polyline points="20,80"/>
</svg>
"""

/** Polyline with a non-numeric coordinate. */
internal const val POLYLINE_NON_NUMERIC_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <polyline points="20,80 60,oops 20,50"/>
</svg>
"""

/* K:TAGS — open-vocabulary game-semantic tokens */

/** Polygon carrying k:tags; whitespace-messy value exercises tolerant token splitting. */
internal const val TAGGED_POLYGON_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <polygon points="20,80 60,80 20,50" k:tags="  brick   Steel "/>
</svg>
"""

/** Group-level k:tags union into leaves (style-cascade bake idiom); leaf adds its own token. */
internal const val TAGGED_GROUP_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <g k:tags="brick">
    <polygon points="20,80 60,80 20,50" k:tags="steel"/>
    <circle cx="50" cy="50" r="3"/>
  </g>
</svg>
"""

/** Dipole carrier <g> holds the tags; k:tags on its round-point children is ignored. */
internal const val TAGGED_DIPOLE_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <g k:type="dipole" k:tags="portal">
    <circle k:type="round-point" cx="40" cy="50" r="4" k:tags="ignored"/>
    <circle k:type="round-point" cx="60" cy="50" r="4"/>
  </g>
</svg>
"""

/* <USE> TEMPLATE EXPANSION */

/** Brick template, 40×20: explicit width/height = viewBox extents; fill left unset (inherits from use site). */
internal const val BRICK_TEMPLATE_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" width="40" height="20" viewBox="0 0 40 20">
  <rect x="0" y="0" width="40" height="20" k:tags="brick"/>
</svg>
"""

/** Template whose shape sets an explicit fill — must win over any use-site fill (SVG cascade rule). */
internal const val GREEN_TEMPLATE_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" width="40" height="20" viewBox="0 0 40 20">
  <rect x="0" y="0" width="40" height="20" fill="#00ff00"/>
</svg>
"""

/** Transitive template: contains a <use> of gem.svg centered in its own space. */
internal const val COMPOSITE_TEMPLATE_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" width="40" height="20" viewBox="0 0 40 20">
  <use href="gem.svg" x="10" y="5"/>
</svg>
"""

/** Leaf of the transitive chain, 20×10. */
internal const val GEM_TEMPLATE_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" width="20" height="10" viewBox="0 0 20 10">
  <rect x="0" y="0" width="20" height="10" k:tags="gem"/>
</svg>
"""

/** Template missing the explicit width/height contract (viewBox only). */
internal const val SIZELESS_TEMPLATE_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 40 20">
  <rect x="0" y="0" width="40" height="20"/>
</svg>
"""

/** Template whose width contradicts its viewBox extents. */
internal const val MISMATCHED_TEMPLATE_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" width="30" height="20" viewBox="0 0 40 20">
  <rect x="0" y="0" width="40" height="20"/>
</svg>
"""

/** Two-template cycle: a → b → a. */
internal const val CYCLE_A_TEMPLATE_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" width="40" height="20" viewBox="0 0 40 20">
  <use href="cycle-b.svg"/>
</svg>
"""

internal const val CYCLE_B_TEMPLATE_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" width="40" height="20" viewBox="0 0 40 20">
  <use href="cycle-a.svg"/>
</svg>
"""

/**
 * Layout composing two bricks from one template. First use restyles (fill) and tags (steel) its
 * instance; second carries a #fragment (tolerated, ignored) and no overrides.
 */
internal const val WALL_LAYOUT_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 200 100">
  <use href="steel.svg" x="10" y="10" fill="#888888" k:tags="steel"/>
  <use href="steel.svg#brick" x="60" y="10"/>
</svg>
"""

/** Layout exercising the transitive chain: composite.svg → gem.svg. */
internal const val TRANSITIVE_LAYOUT_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 200 100">
  <use href="composite.svg" x="60" y="10"/>
</svg>
"""

/** Minimal layouts for the malformed-use cases. */
internal const val USE_NO_HREF_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <use x="10" y="10"/>
</svg>
"""

internal const val USE_FRAGMENT_ONLY_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <use href="#brick" x="10" y="10"/>
</svg>
"""

internal const val USE_WITH_SIZE_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <use href="steel.svg" x="10" y="10" width="40" height="20"/>
</svg>
"""

internal const val USE_WITH_CHILD_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <use href="steel.svg"><circle cx="1" cy="1" r="1"/></use>
</svg>
"""

/** Layout referencing a cycle entry point. */
internal const val CYCLE_LAYOUT_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="0 0 100 100">
  <use href="cycle-a.svg"/>
</svg>
"""
