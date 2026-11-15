package fromcrackpot.kintsugi.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.unit.Density
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.scenegraph.SceneBounds
import fromcrackpot.kintsugi.scenegraph.nodes.GroupNode
import fromcrackpot.kintsugi.scenegraph.nodes.composites.PolygonNode
import fromcrackpot.kintsugi.scenegraph.style.ShapeColor
import fromcrackpot.kintsugi.scenegraph.style.ShapeStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val BOUNDS = SceneBounds(Polygon.Rectangle(FlatPoint(-50.0, -50.0), FlatPoint(50.0, 50.0)))

private fun square(minX: Double, color: ShapeColor): PolygonNode = PolygonNode(
    Polygon.Rectangle(FlatPoint(minX, -15.0), FlatPoint(minX + 30.0, 15.0)),
    ShapeStyle.POLYGON.copy(fill = color, stroke = ShapeColor.NONE),
)

private var comparing = false

// Counts draw passes. Each reads the leaf's style once; reads inside `equals` (Compose's skip check) don't count.
private class CountingSquare(minX: Double, color: ShapeColor) : PolygonNode(square(minX, color)) {
    var draws = 0
    override val style: ShapeStyle get() = super.style.also { if (!comparing) draws++ }

    override fun equals(other: Any?): Boolean {
        comparing = true
        return try { super.equals(other) } finally { comparing = false }
    }

    override fun hashCode(): Int = super.hashCode()
}

private fun imageScene(content: @Composable () -> Unit) = ImageComposeScene(100, 100, Density(1f), content = content)

private inline fun ImageComposeScene.use(block: (ImageComposeScene) -> Unit) = try {
    block(this)
} finally {
    close()
}

class SceneTest {

    /* LAYERS */

    @Test
    fun `an unchanged child replays its layer while a replaced sibling redraws`() {
        val left = CountingSquare(-40.0, ShapeColor(0xFFFF0000uL))
        var right by mutableStateOf(square(10.0, ShapeColor(0xFF0000FFuL)))
        imageScene { Scene(GroupNode(listOf(left, right), BOUNDS)) }.use { scene ->
            scene.render()
            val drawsBefore = left.draws
            assertTrue(drawsBefore > 0, "the first frame draws every layer")

            right = square(10.0, ShapeColor(0xFF00FF00uL))
            Snapshot.sendApplyNotifications()
            val pixels = scene.render().toComposeImageBitmap().toPixelMap()

            assertEquals(Color(0xFF00FF00), pixels[75, 50], "the replaced child redrew")
            assertEquals(Color(0xFFFF0000), pixels[25, 50], "the unchanged child still shows")
            assertEquals(drawsBefore, left.draws, "the unchanged child's layer was replayed, not redrawn")
        }
    }
}
