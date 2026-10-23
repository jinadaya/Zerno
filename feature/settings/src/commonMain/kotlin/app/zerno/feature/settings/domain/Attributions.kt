package app.zerno.feature.settings.domain

import app.zerno.entities.bible.TranslationId
import app.zerno.feature.settings.resources.Res
import app.zerno.feature.settings.resources.settings_attribution_kjv
import app.zerno.feature.settings.resources.settings_attribution_rst
import org.jetbrains.compose.resources.StringResource

/**
 * Credit and licence for the bundled translations. Downloaded ones will carry theirs from the catalog (BE-04); until
 * then a translation without a line shows its name only.
 */
internal val TranslationId.attribution: StringResource?
    get() = when (this) {
        Kjv -> Res.string.settings_attribution_kjv
        Rst -> Res.string.settings_attribution_rst
        else -> null
    }
