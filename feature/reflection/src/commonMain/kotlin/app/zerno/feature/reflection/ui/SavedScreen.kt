package app.zerno.feature.reflection.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextOverflow
import app.zerno.designsystem.component.SjChipGroup
import app.zerno.designsystem.component.SjLinkButton
import app.zerno.designsystem.component.SjTag
import app.zerno.designsystem.foundation.sjHairline
import app.zerno.designsystem.theme.SjTheme
import app.zerno.entities.bible.BookNameLanguage
import app.zerno.feature.reader.api.ReaderRoute
import app.zerno.feature.reflection.domain.SavedFilter
import app.zerno.feature.reflection.domain.SavedItem
import app.zerno.feature.reflection.domain.SavedSort
import app.zerno.feature.reflection.domain.display
import app.zerno.feature.reflection.domain.shortText
import app.zerno.feature.reflection.resources.Res
import app.zerno.feature.reflection.resources.saved_back
import app.zerno.feature.reflection.resources.saved_delete
import app.zerno.feature.reflection.resources.saved_delete_question
import app.zerno.feature.reflection.resources.saved_edit
import app.zerno.feature.reflection.resources.saved_empty
import app.zerno.feature.reflection.resources.saved_filter
import app.zerno.feature.reflection.resources.saved_filter_all
import app.zerno.feature.reflection.resources.saved_filter_notes
import app.zerno.feature.reflection.resources.saved_filter_verses
import app.zerno.feature.reflection.resources.saved_intro
import app.zerno.feature.reflection.resources.saved_keep
import app.zerno.feature.reflection.resources.saved_note
import app.zerno.feature.reflection.resources.saved_open
import app.zerno.feature.reflection.resources.saved_sort
import app.zerno.feature.reflection.resources.saved_sort_book
import app.zerno.feature.reflection.resources.saved_sort_date
import app.zerno.feature.reflection.resources.saved_title
import app.zerno.feature.reflection.resources.saved_verse
import com.royaal.arch.mvi.EffectHandler
import com.royaal.navigation.LocalNavigator
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

/** Saved verses and reflections: filter, order, open, edit and delete. */
@Composable
fun SavedScreen(viewModel: SavedViewModel) {
    val navigator = LocalNavigator.current
    EffectHandler(viewModel) { effect ->
        when (effect) {
            SavedEffect.OpenReader -> navigator.navigate(ReaderRoute)
            is SavedEffect.EditReflection -> navigator.navigate(effect.route)
            SavedEffect.Close -> navigator.back()
        }
    }
    val state by viewModel.state.collectAsState()
    val colors = SjTheme.colors
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.paper)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SjTheme.spacing.gutter, vertical = SjTheme.spacing.space6),
        verticalArrangement = Arrangement.spacedBy(SjTheme.spacing.space6),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(SjTheme.spacing.space2)) {
            SjLinkButton(stringResource(Res.string.saved_back), { viewModel.sendEvent(SavedEvent.BackClicked) })
            BasicText(
                stringResource(Res.string.saved_title),
                Modifier.semantics { heading() },
                style = SjTheme.type.headline.copy(color = colors.ink),
            )
            BasicText(stringResource(Res.string.saved_intro), style = SjTheme.type.body.copy(color = colors.inkMuted))
        }
        Controls(state, viewModel::sendEvent)
        state.items?.let { items ->
            if (items.isEmpty()) {
                BasicText(
                    stringResource(Res.string.saved_empty),
                    style = SjTheme.type.body.copy(color = colors.inkMuted),
                )
            } else {
                Column {
                    items.forEach { item ->
                        SavedRow(item, confirming = item.key == state.confirming, viewModel::sendEvent)
                    }
                }
            }
        }
    }
}

@Composable
private fun Controls(state: SavedState, onEvent: (SavedEvent) -> Unit) {
    val filters = SavedFilter.entries
    val sorts = SavedSort.entries
    SjChipGroup(
        options = filters.map { stringResource(it.label) },
        selected = filters.indexOf(state.filter),
        onSelect = { onEvent(SavedEvent.FilterChosen(filters[it])) },
        label = stringResource(Res.string.saved_filter),
    )
    SjChipGroup(
        options = sorts.map { stringResource(it.label) },
        selected = sorts.indexOf(state.sort),
        onSelect = { onEvent(SavedEvent.SortChosen(sorts[it])) },
        label = stringResource(Res.string.saved_sort),
    )
}

/** One saved verse or reflection: reference, date, the note's text, and its actions. */
@Composable
private fun SavedRow(item: SavedItem, confirming: Boolean, onEvent: (SavedEvent) -> Unit) {
    val colors = SjTheme.colors
    val type = SjTheme.type
    val locale = Locale.current.language
    val language = BookNameLanguage.fromCode(locale)
    val zone = remember { TimeZone.currentSystemDefault() }
    Column(
        Modifier
            .fillMaxWidth()
            .sjHairline(colors.rule)
            .padding(vertical = SjTheme.spacing.space3),
        verticalArrangement = Arrangement.spacedBy(SjTheme.spacing.space2),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button) { onEvent(SavedEvent.ItemClicked(item.key)) }
                .semantics(mergeDescendants = true) {},
            verticalArrangement = Arrangement.spacedBy(SjTheme.spacing.space1),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(SjTheme.spacing.space2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SjTag(stringResource(if (item is SavedItem.Note) Res.string.saved_note else Res.string.saved_verse))
                BasicText(item.passage.display(language), style = type.bodyStrong.copy(color = colors.ink))
            }
            if (item is SavedItem.Note) {
                BasicText(
                    item.saved.reflection.text,
                    style = type.body.copy(color = colors.ink, fontStyle = FontStyle.Italic),
                    maxLines = PreviewLines,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BasicText(
                item.date.toLocalDateTime(zone).date.shortText(locale),
                style = type.caption.copy(color = colors.inkMuted),
            )
        }
        if (confirming) {
            BasicText(
                stringResource(Res.string.saved_delete_question),
                style = type.bodyStrong.copy(color = colors.ink),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(SjTheme.spacing.space4)) {
                SjLinkButton(stringResource(Res.string.saved_delete), { onEvent(SavedEvent.DeleteConfirmed) })
                SjLinkButton(stringResource(Res.string.saved_keep), { onEvent(SavedEvent.DeleteCancelled) })
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(SjTheme.spacing.space4)) {
                SjLinkButton(
                    stringResource(if (item is SavedItem.Note) Res.string.saved_edit else Res.string.saved_open),
                    { onEvent(SavedEvent.ItemClicked(item.key)) },
                )
                SjLinkButton(stringResource(Res.string.saved_delete), { onEvent(SavedEvent.DeleteClicked(item.key)) })
            }
        }
    }
}

private const val PreviewLines = 3

private val SavedFilter.label
    get() = when (this) {
        SavedFilter.All -> Res.string.saved_filter_all
        SavedFilter.Verses -> Res.string.saved_filter_verses
        SavedFilter.Notes -> Res.string.saved_filter_notes
    }

private val SavedSort.label
    get() = when (this) {
        SavedSort.Date -> Res.string.saved_sort_date
        SavedSort.Book -> Res.string.saved_sort_book
    }
