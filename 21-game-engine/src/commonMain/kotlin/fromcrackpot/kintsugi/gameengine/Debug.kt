package fromcrackpot.kintsugi.gameengine

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fromcrackpot.kintsugi.scenegraph.nodes.GroupNode
import kotlin.math.roundToInt

internal class FrameHistory(private val capacity: Int = 600) {

    private val frames = ArrayDeque<GroupNode>()
    private var behindHead = 0

    fun push(frame: GroupNode) {
        if (frames.size == capacity) frames.removeFirst()
        frames.addLast(frame)
    }

    fun back(): GroupNode? {
        if (behindHead >= frames.lastIndex) return null
        behindHead++
        return frames[frames.lastIndex - behindHead]
    }

    fun forward(): GroupNode? {
        if (behindHead == 0) return null
        behindHead--
        return frames[frames.lastIndex - behindHead]
    }

    fun toHead(): GroupNode {
        behindHead = 0
        return frames.last()
    }
}

internal class FrameStats(private val windowNanos: Long = 500_000_000L) {

    private var elapsed = 0L
    private var frames = 0
    private var worst = 0L

    fun record(intervalNanos: Long): String? {
        elapsed += intervalNanos
        frames++
        worst = maxOf(worst, intervalNanos)
        if (elapsed < windowNanos) return null
        val label = "${(frames * 1e9 / elapsed).roundToInt()} fps · ${(worst / 1e6).roundToInt()} ms"
        elapsed = 0L
        frames = 0
        worst = 0L
        return label
    }
}

@Composable
internal fun Modifier.debugOverlay(label: () -> String?): Modifier {
    val measurer = rememberTextMeasurer()
    return drawWithContent {
        drawContent()
        val text = label() ?: return@drawWithContent
        val layout = measurer.measure(text, TextStyle(color = Color.White, fontSize = 14.sp))
        val margin = 8.dp.toPx()
        val padding = 4.dp.toPx()
        drawRect(
            color = Color.Black.copy(alpha = 0.7f),
            topLeft = Offset(margin, margin),
            size = Size(layout.size.width + 2 * padding, layout.size.height + 2 * padding),
        )
        drawText(layout, topLeft = Offset(margin + padding, margin + padding))
    }
}
