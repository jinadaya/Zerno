package app.zerno.feature.settings.ui

import androidx.lifecycle.SavedStateHandle
import app.zerno.data.prefs.AppPreferences
import app.zerno.data.prefs.ScriptureSize
import app.zerno.feature.settings.data.InstalledTranslations
import com.royaal.arch.mvi.ScreenSnapshot
import com.royaal.arch.mvi.SnapshotHandler
import com.royaal.arch.mvi.viewmodel.MviViewModel
import kotlinx.coroutines.CancellationException

/**
 * Settings write straight to [AppPreferences]; the app root, the reader and every screen collecting it apply a
 * change at once.
 */
class SettingsViewModel(
    savedStateHandle: SavedStateHandle,
    private val prefs: AppPreferences,
    private val translations: InstalledTranslations,
) : MviViewModel<SettingsState, SettingsEvent, SettingsInnerEvent, SettingsEffect, ScreenSnapshot.NoSnapshot>(
    savedStateHandle = savedStateHandle,
    initialState = SettingsState(),
    reducer = SettingsReducer,
    snapshotHandler = SnapshotHandler.noSnapshot(SettingsState()),
) {
    init {
        subscription(PREFS) { prefs.prefs.collect { emit(SettingsInnerEvent.PrefsChanged(it)) } }
        loadTranslations()
    }

    override fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.TranslationChosen -> prefs.setTranslation(event.id)

            is SettingsEvent.FontSizeStepped -> {
                val current = state.value.prefs.fontSize ?: ScriptureSize.DEFAULT
                prefs.setFontSize(ScriptureSize.step(current, event.steps))
            }

            is SettingsEvent.PageThemeChosen -> prefs.setPageTheme(event.theme)

            is SettingsEvent.AppThemeChosen -> prefs.setAppTheme(event.theme)

            is SettingsEvent.UiLanguageChosen -> prefs.setUiLanguage(event.language)

            SettingsEvent.BackClicked -> intent(CLOSE) { postEffect(SettingsEffect.Close) }
        }
    }

    // A file that cannot be listed leaves the choice out rather than the screen.
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun loadTranslations() = intent(TRANSLATIONS) {
        val list = try {
            translations.list()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }
        emit(SettingsInnerEvent.TranslationsLoaded(list))
    }

    private companion object {
        const val PREFS = "prefs"
        const val TRANSLATIONS = "translations"
        const val CLOSE = "close"
    }
}
