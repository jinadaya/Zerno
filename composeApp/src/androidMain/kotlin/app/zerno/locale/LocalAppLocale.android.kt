package app.zerno.locale

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Locale

actual object LocalAppLocale {
    /** The device locale, kept from before the first override. */
    private var device: Locale? = null

    @Composable
    actual infix fun provides(tag: String?): ProvidedValue<*> {
        val deviceLocale = device ?: Locale.getDefault().also { device = it }
        val locale = tag?.let(Locale::forLanguageTag) ?: deviceLocale
        Locale.setDefault(locale)
        val configuration = Configuration(LocalConfiguration.current).apply { setLocale(locale) }
        return LocalConfiguration provides configuration
    }
}
