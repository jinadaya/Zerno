package app.zerno.data.prefs

import app.zerno.entities.bible.TranslationId
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlinx.datetime.isoDayNumber

/** Light or dark chrome, or whatever the system says. */
enum class AppTheme { System, Light, Dark }

/** Colour of the reading page, separate from the app theme. */
enum class PageTheme { Light, Sepia, Dark }

/** Language of the app's own text; [System] follows the device. Scripture follows the chosen translation. */
enum class UiLanguage { System, English, Russian }

/** Scripture sizes in sp that the reader and settings offer, the `scripture` range of `SjReaderText`. */
object ScriptureSize {
    const val MIN = 16
    const val MAX = 28
    const val DEFAULT = 20
    const val STEP = 2

    /** [size] moved by [steps] steps, kept in range. */
    fun step(size: Int, steps: Int): Int = (size + steps * STEP).coerceIn(MIN, MAX)
}

private const val DAYS_IN_WEEK = 7

/** Monday to Sunday. */
val EveryDay: Set<DayOfWeek> = (1..DAYS_IN_WEEK).mapTo(LinkedHashSet()) { DayOfWeek(it) }

/** Everything the user sets once and expects to find again after a restart. `null` means never set. */
data class AppPrefs(
    val appTheme: AppTheme = AppTheme.System,
    val pageTheme: PageTheme = PageTheme.Light,
    val uiLanguage: UiLanguage = UiLanguage.System,
    val translation: TranslationId? = null,
    val fontSize: Int? = null,
    val reminderTime: LocalTime? = null,
    /** Days the reminder may come on; every day unless the user narrowed it. */
    val reminderDays: Set<DayOfWeek> = EveryDay,
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

    fun setAppTheme(theme: AppTheme) = write {
        putString(APP_THEME, theme.name)
        it.copy(appTheme = theme)
    }

    fun setPageTheme(theme: PageTheme) = write {
        putString(PAGE_THEME, theme.name)
        it.copy(pageTheme = theme)
    }

    fun setUiLanguage(language: UiLanguage) = write {
        putString(UI_LANGUAGE, language.name)
        it.copy(uiLanguage = language)
    }

    fun setTranslation(translation: TranslationId) = write {
        putString(TRANSLATION, translation.value)
        it.copy(translation = translation)
    }

    fun setFontSize(size: Int) = write {
        putInt(FONT_SIZE, size)
        it.copy(fontSize = size)
    }

    /** `null` turns the daily reminder off. */
    fun setReminderTime(time: LocalTime?) = write {
        if (time == null) remove(REMINDER_TIME) else putString(REMINDER_TIME, time.toString())
        it.copy(reminderTime = time)
    }

    /** An empty set is kept as is: the reminder then never comes, like turning it off. */
    fun setReminderDays(days: Set<DayOfWeek>) = write {
        putString(REMINDER_DAYS, days.map { it.isoDayNumber }.sorted().joinToString(","))
        it.copy(reminderDays = days)
    }

    fun setOnboardingDone(done: Boolean) = write {
        putBoolean(ONBOARDING_DONE, done)
        it.copy(onboardingDone = done)
    }

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
            reminderDays = getStringOrNull(REMINDER_DAYS)?.let(::parseDays) ?: defaults.reminderDays,
            onboardingDone = getBoolean(ONBOARDING_DONE, defaults.onboardingDone),
        )
    }

    private companion object {
        const val APP_THEME = "app.theme"
        const val PAGE_THEME = "page.theme"
        const val UI_LANGUAGE = "ui.language"
        const val TRANSLATION = "reading.translation"
        const val FONT_SIZE = "reading.font-size"
        const val REMINDER_TIME = "reminder.time"
        const val REMINDER_DAYS = "reminder.days"
        const val ONBOARDING_DONE = "onboarding.done"
    }
}

/** ISO day numbers, e.g. "1,3,5"; numbers out of range are skipped. */
private fun parseDays(value: String): Set<DayOfWeek> = value.split(",")
    .mapNotNull { it.trim().toIntOrNull() }
    .filter { it in 1..DAYS_IN_WEEK }
    .mapTo(HashSet()) { DayOfWeek(it) }

private inline fun <reified E : Enum<E>> enumOrNull(name: String?): E? = enumValues<E>().firstOrNull { it.name == name }
