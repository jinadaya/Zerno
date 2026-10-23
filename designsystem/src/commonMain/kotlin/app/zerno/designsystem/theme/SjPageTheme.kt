package app.zerno.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import app.zerno.designsystem.tokens.SjColors

/** The colour of the reading page, chosen apart from the app theme. */
enum class SjPageTheme { Light, Sepia, Dark }

/**
 * Colours for the Scripture page in [theme], whatever the app theme: light and dark are the app's own palettes, and
 * sepia is the light one on a warm `reader-paper`. Wrap only the reader in it.
 */
@Composable
fun SjPageTheme(theme: SjPageTheme, content: @Composable () -> Unit) {
    val colors = when (theme) {
        SjPageTheme.Light -> SjColors.Light
        SjPageTheme.Dark -> SjColors.Dark
        SjPageTheme.Sepia -> SepiaColors
    }
    CompositionLocalProvider(LocalSjColors provides colors, content = content)
}

/** Ink and muted ink keep their contrast on the warmer paper (16:1 and 5:1 or more). */
private val SepiaColors: SjColors = SjColors.Light.copy(
    readerPaper = Color(0xFFF3E7CF),
    paper = Color(0xFFEFE2C6),
    ink = Color(0xFF221A10),
    inkMuted = Color(0xFF5E5242),
)
