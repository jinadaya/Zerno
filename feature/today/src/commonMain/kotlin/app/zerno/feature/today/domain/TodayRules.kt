package app.zerno.feature.today.domain

import app.zerno.entities.bible.BookNameLanguage
import app.zerno.entities.bible.PassageRef
import app.zerno.entities.bible.Versification
import app.zerno.entities.bible.format
import app.zerno.entities.bible.mapVersification
import app.zerno.entities.plan.PlanDay
import app.zerno.entities.plan.ReadingPlan
import app.zerno.feature.gamification.api.StreakState
import app.zerno.feature.plans.api.PlanToday
import app.zerno.feature.reader.api.PassageProgress

/** The Today card's button: Start before reading, Continue part way, Read again once the day is done. */
enum class TodayAction { Start, Continue, ReadAgain }

/** What the Today card shows. [progress] is set only for [TodayAction.Continue]. */
data class TodayCard(
    val plan: ReadingPlan,
    val day: PlanDay,
    val action: TodayAction,
    val progress: PassageProgress? = null,
) {
    /** Where the reader opens: the day's first passage. */
    val passage: PassageRef
        get() = day.passages.first()
}

/** A done day always reads "Read again"; otherwise any progress past the start of the passage means Continue. */
fun todayCard(today: PlanToday, progress: PassageProgress?): TodayCard = when {
    today.done -> TodayCard(today.plan, today.day, TodayAction.ReadAgain)
    progress != null -> TodayCard(today.plan, today.day, TodayAction.Continue, progress)
    else -> TodayCard(today.plan, today.day, TodayAction.Start)
}

/**
 * The day's passages as the UI language writes them: "John 4", or "Пс 22" in Russian, whose readers use the
 * Synodal numbering.
 */
fun PlanDay.reference(language: BookNameLanguage): String = passages.joinToString(", ") { passage ->
    when (language) {
        BookNameLanguage.English -> passage.format(language)

        BookNameLanguage.Russian -> passage.mapVersification(Versification.English, Versification.Russian)
            .format(language)
    }
}

/** Position of today in the week, or null before the streak is first computed. */
val StreakState.todayIndex: Int?
    get() = week.indexOfFirst { it.date == today }.takeIf { it >= 0 }
