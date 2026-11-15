package fromcrackpot.kintsugi.gameengine.entity

/**
 * A thing in the game world with a position, read as [x] and [y].
 *
 * - The position derives from the entity's own geometry, so a moved entity is a new value at the new place.
 */
public interface Entity {

    /** The position's x coordinate. */
    public val x: Double

    /** The position's y coordinate. */
    public val y: Double
}
