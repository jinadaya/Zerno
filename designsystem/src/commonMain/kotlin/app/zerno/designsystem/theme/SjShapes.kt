package app.zerno.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import app.zerno.designsystem.tokens.SjRadius

/** Shapes for the `radius` tokens, so components do not build their own [RoundedCornerShape]. */
@Immutable
data class SjShapes(
    /** `radius-none`: text fields, list rows, the reader. */
    val none: Shape = RectangleShape,
    /** `radius-sticker`: sticker cards. */
    val sticker: Shape = RoundedCornerShape(SjRadius.Default.sticker),
    /** `radius-pill`: buttons, tags, switches, streak dots. Half the height, whatever the height is. */
    val pill: Shape = RoundedCornerShape(percent = 50),
) {
    companion object {
        val Default: SjShapes = SjShapes()
    }
}
