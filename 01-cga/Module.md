# Module cga

2D conformal geometric algebra for the plane: points, lines, circles, and dipoles, plus the
segments, polygons, and polylines composed from them, and the versors (`Motor`, `Flector`) that
move them — immutable Kotlin types joined by infix operators (`join`, `meet`, `dot`, `on`,
`intersects`, `distanceTo`, `angleTo`, `collide`, `escape`, `gap`, `coerceOut`, `coerceIn`, `coerceOn`),
the containment operator
`in`/`!in`, the orientation flip `-`, and the transform action `*`.

The algebra is conformal GA over the null basis `e1..e4` (`e1`, `e2`
Euclidean; `e3` projective; `e4` the point at infinity). Components are stored exact at full
`Double` precision; equality and predicates round at the `SCALE` quantum — store exact, round
only for identity.

Every shape class is open and carries a `copy` rebuild hook, and the shape-preserving
operations (`*`, `-`, `coerceOut`, `coerceIn`, `coerceOn`, `canonical`) are generic over the receiver's type: a
subclass that overrides its shape's `copy` comes back from every such operation as itself.

# Package fromcrackpot.kintsugi.cga.d2.products

The algebra tier: infix operators over the shapes, returning geometry or `Double` raw at full
precision (the segment-bounded meets return `null` on a miss) — scalar multiplication (`*`),
the inner-product family (`dot`), join (`join`, the wedge `∧`), meet (`meet`, the
antiwedge `∨`), and the two interior products: contraction (`contract`, the bulk contraction
`a ∨ b^★`) and expansion (`expand`, the weight expansion `a ∧ b^☆`).

## Scalar multiplication

The homogeneous `*` scales a flat point's components raw: a finite point comes back at the
same position — the factor cancels on re-unitization — while a direction genuinely scales in
magnitude.

| Pairing | Result |
|---------|--------|
| `FlatPoint * Double` | the scaled flat point, as the receiver's class — the same position when finite, a scaled direction at infinity |

## The `dot` family

Every `dot` function returns a raw `Double` and serves the same call-site purpose: incidence
and signed distance/power probing. One rule covers all seven pairings: a flat point enters
through its round representative, then same-grade operands take the strict inner product
under the conformal metric 𝐆, and a point against a grade-3 object takes the scalar
antiwedge pairing. Those are the only pairings the algebra gives a canonical scalar —
exactly the family's coverage. The metric is the null pairing `e3·e4 = −1`, giving
the sign convention `A dot A = −radius²` for every round or flat element.

| Pairing | Value for finite operands |
|---------|---------------------------|
| `RoundPoint dot RoundPoint`<br>`FlatPoint dot FlatPoint` | `−½·d²` for points `d` apart |
| `FlatPoint dot Line`<br>`RoundPoint dot Line` | `0` when the point lies on the line; the signed Euclidean distance otherwise |
| `FlatPoint dot Circle`<br>`RoundPoint dot Circle` | `½·(r² − d²)` against a classic circle: positive inside, zero on, negative outside |
| `Circle dot Circle` | `½·(d² − r₁² − r₂²)` for two classic circles — zero exactly on orthogonal circles |

The circle identities take `r²` signed, so they extend to imaginary circles — an imaginary
classic circle probes negative everywhere. An inside-out circle negates every component, so
any `dot` against one negates. At-infinity operands degenerate the bilinear form to a
directional pairing with no distance reading.

## The `join` family

`join` is the wedge `∧`: grades add, and two operands join into the higher-grade primitive
through both. The cross-type pairings come in both operand orders, and the two spellings
build the identical result.

| Pairing | Result |
|---------|--------|
| `FlatPoint join FlatPoint` | the `Line` through both points |
| `FlatPoint join RoundPoint`<br>`RoundPoint join FlatPoint` | the `Line` through both points |
| `RoundPoint join RoundPoint` | the `Dipole` holding both points, first operand as `start` |
| `Dipole join RoundPoint`<br>`RoundPoint join Dipole` | the `Circle` through the dipole's endpoints and the point |
| `Segment join RoundPoint`<br>`RoundPoint join Segment` | the `Circle` through the segment's endpoints and the point |

Degenerate inputs: coincident flat points collapse to the all-zero `Line`; coincident round
points — or two at-infinity points with parallel directions — throw
`DegenerateDipoleException`; a third point on the dipole's carrier throws
`DegenerateCircleException` carrying the `Line` through the collinear points.

## The `meet` family

`meet` is the antiwedge `∨`: grades reduce, and two operands meet in the lower-grade
primitive where they intersect.

| Pairing | Result |
|---------|--------|
| `Line meet Line` | the `FlatPoint` where the lines cross; at infinity when parallel |
| `Line meet Circle`<br>`Circle meet Line` | the `Dipole` of the two crossing points — imaginary on a miss |
| `Circle meet Circle` | the `Dipole` of the two crossing points — imaginary when the circles don't touch |
| `Line meet Dipole` | the `RoundPoint` where the line crosses the dipole's carrier |
| `Line meet Segment`<br>`Segment meet Line` | the crossing `RoundPoint`, or `null` when it misses the segment |
| `Segment meet Segment` | the crossing `RoundPoint`, or `null` when it misses either segment |

Degenerate inputs: parallel lines meet at infinity; tangency yields a zero-radius `Dipole`
centered at the tangent point (first-class, never an error); concentric circles meet in an
at-infinity `Dipole`; coincident circles — or an all-zero line against a circle — collapse
and throw `DegenerateDipoleException`.

## The `contract` family

`contract` is the bulk contraction `a ∨ b^★`: grades reduce, and the result is contained in
the higher-grade receiver and orthogonal to the argument contracted away.

| Pairing | Result |
|---------|--------|
| `Line contract RoundPoint` | the `Dipole` on the line centered at the point's perpendicular foot — imaginary off the line |
| `Circle contract RoundPoint` | the `Dipole` where the circle crosses its radical axis with the point — imaginary on a miss |
| `Line contract FlatPoint` | the line's running direction: an at-infinity `RoundPoint` |
| `Circle contract FlatPoint` | the circle's tangent bearing at the point: an at-infinity `RoundPoint` |
| `Line contract Dipole` | the `RoundPoint` where the line crosses the endpoints' perpendicular bisector |
| `Circle contract Dipole` | the pairing `RoundPoint` on the endpoints' perpendicular bisector |
| `Dipole contract RoundPoint` | the swap-circle's center as a `RoundPoint`, squared radius negated |
| `FlatPoint contract RoundPoint` | the flat point's position, sized by minus the squared distance to the point |

Degenerate inputs: a point at infinity collapses `Line contract RoundPoint` and throws
`DegenerateDipoleException`; every `RoundPoint`-valued contraction is total — degenerate
cases come back at infinity, kept raw.

## The `expand` family

`expand` is the weight expansion `a ∧ b^☆`: grades raise, and the result contains the
lower-grade receiver and is orthogonal to the argument expanded onto.

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
`DegenerateCircleException` carrying the collapsed payload — a point at a circle's center
(its inversion escapes to infinity), a point on the endpoints' perpendicular bisector (the
swap-circle straightens into that mirror line), a carrier perpendicular to the target line
(the circle straightens into the carrier). The `Line`-valued expansions never throw: they
collapse to the all-zero line instead.

# Package fromcrackpot.kintsugi.cga.d2.projections

The projection tier: composite operations built from the products. Projection (`project`,
`b ∨ (a ∧ b^☆)`) carries a shape onto a higher-grade target; antiprojection (`antiproject`,
`b ∧ (a ∨ b^☆)`) carries one onto a lower-grade target. Both divide by the target's
self-pairing `b ∘ b`, so the receiver's weight and orientation carry through — an imaginary
target (negative self-pairing) negates every component, and a target whose self-pairing
vanishes (a line at infinity, a zero-radius circle, coincident endpoints) floods the division
instead of throwing. The one bounded target, a `Segment`, is reached through its carrier's
projection and clamped to the segment — no self-pairing of its own.

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
`RoundPoint project FlatPoint`: projecting a point onto a point is content-free — the answer
is the target's location.

## The `antiproject` family

Antiprojection's inner product is the *weight* contraction `a ∨ b^☆` — the blade-wise
negation of the exposed bulk `contract` — so the compositional spelling
`b join (a contract b)` comes back orientation-flipped relative to the fused forms.

Point targets are one geometric operation with two spellings: the fused forms live on
`FlatPoint`, the representation the algebra keeps non-degenerate, and the `RoundPoint`
overloads forward through `toFlat` (the raw round-target forms collapse to the target's own
weighted point-circle for null lifts). A circle antiprojected onto a point returns a `Line`,
not a circle: every flat-point blade carries `e4`, so no round weight can come back — the
flat limit of carrying the circle's attitude over to the point.

| Pairing | Result |
|---------|--------|
| `Line antiproject FlatPoint`<br>`Line antiproject RoundPoint` | the parallel through the point |
| `Circle antiproject FlatPoint`<br>`Circle antiproject RoundPoint` | the `Line` through the point perpendicular to its radial |
| `Line antiproject Dipole` | the `Circle` through the endpoints incident with the line's bisector crossing |
| `Circle antiproject Dipole` | the `Circle` through the endpoints paired to the circle |

A circle already through the endpoints antiprojects to itself. `Dipole` and `FlatPoint` have
no antiprojection receivers: their only lower-grade target is a round point, where the
operation degenerates.

Degenerate inputs: a line parallel to the dipole's carrier — or a collapsing round weight —
throws `DegenerateCircleException` carrying the straightened line.

# Package fromcrackpot.kintsugi.cga.d2.queries

The query tier, split by answer kind: `predicates` carries the Boolean queries, rounded at
the `SCALE` quantum; `measures` carries the scalar measurements — raw Euclidean distances and
angles; the data queries at this level — penetration and `gap` — answer with geometry-flavored
data — offending edge, depth, normal, the shortest segment — whose values stay raw behind
rounded existence gates.

## The penetration family

The dual collision queries: `subject collide collider` answers where the subject polygon
reaches into the collider's solid region, `subject escape boundary` where it reaches out of the
boundary's — the violated face, the penetration depth, and the way back. The queried polygon's
orientation decides which side is solid, and with it the strategy:

| Pairing | Result |
|---------|--------|
| `Polygon collide Polygon` (classic collider — an obstacle) | the subject's shallowest push-out past a single face, or `null` when they don't overlap or merely touch |
| `Polygon collide Polygon` (inside-out collider — a wall) | the subject's deepest protrusion out of the allowed interior, or `null` when it lies inside |
| `Polygon escape Polygon` (classic boundary — a container) | the subject's deepest protrusion out of the boundary's interior, or `null` when it lies inside |
| `Polygon escape Polygon` (inside-out boundary — its loop forbidden) | the subject's shallowest push-out of the loop past a single face, or `null` when it lies clear of the loop or merely touches it |
| `Penetration? { onPenetration }` | the handler spelling: runs the lambda on the penetration when there is one, and passes the answer through |

The two verbs are duals through negation — `subject escape polygon` agrees with
`subject collide -polygon`, and `subject collide polygon` with `subject escape -polygon`, on
depth and normal — each named in its own right rather than spelled through the other. The
shared contract, inherited by `constraints`' coerce verbs:

- **Convex loop on the queried polygon.** Candidate faces come from the collider or boundary —
  the subject contributes its vertices — and the pick is sound only when that loop is convex;
  a concave one yields an arbitrary pick, not an error. Where the subject is kept out of a
  bounded region (a classic collider, an inside-out boundary) the subject must be convex too —
  a concave one may be reported clear while it overlaps; where it is kept within one (an
  inside-out collider, a classic boundary) any polygon works.
- **One penetration per call.** Situations violating several faces at once converge by
  resolving and querying again.
- **Touching is not penetration.** Existence rounds at the `SCALE` quantum: a subject exactly
  on the solid region's boundary needs no correction, so the query returns `null` — whichever
  side's vertex rests on the other's face. The returned depth itself stays raw.

`Penetration.depth` is a raw Euclidean distance and `Penetration.normal` a unit direction
pointed out of the region the subject may not occupy — the correcting direction of the
penetration.

## The gap query

The closest pair of two polygons clear of each other: `subject gap other` answers the shortest
segment from the subject's boundary to the other's, its length the shortest distance between
them.

| Pairing | Result |
|---------|--------|
| `Polygon gap Polygon` | the shortest `Segment` from this polygon to the other — `start` on this polygon, `end` on the other — or `null` when the solid regions share any point, touching included |

The contract:

- **Disjoint solid regions.** Existence is the closed `intersects` gate of the `predicates`
  package, rounded at the `SCALE` quantum: overlapping or touching regions have no gap.
- **Oriented regions.** Each operand is its oriented solid region — a polygon within an
  inside-out one pairs to the loop from inside, and two inside-out polygons never have a gap.
- **No convexity required** of either operand.
- **Ties are arbitrary.** Parallel faces share the shortest distance; one pair is picked, and
  the displacement from `start` to `end` is the same for all of them. The segment itself stays
  raw.

# Package fromcrackpot.kintsugi.cga.d2.queries.measures

The measure tier: scalar Euclidean measurements answered raw — distances as non-negative
`Double`s, angles as signed `Angle`s.

| Pairing | Result |
|---------|--------|
| `FlatPoint distanceTo FlatPoint`<br>`RoundPoint distanceTo RoundPoint`<br>`FlatPoint distanceTo RoundPoint`<br>`RoundPoint distanceTo FlatPoint` | the raw distance between the positions |
| `FlatPoint distanceTo Line`<br>`RoundPoint distanceTo Line` | the raw perpendicular distance to the line |
| `Line angleTo Line` | the raw signed angle turning the first direction onto the second |

The family contract:

- **Distances are absolute.** A distance is non-negative; the signed sidedness of a point
  against a line keeps its one spelling in the dot family (`point dot line`).
- **Angles are signed and orientation-sensitive.** Counter-clockwise positive in `(−π, π]`;
  reversing either line's orientation offsets the answer by a half turn.
- **Positions only.** A sized or imaginary round point measures from its position — the
  conformal component takes no part. Circles and dipoles have no pairing here: a scalar
  against a genuinely round object is a conformal measurement, not a Euclidean one, and
  lives in the dot family.
- **Raw everywhere.** No `SCALE` rounding anywhere in the family — measures are values, not
  decisions.
- **Finite operands.** An operand at infinity has no Euclidean reading and the answer
  degenerates; gate with the `isAtInfinity` predicates.

# Package fromcrackpot.kintsugi.cga.d2.queries.predicates

The decision tier: predicates answering `Boolean` where the operator tier answers geometry —
incidence (`on`), containment (`contains`, spelled `in`/`!in`), and region overlap
(`intersects`). Every answer is rounded at the `SCALE` quantum.

Every predicate is closed — the boundary belongs to the shape:

| State | Spelling | Decision |
|---------|------------|----------|
| on | `p on c` | `round(dot) == 0` |
| inside | `p in c`<br>`c contains p` | `round(dot) >= 0` |
| outside | `p !in c`<br>`!(c contains p)` | `round(dot) < 0` |

so the three states partition exactly at the quantum and on implies inside (`p on c` implies
`p in c`). `intersects` is closed on the same terms, giving on-implies-intersects — the
derived families agree with `on` at the boundary by construction. The strict (open) interior
is deliberately unnamed — negation spells it: `p !in -c` holds exactly when `p` is strictly
inside `c`, because both orientations are closed and only the strict interior escapes the
complement.

## The `on` family

`on` tests whether a point lies on a shape's boundary.

| Pairing | True when |
|---------|-----------|
| `FlatPoint on Line`<br>`RoundPoint on Line` | the point lies on the line |
| `FlatPoint on Circle`<br>`RoundPoint on Circle` | the point lies on the circle — orientation-blind |
| `FlatPoint on Segment`<br>`RoundPoint on Segment` | the point lies on the segment itself, endpoints included |
| `FlatPoint on Polygon`<br>`RoundPoint on Polygon` | the point lies on any edge |

Precision is not uniform across the pairings: the Line, Segment, and Polygon forms are
all-linear and robust at the `SCALE` quantum; the Circle forms are quadratic, so the residual
grows with the radius and a point near a large circle's boundary may fail the test. Callers
wanting a "near" test need their own tolerance.

## The `contains` family

`contains` decides containment, boundary included. It is a Kotlin `operator fun`, so the
call-site spellings are the language's: `point in circle` reads inside-or-on, and
`point !in circle` the strict outside — neither inside nor on, the boundary belonging to the
shape. There are deliberately no `inside`/`outside` functions: one relation, one name, with
`in`/`!in` as its syntax.

| Pairing | `in` is true when |
|---------|-------------------|
| `Circle contains FlatPoint`<br>`Circle contains RoundPoint` | the point lies in the circle's interior or on it |
| `Circle contains Circle` | the other circle's region lies within the receiver's |
| `Polygon contains FlatPoint`<br>`Polygon contains RoundPoint` | the point lies within the polygon's solid region or on its boundary |

Every form is orientation-aware: an inside-out circle's interior is the unbounded region
outside it, and an inside-out polygon — wound clockwise — contains the closed complement of
its loop. The circle forms share `on`'s quadratic precision caveat. Classic polygon
containment follows the nonzero winding rule, so self-touching and self-crossing loops
classify by winding; the inside-out reading is its complement, boundary included either way.

## The `intersects` family

`intersects` tests region overlap — "not disjoint": two shapes intersect when they share at
least one point, touching included. Polygon operands count as closed regions (interior plus
boundary); segments and lines as their point sets. The predicate is symmetric, and cross-type
pairings come in both operand orders.

| Pairing | True when |
|---------|-----------|
| `Segment intersects Line`<br>`Line intersects Segment` | the line touches or crosses the segment |
| `Segment intersects Segment` | the segments touch, cross, or overlap |
| `Polygon intersects Line`<br>`Line intersects Polygon` | the line reaches the polygon's solid region — always, when the polygon is inside-out |
| `Segment intersects Polygon`<br>`Polygon intersects Segment` | the segment touches the boundary or lies within the solid region |
| `Polygon intersects Polygon` | the solid regions share any point — always, when both are inside-out |

A polygon operand is its oriented solid region: a classic polygon fills its loop, an
inside-out one the closed complement. Two inside-out polygons always intersect — both solid
regions are unbounded, and far enough out every direction belongs to both. The name is the
DE-9IM sense of `intersects` — not DE-9IM `overlaps`, which would exclude containment and
touching. The boolean answer is the whole product: the crossing geometry itself comes from
`meet`.

# Package fromcrackpot.kintsugi.cga.d2.transforms

The transform tier: the versors that move shapes — `Motor` (rotations and translations, built
by the `T` and `R` factories) and `Flector` (reflections and glide reflections, built by `F`
and `G`) — applied with `shape * versor` and composed with `versor * versor`. The per-shape
actions live in the `primitives` and `composites` subpackages; a receiver typed only as `Shape`
moves through the same spelling — the `Shape * Versor` dispatch delegates to the per-shape
actions, preserving the receiver's class.

## The factory family

Versors are built by four factories over plain Euclidean and primitive inputs — the half
coefficients of the versor representation stay inside the factories, so arguments are true
distances, angles, and mirror lines.

| Factory | Result |
|---------|--------|
| `T(dx, dy)`<br>`T(direction)` | the translating `Motor` — by `(dx, dy)`, or by a `Direction`'s free vector |
| `R(center, angle)` | the `Motor` rotating counter-clockwise by the angle about the center |
| `F(line)` | the `Flector` reflecting across the line |
| `G(line, glide)` | the `Flector` reflecting across the line, then gliding `glide` units along it |

Degenerate inputs: a mirror line at infinity throws `DegenerateFlectorException`; `R` about an
at-infinity center degrades to a translator, throwing `DegenerateMotorException` only at a
half-turn.

## The composition family

Versors compose with `*` into the single versor performing the left operand's motion first,
then the right's. Composition is exactly associative with the action,
`(u * A) * B == u * (A * B)`, so a stored composite motion behaves identically to chained
applications. Every versor is stored unitized and unitization survives composition, so
composing never throws.

| Pairing | Result |
|---------|--------|
| `Motor * Motor` | `Motor` — two rigid motions chain into one |
| `Flector * Flector` | `Motor` — the orientation flips cancel |
| `Flector * Motor`<br>`Motor * Flector` | `Flector` — one flip remains |
| `Versor * Versor` | `Versor` — the dispatch for operands typed as `Versor` |

# Package fromcrackpot.kintsugi.cga.d2.transforms.primitives

The action tier for the primitives: `shape * versor` applies a versor's motion to each of the
five primitives and returns the same primitive type — a `* Motor` form, a `* Flector` form,
and a `* Versor` dispatch per shape. Results carry full raw `Double` precision, and each form
is generic over the receiver's type — the moved geometry returns through the shape's `copy`
hook, so a subclass overriding it comes back as its own class. A motor's
rigid motion preserves orientation; a flector's mirror motion reverses it, and the oriented
types — `Line`, `Dipole`, `Circle` — record the reversal in their own components. Imaginary
and at-infinity operands transform
faithfully — first-class data, never an error — and no action on a primitive throws.

| Pairing | Result |
|---------|--------|
| `FlatPoint * Motor`<br>`FlatPoint * Flector` | the moved `FlatPoint`; at infinity — zero weight — it turns as a free vector |
| `Line * Motor`<br>`Line * Flector` | the moved `Line`, orientation carried; a flector genuinely mirrors it |
| `RoundPoint * Motor`<br>`RoundPoint * Flector` | the moved `RoundPoint`, squared radius kept |
| `Dipole * Motor`<br>`Dipole * Flector` | the dipole of the moved endpoints, `start` staying `start` |
| `Circle * Motor`<br>`Circle * Flector` | the moved `Circle`, radius kept; a flector flips classic ↔ inside-out |

# Package fromcrackpot.kintsugi.cga.d2.transforms.composites

The action tier for the composites: `shape * versor` moves a segment by its endpoints and a
polyline or polygon vertex-by-vertex, with the same `* Motor` / `* Flector` /
`* Versor` trio per shape. Results carry full raw `Double` precision, and each form is generic
over the receiver's type — the moved geometry returns through the shape's `copy` hook, so a
subclass overriding it comes back as its own class.

| Pairing | Result |
|---------|--------|
| `Segment * Motor`<br>`Segment * Flector` | the segment of the moved endpoints |
| `Polyline * Motor`<br>`Polyline * Flector` | the polyline of the moved vertices |
| `Polygon * Motor`<br>`Polygon * Flector` | the polygon of the moved vertices; a flector reverses the winding |

Degenerate inputs: a chain whose consecutive vertices lie within a `SCALE` quantum of each
other can come back rounded-coincident, throwing `DegeneratePolylineException` or
`DegeneratePolygonException`; `Segment * versor` never throws.

# Package fromcrackpot.kintsugi.cga.d2.constraints

The constraint tier: operations that solve for the motion satisfying a geometric relation and
return the shape already moved. The dual verbs `coerceOut` and `coerceIn` keep a polygon out
of — respectively inside — an oriented solid region, in the standard library's coerce posture:
an already-valid receiver comes back untouched, a violating one minimally moved — generic over
the receiver's type, so a `Polygon` subclass comes back as its own class. `coerceOn`, the equality
constraint beside them, rests a polygon against the region: where the two inequality verbs accept
every pose that does not violate, `coerceOn` accepts exactly the touching ones — resolved through
`collide` when the subject overlaps the region and through `gap` when it lies apart.

| Pairing | Result |
|---------|--------|
| `subject coerceOut collider` (classic collider — an obstacle) | the subject nudged out along the shallowest push-out, or untouched when they don't overlap |
| `subject coerceOut collider` (inside-out collider — a wall) | the subject retreated back inside the allowed interior, or untouched when it already lies inside |
| `subject coerceIn boundary` (classic boundary) | the subject retreated back inside the boundary's bounded interior, or untouched when it already lies inside |
| `subject coerceIn boundary` (inside-out boundary) | the subject nudged back into the complement of the boundary's loop, or untouched when it already lies there |
| `subject coerceOn support` (classic support) | the subject rested against the loop from outside: pushed out along the shallowest push-out when overlapping, drawn in along its shortest gap when apart, or untouched when touching |
| `subject coerceOn support` (inside-out support) | the subject rested against the loop's inner wall: retreated back inside when protruding, drawn out to the wall along its shortest gap when floating within, or untouched when touching |

The two verbs are duals through negation: `subject coerceIn boundary` lands the subject where
`subject coerceOut -boundary` does — staying inside a region is staying out of its complement —
each resolving its own query, `escape` and `collide` respectively. The penetration contract of
the `queries` package applies wholesale: convex loop on the queried polygon, one penetration
resolved per call — several simultaneous violations converge by coercing again — and touching
is not penetration. For `coerceOn` that contract binds the overlapping half alone — the separated
half, through `gap`, asks no convexity of either operand. The resolution is the nudge: a
translation along the penetration normal by the raw depth — or, for a separated `coerceOn`
subject, the translation closing its gap. A resolution of the caller's own is spelled on the query itself —
`(subject collide collider) { penetration -> … }` or `(subject escape boundary) { … }`.
