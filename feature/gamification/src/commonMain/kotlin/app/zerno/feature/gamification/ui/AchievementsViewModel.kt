package app.zerno.feature.gamification.ui

import androidx.lifecycle.SavedStateHandle
import app.zerno.entities.achievement.Achievement
import app.zerno.entities.achievement.AchievementCode
import app.zerno.entities.achievement.AchievementSet
import app.zerno.feature.gamification.api.AchievementTexts
import app.zerno.feature.gamification.api.GamificationState
import app.zerno.feature.gamification.data.SeenSealsStore
import com.royaal.arch.mvi.ScreenSnapshot
import com.royaal.arch.mvi.SnapshotHandler
import com.royaal.arch.mvi.viewmodel.MviViewModel

/**
 * Every seal by set, unlocked and locked with progress. Seals unlocked since the screen was last opened drop in
 * once: the seen set is read when the screen opens, and every unlocked seal shown is then remembered as seen.
 */
class AchievementsViewModel(
    savedStateHandle: SavedStateHandle,
    private val gamification: GamificationState,
    private val texts: AchievementTexts,
    private val seen: SeenSealsStore,
) : MviViewModel<
    AchievementsState,
    AchievementsEvent,
    AchievementsInnerEvent,
    AchievementsEffect,
    ScreenSnapshot.NoSnapshot,
    >(
    savedStateHandle = savedStateHandle,
    initialState = AchievementsState(),
    reducer = AchievementsReducer,
    snapshotHandler = SnapshotHandler.noSnapshot(AchievementsState()),
) {
    init {
        subscription(LOAD) {
            val seenAtOpen = seen.seen()
            // The list is empty until gamification first computes it.
            if (gamification.achievements.value.isEmpty()) gamification.refresh()
            gamification.achievements.collect { achievements ->
                if (achievements.isEmpty()) return@collect
                emit(AchievementsInnerEvent.Loaded(sections(achievements, seenAtOpen)))
                seen.markSeen(achievements.filter { it.isUnlocked }.mapTo(HashSet()) { it.code })
            }
        }
    }

    override fun onEvent(event: AchievementsEvent) {
        when (event) {
            AchievementsEvent.BackClicked -> intent(CLOSE) { postEffect(AchievementsEffect.Close) }
        }
    }

    private suspend fun sections(achievements: List<Achievement>, seenAtOpen: Set<AchievementCode>): List<SealSection> {
        val bySet = achievements.groupBy { it.set }
        return SetOrder.mapNotNull { set ->
            val seals = bySet[set] ?: return@mapNotNull null
            SealSection(set, seals.map { it.toItem(dropIn = it.isUnlocked && it.code !in seenAtOpen) })
        }
    }

    private suspend fun Achievement.toItem(dropIn: Boolean) = SealItem(
        code = code,
        title = texts.title(this),
        description = texts.description(this),
        set = set,
        icon = icon,
        unlocked = isUnlocked,
        progress = progress,
        dropIn = dropIn,
    )

    private companion object {
        const val LOAD = "load"
        const val CLOSE = "close"

        /** Pip (habit), Woolly (reading), Lumi (reflection). */
        val SetOrder = listOf(AchievementSet.Bird, AchievementSet.Sheep, AchievementSet.Lamp)
    }
}
