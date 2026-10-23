package app.zerno.feature.settings.data

import app.zerno.data.prefs.AppPreferences
import app.zerno.feature.settings.api.AppLanguage
import app.zerno.feature.settings.domain.firstRunTranslation
import app.zerno.feature.settings.domain.tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** [AppLanguage] on [AppPreferences]: the UI language and translation the user set, or the first-run defaults. */
internal class PrefsAppLanguage(private val prefs: AppPreferences) : AppLanguage {
    override val uiLanguage: Flow<String?> = prefs.prefs.map { it.uiLanguage.tag }.distinctUntilChanged()

    override val currentUiLanguage: String? get() = prefs.prefs.value.uiLanguage.tag

    override fun chooseFirstRunTranslation(deviceLanguage: String) {
        if (prefs.prefs.value.translation == null) prefs.setTranslation(firstRunTranslation(deviceLanguage))
    }
}
