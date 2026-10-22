package app.zerno.designsystem.foundation

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.ui.Modifier
import app.zerno.designsystem.tokens.SjSize

/** At least `tap-min` (48 dp) in both directions, so anything tappable meets the touch target size. */
fun Modifier.sjMinTapTarget(): Modifier =
    defaultMinSize(minWidth = SjSize.Default.tapMin, minHeight = SjSize.Default.tapMin)
