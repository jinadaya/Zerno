package app.zerno.feature.today.ui

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import app.zerno.designsystem.component.SjPrimaryButton
import app.zerno.designsystem.component.SjTodayCard
import app.zerno.designsystem.component.SjTodayProgress
import app.zerno.designsystem.component.SjWelcomeBack
import app.zerno.designsystem.foundation.StickerSurface
import app.zerno.designsystem.mascot.Mascot
import app.zerno.designsystem.mascot.MascotCharacter
import app.zerno.designsystem.mascot.MascotPose
import app.zerno.designsystem.theme.SjTheme
import app.zerno.entities.bible.BookNameLanguage
import app.zerno.feature.plans.api.PlansRoute
import app.zerno.feature.reader.api.ReaderRoute
import app.zerno.feature.today.domain.TodayAction
import app.zerno.feature.today.domain.TodayCard
import app.zerno.feature.today.domain.reference
import app.zerno.feature.today.resources.Res
import app.zerno.feature.today.resources.today_continue
import app.zerno.feature.today.resources.today_day
import app.zerno.feature.today.resources.today_minutes_unit
import app.zerno.feature.today.resources.today_no_plan_action
import app.zerno.feature.today.resources.today_no_plan_body
import app.zerno.feature.today.resources.today_no_plan_title
import app.zerno.feature.today.resources.today_open_failed
import app.zerno.feature.today.resources.today_progress_description
import app.zerno.feature.today.resources.today_read_again
import app.zerno.feature.today.resources.today_start
import app.zerno.feature.today.resources.today_tag
import app.zerno.feature.today.resources.today_title
import app.zerno.feature.today.resources.today_verse_position
import app.zerno.feature.today.resources.today_welcome_lighter
import app.zerno.feature.today.resources.today_welcome_message
import app.zerno.feature.today.resources.today_welcome_message_no_plan
import app.zerno.feature.today.resources.today_welcome_resume
import app.zerno.feature.today.resources.today_welcome_title
import com.royaal.arch.mvi.EffectHandler
import com.royaal.navigation.LocalNavigator
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Composable
fun TodayScreen(viewModel: TodayViewModel) {
    val navigator = LocalNavigator.current
    EffectHandler(viewModel) { effect ->
        when (effect) {
            // The reader opens over Today, so back returns here.
            TodayEffect.OpenReader -> navigator.navigate(ReaderRoute)

            TodayEffect.OpenPlans -> navigator.selectTab(PlansRoute)
        }
    }
    // Runs again on return from the reader, so Start turns into Continue.
    LaunchedEffect(Unit) { viewModel.sendEvent(TodayEvent.Shown) }
    val state by viewModel.state.collectAsState()
    TodayContent(state, viewModel::sendEvent)
}

@Composable
private fun TodayContent(state: TodayState, onEvent: (TodayEvent) -> Unit) {
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
        BasicText(
            stringResource(Res.string.today_title),
            Modifier.semantics { heading() },
            style = SjTheme.type.headline.copy(color = colors.ink),
        )
        when {
            !state.planLoaded -> Unit
            state.welcomeBack -> WelcomeBack(state.card, onEvent)
            state.card == null -> NoPlan(onEvent)
            else -> Card(state.card, onEvent)
        }
        if (state.openFailed) {
            BasicText(
                stringResource(Res.string.today_open_failed),
                style = SjTheme.type.body.copy(color = colors.inkMuted),
            )
        }
        state.streak?.let { StreakWeek(it) }
    }
}

@Composable
private fun Card(card: TodayCard, onEvent: (TodayEvent) -> Unit) {
    val language = Locale.current.language
    val progress = card.progress
    SjTodayCard(
        tags = listOf(
            stringResource(Res.string.today_tag),
            stringResource(Res.string.today_day, card.day.day, card.plan.length),
        ),
        theme = card.day.reference(BookNameLanguage.fromCode(language)),
        reference = card.plan.title.get(language),
        minutes = card.day.minutes.toString(),
        minutesUnit = stringResource(Res.string.today_minutes_unit),
        actionLabel = stringResource(card.action.label),
        onAction = { onEvent(TodayEvent.ActionClicked) },
        progress = progress?.let {
            SjTodayProgress(
                progress = it.fraction,
                position = stringResource(Res.string.today_verse_position, it.verse, it.verses),
                percent = "${(it.fraction * Percent).roundToInt()}%",
                description = stringResource(Res.string.today_progress_description),
            )
        },
    )
}

/** Back after two or more days: where they stopped, and a lighter way in. Never the days missed. */
@Composable
private fun WelcomeBack(card: TodayCard?, onEvent: (TodayEvent) -> Unit) {
    val language = BookNameLanguage.fromCode(Locale.current.language)
    SjWelcomeBack(
        headline = stringResource(Res.string.today_welcome_title),
        message = card?.let { stringResource(Res.string.today_welcome_message, it.day.reference(language)) }
            ?: stringResource(Res.string.today_welcome_message_no_plan),
        resumeLabel = stringResource(Res.string.today_welcome_resume),
        onResume = { onEvent(TodayEvent.ResumeClicked) },
        todayLabel = stringResource(Res.string.today_welcome_lighter),
        onToday = { onEvent(TodayEvent.LighterClicked) },
        illustration = { Mascot(MascotCharacter.Pip, MascotPose.WelcomeBack, size = 120.dp) },
    )
}

/** No plan is active: Woolly and a way to the plan list. */
@Composable
private fun NoPlan(onEvent: (TodayEvent) -> Unit) {
    val type = SjTheme.type
    StickerSurface(SjTheme.colors.sage, Modifier.fillMaxWidth()) {
        Mascot(MascotCharacter.Woolly, MascotPose.Reading, size = 96.dp)
        BasicText(
            stringResource(Res.string.today_no_plan_title),
            Modifier.semantics { heading() },
            style = type.titleItalic.copy(color = SjTheme.contentColor),
        )
        BasicText(stringResource(Res.string.today_no_plan_body), style = type.body.copy(color = SjTheme.contentColor))
        SjPrimaryButton(
            stringResource(Res.string.today_no_plan_action),
            { onEvent(TodayEvent.ChoosePlanClicked) },
            Modifier.fillMaxWidth(),
        )
    }
}

private val TodayAction.label
    get() = when (this) {
        TodayAction.Start -> Res.string.today_start
        TodayAction.Continue -> Res.string.today_continue
        TodayAction.ReadAgain -> Res.string.today_read_again
    }

private const val Percent = 100
