package fromcrackpot.kintsugi.svg

import fromcrackpot.kintsugi.scenegraph.nodes.GroupNode

/**
 * The violation of the `/v1` format by a document or scene — the sealed base of every failure the
 * module's entry points raise.
 *
 * `parseSvg` surfaces each format violation as a subtype of this class naming its condition, so one
 * catch covers "this text does not load"; only XML well-formedness errors from the underlying
 * reader fall outside the hierarchy. Messages end with the source line/column when the reader knows
 * it. The serialize side shares the hierarchy: [MissingViewBoxException], its one failure, is a
 * subtype too.
 */
public sealed class SvgFormatException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

private fun at(location: String?): String = location?.let { " at $it" } ?: ""

/**
 * The rejection of an SVG element outside the `/v1` vocabulary.
 *
 * Thrown for a child element that is none of `<rect>`, `<circle>`, `<line>`, `<polygon>`,
 * `<polyline>`, `<g>`, `<use>`, and for a document whose root element is not `<svg>`.
 */
public class UnsupportedSvgElementException(
    /** The offending element's local name, without brackets or namespace prefix. */
    public val elementName: String,
    location: String? = null,
) : SvgFormatException("Unsupported SVG element <$elementName>" + at(location))

/** The rejection of input with no `<svg>` element at all — nothing to parse. */
public class MissingSvgRootException :
    SvgFormatException("Document has no <svg> root element")

/**
 * The absence of a viewport on the document or scene root.
 *
 * The parse side throws it when the root `<svg>` carries neither a `viewBox` nor the
 * `width`/`height` fallback pair; the serialize side when the root [GroupNode]'s `bounds` is
 * `null`. The viewport anchors the format's viewBox-relative coordinate mapping, so neither
 * direction can proceed without one.
 */
public class MissingViewBoxException :
    SvgFormatException("SVG root has no viewBox and no width/height attributes")

/**
 * The rejection of a `<rect>` that fits neither of the element's two readings.
 *
 * The rectangle reading requires all of `x`, `y`, `width`, `height`, rejects rounded corners
 * (non-zero `rx`/`ry`), and rejects zero or negative extents — an authoring error stays an error
 * rather than silently re-anchoring the rectangle. The flat-point reading
 * (`k:type="flat-point"`) requires a square `<rect>` of positive side: the side is the marker
 * size the point renders at, so a non-square rect carries no single size to read.
 */
public class MalformedRectException(
    reason: String,
    location: String? = null,
) : SvgFormatException("Malformed <rect>: $reason" + at(location))

/**
 * The rejection of a `<line k:type="line">` missing part of its geometry.
 *
 * The infinite-line reading requires all of `x1`, `y1`, `x2`, `y2`. Two coincident attribute
 * points are not rejected here — they parse into a degenerate line, kept as data.
 */
public class MalformedLineException(
    reason: String,
    location: String? = null,
) : SvgFormatException("Malformed <line>: $reason" + at(location))

/**
 * The rejection of a bare `<line>` that builds no segment.
 *
 * The segment reading requires all of `x1`, `y1`, `x2`, `y2`, naming two distinct points —
 * coincident endpoints leave no segment to build.
 */
public class MalformedSegmentException(
    reason: String,
    location: String? = null,
) : SvgFormatException("Malformed segment <line>: $reason" + at(location))

/**
 * The rejection of a `<polygon>` whose points close no polygon.
 *
 * Thrown for a missing `points` attribute, a non-numeric coordinate, an odd coordinate count
 * (never silently dropped the way browsers drop a trailing number), or a vertex list the CGA
 * `Polygon` constructor rejects — that diagnostic is carried through in the message.
 */
public class MalformedPolygonException(
    reason: String,
    location: String? = null,
) : SvgFormatException("Malformed <polygon>: $reason" + at(location))

/**
 * The rejection of a `<polyline>` whose points chain into no polyline.
 *
 * Thrown for a missing `points` attribute, a non-numeric coordinate, an odd coordinate count
 * (never silently dropped the way browsers drop a trailing number), or a vertex list the CGA
 * `Polyline` constructor rejects — that diagnostic is carried through in the message.
 */
public class MalformedPolylineException(
    reason: String,
    location: String? = null,
) : SvgFormatException("Malformed <polyline>: $reason" + at(location))

/**
 * The rejection of a `<circle>` with no usable radius.
 *
 * Thrown for a missing `r` attribute or a non-positive `r`. An imaginary circle is not written
 * this way — it persists with a positive `r` and a `k:flags` marker — so a non-positive radius
 * is always an authoring error.
 */
public class MalformedCircleException(
    reason: String,
    location: String? = null,
) : SvgFormatException("Malformed <circle>: $reason" + at(location))

/**
 * The rejection of a round-point `<circle>` (`k:type="round-point"`) with no usable marker size.
 *
 * Thrown for a missing `r` attribute or a non-positive `r`. On a round point `r` is the render
 * marker's radius, not geometry — the algebraic weight rides the optional `k:aw` attribute,
 * which is unconstrained here.
 */
public class MalformedRoundPointException(
    reason: String,
    location: String? = null,
) : SvgFormatException("Malformed round-point <circle>: $reason" + at(location))

/**
 * The rejection of a dipole carrier `<g k:type="dipole">` that holds no valid endpoint pair.
 *
 * The carrier must contain exactly two `<circle k:type="round-point">` children — child order is
 * the dipole's orientation — plus at most the connector `<line>` chrome, which is read past and
 * never interpreted. Thrown for any other child element, a missing or surplus round point, two
 * coincident round-point children (no dipole exists to build), or an unclosed carrier `<g>`.
 */
public class MalformedDipoleException(
    reason: String,
    location: String? = null,
) : SvgFormatException("Malformed <g k:type=\"dipole\">: $reason" + at(location))

/**
 * The rejection of a `viewBox` attribute that describes no viewport.
 *
 * The value must split into the four numbers `min-x min-y width height` (whitespace or comma
 * separated) with positive width and height.
 */
public class MalformedViewBoxException(
    /** The attribute text exactly as it appeared in the document. */
    public val raw: String,
) : SvgFormatException("Malformed viewBox value: \"$raw\"")

/**
 * The rejection of a paint value outside the `/v1` color grammar.
 *
 * `fill` and `stroke` accept `none`, hex colors (`#rgb`, `#rrggbb`, `#rgba`, `#rrggbbaa`), and
 * the `rgb()`/`rgba()` functions. Anything else — named colors, `currentColor`, `inherit`,
 * `url()` references, `hsl()` — is rejected rather than guessed at.
 */
public class UnsupportedPaintException(
    /** The paint text exactly as it appeared in the attribute. */
    public val value: String,
    location: String? = null,
) : SvgFormatException("Unsupported paint value: \"$value\"" + at(location))

/**
 * The rejection of a `<use>` that doesn't fit the format's external-template form.
 *
 * `href` is required and must name an external document — a bare fragment (`href="#id"`, an
 * internal reference) is outside `/v1`, though a `#fragment` suffix after a document path is
 * tolerated and ignored. `width` and `height` are rejected: a template is never scaled, because
 * one CGA unit is one SVG user unit in every document. The element must be empty and closed.
 */
public class MalformedUseException(
    reason: String,
    location: String? = null,
) : SvgFormatException("Malformed <use>: $reason" + at(location))

/**
 * The absence of a template document a `<use>` references.
 *
 * `parseSvg` resolves each `<use>` against its templates map; an [href] with no entry throws.
 * `scanSvgRefs` lists the documents a layout needs, so a caller can fetch them all before
 * parsing.
 */
public class MissingTemplateException(
    /** The referenced document path — the `<use>` element's `href` up to any `#`. */
    public val href: String,
    location: String? = null,
) : SvgFormatException("No template document supplied for <use> href=\"$href\"" + at(location))

/**
 * The rejection of a template document that violates the template size contract.
 *
 * A document referenced by `<use>` must declare explicit `width` and `height` equal to its
 * viewBox extents — the size a browser renders the reference at; a sizeless nested `<svg>`
 * would silently inflate to 100%.
 */
public class MalformedTemplateException(
    reason: String,
    location: String? = null,
) : SvgFormatException("Malformed template: $reason" + at(location))

/**
 * The rejection of template documents that reference each other in a loop.
 *
 * Templates may `<use>` further templates; expansion tracks the documents being expanded and
 * throws when a `<use>` names one already on that chain, instead of recursing forever.
 */
public class SvgTemplateCycleException(
    /** The documents in expansion order; the last entry names the document that closed the loop. */
    public val chain: List<String>,
) : SvgFormatException("Template reference cycle: " + chain.joinToString(" -> "))
