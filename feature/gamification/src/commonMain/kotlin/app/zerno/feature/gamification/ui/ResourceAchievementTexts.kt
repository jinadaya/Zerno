package app.zerno.feature.gamification.ui

import app.zerno.entities.achievement.Achievement
import app.zerno.feature.gamification.api.AchievementTexts
import app.zerno.feature.gamification.resources.Res
import app.zerno.feature.gamification.resources.allStringResources
import org.jetbrains.compose.resources.getString

/**
 * [AchievementTexts] from this module's strings, found by the keys the catalog names. A key the app does not have
 * (a definition newer than the app) shows as the key itself rather than failing.
 */
class ResourceAchievementTexts : AchievementTexts {
    override suspend fun title(achievement: Achievement): String = text(achievement.titleKey)

    override suspend fun description(achievement: Achievement): String = text(achievement.descriptionKey)

    private suspend fun text(key: String): String = Res.allStringResources[key]?.let { getString(it) } ?: key
}
