package app.zerno.feature.reader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.zerno.designsystem.tokens.SjColors
import app.zerno.designsystem.tokens.SjRadius
import app.zerno.designsystem.tokens.SjSize
import app.zerno.designsystem.tokens.SjSpacing
import app.zerno.designsystem.type.SjFonts
import app.zerno.designsystem.type.rememberSjFonts
import app.zerno.entities.bible.Book
import app.zerno.entities.bible.BookId
import app.zerno.entities.bible.ChapterRef
import app.zerno.entities.bible.Testament
import app.zerno.feature.reader.resources.Res
import app.zerno.feature.reader.resources.books_back
import app.zerno.feature.reader.resources.books_chapter
import app.zerno.feature.reader.resources.books_chapter_count
import app.zerno.feature.reader.resources.books_new_testament
import app.zerno.feature.reader.resources.books_old_testament
import app.zerno.feature.reader.resources.books_title
import com.royaal.arch.mvi.EffectHandler
import com.royaal.navigation.LocalNavigator
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun BookPickerScreen(viewModel: BookPickerViewModel) {
    val navigator = LocalNavigator.current
    EffectHandler(viewModel) { effect ->
        when (effect) {
            BookPickerEffect.Close -> navigator.back()
        }
    }
    val state by viewModel.state.collectAsState()
    BookPickerContent(state, viewModel::sendEvent)
}

@Composable
private fun BookPickerContent(state: BookPickerState, onEvent: (BookPickerEvent) -> Unit) {
    val colors = readerColors()
    val fonts = rememberSjFonts()
    val spacing = SjSpacing.Default
    val rows = remember(state.books, state.expanded) { pickerRows(state.books, state.expanded) }
    val listState = rememberLazyListState()

    // Bring the current book near the top once the books arrive.
    LaunchedEffect(state.books.isNotEmpty()) {
        val index = rows.indexOfFirst { it is PickerRow.BookRow && it.book.id == state.current?.book }
        if (index > 0) listState.scrollToItem(index - 1)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.paper)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        PickerTopBar(colors, fonts) { onEvent(BookPickerEvent.BackClicked) }
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            items(rows.size, key = { rows[it].key }) { index ->
                when (val row = rows[index]) {
                    is PickerRow.Header -> TestamentHeader(row.testament, colors, fonts)
                    is PickerRow.BookRow -> BookRow(row.book, row.expanded, colors, fonts) {
                        onEvent(BookPickerEvent.BookClicked(row.book.id))
                    }
                    is PickerRow.Chapters -> ChapterGrid(row.book, state.current, colors, fonts) { chapter ->
                        onEvent(BookPickerEvent.ChapterClicked(chapter))
                    }
                }
            }
            item(key = "end") { Spacer(Modifier.height(spacing.space12)) }
        }
    }
}

@Composable
private fun PickerTopBar(colors: SjColors, fonts: SjFonts, onBack: () -> Unit) {
    val spacing = SjSpacing.Default
    val back = stringResource(Res.string.books_back)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(BarHeight)
            .padding(horizontal = spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(SjSize.Default.tapMin)
                .clickable(role = Role.Button, onClick = onBack)
                .semantics { contentDescription = back },
            contentAlignment = Alignment.Center,
        ) {
            BackArrow(colors.ink)
        }
        BasicText(
            stringResource(Res.string.books_title),
            style = TextStyle(fontFamily = fonts.serif, fontSize = 24.sp, lineHeight = 28.sp, color = colors.ink),
            modifier = Modifier.padding(start = spacing.space2).semantics { heading() },
        )
    }
}

@Composable
private fun TestamentHeader(testament: Testament, colors: SjColors, fonts: SjFonts) {
    val spacing = SjSpacing.Default
    val title = when (testament) {
        Testament.Old -> stringResource(Res.string.books_old_testament)
        Testament.New -> stringResource(Res.string.books_new_testament)
    }
    BasicText(
        title.uppercase(),
        style = fonts.label(colors.inkMuted),
        modifier = Modifier
            .padding(start = spacing.space4, end = spacing.space4, top = spacing.space6, bottom = spacing.space2)
            .semantics { heading() },
    )
}

/** A list row: the book's name and its chapter count, a hairline `rule` under it. */
@Composable
private fun BookRow(book: Book, expanded: Boolean, colors: SjColors, fonts: SjFonts, onClick: () -> Unit) {
    val spacing = SjSpacing.Default
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = SjSize.Default.tapMin)
            .clickable(role = Role.Button, onClick = onClick)
            .drawBehind {
                val stroke = SjSize.Default.hairline.toPx()
                drawLine(colors.rule, Offset(0f, size.height - stroke / 2), Offset(size.width, size.height - stroke / 2), stroke)
            }
            .padding(horizontal = spacing.space4, vertical = spacing.space3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.space3),
    ) {
        BasicText(
            book.name,
            style = fonts.body(colors.ink, if (expanded) FontWeight.SemiBold else FontWeight.Normal),
            modifier = Modifier.weight(1f),
        )
        BasicText(
            pluralStringResource(Res.plurals.books_chapter_count, book.chapterCount, book.chapterCount),
            style = fonts.body(colors.inkMuted).copy(fontSize = 13.sp, lineHeight = 18.sp),
        )
        Chevron(colors.inkMuted, up = expanded)
    }
}

/** Chapters as square cells, [COLUMNS] to a row; the chapter open in the reader is filled with `ink`. */
@Composable
private fun ChapterGrid(
    book: Book,
    current: ChapterRef?,
    colors: SjColors,
    fonts: SjFonts,
    onChapter: (ChapterRef) -> Unit,
) {
    val spacing = SjSpacing.Default
    Column(
        modifier = Modifier.fillMaxWidth().padding(spacing.space4),
        verticalArrangement = Arrangement.spacedBy(spacing.space2),
    ) {
        (1..book.chapterCount).chunked(COLUMNS).forEach { chapters ->
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.space2)) {
                for (column in 0 until COLUMNS) {
                    val chapter = chapters.getOrNull(column)
                    if (chapter == null) {
                        Spacer(Modifier.weight(1f))
                        continue
                    }
                    val ref = ChapterRef(book.id, chapter)
                    val isCurrent = ref == current
                    val label = stringResource(Res.string.books_chapter, chapter)
                    val shape = RoundedCornerShape(SjRadius.Default.pill)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .heightIn(min = SjSize.Default.tapMin)
                            .then(
                                if (isCurrent) {
                                    Modifier.background(colors.ink, shape)
                                } else {
                                    Modifier.border(SjSize.Default.hairline, colors.rule, shape)
                                },
                            )
                            .clickable(role = Role.Button) { onChapter(ref) }
                            .semantics {
                                contentDescription = label
                                selected = isCurrent
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        BasicText(
                            chapter.toString(),
                            style = fonts.body(if (isCurrent) colors.onInk else colors.ink, FontWeight.Medium),
                        )
                    }
                }
            }
        }
    }
}

private sealed interface PickerRow {
    val key: String

    data class Header(val testament: Testament) : PickerRow {
        override val key get() = "header-$testament"
    }

    data class BookRow(val book: Book, val expanded: Boolean) : PickerRow {
        override val key get() = "book-${book.id}"
    }

    data class Chapters(val book: Book) : PickerRow {
        override val key get() = "chapters-${book.id}"
    }
}

/** Books under a header per testament, in the translation's order; the expanded book is followed by its grid. */
private fun pickerRows(books: List<Book>, expanded: BookId?): List<PickerRow> = buildList {
    var testament: Testament? = null
    for (book in books) {
        if (book.id.testament != testament) {
            testament = book.id.testament
            add(PickerRow.Header(testament))
        }
        val isExpanded = book.id == expanded
        add(PickerRow.BookRow(book, isExpanded))
        if (isExpanded) add(PickerRow.Chapters(book))
    }
}

private const val COLUMNS = 6
