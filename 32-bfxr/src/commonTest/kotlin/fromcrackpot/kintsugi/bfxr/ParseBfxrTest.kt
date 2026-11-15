package fromcrackpot.kintsugi.bfxr

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ParseBfxrTest {
    private fun problems(text: String): List<String> = assertFailsWith<BfxrFormatException> { parseBfxr(text) }.problems

    private fun assertProblem(text: String, vararg fragments: String) {
        val problems = problems(text)
        assertTrue(
            problems.any { problem -> fragments.all { it in problem } },
            "expected a problem with ${fragments.toList()}, got $problems",
        )
    }

    private fun clonkr(params: String, extra: String = ""): String =
        """{"synth_type":"Clonkr","version":"1.0.0","params":{$params}$extra}"""

    /* ACCEPTED */

    @Test
    fun `a complete document parses with its values`() {
        val sound = parseBfxr(clonkr(""""material":2,"size":0.25""", ""","renderSeed":0.75,"file_name":"clang""""))
        assertEquals(BfxrSynth.CLONKR, sound.synth)
        assertEquals(BfxrValue.Number(2.0), sound.params["material"])
        assertEquals(BfxrValue.Number(0.25), sound.params["size"])
        assertEquals(0.75, sound.renderSeed)
    }

    @Test
    fun `omitted parameters and renderSeed take their defaults`() {
        val sound = parseBfxr(clonkr(""))
        assertEquals(BfxrSynth.CLONKR.params.map { it.name to it.default }, sound.params.toList())
        assertEquals(0.5, sound.renderSeed)
    }

    @Test
    fun `range bounds are inclusive`() {
        parseBfxr(clonkr(""""duration":0.1,"size":1"""))
    }

    @Test
    fun `a transition parses with omitted fields taken from its default`() {
        val sound = parseBfxr(
            """{"synth_type":"Transfxr","version":"1.0.0","params":{"pitch":{"start":0.1,"curve":"Steps"}}}""",
        )
        assertEquals(BfxrValue.Transition(0.1, 0.25, "Steps"), sound.params["pitch"])
    }

    @Test
    fun `a text parameter parses as text`() {
        val phrase = """[{\"degree\":0,\"beats\":1}]"""
        val sound = parseBfxr("""{"synth_type":"Jinglr","version":"1.0.0","params":{"phrase":"$phrase"}}""")
        assertEquals(BfxrValue.Text("""[{"degree":0,"beats":1}]"""), sound.params["phrase"])
    }

    /* TOP LEVEL */

    @Test
    fun `text that is not JSON is rejected`() {
        assertProblem("""{"synth_type":""", "not valid JSON")
    }

    @Test
    fun `a top level that is not an object is rejected`() {
        assertProblem("[]", "expected a JSON object")
    }

    @Test
    fun `an unknown top-level key gets a hint`() {
        assertProblem(clonkr("", ""","render_seed":0.5"""), "render_seed", "did you mean 'renderSeed'")
    }

    @Test
    fun `synth_type and version and params are required`() {
        val problems = problems("{}")
        for (key in listOf("synth_type", "version", "params")) {
            assertTrue(problems.any { it == "$key: missing" }, "$key in $problems")
        }
    }

    @Test
    fun `an unknown synth gets a hint`() {
        assertProblem("""{"synth_type":"Clonker","version":"1.0.0","params":{}}""", "Clonker", "did you mean 'Clonkr'")
    }

    @Test
    fun `synth names are case-sensitive`() {
        assertProblem("""{"synth_type":"clonkr","version":"1.0.0","params":{}}""", "did you mean 'Clonkr'")
    }

    @Test
    fun `a version other than the pinned one is rejected`() {
        assertProblem("""{"synth_type":"Bfxr","version":"1.0.0","params":{}}""", "version", "'1.0.4'")
    }

    @Test
    fun `renderSeed must lie in the unit range`() {
        assertProblem(clonkr("", ""","renderSeed":1.5"""), "renderSeed", "outside")
    }

    @Test
    fun `a non-finite literal is rejected`() {
        assertFailsWith<BfxrFormatException> { parseBfxr(clonkr("", ""","renderSeed":NaN""")) }
    }

    /* PARAMS */

    @Test
    fun `an unknown parameter gets a hint`() {
        assertProblem(clonkr(""""materiel":1"""), "params.materiel", "did you mean 'material'")
    }

    @Test
    fun `a far-off unknown parameter gets no hint`() {
        val problems = problems(clonkr(""""frequency_start":0.5"""))
        assertTrue(problems.single().let { "unknown Clonkr parameter" in it && "did you mean" !in it }, "$problems")
    }

    @Test
    fun `a value outside its range is rejected rather than clamped`() {
        assertProblem(clonkr(""""duration":9"""), "params.duration", "outside")
    }

    @Test
    fun `a choice outside its options is rejected`() {
        assertProblem(clonkr(""""material":5"""), "params.material", "not one of")
    }

    @Test
    fun `a number given as a string is rejected`() {
        assertProblem(clonkr(""""size":"0.5""""), "params.size", "expected a number")
    }

    @Test
    fun `a transition needs an object with known keys and curve`() {
        val base = """{"synth_type":"Transfxr","version":"1.0.0","params":{"""
        assertProblem("""$base"pitch":0.5}}""", "params.pitch", "expected an object")
        assertProblem("""$base"pitch":{"start":0.5,"strat":1}}}""", "params.pitch.strat", "did you mean 'start'")
        assertProblem("""$base"pitch":{"curve":"Wobble"}}}""", "params.pitch.curve", "Wobble")
        assertProblem("""$base"pitch":{"end":2}}}""", "params.pitch.end", "outside")
    }

    @Test
    fun `a text longer than its limit is rejected rather than cut`() {
        val long = "x".repeat(4097)
        assertProblem("""{"synth_type":"Jinglr","version":"1.0.0","params":{"phrase":"$long"}}""", "params.phrase", "longer")
    }

    @Test
    fun `every problem is reported at once`() {
        val problems = problems("""{"synth_type":"Clonkr","version":"2","params":{"size":7,"materiel":1},"seed":1}""")
        assertEquals(4, problems.size, "$problems")
    }

    @Test
    fun `the message lists each problem on its own line`() {
        val e = assertFailsWith<BfxrFormatException> { parseBfxr(clonkr(""""size":7,"materiel":1""")) }
        assertEquals(e.problems.joinToString(prefix = "Invalid .bfxr sound:\n- ", separator = "\n- "), e.message)
    }

    /* JINGLR PHRASE */

    private fun jinglr(phrase: String): String {
        val escaped = phrase.replace("\"", "\\\"")
        return """{"synth_type":"Jinglr","version":"1.0.0","params":{"phrase":"$escaped"}}"""
    }

    @Test
    fun `a phrase at its limits parses`() {
        val phrase = """[{"degree":-7,"beats":0.25},{"degree":14,"beats":2},{"degree":null,"beats":1.75}]"""
        assertEquals(BfxrValue.Text(phrase), parseBfxr(jinglr(phrase)).params["phrase"])
    }

    @Test
    fun `an omitted note key takes its default`() {
        val notes = readPhrase("""[{"beats":1},{"degree":2}]""", "phrase", mutableListOf())
        assertEquals(listOf(0 to 1.0, 2 to 0.5), notes.map { it.degree to it.beats })
        parseBfxr(jinglr("""[{"beats":1},{"degree":2}]"""))
    }

    @Test
    fun `a phrase that is not a JSON array of notes is rejected`() {
        assertProblem(jinglr("""[{"degree":0"""), "params.phrase", "not valid JSON")
        assertProblem(jinglr("""{"degree":0}"""), "params.phrase", "expected a JSON array of notes")
        assertProblem(jinglr("[]"), "params.phrase", "at least one note")
        assertProblem(jinglr("[1]"), "params.phrase[0]", "expected an object")
    }

    @Test
    fun `a phrase over twelve notes is rejected rather than cut`() {
        val notes = List(13) { """{"degree":$it}""" }.joinToString(",", "[", "]")
        assertProblem(jinglr(notes), "params.phrase", "13 notes, at most 12")
    }

    @Test
    fun `a degree that is fractional or out of range is rejected rather than rounded`() {
        val phrase = """[{"degree":2.5},{"degree":15},{"degree":-8}]"""
        assertProblem(jinglr(phrase), "params.phrase[0].degree", "not an integer")
        assertProblem(jinglr(phrase), "params.phrase[1].degree", "outside [-7, 14]")
        assertProblem(jinglr(phrase), "params.phrase[2].degree", "outside [-7, 14]")
    }

    @Test
    fun `beats off the quarter grid or out of range are rejected rather than rounded`() {
        val phrase = """[{"beats":0.3},{"beats":2.25},{"beats":0}]"""
        assertProblem(jinglr(phrase), "params.phrase[0].beats", "not a multiple of 0.25")
        assertProblem(jinglr(phrase), "params.phrase[1].beats", "outside [0.25, 2]")
        assertProblem(jinglr(phrase), "params.phrase[2].beats", "outside [0.25, 2]")
    }

    @Test
    fun `a note value of the wrong type is rejected`() {
        val phrase = """[{"degree":"3","beats":null}]"""
        assertProblem(jinglr(phrase), "params.phrase[0].degree", "expected a number")
        assertProblem(jinglr(phrase), "params.phrase[0].beats", "expected a number")
    }

    @Test
    fun `an unknown note key gets a hint`() {
        assertProblem(jinglr("""[{"degre":3}]"""), "params.phrase[0].degre", "did you mean 'degree'")
    }

    /* CLASSIC BFXR */

    private fun bfxr(params: String): String = """{"synth_type":"Bfxr","version":"1.0.4","params":{$params}}"""

    @Test
    fun `a classic sound muted before its first sample is rejected`() {
        val bitnoise = bfxr(""""waveType":9,"frequency_start":0,"min_frequency_relative_to_starting_frequency":0.99""")
        assertProblem(bitnoise, "params.min_frequency_relative_to_starting_frequency", "before its first sample")
    }

    @Test
    fun `a classic sound muted later by its minimum frequency parses and renders`() {
        val falling = bfxr(""""frequency_slide":-0.5,"min_frequency_relative_to_starting_frequency":0.5""")
        assertTrue(parseBfxr(falling).render().samples.isNotEmpty())
    }

    /* MIXR SOURCES */

    private fun mixr(sources: String): String {
        val escaped = sources.replace("\\", "\\\\").replace("\"", "\\\"")
        return """{"synth_type":"Mixr","version":"1.0.0","params":{"sources":"$escaped"}}"""
    }

    @Test
    fun `a null slot and a source with omitted params and renderSeed parse and render`() {
        val sound = parseBfxr(mixr("""[null,{"synth":"Clonkr"}]"""))
        val sources = readSources((sound.params.getValue("sources") as BfxrValue.Text).value, "sources", mutableListOf())
        assertEquals(null, sources[0])
        assertEquals(BfxrSynth.CLONKR.params.map { it.name to it.default }, sources[1]?.params?.toList())
        assertEquals(null, sources[1]?.renderSeed)
        assertTrue(sound.render().samples.size >= 4410)
    }

    @Test
    fun `generators are kept as text without a check against any list`() {
        parseBfxr(mixr("""[{"synth":"Clonkr","generator":"*","selectedGenerator":"generate_anything"}]"""))
    }

    @Test
    fun `sources that are not a JSON array of sources are rejected`() {
        assertProblem(mixr("""[{"synth":"Clonkr"}"""), "params.sources", "not valid JSON")
        assertProblem(mixr("""{"synth":"Clonkr"}"""), "params.sources", "expected a JSON array of sources")
        assertProblem(mixr("[1]"), "params.sources[0]", "expected a source object or null")
    }

    @Test
    fun `more than two sources are rejected rather than cut`() {
        assertProblem(mixr("[null,null,null]"), "params.sources", "3 sources, at most 2")
    }

    @Test
    fun `a source needs a known synth other than Mixr`() {
        assertProblem(mixr("[{}]"), "params.sources[0].synth", "missing")
        assertProblem(mixr("""[{"synth":"Clonker"}]"""), "params.sources[0].synth", "did you mean 'Clonkr'")
        assertProblem(mixr("""[{"synth":"Mixr"}]"""), "params.sources[0].synth", "can't be a Mixr")
    }

    @Test
    fun `an unknown source key gets a hint`() {
        assertProblem(mixr("""[{"synth":"Clonkr","nmae":"x"}]"""), "params.sources[0].nmae", "did you mean 'name'")
    }

    @Test
    fun `source fields of the wrong type or out of range are rejected rather than dropped or clamped`() {
        val sources = """[null,{"synth":"Clonkr","name":3,"generator":true,"renderSeed":2,"params":[]}]"""
        assertProblem(mixr(sources), "params.sources[1].name", "expected a string")
        assertProblem(mixr(sources), "params.sources[1].generator", "expected a string")
        assertProblem(mixr(sources), "params.sources[1].renderSeed", "outside [0, 1]")
        assertProblem(mixr(sources), "params.sources[1].params", "expected an object")
    }

    @Test
    fun `a source name over sixty characters is rejected rather than cut`() {
        val name = "x".repeat(61)
        assertProblem(mixr("""[{"synth":"Clonkr","name":"$name"}]"""), "params.sources[0].name", "longer than 60")
    }

    @Test
    fun `a source's params get the checks of a sound of its synth`() {
        val sources = """[{"synth":"Clonkr","params":{"size":7,"materiel":1}}]"""
        assertProblem(mixr(sources), "params.sources[0].params.size", "outside [0.0, 1.0]")
        assertProblem(mixr(sources), "params.sources[0].params.materiel", "did you mean 'material'")
    }

    @Test
    fun `a nested phrase gets the Jinglr checks`() {
        val sources = """[{"synth":"Jinglr","params":{"phrase":"[{\"degree\":2.5}]"}}]"""
        assertProblem(mixr(sources), "params.sources[0].params.phrase[0].degree", "not an integer")
    }

    @Test
    fun `a classic source muted before its first sample is rejected`() {
        val params = """"waveType":9,"frequency_start":0,"min_frequency_relative_to_starting_frequency":0.99"""
        assertProblem(
            mixr("""[{"synth":"Bfxr","params":{$params}}]"""),
            "params.sources[0].params.min_frequency_relative_to_starting_frequency",
            "before its first sample",
        )
    }

    @Test
    fun `the constructor checks sources too`() {
        val e = assertFailsWith<BfxrFormatException> {
            BfxrSound(BfxrSynth.MIXR, mapOf("sources" to BfxrValue.Text("""[{"synth":"Mixr"}]""")))
        }
        assertTrue(e.problems.single().startsWith("params.sources[0].synth"), e.problems.toString())
    }

    @Test
    fun `a Footsteppr source renders`() {
        val sound = parseBfxr(mixr("""[{"synth":"Footsteppr","params":{"terrain":3}}]"""))
        assertTrue(sound.render().samples.any { it != 0f })
    }
}
