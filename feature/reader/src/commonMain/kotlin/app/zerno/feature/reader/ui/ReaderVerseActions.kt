package app.zerno.feature.reader.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.zerno.designsystem.component.SjReaderTextLayout
import app.zerno.designsystem.component.SjVerseAction
import app.zerno.designsystem.component.SjVerseActions
import app.zerno.designsystem.icon.SjIcons
import app.zerno.feature.reader.domain.kjvPassage
import app.zerno.feature.reader.resources.Res
import app.zerno.feature.reader.resources.reader_verse_copy
import app.zerno.feature.reader.resources.reader_verse_note
import app.zerno.feature.reader.resources.reader_verse_save
import app.zerno.feature.reader.resources.reader_verse_saved
import app.zerno.feature.reader.resources.reader_verse_share
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/**
 * Save, note, copy and share over the tapped verse. Call it inside the scrolling column, which the bar is placed
 * against: it sits just above the verse, or at the top of the text for a verse near it.
 */
@Composable
internal fun VerseActionBar(
    page: ReaderPage,
    state: ReaderState,
    layout: SjReaderTextLayout,
    onEvent: (ReaderEvent) -> Unit,
) {
    val index = state.selected ?: return
    val verse = page.verses.getOrNull(index) ?: return
    val saved = verse.kjvPassage(page.versification) in state.bookmarks
    val lift = with(LocalDensity.current) { BarLift.toPx() }
    val top = layout.verseTop(index)?.let { (it - lift).coerceAtLeast(0f).roundToInt() } ?: 0
    SjVerseActions(
        actions = listOf(
            SjVerseAction(
                label = stringResource(if (saved) Res.string.reader_verse_saved else Res.string.reader_verse_save),
                icon = SjIcons.Save,
                applied = saved,
                onClick = { onEvent(VerseEvent.SaveClicked) },
            ),
            SjVerseAction(stringResource(Res.string.reader_verse_note), SjIcons.Reflect) {
                onEvent(VerseEvent.NoteClicked)
            },
            SjVerseAction(stringResource(Res.string.reader_verse_copy), SjIcons.Copy) {
                onEvent(VerseEvent.CopyClicked)
            },
            SjVerseAction(stringResource(Res.string.reader_verse_share), SjIcons.Share) {
                onEvent(VerseEvent.ShareClicked)
            },
        ),
        onDismiss = { onEvent(VerseEvent.Dismissed) },
        offset = IntOffset(0, top),
    )
}

/** How far above the verse's first line the bar sits: its own height and a little air. */
private val BarLift = 64.dp
