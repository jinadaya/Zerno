package app.zerno.feature.plans.ui

import androidx.compose.runtime.Composable
import app.zerno.designsystem.component.SjPlanKind
import app.zerno.entities.bible.BookNameLanguage
import app.zerno.entities.bible.PassageRef
import app.zerno.entities.bible.Versification
import app.zerno.entities.bible.format
import app.zerno.entities.bible.mapVersification
import app.zerno.entities.plan.PlanType
import app.zerno.feature.plans.api.PlanOverview
import app.zerno.feature.plans.api.PlanRunStatus
import app.zerno.feature.plans.domain.minutesPerDay
import app.zerno.feature.plans.domain.shortText
import app.zerno.feature.plans.resources.Res
import app.zerno.feature.plans.resources.plans_row_finished
import app.zerno.feature.plans.resources.plans_row_new
import app.zerno.feature.plans.resources.plans_row_paused
import app.zerno.feature.plans.resources.plans_row_progress
import org.jetbrains.compose.resources.stringResource

internal val PlanType.kind: SjPlanKind
    get() = when (this) {
        PlanType.Introductory -> SjPlanKind.Introductory
        PlanType.Sequential, PlanType.WholeBible -> SjPlanKind.Sequential
        PlanType.Thematic -> SjPlanKind.Thematic
    }

/** The row caption: where the user is, how long a day takes, or when the plan was finished. Never "behind". */
@Composable
internal fun PlanOverview.caption(language: String): String {
    val length = plan.length
    val day = nextDay ?: length
    return when (status) {
        PlanRunStatus.Active -> stringResource(Res.string.plans_row_progress, day, length, plan.minutesPerDay)

        PlanRunStatus.Paused -> stringResource(Res.string.plans_row_paused, day, length)

        PlanRunStatus.NotStarted -> stringResource(Res.string.plans_row_new, length, plan.minutesPerDay)

        PlanRunStatus.Completed ->
            stringResource(Res.string.plans_row_finished, completedOn?.shortText(language).orEmpty())
    }
}

/** A plan passage (KJV numbering) as the UI language writes it; Russian uses the Synodal numbering. */
internal fun PassageRef.display(language: String): String = when (BookNameLanguage.fromCode(language)) {
    BookNameLanguage.English -> format(BookNameLanguage.English)

    BookNameLanguage.Russian ->
        mapVersification(Versification.English, Versification.Russian).format(BookNameLanguage.Russian)
}
