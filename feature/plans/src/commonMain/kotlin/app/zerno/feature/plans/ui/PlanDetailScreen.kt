package app.zerno.feature.plans.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import app.zerno.designsystem.component.SjLinkButton
import app.zerno.designsystem.component.SjOutlineButton
import app.zerno.designsystem.component.SjPrimaryButton
import app.zerno.designsystem.component.SjTag
import app.zerno.designsystem.component.forPlan
import app.zerno.designsystem.foundation.StickerSurface
import app.zerno.designsystem.foundation.sjHairline
import app.zerno.designsystem.mascot.Mascot
import app.zerno.designsystem.mascot.MascotCharacter
import app.zerno.designsystem.mascot.MascotPose
import app.zerno.designsystem.theme.SjTheme
import app.zerno.entities.plan.PlanDay
import app.zerno.feature.plans.api.PlanOverview
import app.zerno.feature.plans.api.PlanRunStatus
import app.zerno.feature.plans.domain.PlanDifficulty
import app.zerno.feature.plans.domain.difficulty
import app.zerno.feature.plans.domain.minutesPerDay
import app.zerno.feature.plans.resources.Res
import app.zerno.feature.plans.resources.plans_back
import app.zerno.feature.plans.resources.plans_current
import app.zerno.feature.plans.resources.plans_day
import app.zerno.feature.plans.resources.plans_day_done
import app.zerno.feature.plans.resources.plans_days
import app.zerno.feature.plans.resources.plans_difficulty_deep
import app.zerno.feature.plans.resources.plans_difficulty_gentle
import app.zerno.feature.plans.resources.plans_difficulty_steady
import app.zerno.feature.plans.resources.plans_failed
import app.zerno.feature.plans.resources.plans_length
import app.zerno.feature.plans.resources.plans_minutes
import app.zerno.feature.plans.resources.plans_pause
import app.zerno.feature.plans.resources.plans_read_count
import app.zerno.feature.plans.resources.plans_restart
import app.zerno.feature.plans.resources.plans_resume
import app.zerno.feature.plans.resources.plans_start
import app.zerno.feature.plans.resources.plans_switch
import app.zerno.feature.plans.resources.plans_switch_note
import app.zerno.feature.today.api.TodayRoute
import com.royaal.arch.mvi.EffectHandler
import com.royaal.navigation.LocalNavigator
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun PlanDetailScreen(viewModel: PlanDetailViewModel) {
    val navigator = LocalNavigator.current
    EffectHandler(viewModel) { effect ->
        when (effect) {
            PlanDetailEffect.OpenToday -> {
                navigator.back()
                navigator.selectTab(TodayRoute)
            }

            PlanDetailEffect.Close -> navigator.back()
        }
    }
    val state by viewModel.state.collectAsState()
    Column(
        Modifier
            .fillMaxSize()
            .background(SjTheme.colors.paper)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SjTheme.spacing.gutter, vertical = SjTheme.spacing.space6),
        verticalArrangement = Arrangement.spacedBy(SjTheme.spacing.space6),
    ) {
        SjLinkButton(stringResource(Res.string.plans_back), { viewModel.sendEvent(PlanDetailEvent.BackClicked) })
        state.plan?.let { plan ->
            About(plan)
            Actions(state, plan, viewModel::sendEvent)
            Days(plan)
        }
    }
}

/** The plan on its colour: Woolly, the title, length, minutes a day, difficulty and description. */
@Composable
private fun About(overview: PlanOverview) {
    val language = Locale.current.language
    val type = SjTheme.type
    val plan = overview.plan
    StickerSurface(SjTheme.colors.forPlan(plan.type.kind), Modifier.fillMaxWidth()) {
        Mascot(MascotCharacter.Woolly, MascotPose.Reading, size = 96.dp)
        if (overview.status == PlanRunStatus.Active) SjTag(stringResource(Res.string.plans_current), solid = true)
        BasicText(
            plan.title.get(language),
            Modifier.semantics { heading() },
            style = type.titleItalic.copy(color = SjTheme.contentColor),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(SjTheme.spacing.space2)) {
            SjTag(pluralStringResource(Res.plurals.plans_length, plan.length, plan.length))
            SjTag(stringResource(Res.string.plans_minutes, plan.minutesPerDay))
            SjTag(stringResource(plan.difficulty.label))
        }
        BasicText(plan.description.get(language), style = type.body.copy(color = SjTheme.contentColor))
    }
}

/** One clear next step: start, resume, switch to or restart the plan, or pause the current one. */
@Composable
private fun Actions(state: PlanDetailState, overview: PlanOverview, onEvent: (PlanDetailEvent) -> Unit) {
    val colors = SjTheme.colors
    val type = SjTheme.type
    BasicText(
        stringResource(Res.string.plans_read_count, overview.completedDays.size, overview.plan.length),
        style = type.bodyStrong.copy(color = colors.ink),
    )
    if (overview.status == PlanRunStatus.Active) {
        SjOutlineButton(
            stringResource(Res.string.plans_pause),
            { onEvent(PlanDetailEvent.PauseClicked) },
            Modifier.fillMaxWidth(),
            enabled = !state.busy,
        )
    } else {
        val label = when {
            overview.status == PlanRunStatus.Paused -> Res.string.plans_resume
            overview.status == PlanRunStatus.Completed -> Res.string.plans_restart
            state.otherActive -> Res.string.plans_switch
            else -> Res.string.plans_start
        }
        SjPrimaryButton(
            stringResource(label),
            { onEvent(PlanDetailEvent.StartClicked) },
            Modifier.fillMaxWidth(),
            enabled = !state.busy,
        )
        if (state.otherActive) {
            BasicText(stringResource(Res.string.plans_switch_note), style = type.caption.copy(color = colors.inkMuted))
        }
    }
    if (state.failed) {
        BasicText(
            stringResource(Res.string.plans_failed),
            style = type.body.copy(color = colors.inkMuted),
        )
    }
}

/** Every day with its passages; days done in this run carry a mark, kept when the plan is paused or switched. */
@Composable
private fun Days(overview: PlanOverview) {
    val colors = SjTheme.colors
    Column {
        BasicText(
            stringResource(Res.string.plans_days),
            Modifier.semantics { heading() }.padding(bottom = SjTheme.spacing.space2),
            style = SjTheme.type.headline.copy(color = colors.ink),
        )
        overview.plan.days.forEach { day -> DayRow(day, done = day.day in overview.completedDays) }
    }
}

@Composable
private fun DayRow(day: PlanDay, done: Boolean) {
    val colors = SjTheme.colors
    val type = SjTheme.type
    val language = Locale.current.language
    Row(
        Modifier
            .fillMaxWidth()
            .sjHairline(colors.rule)
            .semantics(mergeDescendants = true) {}
            .padding(vertical = SjTheme.spacing.space3),
        horizontalArrangement = Arrangement.spacedBy(SjTheme.spacing.space3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(stringResource(Res.string.plans_day, day.day), style = type.bodyStrong.copy(color = colors.ink))
        BasicText(
            day.passages.joinToString(", ") { it.display(language) },
            Modifier.weight(1f),
            style = type.body.copy(color = colors.ink),
        )
        if (done) {
            BasicText(
                stringResource(Res.string.plans_day_done),
                style = type.caption.copy(color = colors.inkMuted),
            )
        }
    }
}

private val PlanDifficulty.label
    get() = when (this) {
        PlanDifficulty.Gentle -> Res.string.plans_difficulty_gentle
        PlanDifficulty.Steady -> Res.string.plans_difficulty_steady
        PlanDifficulty.Deep -> Res.string.plans_difficulty_deep
    }
