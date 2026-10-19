package app.zerno.feature.gamification.domain.streak

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** A reading session the user finished, with the zone the device was in at that moment. */
data class ReadingCompleted(val at: Instant, val zone: TimeZone)

/** A reflection the user gave, with the zone the device was in at that moment. */
data class ReflectionGiven(val at: Instant, val zone: TimeZone)

internal val ReadingCompleted.localDate: LocalDate get() = at.toLocalDateTime(zone).date

internal val ReflectionGiven.localDate: LocalDate get() = at.toLocalDateTime(zone).date
