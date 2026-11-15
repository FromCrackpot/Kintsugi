package fromcrackpot.kintsugi.scenegraph.style

/**
 * How an open stroke terminates at its endpoints: flush, rounded or squared off.
 *
 * - A closed outline has no ends to cap, so there it is a visual no-op.
 */
public enum class ShapeStrokeCap {
    /** Flush termination at the endpoint, with no overhang. The default. */
    BUTT,

    /** A semicircular cap of half the stroke width, projecting past the endpoint. */
    ROUND,

    /** A square cap projecting half the stroke width past the endpoint. */
    SQUARE,
}
