package fromcrackpot.kintsugi.scenegraph.nodes.primitives

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.Dipole
import fromcrackpot.kintsugi.scenegraph.style.ShapeStyle

/**
 * A scene-graph leaf that is a [Dipole]: the dipole's geometry carrying the concrete
 * [style] it renders with, plus optional [tags].
 *
 * Construction copies [dipole]'s components, so the leaf carries the same geometry at full
 * `Double` precision. Everything a [Dipole] offers is inherited, and every operation typed
 * against [Dipole] accepts the leaf as-is — the shape-preserving operations, [canonical]
 * included, return through [copy] and come back as the leaf, style and tags riding along;
 * everything else returns plain shapes.
 *
 * @constructor Builds the leaf around [dipole]'s geometry.
 * @param dipole The geometry to decorate; its components are copied.
 * @param style The concrete paint; the dipole default when omitted.
 * @param tags Free-form tokens riding the leaf; empty by default.
 */
public open class DipoleNode(
    dipole: Dipole,
    override var style: ShapeStyle = ShapeStyle.DIPOLE,
    override var tags: Set<String> = emptySet(),
) : Dipole(dipole.gx, dipole.gy, dipole.gz, dipole.px, dipole.py, dipole.pz), PrimitiveNode {

    /*
    CTOR
    */

    /**
     * Builds the leaf as a copy of [node]: the same geometry, and [node]'s own style and
     * tags unless either is overridden.
     *
     * Construction copies [node]'s components, so the copy carries the same geometry at
     * full `Double` precision. Overriding [style] or [tags] swaps that part alone — the
     * other still comes from [node].
     *
     * @param node The leaf to copy.
     * @param style The concrete paint; [node]'s own when omitted.
     * @param tags Free-form tokens riding the leaf; [node]'s own when omitted.
     */
    public constructor(
        node: DipoleNode,
        style: ShapeStyle = node.style,
        tags: Set<String> = node.tags,
    ) : this(node as Dipole, style, tags) // upcast: `this(node, …)` would resolve back into this constructor

    /*
    COPY
    */

    /**
     * Returns a [DipoleNode] around [dipole], carrying this leaf's [style] and [tags].
     *
     * The leaf's rebuild hook: every shape-preserving operation returns through it, so the
     * moved leaf keeps its decoration.
     *
     * @param dipole The replacement geometry.
     * @return The rebuilt leaf.
     */
    override fun copy(dipole: Dipole): DipoleNode = DipoleNode(dipole, style, tags)

    /*
    IDENTITY
    */

    /**
     * Rounded, style-inclusive identity: equal when the geometry matches at the `SCALE`
     * quantum and [style] and [tags] match exactly.
     *
     * Identity is exact-class: a `DipoleNode` and a plain [Dipole] never compare equal, in
     * either direction.
     *
     * @param other The object to compare against.
     * @return `true` iff [other] is a `DipoleNode` with equal rounded geometry, equal
     *   [style], and equal [tags].
     */
    override fun equals(other: Any?): Boolean =
        super.equals(other) && other is DipoleNode && style == other.style && tags == other.tags

    /** Returns the geometry hash combined with [style]'s and [tags]', consistent with [equals]. */
    override fun hashCode(): Int = 31 * (31 * super.hashCode() + style.hashCode()) + tags.hashCode()

    /** Returns the raw geometry with the style and tags — full stored precision, no rounding. */
    override fun toString(): String = "DipoleNode(${super.toString()}, style=$style, tags=$tags)"
}
