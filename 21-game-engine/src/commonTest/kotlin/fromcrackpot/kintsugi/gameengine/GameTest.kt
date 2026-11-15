package fromcrackpot.kintsugi.gameengine

import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection.Ltr
import androidx.compose.ui.unit.LayoutDirection.Rtl
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.scenegraph.SceneBounds
import kotlin.test.Test
import kotlin.test.assertEquals

class GameTest {

    private val bounds = SceneBounds(-400.0, -300.0, 800.0, 600.0)
    private val wide = IntSize(1600, 600)

    /* SCENE RECT */

    @Test
    fun `the scene is fitted and centred by default`() {
        assertEquals(IntRect(400, 0, 1200, 600), sceneRect(wide, bounds, 1f, Alignment.Center, Ltr))
    }

    @Test
    fun `a left alignment puts the whole letterbox on the right`() {
        assertEquals(IntRect(0, 0, 800, 600), sceneRect(wide, bounds, 1f, AbsoluteAlignment.CenterLeft, Ltr))
    }

    @Test
    fun `an absolute alignment ignores the layout direction - a relative one follows it`() {
        assertEquals(IntRect(0, 0, 800, 600), sceneRect(wide, bounds, 1f, AbsoluteAlignment.CenterLeft, Rtl))
        assertEquals(IntRect(800, 0, 1600, 600), sceneRect(wide, bounds, 1f, Alignment.CenterStart, Rtl))
    }

    @Test
    fun `a taller surface aligns the scene vertically`() {
        assertEquals(IntRect(0, 0, 800, 600), sceneRect(IntSize(800, 1200), bounds, 1f, Alignment.TopCenter, Ltr))
    }

    @Test
    fun `with no bounds the scene is the whole surface`() {
        assertEquals(IntRect(IntOffset.Zero, wide), sceneRect(wide, null, 2f, AbsoluteAlignment.CenterLeft, Ltr))
    }

    /* SURFACE TO CGA */

    @Test
    fun `a centred scene maps the surface centre to the origin`() {
        assertEquals(FlatPoint(0.0, 0.0), surfaceToCga(Offset(800f, 300f), wide, bounds, 1f, Alignment.Center, Ltr))
    }

    @Test
    fun `a left-aligned scene maps its own corners to the corners of the bounds`() {
        val left = AbsoluteAlignment.CenterLeft
        assertEquals(FlatPoint(-400.0, 0.0), surfaceToCga(Offset(0f, 300f), wide, bounds, 1f, left, Ltr))
        assertEquals(FlatPoint(400.0, 300.0), surfaceToCga(Offset(800f, 0f), wide, bounds, 1f, left, Ltr))
    }

    @Test
    fun `the letterbox maps past the bounds on the same scale`() {
        val left = AbsoluteAlignment.CenterLeft
        assertEquals(FlatPoint(1000.0, 0.0), surfaceToCga(Offset(1400f, 300f), wide, bounds, 1f, left, Ltr))
    }

    @Test
    fun `a scaled left-aligned scene divides by the fit scale`() {
        val surface = IntSize(3200, 1200)
        val left = AbsoluteAlignment.CenterLeft
        assertEquals(FlatPoint(400.0, 300.0), surfaceToCga(Offset(1600f, 0f), surface, bounds, 1f, left, Ltr))
        assertEquals(FlatPoint(1200.0, -300.0), surfaceToCga(Offset(3200f, 1200f), surface, bounds, 1f, left, Ltr))
    }

    @Test
    fun `with no bounds positions map through the display density`() {
        val surface = IntSize(200, 200)
        val left = AbsoluteAlignment.CenterLeft
        assertEquals(FlatPoint(0.0, 0.0), surfaceToCga(Offset(100f, 100f), surface, null, 2f, left, Ltr))
        assertEquals(FlatPoint(30.0, -20.0), surfaceToCga(Offset(160f, 140f), surface, null, 2f, left, Ltr))
    }
}
