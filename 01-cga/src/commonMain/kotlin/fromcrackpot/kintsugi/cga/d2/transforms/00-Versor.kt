package fromcrackpot.kintsugi.cga.d2.transforms

/**
 * Umbrella over the motion types applied to shapes with `*` — [Motor] (rotations and
 * translations) and [Flector] (reflections and glide reflections).
 *
 * A versor is anything that acts by the antiproduct sandwich: `shape * V` applies
 * `V ⟇ u ⟇ V̰` (with the leading minus for the odd [Flector]). The multiplicative spelling is
 * the standard notation for a right group action, **not** the algebra's one-sided product `u ⟇ V`;
 * a versor never appears on the *left* of a shape — `V * shape` does not exist — so the
 * spelling cannot be mistaken for the raw antiproduct.
 *
 * `A * B` composes versors, **left operand applied first** (composition does not commute —
 * the order is the contract), computed as the antiproduct `B ⟇ A`, since
 * `(B⟇A) ⟇ u ⟇ (B⟇A)̰ = B ⟇ (A⟇u⟇Ã) ⟇ B̰` — the inner sandwich `A` runs first. Together these
 * make `*` exactly associative across the whole surface: `(u * A) * B == u * (A * B)` — a
 * stored composite motion behaves identically to chained applications, which is why the
 * action shares its symbol with composition.
 *
 * The sandwich is even in the versor: `V` and `−V` apply the identical motion, though they
 * compare unequal as values.
 *
 * Sealed so the per-shape `* Versor` dispatch overloads and the `Versor * Versor` composition
 * can be exhaustive `when`s.
 */
public sealed interface Versor

/*
COMPOSITION — bodies are `next ⟇ this` expanded against the geometric antiproduct's Cayley table.
The flat/line weight norm is multiplicative across the whole group, so every result
ctor's unitization is a no-op pass and the Degenerate* throws are unreachable.
 */

/**
 * `Motor * Motor => Motor`<br>
 * Returns the single motor performing this motion, then [next]'s.
 *
 * Proper composed with proper stays proper: two rigid motions chain into one rigid motion.
 * Rotation angles add through the half-angle representation, so compositions whose angles sum
 * past a full turn come back as the negated motor — the identical motion, comparing unequal as
 * a value (see [Versor]).
 *
 * @receiver The motor applied first.
 * @param next The motor applied after this one.
 * @return The composite [Motor].
 */
public operator fun Motor.times(next: Motor): Motor = Motor(
    mx = next.mx * this.mw + next.my * this.mz - next.mz * this.my + next.mw * this.mx, // e41
    my = next.my * this.mw - next.mx * this.mz + next.mz * this.mx + next.mw * this.my, // e42
    mz = next.mz * this.mw + next.mw * this.mz,                                         // e43
    mw = next.mw * this.mw - next.mz * this.mz,                                         // 𝟙
)

/**
 * `Flector * Flector => Motor`<br>
 * Returns the single motor performing this motion, then [next]'s.
 *
 * Improper composed with improper is proper: the orientation flips cancel, and two mirror
 * motions chain into a rigid one — a rotation by twice the angle between crossing mirror
 * lines, a translation when they are parallel. The same mirror line taken with opposite
 * orientations composes to the negated identity motor — the identity motion, comparing
 * unequal as a value (see [Versor]).
 *
 * @receiver The flector applied first.
 * @param next The flector applied after this one.
 * @return The composite [Motor].
 */
public operator fun Flector.times(next: Flector): Motor = Motor(
    mx = -next.fx * this.fw - next.fy * this.fz + next.fz * this.fy - next.fw * this.fx, // e41
    my = next.fx * this.fz - next.fy * this.fw - next.fz * this.fx - next.fw * this.fy,  // e42
    mz = next.fy * this.fx - next.fx * this.fy,                                          // e43
    mw = next.fx * this.fx + next.fy * this.fy,                                          // 𝟙
)

/**
 * `Flector * Motor => Flector`<br>
 * Returns the single flector performing this motion, then [next]'s.
 *
 * Improper composed with proper stays improper: the mirror's orientation flip survives the
 * rigid motion, so the composite is again a transflection.
 *
 * @receiver The flector applied first.
 * @param next The motor applied after this one.
 * @return The composite [Flector].
 */
public operator fun Flector.times(next: Motor): Flector = Flector(
    fx = next.mw * this.fx - next.mz * this.fy,                                           // e423
    fy = next.mz * this.fx + next.mw * this.fy,                                           // e431
    fz = next.mx * this.fy - next.my * this.fx + next.mz * this.fw + next.mw * this.fz,   // e412
    fw = -next.mx * this.fx - next.my * this.fy - next.mz * this.fz + next.mw * this.fw,  // e4
)

/**
 * `Motor * Flector => Flector`<br>
 * Returns the single flector performing this motion, then [next]'s.
 *
 * Proper composed with improper is improper: the rigid motion feeds into the mirror and the
 * orientation flip remains, so the composite is again a transflection.
 *
 * @receiver The motor applied first.
 * @param next The flector applied after this one.
 * @return The composite [Flector].
 */
public operator fun Motor.times(next: Flector): Flector = Flector(
    fx = next.fx * this.mw + next.fy * this.mz,                                           // e423
    fy = next.fy * this.mw - next.fx * this.mz,                                           // e431
    fz = next.fx * this.my - next.fy * this.mx + next.fz * this.mw + next.fw * this.mz,   // e412
    fw = -next.fx * this.mx - next.fy * this.my - next.fz * this.mz + next.fw * this.mw,  // e4
)

/**
 * `Versor * Versor => Versor`<br>
 * Returns the single versor performing this motion, then [next]'s.
 *
 * The composition for operands typed only as [Versor]: it dispatches to the concrete pairing,
 * so two motors or two flectors compose into a [Motor] and a mixed pair into a [Flector].
 * Operands with concrete static types resolve to those forms directly and keep their precise
 * return types.
 *
 * @receiver The versor applied first.
 * @param next The versor applied after this one.
 * @return The composite versor — a [Motor] for two motors or two flectors, a [Flector] for a
 *   mixed pair.
 */
public operator fun Versor.times(next: Versor): Versor = when (this) {
    is Motor -> when (next) {
        is Motor -> this * next
        is Flector -> this * next
    }
    is Flector -> when (next) {
        is Motor -> this * next
        is Flector -> this * next
    }
}
