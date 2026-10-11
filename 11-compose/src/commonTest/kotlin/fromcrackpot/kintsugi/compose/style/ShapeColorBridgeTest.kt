package fromcrackpot.kintsugi.compose.style

import androidx.compose.ui.graphics.Color
import fromcrackpot.kintsugi.scenegraph.style.ShapeColor
import kotlin.test.Test
import kotlin.test.assertEquals

class ShapeColorBridgeTest {

    @Test
    fun `opaque rgb round-trips byte-exact through Compose Color`() {
        val cases = listOf(
            ShapeColor.rgb(0, 0, 0),
            ShapeColor.rgb(255, 255, 255),
            ShapeColor.rgb(255, 0, 0),
            ShapeColor.rgb(0, 128, 255),
            ShapeColor.rgb(17, 34, 51),
        )
        for (c in cases) assertEquals(c, c.toComposeColor().toShapeColor())
    }

    @Test
    fun `alpha round-trips byte-exact through Compose Color`() {
        val cases = listOf(
            ShapeColor.rgb(255, 0, 0, a = 0x80),
            ShapeColor.rgb(17, 34, 51, a = 0x44),
            ShapeColor.rgb(0, 0, 0, a = 0x00),
        )
        for (c in cases) assertEquals(c, c.toComposeColor().toShapeColor())
    }

    @Test
    fun `fractional float channels convert via toArgb rounding, not truncation`() {
        // 0.5f · 255 = 127.5 must round to 128 — the "simplified" (f * 255).toInt() bridge
        // would truncate to 127. Pins the toArgb() path the module gotcha warns about.
        val c = Color(red = 0.5f, green = 0.25f, blue = 0.75f, alpha = 0.5f)
        assertEquals(ShapeColor.rgb(128, 64, 191, a = 128), c.toShapeColor())
    }
}
