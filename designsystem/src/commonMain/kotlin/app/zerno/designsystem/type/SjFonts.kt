package app.zerno.designsystem.type

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import app.zerno.designsystem.resources.Res
import app.zerno.designsystem.resources.onest_medium
import app.zerno.designsystem.resources.onest_regular
import app.zerno.designsystem.resources.onest_semibold
import app.zerno.designsystem.resources.spectral_italic
import app.zerno.designsystem.resources.spectral_regular
import app.zerno.designsystem.resources.spectral_semibold
import app.zerno.designsystem.resources.unbounded_bold
import app.zerno.designsystem.resources.unbounded_semibold
import org.jetbrains.compose.resources.Font

/**
 * The three faces from `type.families` in tokens.json, bundled as Compose resources so they look the same on
 * Android and iOS. All three cover Latin and Cyrillic, so the Russian localisation uses the same type.
 * Only the weights the type styles use are bundled; a missing weight is synthesised by the platform.
 * Licences: `designsystem/fonts/OFL-*.txt`.
 */
@Immutable
data class SjFonts(
    /** Unbounded 600 and 700: big numbers, screen titles, buttons and tags. */
    val display: FontFamily,
    /** Spectral 400, 400 italic and 600: Scripture, chapter titles, italic headlines. */
    val serif: FontFamily,
    /** Onest 400, 500 and 600: interface copy and captions. */
    val sans: FontFamily,
)

/** Loads the bundled fonts. Resource fonts load in composition, so this is a composable and not a constant. */
@Composable
fun rememberSjFonts(): SjFonts = SjFonts(
    display = FontFamily(
        Font(Res.font.unbounded_semibold, FontWeight.SemiBold),
        Font(Res.font.unbounded_bold, FontWeight.Bold),
    ),
    serif = FontFamily(
        Font(Res.font.spectral_regular, FontWeight.Normal),
        Font(Res.font.spectral_italic, FontWeight.Normal, FontStyle.Italic),
        Font(Res.font.spectral_semibold, FontWeight.SemiBold),
    ),
    sans = FontFamily(
        Font(Res.font.onest_regular, FontWeight.Normal),
        Font(Res.font.onest_medium, FontWeight.Medium),
        Font(Res.font.onest_semibold, FontWeight.SemiBold),
    ),
)
