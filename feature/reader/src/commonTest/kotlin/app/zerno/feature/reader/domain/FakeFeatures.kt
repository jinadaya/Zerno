package app.zerno.feature.reader.domain

import app.zerno.entities.achievement.Achievement
import app.zerno.entities.plan.PlanId
import app.zerno.entities.plan.ReadingPlan
import app.zerno.entities.session.ReadingSession
import app.zerno.feature.gamification.api.GamificationEvents
import app.zerno.feature.gamification.api.GamificationState
import app.zerno.feature.gamification.api.ReadingHistory
import app.zerno.feature.gamification.api.StreakState
import app.zerno.feature.plans.api.PlanOverview
import app.zerno.feature.plans.api.PlanToday
import app.zerno.feature.plans.api.Plans
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.datetime.LocalDate

class FakeGamificationState : GamificationState {
    var onRefresh: () -> Unit = {}
    override val streak = MutableStateFlow(StreakState(LocalDate(2026, 10, 22), 0, 0, emptyList(), 0))
    override val achievements = MutableStateFlow<List<Achievement>>(emptyList())
    override val newlyUnlocked: Flow<List<Achievement>> = emptyFlow()
    override val history = MutableStateFlow(ReadingHistory())

    override suspend fun refresh() = onRefresh()
}

class FakePlans : Plans {
    val completed = mutableListOf<Pair<PlanId, Int>>()

    var catalog: List<ReadingPlan> = emptyList()

    override suspend fun catalog(): List<ReadingPlan> = catalog

    override suspend fun pause() = Unit

    override val overview: Flow<List<PlanOverview>> = emptyFlow()

    override suspend fun start(planId: PlanId) = Unit

    override suspend fun completeDay(planId: PlanId, day: Int) {
        completed += planId to day
    }

    override fun today(date: LocalDate): Flow<PlanToday?> = emptyFlow()
}

class FakeEvents : GamificationEvents {
    val sessions = mutableListOf<ReadingSession>()
    var onSession: () -> Unit = {}

    override suspend fun onSessionCompleted(session: ReadingSession) {
        sessions += session
        onSession()
    }

    override suspend fun onReflectionSaved(afterPlanStep: Boolean) = Unit

    override suspend fun onVerseSaved() = Unit

    override suspend fun onQuestionAnswered() = Unit

    override suspend fun onPlanCompleted(wholeBible: Boolean) = Unit
}
