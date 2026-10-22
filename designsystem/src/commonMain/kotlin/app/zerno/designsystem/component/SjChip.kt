package app.zerno.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.zerno.designsystem.foundation.StickerSurface
import app.zerno.designsystem.foundation.sjFocusRing
import app.zerno.designsystem.theme.LocalOnSticker
import app.zerno.designsystem.theme.SjTheme

/**
 * One option of a single choice (`sj-chip`): a hairline pill, 44 dp tall in a 48 dp row. [selected] inverts it
 * to an ink fill with `on-ink` text, so the choice never relies on hue. Use it inside [SjChipGroup], which adds the
 * group semantics; on its own it reports itself as a radio button.
 */
@Composable
fun SjChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = SjTheme.colors
    val onSticker = LocalOnSticker.current
    val ink = if (onSticker) colors.stickerInk else colors.ink
    val onInk = if (onSticker) colors.onStickerInk else colors.onInk
    val shape = SjTheme.shapes.pill
    Box(
        modifier = modifier
            .height(SjTheme.sizes.tapMin)
            .sjFocusRing(shape)
            .selectable(
                selected = selected,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .height(ChipHeight)
                .defaultMinSize(minWidth = ChipHeight)
                .then(if (selected) Modifier.background(ink, shape) else Modifier)
                .border(SjTheme.sizes.hairline, ink, shape)
                .padding(horizontal = SjTheme.spacing.space4),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                text = text.uppercase(),
                style = SjTheme.type.button.copy(fontSize = 13.sp, color = if (selected) onInk else ink),
                maxLines = 1,
            )
        }
    }
}

/**
 * A single choice from 2 to 6 short [options], such as reading time in onboarding. [selected] is the index of the
 * chosen option, or null before the user picks one. Chips wrap onto a new line when they do not fit.
 * [label] names the group for screen readers ("Daily reading time").
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SjChipGroup(
    options: List<String>,
    selected: Int?,
    onSelect: (index: Int) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier
            .semantics { contentDescription = label }
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(SjTheme.spacing.space3),
    ) {
        options.forEachIndexed { index, option ->
            SjChip(text = option, selected = index == selected, onClick = { onSelect(index) })
        }
    }
}

private val ChipHeight = 44.dp

@Composable
private fun ChipSamples() {
    val spacing = SjTheme.spacing
    var minutes by remember { mutableIntStateOf(1) }
    Column(
        modifier = Modifier.background(SjTheme.colors.paper).padding(spacing.gutter),
        verticalArrangement = Arrangement.spacedBy(spacing.space4),
    ) {
        TagRow()
        SjChipGroup(listOf("5 min", "10 min", "15 min", "20+"), minutes, { minutes = it }, label = "Daily reading time")
        StickerSurface(SjTheme.colors.butter) {
            TagRow()
            SjChipGroup(listOf("5 мин", "10 мин", "15 мин", "20+"), minutes, { minutes = it }, label = "Время чтения")
        }
    }
}

@Composable
private fun TagRow() {
    Row(horizontalArrangement = Arrangement.spacedBy(SjTheme.spacing.space2)) {
        SjTag("Day 4 / 21")
        SjTag("7 min")
        SjTag("New plan", solid = true)
    }
}

@Preview
@Composable
private fun SjChipLightPreview() {
    SjTheme(darkTheme = false) { ChipSamples() }
}

@Preview
@Composable
private fun SjChipDarkPreview() {
    SjTheme(darkTheme = true) { ChipSamples() }
}
