package app.zerno.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.zerno.designsystem.foundation.StickerSurface
import app.zerno.designsystem.theme.LocalOnSticker
import app.zerno.designsystem.theme.SjTheme

/**
 * Progress as a line (`sj-line`): a 2 dp track, a 4 dp fill in the content colour, and a `flare` dot where the
 * reader is. An optional label row sits underneath in the uppercase `tag` style: what on the left
 * ([startLabel], "Verse 22 / 54"), the percent on the right ([endLabel], "40%").
 *
 * [progress] is 0..1. The fill grows over 600 ms from zero when it first shows and whenever the value goes up; a
 * lower value (a new chapter) jumps there at once, so the line never runs backwards. Screen readers get a progress
 * bar named by [contentDescription] with the current value.
 */
@Composable
fun SjProgressLine(
    progress: Float,
    contentDescription: String,
    modifier: Modifier = Modifier,
    startLabel: String? = null,
    endLabel: String? = null,
) {
    val target = progress.coerceIn(0f, 1f)
    val colors = SjTheme.colors
    val onSticker = LocalOnSticker.current
    val content = SjTheme.contentColor
    val track = if (onSticker) colors.stickerInk.copy(alpha = StickerTrackAlpha) else colors.rule
    val spec = SjTheme.motion.progress<Float>()
    val shown = remember { Animatable(0f) }
    LaunchedEffect(target) {
        if (target >= shown.value) shown.animateTo(target, spec) else shown.snapTo(target)
    }

    Column(
        modifier = modifier.clearAndSetSemantics {
            this.contentDescription = contentDescription
            progressBarRangeInfo = ProgressBarRangeInfo(target, 0f..1f)
        },
        verticalArrangement = Arrangement.spacedBy(SjTheme.spacing.space2),
    ) {
        Canvas(Modifier.fillMaxWidth().height(DotSize)) {
            val mid = size.height / 2
            val trackHeight = TrackHeight.toPx()
            drawRect(track, Offset(0f, mid - trackHeight / 2), Size(size.width, trackHeight))

            val end = size.width * shown.value
            val fillHeight = FillHeight.toPx()
            drawRoundRect(
                color = content,
                topLeft = Offset(0f, mid - fillHeight / 2),
                size = Size(end, fillHeight),
                cornerRadius = CornerRadius(fillHeight / 2),
            )

            val radius = DotSize.toPx() / 2
            val center = Offset(end, mid)
            if (onSticker) {
                drawCircle(colors.flare, radius, center)
                val ring = DotRing.toPx()
                drawCircle(colors.stickerInk, radius - ring / 2, center, style = Stroke(ring))
            } else {
                drawCircle(colors.paper, radius + DotRing.toPx(), center)
                drawCircle(colors.flare, radius, center)
            }
        }
        if (startLabel != null || endLabel != null) {
            val style = SjTheme.type.tag.copy(color = content)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                BasicText(startLabel.orEmpty().uppercase(), style = style, maxLines = 1)
                BasicText(endLabel.orEmpty().uppercase(), style = style, maxLines = 1)
            }
        }
    }
}

private val TrackHeight = 2.dp
private val FillHeight = 4.dp
private val DotSize = 12.dp
private val DotRing = 2.dp
private const val StickerTrackAlpha = 0.18f

@Composable
private fun ProgressSamples() {
    Column(
        modifier = Modifier.background(SjTheme.colors.paper).padding(SjTheme.spacing.gutter),
        verticalArrangement = Arrangement.spacedBy(SjTheme.spacing.space6),
    ) {
        SjProgressLine(0.4f, "Chapter progress", startLabel = "Verse 22 / 54", endLabel = "40%")
        SjProgressLine(0.12f, "Whole Bible progress", startLabel = "139 / 1,189 chapters", endLabel = "12%")
        StickerSurface(SjTheme.colors.butter) {
            SjProgressLine(0.4f, "Прогресс главы", startLabel = "Стих 22 / 54", endLabel = "40%")
        }
        SjBigStat("38", "of John read", unit = "%")
        SjBigStat("23", "дня со Священным Писанием в этом месяце")
    }
}

@Preview
@Composable
private fun SjProgressLightPreview() {
    SjTheme(darkTheme = false) { ProgressSamples() }
}

@Preview
@Composable
private fun SjProgressDarkPreview() {
    SjTheme(darkTheme = true) { ProgressSamples() }
}
