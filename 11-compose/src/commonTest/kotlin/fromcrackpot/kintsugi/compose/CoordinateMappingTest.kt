package fromcrackpot.kintsugi.compose

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.scenegraph.SceneBounds
import fromcrackpot.kintsugi.scenegraph.worldBorder
import kotlin.test.Test
import kotlin.test.assertEquals

class CoordinateMappingTest {

    private val size = Size(200f, 200f)

    /* FORWARD */

    @Test
    fun `cgaToCanvas centers the origin and flips Y`() {
        // Same example DrawTest pins: CGA (30, −20) @ 200×200 → canvas (130, 120).
        assertEquals(Offset(100f, 100f), cgaToCanvas(0.0, 0.0, size))
        assertEquals(Offset(130f, 120f), cgaToCanvas(30.0, -20.0, size))
    }

    /* INVERSE */

    @Test
    fun `canvasToCga centers the origin and flips Y`() {
        assertEquals(FlatPoint(0.0, 0.0), canvasToCga(Offset(100f, 100f), size))
        assertEquals(FlatPoint(30.0, -20.0), canvasToCga(Offset(130f, 120f), size))
    }

    /* ROUND-TRIP */

    @Test
    fun `canvasToCga inverts cgaToCanvas`() {
        val there = cgaToCanvas(87.5, -33.25, size)
        assertEquals(FlatPoint(87.5, -33.25), canvasToCga(there, size))
    }

    @Test
    fun `cgaToCanvas inverts canvasToCga`() {
        val position = Offset(17f, 181f)
        val point = canvasToCga(position, size)
        assertEquals(position, cgaToCanvas(point.px, point.py, size))
    }

    /* SCALE */

    @Test
    fun `scale scales about the center so the origin stays put`() {
        assertEquals(Offset(100f, 100f), cgaToCanvas(0.0, 0.0, size, 2f))
        assertEquals(FlatPoint(0.0, 0.0), canvasToCga(Offset(100f, 100f), size, 2f))
        // CGA (30, −20) @ scale 2 → twice as far from center as the scale-1 (130, 120).
        assertEquals(Offset(160f, 140f), cgaToCanvas(30.0, -20.0, size, 2f))
    }

    @Test
    fun `canvasToCga inverts cgaToCanvas at fractional scale`() {
        val there = cgaToCanvas(30.0, -20.0, size, 1.25f)
        assertEquals(FlatPoint(30.0, -20.0), canvasToCga(there, size, 1.25f))
    }

    /* FIT SCALE */

    private val bounds = SceneBounds(-400.0, -300.0, 800.0, 600.0)

    @Test
    fun `fitScale scales exactly when surface matches bounds aspect`() {
        assertEquals(1f, fitScale(Size(800f, 600f), bounds, 1.25f))
        assertEquals(2f, fitScale(Size(1600f, 1200f), bounds, 1.25f))
    }

    @Test
    fun `fitScale is width-limited on a taller surface`() {
        // 800×1200 surface: width ratio 1, height ratio 2 → min is 1.
        assertEquals(1f, fitScale(Size(800f, 1200f), bounds, 1.25f))
    }

    @Test
    fun `fitScale is height-limited on a wider surface`() {
        // 2400×600 surface: width ratio 3, height ratio 1 → min is 1.
        assertEquals(1f, fitScale(Size(2400f, 600f), bounds, 1.25f))
    }

    @Test
    fun `fitScale with null bounds falls back to the display density`() {
        assertEquals(1.25f, fitScale(Size(800f, 600f), null, 1.25f))
    }

    /* WORLD BORDER */

    @Test
    fun `worldBorder with bounds is the bounds' circumscribed circle, surface ignored`() {
        // 800×600 bounds: half-diagonal = sqrt(400² + 300²) = 500, centered at CGA origin.
        // Surface size and scale must not leak in — same circle at any surface.
        val expected = worldBorder(800.0, 600.0)
        assertEquals(expected, worldBorder(Size(800f, 600f), bounds, 1f))
        assertEquals(expected, worldBorder(Size(2400f, 600f), bounds, 1f))
        assertEquals(expected, worldBorder(Size(1600f, 1200f), bounds, 2f))
        assertEquals(500.0, expected.radius)
    }

    @Test
    fun `worldBorder with bounds ignores minX and minY offsets`() {
        // Border is always origin-centered — offset bounds give the same circle (scene-graph convention).
        assertEquals(
            worldBorder(size, bounds, 1f),
            worldBorder(size, SceneBounds(15.0, -7.0, 800.0, 600.0), 1f),
        )
    }

    @Test
    fun `worldBorder with null bounds circumscribes the viewport in CGA units`() {
        // 800×600 px surface at scale 2 → 400×300 CGA-unit viewport → half-diagonal 250.
        assertEquals(worldBorder(400.0, 300.0), worldBorder(Size(800f, 600f), null, 2f))
        // Scale 1: viewport in CGA units equals the pixel size (the trace-path case).
        assertEquals(worldBorder(800.0, 600.0), worldBorder(Size(800f, 600f), null, 1f))
    }
}
