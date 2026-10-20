package app.zerno.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.zerno.designsystem.tokens.SjColors
import app.zerno.designsystem.tokens.SjRadius
import app.zerno.designsystem.tokens.SjSize
import app.zerno.designsystem.tokens.SjSpacing
import app.zerno.designsystem.type.SjFonts
import app.zerno.designsystem.type.rememberSjFonts
import kotlin.math.min

/**
 * One verse for [SjReaderText]. [text] keeps the verse's own spans (italics, words of Jesus) and may hold line
 * breaks for poetry; [startsParagraph] begins a new paragraph before this verse.
 */
@Immutable
data class SjVerse(
    val number: Int,
    val text: AnnotatedString,
    val startsParagraph: Boolean = false,
)

object SjReaderTextDefaults {
    /** Smallest Scripture size the reader offers. */
    val MinFontSize: TextUnit = 16.sp

    /** Largest Scripture size the reader offers. */
    val MaxFontSize: TextUnit = 28.sp

    /** `scripture` in tokens.json. */
    val FontSize: TextUnit = 20.sp

    /** Line height stays at 1.7 at every size. */
    val LineHeight: TextUnit = 1.7.em
}

/**
 * The reader page from `ReaderText/preview.html`: Scripture on `reader-paper`, a translation tag and the chapter
 * title on top, verses with `verse-number` superscripts, and the translation credit at the bottom. Nothing else
 * belongs on this page: no stickers, streaks or colour blocks.
 *
 * Verses are addressed by their index in [verses], not by verse number, so a passage that starts mid-chapter
 * works the same. [highlighted] verses get `highlight` behind them; the [selected] verse gets a wavy `ultra`
 * underline while the verse actions are open. A tap on a verse calls [onVerseClick] with its index; selection
 * itself is the caller's state.
 *
 * [fontSize] is clamped to [SjReaderTextDefaults.MinFontSize]..[SjReaderTextDefaults.MaxFontSize] and the text
 * column is capped at `reader-measure` of that size. The page does not scroll by itself; put it in a scroll
 * container.
 *
 * Pass a [layout] to find verses on the page: which one is at a scroll offset, and where one starts.
 *
 * [colors] and [fonts] are parameters until `SjTheme` (DS-04) provides them.
 */
@Composable
fun SjReaderText(
    translation: String,
    chapterTitle: String,
    verses: List<SjVerse>,
    credit: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = SjReaderTextDefaults.FontSize,
    highlighted: Set<Int> = emptySet(),
    selected: Int? = null,
    onVerseClick: ((index: Int) -> Unit)? = null,
    layout: SjReaderTextLayout? = null,
    colors: SjColors = SjColors.Light,
    fonts: SjFonts = rememberSjFonts(),
) {
    val spacing = SjSpacing.Default
    val sizes = SjSize.Default
    val scriptureSize = clampFontSize(fontSize)
    val measure = with(LocalDensity.current) { (scriptureSize.value * sizes.readerMeasure.value).sp.toDp() }

    val scripture = TextStyle(
        fontFamily = fonts.serif,
        fontWeight = FontWeight.Normal,
        fontSize = scriptureSize,
        lineHeight = SjReaderTextDefaults.LineHeight,
        color = colors.ink,
    )
    val verseNumber = SpanStyle(
        fontFamily = fonts.sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        color = colors.inkMuted,
        baselineShift = BaselineShift.Superscript,
    )
    val currentOnVerseClick by rememberUpdatedState(onVerseClick)
    val clickable = onVerseClick != null
    val paragraphs = remember(verses, highlighted, colors, fonts, clickable) {
        readerParagraphs(
            verses = verses,
            numberStyle = verseNumber,
            highlighted = highlighted,
            highlightColor = colors.highlight,
            onVerseClick = if (clickable) { index -> currentOnVerseClick?.invoke(index) } else null,
        )
    }

    Column(
        modifier
            .onPlaced { layout?.root = it }
            .background(colors.readerPaper)
            .padding(horizontal = spacing.space6, vertical = spacing.space12),
    ) {
        Column(Modifier.widthIn(max = measure)) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
                ReaderTag(translation, colors, fonts, sizes.hairline)
                BasicText(
                    text = chapterTitle,
                    style = TextStyle(
                        fontFamily = fonts.serif,
                        fontWeight = FontWeight.Normal,
                        fontSize = 40.sp,
                        lineHeight = 44.sp,
                        letterSpacing = (-0.01).em,
                        color = colors.ink,
                    ),
                )
            }
            Spacer(Modifier.height(spacing.space8))
            Column(verticalArrangement = Arrangement.spacedBy(spacing.space4)) {
                paragraphs.forEachIndexed { index, paragraph ->
                    ParagraphText(paragraph, scripture, selected, colors.ultra, layout, index)
                }
            }
            Spacer(Modifier.height(spacing.space8))
            BasicText(
                text = credit,
                style = TextStyle(
                    fontFamily = fonts.sans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = colors.inkMuted,
                ),
                modifier = Modifier
                    .drawBehind {
                        val stroke = sizes.hairline.toPx()
                        drawLine(colors.rule, Offset(0f, stroke / 2), Offset(size.width, stroke / 2), stroke)
                    }
                    .padding(top = spacing.space3),
            )
        }
    }
}

/** The outline `tag` above the chapter title. Uppercase is applied here; the string stays sentence case. */
@Composable
private fun ReaderTag(text: String, colors: SjColors, fonts: SjFonts, hairline: Dp) {
    Box(
        modifier = Modifier
            .height(24.dp)
            .border(hairline, colors.ink, RoundedCornerShape(SjRadius.Default.pill))
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = text.uppercase(),
            style = TextStyle(
                fontFamily = fonts.display,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                letterSpacing = 0.06.em,
                color = colors.ink,
            ),
        )
    }
}

@Composable
private fun ParagraphText(
    paragraph: ReaderParagraph,
    style: TextStyle,
    selected: Int?,
    underline: Color,
    readerLayout: SjReaderTextLayout?,
    position: Int,
) {
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val selectedRange = paragraph.verses.firstOrNull { it.index == selected }
    BasicText(
        text = paragraph.text,
        style = style,
        onTextLayout = { result ->
            layout = result
            readerLayout?.laidOut(position, paragraph.verses.associate { it.index to result.verseTop(it) })
        },
        modifier = Modifier
            .onPlaced { readerLayout?.placed(position, it) }
            .drawBehind {
                val result = layout
                if (selectedRange != null && result != null) {
                    drawWavyUnderline(result, selectedRange.start, selectedRange.end, underline)
                }
            },
    )
}

/**
 * Where each verse of an [SjReaderText] sits, for a screen that restores a reading position or asks which verse is
 * at the top. Offsets are in px from the top of the [SjReaderText], so inside a `verticalScroll` that starts with
 * it they compare directly with `ScrollState.value`. Remember one per page: a new chapter needs a new layout.
 */
@Stable
class SjReaderTextLayout {
    internal var root: LayoutCoordinates? = null
    private val paragraphs = mutableMapOf<Int, PlacedParagraph>()

    /**
     * Grows every time a paragraph is laid out again, e.g. after a font size change. Not read during composition by
     * the page itself; observe it in `snapshotFlow` to wait for a new layout.
     */
    var revision: Int by mutableIntStateOf(0)
        private set

    /** Top of verse [index] (its line, number included), or null while it is not laid out. */
    fun verseTop(index: Int): Float? {
        val root = root?.takeIf { it.isAttached } ?: return null
        val paragraph = paragraphs.values.firstOrNull { index in it.verseTops } ?: return null
        val coordinates = paragraph.coordinates?.takeIf { it.isAttached } ?: return null
        return root.localPositionOf(coordinates, Offset.Zero).y + paragraph.verseTops.getValue(index)
    }

    /** The verse at [y]: the last one whose top is at or above it, or null above the first verse. */
    fun verseAt(y: Float): Int? {
        val root = root?.takeIf { it.isAttached } ?: return null
        var found: Int? = null
        for (paragraph in paragraphs.values) {
            val coordinates = paragraph.coordinates?.takeIf { it.isAttached } ?: continue
            val paragraphTop = root.localPositionOf(coordinates, Offset.Zero).y
            for ((index, top) in paragraph.verseTops) {
                if (paragraphTop + top <= y && (found == null || index > found)) found = index
            }
        }
        return found
    }

    internal fun laidOut(position: Int, verseTops: Map<Int, Float>) {
        paragraphs.getOrPut(position) { PlacedParagraph() }.verseTops = verseTops
        // Called while measuring: reading the counter there would make the text depend on it.
        Snapshot.withoutReadObservation { revision++ }
    }

    internal fun placed(position: Int, coordinates: LayoutCoordinates) {
        paragraphs.getOrPut(position) { PlacedParagraph() }.coordinates = coordinates
    }

    private class PlacedParagraph {
        var coordinates: LayoutCoordinates? = null
        var verseTops: Map<Int, Float> = emptyMap()
    }
}

/** Top of the line [range] starts on. */
private fun TextLayoutResult.verseTop(range: VerseRange): Float =
    getLineTop(getLineForOffset(range.start.coerceAtMost(layoutInput.text.length)))

/** Where one verse's own text sits in its paragraph, without the verse number. */
internal data class VerseRange(val index: Int, val start: Int, val end: Int)

internal class ReaderParagraph(val text: AnnotatedString, val verses: List<VerseRange>)

/**
 * Groups [verses] into paragraphs and builds each one's text: a superscript number, a narrow no-break space so the
 * number never ends a line on its own, then the verse; verses are joined by a space. The tap target covers the verse
 * number and text; highlight and the selection underline cover the text only, as in the preview.
 */
internal fun readerParagraphs(
    verses: List<SjVerse>,
    numberStyle: SpanStyle,
    highlighted: Set<Int>,
    highlightColor: Color,
    onVerseClick: ((Int) -> Unit)?,
): List<ReaderParagraph> {
    val groups = mutableListOf<MutableList<IndexedValue<SjVerse>>>()
    verses.withIndex().forEach { verse ->
        if (groups.isEmpty() || verse.value.startsParagraph) groups.add(mutableListOf())
        groups.last().add(verse)
    }
    return groups.map { group ->
        val ranges = mutableListOf<VerseRange>()
        val text = buildAnnotatedString {
            group.forEachIndexed { position, (index, verse) ->
                if (position > 0) append(' ')
                val link = onVerseClick?.let { click ->
                    pushLink(LinkAnnotation.Clickable(tag = "verse-$index", linkInteractionListener = { click(index) }))
                }
                withStyle(numberStyle) {
                    append(verse.number.toString())
                    append(NarrowNoBreakSpace)
                }
                val start = length
                if (index in highlighted) {
                    withStyle(SpanStyle(background = highlightColor)) { append(verse.text) }
                } else {
                    append(verse.text)
                }
                ranges += VerseRange(index, start, length)
                if (link != null) pop(link)
            }
        }
        ReaderParagraph(text, ranges)
    }
}

internal fun clampFontSize(fontSize: TextUnit): TextUnit {
    val min = SjReaderTextDefaults.MinFontSize.value
    val max = SjReaderTextDefaults.MaxFontSize.value
    return if (fontSize.isSp) fontSize.value.coerceIn(min, max).sp else SjReaderTextDefaults.FontSize
}

/** CSS `text-decoration: underline wavy`, 1.5 dp thick, under every line the range covers. */
private fun DrawScope.drawWavyUnderline(layout: TextLayoutResult, start: Int, end: Int, color: Color) {
    if (end <= start) return
    val text = layout.layoutInput.text.text
    val amplitude = 1.5.dp.toPx()
    val halfWave = 3.dp.toPx()
    val offset = 5.dp.toPx()
    val path = Path()
    for (line in layout.getLineForOffset(start)..layout.getLineForOffset(end - 1)) {
        var from = maxOf(start, layout.getLineStart(line))
        var to = min(end, layout.getLineEnd(line))
        while (from < to && text[from].isWhitespace()) from++
        while (to > from && text[to - 1].isWhitespace()) to--
        if (to <= from) continue
        val left = layout.getBoundingBox(from).left
        val right = layout.getBoundingBox(to - 1).right
        val y = layout.getLineBaseline(line) + offset
        path.moveTo(left, y)
        var x = left
        var up = true
        while (x < right) {
            val next = min(x + halfWave, right)
            path.quadraticTo((x + next) / 2, if (up) y - amplitude * 2 else y + amplitude * 2, next, y)
            x = next
            up = !up
        }
    }
    drawPath(path, color, style = Stroke(width = 1.5.dp.toPx()))
}

private const val NarrowNoBreakSpace = ' '

private val SampleVerses = listOf(
    SjVerse(105, buildAnnotatedString {
        append("Thy word ")
        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append("is") }
        append(" a lamp unto my feet, and a light unto my path.")
    }),
    SjVerse(106, buildAnnotatedString {
        append("I have sworn, and I will perform ")
        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append("it") }
        append(", that I will keep thy righteous judgments.")
    }),
    SjVerse(107, AnnotatedString("I am afflicted very much: quicken me, O LORD, according unto thy word.")),
    SjVerse(108, AnnotatedString("Accept, I beseech thee, the freewill offerings of my mouth, O LORD, and teach me thy judgments.")),
)

private val SampleVersesRu = listOf(
    SjVerse(105, AnnotatedString("Слово Твоё — светильник ноге моей и свет стезе моей.")),
    SjVerse(106, AnnotatedString("Я клялся хранить праведные суды Твои, и исполню.")),
    SjVerse(107, AnnotatedString("Сильно угнетён я, Господи; оживи меня по слову Твоему.")),
    SjVerse(108, AnnotatedString("Благоволи же, Господи, принять добровольную жертву уст моих, и судам Твоим научи меня.")),
)

@Preview
@Composable
private fun SjReaderTextPreview() {
    SjReaderText(
        translation = "Psalms · KJV",
        chapterTitle = "Psalm 119",
        verses = SampleVerses,
        credit = "Psalm 119:105–108, King James Version (public domain). Sample text for layout.",
        highlighted = setOf(1),
        selected = 3,
        onVerseClick = {},
    )
}

@Preview
@Composable
private fun SjReaderTextDarkPreview() {
    SjReaderText(
        translation = "Псалтирь · СП",
        chapterTitle = "Псалом 118",
        verses = SampleVersesRu.mapIndexed { i, verse -> if (i == 2) verse.copy(startsParagraph = true) else verse },
        credit = "Псалом 118:105–108, Синодальный перевод (общественное достояние). Образец для вёрстки.",
        fontSize = 24.sp,
        highlighted = setOf(1),
        selected = 3,
        colors = SjColors.Dark,
    )
}
