package fromcrackpot.kintsugi.svg

import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.XmlWriter

/** SVG default namespace. */
internal const val SVG_NS = "http://www.w3.org/2000/svg"

/**
 * Kintsugi sidecar namespace, declared once as `xmlns:k` on the `<svg>` root.
 * It's an identifier, not a locator, so it need not resolve. The `/v1` suffix leaves
 * room for future breaking format versions.
 */
internal const val KINTSUGI_NS = "https://fromcrackpot.com/ns/kintsugi/svg/v1"
internal const val KINTSUGI_PREFIX = "k"

/*
 * The `k:*` wire vocabulary, a shared serialize/parse protocol. It lives here, the namespace's
 * home, so a typo can't silently desync the read and write sides: a mismatch is no compile error.
 */

// k:* attribute names
internal const val KATTR_TYPE = "type"
internal const val KATTR_AW = "aw"
internal const val KATTR_FLAGS = "flags"
internal const val KATTR_TAGS = "tags"
internal const val KATTR_CHROME = "chrome"
internal const val KATTR_ATTITUDE = "attitude"

// k:type discriminator values
internal const val KTYPE_FLAT_POINT = "flat-point"
internal const val KTYPE_ROUND_POINT = "round-point"
internal const val KTYPE_LINE = "line"
internal const val KTYPE_DIPOLE = "dipole"
internal const val KTYPE_CIRCLE = "circle"

// k:flags tokens
internal const val KFLAG_IMAGINARY = "imaginary"
internal const val KFLAG_INSIDE_OUT = "inside-out"

/** Write a `k:`-prefixed sidecar attribute (e.g. `k:type="flat-point"`). */
internal fun XmlWriter.kAttr(name: String, value: String) {
    attribute(KINTSUGI_NS, name, KINTSUGI_PREFIX, value)
}

/**
 * Read a `k:`-namespaced sidecar attribute by local name, using the explicit Kintsugi namespace.
 *
 * NOT `getAttributeValue(null, name)`: the xmlutil generic reader resolves a `null` namespace as
 * "match this local name in any namespace," which would also pick up a same-named plain attribute.
 * `getAttributeValue(KINTSUGI_NS, name)` matches strictly the `k:` namespace.
 */
internal fun XmlReader.kAttr(name: String): String? =
    getAttributeValue(KINTSUGI_NS, name)

/** Read a plain (no-namespace) attribute by local name and parse as `Double?`. Blank → null. */
internal fun XmlReader.attrAsDouble(name: String): Double? =
    getAttributeValue(null, name)?.trim()?.takeIf { it.isNotEmpty() }?.toDoubleOrNull()

/** Read a `k:`-namespaced sidecar attribute by local name and parse as `Double?`. Blank → null. */
internal fun XmlReader.kAttrAsDouble(name: String): Double? =
    kAttr(name)?.trim()?.takeIf { it.isNotEmpty() }?.toDoubleOrNull()

/**
 * Read the element's `k:tags` token set: trim, split on any whitespace, drop empties.
 * - Same tolerance as `k:flags`, but an **open vocabulary**. Tokens are game semantics, so there is
 *   no known-token list, no unknown-token warning and no lowercasing (tags stay verbatim).
 * - Absent attribute → empty set.
 */
internal fun XmlReader.kTags(): Set<String> =
    kAttr(KATTR_TAGS)?.trim()?.split(Regex("\\s+"))?.filter { it.isNotEmpty() }?.toSet() ?: emptySet()

/**
 * Parse a `k:flags` token list against an element's known vocabulary: trim, lowercase, split on
 * any whitespace, drop empties. SVG attribute normalization may collapse whitespace and
 * hand-edited files may contain tabs or multiple spaces, so be liberal. Unknown tokens warn and
 * are dropped (forward compatibility). Shared by every element that reads `k:flags`, so the
 * tolerance can't drift between parsers.
 */
internal fun parseKFlags(raw: String?, known: Set<String>, element: String, location: String?): Set<String> {
    if (raw == null) return emptySet()
    val tokens = raw.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
    val (knownTokens, unknown) = tokens.partition { it in known }
    unknown.forEach {
        warn("unknown k:flags token '$it' on <$element>" + (location?.let { l -> " at $l" } ?: "") + "; ignoring")
    }
    return knownTokens.toSet()
}

/**
 * Write `k:tags` space-joined, **only when non-empty**, like the other optional feature attributes
 * (`k:flags`, `stroke-linecap`). Insertion order is preserved as parsed/authored.
 */
internal fun XmlWriter.kTagsAttr(tags: Set<String>) {
    if (tags.isNotEmpty()) kAttr(KATTR_TAGS, tags.joinToString(" "))
}

/** The presentation attributes `k:chrome` may list: the ones a writer derives instead of persisting. */
internal val CHROME_ATTRS: Set<String> = setOf(
    "fill", "fill-opacity", "stroke", "stroke-width", "stroke-linecap", "stroke-dasharray", "stroke-dashoffset",
)

private val WHITESPACE = Regex("\\s+")

private fun XmlReader.kChromeTokens(): List<String> =
    kAttr(KATTR_CHROME)?.trim()?.split(WHITESPACE)?.filter { it.isNotEmpty() } ?: emptyList()

/** Warn about `k:chrome` tokens outside [CHROME_ATTRS]. Call once per element, at its dispatch. */
internal fun XmlReader.warnUnknownChrome(location: String?) {
    kChromeTokens().filter { it !in CHROME_ATTRS }.forEach {
        warn("unknown k:chrome token '$it' on <$localName>" + (location?.let { l -> " at $l" } ?: "") + "; ignoring")
    }
}

/**
 * Read a plain presentation attribute. It reads as **absent** when the element's `k:chrome` lists it,
 * so the cascade applies. Every presentation read goes through here; geometry reads never do.
 */
internal fun XmlReader.presentationAttr(name: String): String? =
    if (name in kChromeTokens()) null else getAttributeValue(null, name)

/** [presentationAttr] parsed as `Double?`. Blank → null. */
internal fun XmlReader.presentationAsDouble(name: String): Double? =
    presentationAttr(name)?.trim()?.takeIf { it.isNotEmpty() }?.toDoubleOrNull()

/**
 * Write `k:chrome` space-joined, **only when non-empty**: the presentation attributes this element
 * carries for display only. Emitted after `k:flags`, before `k:tags`.
 */
internal fun XmlWriter.kChromeAttr(names: List<String>) {
    if (names.isNotEmpty()) kAttr(KATTR_CHROME, names.joinToString(" "))
}
