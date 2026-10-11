package fromcrackpot.kintsugi.svg.parsing

import fromcrackpot.kintsugi.scenegraph.SceneBounds
import fromcrackpot.kintsugi.scenegraph.nodes.GroupNode
import fromcrackpot.kintsugi.scenegraph.nodes.Node
import fromcrackpot.kintsugi.svg.KATTR_TYPE
import fromcrackpot.kintsugi.svg.KTYPE_DIPOLE
import fromcrackpot.kintsugi.svg.kAttr
import fromcrackpot.kintsugi.svg.kTags
import fromcrackpot.kintsugi.svg.warn
import nl.adaptivity.xmlutil.XmlReader

/**
 * Parses a `<g>` element (cursor on its START_ELEMENT). The `k:type` fork:
 *
 * - `k:type="dipole"` → a [fromcrackpot.kintsugi.scenegraph.nodes.primitives.DipoleNode] (the tagged
 *   carrier reading, via [parseDipoleGroup]). The carrier honors its own `fill` the same way a
 *   bare `<g>` does for its children — own attr → inherited chain. Stroke isn't load-bearing
 *   here (the chrome line takes its color from `style.fill`, not stroke).
 * - untagged (or unknown `k:type`, which warns) → [GroupNode] — the bare-element-is-default
 *   pattern, mirroring bare `<circle>`/`<line>`. This `<g>`'s own presentation attrs overlay
 *   the inherited chain; children see them as their nearest fallback. Nested groups parse with
 *   `bounds = null` (only the root bounds round-trips, as `<svg viewBox>`).
 */
internal fun XmlReader.parseGElement(viewBox: SceneBounds, inherited: InheritedStyle, ctx: ParseContext): Node {
    val loc = extLocationInfo?.toString()
    val kType = kAttr(KATTR_TYPE)

    if (kType == KTYPE_DIPOLE) {
        val ownFill = readColorAttr("fill", loc)
        // Tags ride the carrier <g> (the dipole's identity); k:tags on its children is ignored.
        return parseDipoleGroup(viewBox, ownFill ?: inherited.fill, inherited.tags + kTags(), loc)
    }
    if (kType != null) {
        warn("unknown k:type=\"$kType\" on <g>${loc?.let { " at $it" } ?: ""}; treating as a group")
    }
    val childInherited = inherited.child(readInheritedStyle(loc))
    val nested = parseGroupChildren(
        groupTagName = "g",
        viewBox = viewBox,
        inherited = childInherited,
        ctx = ctx,
    )
    return GroupNode(children = nested, bounds = null)
}
