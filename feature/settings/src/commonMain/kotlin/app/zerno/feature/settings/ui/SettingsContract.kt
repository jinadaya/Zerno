package app.zerno.feature.settings.ui

import app.zerno.data.db.bible.InstalledTranslation
import app.zerno.data.prefs.AppPrefs
import app.zerno.data.prefs.AppTheme
import app.zerno.data.prefs.PageTheme
import app.zerno.data.prefs.UiLanguage
import app.zerno.entities.bible.TranslationId
import com.royaal.arch.mvi.InnerEvent
import com.royaal.arch.mvi.MviReducer
import com.royaal.arch.mvi.ScreenState
import com.royaal.arch.mvi.SideEffect
import com.royaal.arch.mvi.UserEvent

/** The saved settings and the translations on this device. */
data class SettingsState(
    val prefs: AppPrefs = AppPrefs(),
    val translations: List<InstalledTranslation> = emptyList(),
) : ScreenState

sealed interface SettingsEvent : UserEvent {
    data class TranslationChosen(val id: TranslationId) : SettingsEvent

    data class FontSizeStepped(val steps: Int) : SettingsEvent

    data class PageThemeChosen(val theme: PageTheme) : SettingsEvent

    data class AppThemeChosen(val theme: AppTheme) : SettingsEvent

    data class UiLanguageChosen(val language: UiLanguage) : SettingsEvent

    data object BackClicked : SettingsEvent
}

sealed interface SettingsInnerEvent : InnerEvent {
    data class PrefsChanged(val prefs: AppPrefs) : SettingsInnerEvent

    data class TranslationsLoaded(val translations: List<InstalledTranslation>) : SettingsInnerEvent
}

sealed interface SettingsEffect : SideEffect {
    data object Close : SettingsEffect
}

object SettingsReducer : MviReducer<SettingsState, SettingsInnerEvent> {
    override fun reduce(oldState: SettingsState, event: SettingsInnerEvent): SettingsState = when (event) {
        is SettingsInnerEvent.PrefsChanged -> oldState.copy(prefs = event.prefs)
        is SettingsInnerEvent.TranslationsLoaded -> oldState.copy(translations = event.translations)
    }
}
