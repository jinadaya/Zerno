package app.zerno.feature.progress.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.zerno.designsystem.icon.SjIcon
import app.zerno.designsystem.icon.SjIcons
import app.zerno.designsystem.theme.SjTheme
import app.zerno.entities.bible.BookNameLanguage
import app.zerno.entities.bible.BookNameStyle
import app.zerno.entities.bible.BookNames
import app.zerno.entities.bible.Testament
import app.zerno.feature.gamification.api.StreakDayStatus
import app.zerno.feature.progress.domain.BookCoverage
import app.zerno.feature.progress.domain.CalendarDay
import app.zerno.feature.progress.domain.MonthCalendar
import app.zerno.feature.progress.domain.monthTitle
import app.zerno.feature.progress.resources.Res
import app.zerno.feature.progress.resources.progress_book_coverage
import app.zerno.feature.progress.resources.progress_books
import app.zerno.feature.progress.resources.progress_day_done
import app.zerno.feature.progress.resources.progress_day_not_read
import app.zerno.feature.progress.resources.progress_day_read
import app.zerno.feature.progress.resources.progress_friday
import app.zerno.feature.progress.resources.progress_monday
import app.zerno.feature.progress.resources.progress_month_next
import app.zerno.feature.progress.resources.progress_month_previous
import app.zerno.feature.progress.resources.progress_new_testament
import app.zerno.feature.progress.resources.progress_old_testament
import app.zerno.feature.progress.resources.progress_saturday
import app.zerno.feature.progress.resources.progress_sunday
import app.zerno.feature.progress.resources.progress_thursday
import app.zerno.feature.progress.resources.progress_today
import app.zerno.feature.progress.resources.progress_tuesday
import app.zerno.feature.progress.resources.progress_wednesday
import org.jetbrains.compose.resources.stringResource

/** Every book as a small tile: faint until started, tinted while reading, solid once every chapter is read. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BookCoverageSection(coverage: List<BookCoverage>) {
    if (coverage.isEmpty()) return
    val language = BookNameLanguage.fromCode(Locale.current.language)
    Column(verticalArrangement = Arrangement.spacedBy(SjTheme.spacing.space3)) {
        SectionTitle(stringResource(Res.string.progress_books))
        coverage.groupBy { it.book.testament }.forEach { (testament, books) ->
            BasicText(
                stringResource(
                    if (testament ==
                        Testament.Old
                    ) {
                        Res.string.progress_old_testament
                    } else {
                        Res.string.progress_new_testament
                    },
                ),
                style = SjTheme.type.tag.copy(color = SjTheme.colors.inkMuted),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(SjTheme.spacing.space1),
                verticalArrangement = Arrangement.spacedBy(SjTheme.spacing.space1),
            ) {
                books.forEach { BookTile(it, language) }
            }
        }
    }
}

@Composable
private fun BookTile(book: BookCoverage, language: BookNameLanguage) {
    val colors = SjTheme.colors
    val description = stringResource(
        Res.string.progress_book_coverage,
        BookNames.name(book.book, language, BookNameStyle.Long),
        book.chaptersRead,
        book.chapters,
    )
    val fill = when {
        book.finished -> colors.flare
        book.chaptersRead > 0 -> colors.flare.copy(alpha = StartedAlpha)
        else -> Color.Transparent
    }
    val rule = colors.rule
    Box(
        Modifier
            .size(width = TileWidth, height = TileHeight)
            .drawBehind {
                val radius = CornerRadius(TileRadius.toPx())
                drawRoundRect(fill, cornerRadius = radius)
                if (book.chaptersRead == 0) drawRoundRect(rule, cornerRadius = radius, style = Stroke(1.dp.toPx()))
            }
            .semantics(mergeDescendants = true) { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            BookNames.name(book.book, language),
            style = SjTheme.type.caption.copy(color = if (book.finished) colors.stickerInk else colors.ink),
            maxLines = 1,
        )
    }
}

private const val StartedAlpha = 0.35f
private val TileWidth = 48.dp
private val TileHeight = 32.dp
private val TileRadius = 8.dp

/** One month of reading days; arrows move back to the first reading and forward to this month. */
@Composable
internal fun MonthCalendarSection(calendar: MonthCalendar, onPrevious: () -> Unit, onNext: () -> Unit) {
    val language = Locale.current.language
    Column(verticalArrangement = Arrangement.spacedBy(SjTheme.spacing.space3)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicText(
                calendar.month.monthTitle(language),
                Modifier.weight(1f).semantics { heading() },
                style = SjTheme.type.headline.copy(color = SjTheme.colors.ink),
            )
            MonthArrow(
                SjIcons.ChevronLeft,
                stringResource(Res.string.progress_month_previous),
                calendar.hasPrevious,
                onPrevious,
            )
            MonthArrow(SjIcons.ChevronRight, stringResource(Res.string.progress_month_next), calendar.hasNext, onNext)
        }
        Row(Modifier.fillMaxWidth()) {
            WeekdayLetters.forEach { letter ->
                BasicText(
                    stringResource(letter),
                    Modifier.weight(1f),
                    style = SjTheme.type.caption.copy(color = SjTheme.colors.inkMuted, textAlign = TextAlign.Center),
                )
            }
        }
        val cells: List<CalendarDay?> = List(calendar.leadingBlanks) { null } + calendar.days
        cells.chunked(DaysInWeek).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                        day?.let { DayCell(it) }
                    }
                }
                repeat(DaysInWeek - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun MonthArrow(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(SjTheme.sizes.tapMin)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        SjIcon(icon, label, tint = if (enabled) SjTheme.colors.ink else SjTheme.colors.rule)
    }
}

@Composable
private fun DayCell(day: CalendarDay) {
    val colors = SjTheme.colors
    val status = stringResource(
        when (day.status) {
            StreakDayStatus.Empty -> Res.string.progress_day_not_read
            StreakDayStatus.Read -> Res.string.progress_day_read
            StreakDayStatus.Done -> Res.string.progress_day_done
        },
    )
    val today = stringResource(Res.string.progress_today)
    val description = listOfNotNull(day.date.day.toString(), status, today.takeIf { day.isToday }).joinToString(", ")
    val filled = day.status != StreakDayStatus.Empty
    val flare = colors.flare
    val ink = colors.ink
    val stickerInk = colors.stickerInk
    Box(
        Modifier
            .sizeIn(maxWidth = CellSize, maxHeight = CellSize)
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(2.dp)
            .drawBehind {
                if (filled) drawCircle(flare)
                if (day.isToday && !filled) drawCircle(ink, style = Stroke(1.5.dp.toPx()))
                if (day.status == StreakDayStatus.Done) {
                    drawCircle(stickerInk, radius = DoneDot.toPx(), center = center.copy(y = size.height * DotY))
                }
            }
            .semantics(mergeDescendants = true) { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            day.date.day.toString(),
            style = SjTheme.type.caption.copy(color = if (filled) colors.stickerInk else colors.ink),
        )
    }
}

private val CellSize = 40.dp
private val DoneDot = 2.dp
private const val DotY = 0.8f
private const val DaysInWeek = 7

private val WeekdayLetters = listOf(
    Res.string.progress_monday,
    Res.string.progress_tuesday,
    Res.string.progress_wednesday,
    Res.string.progress_thursday,
    Res.string.progress_friday,
    Res.string.progress_saturday,
    Res.string.progress_sunday,
)
