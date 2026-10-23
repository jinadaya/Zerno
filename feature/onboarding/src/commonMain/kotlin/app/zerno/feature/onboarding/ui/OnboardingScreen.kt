package app.zerno.feature.onboarding.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import app.zerno.designsystem.component.SjChipGroup
import app.zerno.designsystem.component.SjLinkButton
import app.zerno.designsystem.component.SjPlanKind
import app.zerno.designsystem.component.SjPrimaryButton
import app.zerno.designsystem.component.SjTag
import app.zerno.designsystem.component.forPlan
import app.zerno.designsystem.foundation.StickerSurface
import app.zerno.designsystem.mascot.Mascot
import app.zerno.designsystem.mascot.MascotCharacter
import app.zerno.designsystem.mascot.MascotPose
import app.zerno.designsystem.theme.SjTheme
import app.zerno.entities.plan.PlanType
import app.zerno.entities.plan.ReadingPlan
import app.zerno.feature.onboarding.domain.DailyMinutes
import app.zerno.feature.onboarding.domain.OnboardingStep
import app.zerno.feature.onboarding.domain.PlanPreference
import app.zerno.feature.onboarding.domain.ReadingGoal
import app.zerno.feature.onboarding.domain.ReminderTimes
import app.zerno.feature.onboarding.domain.averageMinutes
import app.zerno.feature.onboarding.resources.Res
import app.zerno.feature.onboarding.resources.onboarding_back
import app.zerno.feature.onboarding.resources.onboarding_continue
import app.zerno.feature.onboarding.resources.onboarding_failed
import app.zerno.feature.onboarding.resources.onboarding_goal_consistent
import app.zerno.feature.onboarding.resources.onboarding_goal_start
import app.zerno.feature.onboarding.resources.onboarding_goal_theme
import app.zerno.feature.onboarding.resources.onboarding_goal_title
import app.zerno.feature.onboarding.resources.onboarding_goal_whole
import app.zerno.feature.onboarding.resources.onboarding_preference_guided
import app.zerno.feature.onboarding.resources.onboarding_preference_sequential
import app.zerno.feature.onboarding.resources.onboarding_preference_thematic
import app.zerno.feature.onboarding.resources.onboarding_preference_title
import app.zerno.feature.onboarding.resources.onboarding_recommendation_caption
import app.zerno.feature.onboarding.resources.onboarding_recommendation_days
import app.zerno.feature.onboarding.resources.onboarding_recommendation_minutes
import app.zerno.feature.onboarding.resources.onboarding_recommendation_start
import app.zerno.feature.onboarding.resources.onboarding_recommendation_title
import app.zerno.feature.onboarding.resources.onboarding_reminder_body
import app.zerno.feature.onboarding.resources.onboarding_reminder_skip
import app.zerno.feature.onboarding.resources.onboarding_reminder_start
import app.zerno.feature.onboarding.resources.onboarding_reminder_title
import app.zerno.feature.onboarding.resources.onboarding_step
import app.zerno.feature.onboarding.resources.onboarding_time_minutes
import app.zerno.feature.onboarding.resources.onboarding_time_title
import app.zerno.feature.onboarding.resources.onboarding_time_twenty_plus
import app.zerno.feature.onboarding.resources.onboarding_welcome_body
import app.zerno.feature.onboarding.resources.onboarding_welcome_start
import app.zerno.feature.onboarding.resources.onboarding_welcome_title
import app.zerno.feature.reader.api.ReaderRoute
import com.royaal.arch.mvi.EffectHandler
import com.royaal.navigation.LocalNavigator
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel) {
    val navigator = LocalNavigator.current
    EffectHandler(viewModel) { effect ->
        when (effect) {
            // Onboarding sits on top of Today; the reader is the Bible tab.
            OnboardingEffect.OpenReader -> {
                navigator.back()
                navigator.navigate(ReaderRoute)
            }
        }
    }
    val state by viewModel.state.collectAsState()
    OnboardingContent(state, viewModel::sendEvent)
}

@Composable
private fun OnboardingContent(state: OnboardingState, onEvent: (OnboardingEvent) -> Unit) {
    val colors = SjTheme.colors
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.paper)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SjTheme.spacing.gutter, vertical = SjTheme.spacing.space6),
        verticalArrangement = Arrangement.spacedBy(SjTheme.spacing.space6),
    ) {
        if (state.step != OnboardingStep.Welcome) {
            SjLinkButton(stringResource(Res.string.onboarding_back), { onEvent(OnboardingEvent.BackClicked) })
            SjTag(stringResource(Res.string.onboarding_step, state.step.ordinal, QuestionSteps))
        }
        when (state.step) {
            OnboardingStep.Welcome -> Welcome(onEvent)
            OnboardingStep.Goal -> Goal(state, onEvent)
            OnboardingStep.Time -> Time(state, onEvent)
            OnboardingStep.Preference -> Preference(state, onEvent)
            OnboardingStep.Recommendation -> Recommendation(state, onEvent)
            OnboardingStep.Reminder -> Reminder(state, onEvent)
        }
    }
}

/** Steps after the welcome, counted in the "Step n of m" tag. */
private val QuestionSteps = OnboardingStep.entries.size - 1

@Composable
private fun Welcome(onEvent: (OnboardingEvent) -> Unit) {
    StickerSurface(SjTheme.colors.sage, Modifier.fillMaxWidth()) {
        Mascot(
            MascotCharacter.Woolly,
            MascotPose.Welcome,
            Modifier.align(Alignment.CenterHorizontally),
            size = 160.dp,
        )
    }
    Title(stringResource(Res.string.onboarding_welcome_title))
    Body(stringResource(Res.string.onboarding_welcome_body))
    SjPrimaryButton(
        stringResource(Res.string.onboarding_welcome_start),
        { onEvent(OnboardingEvent.ContinueClicked) },
        Modifier.fillMaxWidth(),
    )
}

@Composable
private fun Goal(state: OnboardingState, onEvent: (OnboardingEvent) -> Unit) {
    val title = stringResource(Res.string.onboarding_goal_title)
    val labels = ReadingGoal.entries.map { stringResource(it.label) }
    Title(title)
    SjChipGroup(
        labels,
        state.answers.goal?.ordinal,
        { onEvent(OnboardingEvent.GoalSelected(ReadingGoal.entries[it])) },
        title,
    )
    ContinueButton(state, onEvent)
}

@Composable
private fun Time(state: OnboardingState, onEvent: (OnboardingEvent) -> Unit) {
    val title = stringResource(Res.string.onboarding_time_title)
    val labels = DailyMinutes.entries.map {
        if (it == DailyMinutes.TwentyPlus) {
            stringResource(Res.string.onboarding_time_twenty_plus)
        } else {
            stringResource(Res.string.onboarding_time_minutes, it.minutes)
        }
    }
    Title(title)
    SjChipGroup(
        labels,
        state.answers.minutes?.ordinal,
        { onEvent(OnboardingEvent.MinutesSelected(DailyMinutes.entries[it])) },
        title,
    )
    ContinueButton(state, onEvent)
}

@Composable
private fun Preference(state: OnboardingState, onEvent: (OnboardingEvent) -> Unit) {
    val title = stringResource(Res.string.onboarding_preference_title)
    val labels = PlanPreference.entries.map { stringResource(it.label) }
    Title(title)
    SjChipGroup(
        labels,
        state.answers.preference?.ordinal,
        { onEvent(OnboardingEvent.PreferenceSelected(PlanPreference.entries[it])) },
        title,
    )
    ContinueButton(state, onEvent)
}

@Composable
private fun Recommendation(state: OnboardingState, onEvent: (OnboardingEvent) -> Unit) {
    Title(stringResource(Res.string.onboarding_recommendation_title))
    state.recommended?.let { PlanCard(it) }
    SjPrimaryButton(
        stringResource(Res.string.onboarding_recommendation_start),
        { onEvent(OnboardingEvent.ContinueClicked) },
        Modifier.fillMaxWidth(),
        enabled = state.canContinue,
    )
}

@Composable
private fun Reminder(state: OnboardingState, onEvent: (OnboardingEvent) -> Unit) {
    val title = stringResource(Res.string.onboarding_reminder_title)
    val selected = ReminderTimes.indexOf(state.answers.reminder).takeIf { it >= 0 }
    Title(title)
    Body(stringResource(Res.string.onboarding_reminder_body))
    SjChipGroup(
        ReminderTimes.map { it.toString() },
        selected,
        { onEvent(OnboardingEvent.ReminderSelected(ReminderTimes[it].takeIf { _ -> it != selected })) },
        title,
    )
    if (state.failed) Body(stringResource(Res.string.onboarding_failed))
    SjPrimaryButton(
        stringResource(Res.string.onboarding_reminder_start),
        { onEvent(OnboardingEvent.ContinueClicked) },
        Modifier.fillMaxWidth(),
        enabled = !state.finishing,
    )
    SjLinkButton(
        stringResource(Res.string.onboarding_reminder_skip),
        { onEvent(OnboardingEvent.SkipReminderClicked) },
        enabled = !state.finishing,
    )
}

@Composable
private fun ContinueButton(state: OnboardingState, onEvent: (OnboardingEvent) -> Unit) {
    SjPrimaryButton(
        stringResource(Res.string.onboarding_continue),
        { onEvent(OnboardingEvent.ContinueClicked) },
        Modifier.fillMaxWidth(),
        enabled = state.canContinue,
    )
}

private val ReadingGoal.label
    get() = when (this) {
        ReadingGoal.StartReading -> Res.string.onboarding_goal_start
        ReadingGoal.ReadConsistently -> Res.string.onboarding_goal_consistent
        ReadingGoal.ExploreTheme -> Res.string.onboarding_goal_theme
        ReadingGoal.WholeBible -> Res.string.onboarding_goal_whole
    }

private val PlanPreference.label
    get() = when (this) {
        PlanPreference.GuidedIntro -> Res.string.onboarding_preference_guided
        PlanPreference.Sequential -> Res.string.onboarding_preference_sequential
        PlanPreference.Thematic -> Res.string.onboarding_preference_thematic
    }
