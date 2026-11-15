package fromcrackpot.kintsugi.bfxr

/**
 * The failure of a `.bfxr` document, or of a [BfxrSound]'s values, to meet the format, listing every problem found.
 *
 * @param problems One line per problem, each led by the path of the offending key, such as `params.duration: …`.
 */
public class BfxrFormatException(
    public val problems: List<String>,
) : IllegalArgumentException(problems.joinToString(prefix = "Invalid .bfxr sound:\n- ", separator = "\n- "))
