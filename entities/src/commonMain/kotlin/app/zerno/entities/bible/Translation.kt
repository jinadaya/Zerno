package app.zerno.entities.bible

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/** Stable translation code, lower case: "rst", "nrt", "kjv", "asv". */
@Serializable
@JvmInline
value class TranslationId(val value: String) {
    init {
        require(value.isNotBlank()) { "TranslationId must not be blank" }
    }

    override fun toString(): String = value
}

/**
 * One Bible translation, bundled with the app or downloaded as a MyBible SQLite file.
 *
 * @property language language code of the text, e.g. "ru" or "en".
 * @property name full display name, e.g. "Russian Synodal Translation".
 * @property licence licence name, e.g. "Public domain".
 * @property attribution text shown on the About and licences screen.
 * @property version content version of the file; a higher number means an update is available.
 * @property bundled true when the file ships inside the app (RST, KJV).
 */
@Serializable
data class Translation(
    val id: TranslationId,
    val language: String,
    val name: String,
    val licence: String,
    val attribution: String,
    val version: Int,
    val bundled: Boolean,
)
