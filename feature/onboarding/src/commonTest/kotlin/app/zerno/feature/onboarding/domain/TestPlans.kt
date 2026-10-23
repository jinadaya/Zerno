package app.zerno.feature.onboarding.domain

import app.zerno.entities.bible.BookId
import app.zerno.entities.bible.PassageRef
import app.zerno.entities.common.LocalizedText
import app.zerno.entities.plan.PlanDay
import app.zerno.entities.plan.PlanId
import app.zerno.entities.plan.PlanType
import app.zerno.entities.plan.ReadingPlan

val Mark = BookId(480)
val John = BookId(500)
val Matthew = BookId(470)

/** A plan of [length] days of [minutes] each, starting at chapter 1 of [book]. */
fun testPlan(id: String, type: PlanType, book: BookId, minutes: Int, length: Int = 7) = ReadingPlan(
    id = PlanId(id),
    type = type,
    title = LocalizedText(mapOf("en" to id)),
    description = LocalizedText(mapOf("en" to "About $id")),
    days = (1..length).map { PlanDay(it, listOf(PassageRef.chapter(book, it)), minutes) },
)

/** Shaped like the launch catalog: one plan of each kind. */
val LaunchLike = listOf(
    testPlan("first-steps", PlanType.Introductory, Mark, minutes = 5),
    testPlan("gospel-of-john", PlanType.Sequential, John, minutes = 6),
    testPlan("prayer", PlanType.Thematic, Matthew, minutes = 3),
)
