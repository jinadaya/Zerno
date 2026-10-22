package app.zerno.feature.settings.domain

import app.zerno.data.prefs.UiLanguage
import app.zerno.entities.bible.TranslationId

/** Bundled translations the first run chooses between. */
internal val Rst = TranslationId("rst")
internal val Kjv = TranslationId("kjv")

/** RST for a Russian device, KJV for any other. [deviceLanguage] is a tag or code such as `ru`, `ru-RU` or `en`. */
internal fun firstRunTranslation(deviceLanguage: String): TranslationId {
    val language = deviceLanguage.substringBefore('-').substringBefore('_')
    return if (language.equals("ru", ignoreCase = true)) Rst else Kjv
}

/** The tag the UI switches to, or null when it follows the device. */
internal val UiLanguage.tag: String?
    get() = when (this) {
        UiLanguage.System -> null
        UiLanguage.English -> "en"
        UiLanguage.Russian -> "ru"
    }
