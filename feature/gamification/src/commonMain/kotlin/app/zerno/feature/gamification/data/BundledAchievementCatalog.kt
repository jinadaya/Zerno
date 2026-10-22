package app.zerno.feature.gamification.data

import app.zerno.feature.gamification.domain.achievement.AchievementCatalog
import app.zerno.feature.gamification.domain.achievement.AchievementCatalogParser
import app.zerno.feature.gamification.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/** The achievements shipped with the app, `composeResources/files/achievements.json`, until the backend serves them. */
class BundledAchievementCatalog(private val read: suspend () -> ByteArray = { readBundled() }) {
    suspend fun load(): AchievementCatalog = AchievementCatalogParser.parse(read().decodeToString())

    private companion object {
        const val PATH = "files/achievements.json"

        @OptIn(ExperimentalResourceApi::class)
        suspend fun readBundled(): ByteArray = Res.readBytes(PATH)
    }
}
