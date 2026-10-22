package app.zerno.data.db.bible

import app.zerno.entities.bible.VerseSpan
import app.zerno.entities.bible.VerseStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VerseMarkupTest {
    @Test
    fun john316InKjvIsCleanText() {
        val verse = VerseMarkup.parse(
            "<pb/><J>For<S>1063</S> God<S>2316</S> so<S>3779</S> loved<S>25</S> the<S>3588</S> world,<S>2889</S> " +
                "that<S>5620</S> he gave<S>1325</S> his<S>848</S> only begotten<S>3439</S> Son,<S>5207</S> " +
                "that<S>2443</S> whosoever<S>3956</S> believeth<S>4100</S> in<S>1519</S> him<S>846</S> should " +
                "not<S>3361</S> perish,<S>622</S> but<S>235</S> have<S>2192</S> everlasting<S>166</S> life.</J><S>2222</S>",
        )
        val text = "For God so loved the world, that he gave his only begotten Son, that whosoever believeth in him " +
            "should not perish, but have everlasting life."
        assertEquals(text, verse.text)
        assertEquals(listOf(VerseSpan(VerseStyle.WordsOfJesus, 0, text.length)), verse.spans)
        assertTrue(verse.startsParagraph)
    }

    @Test
    fun john316InRstIsCleanText() {
        val verse = VerseMarkup.parse(
            "<pb/><J>Ибо<S>1063</S> так<S>3779</S> возлюбил<S>25</S><S>3588</S> " +
                "Бог<S>2316</S><S>3588</S> мир,<S>2889</S> что<S>5620</S><S>3588</S> отдал<S>1325</S> " +
                "Сына<S>5207</S> Своего<S>846</S><S>3588</S> " +
                "Единородного,<S>3439</S> дабы<S>2443</S> всякий<S>3956</S><S>3588</S> " +
                "верующий<S>4100</S> в<S>1519</S> " +
                "Него,<S>846</S> не<S>3361</S> погиб,<S>622</S> но<S>235</S> имел<S>2192</S> жизнь<S>2222</S> " +
                "вечную.<S>166</S></J>",
        )
        val text = "Ибо так возлюбил Бог мир, что отдал Сына Своего Единородного, дабы всякий верующий в Него, " +
            "не погиб, но имел жизнь вечную."
        assertEquals(text, verse.text)
        assertEquals(listOf(VerseSpan(VerseStyle.WordsOfJesus, 0, text.length)), verse.spans)
    }

    @Test
    fun strongsNumberBetweenSpacesLeavesOneSpace() {
        assertEquals(
            "God created the heaven",
            VerseMarkup.parse("God<S>430</S> created<S>1254</S> <S>853</S> the heaven").text,
        )
    }

    @Test
    fun italicsAndNotesBecomeSpans() {
        val verse = VerseMarkup.parse("<n>A Psalm.</n> darkness <i>was</i> upon <em>the</em> <e>deep</e>")
        assertEquals("A Psalm. darkness was upon the deep", verse.text)
        assertEquals(
            listOf(
                VerseSpan(VerseStyle.Note, 0, 8),
                VerseSpan(VerseStyle.Italic, 18, 21),
                VerseSpan(VerseStyle.Italic, 27, 30),
                VerseSpan(VerseStyle.Italic, 31, 35),
            ),
            verse.spans,
        )
    }

    @Test
    fun spanDoesNotStartOrEndOnWhitespace() {
        val verse = VerseMarkup.parse("liveth,<i> there</i> <i>is</i>")
        assertEquals("liveth, there is", verse.text)
        assertEquals(listOf(VerseSpan(VerseStyle.Italic, 8, 13), VerseSpan(VerseStyle.Italic, 14, 16)), verse.spans)
    }

    @Test
    fun footnoteMarkerIsDropped() {
        assertEquals("в пяту.", VerseMarkup.parse("в пяту.<S>6119</S><f>[1]</f>").text)
    }

    @Test
    fun paragraphPoetryAndLineBreaks() {
        val verse = VerseMarkup.parse("И сказал: <t>вот, это кость</t><br/> богатство! <pb/>И все")
        assertEquals("И сказал:\nвот, это кость\nбогатство!\nИ все", verse.text)
        assertEquals(listOf(verse.text.indexOf("И все")), verse.paragraphStarts)
    }

    @Test
    fun unbalancedTagsAreTolerated() {
        // KJV has a `</t>` with no opener; KM has `<J>` never closed.
        assertEquals("Blessed are", VerseMarkup.parse("</t> <pb/>Blessed <i>are</i><t>").text)
        val open = VerseMarkup.parse("<J>Verily I say")
        assertEquals(listOf(VerseSpan(VerseStyle.WordsOfJesus, 0, 12)), open.spans)
        assertEquals("a b", VerseMarkup.parse("a</i> b").text)
    }
}
