package fromcrackpot.kintsugi.svg

import fromcrackpot.kintsugi.cga.d2.shapes.primitives.FlatPoint
import fromcrackpot.kintsugi.cga.d2.shapes.composites.Polygon
import fromcrackpot.kintsugi.scenegraph.*
import fromcrackpot.kintsugi.scenegraph.nodes.*
import fromcrackpot.kintsugi.scenegraph.nodes.composites.*
import fromcrackpot.kintsugi.scenegraph.nodes.primitives.*
import fromcrackpot.kintsugi.scenegraph.nodes.flattenShapes
import fromcrackpot.kintsugi.scenegraph.style.ShapeColor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class UseExpansionTest {

    private val wallTemplates = mapOf("steel.svg" to BRICK_TEMPLATE_SVG)

    /* EXPANSION GEOMETRY — origin anchoring (template viewport top-left at use x,y) */

    @Test fun `use expands the template translated to its placement`() {
        val root = parseSvg(WALL_LAYOUT_SVG, wallTemplates)
        val shapes = root.flattenShapes()
        assertEquals(2, shapes.size)
        // Layout 200×100 (center 100,50); template 40×20. Use 1 at (10,10): template center lands
        // at SVG (30,20) → CGA (-70,30); full-bleed template rect → bl (-90,20), tr (-50,40).
        assertEquals(
            Polygon.Rectangle(FlatPoint(-90.0, 20.0), FlatPoint(-50.0, 40.0)),
            shapes[0].shape,
        )
        // Use 2 at (60,10): center SVG (80,20) → CGA (-20,30) → bl (-40,20), tr (0,40).
        assertEquals(
            Polygon.Rectangle(FlatPoint(-40.0, 20.0), FlatPoint(0.0, 40.0)),
            shapes[1].shape,
        )
    }

    @Test fun `href fragment is tolerated and ignored`() {
        // Use 2 references "steel.svg#brick" — same document, fragment dropped.
        val root = parseSvg(WALL_LAYOUT_SVG, wallTemplates)
        assertEquals(2, root.flattenShapes().size)
    }

    /* TAGS + STYLE FLOW ACROSS THE REFERENCE */

    @Test fun `use-site tags union with template tags on every expanded leaf`() {
        val shapes = parseSvg(WALL_LAYOUT_SVG, wallTemplates).flattenShapes()
        assertEquals(setOf("brick", "steel"), shapes[0].tags)
        assertEquals(setOf("brick"), shapes[1].tags)
    }

    @Test fun `use-site fill flows into the template's unset fill`() {
        val shapes = parseSvg(WALL_LAYOUT_SVG, wallTemplates).flattenShapes()
        assertEquals(ShapeColor.rgb(0x88, 0x88, 0x88), shapes[0].style.fill)
        // Second instance has no override → SVG default.
        assertEquals(ShapeColor.BLACK, shapes[1].style.fill)
    }

    @Test fun `explicit template fill wins over use-site fill`() {
        val root = parseSvg(WALL_LAYOUT_SVG, mapOf("steel.svg" to GREEN_TEMPLATE_SVG))
        val shapes = root.flattenShapes()
        assertEquals(ShapeColor.rgb(0x00, 0xFF, 0x00), shapes[0].style.fill)
    }

    /* TRANSITIVE TEMPLATES */

    @Test fun `templates may use other templates, translations composing`() {
        val root = parseSvg(
            TRANSITIVE_LAYOUT_SVG,
            mapOf("composite.svg" to COMPOSITE_TEMPLATE_SVG, "gem.svg" to GEM_TEMPLATE_SVG),
        )
        val gem = root.flattenShapes().single()
        // composite at (60,10) → T(-20,30); gem centered inside composite → T(0,0);
        // gem 20×10 full-bleed → bl (-30,25), tr (-10,35).
        assertEquals(
            Polygon.Rectangle(FlatPoint(-30.0, 25.0), FlatPoint(-10.0, 35.0)),
            gem.shape,
        )
        assertEquals(setOf("gem"), gem.tags)
    }

    @Test fun `template cycle raises with the chain in expansion order`() {
        val e = assertFailsWith<SvgTemplateCycleException> {
            parseSvg(
                CYCLE_LAYOUT_SVG,
                mapOf("cycle-a.svg" to CYCLE_A_TEMPLATE_SVG, "cycle-b.svg" to CYCLE_B_TEMPLATE_SVG),
            )
        }
        assertEquals(listOf("cycle-a.svg", "cycle-b.svg", "cycle-a.svg"), e.chain)
    }

    /* CONTRACT ERRORS */

    @Test fun `use without href is malformed`() {
        assertFailsWith<MalformedUseException> { parseSvg(USE_NO_HREF_SVG) }
    }

    @Test fun `bare-fragment href is malformed — internal references unsupported`() {
        assertFailsWith<MalformedUseException> { parseSvg(USE_FRAGMENT_ONLY_SVG) }
    }

    @Test fun `width or height on use is malformed — no scaling`() {
        assertFailsWith<MalformedUseException> { parseSvg(USE_WITH_SIZE_SVG, wallTemplates) }
    }

    @Test fun `child element inside use is malformed`() {
        assertFailsWith<MalformedUseException> { parseSvg(USE_WITH_CHILD_SVG, wallTemplates) }
    }

    @Test fun `unresolved template document raises MissingTemplateException`() {
        val e = assertFailsWith<MissingTemplateException> { parseSvg(WALL_LAYOUT_SVG) }
        assertEquals("steel.svg", e.href)
    }

    @Test fun `template without explicit width and height is rejected`() {
        assertFailsWith<MalformedTemplateException> {
            parseSvg(WALL_LAYOUT_SVG, mapOf("steel.svg" to SIZELESS_TEMPLATE_SVG))
        }
    }

    @Test fun `template width mismatching its viewBox is rejected`() {
        assertFailsWith<MalformedTemplateException> {
            parseSvg(WALL_LAYOUT_SVG, mapOf("steel.svg" to MISMATCHED_TEMPLATE_SVG))
        }
    }

    /* PARSE-ONLY: EXPANSION SERIALIZES AS STANDARD SVG */

    @Test fun `expanded layout serializes without use and reparses identically`() {
        val once = parseSvg(WALL_LAYOUT_SVG, wallTemplates)
        val emitted = serializeSvg(once)
        assertTrue(!emitted.contains("<use"), "expansion must be parse-only; actual:\n$emitted")
        // The flattened document is self-contained: reparse needs no templates.
        assertEquals(once, parseSvg(emitted))
    }

    /* SCAN — the fetch pass for :game-engine */

    @Test fun `scanSvgRefs collects referenced documents pre-fragment`() {
        assertEquals(setOf("steel.svg"), scanSvgRefs(WALL_LAYOUT_SVG))
        assertEquals(setOf("composite.svg"), scanSvgRefs(TRANSITIVE_LAYOUT_SVG))
        assertEquals(setOf("gem.svg"), scanSvgRefs(COMPOSITE_TEMPLATE_SVG))
        assertEquals(emptySet(), scanSvgRefs(EMPTY_SVG))
    }
}
