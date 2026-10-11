package fromcrackpot.kintsugi.svg

import fromcrackpot.kintsugi.cga.d2.shapes.Shape
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polyline
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Segment
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Circle
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Dipole
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Line
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.RoundPoint
import fromcrackpot.kintsugi.scenegraph.nodes.ShapeNode
import fromcrackpot.kintsugi.scenegraph.nodes.composites.CompositeNode
import fromcrackpot.kintsugi.scenegraph.nodes.composites.PolygonNode
import fromcrackpot.kintsugi.scenegraph.nodes.composites.PolylineNode
import fromcrackpot.kintsugi.scenegraph.nodes.composites.SegmentNode
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.CircleNode
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.DipoleNode
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.FlatPointNode
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.LineNode
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.PrimitiveNode
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.RoundPointNode

// Test-side decoration shedding: mint a fresh plain shape so equality crosses the
// exact-class divide (`node.shape == shape`). Casts don't cross it — equals reads the
// runtime class — hence the mint. One base-typed extension only: a leaf-typed sibling
// would be ambiguous at leaf-typed sites (properties don't disambiguate by receiver).
val ShapeNode.shape: Shape
    get() = when (this) {
        is PrimitiveNode -> when (this) {
            is FlatPointNode -> FlatPoint(px, py, pz)
            is LineNode -> Line(gx, gy, gz)
            is RoundPointNode -> RoundPoint(ax, ay, az, aw)
            is DipoleNode -> Dipole(gx, gy, gz, px, py, pz)
            is CircleNode -> Circle(cx, cy, cz, cw)
        }

        is CompositeNode -> when (this) {
            is SegmentNode -> Segment(start, end)
            is PolylineNode -> Polyline(vertices)
            is PolygonNode -> Polygon(vertices)
        }

        else -> error("Unknown ShapeNode family: ${this::class}")
    }
