package app.zerno.feature.reflection.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import app.zerno.designsystem.component.SjLinkButton
import app.zerno.designsystem.component.SjPrimaryButton
import app.zerno.designsystem.component.SjReflectionField
import app.zerno.designsystem.component.SjTag
import app.zerno.designsystem.foundation.StickerSurface
import app.zerno.designsystem.mascot.Mascot
import app.zerno.designsystem.mascot.MascotCharacter
import app.zerno.designsystem.mascot.MascotPose
import app.zerno.designsystem.theme.SjTheme
import app.zerno.entities.bible.BookNameLanguage
import app.zerno.entities.bible.PassageRef
import app.zerno.feature.reflection.api.SavedReflection
import app.zerno.feature.reflection.domain.display
import app.zerno.feature.reflection.resources.Res
import app.zerno.feature.reflection.resources.reflection_earlier
import app.zerno.feature.reflection.resources.reflection_field
import app.zerno.feature.reflection.resources.reflection_placeholder
import app.zerno.feature.reflection.resources.reflection_prompt
import app.zerno.feature.reflection.resources.reflection_save
import app.zerno.feature.reflection.resources.reflection_save_failed
import app.zerno.feature.reflection.resources.reflection_skip
import app.zerno.feature.reflection.resources.reflection_title
import com.royaal.arch.mvi.EffectHandler
import com.royaal.navigation.LocalNavigator
import org.jetbrains.compose.resources.stringResource

@Composable
fun ReflectionScreen(passage: PassageRef, viewModel: ReflectionViewModel) {
    val navigator = LocalNavigator.current
    EffectHandler(viewModel) { effect ->
        when (effect) {
            ReflectionEffect.Close -> navigator.back()
        }
    }
    val state by viewModel.state.collectAsState()
    ReflectionContent(passage, state, viewModel::sendEvent)
}

@Composable
private fun ReflectionContent(passage: PassageRef, state: ReflectionState, onEvent: (ReflectionEvent) -> Unit) {
    val colors = SjTheme.colors
    val type = SjTheme.type
    val language = BookNameLanguage.fromCode(Locale.current.language)
    var text by rememberSaveable { mutableStateOf("") }
    // The session's saved reflection fills the field once, unless the user has started typing.
    LaunchedEffect(state.existing) {
        val existing = state.existing
        if (existing != null && text.isEmpty()) text = existing
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.paper)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SjTheme.spacing.gutter, vertical = SjTheme.spacing.space6),
        verticalArrangement = Arrangement.spacedBy(SjTheme.spacing.space6),
    ) {
        SjLinkButton(stringResource(Res.string.reflection_skip), { onEvent(ReflectionEvent.SkipClicked) })
        StickerSurface(colors.lilac, Modifier.fillMaxWidth()) {
            Mascot(MascotCharacter.Lumi, MascotPose.Reading, size = 96.dp)
            SjTag(passage.display(language))
            BasicText(
                stringResource(Res.string.reflection_title),
                Modifier.semantics { heading() },
                style = type.titleItalic.copy(color = SjTheme.contentColor),
            )
            BasicText(
                stringResource(Res.string.reflection_prompt),
                style = type.body.copy(color = SjTheme.contentColor),
            )
        }
        SjReflectionField(
            value = text,
            onValueChange = { text = it },
            label = stringResource(Res.string.reflection_field),
            placeholder = stringResource(Res.string.reflection_placeholder),
            modifier = Modifier.fillMaxWidth(),
        )
        SjPrimaryButton(
            stringResource(Res.string.reflection_save),
            { onEvent(ReflectionEvent.SaveClicked(text)) },
            Modifier.fillMaxWidth(),
            enabled = text.isNotBlank() && !state.saving,
        )
        if (state.failed) {
            BasicText(
                stringResource(Res.string.reflection_save_failed),
                style = type.body.copy(color = colors.inkMuted),
            )
        }
        if (state.earlier.isNotEmpty()) Earlier(state.earlier, language)
    }
}

/** Reflections already written, newest first. */
@Composable
private fun Earlier(earlier: List<SavedReflection>, language: BookNameLanguage) {
    val colors = SjTheme.colors
    val type = SjTheme.type
    Column(verticalArrangement = Arrangement.spacedBy(SjTheme.spacing.space4)) {
        BasicText(
            stringResource(Res.string.reflection_earlier),
            Modifier.semantics { heading() },
            style = type.headline.copy(color = colors.ink),
        )
        earlier.forEach { saved ->
            Column(
                Modifier.semantics(mergeDescendants = true) {},
                verticalArrangement = Arrangement.spacedBy(SjTheme.spacing.space1),
            ) {
                BasicText(
                    saved.reflection.passage.display(language),
                    style = type.caption.copy(color = colors.inkMuted),
                )
                BasicText(saved.reflection.text, style = type.body.copy(color = colors.ink))
            }
        }
    }
}
