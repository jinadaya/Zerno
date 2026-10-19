package app.zerno.designsystem.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertEquals

class SjReaderTextTest {
    private val number = SpanStyle(color = Color.Gray)
    private val highlight = Color.Yellow

    private fun verses(vararg paragraphStarts: Int) = listOf("In the beginning", "And the earth", "And God said", "And God saw")
        .mapIndexed { i, text -> SjVerse(i + 1, AnnotatedString(text), startsParagraph = i in paragraphStarts) }

    @Test
    fun versesWithoutParagraphMarksFormOneParagraph() {
        val paragraphs = readerParagraphs(verses(), number, emptySet(), highlight, null)

        assertEquals(1, paragraphs.size)
        assertEquals("1 In the beginning 2 And the earth 3 And God said 4 And God saw", paragraphs[0].text.text)
    }

    @Test
    fun paragraphMarksSplitTheText() {
        val paragraphs = readerParagraphs(verses(2), number, emptySet(), highlight, null)

        assertEquals(listOf(listOf(0, 1), listOf(2, 3)), paragraphs.map { p -> p.verses.map { it.index } })
        assertEquals("3 And God said 4 And God saw", paragraphs[1].text.text)
    }

    @Test
    fun rangesCoverTheVerseTextWithoutItsNumber() {
        val paragraph = readerParagraphs(verses(), number, emptySet(), highlight, null).single()

        paragraph.verses.forEachIndexed { i, range ->
            assertEquals(verses()[i].text.text, paragraph.text.text.substring(range.start, range.end))
        }
    }

    @Test
    fun highlightCoversOnlyTheHighlightedVerseText() {
        val paragraph = readerParagraphs(verses(), number, setOf(2), highlight, null).single()
        val backgrounds = paragraph.text.spanStyles.filter { it.item.background == highlight }

        val range = paragraph.verses[2]
        assertEquals(listOf(range.start to range.end), backgrounds.map { it.start to it.end })
    }

    @Test
    fun tapsReportTheVerseIndex() {
        val tapped = mutableListOf<Int>()
        val paragraph = readerParagraphs(verses(3), number, emptySet(), highlight) { tapped += it }.last()
        val link = paragraph.text.getLinkAnnotations(0, paragraph.text.length).single()

        (link.item as LinkAnnotation.Clickable).linkInteractionListener?.onClick(link.item)

        assertEquals(listOf(3), tapped)
        assertEquals(0 to paragraph.text.length, link.start to link.end)
    }

    @Test
    fun noLinksWithoutAClickHandler() {
        val paragraph = readerParagraphs(verses(), number, emptySet(), highlight, null).single()

        assertEquals(0, paragraph.text.getLinkAnnotations(0, paragraph.text.length).size)
    }

    @Test
    fun fontSizeIsClampedToTheReaderRange() {
        assertEquals(16.sp, clampFontSize(12.sp))
        assertEquals(22.sp, clampFontSize(22.sp))
        assertEquals(28.sp, clampFontSize(40.sp))
        assertEquals(20.sp, clampFontSize(2.em))
    }
}
