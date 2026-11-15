package fromcrackpot.kintsugi.scenegraph.style

import kotlin.jvm.JvmInline

/**
 * A 32-bit color packed as `0xAARRGGBB`, the renderer-neutral paint of the scene graph's
 * styles.
 *
 * - Equality compares raw bits, so transparent colors with different bits differ. [NONE] is "no paint".
 */
@JvmInline
public value class ShapeColor(
    /** The packed bits: alpha, red, green and blue, one byte each, alpha highest. */
    public val argb: ULong,
) {

    /** The alpha channel, `0` transparent to `255` opaque. */
    public val a: Int get() = ((argb shr 24) and 0xFFu).toInt()

    /** The red channel, `0..255`. */
    public val r: Int get() = ((argb shr 16) and 0xFFu).toInt()

    /** The green channel, `0..255`. */
    public val g: Int get() = ((argb shr 8) and 0xFFu).toInt()

    /** The blue channel, `0..255`. */
    public val b: Int get() = (argb and 0xFFu).toInt()

    public companion object {
        /**
         * Builds a color from `0..255` channel values. [a] defaults to fully opaque.
         *
         * - Out-of-range values wrap to their low 8 bits rather than throw.
         *
         * @param r The red channel, `0..255`.
         * @param g The green channel, `0..255`.
         * @param b The blue channel, `0..255`.
         * @param a The alpha channel, `0..255`, `0xFF` (opaque) by default.
         * @return The packed color.
         */
        public fun rgb(r: Int, g: Int, b: Int, a: Int = 0xFF): ShapeColor = ShapeColor(
            ((a.toULong() and 0xFFu) shl 24) or
                ((r.toULong() and 0xFFu) shl 16) or
                ((g.toULong() and 0xFFu) shl 8) or
                (b.toULong() and 0xFFu)
        )

        /** The canonical "no paint": fully transparent black, all bits zero. */
        public val NONE: ShapeColor = rgb(0, 0, 0, 0)

        /** Opaque black. */
        public val BLACK: ShapeColor = rgb(0, 0, 0)

        /** Opaque white. */
        public val WHITE: ShapeColor = rgb(255, 255, 255)

        /** Opaque pure green. */
        public val GREEN: ShapeColor = rgb(0, 255, 0)

        /** Opaque pure yellow. */
        public val YELLOW: ShapeColor = rgb(255, 255, 0)
    }
}
