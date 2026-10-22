package app.zerno.feature.reader.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.zerno.designsystem.tokens.SjColors
import app.zerno.designsystem.tokens.SjRadius
import app.zerno.designsystem.tokens.SjSize
import app.zerno.designsystem.type.SjFonts

/** Light or dark tokens by the system setting, until `SjTheme` (DS-04) and the page themes (MV-09) arrive. */
@Composable
internal fun readerColors(): SjColors = if (isSystemInDarkTheme()) SjColors.Dark else SjColors.Light

/** Interface copy on the reader's chrome: Onest 16, as `body` in tokens.json. */
internal fun SjFonts.body(color: Color, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = sans,
    fontWeight = weight,
    fontSize = 16.sp,
    lineHeight = 22.sp,
    color = color,
)

/** Small uppercase label above a group of settings, like the `tag` style. */
internal fun SjFonts.label(color: Color) = TextStyle(
    fontFamily = display,
    fontWeight = FontWeight.SemiBold,
    fontSize = 11.sp,
    lineHeight = 14.sp,
    letterSpacing = 0.06.em,
    color = color,
)

/**
 * The outline button from `Button/preview.html` (`sj-btn-outline`), sized for the reader's chrome: a hairline
 * `ink` pill at least [SjSize.tapMin] tall. [description] replaces [text] for screen readers when the text alone
 * would not say what the button does.
 */
@Composable
internal fun OutlinePill(
    text: String,
    colors: SjColors,
    fonts: SjFonts,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    description: String? = null,
    minWidth: Dp = SjSize.Default.tapMin,
) {
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = minWidth, minHeight = SjSize.Default.tapMin)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .border(SjSize.Default.hairline, colors.ink, RoundedCornerShape(SjRadius.Default.pill))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text, style = fonts.body(colors.ink, FontWeight.Medium))
    }
}

/** A downward chevron, drawn so it does not depend on a glyph in the bundled fonts. */
@Composable
internal fun Chevron(color: Color, modifier: Modifier = Modifier, up: Boolean = false) {
    Canvas(modifier.size(10.dp, 6.dp)) {
        val stroke = 1.5.dp.toPx()
        val top = if (up) size.height else 0f
        val bottom = if (up) 0f else size.height
        drawLine(color, Offset(0f, top), Offset(size.width / 2, bottom), stroke, StrokeCap.Round)
        drawLine(color, Offset(size.width / 2, bottom), Offset(size.width, top), stroke, StrokeCap.Round)
    }
}

/** A back arrow, drawn for the same reason as [Chevron]. */
@Composable
internal fun BackArrow(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(18.dp, 14.dp)) {
        val stroke = 1.5.dp.toPx()
        val middle = size.height / 2
        drawLine(color, Offset(0f, middle), Offset(size.width, middle), stroke, StrokeCap.Round)
        drawLine(color, Offset(0f, middle), Offset(middle, 0f), stroke, StrokeCap.Round)
        drawLine(color, Offset(0f, middle), Offset(middle, size.height), stroke, StrokeCap.Round)
    }
}

/** Chrome bar height, the same as `button-height`. */
internal val BarHeight: Dp = 56.dp

private const val DISABLED_ALPHA = 0.35f
