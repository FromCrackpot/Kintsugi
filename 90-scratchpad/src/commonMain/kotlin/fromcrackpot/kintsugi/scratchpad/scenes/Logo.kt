package fromcrackpot.kintsugi.scratchpad.scenes

import fromcrackpot.kintsugi.cga.d2.products.meet
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Circle
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Line
import fromcrackpot.kintsugi.scenegraph.SceneBounds
import fromcrackpot.kintsugi.scenegraph.nodes.GroupNode
import fromcrackpot.kintsugi.scenegraph.nodes.composites.PolygonNode
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.CircleNode
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.DipoleNode
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.FlatPointNode
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.LineNode
import fromcrackpot.kintsugi.scenegraph.style.ShapeColor
import fromcrackpot.kintsugi.scenegraph.style.ShapeStyle

/** Scene half-extent — frames the floor 480×480 around the CGA origin, like the smiley. */
private const val EXTENT = 240.0
fun EXTENT(prc: Double): Double = (EXTENT * prc)

/**
 * Logo scene: a green circle and a diagonal line through the origin on a black square,
 * with the dipole where they meet in yellow.
 */
fun logoScene(): GroupNode = GroupNode(
    children = run {
        val bg = Polygon.Rectangle(FlatPoint(-EXTENT, -EXTENT), FlatPoint(EXTENT, EXTENT))
        val center = FlatPoint(0.0, 0.0)
        val circle = Circle(0.0, 0.0, EXTENT(0.50))
        val line = Line(1.0, -1.0, 0.0)
        val dipole = line meet circle

        listOf(
            PolygonNode(bg, ShapeStyle.POLYGON.copy(fill = ShapeColor.BLACK)),
            LineNode(line, ShapeStyle.LINE.copy(stroke = ShapeColor.GREEN, strokeWidth = EXTENT(0.02))),
            CircleNode(
                circle,
                ShapeStyle.CIRCLE.copy(stroke = ShapeColor.GREEN, fill = ShapeColor.BLACK, strokeWidth = EXTENT(0.02))
            ),
            FlatPointNode(center, ShapeStyle.POINT.copy(fill = ShapeColor.GREEN, size = EXTENT(0.05))),
            DipoleNode(dipole, ShapeStyle.DIPOLE.copy(fill = ShapeColor.YELLOW, size = EXTENT(0.07))),
        )
    },
    bounds = SceneBounds(-EXTENT, -EXTENT, 2 * EXTENT, 2 * EXTENT),
)

