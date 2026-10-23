package app.zerno.feature.gamification.ui

import app.zerno.entities.achievement.AchievementCode
import app.zerno.entities.achievement.AchievementProgress
import app.zerno.entities.achievement.AchievementSet
import com.royaal.arch.mvi.InnerEvent
import com.royaal.arch.mvi.MviReducer
import com.royaal.arch.mvi.ScreenState
import com.royaal.arch.mvi.SideEffect
import com.royaal.arch.mvi.UserEvent

/** One seal with its texts in the UI language. [dropIn] for a seal unlocked since the screen was last opened. */
data class SealItem(
    val code: AchievementCode,
    val title: String,
    val description: String,
    val set: AchievementSet,
    val icon: String,
    val unlocked: Boolean,
    val progress: AchievementProgress?,
    val dropIn: Boolean,
)

/** The seals of one set, in catalog order. */
data class SealSection(val set: AchievementSet, val seals: List<SealItem>) {
    val unlocked: Int get() = seals.count { it.unlocked }
}

/** Sections in the order Bird, Sheep, Lamp; null until loaded. */
data class AchievementsState(val sections: List<SealSection>? = null) : ScreenState

sealed interface AchievementsEvent : UserEvent {
    data object BackClicked : AchievementsEvent
}

sealed interface AchievementsInnerEvent : InnerEvent {
    data class Loaded(val sections: List<SealSection>) : AchievementsInnerEvent
}

sealed interface AchievementsEffect : SideEffect {
    data object Close : AchievementsEffect
}

object AchievementsReducer : MviReducer<AchievementsState, AchievementsInnerEvent> {
    override fun reduce(oldState: AchievementsState, event: AchievementsInnerEvent): AchievementsState = when (event) {
        is AchievementsInnerEvent.Loaded -> oldState.copy(sections = event.sections)
    }
}
