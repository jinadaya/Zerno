package app.zerno.feature.gamification.domain.streak

import kotlinx.datetime.LocalDate

/** How far the user got on one local date. */
enum class DayStatus {
    /** Nothing completed. */
    Empty,

    /** Reading completed. Counts for the streak. */
    Read,

    /** Reading completed and a reflection given the same day. */
    Done,
    ;

    val countsForStreak: Boolean get() = this != Empty
}

data class DayMark(val date: LocalDate, val status: DayStatus)

data class StreakSummary(
    /** The user's local date at the moment of the summary. */
    val today: LocalDate,
    /** Every date with at least a reading; dates not in the map are [DayStatus.Empty]. */
    val days: Map<LocalDate, DayStatus>,
    /** Consecutive days that are at least [DayStatus.Read], ending today or yesterday. */
    val current: Int,
    val longest: Int,
    /** The seven days of the week that contains [today], in order. */
    val week: List<DayMark>,
    val daysReadThisWeek: Int,
)
