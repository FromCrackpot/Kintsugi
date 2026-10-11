package fromcrackpot.kintsugi.svg.serializing

/**
 * Format a Double as an SVG attribute value at full Double precision.
 *
 * Per the Kintsugi tenet "store exact, round only for identity", persistence keeps full
 * Double precision; canonical equality is applied by `:cga`'s rounded `equals` at load
 * time, not by truncating the file on save. The serializer must NOT pre-quantize, or
 * derived values (e.g. `Line meet Circle` endpoints, irrational radii) lose information
 * that downstream math relies on.
 *
 * Output rules: integer values emit no decimal point; signed zero collapses to "0";
 * otherwise the shortest round-trippable representation from `Double.toString`.
 *
 * Caveat: `Double.toString` is platform-divergent in KMP (JVM `"1.0E7"` vs wasmJs
 * `"10000000"`) — both are valid SVG numbers and Double-round-trip safe, but byte-identical
 * output across targets is NOT guaranteed. Don't golden-file or hash serializer output
 * cross-platform.
 */
internal fun Double.formatSvgNumber(): String {
    require(isFinite()) { "Cannot format non-finite double: $this" }
    val sanitized = this + 0.0 // collapse -0.0 to 0.0
    val asLong = sanitized.toLong()
    if (asLong.toDouble() == sanitized) return asLong.toString()
    return sanitized.toString()
}
