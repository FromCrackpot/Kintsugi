package fromcrackpot.kintsugi.gameengine

/**
 * A thing in the game world with a position, read as [x] and [y].
 *
 * The position is derived from the implementing entity's own geometry, never stored beside
 * it: entities are immutable values that move by rebinding, so a moved entity is a new value
 * whose [x] and [y] read the new place.
 */
public interface Entity {

    /** The position's x coordinate. */
    public val x: Double

    /** The position's y coordinate. */
    public val y: Double
}
