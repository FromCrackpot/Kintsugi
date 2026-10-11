package fromcrackpot.kintsugi.scratchpad.scenes

/**
 * Sample scene exercised by the scratchpad end-to-end:
 *  - parsed into a `GroupNode` for on-screen rendering (see [fromcrackpot.kintsugi.scratchpad.App])
 *  - re-serialized and written to disk on desktop (see desktopMain `main`)
 *
 * Authored in the Kintsugi `/v1` SVG format (renderable carrier + `k:*` sidecar): the `xmlns:k`
 * namespace is declared on the root, points carry a `k:type` discriminator, and infinite lines are
 * tagged `k:type="line"`.
 *
 * The scene doubles as the line-clipping debug scene: the viewBox is `-240 -240 960 960` — the
 * original 480x480 content region plus a 240-unit world margin on every side (offsetting the
 * viewBox origin keeps every element coordinate unchanged; the center stays at (240, 240)) — so
 * the worldBorder circle and the clipped line endpoints are visible on-canvas instead of lying
 * outside the surface. The smiley content occupies the central 480x480 square:
 *  - worldBorder: thin dark blue-grey circle, radius 480*sqrt(2) ≈ 678.823 around (240, 240) —
 *    the viewBox's circumscribed circle, i.e. the locus both renderers clip an infinite CGA
 *    Line's displayed segment against (`line meet worldBorder`); the frame lines visibly
 *    terminate on it. In /v1 a `<line>` is a CGA Line when tagged `k:type="line"` — recognition
 *    does not depend on its endpoints lying on this circle.
 *  - face contour: yellow stroked Circle, radius 100 around (240, 240)
 *  - antennas: two yellow CGA Dipoles (`<g k:type="dipole">`), each lying fully outside the face
 *    with one endpoint tangent to the face contour at the 60° and 120° positions (math convention,
 *    +X axis CCW — so a symmetric V pointing upward from the top of the head); each is radial with
 *    radius 20, outer endpoints at distance 140 from the face center. Hand-authored without `k:aw`
 *    or the dashed-line visual chrome — both gain canonical forms on first save (parser uses the
 *    2-arg conformal lift; serializer adds chrome).
 *  - ears: two yellow W-shaped Polylines (`<polyline>`), one per side at mid-height. Each is the
 *    letter W rotated by ±90° (+90° left, −90° right — the rotation swings the horizontal zigzag
 *    vertical so the three W-tips point outward from the head while the two valleys nestle against
 *    the face contour near the side points (140, 240) / (340, 240)); the pair mirrors across x=240.
 *  - hat: two orange filled rectangle Polygons (bare `<rect>`, no `k:type` — parse-only; they
 *    re-serialize as `<polygon>`) forming a top hat between the
 *    antennas — a brim (96x10 at (192, 138)) across the head top and a crown (52x40 at (214, 98))
 *    flush on it, both symmetric about x=240
 *  - eyes: two rings of 8 RoundPoints (`<circle k:type="round-point">`, render size 8 → r=4) on
 *    rings of radius 10 centered at (205, 205) and (275, 205), green
 *  - smile: 9 FlatPoints (`<rect k:type="flat-point">`, render size 8) on an arc of radius 65
 *    centered at (240, 220), angles 30°..150°, rainbow ROYGBIV — bows down in the middle
 *  - bow tie: 6 cyan Segments (bare `<line>`, no `k:type`) forming two triangular wings under the
 *    face; total width 91.844 (= 1.2 · 100 · sin 45°, the tangent-point span chosen visually);
 *    upper edges tangent to the face contour (which fixes T_right.y = −√(R²−T.x²) and the knot
 *    position K = (0, R²/T_right.y) ≈ (0, −112.5719)); outer edges vertical (B shares x with T)
 *    and lower edges mirror upper across the horizontal line through K
 *  - photo frame: 4 blueviolet CGA Lines (`<line k:type="line">`) along the content-region edges
 *    (x=0/480, y=0/480); being infinite, their displayed segments extend past the black square
 *    and end exactly on the worldBorder — the visual proof of the clipping idiom
 *
 * Antennas, eyes, smile, and bow tie are each wrapped in a semantic bare `<g>` (round-trips as a
 * nested `GroupNode`). Shared `fill`/`stroke`/`stroke-width` are hoisted onto each group and
 * resolved into per-shape concrete `ShapeStyle` at parse time via the presentational fallback chain;
 * on save the serializer re-emits each shape's style explicitly (canonical-output rule) and the
 * group attrs are dropped.
 *
 * The loader Y-flips and re-centers via the viewBox, so SVG (240, 240) lands at CGA origin (0, 0)
 * and matches the canvas's centered Y-up convention. Eyes (SVG y < 240) sit above center in CGA;
 * smile (SVG y > 240) sits below. Each point's render size rides in the element geometry (rect side
 * for flat points, 2·r for round points), so the canvas draws 8px markers.
 */
internal const val SMILEY_SVG = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:k="https://fromcrackpot.com/ns/kintsugi/svg/v1" viewBox="-240 -240 960 960">
  <!-- content backdrop: an ordinary rectangle Polygon, first in z-order, covering the original
       480x480 content region — deliberately smaller than the 960x960 viewBox, so the black
       square marks "the content" against the surrounding world margin. Drop it to render the
       scene transparent. -->
  <rect x="0" y="0" width="480" height="480" fill="#000000"/>

  <!-- worldBorder visualization: the SVG-viewBox-circumscribed circle (radius
       480*sqrt(2) ≈ 678.823 around (240,240)). Plain <circle> = CGA Circle. It's the locus both
       renderers clip k:type="line" shapes against (serializer: worldBorder meet line; canvas:
       bounds-derived worldBorder circle) — drawn here, inside the enlarged viewBox, so the line
       clipping is visually inspectable: the frame lines' segments terminate exactly on it. -->
  <circle cx="240" cy="240" r="678.82250994" fill="none" stroke="#2E3440" stroke-width="1"/>

  <!-- face contour: stroked circle r=100 around (240, 240) -->
  <circle cx="240" cy="240" r="100" fill="none" stroke="#ffd700" stroke-width="2"/>

  <!-- antennas: shared yellow fill hoisted; each Dipole inherits via the presentational fallback chain -->
  <g fill="#ffd700">
    <!-- left antenna: Dipole at 120° (upper-left), inner endpoint tangent on face contour, radius 20 -->
    <g k:type="dipole">
      <circle k:type="round-point" cx="170" cy="118.7564" r="4"/>
      <circle k:type="round-point" cx="190" cy="153.3975" r="4"/>
    </g>

    <!-- right antenna: Dipole at 60° (upper-right), inner endpoint tangent on face contour, radius 20 -->
    <g k:type="dipole">
      <circle k:type="round-point" cx="290" cy="153.3975" r="4"/>
      <circle k:type="round-point" cx="310" cy="118.7564" r="4"/>
    </g>
  </g>

  <!-- ears: two W-shaped Polylines (bare <polyline>), one per side, W rotated +-90 deg so its
       tips point outward from the head; back valleys nestle to the face contour at mid-height.
       Shared yellow stroke hoisted; each Polyline inherits via the presentational fallback chain. -->
  <g fill="none" stroke="#ffd700" stroke-width="2">
    <polyline points="90,260 140,250 110,240 140,230 90,220"/>
    <polyline points="390,220 340,230 370,240 340,250 390,260"/>
  </g>

  <!-- hat: two rectangle Polygon composites (bare <rect>, no k:type) forming a top hat between the
       antennas — brim spanning the head top, crown flush on it; shared orange fill hoisted -->
  <g fill="#ffa500">
    <rect x="192" y="138" width="96" height="10"/>
    <rect x="214" y="98" width="52" height="40"/>
  </g>

  <!-- photo frame: 4 CGA Lines (k:type="line") along the content-region edges (x=0/480,
       y=0/480). Infinite lines, so their displayed segments extend past the black square and
       terminate on the worldBorder circle — the on-canvas demonstration of line clipping. The
       authored endpoints are just two points on each line (k:type="line" recognition doesn't
       require them to lie on the worldBorder); on save the serializer re-projects endpoints
       onto the circle. -->
  <line k:type="line" x1="0" y1="0" x2="0" y2="480" stroke="#8a2be2" stroke-width="10"/>
  <line k:type="line" x1="480" y1="0" x2="480" y2="480" stroke="#8a2be2" stroke-width="10"/>
  <line k:type="line" x1="0" y1="0" x2="480" y2="0" stroke="#8a2be2" stroke-width="10"/>
  <line k:type="line" x1="0" y1="480" x2="480" y2="480" stroke="#8a2be2" stroke-width="10"/>

  <!-- eyes: shared green fill hoisted; each RoundPoint inherits via the presentational fallback chain -->
  <g fill="#00ff00">
    <!-- left eye: 8 RoundPoints, ring r=10 around (205, 205) -->
    <circle k:type="round-point" cx="215" cy="205" r="4"/>
    <circle k:type="round-point" cx="212.07" cy="212.07" r="4"/>
    <circle k:type="round-point" cx="205" cy="215" r="4"/>
    <circle k:type="round-point" cx="197.93" cy="212.07" r="4"/>
    <circle k:type="round-point" cx="195" cy="205" r="4"/>
    <circle k:type="round-point" cx="197.93" cy="197.93" r="4"/>
    <circle k:type="round-point" cx="205" cy="195" r="4"/>
    <circle k:type="round-point" cx="212.07" cy="197.93" r="4"/>

    <!-- right eye: 8 RoundPoints, ring r=10 around (275, 205) -->
    <circle k:type="round-point" cx="285" cy="205" r="4"/>
    <circle k:type="round-point" cx="282.07" cy="212.07" r="4"/>
    <circle k:type="round-point" cx="275" cy="215" r="4"/>
    <circle k:type="round-point" cx="267.93" cy="212.07" r="4"/>
    <circle k:type="round-point" cx="265" cy="205" r="4"/>
    <circle k:type="round-point" cx="267.93" cy="197.93" r="4"/>
    <circle k:type="round-point" cx="275" cy="195" r="4"/>
    <circle k:type="round-point" cx="282.07" cy="197.93" r="4"/>
  </g>

  <!-- smile: 9 FlatPoints on arc r=65 around (240, 220), 30°..150°, ROYGBIV gradient -->
  <g>
    <rect k:type="flat-point" x="292.29" y="248.5" width="8" height="8" fill="#ff0000"/>
    <rect k:type="flat-point" x="281.96" y="261.96" width="8" height="8" fill="#ff7f00"/>
    <rect k:type="flat-point" x="268.5" y="272.29" width="8" height="8" fill="#ffff00"/>
    <rect k:type="flat-point" x="252.82" y="278.79" width="8" height="8" fill="#7fff00"/>
    <rect k:type="flat-point" x="236" y="281" width="8" height="8" fill="#00ff00"/>
    <rect k:type="flat-point" x="219.18" y="278.79" width="8" height="8" fill="#00ffff"/>
    <rect k:type="flat-point" x="203.5" y="272.29" width="8" height="8" fill="#0000ff"/>
    <rect k:type="flat-point" x="190.04" y="261.96" width="8" height="8" fill="#4b0082"/>
    <rect k:type="flat-point" x="179.71" y="248.5" width="8" height="8" fill="#9400d3"/>
  </g>

  <!-- bow tie: 6 cyan Segments (bare <line>), total width 91.844. Outer corners B share x with
       the tangent points T (vertical left/right edges). Tangency to the face contour pins
       T_right.y = -sqrt(R² - T_right.x²) ≈ -88.8322 and forces the knot to K = (0, R²/T_right.y)
       ≈ (0, -112.5719). Lower edges mirror upper across the horizontal through K. Shared cyan
       stroke + width hoisted; each Segment inherits via the presentational fallback chain. -->
  <g stroke="#00ffff" stroke-width="2">
    <line x1="240" y1="352.5719" x2="194.0780" y2="328.8322"/>
    <line x1="194.0780" y1="328.8322" x2="194.0780" y2="376.3116"/>
    <line x1="194.0780" y1="376.3116" x2="240" y2="352.5719"/>
    <line x1="240" y1="352.5719" x2="285.9220" y2="328.8322"/>
    <line x1="285.9220" y1="328.8322" x2="285.9220" y2="376.3116"/>
    <line x1="285.9220" y1="376.3116" x2="240" y2="352.5719"/>
  </g>
</svg>
"""
