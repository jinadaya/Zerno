package app.zerno.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.zerno.designsystem.theme.SjTheme

/**
 * What a day of the week shows. There is deliberately no "missed" or "broken" mark: a day without reading is
 * simply [Empty].
 */
enum class DayMark {
    /** Nothing read yet, or nothing read that day. A faint outline. */
    Empty,

    /** A reading session was finished. A solid `flare` circle. */
    Read,

    /** Read, and a reflection was written too. `flare` with a small ink dot. */
    Done,
}

/** Screen reader words for the four states, in the UI language: "read", "read and reflected", "today", "not read". */
@Immutable
data class SjStreakWeekLabels(val read: String, val done: String, val today: String, val notRead: String)

/**
 * Seven day circles (`sj-week`) for the current week. [days] has seven marks, Monday (or the locale's first day)
 * first, and [dayLetters] the matching single letters. [today] is the index of today, or null for a past week:
 * an empty today is a dashed `ink` ring, a read today is `flare` like any read day.
 *
 * Put the count of days read this week above it with `SjBigStat`; never count missed days.
 */
@Composable
fun SjStreakWeek(
    days: List<DayMark>,
    dayLetters: List<String>,
    labels: SjStreakWeekLabels,
    modifier: Modifier = Modifier,
    today: Int? = null,
) {
    require(days.size == DaysInWeek && dayLetters.size == DaysInWeek) {
        "SjStreakWeek needs seven days and seven letters, got ${days.size} and ${dayLetters.size}"
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DayGap),
    ) {
        days.forEachIndexed { index, mark ->
            val isToday = index == today
            val state = when {
                mark == DayMark.Done -> labels.done
                mark == DayMark.Read -> labels.read
                isToday -> labels.today
                else -> labels.notRead
            }
            DayCircle(
                letter = dayLetters[index],
                mark = mark,
                isToday = isToday,
                modifier = Modifier
                    .weight(1f)
                    .clearAndSetSemantics { contentDescription = "${dayLetters[index]}: $state" },
            )
        }
    }
}

@Composable
private fun DayCircle(letter: String, mark: DayMark, isToday: Boolean, modifier: Modifier) {
    val colors = SjTheme.colors
    val filled = mark != DayMark.Empty
    val textColor = when {
        filled -> colors.stickerInk
        isToday -> colors.ink
        else -> colors.inkMuted
    }
    Box(modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2
            when {
                filled -> {
                    drawCircle(colors.flare, radius)
                    if (mark == DayMark.Done) {
                        drawCircle(
                            colors.stickerInk,
                            DoneDot.toPx() / 2,
                            Offset(
                                center.x,
                                center.y + radius * DoneDotDrop,
                            ),
                        )
                    }
                }

                isToday -> {
                    val stroke = TodayRing.toPx()
                    val dash = TodayDash.toPx()
                    drawCircle(
                        colors.ink,
                        radius - stroke / 2,
                        style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash))),
                    )
                }

                else -> {
                    val stroke = SjTheme.sizes.hairline.toPx()
                    drawCircle(colors.rule, radius - stroke / 2, style = Stroke(stroke))
                }
            }
        }
        BasicText(
            text = letter,
            style = SjTheme.type.tag.copy(
                fontSize = 12.sp,
                lineHeight = 12.sp,
                letterSpacing = 0.sp,
                color = textColor,
                textAlign = TextAlign.Center,
            ),
            maxLines = 1,
            modifier = if (mark == DayMark.Done) Modifier.padding(bottom = DoneDot) else Modifier,
        )
    }
}

private const val DaysInWeek = 7
private val DayGap = 6.dp
private val TodayRing = 2.dp
private val TodayDash = 4.dp
private val DoneDot = 5.dp

/** How far below the centre the "done" dot sits, as a share of the radius. */
private const val DoneDotDrop = 0.55f

@Composable
private fun StreakSamples() {
    Column(
        modifier = Modifier.background(SjTheme.colors.paper).padding(SjTheme.spacing.gutter),
        verticalArrangement = Arrangement.spacedBy(SjTheme.spacing.space4),
    ) {
        SjBigStat("4", "days read this week")
        SjStreakWeek(
            days = listOf(
                DayMark.Read,
                DayMark.Done,
                DayMark.Empty,
                DayMark.Read,
                DayMark.Read,
                DayMark.Empty,
                DayMark.Empty,
            ),
            dayLetters = listOf("M", "T", "W", "T", "F", "S", "S"),
            labels = SjStreakWeekLabels(
                read = "read",
                done = "read and reflected",
                today = "today",
                notRead = "not read",
            ),
            today = 5,
        )
        SjStreakWeek(
            days = listOf(
                DayMark.Read,
                DayMark.Read,
                DayMark.Done,
                DayMark.Empty,
                DayMark.Empty,
                DayMark.Empty,
                DayMark.Empty,
            ),
            dayLetters = listOf("П", "В", "С", "Ч", "П", "С", "В"),
            labels = SjStreakWeekLabels(
                read = "прочитано",
                done = "прочитано, есть заметка",
                today = "сегодня",
                notRead = "не прочитано",
            ),
            today = 2,
        )
    }
}

@Preview
@Composable
private fun SjStreakWeekLightPreview() {
    SjTheme(darkTheme = false) { StreakSamples() }
}

@Preview
@Composable
private fun SjStreakWeekDarkPreview() {
    SjTheme(darkTheme = true) { StreakSamples() }
}
