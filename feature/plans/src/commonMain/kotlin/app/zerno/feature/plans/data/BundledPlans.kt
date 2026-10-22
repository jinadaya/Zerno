package app.zerno.feature.plans.data

import app.zerno.entities.plan.ReadingPlan
import app.zerno.feature.plans.domain.PlanFormat
import app.zerno.feature.plans.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/** The plans shipped for launch, `composeResources/files/plans`, until the backend serves the catalog. */
class BundledPlans(
    private val read: suspend (path: String) -> ByteArray = { readBundled(it) },
) {
    suspend fun load(): List<ReadingPlan> = IDS.map { id -> PlanFormat.parse(read("files/plans/$id.json").decodeToString()) }

    companion object {
        /** Launch plans in the order the plan list shows them. */
        val IDS: List<String> = listOf("first-steps", "gospel-of-john", "prayer")

        @OptIn(ExperimentalResourceApi::class)
        private suspend fun readBundled(path: String): ByteArray = Res.readBytes(path)
    }
}
