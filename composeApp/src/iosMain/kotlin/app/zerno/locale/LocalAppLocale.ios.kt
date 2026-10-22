package app.zerno.locale

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import platform.Foundation.NSUserDefaults

actual object LocalAppLocale {
    /** Compose resources read the languages from this user default, so an override is written there. */
    private const val APPLE_LANGUAGES = "AppleLanguages"

    private val Current = staticCompositionLocalOf<String?> { null }

    @Composable
    actual infix fun provides(tag: String?): ProvidedValue<*> {
        val defaults = NSUserDefaults.standardUserDefaults
        when (tag) {
            null -> defaults.removeObjectForKey(APPLE_LANGUAGES)
            else -> defaults.setObject(listOf(tag), APPLE_LANGUAGES)
        }
        return Current provides tag
    }
}
