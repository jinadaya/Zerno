package app.zerno.feature.settings.ui

import app.zerno.data.db.bible.InstalledTranslation
import app.zerno.data.prefs.AppPrefs
import app.zerno.data.prefs.AppTheme
import app.zerno.data.prefs.PageTheme
import app.zerno.data.prefs.UiLanguage
import app.zerno.entities.bible.TranslationId
import app.zerno.feature.settings.domain.TranslationOffer
import com.royaal.arch.mvi.InnerEvent
import com.royaal.arch.mvi.MviReducer
import com.royaal.arch.mvi.ScreenState
import com.royaal.arch.mvi.SideEffect
import com.royaal.arch.mvi.UserEvent
import kotlinx.datetime.DayOfWeek

/** The saved settings, the translations on this device and the ones to download. */
data class SettingsState(
    val prefs: AppPrefs = AppPrefs(),
    val translations: List<InstalledTranslation> = emptyList(),
    /** The user refused notifications when turning the reminder on. */
    val reminderDenied: Boolean = false,
    /** Translations to download, from the backend catalog. */
    val offers: List<TranslationOffer> = emptyList(),
    /** The catalog could not be fetched (offline, or the backend is down). */
    val offersUnavailable: Boolean = false,
    /** Downloads under way: translation id to the share received, 0 to 1. */
    val downloads: Map<String, Float> = emptyMap(),
    /** The translation whose last download failed, to say so next to it. */
    val failedDownload: String? = null,
) : ScreenState

sealed interface SettingsEvent : UserEvent {
    data class TranslationChosen(val id: TranslationId) : SettingsEvent

    data class FontSizeStepped(val steps: Int) : SettingsEvent

    data class PageThemeChosen(val theme: PageTheme) : SettingsEvent

    data class AppThemeChosen(val theme: AppTheme) : SettingsEvent

    data class UiLanguageChosen(val language: UiLanguage) : SettingsEvent

    /** The reminder's switch, time and days. */
    sealed interface ReminderEvent : SettingsEvent

    /** The reminder switched on, after the platform's permission prompt answered [granted]. */
    data class ReminderTurnedOn(val granted: Boolean) : ReminderEvent

    data object ReminderTurnedOff : ReminderEvent

    data class ReminderTimeStepped(val steps: Int) : ReminderEvent

    data class ReminderDayToggled(val day: DayOfWeek) : ReminderEvent

    /** Downloading and removing translations from the catalog. */
    sealed interface DownloadEvent : SettingsEvent

    data class DownloadClicked(val id: String) : DownloadEvent

    data class DeleteClicked(val id: String) : DownloadEvent

    data object BackClicked : SettingsEvent
}

sealed interface SettingsInnerEvent : InnerEvent {
    data class PrefsChanged(val prefs: AppPrefs) : SettingsInnerEvent

    data class TranslationsLoaded(val translations: List<InstalledTranslation>) : SettingsInnerEvent

    data class ReminderDenied(val denied: Boolean) : SettingsInnerEvent

    /** [offers] is null when the catalog could not be fetched. */
    data class OffersLoaded(val offers: List<TranslationOffer>?) : SettingsInnerEvent

    data class DownloadProgressed(val id: String, val share: Float) : SettingsInnerEvent

    data class DownloadEnded(val id: String, val failed: Boolean) : SettingsInnerEvent
}

sealed interface SettingsEffect : SideEffect {
    data object Close : SettingsEffect
}

object SettingsReducer : MviReducer<SettingsState, SettingsInnerEvent> {
    override fun reduce(oldState: SettingsState, event: SettingsInnerEvent): SettingsState = when (event) {
        is SettingsInnerEvent.PrefsChanged -> oldState.copy(prefs = event.prefs)

        is SettingsInnerEvent.TranslationsLoaded -> oldState.copy(translations = event.translations)

        is SettingsInnerEvent.ReminderDenied -> oldState.copy(reminderDenied = event.denied)

        is SettingsInnerEvent.OffersLoaded ->
            oldState.copy(offers = event.offers ?: oldState.offers, offersUnavailable = event.offers == null)

        is SettingsInnerEvent.DownloadProgressed -> oldState.copy(
            downloads = oldState.downloads + (event.id to event.share),
            failedDownload = oldState.failedDownload.takeUnless { it == event.id },
        )

        is SettingsInnerEvent.DownloadEnded -> oldState.copy(
            downloads = oldState.downloads - event.id,
            failedDownload = if (event.failed) event.id else oldState.failedDownload.takeUnless { it == event.id },
        )
    }
}
