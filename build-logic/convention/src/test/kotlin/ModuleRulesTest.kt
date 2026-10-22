import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ModuleRulesTest {
    @Test
    fun featureMayUseItsOwnApiOtherApisAndBaseModules() {
        val deps =
            listOf(
                ":feature:today:api",
                ":feature:reader:api",
                ":arch",
                ":designsystem",
                ":entities",
                ":data",
                ":navigation",
            )
        assertTrue(moduleRuleViolations(":feature:today", deps).isEmpty())
    }

    @Test
    fun featureDependingOnAnotherFeatureImplementationFails() {
        val violations = moduleRuleViolations(":feature:today", listOf(":feature:today:api", ":feature:reader"))
        assertEquals(listOf(":feature:today -> :feature:reader: depend on :feature:reader:api instead"), violations)
    }

    @Test
    fun apiMayUseEntitiesAndOtherApis() {
        assertTrue(moduleRuleViolations(":feature:plans:api", listOf(":entities", ":feature:reader:api")).isEmpty())
    }

    @Test
    fun apiDependingOnAnImplementationModuleFails() {
        val violations = moduleRuleViolations(":feature:plans:api", listOf(":entities", ":data", ":feature:reader"))
        assertEquals(2, violations.size)
        assertTrue(violations.all { it.startsWith(":feature:plans:api -> ") })
    }
}
