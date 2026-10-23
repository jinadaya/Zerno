package app.zerno.feature.gamification.ui

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.zerno.designsystem.component.SjAchievementSeal
import app.zerno.designsystem.component.SjAchievementSet
import app.zerno.designsystem.component.SjLinkButton
import app.zerno.designsystem.component.forAchievementSet
import app.zerno.designsystem.foundation.StickerSurface
import app.zerno.designsystem.icon.SjIcons
import app.zerno.designsystem.mascot.Mascot
import app.zerno.designsystem.mascot.MascotCharacter
import app.zerno.designsystem.mascot.MascotPose
import app.zerno.designsystem.theme.SjTheme
import app.zerno.entities.achievement.AchievementSet
import app.zerno.feature.gamification.resources.Res
import app.zerno.feature.gamification.resources.achievements_back
import app.zerno.feature.gamification.resources.achievements_intro
import app.zerno.feature.gamification.resources.achievements_progress
import app.zerno.feature.gamification.resources.achievements_set_bird
import app.zerno.feature.gamification.resources.achievements_set_bird_line
import app.zerno.feature.gamification.resources.achievements_set_lamp
import app.zerno.feature.gamification.resources.achievements_set_lamp_line
import app.zerno.feature.gamification.resources.achievements_set_sheep
import app.zerno.feature.gamification.resources.achievements_set_sheep_line
import app.zerno.feature.gamification.resources.achievements_unlocked
import app.zerno.feature.gamification.resources.gamification_title
import com.royaal.arch.mvi.EffectHandler
import com.royaal.navigation.LocalNavigator
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Every seal in three sets, each with its mascot. Personal only: no ranks or comparisons. */
@Composable
fun AchievementsScreen(viewModel: AchievementsViewModel) {
    val navigator = LocalNavigator.current
    EffectHandler(viewModel) { effect ->
        when (effect) {
            AchievementsEffect.Close -> navigator.back()
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
            SjLinkButton(stringResource(Res.string.achievements_back), {
                viewModel.sendEvent(AchievementsEvent.BackClicked)
            })
            BasicText(
                stringResource(Res.string.gamification_title),
                Modifier.semantics { heading() },
                style = SjTheme.type.headline.copy(color = colors.ink),
            )
            BasicText(
                stringResource(Res.string.achievements_intro),
                style = SjTheme.type.body.copy(color = colors.inkMuted),
            )
        }
        state.sections?.forEach { Section(it) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Section(section: SealSection) {
    val colors = SjTheme.colors
    val look = section.set.look
    Column(verticalArrangement = Arrangement.spacedBy(SjTheme.spacing.space4)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(SjTheme.spacing.space4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StickerSurface(colors.forAchievementSet(section.set.sj)) {
                Mascot(look.mascot, MascotPose.Achievement, size = 64.dp)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(SjTheme.spacing.space1)) {
                BasicText(
                    stringResource(look.title),
                    Modifier.semantics { heading() },
                    style = SjTheme.type.titleItalic.copy(color = colors.ink),
                )
                BasicText(stringResource(look.line), style = SjTheme.type.body.copy(color = colors.inkMuted))
                BasicText(
                    stringResource(Res.string.achievements_unlocked, section.unlocked, section.seals.size),
                    style = SjTheme.type.caption.copy(color = colors.inkMuted),
                )
            }
        }
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SjTheme.spacing.space4),
            verticalArrangement = Arrangement.spacedBy(SjTheme.spacing.space6),
        ) {
            section.seals.forEachIndexed { index, seal -> Seal(seal, tilt = if (index % 2 == 0) -8f else 6f) }
        }
    }
}

/** A seal, with "12 of 50" under a locked one that counts. */
@Composable
private fun Seal(seal: SealItem, tilt: Float) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SjTheme.spacing.space1),
    ) {
        SjAchievementSeal(
            name = seal.title,
            reason = seal.description,
            set = seal.set.sj,
            unlocked = seal.unlocked,
            icon = sealIcon(seal.icon, seal.set),
            tilt = tilt,
            dropIn = seal.dropIn,
        )
        val progress = seal.progress
        if (!seal.unlocked && progress != null) {
            BasicText(
                stringResource(Res.string.achievements_progress, progress.current, progress.target),
                style = SjTheme.type.caption.copy(color = SjTheme.colors.inkMuted),
            )
        }
    }
}

private class SetLook(val mascot: MascotCharacter, val title: StringResource, val line: StringResource)

private val AchievementSet.look: SetLook
    get() = when (this) {
        AchievementSet.Bird ->
            SetLook(MascotCharacter.Pip, Res.string.achievements_set_bird, Res.string.achievements_set_bird_line)

        AchievementSet.Sheep ->
            SetLook(MascotCharacter.Woolly, Res.string.achievements_set_sheep, Res.string.achievements_set_sheep_line)

        AchievementSet.Lamp ->
            SetLook(MascotCharacter.Lumi, Res.string.achievements_set_lamp, Res.string.achievements_set_lamp_line)
    }

internal val AchievementSet.sj: SjAchievementSet
    get() = when (this) {
        AchievementSet.Bird -> SjAchievementSet.Bird
        AchievementSet.Sheep -> SjAchievementSet.Sheep
        AchievementSet.Lamp -> SjAchievementSet.Lamp
    }

/** The catalog's icon name as an [SjIcons] glyph; an unknown name (a newer catalog) falls back to the set's own. */
private fun sealIcon(name: String, set: AchievementSet): ImageVector = SealIcons[name] ?: when (set) {
    AchievementSet.Bird -> SjIcons.Streak
    AchievementSet.Sheep -> SjIcons.Bible
    AchievementSet.Lamp -> SjIcons.Reflect
}

private val SealIcons: Map<String, ImageVector> by lazy {
    mapOf(
        "Today" to SjIcons.Today,
        "Bible" to SjIcons.Bible,
        "Plans" to SjIcons.Plans,
        "Journey" to SjIcons.Journey,
        "Highlight" to SjIcons.Highlight,
        "Save" to SjIcons.Save,
        "Reflect" to SjIcons.Reflect,
        "Search" to SjIcons.Search,
        "Check" to SjIcons.Check,
        "Streak" to SjIcons.Streak,
    )
}
