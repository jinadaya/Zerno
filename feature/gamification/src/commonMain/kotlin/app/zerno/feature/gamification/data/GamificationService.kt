package app.zerno.feature.gamification.data

import app.zerno.data.time.TimeZoneProvider
import app.zerno.data.time.today
import app.zerno.entities.achievement.Achievement
import app.zerno.entities.session.ReadingSession
import app.zerno.feature.gamification.api.GamificationEvents
import app.zerno.feature.gamification.api.GamificationState
import app.zerno.feature.gamification.api.StreakDay
import app.zerno.feature.gamification.api.StreakDayStatus
import app.zerno.feature.gamification.api.StreakState
import app.zerno.feature.gamification.domain.achievement.AchievementCatalog
import app.zerno.feature.gamification.domain.achievement.AchievementEvaluator
import app.zerno.feature.gamification.domain.achievement.ActivityCounts
import app.zerno.feature.gamification.domain.achievement.StatsBuilder
import app.zerno.feature.gamification.domain.streak.DayStatus
import app.zerno.feature.gamification.domain.streak.ReadingCompleted
import app.zerno.feature.gamification.domain.streak.ReflectionGiven
import app.zerno.feature.gamification.domain.streak.StreakEngine
import app.zerno.feature.gamification.domain.streak.StreakSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

/**
 * Stores each event, then recomputes the streak and achievements from the whole history. History is small (one
 * session a day), so recomputing beats keeping counters that could drift.
 */
class GamificationService(
    private val repository: GamificationRepository,
    private val loadCatalog: suspend () -> AchievementCatalog,
    private val clock: Clock,
    private val zones: TimeZoneProvider,
    scope: CoroutineScope,
    private val streakEngine: StreakEngine = StreakEngine(),
    private val statsBuilder: StatsBuilder = StatsBuilder(),
) : GamificationEvents,
    GamificationState {
    private val mutex = Mutex()
    private var evaluator: AchievementEvaluator? = null

    private val _streak = MutableStateFlow(emptyStreak())
    override val streak: StateFlow<StreakState> = _streak.asStateFlow()

    private val _achievements = MutableStateFlow<List<Achievement>>(emptyList())
    override val achievements: StateFlow<List<Achievement>> = _achievements.asStateFlow()

    private val _newlyUnlocked = MutableSharedFlow<List<Achievement>>(extraBufferCapacity = 8)
    override val newlyUnlocked: Flow<List<Achievement>> = _newlyUnlocked.asSharedFlow()

    init {
        scope.launch { refresh() }
    }

    override suspend fun onSessionCompleted(session: ReadingSession) {
        if (!session.isCompleted) return
        repository.saveSession(session)
        refresh()
    }

    override suspend fun onReflectionSaved(afterPlanStep: Boolean) =
        record(if (afterPlanStep) ActivityKind.PlanStepReflection else ActivityKind.Reflection)

    override suspend fun onVerseSaved() = record(ActivityKind.VerseSaved)

    override suspend fun onQuestionAnswered() = record(ActivityKind.QuestionAnswered)

    override suspend fun onPlanCompleted(wholeBible: Boolean) =
        record(if (wholeBible) ActivityKind.WholeBiblePlanCompleted else ActivityKind.PlanCompleted)

    private suspend fun record(kind: ActivityKind) {
        repository.record(kind, clock.now(), zones.current())
        refresh()
    }

    override suspend fun refresh() {
        mutex.withLock { recompute() }
    }

    private suspend fun recompute() {
        val now = clock.now()
        val sessions = repository.completedSessions.first()
        val activity = repository.activity.first()

        val readings = sessions.map { ReadingCompleted(requireNotNull(it.completedAt), TimeZone.of(it.timeZoneId)) }
        val reflections = activity.filter { it.kind in ReflectionKinds }.map { ReflectionGiven(it.at, it.zone) }
        val summary = streakEngine.summarize(readings, reflections, now, zones.current())
        val marked = repository.dayMarks.first()
        summary.days.filter { (date, status) ->
            marked[date] != status
        }.forEach { (date, status) -> repository.markDay(date, status) }

        val stats = statsBuilder.build(
            sessions = sessions,
            activity = activity.counts(),
            readDays = summary.days.keys,
            currentStreak = summary.current,
            longestStreak = summary.longest,
        )
        val evaluation = evaluator().evaluate(stats, repository.unlocked.first(), now)
        if (evaluation.newlyUnlocked.isNotEmpty()) repository.saveUnlocks(evaluation.unlockedAt)

        _streak.value = summary.toState()
        _achievements.value = evaluation.achievements
        if (evaluation.newlyUnlocked.isNotEmpty()) _newlyUnlocked.emit(evaluation.newlyUnlocked)
    }

    private suspend fun evaluator(): AchievementEvaluator =
        evaluator ?: AchievementEvaluator(loadCatalog()).also { evaluator = it }

    private fun emptyStreak(): StreakState {
        val today = clock.today(zones)
        return StreakState(today, current = 0, longest = 0, week = emptyList(), daysReadThisWeek = 0)
    }

    private companion object {
        val ReflectionKinds = setOf(ActivityKind.Reflection, ActivityKind.PlanStepReflection)
    }
}

private fun List<Activity>.counts(): ActivityCounts {
    val byKind = groupingBy { it.kind }.eachCount()
    fun count(kind: ActivityKind) = byKind[kind] ?: 0
    return ActivityCounts(
        // A reflection after a plan step is still a reflection.
        reflections = count(ActivityKind.Reflection) + count(ActivityKind.PlanStepReflection),
        planStepReflections = count(ActivityKind.PlanStepReflection),
        versesSaved = count(ActivityKind.VerseSaved),
        questionsAnswered = count(ActivityKind.QuestionAnswered),
        // A whole-Bible plan is still a plan.
        plansCompleted = count(ActivityKind.PlanCompleted) + count(ActivityKind.WholeBiblePlanCompleted),
        wholeBiblePlansCompleted = count(ActivityKind.WholeBiblePlanCompleted),
    )
}

private fun StreakSummary.toState() = StreakState(
    today = today,
    current = current,
    longest = longest,
    week = week.map { StreakDay(it.date, it.status.toApi()) },
    daysReadThisWeek = daysReadThisWeek,
)

private fun DayStatus.toApi(): StreakDayStatus = when (this) {
    DayStatus.Empty -> StreakDayStatus.Empty
    DayStatus.Read -> StreakDayStatus.Read
    DayStatus.Done -> StreakDayStatus.Done
}
