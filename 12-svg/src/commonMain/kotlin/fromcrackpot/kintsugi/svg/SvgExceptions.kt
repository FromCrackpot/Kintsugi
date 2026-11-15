package fromcrackpot.kintsugi.svg

import fromcrackpot.kintsugi.scenegraph.nodes.GroupNode

/**
 * The violation of the `/v1` format by a document or scene. It is the sealed base of every failure
 * the module's entry points raise.
 *
 * - One catch covers "this text does not load". Only XML well-formedness errors fall outside.
 */
public sealed class SvgFormatException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

private fun at(location: String?): String = location?.let { " at $it" } ?: ""

/**
 * The rejection of an SVG element outside the `/v1` vocabulary.
 *
 * - Thrown for a child outside `<rect>`, `<circle>`, `<line>`, `<polygon>`, `<polyline>`, `<g>`, `<use>`.
 * - Thrown for a document whose root element is not `<svg>`.
 */
public class UnsupportedSvgElementException(
    /** The offending element's local name, without brackets or namespace prefix. */
    public val elementName: String,
    location: String? = null,
) : SvgFormatException("Unsupported SVG element <$elementName>" + at(location))

/** The rejection of input with no `<svg>` element at all, leaving nothing to parse. */
public class MissingSvgRootException :
    SvgFormatException("Document has no <svg> root element")

/**
 * The absence of a viewport on the document or scene root.
 *
 * - Parse: the root `<svg>` has neither `viewBox` nor `width`/`height`. Serialize: the root's bounds is `null`.
 */
public class MissingViewBoxException :
    SvgFormatException("SVG root has no viewBox and no width/height attributes")

/**
 * The rejection of a `<rect>` that fits neither of the element's two readings.
 *
 * - A rectangle needs `x`, `y`, `width`, `height`, positive extents, and no rounded corners.
 * - A flat point (`k:type="flat-point"`) needs a square of positive side, its marker size.
 */
public class MalformedRectException(
    reason: String,
    location: String? = null,
) : SvgFormatException("Malformed <rect>: $reason" + at(location))

/**
 * The rejection of a `<line k:type="line">` missing part of its geometry.
 *
 * - Needs all of `x1`, `y1`, `x2`, `y2`. Coincident points parse into a degenerate line, kept as data.
 */
public class MalformedLineException(
    reason: String,
    location: String? = null,
) : SvgFormatException("Malformed <line>: $reason" + at(location))

/**
 * The rejection of a bare `<line>` that builds no segment.
 *
 * - Needs all of `x1`, `y1`, `x2`, `y2`, naming two distinct points.
 */
public class MalformedSegmentException(
    reason: String,
    location: String? = null,
) : SvgFormatException("Malformed segment <line>: $reason" + at(location))

/**
 * The rejection of a `<polygon>` whose points close no polygon.
 *
 * - Missing or non-numeric `points`, an odd coordinate count, or a vertex list `Polygon` rejects.
 */
public class MalformedPolygonException(
    reason: String,
    location: String? = null,
) : SvgFormatException("Malformed <polygon>: $reason" + at(location))

/**
 * The rejection of a `<polyline>` whose points chain into no polyline.
 *
 * - Missing or non-numeric `points`, an odd coordinate count, or a vertex list `Polyline` rejects.
 */
public class MalformedPolylineException(
    reason: String,
    location: String? = null,
) : SvgFormatException("Malformed <polyline>: $reason" + at(location))

/**
 * The rejection of a `<circle>` with no usable radius.
 *
 * - Thrown for a missing or non-positive `r`. An imaginary circle persists with a positive `r` and `k:flags`.
 */
public class MalformedCircleException(
    reason: String,
    location: String? = null,
) : SvgFormatException("Malformed <circle>: $reason" + at(location))

/**
 * The rejection of a round-point `<circle>` (`k:type="round-point"`) with no usable marker size.
 *
 * - Thrown for a missing or non-positive `r`, which on a round point is the marker's radius.
 */
public class MalformedRoundPointException(
    reason: String,
    location: String? = null,
) : SvgFormatException("Malformed round-point <circle>: $reason" + at(location))

/**
 * The rejection of a dipole carrier `<g k:type="dipole">` that holds no valid endpoint pair.
 *
 * - It needs exactly two distinct round-point `<circle>` children, in orientation order.
 * - Only the connector `<line>` may accompany them. The carrier must be closed.
 */
public class MalformedDipoleException(
    reason: String,
    location: String? = null,
) : SvgFormatException("Malformed <g k:type=\"dipole\">: $reason" + at(location))

/**
 * The rejection of a `viewBox` attribute that describes no viewport.
 *
 * - It needs four numbers `min-x min-y width height`, with positive width and height.
 */
public class MalformedViewBoxException(
    /** The attribute text exactly as it appeared in the document. */
    public val raw: String,
) : SvgFormatException("Malformed viewBox value: \"$raw\"")

/**
 * The rejection of a paint value outside the `/v1` color grammar.
 *
 * - Accepts `none`, hex colors, `rgb()` and `rgba()`. Named colors, `currentColor`, `url()` and `hsl()` are rejected.
 */
public class UnsupportedPaintException(
    /** The paint text exactly as it appeared in the attribute. */
    public val value: String,
    location: String? = null,
) : SvgFormatException("Unsupported paint value: \"$value\"" + at(location))

/**
 * The rejection of a `<use>` that doesn't fit the format's external-template form.
 *
 * - `href` must name an external document. `width` and `height` are rejected, since templates never scale.
 */
public class MalformedUseException(
    reason: String,
    location: String? = null,
) : SvgFormatException("Malformed <use>: $reason" + at(location))

/**
 * The absence of a template document a `<use>` references.
 *
 * - [scanSvgRefs] lists the documents a layout needs, so they can all be fetched before parsing.
 */
public class MissingTemplateException(
    /** The referenced document path: the `<use>` element's `href` up to any `#`. */
    public val href: String,
    location: String? = null,
) : SvgFormatException("No template document supplied for <use> href=\"$href\"" + at(location))

/**
 * The rejection of a template document that violates the template size contract.
 *
 * - A template must declare `width` and `height` equal to its viewBox extents.
 */
public class MalformedTemplateException(
    reason: String,
    location: String? = null,
) : SvgFormatException("Malformed template: $reason" + at(location))

/** The rejection of template documents that reference each other in a loop. */
public class SvgTemplateCycleException(
    /** The documents in expansion order. The last entry names the document that closed the loop. */
    public val chain: List<String>,
) : SvgFormatException("Template reference cycle: " + chain.joinToString(" -> "))
