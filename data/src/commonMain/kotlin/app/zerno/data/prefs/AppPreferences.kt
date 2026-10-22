package app.zerno.data.prefs

import app.zerno.entities.bible.TranslationId
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalTime

/** Light or dark chrome, or whatever the system says. */
enum class AppTheme { System, Light, Dark }

/** Colour of the reading page, separate from the app theme. */
enum class PageTheme { Light, Sepia, Dark }

/** Language of the app's own text; [System] follows the device. Scripture follows the chosen translation. */
enum class UiLanguage { System, English, Russian }

/** Everything the user sets once and expects to find again after a restart. `null` means never set. */
data class AppPrefs(
    val appTheme: AppTheme = AppTheme.System,
    val pageTheme: PageTheme = PageTheme.Light,
    val uiLanguage: UiLanguage = UiLanguage.System,
    val translation: TranslationId? = null,
    val fontSize: Int? = null,
    val reminderTime: LocalTime? = null,
    val onboardingDone: Boolean = false,
)

/**
 * [AppPrefs] on a key-value [Settings] (SharedPreferences on Android, NSUserDefaults on iOS), read once at start and
 * kept in [prefs]. Every setter writes through and updates [prefs], so screens collecting it apply a change at
 * once. A stored value the app cannot read back (a renamed enum, a hand edit) counts as never set.
 */
class AppPreferences(private val settings: Settings) {

    private val state = MutableStateFlow(read())

    val prefs: StateFlow<AppPrefs> = state.asStateFlow()

    fun setAppTheme(theme: AppTheme) = write { putString(APP_THEME, theme.name); it.copy(appTheme = theme) }

    fun setPageTheme(theme: PageTheme) = write { putString(PAGE_THEME, theme.name); it.copy(pageTheme = theme) }

    fun setUiLanguage(language: UiLanguage) =
        write { putString(UI_LANGUAGE, language.name); it.copy(uiLanguage = language) }

    fun setTranslation(translation: TranslationId) =
        write { putString(TRANSLATION, translation.value); it.copy(translation = translation) }

    fun setFontSize(size: Int) = write { putInt(FONT_SIZE, size); it.copy(fontSize = size) }

    /** `null` turns the daily reminder off. */
    fun setReminderTime(time: LocalTime?) = write {
        if (time == null) remove(REMINDER_TIME) else putString(REMINDER_TIME, time.toString())
        it.copy(reminderTime = time)
    }

    fun setOnboardingDone(done: Boolean) = write { putBoolean(ONBOARDING_DONE, done); it.copy(onboardingDone = done) }

    private inline fun write(crossinline change: Settings.(AppPrefs) -> AppPrefs) {
        state.update { settings.change(it) }
    }

    private fun read(): AppPrefs = with(settings) {
        val defaults = AppPrefs()
        AppPrefs(
            appTheme = enumOrNull<AppTheme>(getStringOrNull(APP_THEME)) ?: defaults.appTheme,
            pageTheme = enumOrNull<PageTheme>(getStringOrNull(PAGE_THEME)) ?: defaults.pageTheme,
            uiLanguage = enumOrNull<UiLanguage>(getStringOrNull(UI_LANGUAGE)) ?: defaults.uiLanguage,
            translation = getStringOrNull(TRANSLATION)?.takeIf { it.isNotBlank() }?.let(::TranslationId),
            fontSize = getIntOrNull(FONT_SIZE),
            reminderTime = getStringOrNull(REMINDER_TIME)?.let { runCatching { LocalTime.parse(it) }.getOrNull() },
            onboardingDone = getBoolean(ONBOARDING_DONE, defaults.onboardingDone),
        )
    }

    private inline fun <reified E : Enum<E>> enumOrNull(name: String?): E? = enumValues<E>().firstOrNull { it.name == name }

    private companion object {
        const val APP_THEME = "app.theme"
        const val PAGE_THEME = "page.theme"
        const val UI_LANGUAGE = "ui.language"
        const val TRANSLATION = "reading.translation"
        const val FONT_SIZE = "reading.font-size"
        const val REMINDER_TIME = "reminder.time"
        const val ONBOARDING_DONE = "onboarding.done"
    }
}
