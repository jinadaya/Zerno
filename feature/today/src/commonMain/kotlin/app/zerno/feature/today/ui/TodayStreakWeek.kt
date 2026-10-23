package app.zerno.feature.today.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import app.zerno.designsystem.component.DayMark
import app.zerno.designsystem.component.SjBigStat
import app.zerno.designsystem.component.SjStreakWeek
import app.zerno.designsystem.component.SjStreakWeekLabels
import app.zerno.designsystem.theme.SjTheme
import app.zerno.feature.gamification.api.StreakDayStatus
import app.zerno.feature.gamification.api.StreakState
import app.zerno.feature.today.domain.todayIndex
import app.zerno.feature.today.resources.Res
import app.zerno.feature.today.resources.today_day_done
import app.zerno.feature.today.resources.today_day_not_read
import app.zerno.feature.today.resources.today_day_read
import app.zerno.feature.today.resources.today_day_today
import app.zerno.feature.today.resources.today_days_read
import app.zerno.feature.today.resources.today_friday
import app.zerno.feature.today.resources.today_monday
import app.zerno.feature.today.resources.today_saturday
import app.zerno.feature.today.resources.today_sunday
import app.zerno.feature.today.resources.today_thursday
import app.zerno.feature.today.resources.today_tuesday
import app.zerno.feature.today.resources.today_wednesday
import kotlinx.datetime.DayOfWeek
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** Days read this week and the seven day circles. Never counts missed days. Hidden until the week is computed. */
@Composable
internal fun StreakWeek(streak: StreakState) {
    if (streak.week.size != DaysInWeek) return
    Column(verticalArrangement = Arrangement.spacedBy(SjTheme.spacing.space4)) {
        SjBigStat(
            streak.daysReadThisWeek.toString(),
            pluralStringResource(Res.plurals.today_days_read, streak.daysReadThisWeek),
        )
        SjStreakWeek(
            days = streak.week.map { it.status.mark },
            dayLetters = streak.week.map { stringResource(it.date.dayOfWeek.letter) },
            labels = SjStreakWeekLabels(
                read = stringResource(Res.string.today_day_read),
                done = stringResource(Res.string.today_day_done),
                today = stringResource(Res.string.today_day_today),
                notRead = stringResource(Res.string.today_day_not_read),
            ),
            today = streak.todayIndex,
        )
    }
}

private val StreakDayStatus.mark: DayMark
    get() = when (this) {
        StreakDayStatus.Empty -> DayMark.Empty
        StreakDayStatus.Read -> DayMark.Read
        StreakDayStatus.Done -> DayMark.Done
    }

private val DayOfWeek.letter: StringResource
    get() = when (this) {
        DayOfWeek.MONDAY -> Res.string.today_monday
        DayOfWeek.TUESDAY -> Res.string.today_tuesday
        DayOfWeek.WEDNESDAY -> Res.string.today_wednesday
        DayOfWeek.THURSDAY -> Res.string.today_thursday
        DayOfWeek.FRIDAY -> Res.string.today_friday
        DayOfWeek.SATURDAY -> Res.string.today_saturday
        DayOfWeek.SUNDAY -> Res.string.today_sunday
    }

private const val DaysInWeek = 7
