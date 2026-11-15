# Module cga

2D conformal geometric algebra for the plane. It covers points, lines, circles and dipoles, the
segments, polygons and polylines composed from them, and the versors (`Motor`, `Flector`) that
move them. The types are immutable and joined by operators:

- the infix operators `join`, `meet`, `dot`, `on`, `intersects`, `distanceTo`, `angleTo`,
  `onCollide`, `onEscape`, `gap`, `coerceOut`, `coerceIn` and `coerceOn`;
- the containment operator `in`/`!in`;
- the orientation flip `-`;
- the transform action `*`.

The algebra is conformal GA over the null basis `e1..e4`: `e1` and `e2` Euclidean, `e3`
projective, `e4` the point at infinity. Components are stored exact, at full `Double` precision.
Equality and predicates round at the `SCALE` quantum: store exact, round only for identity.

Every shape class is open and carries a `copy` rebuild hook. The shape-preserving operations
(`*`, `-`, `coerceOut`, `coerceIn`, `coerceOn`, `canonical`) are generic over the receiver's
type. A subclass that overrides its shape's `copy` comes back from every such operation as itself.

Construction is checked: a degenerate shape or versor throws its `Degenerate*Exception`. So are
the operation preconditions whose violation would otherwise pass silently:

- a non-convex polygon throws `NonConvexPolygonException`, and an inside-out one
  `InsideOutPolygonException`, where the operation forbids them;
- an operand at infinity where a position is needed throws `AtInfinityException`.

Where the out-of-contract answer is the algebra's own, the operation documents it instead.

# Package fromcrackpot.kintsugi.cga.d2.transforms

The transform tier: the versors that move shapes, applied with `shape * versor` and composed with
`versor * versor`.

- `Motor`: rotations and translations, built by the `T` and `R` factories.
- `Flector`: reflections and glide reflections, built by `F` and `G`.

Beside them sits the `Dilator`: uniform scalings about a point, built by `D`, applied with
`shape * dilator` and composed with `dilator * dilator`. The per-shape actions live in the
`primitives` and `composites` subpackages. A receiver typed only as `Shape` moves through the same
spelling: the `Shape * Versor` dispatch delegates to the per-shape actions and preserves the
receiver's class. A dilator applies to concretely typed shapes only.

## The factory family

Five factories build the versors and the dilator from plain Euclidean and primitive inputs. The
representation's half coefficients stay inside the factories, so arguments are true distances,
angles, scales and mirror lines.

| Factory | Result |
|---------|--------|
| `T(dx, dy)`<br>`T(direction)` | the translating `Motor`, by `(dx, dy)` or by a `Direction`'s free vector |
| `R(center, angle)` | the `Motor` rotating counter-clockwise by the angle about the center |
| `F(line)` | the `Flector` reflecting across the line |
| `G(line, glide)` | the `Flector` reflecting across the line, then gliding `glide` units along it |
| `D(center, scale)` | the `Dilator` scaling by `scale` about the center: contracting below `1`, expanding above |

Degenerate inputs:

- A mirror line at infinity throws `DegenerateFlectorException`.
- `R` about an at-infinity center degrades to a translator. It throws `DegenerateMotorException`
  only at a half-turn.
- A scale that rounds to zero or below throws `DegenerateDilatorException`.
- `D` about an at-infinity center degrades to a translator along it.

## The composition family

Versors compose with `*` into the single versor that performs the left operand's motion first,
then the right's. Composition is exactly associative with the action,
`(u * A) * B == u * (A * B)`, so a stored composite motion behaves like chained applications.
Every versor is stored unitized and unitization survives composition, so composing never throws.

| Pairing | Result |
|---------|--------|
| `Motor * Motor` | `Motor`: two rigid motions chain into one |
| `Flector * Flector` | `Motor`: the orientation flips cancel |
| `Flector * Motor`<br>`Motor * Flector` | `Flector`: one flip remains |
| `Versor * Versor` | `Versor`: the dispatch for operands typed as `Versor` |
| `Dilator * Dilator` | `Dilator`: scales multiply, and scales multiplying to `1` leave a translation |

A dilator is not a `Versor` and composes with dilators only. A scaling and a rigid motion chain on
the shape instead: `shape * dilator * motor`. A dilation and a rotation about the same center
commute.

# Package fromcrackpot.kintsugi.cga.d2.transforms.primitives

The action tier for the primitives. `shape * versor` applies a versor's motion to each of the
five primitives and returns the same primitive type. Each shape has a `* Motor` form, a
`* Flector` form and a `* Versor` dispatch.

- Results carry full raw `Double` precision.
- Each form is generic over the receiver's type. The moved geometry returns through the shape's
  `copy` hook, so a subclass that overrides it comes back as its own class.
- A motor's rigid motion preserves orientation. A flector's mirror motion reverses it, and the
  oriented types (`Line`, `Dipole`, `Circle`) record the reversal in their own components.
- Imaginary and at-infinity operands transform faithfully as valid data. No action on a primitive
  throws.

| Pairing | Result |
|---------|--------|
| `FlatPoint * Motor`<br>`FlatPoint * Flector` | the moved `FlatPoint`. At infinity (zero weight), it turns as a free vector |
| `Line * Motor`<br>`Line * Flector` | the moved `Line`, orientation carried. A flector mirrors it |
| `RoundPoint * Motor`<br>`RoundPoint * Flector` | the moved `RoundPoint`, squared radius kept |
| `Dipole * Motor`<br>`Dipole * Flector` | the dipole of the moved endpoints, `start` staying `start` |
| `Circle * Motor`<br>`Circle * Flector` | the moved `Circle`, radius kept. A flector flips classic ↔ inside-out |

## Dilation

`shape * dilator` scales each primitive about the dilator's center and returns the same primitive
type, through the same `copy` hook. Weights are kept, so sizes follow the scale: a radius, a
direction's length and a line's distance from the center are all multiplied by it. Orientation is
preserved.

| Pairing | Result |
|---------|--------|
| `FlatPoint * Dilator` | the dilated `FlatPoint`; at infinity, its length multiplied by the scale |
| `Line * Dilator` | the dilated `Line`, orientation carried; at infinity, `gz` multiplied by the scale |
| `RoundPoint * Dilator` | the dilated `RoundPoint`, radius multiplied by the scale |
| `Dipole * Dilator` | the dipole of the dilated endpoints, radius multiplied by the scale |
| `Circle * Dilator` | the dilated `Circle`, orientation kept, radius multiplied by the scale |

# Package fromcrackpot.kintsugi.cga.d2.transforms.composites

The action tier for the composites. `shape * versor` moves a segment by its endpoints, and a
polyline or polygon vertex by vertex. Each shape has the same `* Motor` / `* Flector` /
`* Versor` trio, plus a `* Dilator` form. Results carry full raw `Double` precision. Each form is
generic over the receiver's type: the moved geometry returns through the shape's `copy` hook, so a
subclass that overrides it comes back as its own class.

| Pairing | Result |
|---------|--------|
| `Segment * Motor`<br>`Segment * Flector` | the segment of the moved endpoints |
| `Polyline * Motor`<br>`Polyline * Flector` | the polyline of the moved vertices |
| `Polygon * Motor`<br>`Polygon * Flector` | the polygon of the moved vertices. A flector reverses the winding |
| `Segment * Dilator` | the segment of the dilated endpoints, length multiplied by the scale |
| `Polyline * Dilator` | the polyline of the dilated vertices |
| `Polygon * Dilator` | the polygon of the dilated vertices, winding kept |

Degenerate inputs: consecutive vertices within a `SCALE` quantum of each other can come back
rounded-coincident, and so can vertices a contraction brings that close. The chain then throws
`DegeneratePolylineException` or `DegeneratePolygonException`. `Segment * versor` and
`Segment * dilator` never throw.

# Package fromcrackpot.kintsugi.cga.d2.operations.products

The algebra tier: infix operators over the shapes that return geometry or a `Double`, raw at full
precision. The segment-bounded meets return `null` on a miss.

- scalar multiplication (`*`);
- the inner-product family (`dot`);
- join (`join`, the wedge `∧`) and meet (`meet`, the antiwedge `∨`);
- the two interior products: contraction (`contract`, the bulk contraction `a ∨ b^★`) and
  expansion (`expand`, the weight expansion `a ∧ b^☆`).

## Scalar multiplication

The homogeneous `*` scales a flat point's components raw. A finite point comes back at the same
position, since the factor cancels on re-unitization. A direction scales in magnitude.

| Pairing | Result |
|---------|--------|
| `FlatPoint * Double` | the scaled flat point, as the receiver's class. A finite point keeps its position; a direction is scaled |

## The `dot` family

Every `dot` function returns a raw `Double` and serves one purpose at the call site: incidence and
signed distance/power probing. One rule covers all seven pairings:

- a flat point enters through its round representative;
- same-grade operands take the strict inner product under the conformal metric 𝐆;
- a point against a grade-3 object takes the scalar antiwedge pairing.

Those are the only pairings the algebra gives a canonical scalar, and exactly the family's
coverage. The metric is the null pairing `e3·e4 = −1`, so `A dot A = −radius²` for every round or
flat element.

| Pairing | Value for finite operands |
|---------|---------------------------|
| `RoundPoint dot RoundPoint`<br>`FlatPoint dot FlatPoint` | `−½·d²` for points `d` apart |
| `FlatPoint dot Line`<br>`RoundPoint dot Line` | `0` when the point lies on the line; the signed Euclidean distance otherwise |
| `FlatPoint dot Circle`<br>`RoundPoint dot Circle` | `½·(r² − d²)` against a classic circle: positive inside, zero on, negative outside |
| `Circle dot Circle` | `½·(d² − r₁² − r₂²)` for two classic circles, zero exactly on orthogonal circles |

The circle identities take `r²` signed, so they extend to imaginary circles: an imaginary classic
circle probes negative everywhere. An inside-out circle negates every component, so any `dot`
against one negates. At-infinity operands reduce the bilinear form to a directional pairing with
no distance reading.

## The `join` family

`join` is the wedge `∧`: grades add, and two operands join into the higher-grade primitive through
both. The cross-type pairings come in both operand orders, and both spellings build the identical
result.

| Pairing | Result |
|---------|--------|
| `FlatPoint join FlatPoint` | the `Line` through both points |
| `FlatPoint join RoundPoint`<br>`RoundPoint join FlatPoint` | the `Line` through both points |
| `RoundPoint join RoundPoint` | the `Dipole` holding both points, first operand as `start` |
| `Dipole join RoundPoint`<br>`RoundPoint join Dipole` | the `Circle` through the dipole's endpoints and the point |
| `Segment join RoundPoint`<br>`RoundPoint join Segment` | the `Circle` through the segment's endpoints and the point |

Degenerate inputs:

- Coincident flat points collapse to the all-zero `Line`.
- Coincident round points, or two at-infinity points with parallel directions, throw
  `DegenerateDipoleException`.
- A third point on the dipole's carrier throws `DegenerateCircleException`, carrying the `Line`
  through the collinear points.

## The `meet` family

`meet` is the antiwedge `∨`: grades reduce, and two operands meet in the lower-grade primitive
where they intersect.

| Pairing | Result |
|---------|--------|
| `Line meet Line` | the `FlatPoint` where the lines cross; at infinity when parallel |
| `Line meet Circle`<br>`Circle meet Line` | the `Dipole` of the two crossing points, imaginary on a miss |
| `Circle meet Circle` | the `Dipole` of the two crossing points, imaginary when the circles don't touch |
| `Line meet Dipole` | the `RoundPoint` where the line crosses the dipole's carrier |
| `Line meet Segment`<br>`Segment meet Line` | the crossing `RoundPoint`, or `null` when it misses the segment |
| `Segment meet Segment` | the crossing `RoundPoint`, or `null` when it misses either segment |

Degenerate inputs:

- Parallel lines meet at infinity.
- Tangency yields a zero-radius `Dipole` centered at the tangent point. It is valid data, never an
  error.
- Concentric circles meet in an at-infinity `Dipole`.
- Coincident circles, or an all-zero line against a circle, collapse and throw
  `DegenerateDipoleException`.

## The `contract` family

`contract` is the bulk contraction `a ∨ b^★`: grades reduce, and the result is contained in the
higher-grade receiver and orthogonal to the argument contracted away.

| Pairing | Result |
|---------|--------|
| `Line contract RoundPoint` | the `Dipole` on the line centered at the point's perpendicular foot, imaginary off the line |
| `Circle contract RoundPoint` | the `Dipole` where the circle crosses its radical axis with the point, imaginary on a miss |
| `Line contract FlatPoint` | the line's running direction, as an at-infinity `RoundPoint` |
| `Circle contract FlatPoint` | the circle's tangent bearing at the point, as an at-infinity `RoundPoint` |
| `Line contract Dipole` | the `RoundPoint` where the line crosses the endpoints' perpendicular bisector |
| `Circle contract Dipole` | the pairing `RoundPoint` on the endpoints' perpendicular bisector |
| `Dipole contract RoundPoint` | the swap-circle's center as a `RoundPoint`, squared radius negated |
| `FlatPoint contract RoundPoint` | the flat point's position, sized by minus the squared distance to the point |

Degenerate inputs: a point at infinity collapses `Line contract RoundPoint`, which throws
`DegenerateDipoleException`. Every `RoundPoint`-valued contraction is total: degenerate cases come
back at infinity, kept raw.

## The `expand` family

`expand` is the weight expansion `a ∧ b^☆`: grades raise, and the result contains the lower-grade
receiver and is orthogonal to the argument expanded onto.

| Pairing | Result |
|---------|--------|
| `RoundPoint expand Line` | the `Dipole` holding the point and its mirror image across the line |
| `RoundPoint expand Circle` | the `Dipole` holding the point and its inversion through the circle |
| `RoundPoint expand Dipole` | the `Circle` through the point swapping the endpoints under inversion |
| `RoundPoint expand FlatPoint` | the `Circle` through the round point centered at the flat point |
| `Dipole expand Line` | the `Circle` through the endpoints, centered on the line |
| `Dipole expand Circle` | the `Circle` through the endpoints, crossing the circle at right angles |
| `FlatPoint expand Line` | the `Line` through the point perpendicular to the line |
| `FlatPoint expand Circle` | the `Line` through the point and the circle's center |

Degenerate inputs: collapsed blades throw `DegenerateDipoleException` or
`DegenerateCircleException`, carrying the collapsed payload:

- a point at a circle's center, whose inversion escapes to infinity;
- a point on the endpoints' perpendicular bisector, where the swap-circle straightens into that
  mirror line;
- a carrier perpendicular to the target line, where the circle straightens into the carrier.

The `Line`-valued expansions never throw. They collapse to the all-zero line instead.

# Package fromcrackpot.kintsugi.cga.d2.operations.projections

The projection tier: composite operations built from the products.

- Projection (`project`, `b ∨ (a ∧ b^☆)`) carries a shape onto a higher-grade target.
- Antiprojection (`antiproject`, `b ∧ (a ∨ b^☆)`) carries one onto a lower-grade target.

Both divide by the target's self-pairing `b ∘ b`, so the receiver's weight and orientation carry
through. An imaginary target (negative self-pairing) negates every component. When the
self-pairing vanishes:

- a target at infinity (a line, or a point antiprojected onto) throws `AtInfinityException`;
- a round target (a zero-radius circle, coincident endpoints) floods the division instead.

The one bounded target, a `Segment`, has no self-pairing of its own. It is reached through its
carrier's projection and clamped to the segment, and it takes a finite point and a finite segment.

## The `project` family

| Pairing | Result |
|---------|--------|
| `RoundPoint project Line` | the perpendicular foot, sized by the squared distance |
| `RoundPoint project Circle` | the incident round point on the radial line through the point |
| `RoundPoint project Dipole` | the carrier point dividing the endpoints in squared-distance ratio |
| `FlatPoint project Line` | the perpendicular foot |
| `FlatPoint project Circle` | the `Dipole` where the circle crosses the radial line through the point |
| `FlatPoint project Segment` | the nearest point of the segment itself: the perpendicular foot when it falls within the segment, else the nearer endpoint |
| `Dipole project Line` | the `Dipole` cut from the endpoint circle by the line |
| `Dipole project Circle` | the `Dipole` where the circle meets the orthogonal endpoint circle |

A shape already on its target projects to itself. There is deliberately no
`RoundPoint project FlatPoint`: projecting a point onto a point is content-free, since the answer
is the target's location.

## The `antiproject` family

Antiprojection's inner product is the *weight* contraction `a ∨ b^☆`, the blade-wise negation of
the exposed bulk `contract`. So the compositional spelling `b join (a contract b)` comes back
orientation-flipped relative to the fused forms.

Point targets are one geometric operation with two spellings. The fused forms live on
`FlatPoint`, the representation the algebra keeps non-degenerate. The `RoundPoint` overloads
forward through `toFlat`, because the raw round-target forms collapse to the target's own weighted
point-circle for null lifts.

A circle antiprojected onto a point returns a `Line`, not a circle. Every flat-point blade carries
`e4`, so no round weight can come back: the result is the flat limit of carrying the circle's
attitude over to the point.

| Pairing | Result |
|---------|--------|
| `Line antiproject FlatPoint`<br>`Line antiproject RoundPoint` | the parallel through the point |
| `Circle antiproject FlatPoint`<br>`Circle antiproject RoundPoint` | the `Line` through the point perpendicular to its radial |
| `Line antiproject Dipole` | the `Circle` through the endpoints and the line's crossing with their bisector |
| `Circle antiproject Dipole` | the `Circle` through the endpoints paired to the circle |

A circle already through the endpoints antiprojects to itself. `Dipole` and `FlatPoint` have no
antiprojection receivers: their only lower-grade target is a round point, where the operation
degenerates.

Degenerate inputs: a line parallel to the dipole's carrier, or a collapsing round weight, throws
`DegenerateCircleException` carrying the straightened line.

# Package fromcrackpot.kintsugi.cga.d2.operations.queries

The query tier, split by answer kind:

- `predicates` holds the Boolean queries, rounded at the `SCALE` quantum.
- `measures` holds the scalar measurements: raw Euclidean distances and angles.
- The data queries at this level, penetration and `gap`, answer with geometry-flavored data: the
  offending edge, depth and normal, or the shortest segment. Their values stay raw behind rounded
  existence gates.

## The penetration family

The dual collision queries report the violated face, the penetration depth and the way back:

- `subject onCollide collider` answers where the subject polygon reaches into the collider's solid
  region.
- `subject onEscape boundary` answers where it reaches out of the boundary's.

The queried polygon's orientation decides which side is solid, and with it the strategy:

| Pairing | Result |
|---------|--------|
| `Polygon onCollide Polygon` (classic collider: an obstacle) | the subject's shallowest push-out past a single face, or `null` when they don't overlap or merely touch |
| `Polygon onCollide Polygon` (inside-out collider: a wall) | the subject's deepest protrusion out of the allowed interior, or `null` when it lies inside |
| `Polygon onEscape Polygon` (classic boundary: a container) | the subject's deepest protrusion out of the boundary's interior, or `null` when it lies inside |
| `Polygon onEscape Polygon` (inside-out boundary: its loop forbidden) | the subject's shallowest push-out of the loop past a single face, or `null` when it lies clear of the loop or merely touches it |
| `Penetration? { onPenetration }` | the handler spelling: runs the lambda on the penetration when there is one, and passes the answer through |

The two verbs are duals through negation. `subject onEscape polygon` agrees with
`subject onCollide -polygon` on depth and normal, and `subject onCollide polygon` with
`subject onEscape -polygon`. Each is named in its own right rather than spelled through the other.
The shared contract, which `constraints`' coerce verbs inherit:

- **Convex loop on the queried polygon, checked.** Candidate faces come from the collider or
  boundary, and the subject contributes its vertices. The pick is sound only when that loop is
  convex, so a non-convex one throws `NonConvexPolygonException`.
  - Where the subject is kept out of a bounded region (a classic collider, an inside-out
    boundary), existence reads the subject's own faces. It must then be convex too, and is checked
    the same way.
  - Where it is kept within one (an inside-out collider, a classic boundary), any polygon works.
  - Faces for the pick come from the queried polygon alone, so a deep corner-on-corner overlap may
    pick a face that is not the shortest way out.
- **One penetration per call.** Situations that violate several faces at once converge by
  resolving and querying again.
- **Touching is not penetration.** Existence rounds at the `SCALE` quantum. A subject exactly on
  the solid region's boundary needs no correction, so the query returns `null`, whichever side's
  vertex rests on the other's face. The returned depth itself stays raw.

`Penetration.depth` is a raw Euclidean distance. `Penetration.normal` is a unit direction pointing
out of the region the subject may not occupy: the correcting direction.

## The gap query

`subject gap other` answers the closest pair of two polygons clear of each other: the shortest
segment from the subject's boundary to the other's. Its length is the shortest distance between
them.

| Pairing | Result |
|---------|--------|
| `Polygon gap Polygon` | the shortest `Segment` from this polygon to the other (`start` on this polygon, `end` on the other), or `null` when the solid regions share any point, touching included |

The contract:

- **Disjoint solid regions.** Existence is the `predicates` package's closed `intersects` gate,
  rounded at the `SCALE` quantum. Overlapping or touching regions have no gap.
- **Oriented regions.** Each operand is its oriented solid region. A polygon within an inside-out
  one pairs to the loop from inside, and two inside-out polygons never have a gap.
- **No convexity required** of either operand.
- **Ties are arbitrary.** Parallel faces share the shortest distance. One pair is picked, and the
  displacement from `start` to `end` is the same for all of them. The segment itself stays raw.

# Package fromcrackpot.kintsugi.cga.d2.operations.queries.measures

The measure tier: scalar Euclidean measurements, answered raw. Distances are non-negative
`Double`s, and angles are signed `Angle`s.

| Pairing | Result |
|---------|--------|
| `FlatPoint distanceTo FlatPoint`<br>`RoundPoint distanceTo RoundPoint`<br>`FlatPoint distanceTo RoundPoint`<br>`RoundPoint distanceTo FlatPoint` | the raw distance between the positions |
| `FlatPoint distanceTo Line`<br>`RoundPoint distanceTo Line` | the raw perpendicular distance to the line |
| `Line angleTo Line` | the raw signed angle turning the first direction onto the second |

The family contract:

- **Distances are absolute.** A distance is non-negative. The signed side of a point against a
  line keeps its one spelling in the dot family (`point dot line`).
- **Angles are signed and orientation-sensitive.** They are counter-clockwise positive in
  `(−π, π]`. Reversing either line's orientation offsets the answer by a half turn.
- **Positions only.** A sized or imaginary round point measures from its position; the conformal
  component takes no part. Circles and dipoles have no pairing here: a scalar against a truly round
  object is a conformal measurement, not a Euclidean one, and lives in the dot family.
- **Raw everywhere.** No `SCALE` rounding anywhere in the family: measures are values, not
  decisions.
- **Finite operands.** An operand at infinity has no Euclidean reading. A distance throws
  `AtInfinityException`, and `angleTo` answers the zero angle for a line at infinity.

# Package fromcrackpot.kintsugi.cga.d2.operations.queries.predicates

The decision tier: predicates that answer `Boolean` where the operator tier answers geometry. It
covers incidence (`on`), containment (`contains`, spelled `in`/`!in`) and region overlap
(`intersects`). Every answer is rounded at the `SCALE` quantum.

Every predicate is closed, so the boundary belongs to the shape:

| State | Spelling | Decision |
|---------|------------|----------|
| on | `p on c` | `round(dot) == 0` |
| inside | `p in c`<br>`c contains p` | `round(dot) >= 0` |
| outside | `p !in c`<br>`!(c contains p)` | `round(dot) < 0` |

The three states partition exactly at the quantum, and on implies inside (`p on c` implies
`p in c`). `intersects` is closed on the same terms, so on implies intersects: the derived
families agree with `on` at the boundary by construction.

The strict (open) interior is deliberately unnamed, and negation spells it. `p !in -c` holds
exactly when `p` is strictly inside `c`, because both orientations are closed and only the strict
interior escapes the complement.

## The `on` family

`on` tests whether a point lies on a shape's boundary.

| Pairing | True when |
|---------|-----------|
| `FlatPoint on Line`<br>`RoundPoint on Line` | the point lies on the line |
| `FlatPoint on Circle`<br>`RoundPoint on Circle` | the point lies on the circle, orientation ignored |
| `FlatPoint on Segment`<br>`RoundPoint on Segment` | the point lies on the segment itself, endpoints included |
| `FlatPoint on Polygon`<br>`RoundPoint on Polygon` | the point lies on any edge |

Precision is not uniform across the pairings. The Line, Segment and Polygon forms are all-linear
and robust at the `SCALE` quantum. The Circle forms are quadratic, so the residual grows with the
radius, and a point near a large circle's boundary may fail the test. Callers wanting a "near"
test need their own tolerance.

## The `contains` family

`contains` decides containment, boundary included. It is a Kotlin `operator fun`, so the
call-site spellings are the language's. `point in circle` reads inside-or-on, and
`point !in circle` the strict outside: neither inside nor on, since the boundary belongs to the
shape. There are deliberately no `inside`/`outside` functions: one relation, one name, with
`in`/`!in` as its syntax.

| Pairing | `in` is true when |
|---------|-------------------|
| `Circle contains FlatPoint`<br>`Circle contains RoundPoint` | the point lies in the circle's interior or on it |
| `Circle contains Circle` | the other circle's region lies within the receiver's |
| `Polygon contains FlatPoint`<br>`Polygon contains RoundPoint` | the point lies within the polygon's solid region or on its boundary |

Every form is orientation-aware. An inside-out circle's interior is the unbounded region outside
it, and an inside-out polygon (wound clockwise) contains the closed complement of its loop. The
circle forms share `on`'s quadratic precision caveat. Classic polygon containment follows the
nonzero winding rule, so self-touching and self-crossing loops classify by winding. The inside-out
reading is its complement, boundary included either way.

`Circle contains Circle` reads per orientation pair, for real circles with radii `r₁` (the
receiver) and `r₂` (the argument) and centers `d` apart:

| Receiver | Argument | Contains when |
|------------|------------|---------------|
| classic | classic | `d + r₂ ≤ r₁`: nested, the argument no bigger than the receiver |
| inside-out | inside-out | `d + r₁ ≤ r₂`: nested, the argument no smaller than the receiver |
| inside-out | classic | `d ≥ r₁ + r₂`: the argument's disk clears the receiver's excluded region |
| classic | inside-out | never: an unbounded region cannot fit in a bounded one |

Imaginary circles are not rejected. The same gates run on the raw magnitudes of their radii, with
no region behind the answer.

## The `intersects` family

`intersects` tests region overlap, in the sense of "not disjoint": two shapes intersect when they
share at least one point, touching included. Polygon operands count as closed regions (interior
plus boundary), and segments and lines as their point sets. The predicate is symmetric, and
cross-type pairings come in both operand orders.

| Pairing | True when |
|---------|-----------|
| `Segment intersects Line`<br>`Line intersects Segment` | the line touches or crosses the segment |
| `Segment intersects Segment` | the segments touch, cross, or overlap |
| `Polygon intersects Line`<br>`Line intersects Polygon` | the line reaches the polygon's solid region (always, for an inside-out polygon) |
| `Segment intersects Polygon`<br>`Polygon intersects Segment` | the segment touches the boundary or lies within the solid region |
| `Polygon intersects Polygon` | the solid regions share any point (always, when both are inside-out) |

A polygon operand is its oriented solid region: a classic polygon fills its loop, an inside-out
one the closed complement. Two inside-out polygons always intersect: both solid regions are
unbounded, and far enough out every direction belongs to both. The name has the DE-9IM sense of
`intersects`, not DE-9IM `overlaps`, which would exclude containment and touching. The boolean
answer is the whole product; the crossing geometry itself comes from `meet`.

# Package fromcrackpot.kintsugi.cga.d2.operations.constraints

The constraint tier: operations that solve for the motion satisfying a geometric relation and
return the shape already moved.

- The dual verbs `coerceOut` and `coerceIn` keep a polygon out of, or inside, an oriented solid
  region. They follow the standard library's coerce posture: an already-valid receiver comes back
  untouched, and a violating one minimally moved.
- `coerceOn` is the equality constraint beside them: it rests a polygon against the region. The
  two inequality verbs accept every pose that does not violate; `coerceOn` accepts exactly the
  touching ones. It resolves as the `coerceOut` nudge followed by `gap`: an overlapping subject is
  nudged clear, then any gap left, from the start or by the nudge, is closed.
- All three are generic over the receiver's type, so a `Polygon` subclass comes back as its own
  class.

| Pairing | Result |
|---------|--------|
| `subject coerceOut collider` (classic collider: an obstacle) | the subject nudged out along the shallowest push-out, or untouched when they don't overlap |
| `subject coerceOut collider` (inside-out collider: a wall) | the subject retreated back inside the allowed interior, or untouched when it already lies inside |
| `subject coerceIn boundary` (classic boundary) | the subject retreated back inside the boundary's bounded interior, or untouched when it already lies inside |
| `subject coerceIn boundary` (inside-out boundary) | the subject nudged back into the complement of the boundary's loop, or untouched when it already lies there |
| `subject coerceOn support` (classic support) | the subject rested against the loop from outside, or untouched when touching. When apart, it is drawn in along its shortest gap. When overlapping, the shallowest push-out comes first, which can leave it apart near a vertex |
| `subject coerceOn support` (inside-out support) | the subject rested against the loop's inner wall, or untouched when touching. It retreats back inside when protruding, and is drawn out to the wall along its shortest gap when floating within |

The two verbs are duals through negation. `subject coerceIn boundary` lands the subject where
`subject coerceOut -boundary` does, since staying inside a region is staying out of its
complement. Each resolves its own query, `onEscape` and `onCollide` respectively.

The penetration contract of the `queries` package applies wholesale: convex loop on the queried
polygon, one penetration resolved per call, and touching is not penetration. Several simultaneous
violations converge by coercing again. For `coerceOn`:

- the contract binds every pose. The nudge's query runs first, so the support is checked convex
  even for a subject starting apart;
- one penetration per call holds too. A subject protruding across a corner of an inside-out
  support can still protrude after one call.

The resolution is the nudge: a translation along the penetration normal by the raw depth. For
`coerceOn`, a second translation then closes any remaining gap. A resolution of the caller's own
is spelled on the query itself: `(subject onCollide collider) { penetration -> … }` or
`(subject onEscape boundary) { … }`.
