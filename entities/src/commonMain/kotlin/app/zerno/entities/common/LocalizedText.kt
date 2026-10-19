package app.zerno.entities.common

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Content text in several languages, keyed by language code ("en", "ru").
 * Used for plan titles and descriptions that ship as data, not as string resources.
 */
@Serializable
@JvmInline
value class LocalizedText(val values: Map<String, String>) {

    /** Text for [language], falling back to English, then to any language present. */
    fun get(language: String): String =
        values[language] ?: values[FALLBACK_LANGUAGE] ?: values.values.firstOrNull().orEmpty()

    companion object {
        const val FALLBACK_LANGUAGE = "en"
    }
}
