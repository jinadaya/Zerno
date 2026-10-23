package app.zerno.feature.progress.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.intl.Locale
import app.zerno.designsystem.component.SjBigStat
import app.zerno.designsystem.component.SjLinkButton
import app.zerno.designsystem.component.SjPlanKind
import app.zerno.designsystem.component.SjPlanList
import app.zerno.designsystem.component.SjPlanRow
import app.zerno.designsystem.component.SjProgressLine
import app.zerno.designsystem.foundation.sjHairline
import app.zerno.designsystem.icon.SjIcon
import app.zerno.designsystem.icon.SjIcons
import app.zerno.designsystem.theme.SjTheme
import app.zerno.entities.bible.BookNameLanguage
import app.zerno.entities.bible.PassageRef
import app.zerno.entities.bible.Versification
import app.zerno.entities.bible.format
import app.zerno.entities.bible.mapVersification
import app.zerno.entities.plan.PlanType
import app.zerno.feature.gamification.api.AchievementsRoute
import app.zerno.feature.gamification.api.StreakState
import app.zerno.feature.plans.api.PlanOverview
import app.zerno.feature.plans.api.PlansRoute
import app.zerno.feature.progress.domain.HistoryEntry
import app.zerno.feature.progress.domain.JourneyNumbers
import app.zerno.feature.progress.domain.fraction
import app.zerno.feature.progress.domain.shortText
import app.zerno.feature.progress.resources.Res
import app.zerno.feature.progress.resources.progress_books_finished
import app.zerno.feature.progress.resources.progress_chapters
import app.zerno.feature.progress.resources.progress_chapters_line
import app.zerno.feature.progress.resources.progress_history
import app.zerno.feature.progress.resources.progress_history_empty
import app.zerno.feature.progress.resources.progress_intro
import app.zerno.feature.progress.resources.progress_plan
import app.zerno.feature.progress.resources.progress_plan_browse
import app.zerno.feature.progress.resources.progress_plan_caption
import app.zerno.feature.progress.resources.progress_plan_none
import app.zerno.feature.progress.resources.progress_saved
import app.zerno.feature.progress.resources.progress_saved_line
import app.zerno.feature.progress.resources.progress_saved_open
import app.zerno.feature.progress.resources.progress_seals
import app.zerno.feature.progress.resources.progress_seals_count
import app.zerno.feature.progress.resources.progress_seals_open
import app.zerno.feature.progress.resources.progress_sessions
import app.zerno.feature.progress.resources.progress_settings
import app.zerno.feature.progress.resources.progress_streak
import app.zerno.feature.progress.resources.progress_streak_current
import app.zerno.feature.progress.resources.progress_streak_longest
import app.zerno.feature.progress.resources.progress_title
import app.zerno.feature.reflection.api.SavedRoute
import app.zerno.feature.settings.api.SettingsRoute
import com.royaal.arch.mvi.EffectHandler
import com.royaal.navigation.LocalNavigator
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** The Journey tab. Counts only what was read; never what was missed. */
@Composable
fun JourneyScreen(viewModel: JourneyViewModel) {
    val navigator = LocalNavigator.current
    EffectHandler(viewModel) { effect ->
        when (effect) {
            JourneyEffect.OpenPlans -> navigator.selectTab(PlansRoute)
            JourneyEffect.OpenAchievements -> navigator.navigate(AchievementsRoute)
            JourneyEffect.OpenSaved -> navigator.navigate(SavedRoute)
            JourneyEffect.OpenSettings -> navigator.navigate(SettingsRoute)
        }
    }
    LaunchedEffect(Unit) { viewModel.sendEvent(JourneyEvent.Shown) }
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    stringResource(Res.string.progress_title),
                    Modifier.weight(1f).semantics { heading() },
                    style = SjTheme.type.headline.copy(color = colors.ink),
                )
                Box(
                    Modifier
                        .size(SjTheme.sizes.tapMin)
                        .clickable(role = Role.Button) { viewModel.sendEvent(JourneyEvent.SettingsClicked) },
                    contentAlignment = Alignment.Center,
                ) {
                    SjIcon(SjIcons.Settings, stringResource(Res.string.progress_settings), tint = colors.ink)
                }
            }
            BasicText(
                stringResource(Res.string.progress_intro),
                style = SjTheme.type.body.copy(color = colors.inkMuted),
            )
        }
        Totals(state.numbers)
        BookCoverageSection(state.numbers.coverage)
        CurrentPlan(state.currentPlan) { viewModel.sendEvent(JourneyEvent.CurrentPlanClicked) }
        state.streak?.let { Streak(it) }
        state.calendar?.let { calendar ->
            MonthCalendarSection(
                calendar,
                onPrevious = { viewModel.sendEvent(JourneyEvent.PreviousMonthClicked) },
                onNext = { viewModel.sendEvent(JourneyEvent.NextMonthClicked) },
            )
        }
        Seals(state.sealsUnlocked, state.sealsTotal) { viewModel.sendEvent(JourneyEvent.AchievementsClicked) }
        Saved { viewModel.sendEvent(JourneyEvent.SavedClicked) }
        History(state.history)
    }
}

@Composable
internal fun SectionTitle(text: String) {
    BasicText(text, Modifier.semantics { heading() }, style = SjTheme.type.headline.copy(color = SjTheme.colors.ink))
}

/** Sessions as the big number, then chapters toward the whole Bible and books finished. */
@Composable
private fun Totals(numbers: JourneyNumbers) {
    val colors = SjTheme.colors
    val totalChapters = numbers.coverage.sumOf { it.chapters }
    Column(verticalArrangement = Arrangement.spacedBy(SjTheme.spacing.space4)) {
        SjBigStat(numbers.sessions.toString(), pluralStringResource(Res.plurals.progress_sessions, numbers.sessions))
        if (totalChapters > 0) {
            val fraction = numbers.chaptersRead.toFloat() / totalChapters
            SjProgressLine(
                progress = fraction,
                contentDescription = stringResource(Res.string.progress_chapters, numbers.chaptersRead, totalChapters),
                startLabel = stringResource(Res.string.progress_chapters_line, numbers.chaptersRead, totalChapters),
                endLabel = percent(fraction),
            )
            BasicText(
                stringResource(Res.string.progress_books_finished, numbers.booksFinished, numbers.coverage.size),
                style = SjTheme.type.bodyStrong.copy(color = colors.ink),
            )
        }
    }
}

/** Whole percents, but never "0%" once something is read and never "100%" before everything is. */
internal fun percent(fraction: Float): String {
    val value = (fraction * PercentScale).toInt()
    val shown = when {
        fraction > 0f && value == 0 -> 1
        fraction < 1f && value == PercentScale -> PercentScale - 1
        else -> value
    }
    return "$shown%"
}

private const val PercentScale = 100

@Composable
private fun CurrentPlan(plan: PlanOverview?, onClick: () -> Unit) {
    val language = Locale.current.language
    Column(verticalArrangement = Arrangement.spacedBy(SjTheme.spacing.space3)) {
        SectionTitle(stringResource(Res.string.progress_plan))
        if (plan == null) {
            BasicText(
                stringResource(Res.string.progress_plan_none),
                style = SjTheme.type.body.copy(color = SjTheme.colors.inkMuted),
            )
            SjLinkButton(stringResource(Res.string.progress_plan_browse), onClick)
        } else {
            SjPlanList {
                SjPlanRow(
                    name = plan.plan.title.get(language),
                    caption = stringResource(
                        Res.string.progress_plan_caption,
                        plan.nextDay ?: plan.plan.length,
                        plan.plan.length,
                    ),
                    kind = plan.plan.type.kind,
                    progress = plan.fraction,
                    onClick = onClick,
                )
            }
        }
    }
}

private val PlanType.kind: SjPlanKind
    get() = when (this) {
        PlanType.Introductory -> SjPlanKind.Introductory
        PlanType.Sequential, PlanType.WholeBible -> SjPlanKind.Sequential
        PlanType.Thematic -> SjPlanKind.Thematic
    }

@Composable
private fun Streak(streak: StreakState) {
    Column(verticalArrangement = Arrangement.spacedBy(SjTheme.spacing.space3)) {
        SectionTitle(stringResource(Res.string.progress_streak))
        Row(horizontalArrangement = Arrangement.spacedBy(SjTheme.spacing.space8)) {
            SjBigStat(
                streak.current.toString(),
                pluralStringResource(Res.plurals.progress_streak_current, streak.current),
            )
            SjBigStat(
                streak.longest.toString(),
                pluralStringResource(Res.plurals.progress_streak_longest, streak.longest),
            )
        }
    }
}

@Composable
private fun Seals(unlocked: Int, total: Int, onOpen: () -> Unit) {
    if (total == 0) return
    Column(verticalArrangement = Arrangement.spacedBy(SjTheme.spacing.space3)) {
        SectionTitle(stringResource(Res.string.progress_seals))
        BasicText(
            stringResource(Res.string.progress_seals_count, unlocked, total),
            style = SjTheme.type.bodyStrong.copy(color = SjTheme.colors.ink),
        )
        SjLinkButton(stringResource(Res.string.progress_seals_open), onOpen)
    }
}

@Composable
private fun Saved(onOpen: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(SjTheme.spacing.space3)) {
        SectionTitle(stringResource(Res.string.progress_saved))
        BasicText(
            stringResource(Res.string.progress_saved_line),
            style = SjTheme.type.body.copy(color = SjTheme.colors.inkMuted),
        )
        SjLinkButton(stringResource(Res.string.progress_saved_open), onOpen)
    }
}

/** The latest readings: date, passage and plan. */
@Composable
private fun History(entries: List<HistoryEntry>) {
    val colors = SjTheme.colors
    val type = SjTheme.type
    val language = Locale.current.language
    Column {
        SectionTitle(stringResource(Res.string.progress_history))
        if (entries.isEmpty()) {
            BasicText(
                stringResource(Res.string.progress_history_empty),
                Modifier.padding(top = SjTheme.spacing.space3),
                style = type.body.copy(color = colors.inkMuted),
            )
        }
        entries.forEach { entry ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .sjHairline(colors.rule)
                    .semantics(mergeDescendants = true) {}
                    .padding(vertical = SjTheme.spacing.space3),
                horizontalArrangement = Arrangement.spacedBy(SjTheme.spacing.space3),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicText(entry.date.shortText(language), style = type.caption.copy(color = colors.inkMuted))
                Column(Modifier.weight(1f)) {
                    BasicText(entry.passage.display(language), style = type.bodyStrong.copy(color = colors.ink))
                    entry.plan?.let { plan ->
                        BasicText(plan.title.get(language), style = type.caption.copy(color = colors.inkMuted))
                    }
                }
            }
        }
    }
}

/** A passage (KJV numbering) as the UI language writes it; Russian uses the Synodal numbering. */
internal fun PassageRef.display(language: String): String = when (BookNameLanguage.fromCode(language)) {
    BookNameLanguage.English -> format(BookNameLanguage.English)

    BookNameLanguage.Russian ->
        mapVersification(Versification.English, Versification.Russian).format(BookNameLanguage.Russian)
}
