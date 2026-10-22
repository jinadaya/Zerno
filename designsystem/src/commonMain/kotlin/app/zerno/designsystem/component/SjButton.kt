package app.zerno.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.zerno.designsystem.foundation.StickerSurface
import app.zerno.designsystem.foundation.sjFocusRing
import app.zerno.designsystem.icon.SjIcon
import app.zerno.designsystem.icon.SjIcons
import app.zerno.designsystem.theme.LocalOnSticker
import app.zerno.designsystem.theme.SjTheme

/**
 * The primary button (`sj-btn-primary`): an ink pill, 56 dp tall, with an uppercase label and a 44 dp `flare`
 * circle holding an arrow. One per screen. Write [text] in sentence case, verb first; it is uppercased here.
 * Add `Modifier.fillMaxWidth()` for a full-width button: the label stays left and the arrow right.
 * Inside a `StickerSurface` the fill switches to `sticker-ink`.
 */
@Composable
fun SjPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = SjTheme.colors
    val onSticker = LocalOnSticker.current
    val fill = if (onSticker) colors.stickerInk else colors.ink
    val label = if (onSticker) colors.onStickerInk else colors.onInk
    ButtonFrame(onClick, modifier, enabled) {
        Row(
            modifier = Modifier
                .height(SjTheme.sizes.buttonHeight)
                .background(fill, SjTheme.shapes.pill)
                .border(SjTheme.sizes.hairline, fill, SjTheme.shapes.pill)
                .padding(start = SjTheme.spacing.space6, end = GoInset),
            // Label left, arrow right; the spacer keeps them apart when the button is only as wide as its label.
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ButtonLabel(text, label, Modifier.weight(1f, fill = false))
            Spacer(Modifier.width(SjTheme.spacing.space4))
            Box(Modifier.size(GoSize).background(colors.flare, SjTheme.shapes.pill), contentAlignment = Alignment.Center) {
                SjIcon(SjIcons.ArrowRight, contentDescription = null, tint = colors.stickerInk, size = GoIconSize)
            }
        }
    }
}

/** The outline button (`sj-btn-outline`) for neutral choices: a hairline pill, 56 dp tall, label centred. */
@Composable
fun SjOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val content = SjTheme.contentColor
    ButtonFrame(onClick, modifier, enabled) {
        Box(
            modifier = Modifier
                .height(SjTheme.sizes.buttonHeight)
                .border(SjTheme.sizes.hairline, content, SjTheme.shapes.pill)
                .padding(PaddingValues(horizontal = SjTheme.spacing.space6)),
            contentAlignment = Alignment.Center,
        ) {
            ButtonLabel(text, content)
        }
    }
}

/** The text link (`sj-btn-link`) for skip and dismiss: an underlined label in a 48 dp tall tap area. */
@Composable
fun SjLinkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val content = SjTheme.contentColor
    val hairline = SjTheme.sizes.hairline
    var baseline by remember { mutableFloatStateOf(Float.NaN) }
    ButtonFrame(onClick, modifier, enabled) {
        Box(Modifier.height(SjTheme.sizes.tapMin), contentAlignment = Alignment.Center) {
            BasicText(
                text = text.uppercase(),
                style = SjTheme.type.button.copy(color = content),
                maxLines = 1,
                onTextLayout = { baseline = it.firstBaseline },
                modifier = Modifier.drawBehind {
                    if (!baseline.isNaN()) {
                        val stroke = hairline.toPx()
                        val y = baseline + UnderlineOffset.toPx()
                        drawLine(content, Offset(0f, y), Offset(size.width, y), stroke)
                    }
                },
            )
        }
    }
}

/** Click handling shared by the three buttons: focus ring, button role, 35% when disabled, a small press. */
@Composable
private fun ButtonFrame(
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val reduced = SjTheme.motion.reduced
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) PressedScale else 1f,
        animationSpec = if (reduced) snap() else tween(PressMillis),
    )
    Box(
        modifier = modifier
            .scale(scale)
            .alpha(if (enabled) 1f else DisabledAlpha)
            .sjFocusRing(SjTheme.shapes.pill)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        propagateMinConstraints = true,
    ) {
        content()
    }
}

@Composable
private fun ButtonLabel(text: String, color: Color, modifier: Modifier = Modifier) {
    BasicText(
        text = text.uppercase(),
        style = SjTheme.type.button.copy(color = color),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/** Disabled buttons drop to 35%. Prefer explaining why over disabling. */
internal const val DisabledAlpha = 0.35f
private const val PressedScale = 0.97f
private const val PressMillis = 120
private val GoSize = 44.dp
private val GoIconSize = 20.dp
private val GoInset = 6.dp
private val UnderlineOffset = 6.dp

@Composable
private fun ButtonSamples() {
    val spacing = SjTheme.spacing
    Column(
        modifier = Modifier.background(SjTheme.colors.paper).padding(spacing.gutter),
        verticalArrangement = Arrangement.spacedBy(spacing.space3),
    ) {
        SjPrimaryButton("Start reading", onClick = {})
        SjOutlineButton("Change plan", onClick = {})
        SjLinkButton("Skip for now", onClick = {})
        SjPrimaryButton("Finish", onClick = {}, enabled = false)
        StickerSurface(SjTheme.colors.butter) {
            SjPrimaryButton("Продолжить", onClick = {}, modifier = Modifier.fillMaxWidth())
            SjOutlineButton("Сменить план", onClick = {})
            SjLinkButton("Пропустить", onClick = {})
        }
    }
}

@Preview
@Composable
private fun SjButtonLightPreview() {
    SjTheme(darkTheme = false) { ButtonSamples() }
}

@Preview
@Composable
private fun SjButtonDarkPreview() {
    SjTheme(darkTheme = true) { ButtonSamples() }
}
