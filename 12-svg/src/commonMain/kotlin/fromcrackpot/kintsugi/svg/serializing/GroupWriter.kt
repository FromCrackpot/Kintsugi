package fromcrackpot.kintsugi.svg.serializing

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Circle
import fromcrackpot.kintsugi.scenegraph.SceneBounds
import fromcrackpot.kintsugi.scenegraph.nodes.GroupNode
import fromcrackpot.kintsugi.svg.SVG_NS
import fromcrackpot.kintsugi.svg.warn
import nl.adaptivity.xmlutil.XmlWriter

/**
 * GroupNode → `<g>` carrying the recursively-encoded children.
 *
 * - Plain `<g>` is the GroupNode carrier in `/v1`, with no `k:type` (like the bare-`<circle>`-is-Circle
 *   and bare-`<line>`-is-Segment defaults).
 * - Nested `GroupNode.bounds` is NOT persisted: only the root bounds round-trips, as `<svg viewBox>`.
 *   It warns and drops them, then still emits the group so its children survive.
 */
internal fun XmlWriter.writeGroupElement(
    group: GroupNode,
    viewBox: SceneBounds,
    worldBorder: Circle,
) {
    if (group.bounds != null) {
        warn("dropping nested GroupNode.bounds on serialize (only root bounds is encoded in /v1, as <svg viewBox>)")
    }
    startTag(SVG_NS, "g", "")
    writeNodes(group.children, viewBox, worldBorder)
    endTag(SVG_NS, "g", "")
}
