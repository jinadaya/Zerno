package app.zerno.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import app.zerno.designsystem.component.SjLinkButton
import app.zerno.designsystem.component.SjOutlineButton
import app.zerno.designsystem.component.SjProgressLine
import app.zerno.designsystem.theme.SjTheme
import app.zerno.feature.settings.domain.TranslationOffer
import app.zerno.feature.settings.resources.Res
import app.zerno.feature.settings.resources.settings_delete
import app.zerno.feature.settings.resources.settings_download
import app.zerno.feature.settings.resources.settings_download_failed
import app.zerno.feature.settings.resources.settings_download_size
import app.zerno.feature.settings.resources.settings_downloading
import app.zerno.feature.settings.resources.settings_downloads
import app.zerno.feature.settings.resources.settings_downloads_offline
import app.zerno.feature.settings.resources.settings_update
import org.jetbrains.compose.resources.stringResource

/** Translations from the backend catalog: download, follow the progress, update or remove. */
@Composable
internal fun Downloads(state: SettingsState, onEvent: (SettingsEvent) -> Unit) {
    if (state.offers.isEmpty() && !state.offersUnavailable) return
    Section(stringResource(Res.string.settings_downloads)) {
        state.offers.forEach { offer ->
            Offer(offer, state.downloads[offer.translation.id], state.failedDownload == offer.translation.id, onEvent)
        }
        if (state.offersUnavailable && state.offers.isEmpty()) {
            BasicText(
                stringResource(Res.string.settings_downloads_offline),
                style = SjTheme.type.body.copy(color = SjTheme.colors.inkMuted),
            )
        }
    }
}

@Composable
private fun Offer(offer: TranslationOffer, progress: Float?, failed: Boolean, onEvent: (SettingsEvent) -> Unit) {
    val colors = SjTheme.colors
    val type = SjTheme.type
    val translation = offer.translation
    Column(verticalArrangement = Arrangement.spacedBy(SjTheme.spacing.space2)) {
        Column(Modifier.semantics(mergeDescendants = true) {}) {
            BasicText(translation.name, style = type.bodyStrong.copy(color = colors.ink))
            BasicText(
                stringResource(Res.string.settings_download_size, megabytes(translation.size)),
                style = type.caption.copy(color = colors.inkMuted),
            )
        }
        when {
            progress != null -> SjProgressLine(
                progress,
                contentDescription = stringResource(Res.string.settings_downloading, translation.name),
                endLabel = "${(progress * PERCENT).toInt()}%",
            )

            offer.installed -> Row(
                horizontalArrangement = Arrangement.spacedBy(SjTheme.spacing.space3),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (offer.updateAvailable) {
                    SjOutlineButton(
                        stringResource(Res.string.settings_update),
                        { onEvent(SettingsEvent.DownloadClicked(translation.id)) },
                    )
                }
                SjLinkButton(
                    stringResource(Res.string.settings_delete),
                    { onEvent(SettingsEvent.DeleteClicked(translation.id)) },
                )
            }

            else -> SjOutlineButton(
                stringResource(Res.string.settings_download),
                { onEvent(SettingsEvent.DownloadClicked(translation.id)) },
            )
        }
        if (failed) {
            BasicText(
                stringResource(Res.string.settings_download_failed),
                style = type.body.copy(color = colors.inkMuted),
            )
        }
    }
}

private const val PERCENT = 100
private const val BYTES_PER_MEGABYTE = 1_000_000L

/** Whole megabytes, at least 1, so a small file never reads "0 MB". */
private fun megabytes(bytes: Long): Int =
    ((bytes + BYTES_PER_MEGABYTE / 2) / BYTES_PER_MEGABYTE).coerceAtLeast(1).toInt()
