package app.zerno.designsystem.foundation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import app.zerno.designsystem.theme.LocalOnSticker
import app.zerno.designsystem.theme.SjTheme

/**
 * A sticker card (`sj-sticker sj-grain sj-on-sticker`): one solid sticker colour with `radius-sticker` corners,
 * film grain, 24 dp padding and a 16 dp gap between children. No border, no shadow.
 *
 * Inside it [LocalOnSticker] is true, so components switch to `sticker-ink` (black in both themes) through
 * `SjTheme.contentColor`. [color] should be one of `butter`, `sky`, `sage`, `rose`, `lilac` or `flare`.
 * Content is not clipped: a seal may overhang the corner, as on the Today card.
 */
@Composable
fun StickerSurface(
    color: Color,
    modifier: Modifier = Modifier,
    shape: Shape = SjTheme.shapes.sticker,
    contentPadding: PaddingValues = PaddingValues(SjTheme.spacing.stickerPadding),
    content: @Composable ColumnScope.() -> Unit,
) {
    CompositionLocalProvider(LocalOnSticker provides true) {
        Column(
            modifier = modifier
                .background(color, shape)
                .sjGrain(shape)
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(SjTheme.spacing.space4),
            content = content,
        )
    }
}

@Composable
private fun StickerSamples() {
    val colors = SjTheme.colors
    Column(
        modifier = Modifier.background(colors.paper).padding(SjTheme.spacing.gutter),
        verticalArrangement = Arrangement.spacedBy(SjTheme.spacing.space4),
    ) {
        listOf(
            "butter" to colors.butter,
            "sky" to colors.sky,
            "sage" to colors.sage,
            "rose" to colors.rose,
            "lilac" to colors.lilac,
            "flare" to colors.flare,
        ).forEach { (name, color) ->
            StickerSurface(color) {
                BasicText(name.uppercase(), style = SjTheme.type.tag.copy(color = SjTheme.contentColor))
                BasicText("the woman at the well", style = SjTheme.type.titleItalic.copy(color = SjTheme.contentColor))
            }
        }
        BasicText(
            "On paper",
            style = SjTheme.type.body.copy(color = SjTheme.contentColor),
            modifier = Modifier.sjHairline(colors.rule).padding(vertical = SjTheme.spacing.space3),
        )
    }
}

@Preview
@Composable
private fun StickerSurfaceLightPreview() {
    SjTheme(darkTheme = false) { StickerSamples() }
}

@Preview
@Composable
private fun StickerSurfaceDarkPreview() {
    SjTheme(darkTheme = true) { StickerSamples() }
}
