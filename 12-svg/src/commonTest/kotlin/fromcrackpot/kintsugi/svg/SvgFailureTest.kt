package fromcrackpot.kintsugi.svg

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SvgFailureTest {

    // Bare-rect cases moved to RectangleTest — an untagged <rect> is now the Rectangle default.

    @Test fun `non-square flat-point rect throws MalformedRectException`() {
        assertFailsWith<MalformedRectException> { parseSvg(NON_SQUARE_FLAT_POINT_SVG) }
    }

    @Test fun `circle without r throws MalformedCircleException`() {
        assertFailsWith<MalformedCircleException> { parseSvg(CIRCLE_MISSING_R_SVG) }
    }

    @Test fun `circle with non-positive r throws MalformedCircleException`() {
        assertFailsWith<MalformedCircleException> { parseSvg(CIRCLE_ZERO_R_SVG) }
    }

    @Test fun `round-point circle without r throws MalformedRoundPointException`() {
        assertFailsWith<MalformedRoundPointException> { parseSvg(ROUND_POINT_MISSING_R_SVG) }
    }

    @Test fun `path element throws Unsupported`() {
        val ex = assertFailsWith<UnsupportedSvgElementException> { parseSvg(PATH_UNSUPPORTED_SVG) }
        assertEquals("path", ex.elementName)
    }

    @Test fun `svg with no viewBox and no width or height throws MissingViewBox`() {
        assertFailsWith<MissingViewBoxException> { parseSvg(NO_VIEWBOX_NO_DIMENSIONS_SVG) }
    }

    @Test fun `malformed viewBox value throws MalformedViewBox`() {
        val ex = assertFailsWith<MalformedViewBoxException> { parseSvg(MALFORMED_VIEWBOX_SVG) }
        assertEquals("0 0 100", ex.raw)
    }

    /* COLOR FAILURES */

    @Test fun `named color fill throws UnsupportedPaint`() {
        val ex = assertFailsWith<UnsupportedPaintException> { parseSvg(FILL_NAMED_RED_SVG) }
        assertEquals("red", ex.value)
    }

    @Test fun `fill currentColor throws UnsupportedPaint`() {
        val ex = assertFailsWith<UnsupportedPaintException> { parseSvg(FILL_CURRENT_COLOR_SVG) }
        assertEquals("currentColor", ex.value)
    }

    /* DIPOLE FAILURES */

    @Test fun `dipole with one child throws MalformedDipoleException`() {
        assertFailsWith<MalformedDipoleException> { parseSvg(DIPOLE_ONE_CHILD_SVG) }
    }

    @Test fun `dipole with three children throws MalformedDipoleException`() {
        assertFailsWith<MalformedDipoleException> { parseSvg(DIPOLE_THREE_CHILDREN_SVG) }
    }

    @Test fun `dipole with a rect child throws MalformedDipoleException`() {
        assertFailsWith<MalformedDipoleException> { parseSvg(DIPOLE_REJECT_RECT_CHILD_SVG) }
    }

    @Test fun `dipole with an untagged circle child throws MalformedDipoleException`() {
        assertFailsWith<MalformedDipoleException> { parseSvg(DIPOLE_NON_ROUND_POINT_CHILD_SVG) }
    }

    @Test fun `dipole with coincident endpoints throws MalformedDipoleException`() {
        assertFailsWith<MalformedDipoleException> { parseSvg(DIPOLE_COINCIDENT_SVG) }
    }
}
