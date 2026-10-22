package app.zerno.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import app.zerno.designsystem.foundation.StickerSurface
import app.zerno.designsystem.theme.SjTheme

/**
 * The welcome back card (`WelcomeBack/preview.html`): a `lilac` sticker for someone returning after a few days.
 * A large italic [headline] ("welcome back"), one sentence saying where they stopped ([message]), a primary
 * [resumeLabel] button and a [todayLabel] link to today's reading. Never mention days missed or a lost streak.
 * [illustration] is an optional slot above the headline, for Pip's welcome back pose.
 */
@Composable
fun SjWelcomeBack(
    headline: String,
    message: String,
    resumeLabel: String,
    onResume: () -> Unit,
    todayLabel: String,
    onToday: () -> Unit,
    modifier: Modifier = Modifier,
    illustration: (@Composable () -> Unit)? = null,
) {
    val colors = SjTheme.colors
    val type = SjTheme.type
    StickerSurface(colors.lilac, modifier.fillMaxWidth()) {
        illustration?.invoke()
        BasicText(
            headline,
            style = type.titleItalic.copy(fontSize = 44.sp, lineHeight = 46.sp, color = colors.stickerInk),
        )
        BasicText(message, style = type.body.copy(color = colors.stickerInk))
        Row(
            horizontalArrangement = Arrangement.spacedBy(SjTheme.spacing.space4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SjPrimaryButton(resumeLabel, onResume)
            SjLinkButton(todayLabel, onToday)
        }
    }
}

@Composable
private fun WelcomeSample(ru: Boolean) {
    Box(Modifier.background(SjTheme.colors.paper).padding(SjTheme.spacing.gutter)) {
        if (ru) {
            SjWelcomeBack(
                headline = "с возвращением",
                message = "Вы читали Ин 4. Продолжите с того места или начните с сегодняшнего отрывка.",
                resumeLabel = "Продолжить",
                onResume = {},
                todayLabel = "На сегодня",
                onToday = {},
            )
        } else {
            SjWelcomeBack(
                headline = "welcome\nback",
                message = "You were reading John 4. Pick up where you left off, or start with today's passage.",
                resumeLabel = "Resume",
                onResume = {},
                todayLabel = "Today's reading",
                onToday = {},
            )
        }
    }
}

@Preview
@Composable
private fun SjWelcomeBackLightPreview() {
    SjTheme(darkTheme = false) { WelcomeSample(ru = false) }
}

@Preview
@Composable
private fun SjWelcomeBackDarkPreview() {
    SjTheme(darkTheme = true) { WelcomeSample(ru = true) }
}
