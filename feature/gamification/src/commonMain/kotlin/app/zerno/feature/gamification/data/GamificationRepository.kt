package app.zerno.feature.gamification.data

import app.zerno.data.db.user.ActivityEventRow
import app.zerno.data.db.user.DayMarkRow
import app.zerno.data.db.user.GamificationDao
import app.zerno.data.db.user.ReadingSessionRow
import app.zerno.data.db.user.UnlockedAchievementRow
import app.zerno.entities.achievement.AchievementCode
import app.zerno.entities.bible.PassageRef
import app.zerno.entities.bible.TranslationId
import app.zerno.entities.bible.format
import app.zerno.entities.bible.parseOrThrow
import app.zerno.entities.plan.PlanId
import app.zerno.entities.session.ReadingSession
import app.zerno.entities.session.SessionId
import app.zerno.feature.gamification.domain.streak.DayStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.time.Instant

/** Things other features report that achievements count. Stored by name, so never rename an entry. */
enum class ActivityKind {
    Reflection,
    PlanStepReflection,
    VerseSaved,
    QuestionAnswered,
    PlanCompleted,
    WholeBiblePlanCompleted,
}

data class Activity(val kind: ActivityKind, val at: Instant, val zone: TimeZone)

/** Gamification's stored state in `UserDb`: sessions, activity, day marks and unlocks. */
class GamificationRepository(private val dao: GamificationDao) {
    val completedSessions: Flow<List<ReadingSession>> =
        dao.completedSessions().map { rows -> rows.map { it.toSession() } }

    val activity: Flow<List<Activity>> = dao.events().map { rows ->
        // A kind this version does not know (written by a newer one) is skipped, not a crash.
        rows.mapNotNull { row ->
            val kind = ActivityKind.entries.firstOrNull { it.name == row.kind } ?: return@mapNotNull null
            Activity(kind, Instant.fromEpochMilliseconds(row.at), TimeZone.of(row.timeZone))
        }
    }

    val dayMarks: Flow<Map<LocalDate, DayStatus>> = dao.dayMarks().map { rows ->
        rows.associate { LocalDate.parse(it.date) to DayStatus.valueOf(it.status) }
    }

    val unlocked: Flow<Map<AchievementCode, Instant>> = dao.unlocked().map { rows ->
        rows.associate { AchievementCode(it.code) to Instant.fromEpochMilliseconds(it.unlockedAt) }
    }

    suspend fun saveSession(session: ReadingSession) = dao.putSession(session.toRow())

    suspend fun record(kind: ActivityKind, at: Instant, zone: TimeZone) =
        dao.addEvent(ActivityEventRow(kind = kind.name, at = at.toEpochMilliseconds(), timeZone = zone.id))

    /** Raises [date] to [status]; a day never goes down, so a later reading never undoes a reflection. */
    suspend fun markDay(date: LocalDate, status: DayStatus) {
        if (status == DayStatus.Empty) return
        val current = dao.dayMark(date.toString())?.let { DayStatus.valueOf(it) } ?: DayStatus.Empty
        if (status > current) dao.putDayMark(DayMarkRow(date.toString(), status.name))
    }

    /** Stores new unlocks; codes already unlocked keep their first date. */
    suspend fun saveUnlocks(unlocks: Map<AchievementCode, Instant>) =
        dao.unlock(unlocks.map { (code, at) -> UnlockedAchievementRow(code.value, at.toEpochMilliseconds()) })
}

private fun ReadingSession.toRow() = ReadingSessionRow(
    id = id.value,
    passage = passage.format(),
    translation = translation.value,
    planId = planId?.value,
    planDay = planDay,
    startedAt = startedAt.toEpochMilliseconds(),
    completedAt = completedAt?.toEpochMilliseconds(),
    timeZone = timeZoneId,
)

private fun ReadingSessionRow.toSession() = ReadingSession(
    id = SessionId(id),
    passage = PassageRef.parseOrThrow(passage),
    translation = TranslationId(translation),
    planId = planId?.let(::PlanId),
    planDay = planDay,
    startedAt = Instant.fromEpochMilliseconds(startedAt),
    completedAt = completedAt?.let(Instant::fromEpochMilliseconds),
    timeZoneId = timeZone,
)
