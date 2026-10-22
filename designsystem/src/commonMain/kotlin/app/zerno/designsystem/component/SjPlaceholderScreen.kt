package app.zerno.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.zerno.designsystem.theme.SjTheme

/**
 * Stand-in for a screen that is not built yet: its [title] and one line on what will be there. Reached from the
 * debug menu until the feature replaces it. [onBack] adds a [backLabel] link, for platforms without a back gesture.
 */
@Composable
fun SjPlaceholderScreen(
    title: String,
    caption: String,
    modifier: Modifier = Modifier,
    backLabel: String = "Back",
    onBack: (() -> Unit)? = null,
) {
    val colors = SjTheme.colors
    val type = SjTheme.type
    Column(
        modifier
            .fillMaxSize()
            .background(colors.paper)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = SjTheme.spacing.gutter, vertical = SjTheme.spacing.space6),
        verticalArrangement = Arrangement.spacedBy(SjTheme.spacing.space3),
    ) {
        if (onBack != null) SjLinkButton(backLabel, onBack)
        BasicText(title, style = type.headline.copy(color = colors.ink))
        BasicText(caption, style = type.body.copy(color = colors.inkMuted))
    }
}

@Preview
@Composable
private fun SjPlaceholderScreenPreview() {
    SjTheme { SjPlaceholderScreen("Plans", "Reading plans will be listed here.", onBack = {}) }
}

@Preview
@Composable
private fun SjPlaceholderScreenDarkRuPreview() {
    SjTheme(darkTheme = true) {
        SjPlaceholderScreen("Планы", "Здесь будут планы чтения.", backLabel = "Назад", onBack = {})
    }
}
