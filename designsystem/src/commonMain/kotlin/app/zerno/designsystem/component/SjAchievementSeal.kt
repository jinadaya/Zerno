package app.zerno.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.zerno.designsystem.foundation.SealShape
import app.zerno.designsystem.icon.SjIcon
import app.zerno.designsystem.icon.SjIcons
import app.zerno.designsystem.mascot.Mascot
import app.zerno.designsystem.mascot.MascotCharacter
import app.zerno.designsystem.mascot.MascotPose
import app.zerno.designsystem.theme.SjTheme
import app.zerno.designsystem.tokens.SjColors

/**
 * The three achievement sets. Each belongs to a mascot, which decides the seal's default colour, icon and badge:
 * [Bird] is Pip's (streaks, milestones), [Sheep] is Woolly's (reading), [Lamp] is Lumi's (reflection).
 */
enum class SjAchievementSet { Bird, Sheep, Lamp }

/** Default seal colour per set: Bird `flare`, Sheep `sage`, Lamp `butter`. */
fun SjColors.forAchievementSet(set: SjAchievementSet): Color = when (set) {
    SjAchievementSet.Bird -> flare
    SjAchievementSet.Sheep -> sage
    SjAchievementSet.Lamp -> butter
}

/**
 * An achievement as a collectible sticker (`sj-badge`): a scalloped seal tilted by [tilt] degrees like a sticker on
 * a notebook, with the uppercase [name] and a one-line [reason] under it.
 *
 * Unlocked, the seal is filled with [color] (the set's colour unless a plan colour is passed), carries [icon] in
 * `sticker-ink` and the set's mascot as a small badge. Locked, it is a dashed `ink-muted` outline with a lock, no
 * tilt, and [reason] says how to earn it ("Read all 150"). Personal only: no ranks or comparisons.
 *
 * [dropIn] plays the drop-in once when the seal first appears, for a newly earned achievement; reduced motion skips
 * it. The caller decides when it is new.
 */
@Composable
fun SjAchievementSeal(
    name: String,
    reason: String,
    set: SjAchievementSet,
    unlocked: Boolean,
    modifier: Modifier = Modifier,
    icon: ImageVector = defaultIcon(set),
    color: Color = SjTheme.colors.forAchievementSet(set),
    tilt: Float = -8f,
    dropIn: Boolean = false,
) {
    val colors = SjTheme.colors
    val type = SjTheme.type
    Column(
        modifier = modifier.width(BadgeWidth).semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(SjTheme.spacing.space2),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Medal(set, unlocked, icon, color, if (unlocked) tilt else 0f, dropIn && unlocked)
        BasicText(
            text = name.uppercase(),
            style = type.tag.copy(letterSpacing = 0.04.em, color = colors.ink, textAlign = TextAlign.Center),
        )
        BasicText(
            text = reason,
            style = type.caption.copy(
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = colors.inkMuted,
                textAlign = TextAlign.Center,
            ),
        )
    }
}

@Composable
private fun Medal(
    set: SjAchievementSet,
    unlocked: Boolean,
    icon: ImageVector,
    color: Color,
    tilt: Float,
    dropIn: Boolean,
) {
    val colors = SjTheme.colors
    val reduced = SjTheme.motion.reduced
    val spring = SjTheme.motion.spring<Float>()
    val progress = remember { Animatable(if (dropIn && !reduced) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (progress.value < 1f) progress.animateTo(1f, spring)
    }
    val muted = colors.inkMuted
    Box(
        modifier = Modifier
            .size(MedalSize)
            .graphicsLayer {
                val p = progress.value
                rotationZ = tilt + (1f - p) * DropSpin
                scaleX = 1f + (1f - p) * DropGrow
                scaleY = scaleX
                alpha = p.coerceIn(0f, 1f)
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val outline = SealShape.createOutline(size, layoutDirection, this)
            if (unlocked) {
                drawOutline(outline, color)
            } else {
                val dash = size.width * LockedDash
                drawOutline(
                    outline,
                    muted,
                    style = Stroke(
                        size.width * LockedStroke,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash)),
                    ),
                )
            }
        }
        SjIcon(
            icon = if (unlocked) icon else SjIcons.Lock,
            contentDescription = null,
            tint = if (unlocked) colors.stickerInk else muted,
            size = MedalIconSize,
        )
        if (unlocked) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 4.dp, y = 2.dp)
                    .size(BadgeSize)
                    .background(color, SjTheme.shapes.pill),
                contentAlignment = Alignment.Center,
            ) {
                Mascot(mascotFor(set), MascotPose.Achievement, size = BadgeSize)
            }
        }
    }
}

private fun defaultIcon(set: SjAchievementSet): ImageVector = when (set) {
    SjAchievementSet.Bird -> SjIcons.Streak
    SjAchievementSet.Sheep -> SjIcons.Bible
    SjAchievementSet.Lamp -> SjIcons.Reflect
}

private fun mascotFor(set: SjAchievementSet): MascotCharacter = when (set) {
    SjAchievementSet.Bird -> MascotCharacter.Pip
    SjAchievementSet.Sheep -> MascotCharacter.Woolly
    SjAchievementSet.Lamp -> MascotCharacter.Lumi
}

private val BadgeWidth = 100.dp
private val MedalSize = 88.dp
private val MedalIconSize = 30.dp
private val BadgeSize = 32.dp

/** Locked outline: 1.5 wide and dashes of 4 on the seal's 100 grid. */
private const val LockedStroke = 0.015f
private const val LockedDash = 0.04f

/** The drop-in starts 12 degrees further round and 30% larger, transparent, then springs into place. */
private const val DropSpin = 12f
private const val DropGrow = 0.3f

@Composable
private fun SealSamples() {
    Column(Modifier.background(SjTheme.colors.paper).padding(SjTheme.spacing.gutter)) {
        Row(horizontalArrangement = Arrangement.spacedBy(SjTheme.spacing.space4)) {
            SjAchievementSeal("First step", "First chapter read", SjAchievementSet.Sheep, unlocked = true)
            SjAchievementSeal("Неделя", "7 дней с Писанием", SjAchievementSet.Bird, unlocked = true, tilt = 6f)
            SjAchievementSeal("Reflective", "10 reflections", SjAchievementSet.Lamp, unlocked = true)
            SjAchievementSeal("Psalms", "Read all 150", SjAchievementSet.Sheep, unlocked = false)
        }
    }
}

@Preview
@Composable
private fun SjAchievementSealLightPreview() {
    SjTheme(darkTheme = false) { SealSamples() }
}

@Preview
@Composable
private fun SjAchievementSealDarkPreview() {
    SjTheme(darkTheme = true) { SealSamples() }
}
