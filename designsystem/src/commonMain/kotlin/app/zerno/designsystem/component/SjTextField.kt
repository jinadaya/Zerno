package app.zerno.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.zerno.designsystem.theme.SjTheme

/**
 * An editorial text field (`sj-field`): an uppercase [label] tag, the input in 20 sp Onest, and one `ink` hairline
 * underneath that turns into a 2 dp `ultra` line on focus. With an [error] the line is a 2 dp `alert` and the error
 * replaces the [hint]; write it as the fix ("Enter a time between 00:00 and 23:59"). No box, no fill.
 *
 * Screens that hold fields should pad for the keyboard (`Modifier.imePadding()`) and scroll, so the focused field
 * is brought into view on both platforms.
 */
@Composable
fun SjTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    hint: String? = null,
    error: String? = null,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    enabled: Boolean = true,
) {
    val colors = SjTheme.colors
    val type = SjTheme.type
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val lineColor = when {
        error != null -> colors.alert
        focused -> colors.ultra
        else -> colors.ink
    }
    val lineWidth = if (error != null || focused) StrongLine else SjTheme.sizes.hairline
    val inputStyle = type.body.copy(fontSize = 20.sp, lineHeight = 28.sp, color = colors.ink)

    Column(modifier, verticalArrangement = Arrangement.spacedBy(SjTheme.spacing.space2)) {
        BasicText(label.uppercase(), style = type.tag.copy(color = colors.ink))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = singleLine,
            textStyle = inputStyle,
            cursorBrush = SolidColor(colors.ink),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = interaction,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = label
                    if (error != null) this.error(error)
                },
            decorationBox = { inner ->
                Box(
                    modifier = Modifier
                        .defaultMinSize(minHeight = SjTheme.sizes.tapMin)
                        .drawBehind {
                            val stroke = lineWidth.toPx()
                            val y = size.height - stroke / 2
                            drawLine(lineColor, Offset(0f, y), Offset(size.width, y), stroke)
                        }
                        .padding(vertical = SjTheme.spacing.space2),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty() && placeholder != null) {
                        BasicText(placeholder, style = inputStyle.copy(color = colors.inkMuted), maxLines = 1)
                    }
                    inner()
                }
            },
        )
        val note = error ?: hint
        if (note != null) {
            BasicText(note, style = type.caption.copy(color = if (error != null) colors.alert else colors.inkMuted))
        }
    }
}

/**
 * The reflection field (`sj-field textarea`): Spectral italic 19/32 written on `rule` lines every 32, like a
 * journal, at least 160 dp tall. Lines follow the text as it grows. Reflections are always optional; pair the field
 * with a Skip link button.
 */
@Composable
fun SjReflectionField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
) {
    val colors = SjTheme.colors
    val type = SjTheme.type
    val style = type.scripture.copy(
        fontSize = 19.sp,
        lineHeight = 32.sp,
        fontStyle = FontStyle.Italic,
        fontWeight = FontWeight.Normal,
        color = colors.ink,
    )
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val rule = colors.rule
    val hairline = SjTheme.sizes.hairline

    Column(modifier, verticalArrangement = Arrangement.spacedBy(SjTheme.spacing.space2)) {
        BasicText(label.uppercase(), style = type.tag.copy(color = colors.ink))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = style,
            cursorBrush = SolidColor(colors.ink),
            onTextLayout = { layout = it },
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
            decorationBox = { inner ->
                Box(
                    Modifier
                        .defaultMinSize(minHeight = ReflectionMinHeight)
                        .drawBehind {
                            // One rule under every line: the text's own line pitch, carried on to fill the field.
                            val result = layout
                            val pitch = if (result != null && result.lineCount > 0) {
                                result.getLineBottom(0) - result.getLineTop(0)
                            } else {
                                32.sp.toPx()
                            }
                            val stroke = hairline.toPx()
                            var y = TopInset.toPx() + pitch - stroke / 2
                            while (y < size.height) {
                                drawLine(rule, Offset(0f, y), Offset(size.width, y), stroke)
                                y += pitch
                            }
                        }
                        .padding(top = TopInset),
                ) {
                    if (value.isEmpty() && placeholder != null) {
                        BasicText(placeholder, style = style.copy(color = colors.inkMuted))
                    }
                    inner()
                }
            },
        )
    }
}

private val StrongLine = 2.dp
private val ReflectionMinHeight = 160.dp
private val TopInset = 4.dp

@Composable
private fun FieldSamples() {
    var name by remember { mutableStateOf("") }
    var reflection by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("25:00") }
    Column(
        modifier = Modifier.background(SjTheme.colors.paper).padding(SjTheme.spacing.gutter),
        verticalArrangement = Arrangement.spacedBy(SjTheme.spacing.space6),
    ) {
        SjTextField(name, { name = it }, label = "Your name", placeholder = "What should we call you?")
        SjReflectionField(reflection, {
            reflection = it
        }, label = "Размышление", placeholder = "Что вам запомнилось сегодня?")
        SjTextField(time, { time = it }, label = "Reminder time", error = "Enter a time between 00:00 and 23:59.")
    }
}

@Preview
@Composable
private fun SjTextFieldLightPreview() {
    SjTheme(darkTheme = false) { FieldSamples() }
}

@Preview
@Composable
private fun SjTextFieldDarkPreview() {
    SjTheme(darkTheme = true) { FieldSamples() }
}
