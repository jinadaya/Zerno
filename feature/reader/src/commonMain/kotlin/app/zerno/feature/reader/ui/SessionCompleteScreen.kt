package app.zerno.feature.reader.ui

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import app.zerno.designsystem.component.SjAchievementSeal
import app.zerno.designsystem.component.SjBigStat
import app.zerno.designsystem.component.SjLinkButton
import app.zerno.designsystem.component.SjPrimaryButton
import app.zerno.designsystem.component.SjTag
import app.zerno.designsystem.foundation.StickerSurface
import app.zerno.designsystem.mascot.Mascot
import app.zerno.designsystem.mascot.MascotCharacter
import app.zerno.designsystem.mascot.MascotPose
import app.zerno.designsystem.theme.SjTheme
import app.zerno.entities.bible.BookNameLanguage
import app.zerno.entities.bible.PassageRef
import app.zerno.entities.bible.Versification
import app.zerno.entities.bible.format
import app.zerno.entities.bible.mapVersification
import app.zerno.feature.gamification.api.StreakDayStatus
import app.zerno.feature.reader.resources.Res
import app.zerno.feature.reader.resources.session_back_to_today
import app.zerno.feature.reader.resources.session_gone
import app.zerno.feature.reader.resources.session_new_seals
import app.zerno.feature.reader.resources.session_plan_step
import app.zerno.feature.reader.resources.session_read
import app.zerno.feature.reader.resources.session_reflect
import app.zerno.feature.reader.resources.session_streak
import app.zerno.feature.reader.resources.session_title
import app.zerno.feature.reader.resources.session_today_done
import app.zerno.feature.reader.resources.session_today_read
import app.zerno.feature.reflection.api.ReflectionRoute
import com.royaal.arch.mvi.EffectHandler
import com.royaal.navigation.LocalNavigator
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun SessionCompleteScreen(viewModel: SessionCompleteViewModel) {
    val navigator = LocalNavigator.current
    EffectHandler(viewModel) { effect ->
        when (effect) {
            is SessionCompleteEffect.OpenReflection ->
                navigator.navigate(ReflectionRoute(effect.passage, effect.sessionId, effect.afterPlanStep))

            // The reader made way for this screen on top of Today.
            SessionCompleteEffect.BackToToday -> navigator.back()
        }
    }
    val state by viewModel.state.collectAsState()
    SessionCompleteContent(state, viewModel::sendEvent)
}

@Composable
private fun SessionCompleteContent(state: SessionCompleteState, onEvent: (SessionCompleteEvent) -> Unit) {
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
        Streak(state)
        val summary = state.summary
        if (summary == null) {
            BasicText(stringResource(Res.string.session_gone), style = SjTheme.type.body.copy(color = colors.inkMuted))
        } else {
            Read(summary)
            if (summary.seals.isNotEmpty()) Seals(summary.seals)
            SjPrimaryButton(
                stringResource(Res.string.session_reflect),
                { onEvent(SessionCompleteEvent.ReflectClicked) },
                Modifier.fillMaxWidth(),
            )
        }
        SjLinkButton(stringResource(Res.string.session_back_to_today), { onEvent(SessionCompleteEvent.DoneClicked) })
    }
}

/** Pip with the streak, and whether today is Read or, after a reflection, Done. */
@Composable
private fun Streak(state: SessionCompleteState) {
    val type = SjTheme.type
    StickerSurface(SjTheme.colors.flare, Modifier.fillMaxWidth()) {
        Mascot(MascotCharacter.Pip, MascotPose.Streak, size = 120.dp)
        BasicText(
            stringResource(Res.string.session_title),
            Modifier.semantics { heading() },
            style = type.titleItalic.copy(color = SjTheme.contentColor),
        )
        if (state.streak > 0) {
            SjBigStat(state.streak.toString(), pluralStringResource(Res.plurals.session_streak, state.streak))
        }
        val today = if (state.today ==
            StreakDayStatus.Done
        ) {
            Res.string.session_today_done
        } else {
            Res.string.session_today_read
        }
        BasicText(stringResource(today), style = type.body.copy(color = SjTheme.contentColor))
    }
}

/** The passage and translation read, and the plan step it ticked. */
@Composable
private fun Read(summary: SessionSummary) {
    val colors = SjTheme.colors
    val type = SjTheme.type
    val language = BookNameLanguage.fromCode(Locale.current.language)
    Column(verticalArrangement = Arrangement.spacedBy(SjTheme.spacing.space2)) {
        if (summary.day != null && summary.planLength != null) {
            SjTag(stringResource(Res.string.session_plan_step, summary.day, summary.planLength))
        }
        BasicText(
            stringResource(Res.string.session_read, summary.passage.display(language), summary.translation),
            style = type.headline.copy(color = colors.ink),
        )
        summary.planTitle?.let { BasicText(it, style = type.caption.copy(color = colors.inkMuted)) }
    }
}

@Composable
private fun Seals(seals: List<SealText>) {
    Column(verticalArrangement = Arrangement.spacedBy(SjTheme.spacing.space4)) {
        BasicText(
            stringResource(Res.string.session_new_seals),
            Modifier.semantics { heading() },
            style = SjTheme.type.headline.copy(color = SjTheme.colors.ink),
        )
        seals.forEach { seal ->
            SjAchievementSeal(
                name = seal.title,
                reason = seal.description,
                set = seal.set,
                unlocked = true,
                dropIn = true,
            )
        }
    }
}

/** In Russian the passage reads in the Synodal numbering its readers know: "Пс 22" for Ps 23. */
private fun PassageRef.display(language: BookNameLanguage): String = when (language) {
    BookNameLanguage.English -> format(language)
    BookNameLanguage.Russian -> mapVersification(Versification.English, Versification.Russian).format(language)
}
