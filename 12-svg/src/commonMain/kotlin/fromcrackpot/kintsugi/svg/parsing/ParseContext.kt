package fromcrackpot.kintsugi.svg.parsing

/**
 * Cross-document parse state threaded through the recursive parsers (`ElementDispatch` →
 * `GroupParser`/`UseParser`); leaf parsers never see it. [templates] is the caller-supplied
 * href → raw-SVG-text map (`parseSvg(content, templates)`) — text, not pre-parsed trees,
 * because each `<use>` site re-parses its template inside the site's inherited-style chain
 * (explicit-vs-unset is unknowable after styles are baked). [expansionStack] is the chain of
 * documents currently being expanded, for cycle detection.
 */
internal class ParseContext(
    val templates: Map<String, String>,
    val expansionStack: List<String> = emptyList(),
) {
    fun push(templateKey: String): ParseContext =
        ParseContext(templates, expansionStack + templateKey)
}
