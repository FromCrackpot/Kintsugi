package fromcrackpot.kintsugi.cga

import fromcrackpot.kintsugi.cga.d2.shapes.Shape
import kotlin.math.round

/**
 * The rounding scale of the module's identity. Values that round to the same multiple of
 * `1.0 / SCALE` compare equal.
 *
 * - Components are stored raw. Only equality, hashing and the boolean gates round.
 * - Import it wherever the quantum is needed. A redeclared copy can drift from the identity.
 */
public const val SCALE: Double = 10_000.0

/**
 * Rounds [value] to the nearest multiple of `1.0 / SCALE`. Use it for identity decisions, never for
 * storing.
 *
 * @param value The raw value to round.
 * @return The nearest multiple of `1.0 / SCALE`; never `-0.0`.
 */
public fun round(value: Double): Double = round(value * SCALE) / SCALE + 0.0

/**
 * Delegate for object-allocating derived properties (`val center by cached { … }`).
 *
 * - Scalar and trivial deriveds stay `get() =`. Only initializers that allocate, or bridge into
 *   another type, get a delegate. Bridges must be lazy: an eager `toFlat`/`toRound` recurses forever.
 * - [LazyThreadSafetyMode.PUBLICATION] over `NONE`: the first to publish wins, so every thread sees
 *   one instance. That keeps the by-reference sharing invariants (Segment's endpoints) under
 *   concurrent first access.
 * - No lock: after init it costs one volatile read, like the default `SYNCHRONIZED`.
 */
internal fun <T> cached(initializer: () -> T): Lazy<T> =
    lazy(LazyThreadSafetyMode.PUBLICATION, initializer)

/**
 * Narrows a `copy`-hook result back to the receiver's type. It is the only cast site of the
 * generic shape operations.
 *
 * - Erasure makes the cast check only the bound. Without the class check, a missing `copy`
 *   override escapes typed as `T`, then silently sheds its subclass wherever no cast follows.
 */
internal fun <T : Shape> T.narrowed(copied: Shape): T {
    check(copied::class == this::class) {
        "${this::class.simpleName}.copy returned ${copied::class.simpleName}, not its own class"
    }
    @Suppress("UNCHECKED_CAST")
    return copied as T
}
