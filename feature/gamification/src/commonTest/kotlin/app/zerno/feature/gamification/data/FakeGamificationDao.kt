package app.zerno.feature.gamification.data

import app.zerno.data.db.user.ActivityEventRow
import app.zerno.data.db.user.DayMarkRow
import app.zerno.data.db.user.GamificationDao
import app.zerno.data.db.user.ReadingSessionRow
import app.zerno.data.db.user.UnlockedAchievementRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory [GamificationDao] with the same conflict rules as the Room one. */
class FakeGamificationDao : GamificationDao {
    private val sessions = MutableStateFlow<Map<String, ReadingSessionRow>>(emptyMap())
    private val events = MutableStateFlow<List<ActivityEventRow>>(emptyList())
    private val marks = MutableStateFlow<Map<String, String>>(emptyMap())
    private val unlocks = MutableStateFlow<Map<String, Long>>(emptyMap())

    override suspend fun putSession(session: ReadingSessionRow) {
        sessions.value += session.id to session
    }

    override fun completedSessions(): Flow<List<ReadingSessionRow>> =
        sessions.map { rows -> rows.values.filter { it.completedAt != null }.sortedBy { it.completedAt } }

    override suspend fun addEvent(event: ActivityEventRow) {
        events.value += event.copy(id = events.value.size + 1L)
    }

    override fun events(): Flow<List<ActivityEventRow>> = events.map { rows -> rows.sortedBy { it.at } }

    override suspend fun putDayMark(mark: DayMarkRow) {
        marks.value += mark.date to mark.status
    }

    override suspend fun dayMark(date: String): String? = marks.value[date]

    override fun dayMarks(): Flow<List<DayMarkRow>> =
        marks.map { rows -> rows.entries.sortedBy { it.key }.map { DayMarkRow(it.key, it.value) } }

    override suspend fun unlock(rows: List<UnlockedAchievementRow>) {
        unlocks.value += rows.filter { it.code !in unlocks.value }.associate { it.code to it.unlockedAt }
    }

    override fun unlocked(): Flow<List<UnlockedAchievementRow>> =
        unlocks.map { rows -> rows.map { UnlockedAchievementRow(it.key, it.value) } }
}
