package app.zerno.data.db.user

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * A reading session (`ReadingSession` in `:entities`). Instants are epoch milliseconds; [passage] is the English
 * reference text ("John 4:1-26"), which `PassageRef.parse` reads back.
 */
@Entity(tableName = "reading_session")
data class ReadingSessionRow(
    @PrimaryKey val id: String,
    val passage: String,
    val translation: String,
    @ColumnInfo(name = "plan_id") val planId: String?,
    @ColumnInfo(name = "plan_day") val planDay: Int?,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "completed_at") val completedAt: Long?,
    @ColumnInfo(name = "time_zone") val timeZone: String,
)

/**
 * A reflection, saved verse or answered question, kept as an event with the zone it happened in so the streak and
 * achievement counts can be rebuilt. [kind] is a name owned by `:feature:gamification`.
 */
@Entity(tableName = "activity_event")
data class ActivityEventRow(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: String,
    val at: Long,
    @ColumnInfo(name = "time_zone") val timeZone: String,
)

/** The local date status the streak showed when the day was read, so later travel never changes it. */
@Entity(tableName = "day_mark")
data class DayMarkRow(
    /** ISO local date, e.g. "2026-03-02". */
    @PrimaryKey val date: String,
    /** `DayStatus` name: "Read" or "Done". */
    val status: String,
)

@Entity(tableName = "unlocked_achievement")
data class UnlockedAchievementRow(
    @PrimaryKey val code: String,
    @ColumnInfo(name = "unlocked_at") val unlockedAt: Long,
)

@Dao
interface GamificationDao {
    @Upsert
    suspend fun putSession(session: ReadingSessionRow)

    @Query("SELECT * FROM reading_session WHERE completed_at IS NOT NULL ORDER BY completed_at")
    fun completedSessions(): Flow<List<ReadingSessionRow>>

    @Insert
    suspend fun addEvent(event: ActivityEventRow)

    @Query("SELECT * FROM activity_event ORDER BY at")
    fun events(): Flow<List<ActivityEventRow>>

    @Upsert
    suspend fun putDayMark(mark: DayMarkRow)

    @Query("SELECT status FROM day_mark WHERE date = :date")
    suspend fun dayMark(date: String): String?

    @Query("SELECT * FROM day_mark ORDER BY date")
    fun dayMarks(): Flow<List<DayMarkRow>>

    /** Keeps the first date: an unlock is permanent and never moves. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun unlock(rows: List<UnlockedAchievementRow>)

    @Query("SELECT * FROM unlocked_achievement")
    fun unlocked(): Flow<List<UnlockedAchievementRow>>
}
