package fromcrackpot.kintsugi.cga

import fromcrackpot.kintsugi.cga.d2.shapes.Shape
import kotlin.math.round

/**
 * The rounding scale of the module's identity: values that round to the same multiple of
 * `1.0 / SCALE` compare equal.
 *
 * Components are stored raw at full `Double` precision; only identity rounds — equality,
 * hashing, and the module's boolean gates compare components rounded to the nearest
 * `1.0 / SCALE`, a quantum of `0.0001` of a unit. Import this constant wherever the quantum
 * is needed — a redeclared copy can drift from the identity it must match.
 */
public const val SCALE: Double = 10_000.0

/**
 * Rounds [value] to the nearest multiple of the identity quantum `1.0 / SCALE`.
 *
 * Identity in this module rounds — equality, hashing, and the boolean gates compare
 * components rounded to the nearest `1.0 / SCALE` (see [SCALE]); call this function to make
 * the same identity decision over any raw value — "does this shift round to zero?", "do
 * these values round equal?". The result is for deciding, never for storing: components stay
 * raw at full `Double` precision — store exact, round only for identity. Signed zero is
 * sanitized: `-0.0` rounds to `+0.0`, so a rounded zero carries no stray sign.
 *
 * @param value The raw value to round.
 * @return The nearest multiple of `1.0 / SCALE`; never `-0.0`.
 */
public fun round(value: Double): Double = round(value * SCALE) / SCALE + 0.0

/**
 * Delegate for object-allocating derived properties (`val center by cached { … }`).
 *
 * The module rule: scalar/trivial deriveds are `get() =` computed properties; only
 * initializers that allocate an object (or bridge into another type — where laziness is
 * structural, e.g. `toFlat`/`toRound` would recurse infinitely if eager) get a delegate.
 *
 * [LazyThreadSafetyMode.PUBLICATION] over `NONE`: first-to-publish wins, so all threads
 * observe a single instance — preserving the by-reference sharing invariants (Segment's
 * endpoint caching) under concurrent first access — with no lock
 * (a volatile read after init, same steady-state cost as the default `SYNCHRONIZED`).
 */
internal fun <T> cached(initializer: () -> T): Lazy<T> =
    lazy(LazyThreadSafetyMode.PUBLICATION, initializer)

/**
 * Narrows a `copy`-hook result back to the receiver's type — the single cast site of the
 * generic shape operations.
 *
 * The hook contract is that `copy` returns the receiver's own runtime class; this helper
 * enforces it, converting a missing override into an immediate named error instead of a
 * downstream `ClassCastException`.
 */
internal fun <T : Shape> T.narrowed(copied: Shape): T {
    check(copied::class == this::class) {
        "${this::class.simpleName}.copy returned ${copied::class.simpleName} — override copy in ${this::class.simpleName}"
    }
    @Suppress("UNCHECKED_CAST")
    return copied as T
}
