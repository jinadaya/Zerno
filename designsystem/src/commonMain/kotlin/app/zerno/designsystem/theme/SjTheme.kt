package app.zerno.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import app.zerno.designsystem.tokens.SjColors
import app.zerno.designsystem.tokens.SjSize
import app.zerno.designsystem.tokens.SjSpacing
import app.zerno.designsystem.tokens.SjTypography
import app.zerno.designsystem.type.rememberSjTypography

val LocalSjColors = staticCompositionLocalOf { SjColors.Light }
val LocalSjType = staticCompositionLocalOf<SjTypography> { error("No SjTypography here: wrap the UI in SjTheme") }
val LocalSjSpacing = staticCompositionLocalOf { SjSpacing.Default }
val LocalSjShapes = staticCompositionLocalOf { SjShapes.Default }
val LocalSjMotion = staticCompositionLocalOf { SjMotion.Default }

/** True when the system asks for reduced motion. Same as `SjTheme.motion.reduced`; here for code that only needs the flag. */
val LocalReducedMotion = staticCompositionLocalOf { false }

/**
 * The design system's theme. There is no Material theme underneath: every component reads its tokens through
 * [SjTheme.colors], [SjTheme.type], [SjTheme.spacing], [SjTheme.shapes] and [SjTheme.motion].
 *
 * [darkTheme] picks [SjColors.Dark] or [SjColors.Light]; it follows the system unless the app theme setting (MV-09)
 * overrides it. [reducedMotion] follows the system too and swaps every [SjMotion] spec for a snap.
 */
@Composable
fun SjTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    reducedMotion: Boolean = systemReducedMotion(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) SjColors.Dark else SjColors.Light
    val selection = remember(colors) {
        TextSelectionColors(handleColor = colors.ultra, backgroundColor = colors.ultra.copy(alpha = 0.3f))
    }
    CompositionLocalProvider(
        LocalSjColors provides colors,
        LocalSjType provides rememberSjTypography(),
        LocalSjSpacing provides SjSpacing.Default,
        LocalSjShapes provides SjShapes.Default,
        LocalSjMotion provides if (reducedMotion) SjMotion.Reduced else SjMotion.Default,
        LocalReducedMotion provides reducedMotion,
        LocalTextSelectionColors provides selection,
        content = content,
    )
}

/** Shortcuts to the current theme values, for use inside [SjTheme]. */
object SjTheme {
    val colors: SjColors
        @Composable @ReadOnlyComposable
        get() = LocalSjColors.current

    val type: SjTypography
        @Composable @ReadOnlyComposable
        get() = LocalSjType.current

    val spacing: SjSpacing
        @Composable @ReadOnlyComposable
        get() = LocalSjSpacing.current

    val shapes: SjShapes
        @Composable @ReadOnlyComposable
        get() = LocalSjShapes.current

    val motion: SjMotion
        @Composable @ReadOnlyComposable
        get() = LocalSjMotion.current

    /** Fixed component sizes. They do not change with the theme, so there is no local for them. */
    val sizes: SjSize
        get() = SjSize.Default
}
