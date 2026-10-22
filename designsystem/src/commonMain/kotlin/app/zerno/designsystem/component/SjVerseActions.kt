package app.zerno.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import app.zerno.designsystem.foundation.sjFocusRing
import app.zerno.designsystem.icon.SjIcon
import app.zerno.designsystem.icon.SjIcons
import app.zerno.designsystem.theme.SjTheme

/**
 * One action of [SjVerseActions]. [applied] marks an action already done for this verse (it is highlighted,
 * it is saved); the button then turns `butter`. [label] is sentence case.
 */
@Immutable
data class SjVerseAction(
    val label: String,
    val icon: ImageVector,
    val applied: Boolean = false,
    val onClick: () -> Unit,
)

/**
 * The verse action bar (`sj-verse-actions`) floating above a tapped verse, in a popup. Tapping outside or the back
 * gesture calls [onDismiss]; the reader also dismisses it on scroll. [offset] places the bar relative to
 * [alignment] in the parent, usually just above the selected verse (see `SjReaderTextLayout.verseTop`).
 */
@Composable
fun SjVerseActions(
    actions: List<SjVerseAction>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.TopCenter,
    offset: IntOffset = IntOffset.Zero,
) {
    Popup(
        alignment = alignment,
        offset = offset,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        SjVerseActionBar(actions, modifier)
    }
}

/**
 * The capsule itself, without the popup: an `ink` pill with up to four actions, each an icon over an uppercase
 * label in `on-ink`. An applied action is `butter` with `sticker-ink`.
 */
@Composable
fun SjVerseActionBar(
    actions: List<SjVerseAction>,
    modifier: Modifier = Modifier,
) {
    require(actions.size <= MaxActions) { "SjVerseActionBar takes at most $MaxActions actions, got ${actions.size}" }
    val colors = SjTheme.colors
    Row(
        modifier = modifier
            .background(colors.ink, SjTheme.shapes.pill)
            .padding(BarPadding),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        actions.forEach { action -> ActionButton(action) }
    }
}

@Composable
private fun ActionButton(action: SjVerseAction) {
    val colors = SjTheme.colors
    val content = if (action.applied) colors.stickerInk else colors.onInk
    val shape = SjTheme.shapes.pill
    Box(
        modifier = Modifier
            .height(ActionHeight)
            .defaultMinSize(minWidth = ActionMinWidth)
            .sjFocusRing(shape)
            .then(if (action.applied) Modifier.background(colors.butter, shape) else Modifier)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = action.onClick,
            )
            .semantics { selected = action.applied }
            .padding(horizontal = SjTheme.spacing.space2),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(SjTheme.spacing.space1),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SjIcon(action.icon, contentDescription = null, tint = content, size = ActionIconSize)
            BasicText(
                text = action.label.uppercase(),
                style = SjTheme.type.tag.copy(fontSize = 10.sp, lineHeight = 12.sp, letterSpacing = 0.05.em, color = content),
                maxLines = 1,
            )
        }
    }
}

private const val MaxActions = 4
private val BarPadding = 6.dp
private val ActionHeight = 56.dp
private val ActionMinWidth = 64.dp
private val ActionIconSize = 20.dp

@Composable
private fun ActionSamples(labels: List<String>) {
    val icons = listOf(SjIcons.Highlight, SjIcons.Save, SjIcons.Reflect, SjIcons.Share)
    Box(Modifier.background(SjTheme.colors.readerPaper).padding(SjTheme.spacing.gutter)) {
        SjVerseActionBar(labels.zip(icons).mapIndexed { i, (label, icon) -> SjVerseAction(label, icon, applied = i == 0, onClick = {}) })
    }
}

@Preview
@Composable
private fun SjVerseActionsLightPreview() {
    SjTheme(darkTheme = false) { ActionSamples(listOf("Highlight", "Save", "Reflect", "Share")) }
}

@Preview
@Composable
private fun SjVerseActionsDarkPreview() {
    SjTheme(darkTheme = true) { ActionSamples(listOf("Выделить", "Сохранить", "Заметка", "Поделиться")) }
}
