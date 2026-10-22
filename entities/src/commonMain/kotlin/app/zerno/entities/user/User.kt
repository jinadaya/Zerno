package app.zerno.entities.user

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class UserId(val value: String) {
    override fun toString(): String = value
}

/**
 * The person using the app. The app works fully as a guest; an account is offered after the
 * first session to back up progress (B2), so [email] and [displayName] are null for guests.
 */
@Serializable
data class User(val id: UserId, val isGuest: Boolean, val displayName: String? = null, val email: String? = null)
