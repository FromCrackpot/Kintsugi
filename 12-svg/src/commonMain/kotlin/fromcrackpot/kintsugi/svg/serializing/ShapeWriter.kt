package fromcrackpot.kintsugi.svg.serializing

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Circle
import fromcrackpot.kintsugi.scenegraph.SceneBounds
import fromcrackpot.kintsugi.scenegraph.nodes.GroupNode
import fromcrackpot.kintsugi.scenegraph.nodes.Node
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
import fromcrackpot.kintsugi.svg.serializing.composites.writePolygonElement
import fromcrackpot.kintsugi.svg.serializing.composites.writePolylineElement
import fromcrackpot.kintsugi.svg.serializing.composites.writeSegmentElement
import fromcrackpot.kintsugi.svg.serializing.primitives.writeCircleElement
import fromcrackpot.kintsugi.svg.serializing.primitives.writeDipoleGroup
import fromcrackpot.kintsugi.svg.serializing.primitives.writeFlatPointRect
import fromcrackpot.kintsugi.svg.serializing.primitives.writeLineElement
import fromcrackpot.kintsugi.svg.serializing.primitives.writeRoundPointCircle
import nl.adaptivity.xmlutil.XmlWriter

/**
 * Recursively serialize the children of a group, dispatching each [Node] to its writer. Used by
 * the root walker in `SerializeSvg.kt` (the `<svg>` element) and by [writeGroupElement] (nested
 * `<g>` elements).
 */
internal fun XmlWriter.writeNodes(
    nodes: List<Node>,
    viewBox: SceneBounds,
    worldBorder: Circle,
) {
    nodes.forEach { node ->
        when (node) {
            is ShapeNode -> writeShape(node, viewBox, worldBorder)
            is GroupNode -> writeGroupElement(node, viewBox, worldBorder)
        }
    }
}

/*
 * Leaf dispatch: ShapeNode is unsealed, the two families are — the nested `when`s stay
 * exhaustive (a new leaf is a compile error here), the `else` guards only a foreign ShapeNode
 * family. Deliberately the same convention as `:compose`'s `drawShape` — don't "fix" it in
 * either direction.
 */
private fun XmlWriter.writeShape(node: ShapeNode, viewBox: SceneBounds, worldBorder: Circle) {
    when (node) {
        is PrimitiveNode -> when (node) {
            is FlatPointNode -> writeFlatPointRect(node, viewBox)
            is LineNode -> writeLineElement(node, viewBox, worldBorder)
            is RoundPointNode -> writeRoundPointCircle(node, viewBox)
            is DipoleNode -> writeDipoleGroup(node, viewBox)
            is CircleNode -> writeCircleElement(node, viewBox)
        }
        is CompositeNode -> when (node) {
            is SegmentNode -> writeSegmentElement(node, viewBox)
            is PolylineNode -> writePolylineElement(node, viewBox)
            is PolygonNode -> writePolygonElement(node, viewBox)
        }
        else -> error("unhandled ShapeNode family: $node")
    }
}
