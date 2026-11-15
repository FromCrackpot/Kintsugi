package fromcrackpot.kintsugi.scratchpad.sketches

import androidx.compose.ui.input.key.Key
import fromcrackpot.kintsugi.cga.d2.operations.queries.measures.distanceTo
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.transforms.D
import fromcrackpot.kintsugi.cga.d2.transforms.composites.times
import fromcrackpot.kintsugi.gameengine.Step
import fromcrackpot.kintsugi.gameengine.World
import fromcrackpot.kintsugi.gameengine.restart
import fromcrackpot.kintsugi.scenegraph.SceneBounds
import fromcrackpot.kintsugi.scenegraph.nodes.GroupNode
import fromcrackpot.kintsugi.scenegraph.nodes.backdrop
import fromcrackpot.kintsugi.scenegraph.nodes.composites.PolygonNode
import fromcrackpot.kintsugi.scenegraph.style.ShapeColor
import fromcrackpot.kintsugi.scenegraph.style.ShapeStyle
import kotlin.math.exp

/* TUNING */

private const val EXTENT = 240.0
private const val COLS = 6
private const val ROWS = 2
private const val TILE = 10.0
private const val BURST_RATE = 20.0
private const val DRAG = 1.0
private const val ERODE_LENGTH = 40.0
private const val MIN_TILE = 0.5

private val TILE_STYLE = ShapeStyle.POLYGON.copy(fill = ShapeColor.rgb(255, 165, 0), stroke = ShapeColor.NONE)

private fun spread(t: Double): Double = 1 + BURST_RATE / DRAG * (1 - exp(-DRAG * t))

/* BOOT */

/** A COLS×ROWS brick of flush TILE squares, centered on the origin. */
fun brick(): List<PolygonNode> = (0 until ROWS).flatMap { row ->
    (0 until COLS).map { col ->
        val x = (col - COLS / 2.0) * TILE
        val y = (row - ROWS / 2.0) * TILE
        PolygonNode(Polygon.Rectangle(FlatPoint(x, y), FlatPoint(x + TILE, y + TILE)), TILE_STYLE)
    }
}

/* WORLD */

/** Dilation playground: space starts/pauses a looping blast of the brick's squares, R restarts. */
class Boom(
    internal var fragments: List<PolygonNode>,
) : World(SceneBounds(-EXTENT, -EXTENT, 2 * EXTENT, 2 * EXTENT)) {

    internal var running = false
    internal var elapsed = 0.0

    override val scene get() = GroupNode(listOf(backdrop(bounds)) + fragments)

    context(step: Step)
    override fun update() {
        if (step.input.keys.wasPressed(Key.R)) restart()
        if (step.input.keys.wasPressed(Key.Spacebar)) running = !running
        if (!running) return

        // σ(t + dt)/σ(t): the per-step ratios telescope, so the blast is exact however dt splits
        val sigma = spread(elapsed + step.dt) / spread(elapsed)
        elapsed += step.dt

        val c = FlatPoint(fragments.map { it.centroid.px }.average(), fragments.map { it.centroid.py }.average())
        // size × exp(−traveled/L), the inner D's 1/σ cancelling D(c, σ)'s zoom; tiny squares culled before D/Polygon throw
        fragments = fragments.map {
            val traveled = (sigma - 1) * (it.centroid distanceTo c)
            it * D(it.centroid, exp(-traveled / ERODE_LENGTH) / sigma) * D(c, sigma)
        }.filter { it.edges[0].length > MIN_TILE }

        if (fragments.isEmpty()) {
            fragments = brick()
            elapsed = 0.0
        }
    }
}
