package app.zerno.feature.reader.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import app.zerno.designsystem.component.SjVerse
import app.zerno.entities.bible.Verse
import app.zerno.entities.bible.VerseStyle
import app.zerno.entities.bible.VerseText

/** A verse as [app.zerno.designsystem.component.SjReaderText] shows it; notes take [noteColor]. */
internal fun Verse.toSjVerse(noteColor: Color): SjVerse =
    SjVerse(number = number, text = text.toAnnotatedString(noteColor), startsParagraph = text.startsParagraph)

/** Italics as italics and notes muted; words of Jesus stay unstyled for now (DA-02). */
internal fun VerseText.toAnnotatedString(noteColor: Color): AnnotatedString = buildAnnotatedString {
    append(text)
    spans.forEach { span ->
        val style = when (span.style) {
            VerseStyle.Italic -> SpanStyle(fontStyle = FontStyle.Italic)
            VerseStyle.Note -> SpanStyle(color = noteColor)
            VerseStyle.WordsOfJesus -> null
        }
        if (style != null) addStyle(style, span.start, span.end)
    }
}
