package fromcrackpot.kintsugi.svg.parsing

/**
 * Cross-document parse state threaded through the recursive parsers (`ElementDispatch` →
 * `GroupParser`/`UseParser`). Leaf parsers never see it.
 *
 * - [templates] is the caller-supplied href → raw-SVG-text map (`parseSvg(content, templates)`).
 *   It holds text, not parsed trees: each `<use>` site re-parses its template inside the site's
 *   inherited-style chain, and explicit-vs-unset is unknowable once styles are baked.
 * - [expansionStack] is the chain of documents being expanded, for cycle detection.
 */
internal class ParseContext(
    val templates: Map<String, String>,
    val expansionStack: List<String> = emptyList(),
) {
    fun push(templateKey: String): ParseContext =
        ParseContext(templates, expansionStack + templateKey)
}
