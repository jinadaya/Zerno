package app.zerno.designsystem.mascot

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.zerno.designsystem.foundation.StickerSurface
import app.zerno.designsystem.theme.SjTheme

/**
 * The three mascots. Each owns one part of the app and appears only there:
 * - [Woolly], the sheep: reading. Daily reading, plans, finishing a reading.
 * - [Lumi], the lamp: reflection. Reflections, saved verses, comprehension questions.
 * - [Pip], the birdie: gamification. Streaks, milestones, the achievements screen.
 */
enum class MascotCharacter { Woolly, Lumi, Pip }

/** The five poses every mascot has. */
enum class MascotPose { Welcome, Reading, Streak, WelcomeBack, Achievement }

/**
 * A mascot drawing, 200 x 200 by default. The drawings use fixed ink outlines and sticker fills, so always place
 * them on a sticker colour (inside `StickerSurface` or on a sticker-coloured block): there the outline reads in both
 * themes. Never put a mascot on the Scripture reading page.
 *
 * Mascots are illustration: pass a [contentDescription] only when the drawing says something the text around it
 * does not.
 */
@Composable
fun Mascot(
    character: MascotCharacter,
    pose: MascotPose,
    modifier: Modifier = Modifier,
    size: Dp = MascotSize,
    contentDescription: String? = null,
) {
    Image(
        painter = rememberVectorPainter(mascotVector(character, pose)),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
    )
}

/** Default size: the drawings' own 200 x 200 grid. */
val MascotSize: Dp = 200.dp

internal fun mascotVector(character: MascotCharacter, pose: MascotPose): ImageVector = with(MascotVectors) {
    when (character) {
        MascotCharacter.Woolly -> when (pose) {
            MascotPose.Welcome -> WoollyWelcome
            MascotPose.Reading -> WoollyReading
            MascotPose.Streak -> WoollyStreak
            MascotPose.WelcomeBack -> WoollyWelcomeBack
            MascotPose.Achievement -> WoollyAchievement
        }
        MascotCharacter.Lumi -> when (pose) {
            MascotPose.Welcome -> LumiWelcome
            MascotPose.Reading -> LumiReading
            MascotPose.Streak -> LumiStreak
            MascotPose.WelcomeBack -> LumiWelcomeBack
            MascotPose.Achievement -> LumiAchievement
        }
        MascotCharacter.Pip -> when (pose) {
            MascotPose.Welcome -> PipWelcome
            MascotPose.Reading -> PipReading
            MascotPose.Streak -> PipStreak
            MascotPose.WelcomeBack -> PipWelcomeBack
            MascotPose.Achievement -> PipAchievement
        }
    }
}

@Composable
private fun MascotSamples() {
    val colors = SjTheme.colors
    val stickers = mapOf(
        MascotCharacter.Woolly to colors.sage,
        MascotCharacter.Lumi to colors.butter,
        MascotCharacter.Pip to colors.rose,
    )
    Column(
        modifier = Modifier.background(colors.paper).padding(SjTheme.spacing.gutter),
        verticalArrangement = Arrangement.spacedBy(SjTheme.spacing.space4),
    ) {
        MascotCharacter.entries.forEach { character ->
            StickerSurface(stickers.getValue(character)) {
                Row(horizontalArrangement = Arrangement.spacedBy(SjTheme.spacing.space2)) {
                    MascotPose.entries.forEach { pose -> Mascot(character, pose, size = 56.dp) }
                }
            }
        }
    }
}

@Preview
@Composable
private fun MascotLightPreview() {
    SjTheme(darkTheme = false) { MascotSamples() }
}

@Preview
@Composable
private fun MascotDarkPreview() {
    SjTheme(darkTheme = true) { MascotSamples() }
}
