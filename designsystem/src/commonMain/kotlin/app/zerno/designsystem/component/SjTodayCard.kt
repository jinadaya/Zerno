package app.zerno.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.zerno.designsystem.foundation.SealShape
import app.zerno.designsystem.foundation.StickerSurface
import app.zerno.designsystem.theme.SjTheme

/** Where the reader stopped in today's passage, for the Continue state. */
@Immutable
data class SjTodayProgress(
    val progress: Float,
    /** "Verse 22 / 54". */
    val position: String,
    /** "40%". */
    val percent: String,
    /** Screen reader name for the line: "Chapter progress". */
    val description: String,
)

/**
 * The Today card (`TodayCard/preview.html`): a `butter` sticker that answers "what do I read now?". A tilted
 * `flare` seal in the corner carries the reading time; then the [tags] (Today, plan day), the passage [theme] as an
 * italic headline (lowercase is fine), the [reference] as a caption, the [progress] line when resuming, and the
 * primary button.
 *
 * [actionLabel] follows the state, in the UI language: Start before reading, Continue with [progress], Read again
 * once today's passage is done. [minutes] and [minutesUnit] fill the seal ("7", "min").
 */
@Composable
fun SjTodayCard(
    tags: List<String>,
    theme: String,
    reference: String,
    minutes: String,
    minutesUnit: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    progress: SjTodayProgress? = null,
) {
    val colors = SjTheme.colors
    val type = SjTheme.type
    Box(modifier.fillMaxWidth()) {
        StickerSurface(colors.butter, Modifier.fillMaxWidth()) {
            Row(
                // Keeps the tags clear of the seal.
                modifier = Modifier.padding(end = SealSize - SjTheme.spacing.space6 + SealInset),
                horizontalArrangement = Arrangement.spacedBy(SjTheme.spacing.space2),
            ) {
                tags.forEach { SjTag(it) }
            }
            BasicText(theme, style = type.titleItalic.copy(color = colors.stickerInk))
            BasicText(reference, style = type.caption.copy(color = colors.stickerInk.copy(alpha = CaptionAlpha)))
            if (progress != null) {
                SjProgressLine(
                    progress = progress.progress,
                    contentDescription = progress.description,
                    startLabel = progress.position,
                    endLabel = progress.percent,
                )
            }
            SjPrimaryButton(actionLabel, onAction, Modifier.fillMaxWidth())
        }
        MinutesSeal(
            minutes = minutes,
            unit = minutesUnit,
            modifier = Modifier.align(Alignment.TopEnd).offset(x = -SealInset, y = -SealOverhang),
        )
    }
}

@Composable
private fun MinutesSeal(minutes: String, unit: String, modifier: Modifier) {
    val colors = SjTheme.colors
    val display = SjTheme.type.mega
    Box(
        modifier = modifier
            .size(SealSize)
            .rotate(SealTilt)
            .background(colors.flare, SealShape)
            .clearAndSetSemantics { contentDescription = "$minutes $unit" },
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = buildAnnotatedString {
                append(minutes)
                append("\n")
                withStyle(SpanStyle(fontSize = 9.sp, fontWeight = SjTheme.type.tag.fontWeight, letterSpacing = 0.06.em)) {
                    append(unit.uppercase())
                }
            },
            style = display.copy(fontSize = 15.sp, lineHeight = 15.sp, letterSpacing = (-0.02).em, color = colors.stickerInk, textAlign = TextAlign.Center),
        )
    }
}

private val SealSize = 76.dp
private val SealInset = 18.dp
private val SealOverhang = 10.dp
private const val SealTilt = 10f
private const val CaptionAlpha = 0.78f

@Composable
private fun TodaySamples() {
    Column(
        modifier = Modifier.background(SjTheme.colors.paper).padding(SjTheme.spacing.gutter).padding(top = SjTheme.spacing.space4),
        verticalArrangement = Arrangement.spacedBy(SjTheme.spacing.space8),
    ) {
        SjTodayCard(
            tags = listOf("Today", "Day 4 / 21"),
            theme = "the woman\nat the well",
            reference = "John 4 · Gospel of John",
            minutes = "7",
            minutesUnit = "min",
            actionLabel = "Continue",
            onAction = {},
            progress = SjTodayProgress(0.4f, "Verse 22 / 54", "40%", "Chapter progress"),
        )
        SjTodayCard(
            tags = listOf("Сегодня", "День 5 / 21"),
            theme = "хлеб жизни",
            reference = "Ин 6 · Евангелие от Иоанна",
            minutes = "9",
            minutesUnit = "мин",
            actionLabel = "Начать",
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun SjTodayCardLightPreview() {
    SjTheme(darkTheme = false) { TodaySamples() }
}

@Preview
@Composable
private fun SjTodayCardDarkPreview() {
    SjTheme(darkTheme = true) { TodaySamples() }
}
