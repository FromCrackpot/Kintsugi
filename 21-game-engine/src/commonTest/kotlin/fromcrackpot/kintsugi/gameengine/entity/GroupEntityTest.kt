package fromcrackpot.kintsugi.gameengine.entity

import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.transforms.T
import fromcrackpot.kintsugi.scenegraph.nodes.GroupNode
import fromcrackpot.kintsugi.scenegraph.nodes.composites.PolygonNode
import fromcrackpot.kintsugi.scenegraph.nodes.times
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

private fun quad(minX: Double, minY: Double, maxX: Double, maxY: Double): PolygonNode =
    PolygonNode(Polygon.Rectangle(FlatPoint(minX, minY), FlatPoint(maxX, maxY)))

private class Member(node: PolygonNode) : PolygonEntity(node) {
    override fun rebuild(node: PolygonNode): Member = Member(node)
}

private class Wall(members: List<Member>) : GroupEntity<Member>(members) {
    var hits = 0
    override fun rebuild(children: List<Member>): Wall = Wall(children).also { it.hits = hits }
}

private class WrongGroupRebuild(members: List<Member>) : GroupEntity<Member>(members) {
    // Deliberately returns a different entity class. The narrowed check must catch it.
    override fun rebuild(children: List<Member>): GroupEntity<Member> = Wall(children)
}

class GroupEntityTest {

    @Test
    fun `times returns the entity's class — typed children moved - facts carried by rebuild`() {
        val wall = Wall(listOf(Member(quad(0.0, 0.0, 2.0, 2.0)), Member(quad(4.0, 0.0, 6.0, 2.0))))
        wall.hits = 3

        val moved: Wall = wall * T(10.0, 0.0)

        assertEquals(FlatPoint(11.0, 1.0), moved.children[0].centroid, "the first member moved")
        assertEquals(FlatPoint(15.0, 1.0), moved.children[1].centroid, "the second member moved")
        assertEquals(3, moved.hits, "the fact rode through rebuild")
    }

    @Test
    fun `a one-arg copy call rebuilds the entity's class through rebuild`() {
        val wall = Wall(listOf(Member(quad(0.0, 0.0, 2.0, 2.0))))
        wall.hits = 7

        val rebuilt = wall.copy(listOf(Member(quad(4.0, 0.0, 6.0, 2.0))))

        assertIs<Wall>(rebuilt)
        assertEquals(FlatPoint(5.0, 1.0), rebuilt.children.single().centroid)
        assertEquals(7, rebuilt.hits, "the fact rode through rebuild")
    }

    @Test
    fun `a rebuild override returning the wrong class fails fast with a named error`() {
        val wrong = WrongGroupRebuild(listOf(Member(quad(0.0, 0.0, 2.0, 2.0))))
        val failure = assertFailsWith<IllegalStateException> { wrong * T(1.0, 0.0) }
        val message = assertNotNull(failure.message)
        assertTrue("WrongGroupRebuild.copy returned" in message, "the error names the offending class: $message")
        assertTrue("Wall" in message, "the error names the returned class: $message")
    }

    @Test
    fun `facts are excluded from the inherited structural equality — and a plain group never equals an entity`() {
        val members = listOf(Member(quad(0.0, 0.0, 2.0, 2.0)))
        val a = Wall(members)
        val b = Wall(members)
        b.hits = 9
        assertEquals(a, b, "equality is structural and fact-exclusive")
        assertNotEquals<GroupNode>(GroupNode(children = members), a)
        assertNotEquals<GroupNode>(a, GroupNode(children = members))
    }

    @Test
    fun `minus removes by identity — the structurally equal twin survives - facts carried`() {
        val target = Member(quad(0.0, 0.0, 2.0, 2.0))
        val twin = Member(quad(0.0, 0.0, 2.0, 2.0))
        assertEquals<Member>(target, twin, "twins must be structurally equal for the pin to bite")
        val wall = Wall(listOf(target, twin))
        wall.hits = 4

        val shrunk: Wall = wall - target

        assertSame(twin, shrunk.children.single(), "identity picked the right twin")
        assertEquals(4, shrunk.hits, "the fact rode through rebuild")
    }

    @Test
    fun `minus with an absent child is a no-op — the receiver itself returns`() {
        val wall = Wall(listOf(Member(quad(0.0, 0.0, 2.0, 2.0))))
        val foreign = Member(quad(0.0, 0.0, 2.0, 2.0))

        assertSame(wall, wall - foreign, "structurally equal but absent — nothing to remove")
    }

    @Test
    fun `a rebuild override returning the wrong class fails fast through minus too`() {
        val member = Member(quad(0.0, 0.0, 2.0, 2.0))
        val wrong = WrongGroupRebuild(listOf(member))
        val failure = assertFailsWith<IllegalStateException> { wrong - member }
        val message = assertNotNull(failure.message)
        assertTrue("WrongGroupRebuild.copy returned" in message, "the error names the offending class: $message")
        assertTrue("Wall" in message, "the error names the returned class: $message")
    }

    @Test
    fun `forEach and the for statement visit the children in draw order`() {
        val first = Member(quad(0.0, 0.0, 2.0, 2.0))
        val second = Member(quad(4.0, 0.0, 6.0, 2.0))
        val wall = Wall(listOf(first, second))

        val viaForEach = mutableListOf<Member>()
        wall.forEach { viaForEach.add(it) }
        assertEquals(listOf(first, second), viaForEach)

        val viaFor = mutableListOf<Member>()
        for (member in wall) viaFor.add(member)
        assertEquals(listOf(first, second), viaFor)
    }

    @Test
    fun `a nested group entity is preserved through a plain tree's rebuild`() {
        val wall = Wall(listOf(Member(quad(0.0, 0.0, 2.0, 2.0))))
        wall.hits = 5

        val movedTree = GroupNode(wall) * T(10.0, 0.0)

        val nested = assertIs<Wall>(movedTree.children.single(), "the nested entity kept its class")
        assertEquals(FlatPoint(11.0, 1.0), nested.children.single().centroid, "its member moved")
        assertEquals(5, nested.hits, "its fact rode through")
    }

    @Test
    fun `a factory group keeps its kind through a move and a removal`() {
        val group = GroupEntity(listOf(Member(quad(0.0, 0.0, 2.0, 2.0)), Member(quad(4.0, 0.0, 6.0, 2.0))))

        val moved = group * T(10.0, 0.0)
        assertEquals(group::class, moved::class, "the move returned the same kind of group")
        assertEquals(FlatPoint(11.0, 1.0), moved.children[0].centroid, "the members moved")

        val survivors = moved - moved.children[0]
        assertEquals(group::class, survivors::class, "the removal returned the same kind of group")
        assertEquals(listOf(moved.children[1]), survivors.children, "only the removed member is gone")
    }

    @Test
    fun `factory groups compare by content and never equal a named subclass`() {
        val members = listOf(Member(quad(0.0, 0.0, 2.0, 2.0)))
        assertEquals(GroupEntity(members), GroupEntity(members), "same children, same kind of group")
        assertNotEquals<GroupNode>(GroupEntity(members), Wall(members), "a named subclass is another class")
    }
}
