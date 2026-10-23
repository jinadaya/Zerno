package app.zerno.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.sp
import app.zerno.data.prefs.AppTheme
import app.zerno.data.prefs.PageTheme
import app.zerno.data.prefs.ScriptureSize
import app.zerno.data.prefs.UiLanguage
import app.zerno.designsystem.component.SjChip
import app.zerno.designsystem.component.SjChipGroup
import app.zerno.designsystem.component.SjLinkButton
import app.zerno.designsystem.component.SjOutlineButton
import app.zerno.designsystem.component.SjSwitchRow
import app.zerno.designsystem.foundation.SjEdge
import app.zerno.designsystem.foundation.sjHairline
import app.zerno.designsystem.theme.SjTheme
import app.zerno.feature.settings.domain.attribution
import app.zerno.feature.settings.reminder.rememberNotificationPermission
import app.zerno.feature.settings.resources.Res
import app.zerno.feature.settings.resources.settings_about
import app.zerno.feature.settings.resources.settings_app
import app.zerno.feature.settings.resources.settings_app_theme
import app.zerno.feature.settings.resources.settings_back
import app.zerno.feature.settings.resources.settings_day_1
import app.zerno.feature.settings.resources.settings_day_2
import app.zerno.feature.settings.resources.settings_day_3
import app.zerno.feature.settings.resources.settings_day_4
import app.zerno.feature.settings.resources.settings_day_5
import app.zerno.feature.settings.resources.settings_day_6
import app.zerno.feature.settings.resources.settings_day_7
import app.zerno.feature.settings.resources.settings_fonts
import app.zerno.feature.settings.resources.settings_language
import app.zerno.feature.settings.resources.settings_language_english
import app.zerno.feature.settings.resources.settings_language_russian
import app.zerno.feature.settings.resources.settings_language_system
import app.zerno.feature.settings.resources.settings_larger
import app.zerno.feature.settings.resources.settings_page_dark
import app.zerno.feature.settings.resources.settings_page_light
import app.zerno.feature.settings.resources.settings_page_sepia
import app.zerno.feature.settings.resources.settings_page_theme
import app.zerno.feature.settings.resources.settings_reading
import app.zerno.feature.settings.resources.settings_reminder
import app.zerno.feature.settings.resources.settings_reminder_daily
import app.zerno.feature.settings.resources.settings_reminder_days
import app.zerno.feature.settings.resources.settings_reminder_denied
import app.zerno.feature.settings.resources.settings_reminder_earlier
import app.zerno.feature.settings.resources.settings_reminder_later
import app.zerno.feature.settings.resources.settings_reminder_note
import app.zerno.feature.settings.resources.settings_reminder_time
import app.zerno.feature.settings.resources.settings_sample
import app.zerno.feature.settings.resources.settings_smaller
import app.zerno.feature.settings.resources.settings_text_size
import app.zerno.feature.settings.resources.settings_theme_dark
import app.zerno.feature.settings.resources.settings_theme_light
import app.zerno.feature.settings.resources.settings_theme_system
import app.zerno.feature.settings.resources.settings_title
import app.zerno.feature.settings.resources.settings_translation
import com.royaal.arch.mvi.EffectHandler
import com.royaal.navigation.LocalNavigator
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import org.jetbrains.compose.resources.stringResource

/** Reading, app and about. Every choice applies at once; there is no save button. */
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val navigator = LocalNavigator.current
    EffectHandler(viewModel) { effect ->
        when (effect) {
            SettingsEffect.Close -> navigator.back()
        }
    }
    val state by viewModel.state.collectAsState()
    val colors = SjTheme.colors
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.paper)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SjTheme.spacing.gutter, vertical = SjTheme.spacing.space6),
        verticalArrangement = Arrangement.spacedBy(SjTheme.spacing.space8),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(SjTheme.spacing.space2)) {
            SjLinkButton(stringResource(Res.string.settings_back), { viewModel.sendEvent(SettingsEvent.BackClicked) })
            BasicText(
                stringResource(Res.string.settings_title),
                Modifier.semantics { heading() },
                style = SjTheme.type.headline.copy(color = colors.ink),
            )
        }
        Reading(state, viewModel::sendEvent)
        Reminders(state, viewModel::sendEvent)
        AppSection(state, viewModel::sendEvent)
        About(state)
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().sjHairline(SjTheme.colors.rule, SjEdge.Top),
        verticalArrangement = Arrangement.spacedBy(SjTheme.spacing.space4),
    ) {
        BasicText(
            title,
            Modifier.semantics { heading() }.padding(top = SjTheme.spacing.space4),
            style = SjTheme.type.titleItalic.copy(color = SjTheme.colors.ink),
        )
        content()
    }
}

@Composable
private fun Reading(state: SettingsState, onEvent: (SettingsEvent) -> Unit) {
    Section(stringResource(Res.string.settings_reading)) {
        if (state.translations.isNotEmpty()) {
            val chosen = state.translations.indexOfFirst { it.id == state.prefs.translation }
            SjChipGroup(
                options = state.translations.map { it.name },
                selected = chosen.takeIf { it >= 0 },
                onSelect = { onEvent(SettingsEvent.TranslationChosen(state.translations[it].id)) },
                label = stringResource(Res.string.settings_translation),
            )
        }
        TextSize(state.prefs.fontSize ?: ScriptureSize.DEFAULT, onEvent)
        val pages = PageTheme.entries
        SjChipGroup(
            options = pages.map { stringResource(it.label) },
            selected = pages.indexOf(state.prefs.pageTheme),
            onSelect = { onEvent(SettingsEvent.PageThemeChosen(pages[it])) },
            label = stringResource(Res.string.settings_page_theme),
        )
    }
}

/** Smaller and larger, with a line of Scripture at the chosen size. */
@Composable
private fun TextSize(size: Int, onEvent: (SettingsEvent) -> Unit) {
    val colors = SjTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(SjTheme.spacing.space2)) {
        BasicText(stringResource(Res.string.settings_text_size), style = SjTheme.type.tag.copy(color = colors.inkMuted))
        Row(
            horizontalArrangement = Arrangement.spacedBy(SjTheme.spacing.space3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SjOutlineButton(
                stringResource(Res.string.settings_smaller),
                { onEvent(SettingsEvent.FontSizeStepped(-1)) },
                enabled = size > ScriptureSize.MIN,
            )
            BasicText(size.toString(), style = SjTheme.type.bodyStrong.copy(color = colors.ink))
            SjOutlineButton(
                stringResource(Res.string.settings_larger),
                { onEvent(SettingsEvent.FontSizeStepped(1)) },
                enabled = size < ScriptureSize.MAX,
            )
        }
        BasicText(
            stringResource(Res.string.settings_sample),
            style = SjTheme.type.scripture.copy(color = colors.ink, fontSize = size.sp, lineHeight = (size * 1.5f).sp),
        )
    }
}

/** On or off, the time in quarter-hour steps, and the days it may come on. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Reminders(state: SettingsState, onEvent: (SettingsEvent) -> Unit) {
    val colors = SjTheme.colors
    val type = SjTheme.type
    val askPermission = rememberNotificationPermission { granted -> onEvent(SettingsEvent.ReminderTurnedOn(granted)) }
    val time = state.prefs.reminderTime
    Section(stringResource(Res.string.settings_reminder)) {
        SjSwitchRow(
            label = stringResource(Res.string.settings_reminder_daily),
            checked = time != null,
            onCheckedChange = { on -> if (on) askPermission() else onEvent(SettingsEvent.ReminderTurnedOff) },
        )
        if (state.reminderDenied) {
            BasicText(
                stringResource(Res.string.settings_reminder_denied),
                style = type.body.copy(color = colors.inkMuted),
            )
        }
        if (time != null) {
            Column(verticalArrangement = Arrangement.spacedBy(SjTheme.spacing.space2)) {
                BasicText(
                    stringResource(Res.string.settings_reminder_time),
                    style = type.tag.copy(color = colors.inkMuted),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(SjTheme.spacing.space3),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SjOutlineButton(
                        stringResource(Res.string.settings_reminder_earlier),
                        { onEvent(SettingsEvent.ReminderTimeStepped(-1)) },
                    )
                    BasicText(time.clockText(), style = type.bodyStrong.copy(color = colors.ink))
                    SjOutlineButton(
                        stringResource(Res.string.settings_reminder_later),
                        { onEvent(SettingsEvent.ReminderTimeStepped(1)) },
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(SjTheme.spacing.space2)) {
                BasicText(
                    stringResource(Res.string.settings_reminder_days),
                    style = type.tag.copy(color = colors.inkMuted),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(SjTheme.spacing.space2)) {
                    WeekDays.forEachIndexed { index, day ->
                        SjChip(
                            text = stringResource(DayLabels[index]),
                            selected = day in state.prefs.reminderDays,
                            onClick = { onEvent(SettingsEvent.ReminderDayToggled(day)) },
                        )
                    }
                }
            }
            BasicText(
                stringResource(Res.string.settings_reminder_note),
                style = type.caption.copy(color = colors.inkMuted),
            )
        }
    }
}

private fun LocalTime.clockText() = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

private val WeekDays = (1..7).map { DayOfWeek(it) }

private val DayLabels = listOf(
    Res.string.settings_day_1,
    Res.string.settings_day_2,
    Res.string.settings_day_3,
    Res.string.settings_day_4,
    Res.string.settings_day_5,
    Res.string.settings_day_6,
    Res.string.settings_day_7,
)

@Composable
private fun AppSection(state: SettingsState, onEvent: (SettingsEvent) -> Unit) {
    Section(stringResource(Res.string.settings_app)) {
        val themes = AppTheme.entries
        SjChipGroup(
            options = themes.map { stringResource(it.label) },
            selected = themes.indexOf(state.prefs.appTheme),
            onSelect = { onEvent(SettingsEvent.AppThemeChosen(themes[it])) },
            label = stringResource(Res.string.settings_app_theme),
        )
        val languages = UiLanguage.entries
        SjChipGroup(
            options = languages.map { stringResource(it.label) },
            selected = languages.indexOf(state.prefs.uiLanguage),
            onSelect = { onEvent(SettingsEvent.UiLanguageChosen(languages[it])) },
            label = stringResource(Res.string.settings_language),
        )
    }
}

/** Where the texts and fonts come from. */
@Composable
private fun About(state: SettingsState) {
    val colors = SjTheme.colors
    val type = SjTheme.type
    Section(stringResource(Res.string.settings_about)) {
        state.translations.forEach { translation ->
            Column(
                Modifier.semantics(mergeDescendants = true) {},
                verticalArrangement = Arrangement.spacedBy(SjTheme.spacing.space1),
            ) {
                BasicText(translation.name, style = type.bodyStrong.copy(color = colors.ink))
                translation.id.attribution?.let { line ->
                    BasicText(stringResource(line), style = type.body.copy(color = colors.inkMuted))
                }
            }
        }
        BasicText(stringResource(Res.string.settings_fonts), style = type.body.copy(color = colors.inkMuted))
    }
}

private val PageTheme.label
    get() = when (this) {
        PageTheme.Light -> Res.string.settings_page_light
        PageTheme.Sepia -> Res.string.settings_page_sepia
        PageTheme.Dark -> Res.string.settings_page_dark
    }

private val AppTheme.label
    get() = when (this) {
        AppTheme.System -> Res.string.settings_theme_system
        AppTheme.Light -> Res.string.settings_theme_light
        AppTheme.Dark -> Res.string.settings_theme_dark
    }

private val UiLanguage.label
    get() = when (this) {
        UiLanguage.System -> Res.string.settings_language_system
        UiLanguage.English -> Res.string.settings_language_english
        UiLanguage.Russian -> Res.string.settings_language_russian
    }
