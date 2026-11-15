package fromcrackpot.kintsugi.cga.d2.transforms

/**
 * Umbrella over the motions applied to shapes with `*`: [Motor] (rotations and translations) and
 * [Flector] (reflections and glide reflections).
 *
 * - `A * B` applies `A` first, then `B`, and `(u * A) * B == u * (A * B)`.
 * - A versor and its negation apply the identical motion yet compare unequal as values.
 * - A versor only ever sits on the right: `shape * versor`, never `versor * shape`.
 */
public sealed interface Versor

/*
COMPOSITION
Bodies are `next ⟇ this` expanded against the geometric antiproduct's Cayley table. The flat/line
weight norm is multiplicative across the whole group, so every result ctor's unitization is a
no-op pass and the Degenerate* throws are unreachable.
 */

/**
 * `Motor * Motor => Motor`<br>
 * Returns the single motor performing this motion, then [next]'s.
 *
 * - Angles summing past a full turn come back as the negated motor. The motion is the same; the value is not.
 *
 * @receiver The motor applied first.
 * @param next The motor applied after this one.
 * @return The composite motor.
 */
public operator fun Motor.times(next: Motor): Motor = Motor(
    mx = next.mx * this.mw + next.my * this.mz - next.mz * this.my + next.mw * this.mx, // e41
    my = next.my * this.mw - next.mx * this.mz + next.mz * this.mx + next.mw * this.my, // e42
    mz = next.mz * this.mw + next.mw * this.mz,                                         // e43
    mw = next.mw * this.mw - next.mz * this.mz,                                         // 𝟙
)

/**
 * `Flector * Flector => Motor`<br>
 * Returns the single motor performing this motion, then [next]'s. Two mirrors chain into a rigid
 * motion.
 *
 * @receiver The flector applied first.
 * @param next The flector applied after this one.
 * @return The composite motor: a rotation for crossing mirrors, a translation for parallel ones.
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
 * @receiver The flector applied first.
 * @param next The motor applied after this one.
 * @return The composite flector.
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
 * @receiver The motor applied first.
 * @param next The flector applied after this one.
 * @return The composite flector.
 */
public operator fun Motor.times(next: Flector): Flector = Flector(
    fx = next.fx * this.mw + next.fy * this.mz,                                           // e423
    fy = next.fy * this.mw - next.fx * this.mz,                                           // e431
    fz = next.fx * this.my - next.fy * this.mx + next.fz * this.mw + next.fw * this.mz,   // e412
    fw = -next.fx * this.mx - next.fy * this.my - next.fz * this.mz + next.fw * this.mw,  // e4
)

/**
 * `Versor * Versor => Versor`<br>
 * Returns the single versor performing this motion, then [next]'s, for operands typed only as
 * [Versor].
 *
 * @receiver The versor applied first.
 * @param next The versor applied after this one.
 * @return A [Motor] for two motors or two flectors, a [Flector] for a mixed pair.
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
