package fromcrackpot.kintsugi.compose.style

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import fromcrackpot.kintsugi.scenegraph.style.ShapeColor

/**
 * Returns this color as the equivalent Compose `Color`, in sRGB.
 *
 * The conversion is channel-exact: the packed `0..255` channels feed Compose's integer
 * constructor unchanged.
 *
 * @receiver The renderer-neutral color to bridge.
 * @return The matching sRGB Compose `Color`.
 */
public fun ShapeColor.toComposeColor(): Color = Color(red = r, green = g, blue = b, alpha = a)

/**
 * Returns this Compose `Color` as the equivalent [ShapeColor].
 *
 * The conversion goes through the color's packed ARGB form: a color in a non-sRGB working
 * space converts to sRGB, and each channel rounds to the nearest 8-bit value. For sRGB colors
 * built from integer channels the round-trip with [toComposeColor] is exact.
 *
 * @receiver The Compose color to bridge.
 * @return The matching packed [ShapeColor].
 */
public fun Color.toShapeColor(): ShapeColor {
    val argb = toArgb()
    return ShapeColor.rgb(
        r = (argb shr 16) and 0xFF,
        g = (argb shr 8) and 0xFF,
        b = argb and 0xFF,
        a = (argb shr 24) and 0xFF,
    )
}
