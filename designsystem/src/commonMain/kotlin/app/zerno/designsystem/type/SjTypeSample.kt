package app.zerno.designsystem.type

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import app.zerno.designsystem.theme.SjTheme
import app.zerno.designsystem.tokens.SjTypography

private class TypeSample(
    val name: String,
    val style: (SjTypography) -> TextStyle,
    val en: String,
    val ru: String,
    val upper: Boolean = false,
)

private val Samples = listOf(
    TypeSample("mega", { it.mega }, "38%", "38%"),
    TypeSample("headline", { it.headline }, "Today", "Сегодня"),
    TypeSample("button", { it.button }, "Start reading", "Начать чтение", upper = true),
    TypeSample("tag", { it.tag }, "Day 4 / 21", "День 4 / 21", upper = true),
    TypeSample("title-italic", { it.titleItalic }, "a lamp unto my feet", "светильник ноге моей"),
    TypeSample("chapter-title", { it.chapterTitle }, "Psalm 119", "Псалом 118"),
    TypeSample("scripture", { it.scripture }, "Thy word is a lamp unto my feet.", "Слово Твоё — светильник ноге моей."),
    TypeSample("body", { it.body }, "Pick up where you left off.", "Продолжите с того места, где остановились."),
    TypeSample("body-strong", { it.bodyStrong }, "Gospel of John", "Евангелие от Иоанна"),
    TypeSample("caption", { it.caption }, "7 min read", "7 мин чтения"),
    TypeSample("verse-number", { it.verseNumber }, "105", "105"),
)

/** Every `SjTheme.type` style with an English and a Russian sample, on the current theme's paper. For the catalog. */
@Composable
fun SjTypeSample(modifier: Modifier = Modifier) {
    val colors = SjTheme.colors
    val type = SjTheme.type
    Column(
        modifier = modifier.background(colors.paper).padding(SjTheme.spacing.gutter),
        verticalArrangement = Arrangement.spacedBy(SjTheme.spacing.space6),
    ) {
        Samples.forEach { sample ->
            val style = sample.style(type).copy(color = colors.ink)
            Column(verticalArrangement = Arrangement.spacedBy(SjTheme.spacing.space1)) {
                BasicText(sample.name, style = type.caption.copy(color = colors.inkMuted))
                BasicText(if (sample.upper) sample.en.uppercase() else sample.en, style = style)
                BasicText(if (sample.upper) sample.ru.uppercase() else sample.ru, style = style)
            }
        }
    }
}

@Preview
@Composable
private fun SjTypeSampleLightPreview() {
    SjTheme(darkTheme = false) { SjTypeSample() }
}

@Preview
@Composable
private fun SjTypeSampleDarkPreview() {
    SjTheme(darkTheme = true) { SjTypeSample() }
}
