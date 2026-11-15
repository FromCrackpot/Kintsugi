package fromcrackpot.kintsugi.scratchpad.sketches

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fromcrackpot.kintsugi.audio.Clip
import fromcrackpot.kintsugi.audio.PcmSink
import fromcrackpot.kintsugi.audio.Voice
import fromcrackpot.kintsugi.bfxr.parseBfxr
import fromcrackpot.kintsugi.scratchpad.generated.resources.Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi

/* SOUNDS */

private class SfxSynth(val name: String, val presets: List<String>)

private class LoadedSfx(val name: String, val key: Key, val keyLabel: String, val clip: Clip)

private val KEYS = listOf(
    Key.One to "1", Key.Two to "2", Key.Three to "3", Key.Four to "4", Key.Five to "5",
    Key.Six to "6", Key.Seven to "7", Key.Eight to "8", Key.Nine to "9", Key.Zero to "0",
    Key.Q to "Q", Key.W to "W", Key.E to "E", Key.R to "R", Key.T to "T",
    Key.Y to "Y", Key.U to "U", Key.I to "I", Key.O to "O", Key.P to "P",
)

private val TEXT = TextStyle(color = Color(0xFFE0E0E0), fontSize = 18.sp)

@OptIn(ExperimentalResourceApi::class)
private suspend fun readIndex(): List<SfxSynth> =
    Res.readBytes("files/sfx/index.txt").decodeToString().lines().filter { it.isNotBlank() }.map { line ->
        val words = line.trim().split(' ')
        SfxSynth(words.first(), words.drop(1))
    }

@OptIn(ExperimentalResourceApi::class)
private suspend fun loadSynth(synth: SfxSynth, sink: PcmSink): List<LoadedSfx> =
    synth.presets.zip(KEYS) { preset, (key, label) ->
        val text = Res.readBytes("files/sfx/${synth.name.lowercase()}/$preset.bfxr").decodeToString()
        val pcm = withContext(Dispatchers.Default) { parseBfxr(text).render() }
        LoadedSfx(preset, key, label, sink.load(pcm))
    }

/* SKETCH */

@Composable
fun Sfx(modifier: Modifier) {
    val sink = remember { PcmSink() }
    DisposableEffect(sink) { onDispose { sink.close() } }
    val synths by produceState(emptyList<SfxSynth>()) { value = readIndex() }
    var current by remember { mutableStateOf(0) }
    val loaded = remember { mutableStateMapOf<String, List<LoadedSfx>>() }
    val synth = synths.getOrNull(current)
    LaunchedEffect(synth) {
        if (synth != null && synth.name !in loaded) loaded[synth.name] = loadSynth(synth, sink)
    }
    val sounds = synth?.let { loaded[it.name] }.orEmpty()
    var loop by remember { mutableStateOf(false) }
    val voices = remember { mutableListOf<Voice>() }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    fun play(sfx: LoadedSfx) {
        voices += sfx.clip.play(loop = loop)
    }

    fun stopAll() {
        voices.forEach(Voice::stop)
        voices.clear()
    }

    fun switch(step: Int) {
        if (synths.isNotEmpty()) current = (current + step + synths.size) % synths.size
    }

    Column(
        modifier
            .background(Color(0xFF101014))
            .focusRequester(focus)
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                val sfx = sounds.firstOrNull { it.key == event.key }
                when {
                    sfx != null -> play(sfx)
                    event.key == Key.DirectionRight -> switch(1)
                    event.key == Key.DirectionLeft -> switch(-1)
                    event.key == Key.L -> loop = !loop
                    event.key == Key.Spacebar -> stopAll()
                    else -> return@onKeyEvent false
                }
                true
            }
            .padding(24.dp),
    ) {
        BasicText(
            "← → switch synth · click a row or press its key · L loop: ${if (loop) "on" else "off"} · Space stops all",
            style = TEXT,
        )
        val title = synth?.let { "${it.name} presets  (${current + 1}/${synths.size})" } ?: "Loading…"
        BasicText(title, Modifier.padding(top = 16.dp, bottom = 4.dp), style = TEXT)
        if (synth != null && synth.name !in loaded) BasicText("Rendering…", style = TEXT)
        sounds.forEach { sfx ->
            BasicText("${sfx.keyLabel}  ${sfx.name}", Modifier.clickable { play(sfx) }.padding(vertical = 6.dp), style = TEXT)
        }
    }
}
