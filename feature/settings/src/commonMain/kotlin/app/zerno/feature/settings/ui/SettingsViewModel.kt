package app.zerno.feature.settings.ui

import androidx.lifecycle.SavedStateHandle
import app.zerno.data.prefs.AppPreferences
import app.zerno.data.prefs.ScriptureSize
import app.zerno.entities.bible.TranslationId
import app.zerno.feature.settings.data.InstalledTranslations
import app.zerno.feature.settings.data.TranslationLibrary
import app.zerno.feature.settings.domain.DefaultReminderTime
import app.zerno.feature.settings.domain.ReminderStepMinutes
import app.zerno.feature.settings.domain.offersOf
import app.zerno.feature.settings.domain.stepBy
import app.zerno.feature.settings.domain.toggled
import app.zerno.feature.settings.domain.translationAfterDeleting
import com.royaal.arch.mvi.ScreenSnapshot
import com.royaal.arch.mvi.SnapshotHandler
import com.royaal.arch.mvi.viewmodel.MviViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Settings write straight to [AppPreferences]; the app root, the reader and every screen collecting it apply a
 * change at once.
 */
class SettingsViewModel(
    savedStateHandle: SavedStateHandle,
    private val prefs: AppPreferences,
    private val translations: InstalledTranslations,
    private val library: TranslationLibrary,
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

            is SettingsEvent.ReminderEvent -> onReminderEvent(event)

            is SettingsEvent.DownloadEvent -> onDownloadEvent(event)

            SettingsEvent.BackClicked -> intent(CLOSE) { postEffect(SettingsEffect.Close) }
        }
    }

    private fun onReminderEvent(event: SettingsEvent.ReminderEvent) {
        when (event) {
            is SettingsEvent.ReminderTurnedOn -> intent(REMINDER) {
                emit(SettingsInnerEvent.ReminderDenied(!event.granted))
                if (event.granted) prefs.setReminderTime(state.value.prefs.reminderTime ?: DefaultReminderTime)
            }

            SettingsEvent.ReminderTurnedOff -> prefs.setReminderTime(null)

            is SettingsEvent.ReminderTimeStepped -> state.value.prefs.reminderTime?.let { time ->
                prefs.setReminderTime(time.stepBy(event.steps * ReminderStepMinutes))
            }

            is SettingsEvent.ReminderDayToggled ->
                prefs.setReminderDays(state.value.prefs.reminderDays.toggled(event.day))
        }
    }

    private fun onDownloadEvent(event: SettingsEvent.DownloadEvent) {
        when (event) {
            is SettingsEvent.DownloadClicked -> download(event.id)
            is SettingsEvent.DeleteClicked -> delete(event.id)
        }
    }

    // Leaving Settings cancels a download; what arrived stays on disk and the next try goes on from there.
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun download(id: String) = intent(DOWNLOAD + id) {
        val offer = state.value.offers.firstOrNull { it.translation.id == id } ?: return@intent
        val progress = MutableStateFlow(0f)
        val failed = try {
            coroutineScope {
                val reporter = launch { progress.collect { emit(SettingsInnerEvent.DownloadProgressed(id, it)) } }
                library.download(offer.translation) { progress.value = it }
                reporter.cancel()
            }
            false
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            true
        }
        emit(SettingsInnerEvent.DownloadEnded(id, failed))
        if (!failed) loadTranslations()
    }

    private fun delete(id: String) = intent(DELETE + id) {
        val current = state.value
        translationAfterDeleting(TranslationId(id), current.prefs.translation, current.translations)
            ?.let(prefs::setTranslation)
        library.delete(id)
        loadTranslations()
    }

    // A file that cannot be listed leaves the choice out rather than the screen; no catalog leaves the downloads out.
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
        val offers = try {
            val catalog = library.catalog()
            val versions = catalog.filterNot { it.bundled }.associate { it.id to library.downloadedVersion(it.id) }
            offersOf(catalog, list, versions)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
        emit(SettingsInnerEvent.OffersLoaded(offers))
    }

    private companion object {
        const val PREFS = "prefs"
        const val TRANSLATIONS = "translations"
        const val CLOSE = "close"
        const val REMINDER = "reminder"
        const val DOWNLOAD = "download."
        const val DELETE = "delete."
    }
}
