package app.zerno.designsystem.foundation

import androidx.compose.runtime.Composable

/** A function that opens the system share sheet with plain text: the Android chooser, or iOS's activity sheet. */
@Composable
expect fun rememberShareText(): (text: String) -> Unit
