package app.zerno.designsystem.theme

import androidx.compose.runtime.Composable

/**
 * Whether the system asks for less motion: "Remove animations" on Android, "Reduce Motion" on iOS. Recomposes when
 * the user changes it while the app is open.
 */
@Composable
internal expect fun systemReducedMotion(): Boolean
