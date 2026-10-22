package app.zerno.feature.settings.api

import kotlinx.coroutines.flow.Flow

/**
 * The language choices the app makes before any screen: the UI language and, on first run, the translation.
 * The app root applies [uiLanguage]; settings change it.
 */
interface AppLanguage {
    /** The language the UI is set to, as a tag such as `ru`, or null to follow the device. */
    val uiLanguage: Flow<String?>

    /** [uiLanguage] now, for the first frame. */
    val currentUiLanguage: String?

    /**
     * On the first run, picks the translation that suits [deviceLanguage]: RST for Russian, KJV otherwise. Does
     * nothing once a translation is chosen.
     */
    fun chooseFirstRunTranslation(deviceLanguage: String)
}
