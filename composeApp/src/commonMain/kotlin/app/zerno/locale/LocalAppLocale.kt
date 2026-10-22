package app.zerno.locale

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue

/**
 * The locale Compose resources read their strings in. Providing a language tag switches the app's own text to it;
 * providing null goes back to the device language. Wrap the content in `key(tag)` so every string is read again.
 */
expect object LocalAppLocale {
    @Composable
    infix fun provides(tag: String?): ProvidedValue<*>
}
