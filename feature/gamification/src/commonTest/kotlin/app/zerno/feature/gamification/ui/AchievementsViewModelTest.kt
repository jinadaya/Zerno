package app.zerno.feature.gamification.ui

import androidx.lifecycle.SavedStateHandle
import app.zerno.data.db.user.AppMeta
import app.zerno.data.db.user.AppMetaDao
import app.zerno.entities.achievement.Achievement
import app.zerno.entities.achievement.AchievementCode
import app.zerno.entities.achievement.AchievementProgress
import app.zerno.entities.achievement.AchievementSet
import app.zerno.feature.gamification.api.AchievementTexts
import app.zerno.feature.gamification.api.GamificationState
import app.zerno.feature.gamification.api.ReadingHistory
import app.zerno.feature.gamification.api.StreakState
import app.zerno.feature.gamification.data.SeenSealsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class AchievementsViewModelTest {
    private val rows = mutableMapOf<String, String>()
    private val seen = SeenSealsStore(
        object : AppMetaDao {
            override suspend fun get(key: String) = rows[key]
            override suspend fun put(meta: AppMeta) {
                rows[meta.key] = meta.value
            }
        },
    )
    private val gamification = FakeGamification()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = AchievementsViewModel(SavedStateHandle(), gamification, FakeTexts, seen)

    private fun seal(code: String, set: AchievementSet, unlocked: Boolean, progress: AchievementProgress? = null) =
        Achievement(
            code = AchievementCode(code),
            set = set,
            titleKey = "${code}_title",
            descriptionKey = "${code}_description",
            icon = "Bible",
            unlockedAt = if (unlocked) Instant.parse("2026-10-20T08:00:00Z") else null,
            progress = progress,
        )

    @Test
    fun sectionsComeInPipWoollyLumiOrderWithTexts() = runTest {
        gamification.achievements.value = listOf(
            seal("first_steps", AchievementSet.Sheep, unlocked = true),
            seal("reflect_1", AchievementSet.Lamp, unlocked = false),
            seal("streak_3", AchievementSet.Bird, unlocked = false, AchievementProgress(1, 3)),
        )

        val viewModel = viewModel()
        advanceUntilIdle()

        val sections = viewModel.state.value.sections.orEmpty()
        assertEquals(listOf(AchievementSet.Bird, AchievementSet.Sheep, AchievementSet.Lamp), sections.map { it.set })
        assertEquals("first_steps_title", sections[1].seals.single().title)
        assertEquals(1, sections[1].unlocked)
        assertEquals(AchievementProgress(1, 3), sections[0].seals.single().progress)
    }

    @Test
    fun aNewSealDropsInOnlyTheFirstTimeTheScreenOpens() = runTest {
        gamification.achievements.value = listOf(
            seal("first_steps", AchievementSet.Sheep, unlocked = true),
            seal("chapters_10", AchievementSet.Sheep, unlocked = false),
        )

        val first = viewModel()
        advanceUntilIdle()
        val firstSeals = first.state.value.sections.orEmpty().single().seals
        assertTrue(firstSeals.single { it.code.value == "first_steps" }.dropIn)
        assertFalse(firstSeals.single { it.code.value == "chapters_10" }.dropIn)

        val second = viewModel()
        advanceUntilIdle()
        assertFalse(second.state.value.sections.orEmpty().single().seals.any { it.dropIn })
    }

    @Test
    fun anEmptyListAsksGamificationToCompute() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals(1, gamification.refreshes)
        assertNull(viewModel.state.value.sections)
    }

    @Test
    fun backCloses() = runTest {
        val viewModel = viewModel()

        viewModel.sendEvent(AchievementsEvent.BackClicked)

        assertEquals(AchievementsEffect.Close, viewModel.effects.first())
    }

    private object FakeTexts : AchievementTexts {
        override suspend fun title(achievement: Achievement) = achievement.titleKey

        override suspend fun description(achievement: Achievement) = achievement.descriptionKey
    }

    private class FakeGamification : GamificationState {
        var refreshes = 0
        override val streak = MutableStateFlow(StreakState(LocalDate(2026, 10, 23), 0, 0, emptyList(), 0))
        override val achievements = MutableStateFlow<List<Achievement>>(emptyList())
        override val newlyUnlocked: Flow<List<Achievement>> = emptyFlow()
        override val history = MutableStateFlow(ReadingHistory())

        override suspend fun refresh() {
            refreshes++
        }
    }
}
