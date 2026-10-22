package app.zerno.designsystem.foundation

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import app.zerno.designsystem.tokens.SjSize

/** Which edge of a box an [sjHairline] runs along. */
enum class SjEdge { Top, Bottom }

/**
 * A `hairline` rule along one edge, drawn inside the bounds, as `border-top` / `border-bottom` in bundle.css.
 * Use `rule` between list rows and `ink` for a section top (`sj-list`, `sj-nav`). Separation comes from rules
 * and colour blocks, never shadows.
 */
fun Modifier.sjHairline(color: Color, edge: SjEdge = SjEdge.Bottom, width: Dp = SjSize.Default.hairline): Modifier =
    drawBehind {
        val stroke = width.toPx()
        val y = if (edge == SjEdge.Top) stroke / 2 else size.height - stroke / 2
        drawLine(color, Offset(0f, y), Offset(size.width, y), stroke)
    }
