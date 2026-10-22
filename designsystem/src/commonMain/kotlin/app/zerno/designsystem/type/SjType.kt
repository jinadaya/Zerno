package app.zerno.designsystem.type

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.zerno.designsystem.tokens.SjTypography

/** The generated [SjTypography] set in the bundled faces. `SjTheme` calls this; components read `SjTheme.type`. */
@Composable
fun rememberSjTypography(): SjTypography {
    val fonts = rememberSjFonts()
    return remember(fonts) { SjTypography.from(display = fonts.display, serif = fonts.serif, sans = fonts.sans) }
}
