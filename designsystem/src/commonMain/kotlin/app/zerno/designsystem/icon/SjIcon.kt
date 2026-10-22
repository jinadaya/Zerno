package app.zerno.designsystem.icon

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.zerno.designsystem.foundation.StickerSurface
import app.zerno.designsystem.theme.SjTheme

/** Icon size from the design system README. */
val SjIconSize: Dp = 24.dp

/**
 * Draws one of [SjIcons], tinted like `currentColor`: [tint] defaults to the content colour, so the icon is `ink`
 * on paper and `sticker-ink` on a sticker. Pass a [contentDescription] only when no label next to the icon says
 * the same thing; decorative icons pass null.
 */
@Composable
fun SjIcon(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = SjTheme.contentColor,
    size: Dp = SjIconSize,
) {
    Image(
        painter = rememberVectorPainter(icon),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        colorFilter = ColorFilter.tint(tint),
    )
}

/** A Lucide icon: stroked paths on a 24 x 24 grid, 2 wide, round caps and joins. Black, so a tint replaces it. */
internal fun lucide(name: String, vararg paths: String): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = SjIconSize,
    defaultHeight = SjIconSize,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    paths.forEach { d ->
        addPath(
            pathData = addPathNodes(d),
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }
}.build()

private val AllIcons: List<ImageVector>
    get() = with(SjIcons) {
        listOf(
            Today, Bible, Plans, Journey, Highlight, Save, Reflect, Share, Copy, ArrowRight, ArrowLeft, ChevronLeft,
            ChevronRight, ChevronDown, Close, Check, Lock, Settings, Reminder, Streak, TextSize, Translation, Search,
            Add,
        )
    }

@Composable
private fun IconGrid() {
    val gap = Arrangement.spacedBy(SjTheme.spacing.space4)
    Column(verticalArrangement = gap) {
        AllIcons.chunked(6).forEach { row ->
            Row(horizontalArrangement = gap) { row.forEach { SjIcon(it, contentDescription = null) } }
        }
    }
}

@Composable
private fun IconSamples() {
    Column(
        modifier = Modifier.background(SjTheme.colors.paper).padding(SjTheme.spacing.gutter),
        verticalArrangement = Arrangement.spacedBy(SjTheme.spacing.space4),
    ) {
        IconGrid()
        StickerSurface(SjTheme.colors.butter) { IconGrid() }
    }
}

@Preview
@Composable
private fun SjIconsLightPreview() {
    SjTheme(darkTheme = false) { IconSamples() }
}

@Preview
@Composable
private fun SjIconsDarkPreview() {
    SjTheme(darkTheme = true) { IconSamples() }
}
