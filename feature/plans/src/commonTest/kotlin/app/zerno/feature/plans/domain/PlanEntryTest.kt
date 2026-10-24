package app.zerno.feature.plans.domain

import app.zerno.entities.plan.PlanId
import kotlin.test.Test
import kotlin.test.assertEquals

/** A one-day plan file reading [passage], in the served format. */
fun planFile(id: String, passage: String = "John 1") =
    """{"id": "$id", "type": "Sequential", "title": {"en": "$id", "ru": "$id"}, """ +
        """"description": {"en": "d", "ru": "о"}, "days": [{"day": 1, "passages": ["$passage"], "minutes": 5}]}"""

class PlanEntryTest {
    private fun entries(vararg ids: String) = parsePlanEntries(ids.map { planFile(it) })

    @Test
    fun plansThisAppCannotReadAreLeftOut() {
        val files = listOf(planFile("a"), planFile("b", passage = "Hezekiah 1"), "{not json", planFile("c"))
        assertEquals(listOf("a", "c"), parsePlanEntries(files).map { it.plan.id.value })
    }

    @Test
    fun theServedOrderWins() {
        val merged = mergeCatalog(entries("c", "a"), previous = entries("a", "b", "c"), started = emptySet())
        assertEquals(listOf("c", "a"), merged.map { it.plan.id.value })
    }

    @Test
    fun aStartedPlanTheServerDroppedIsKeptAtTheEnd() {
        val merged = mergeCatalog(entries("a"), previous = entries("a", "b", "c"), started = setOf(PlanId("b")))
        assertEquals(listOf("a", "b"), merged.map { it.plan.id.value })
    }

    @Test
    fun aServedPlanReplacesTheOldCopy() {
        val served = parsePlanEntries(listOf(planFile("a", passage = "Mark 1")))
        val merged = mergeCatalog(served, previous = entries("a"), started = setOf(PlanId("a")))
        assertEquals(served, merged)
    }
}
