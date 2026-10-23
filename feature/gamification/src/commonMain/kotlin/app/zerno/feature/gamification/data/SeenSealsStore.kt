package app.zerno.feature.gamification.data

import app.zerno.data.db.user.AppMeta
import app.zerno.data.db.user.AppMetaDao
import app.zerno.entities.achievement.AchievementCode

/**
 * Unlocked seals the user has already seen on the achievements screen, so each drops in only once. Stored as
 * comma-separated codes in the `achievements.seen` row of `app_meta`; codes never contain commas.
 */
class SeenSealsStore(private val meta: AppMetaDao) {
    suspend fun seen(): Set<AchievementCode> = meta.get(KEY)
        ?.split(SEPARATOR)
        ?.filter { it.isNotBlank() }
        ?.mapTo(HashSet(), ::AchievementCode)
        .orEmpty()

    /** Adds [codes] to the seen set. */
    suspend fun markSeen(codes: Set<AchievementCode>) {
        val all = seen() + codes
        meta.put(AppMeta(KEY, all.map { it.value }.sorted().joinToString(SEPARATOR)))
    }

    private companion object {
        const val KEY = "achievements.seen"
        const val SEPARATOR = ","
    }
}
