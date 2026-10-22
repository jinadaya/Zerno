package app.zerno.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.zerno.designsystem.foundation.SjEdge
import app.zerno.designsystem.foundation.sjFocusRing
import app.zerno.designsystem.foundation.sjHairline
import app.zerno.designsystem.icon.SjIcon
import app.zerno.designsystem.icon.SjIcons
import app.zerno.designsystem.theme.SjTheme
import app.zerno.designsystem.tokens.SjColors
import kotlin.math.roundToInt

/** The kinds of reading plan. Each owns one sticker colour, used for its row dot, cards and seals. */
enum class SjPlanKind { Introductory, Sequential, Thematic, Psalms }

/** introductory `sage`, sequential `sky`, thematic `rose`, Psalms and poetry `lilac`. */
fun SjColors.forPlan(kind: SjPlanKind): Color = when (kind) {
    SjPlanKind.Introductory -> sage
    SjPlanKind.Sequential -> sky
    SjPlanKind.Thematic -> rose
    SjPlanKind.Psalms -> lilac
}

/** Plans as a table of contents (`sj-list`): an `ink` rule on top, then [SjPlanRow]s divided by `rule`. */
@Composable
fun SjPlanList(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().sjHairline(SjTheme.colors.ink, SjEdge.Top), content = content)
}

/**
 * One plan (`sj-row`): a big percentage, the plan [name] with a [caption] ("Day 8 / 21 · 10 min a day"), a thin
 * progress line, and the plan's colour as a dot. A finished plan passes [finished] ("7/7"), which replaces the
 * percentage; its caption carries the finish date, the line goes and the dot gets a check. Never "behind schedule".
 */
@Composable
fun SjPlanRow(
    name: String,
    caption: String,
    kind: SjPlanKind,
    progress: Float,
    modifier: Modifier = Modifier,
    finished: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val colors = SjTheme.colors
    val type = SjTheme.type
    val number = buildAnnotatedString {
        if (finished != null) {
            append(finished)
        } else {
            append((progress.coerceIn(0f, 1f) * 100).roundToInt().toString())
            withStyle(SpanStyle(fontSize = 12.sp, letterSpacing = 0.sp)) { append("%") }
        }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .sjHairline(colors.rule)
            .then(
                if (onClick != null) {
                    Modifier
                        .sjFocusRing()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Button,
                            onClick = onClick,
                        )
                } else {
                    Modifier
                },
            )
            .semantics(mergeDescendants = true) {}
            .padding(vertical = SjTheme.spacing.space4),
        horizontalArrangement = Arrangement.spacedBy(SjTheme.spacing.space3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = number,
            style = type.mega.copy(fontSize = 28.sp, lineHeight = 28.sp, color = colors.ink),
            maxLines = 1,
            modifier = Modifier.width(NumberColumn),
        )
        Column(Modifier.weight(1f)) {
            BasicText(name, style = type.bodyStrong.copy(color = colors.ink))
            BasicText(caption, style = type.caption.copy(color = colors.inkMuted))
            if (finished == null) {
                val fill = colors.ink
                val track = colors.rule
                val shown = progress.coerceIn(0f, 1f)
                Canvas(Modifier.padding(top = 10.dp).fillMaxWidth().height(FillHeight)) {
                    val trackHeight = TrackHeight.toPx()
                    drawRect(track, Offset(0f, (size.height - trackHeight) / 2), Size(size.width, trackHeight))
                    drawRoundRect(
                        fill,
                        Offset.Zero,
                        Size(size.width * shown, size.height),
                        CornerRadius(size.height / 2),
                    )
                }
            }
        }
        Box(
            Modifier.size(DotSize).background(colors.forPlan(kind), SjTheme.shapes.pill),
            contentAlignment = Alignment.Center,
        ) {
            if (finished != null) {
                SjIcon(SjIcons.Check, contentDescription = null, tint = colors.stickerInk, size = CheckSize)
            }
        }
    }
}

private val NumberColumn = 56.dp
private val TrackHeight = 2.dp
private val FillHeight = 4.dp
private val DotSize = 28.dp
private val CheckSize = 14.dp

@Composable
private fun PlanSamples() {
    Box(Modifier.background(SjTheme.colors.paper).padding(SjTheme.spacing.gutter)) {
        SjPlanList {
            SjPlanRow("Gospel of John", "Day 8 / 21 · 10 min a day", SjPlanKind.Sequential, 0.38f, onClick = {})
            SjPlanRow("Псалмы утешения", "День 2 / 14 · 5 мин в день", SjPlanKind.Psalms, 0.12f, onClick = {})
            SjPlanRow("First steps", "Finished 2 Oct", SjPlanKind.Introductory, 1f, finished = "7/7")
        }
    }
}

@Preview
@Composable
private fun SjPlanListLightPreview() {
    SjTheme(darkTheme = false) { PlanSamples() }
}

@Preview
@Composable
private fun SjPlanListDarkPreview() {
    SjTheme(darkTheme = true) { PlanSamples() }
}
