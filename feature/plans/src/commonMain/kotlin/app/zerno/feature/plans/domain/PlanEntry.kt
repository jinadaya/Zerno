package app.zerno.feature.plans.domain

import app.zerno.entities.plan.PlanId
import app.zerno.entities.plan.ReadingPlan

/** A plan with the file it was read from, so the catalog can be stored as it was served. */
data class PlanEntry(val plan: ReadingPlan, val json: String)

/** Reads served plan files, leaving out any this version of the app cannot read (a newer format, a bad reference). */
fun parsePlanEntries(files: List<String>): List<PlanEntry> = files.mapNotNull { file ->
    try {
        PlanEntry(PlanFormat.parse(file), file)
    } catch (_: InvalidPlanFile) {
        null
    }
}

/**
 * The catalog after a refresh: the served plans in the server's order, then any plan in [started] the server no
 * longer lists, taken from [previous], so a plan the user is on never disappears.
 */
fun mergeCatalog(served: List<PlanEntry>, previous: List<PlanEntry>, started: Set<PlanId>): List<PlanEntry> {
    val servedIds = served.map { it.plan.id }.toSet()
    return served + previous.filter { it.plan.id in started && it.plan.id !in servedIds }
}
