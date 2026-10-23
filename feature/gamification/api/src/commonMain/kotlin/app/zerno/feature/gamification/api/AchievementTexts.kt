package app.zerno.feature.gamification.api

import app.zerno.entities.achievement.Achievement

/** Achievement titles and descriptions in the UI language, for screens outside gamification. */
interface AchievementTexts {
    suspend fun title(achievement: Achievement): String

    suspend fun description(achievement: Achievement): String
}
