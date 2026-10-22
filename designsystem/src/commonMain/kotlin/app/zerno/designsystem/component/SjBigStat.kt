package app.zerno.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import app.zerno.designsystem.theme.SjTheme

/**
 * One oversized number with an italic line under it (`sj-stat`), in place of rings and charts: "38% of John read",
 * "23 days with Scripture this month". One per screen section. A [unit] such as "%" is set small and raised.
 * Count what was done, never what was missed. Screen readers hear number and line as one phrase.
 */
@Composable
fun SjBigStat(
    value: String,
    line: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
) {
    val content = SjTheme.contentColor
    val type = SjTheme.type
    Column(
        modifier = modifier.semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(SjTheme.spacing.space1),
    ) {
        BasicText(
            text = buildAnnotatedString {
                append(value)
                if (unit != null) {
                    withStyle(SpanStyle(fontSize = UnitSize, letterSpacing = 0.sp, baselineShift = UnitShift)) { append(unit) }
                }
            },
            style = type.mega.copy(color = content),
            maxLines = 1,
        )
        BasicText(
            text = line,
            style = type.titleItalic.copy(fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = 0.sp, color = content),
        )
    }
}

/** `sup` in `.sj-stat`: 28 px, raised 32 px. */
private val UnitSize = 28.sp
private val UnitShift = BaselineShift(32f / 28f)
