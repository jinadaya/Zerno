package app.zerno.feature.plans.domain

import app.zerno.entities.plan.ReadingPlan
import app.zerno.feature.plans.api.PlanOverview
import app.zerno.feature.plans.api.PlanRunStatus
import kotlinx.datetime.LocalDate

/** The plan list's order: the active plan, then paused ones, then plans not started, then finished ones. */
fun List<PlanOverview>.inListOrder(): List<PlanOverview> = sortedBy { it.status.listRank }

private val PlanRunStatus.listRank: Int
    get() = when (this) {
        PlanRunStatus.Active -> 0
        PlanRunStatus.Paused -> 1
        PlanRunStatus.NotStarted -> 2
        PlanRunStatus.Completed -> 3
    }

/** Share of the plan's days done, 0 to 1; a finished plan is 1. */
val PlanOverview.fraction: Float
    get() = if (status == PlanRunStatus.Completed) 1f else completedDays.size.toFloat() / plan.length

/** How demanding a plan is, from its daily reading time. */
enum class PlanDifficulty { Gentle, Steady, Deep }

val ReadingPlan.difficulty: PlanDifficulty
    get() = when {
        minutesPerDay <= GentleMinutes -> PlanDifficulty.Gentle
        minutesPerDay <= SteadyMinutes -> PlanDifficulty.Steady
        else -> PlanDifficulty.Deep
    }

private const val GentleMinutes = 5
private const val SteadyMinutes = 10

/** "22 Oct 2026" or "22 окт. 2026", for finish dates. Other languages fall back to English. */
fun LocalDate.shortText(language: String): String {
    val months = if (language == "ru") RussianMonths else EnglishMonths
    return "$day ${months[month.ordinal]} $year"
}

private val EnglishMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
private val RussianMonths =
    listOf("янв.", "февр.", "марта", "апр.", "мая", "июня", "июля", "авг.", "сент.", "окт.", "нояб.", "дек.")
