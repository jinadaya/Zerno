package app.zerno.feature.onboarding.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import app.zerno.designsystem.component.SjPlanKind
import app.zerno.designsystem.component.forPlan
import app.zerno.designsystem.foundation.StickerSurface
import app.zerno.designsystem.mascot.Mascot
import app.zerno.designsystem.mascot.MascotCharacter
import app.zerno.designsystem.mascot.MascotPose
import app.zerno.designsystem.theme.SjTheme
import app.zerno.entities.plan.PlanType
import app.zerno.entities.plan.ReadingPlan
import app.zerno.feature.onboarding.domain.averageMinutes
import app.zerno.feature.onboarding.resources.Res
import app.zerno.feature.onboarding.resources.onboarding_recommendation_caption
import app.zerno.feature.onboarding.resources.onboarding_recommendation_days
import app.zerno.feature.onboarding.resources.onboarding_recommendation_minutes
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** Heading of a step. */
@Composable
internal fun Title(text: String) {
    BasicText(
        text,
        Modifier.semantics { heading() },
        style = SjTheme.type.headline.copy(color = SjTheme.colors.ink),
    )
}

/** Supporting text under a heading. */
@Composable
internal fun Body(text: String) {
    BasicText(text, style = SjTheme.type.body.copy(color = SjTheme.colors.inkMuted))
}

/** The recommended plan on its colour: Woolly reading, the title, length and minutes a day, and the description. */
@Composable
internal fun PlanCard(plan: ReadingPlan) {
    val language = Locale.current.language
    val type = SjTheme.type
    val days = pluralStringResource(Res.plurals.onboarding_recommendation_days, plan.length, plan.length)
    val minutes = stringResource(Res.string.onboarding_recommendation_minutes, plan.averageMinutes)
    StickerSurface(SjTheme.colors.forPlan(plan.type.kind), Modifier.fillMaxWidth()) {
        Mascot(MascotCharacter.Woolly, MascotPose.Reading, size = 96.dp)
        BasicText(
            plan.title.get(language),
            Modifier.semantics { heading() },
            style = type.titleItalic.copy(color = SjTheme.contentColor),
        )
        BasicText(
            stringResource(Res.string.onboarding_recommendation_caption, days, minutes),
            style = type.caption.copy(color = SjTheme.contentColor),
        )
        BasicText(plan.description.get(language), style = type.body.copy(color = SjTheme.contentColor))
    }
}

private val PlanType.kind: SjPlanKind
    get() = when (this) {
        PlanType.Introductory -> SjPlanKind.Introductory
        PlanType.Sequential, PlanType.WholeBible -> SjPlanKind.Sequential
        PlanType.Thematic -> SjPlanKind.Thematic
    }
