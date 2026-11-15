package fromcrackpot.kintsugi.compose.style

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import fromcrackpot.kintsugi.scenegraph.style.ShapeColor

/**
 * Returns this color as the equivalent sRGB Compose `Color`, channel-exact.
 *
 * @receiver The renderer-neutral color to bridge.
 * @return The matching sRGB Compose `Color`.
 */
public fun ShapeColor.toComposeColor(): Color = Color(red = r, green = g, blue = b, alpha = a)

/**
 * Returns this Compose `Color` as the equivalent [ShapeColor].
 *
 * - Non-sRGB colors convert to sRGB and each channel rounds to 8 bits; sRGB integer colors round-trip.
 *
 * @receiver The Compose color to bridge.
 * @return The matching packed color.
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
