package app.zerno.feature.plans.data

import app.zerno.data.db.user.AppMeta
import app.zerno.data.db.user.AppMetaDao
import app.zerno.entities.plan.PlanId
import app.zerno.feature.plans.domain.PlanJourney
import app.zerno.feature.plans.domain.PlanProgress
import app.zerno.feature.plans.domain.PlanStatus
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * [PlanJourney] as JSON in the `plans.journey` row of the `app_meta` key-value table, so the prototype needs no schema
 * change. A value the app cannot read back counts as no plans started.
 */
class PlanJourneyStore(private val meta: AppMetaDao) {
    suspend fun load(): PlanJourney = meta.get(KEY)?.let { value ->
        runCatching { json.decodeFromString<StoredJourney>(value).toJourney() }.getOrNull()
    } ?: PlanJourney()

    suspend fun save(journey: PlanJourney) {
        meta.put(AppMeta(KEY, json.encodeToString(StoredJourney.from(journey))))
    }

    @Serializable
    private data class StoredJourney(val plans: List<StoredProgress>) {
        fun toJourney() = PlanJourney(plans.associate { PlanId(it.planId) to it.toProgress() })

        companion object {
            fun from(journey: PlanJourney) = StoredJourney(journey.plans.values.map(StoredProgress::from))
        }
    }

    @Serializable
    private data class StoredProgress(
        val planId: String,
        val length: Int,
        val status: String,
        val startedOn: String,
        val completed: Map<Int, String> = emptyMap(),
        val completedOn: String? = null,
        val timesCompleted: Int = 0,
    ) {
        fun toProgress() = PlanProgress(
            planId = PlanId(planId),
            length = length,
            status = PlanStatus.valueOf(status),
            startedOn = LocalDate.parse(startedOn),
            completed = completed.mapValues { LocalDate.parse(it.value) },
            completedOn = completedOn?.let(LocalDate::parse),
            timesCompleted = timesCompleted,
        )

        companion object {
            fun from(progress: PlanProgress) = StoredProgress(
                planId = progress.planId.value,
                length = progress.length,
                status = progress.status.name,
                startedOn = progress.startedOn.toString(),
                completed = progress.completed.mapValues { it.value.toString() },
                completedOn = progress.completedOn?.toString(),
                timesCompleted = progress.timesCompleted,
            )
        }
    }

    private companion object {
        const val KEY = "plans.journey"
        val json = Json { ignoreUnknownKeys = true }
    }
}
