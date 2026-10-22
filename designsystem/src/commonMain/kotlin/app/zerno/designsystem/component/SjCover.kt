package app.zerno.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.zerno.designsystem.foundation.SealShape
import app.zerno.designsystem.theme.SjTheme

/**
 * The brand cover (`Cover/preview.html`): the app [title] in large italic Spectral with a small uppercase
 * [tagline], next to the sticker art: a butter slab, a sky slab, an ink disc and three tilted seals, like cards
 * dropped on a desk. Wide layouts put art and words side by side; phones stack the art above the words.
 * For the splash, onboarding start and store art.
 */
@Composable
fun SjCover(
    title: String,
    tagline: String,
    modifier: Modifier = Modifier,
) {
    val colors = SjTheme.colors
    BoxWithConstraints(modifier.fillMaxWidth().background(colors.paper).clipToBounds()) {
        val words = @Composable {
            Column(
                modifier = Modifier.semantics(mergeDescendants = true) {}.widthIn(max = WordsMaxWidth),
                verticalArrangement = Arrangement.spacedBy(SjTheme.spacing.space3),
            ) {
                BasicText(
                    title,
                    style = SjTheme.type.titleItalic.copy(fontSize = 84.sp, lineHeight = 0.95.em, letterSpacing = (-0.02).em, color = colors.ink),
                )
                BasicText(tagline.uppercase(), style = SjTheme.type.tag.copy(color = colors.inkMuted), modifier = Modifier.padding(start = 4.dp))
            }
        }
        if (maxWidth >= WideLayout) {
            Row(
                modifier = Modifier.padding(SjTheme.spacing.space8),
                horizontalArrangement = Arrangement.spacedBy(SjTheme.spacing.space8),
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(Modifier.weight(1f)) { words() }
                CoverArt(Modifier.weight(1f).aspectRatio(ArtWidth / ArtHeight))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(SjTheme.spacing.space6)) {
                CoverArt(Modifier.fillMaxWidth().aspectRatio(ArtWidth / ArtHeight))
                Column(Modifier.padding(start = SjTheme.spacing.gutter, end = SjTheme.spacing.gutter, bottom = SjTheme.spacing.space8)) {
                    words()
                }
            }
        }
    }
}

/** The 480 x 288 art from the preview, scaled to the space it gets. Decorative. */
@Composable
private fun CoverArt(modifier: Modifier) {
    val colors = SjTheme.colors
    Canvas(modifier.clipToBounds()) {
        scale(size.width / ArtWidth, size.height / ArtHeight, pivot = Offset.Zero) {
            val radius = CornerRadius(32f)
            drawRoundRect(colors.butter, Offset(16f, -32f), Size(200f, 248f), radius)
            drawRoundRect(colors.sky, Offset(240f, 128f), Size(260f, 140f), radius)
            drawCircle(colors.ink, 56f, Offset(300f, 64f))
            seal(colors.flare, Offset(160f, 196f), 88f, -8f)
            seal(colors.sage, Offset(430f, 72f), 64f, 10f)
            seal(colors.flare, Offset(236f, 246f), 48f, 6f)
        }
    }
}

private fun DrawScope.seal(color: Color, center: Offset, diameter: Float, degrees: Float) {
    rotate(degrees, pivot = center) {
        translate(center.x - diameter / 2, center.y - diameter / 2) {
            drawOutline(SealShape.createOutline(Size(diameter, diameter), layoutDirection, this), color)
        }
    }
}

private const val ArtWidth = 480f
private const val ArtHeight = 288f
private val WideLayout = 720.dp
private val WordsMaxWidth = 440.dp

@Preview
@Composable
private fun SjCoverLightPreview() {
    SjTheme(darkTheme = false) { SjCover("Scripture Journey", "Read a little every day. Collect the journey.") }
}

@Preview
@Composable
private fun SjCoverDarkPreview() {
    SjTheme(darkTheme = true) { SjCover("Зерно", "Читайте понемногу каждый день.") }
}
