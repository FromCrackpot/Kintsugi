package fromcrackpot.kintsugi.brickit.entities

import fromcrackpot.kintsugi.gameengine.GroupEntity

class BrickWall(bricks: List<Brick>) : GroupEntity<Brick>(bricks) {
    override fun copy(children: List<Brick>): BrickWall = BrickWall(children)
}
