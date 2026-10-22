package app.zerno.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.zerno.designsystem.foundation.sjFocusRing
import app.zerno.designsystem.foundation.sjHairline
import app.zerno.designsystem.theme.SjTheme

/**
 * A settings row with a switch (`sj-switch-row`): the whole row is the label and the tap target, at least 56 dp
 * tall, with a `rule` hairline below. Only for settings that apply at once. Screen readers hear one switch
 * named by [label].
 */
@Composable
fun SjSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = SjTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = RowHeight)
            .sjHairline(colors.rule)
            .sjFocusRing()
            .toggleable(
                value = checked,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .alpha(if (enabled) 1f else DisabledAlpha),
        horizontalArrangement = Arrangement.spacedBy(SjTheme.spacing.space4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = label,
            style = SjTheme.type.body.copy(fontWeight = FontWeight.Medium, color = SjTheme.contentColor),
            modifier = Modifier.weight(1f).padding(vertical = SjTheme.spacing.space3),
        )
        SjSwitch(checked = checked)
    }
}

/**
 * The switch itself (`sj-switch`): an outlined capsule with an ink knob. On, it fills with `flare` and the knob
 * springs right. It has no click handling; [SjSwitchRow] makes the whole row toggle it and fades it when disabled.
 */
@Composable
fun SjSwitch(
    checked: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = SjTheme.colors
    val motion = SjTheme.motion
    val track by animateColorAsState(
        targetValue = if (checked) colors.flare else Color.Transparent,
        animationSpec = if (motion.reduced) snap() else tween(TrackMillis),
    )
    val knobColor by animateColorAsState(
        targetValue = if (checked) colors.stickerInk else colors.ink,
        animationSpec = if (motion.reduced) snap() else tween(TrackMillis),
    )
    val knobOffset by animateDpAsState(
        targetValue = if (checked) KnobTravel else 0.dp,
        animationSpec = motion.spring(),
    )
    val shape = SjTheme.shapes.pill
    Box(
        modifier = modifier
            .size(TrackWidth, TrackHeight)
            .background(track, shape)
            .border(SjTheme.sizes.hairline, colors.ink, shape)
            .padding(KnobInset),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.offset(x = knobOffset).size(KnobSize).background(knobColor, shape))
    }
}

private val RowHeight = 56.dp
private val TrackWidth = 56.dp
private val TrackHeight = 32.dp
private val KnobSize = 22.dp
private val KnobInset = 4.dp
private val KnobTravel = 24.dp
private const val TrackMillis = 200

@Composable
private fun SwitchSamples() {
    var reminder by remember { mutableStateOf(true) }
    var questions by remember { mutableStateOf(false) }
    Column(Modifier.background(SjTheme.colors.paper).padding(SjTheme.spacing.gutter)) {
        SjSwitchRow("Daily reminder", reminder, { reminder = it })
        SjSwitchRow("Вопросы после чтения", questions, { questions = it })
    }
}

@Preview
@Composable
private fun SjSwitchLightPreview() {
    SjTheme(darkTheme = false) { SwitchSamples() }
}

@Preview
@Composable
private fun SjSwitchDarkPreview() {
    SjTheme(darkTheme = true) { SwitchSamples() }
}
