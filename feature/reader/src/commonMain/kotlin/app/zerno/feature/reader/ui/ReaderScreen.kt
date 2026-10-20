package app.zerno.feature.reader.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.zerno.designsystem.component.SjReaderText
import app.zerno.designsystem.component.SjReaderTextLayout
import app.zerno.designsystem.tokens.SjColors
import app.zerno.designsystem.tokens.SjRadius
import app.zerno.designsystem.tokens.SjSize
import app.zerno.designsystem.tokens.SjSpacing
import app.zerno.designsystem.type.SjFonts
import app.zerno.designsystem.type.rememberSjFonts
import app.zerno.feature.reader.domain.ReaderFontSize
import app.zerno.feature.reader.domain.ReaderTranslation
import app.zerno.feature.reader.resources.Res
import app.zerno.feature.reader.resources.reader_choose_chapter
import app.zerno.feature.reader.resources.reader_failed
import app.zerno.feature.reader.resources.reader_larger_text
import app.zerno.feature.reader.resources.reader_next_chapter
import app.zerno.feature.reader.resources.reader_previous_chapter
import app.zerno.feature.reader.resources.reader_settings
import app.zerno.feature.reader.resources.reader_smaller_text
import app.zerno.feature.reader.resources.reader_text_size
import app.zerno.feature.reader.resources.reader_translation
import com.royaal.arch.mvi.EffectHandler
import com.royaal.navigation.LocalNavigator
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Composable
fun ReaderScreen(viewModel: ReaderViewModel) {
    val navigator = LocalNavigator.current
    EffectHandler(viewModel) { effect ->
        when (effect) {
            ReaderEffect.OpenBookPicker -> navigator.navigate(BookPickerRoute)
        }
    }
    val state by viewModel.state.collectAsState()
    ReaderContent(state, viewModel::sendEvent)
}

@Composable
private fun ReaderContent(state: ReaderState, onEvent: (ReaderEvent) -> Unit) {
    val colors = readerColors()
    val fonts = rememberSjFonts()
    val page = state.page
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.readerPaper)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        ReaderTopBar(page, colors, fonts, onEvent)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                page != null -> ReaderPageContent(page, state.fontSize, colors, fonts, onEvent)
                state.failed -> BasicText(
                    text = stringResource(Res.string.reader_failed),
                    style = fonts.body(colors.inkMuted).copy(textAlign = TextAlign.Center),
                    modifier = Modifier.align(Alignment.Center).padding(SjSpacing.Default.space6),
                )
            }
            if (state.settingsOpen) {
                ReaderSettings(state, page?.translation, colors, fonts, onEvent)
            }
        }
    }
}

/** The chapter title opens the book picker; "Aa" opens text size and translation. */
@Composable
private fun ReaderTopBar(page: ReaderPage?, colors: SjColors, fonts: SjFonts, onEvent: (ReaderEvent) -> Unit) {
    val spacing = SjSpacing.Default
    val chooseChapter = stringResource(Res.string.reader_choose_chapter)
    val settings = stringResource(Res.string.reader_settings)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(BarHeight)
            .drawBehind {
                val stroke = SjSize.Default.hairline.toPx()
                drawLine(colors.rule, Offset(0f, size.height - stroke / 2), Offset(size.width, size.height - stroke / 2), stroke)
            }
            .padding(horizontal = spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .height(SjSize.Default.tapMin)
                .clickable(enabled = page != null, onClickLabel = chooseChapter, role = Role.Button) {
                    onEvent(ReaderEvent.BookPickerClicked)
                }
                .padding(horizontal = spacing.space2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.space2),
        ) {
            BasicText(page?.title.orEmpty(), style = fonts.body(colors.ink, FontWeight.SemiBold), maxLines = 1)
            if (page != null) Chevron(colors.ink)
        }
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(SjSize.Default.tapMin)
                .clickable(role = Role.Button) { onEvent(ReaderEvent.SettingsClicked) }
                .semantics { contentDescription = settings },
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                "Aa",
                style = TextStyle(fontFamily = fonts.serif, fontSize = 20.sp, lineHeight = 24.sp, color = colors.ink),
            )
        }
    }
}

/**
 * The chapter in a scrolling column, with links to the chapters around it. Opens at the page's saved verse once
 * the text is laid out, reports the verse at the top as the reader scrolls, and keeps that verse at the top when
 * the text size changes.
 */
@Composable
private fun ReaderPageContent(
    page: ReaderPage,
    fontSize: Int,
    colors: SjColors,
    fonts: SjFonts,
    onEvent: (ReaderEvent) -> Unit,
) {
    val spacing = SjSpacing.Default
    val scroll = remember(page.location) { ScrollState(0) }
    val textLayout = remember(page.location) { SjReaderTextLayout() }
    // Index of the verse at the top, as last reported. Not state: nothing draws from it.
    val topVerse = remember(page.location) { TopVerse() }
    val currentOnEvent by rememberUpdatedState(onEvent)
    val readingLine = with(LocalDensity.current) { ReadingLine.toPx() }
    val verses = remember(page.verses, colors) { page.verses.map { it.toSjVerse(colors.inkMuted) } }

    LaunchedEffect(page.location) {
        val saved = page.location.verse
        val index = if (saved == null) -1 else page.verses.indexOfFirst { it.number >= saved }
        if (index >= 0) {
            val top = snapshotFlow { textLayout.revision.let { textLayout.verseTop(index) } }.filterNotNull().first()
            scroll.scrollTo(top.roundToInt())
        }
        snapshotFlow { textLayout.revision.let { textLayout.verseAt(scroll.value + readingLine) } }
            .distinctUntilChanged()
            .collect { found ->
                topVerse.index = found
                currentOnEvent(ReaderEvent.TopVerseChanged(found?.let { page.verses[it].number }))
            }
    }

    // On a new text size, wait for the new layout and bring the verse that was at the top back there.
    val sizeChange = remember(page.location) { FontSizeChange(fontSize) }
    if (sizeChange.size != fontSize) {
        sizeChange.size = fontSize
        sizeChange.anchor = topVerse.index
        sizeChange.revision = Snapshot.withoutReadObservation { textLayout.revision }
    }
    LaunchedEffect(page.location, fontSize) {
        val anchor = sizeChange.anchor ?: return@LaunchedEffect
        val before = sizeChange.revision
        snapshotFlow { textLayout.revision }.first { it != before }
        val top = snapshotFlow { textLayout.revision.let { textLayout.verseTop(anchor) } }.filterNotNull().first()
        sizeChange.anchor = null
        scroll.scrollTo(top.roundToInt())
    }

    Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
        SjReaderText(
            translation = "${page.book.name} · ${page.translation.id.value.uppercase()}",
            chapterTitle = page.title,
            verses = verses,
            credit = "${page.title}, ${page.translation.name}",
            fontSize = fontSize.sp,
            layout = textLayout,
            colors = colors,
            fonts = fonts,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = spacing.space6, end = spacing.space6, bottom = spacing.space12),
            horizontalArrangement = Arrangement.spacedBy(spacing.space3),
        ) {
            page.previous?.let { previous ->
                OutlinePill(
                    text = "← ${previous.title}",
                    description = stringResource(Res.string.reader_previous_chapter, previous.title),
                    colors = colors,
                    fonts = fonts,
                    onClick = { onEvent(ReaderEvent.PreviousChapterClicked) },
                )
            }
            Spacer(Modifier.weight(1f))
            page.next?.let { next ->
                OutlinePill(
                    text = "${next.title} →",
                    description = stringResource(Res.string.reader_next_chapter, next.title),
                    colors = colors,
                    fonts = fonts,
                    onClick = { onEvent(ReaderEvent.NextChapterClicked) },
                )
            }
        }
    }
}

/** Text size and translation, over the page; a tap outside closes it. */
@Composable
private fun BoxScope.ReaderSettings(
    state: ReaderState,
    current: ReaderTranslation?,
    colors: SjColors,
    fonts: SjFonts,
    onEvent: (ReaderEvent) -> Unit,
) {
    val spacing = SjSpacing.Default
    Box(
        Modifier
            .fillMaxSize()
            .background(colors.scrim)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                onEvent(ReaderEvent.SettingsDismissed)
            },
    )
    Column(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(spacing.space4)
            .widthIn(max = 360.dp)
            .fillMaxWidth()
            .background(colors.card, RoundedCornerShape(SjRadius.Default.sticker))
            // Taps on the panel itself must not reach the scrim and close it.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .padding(spacing.space6),
        verticalArrangement = Arrangement.spacedBy(spacing.space4),
    ) {
        BasicText(
            stringResource(Res.string.reader_text_size).uppercase(),
            style = fonts.label(colors.inkMuted),
            modifier = Modifier.semantics { heading() },
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinePill(
                text = "A−",
                description = stringResource(Res.string.reader_smaller_text),
                enabled = state.fontSize > ReaderFontSize.MIN,
                colors = colors,
                fonts = fonts,
                onClick = { onEvent(ReaderEvent.FontSizeStepped(-1)) },
            )
            BasicText(
                state.fontSize.toString(),
                style = fonts.body(colors.ink, FontWeight.SemiBold).copy(textAlign = TextAlign.Center),
                modifier = Modifier.weight(1f),
            )
            OutlinePill(
                text = "A+",
                description = stringResource(Res.string.reader_larger_text),
                enabled = state.fontSize < ReaderFontSize.MAX,
                colors = colors,
                fonts = fonts,
                onClick = { onEvent(ReaderEvent.FontSizeStepped(+1)) },
            )
        }
        if (state.translations.size > 1) {
            BasicText(
                stringResource(Res.string.reader_translation).uppercase(),
                style = fonts.label(colors.inkMuted),
                modifier = Modifier.semantics { heading() },
            )
            Column {
                state.translations.forEach { translation ->
                    TranslationRow(translation, translation.id == current?.id, colors, fonts) {
                        onEvent(ReaderEvent.TranslationSelected(translation.id))
                    }
                }
            }
        }
    }
}

/** One translation: its tag and name; the current one is filled like a selected chip. */
@Composable
private fun TranslationRow(
    translation: ReaderTranslation,
    selected: Boolean,
    colors: SjColors,
    fonts: SjFonts,
    onClick: () -> Unit,
) {
    val spacing = SjSpacing.Default
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(SjSize.Default.tapMin)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { this.selected = selected },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.space3),
    ) {
        val tagShape = RoundedCornerShape(SjRadius.Default.pill)
        Box(
            modifier = Modifier
                .width(56.dp)
                .height(28.dp)
                .then(
                    if (selected) {
                        Modifier.background(colors.ink, tagShape)
                    } else {
                        Modifier.border(SjSize.Default.hairline, colors.ink, tagShape)
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(translation.id.value.uppercase(), style = fonts.label(if (selected) colors.onInk else colors.ink))
        }
        BasicText(
            translation.name,
            style = fonts.body(colors.ink, if (selected) FontWeight.SemiBold else FontWeight.Normal),
            maxLines = 1,
        )
    }
}

/** How far below the top edge a verse must start to count as the one being read. */
private val ReadingLine = 24.dp

private class TopVerse {
    var index: Int? = null
}

private class FontSizeChange(var size: Int) {
    var anchor: Int? = null
    var revision: Int = 0
}
