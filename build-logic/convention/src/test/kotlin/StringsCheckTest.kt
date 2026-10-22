import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StringsCheckTest {
    @Test
    fun readsStringsPluralsAndArrays() {
        val xml = """
            <resources>
                <string name="title">Today</string>
                <plurals name="chapters"><item quantity="one">%1${'$'}d</item></plurals>
                <string-array name="days"><item>M</item></string-array>
            </resources>
        """.trimIndent()
        assertEquals(setOf("title", "chapters", "days"), stringKeys(xml))
    }

    @Test
    fun matchingLocalesPass() {
        assertTrue(missingStringKeys(mapOf("values" to setOf("a", "b"), "values-ru" to setOf("b", "a"))).isEmpty())
    }

    @Test
    fun moduleWithoutStringsPasses() {
        assertTrue(missingStringKeys(emptyMap()).isEmpty())
    }

    @Test
    fun keyMissingOnEitherSideFails() {
        val missing = missingStringKeys(mapOf("values" to setOf("a", "b"), "values-ru" to setOf("a", "c")))
        assertEquals(listOf("values: missing \"c\"", "values-ru: missing \"b\""), missing)
    }

    @Test
    fun missingRussianFolderFailsEveryKey() {
        assertEquals(listOf("values-ru: missing \"a\""), missingStringKeys(mapOf("values" to setOf("a"))))
    }
}
