package app.zerno.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.zerno.designsystem.theme.SjTheme

/**
 * A small uppercase capsule (`sj-tag`) for plan days, durations and status: "Day 4 / 21", "7 min".
 * The outline tag takes the content colour, so it works on paper and on stickers. [solid] makes the `flare`
 * capsule with `sticker-ink` text, for one "new" or "today" marker at a time.
 * Write [text] in sentence case, four words at most; it is uppercased here.
 */
@Composable
fun SjTag(text: String, modifier: Modifier = Modifier, solid: Boolean = false) {
    val colors = SjTheme.colors
    val content = if (solid) colors.stickerInk else SjTheme.contentColor
    val shape = SjTheme.shapes.pill
    Row(
        modifier = modifier
            .height(TagHeight)
            .then(
                if (solid) {
                    Modifier.background(colors.flare, shape)
                } else {
                    Modifier.border(SjTheme.sizes.hairline, content, shape)
                },
            )
            .padding(horizontal = TagPadding),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(text.uppercase(), style = SjTheme.type.tag.copy(color = content), maxLines = 1)
    }
}

private val TagHeight = 24.dp
private val TagPadding = 10.dp
