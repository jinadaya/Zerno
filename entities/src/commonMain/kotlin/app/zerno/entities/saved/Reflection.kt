package app.zerno.entities.saved

import app.zerno.entities.bible.PassageRef
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.time.Instant

@Serializable
@JvmInline
value class ReflectionId(val value: String) {
    override fun toString(): String = value
}

/**
 * A private reflection note tied to a Scripture reference (A6). Never shared.
 * On sync, the later [updatedAt] wins.
 */
@Serializable
data class Reflection(
    val id: ReflectionId,
    val passage: PassageRef,
    val text: String,
    val createdAt: Instant,
    val updatedAt: Instant = createdAt,
) {
    init {
        require(updatedAt >= createdAt) { "Reflection $id updated before it was created" }
    }
}
