package app.zerno.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.zerno.designsystem.foundation.SjEdge
import app.zerno.designsystem.foundation.sjFocusRing
import app.zerno.designsystem.foundation.sjHairline
import app.zerno.designsystem.icon.SjIcon
import app.zerno.designsystem.icon.SjIcons
import app.zerno.designsystem.theme.SjTheme

/** One tab of [SjBottomNav]. [label] is sentence case; the bar uppercases it. */
@Immutable
data class SjNavItem(val label: String, val icon: ImageVector)

/**
 * The bottom navigation (`sj-nav`): four tabs (Today, Bible, Plans, Journey) under a full-width `ink` rule. Each tab
 * is an icon over an uppercase label; the current one is `ink` with a `flare` dot sitting on the rule, the others
 * `ink-muted`. No pills, badges or counters. The bar pads itself above the system navigation bar.
 *
 * The app shell hides it while reading; it has no hidden state of its own.
 */
@Composable
fun SjBottomNav(
    items: List<SjNavItem>,
    selected: Int,
    onSelect: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SjTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.paper)
            .sjHairline(colors.ink, SjEdge.Top)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = SjTheme.spacing.space2, end = SjTheme.spacing.space2, bottom = SjTheme.spacing.space3)
            .selectableGroup(),
    ) {
        items.forEachIndexed { index, item ->
            NavTab(item, index == selected, { onSelect(index) }, Modifier.weight(1f))
        }
    }
}

@Composable
private fun NavTab(item: SjNavItem, current: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val colors = SjTheme.colors
    val color = if (current) colors.ink else colors.inkMuted
    Box(
        modifier = modifier
            .sjFocusRing()
            .selectable(
                selected = current,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            ),
        contentAlignment = Alignment.TopCenter,
    ) {
        if (current) {
            // The dot sits on the rule: its centre is on the bar's top edge.
            val flare = colors.flare
            Canvas(Modifier.offset(y = -DotSize / 2).size(DotSize)) { drawCircle(flare) }
        }
        Column(
            modifier = Modifier.padding(top = SjTheme.spacing.space4, bottom = SjTheme.spacing.space2),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SjIcon(item.icon, contentDescription = null, tint = color, size = TabIconSize)
            BasicText(
                text = item.label.uppercase(),
                style = SjTheme.type.tag.copy(fontSize = 10.sp, lineHeight = 12.sp, color = color),
                maxLines = 1,
            )
        }
    }
}

private val DotSize = 8.dp
private val TabIconSize = 20.dp

@Composable
private fun NavSample(labels: List<String>) {
    var selected by remember { mutableIntStateOf(0) }
    val icons = listOf(SjIcons.Today, SjIcons.Bible, SjIcons.Plans, SjIcons.Journey)
    Box(Modifier.background(SjTheme.colors.paper).padding(top = SjTheme.spacing.space8)) {
        SjBottomNav(labels.zip(icons) { label, icon -> SjNavItem(label, icon) }, selected, { selected = it })
    }
}

@Preview
@Composable
private fun SjBottomNavLightPreview() {
    SjTheme(darkTheme = false) { NavSample(listOf("Today", "Bible", "Plans", "Journey")) }
}

@Preview
@Composable
private fun SjBottomNavDarkPreview() {
    SjTheme(darkTheme = true) { NavSample(listOf("Сегодня", "Библия", "Планы", "Путь")) }
}
