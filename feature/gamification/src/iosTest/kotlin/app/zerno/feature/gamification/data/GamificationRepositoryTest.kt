@file:OptIn(ExperimentalForeignApi::class)

package app.zerno.feature.gamification.data

import androidx.room.Room
import app.zerno.data.db.user.UserDb
import app.zerno.data.db.user.openUserDb
import app.zerno.entities.achievement.AchievementCode
import app.zerno.entities.bible.PassageRef
import app.zerno.entities.bible.TranslationId
import app.zerno.entities.bible.parseOrThrow
import app.zerno.entities.plan.PlanId
import app.zerno.entities.session.ReadingSession
import app.zerno.entities.session.SessionId
import app.zerno.feature.gamification.domain.streak.DayStatus
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class GamificationRepositoryTest {
    private val path = "${NSTemporaryDirectory().trimEnd('/')}/test-gamification.db"
    private val started = Instant.parse("2026-03-02T07:00:00Z")
    private val finished = Instant.parse("2026-03-02T07:12:00Z")
    private val monday = LocalDate(2026, 3, 2)

    @AfterTest
    fun clean() {
        listOf(path, "$path-wal", "$path-shm").forEach { NSFileManager.defaultManager.removeItemAtPath(it, null) }
    }

    private fun open() = openUserDb(Room.databaseBuilder<UserDb>(name = path))

    @Test
    fun everythingSurvivesRestart() = runBlocking {
        val session = ReadingSession(
            id = SessionId("s1"),
            passage = PassageRef.parseOrThrow("John 4:1-26"),
            translation = TranslationId("kjv"),
            planId = PlanId("gospel-of-john"),
            planDay = 4,
            startedAt = started,
            completedAt = finished,
            timeZoneId = "Europe/Moscow",
        )
        val first = open()
        GamificationRepository(first.gamificationDao()).apply {
            saveSession(session)
            saveSession(session.copy(id = SessionId("open"), completedAt = null))
            record(ActivityKind.Reflection, finished, TimeZone.of("Europe/Moscow"))
            markDay(monday, DayStatus.Done)
            saveUnlocks(mapOf(AchievementCode("first_steps") to finished))
        }
        first.close()

        val reopened = open()
        val repository = GamificationRepository(reopened.gamificationDao())
        // Only finished sessions count.
        assertEquals(listOf(session), repository.completedSessions.first())
        assertEquals(listOf(Activity(ActivityKind.Reflection, finished, TimeZone.of("Europe/Moscow"))), repository.activity.first())
        assertEquals(mapOf(monday to DayStatus.Done), repository.dayMarks.first())
        assertEquals(mapOf(AchievementCode("first_steps") to finished), repository.unlocked.first())
        reopened.close()
    }

    @Test
    fun dayMarkNeverGoesDown() = runBlocking {
        val db = open()
        val repository = GamificationRepository(db.gamificationDao())
        repository.markDay(monday, DayStatus.Done)
        repository.markDay(monday, DayStatus.Read)
        repository.markDay(monday.plusDays(1), DayStatus.Read)
        repository.markDay(monday.plusDays(1), DayStatus.Done)

        assertEquals(mapOf(monday to DayStatus.Done, monday.plusDays(1) to DayStatus.Done), repository.dayMarks.first())
        db.close()
    }

    @Test
    fun unlockKeepsItsFirstDate() = runBlocking {
        val db = open()
        val repository = GamificationRepository(db.gamificationDao())
        val code = AchievementCode("streak_3")
        repository.saveUnlocks(mapOf(code to started))
        repository.saveUnlocks(mapOf(code to finished))

        assertEquals(mapOf(code to started), repository.unlocked.first())
        db.close()
    }

    private fun LocalDate.plusDays(days: Int) = plus(DatePeriod(days = days))
}
