package app.zerno.feature.plans.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.intl.Locale
import app.zerno.designsystem.component.SjPlanList
import app.zerno.designsystem.component.SjPlanRow
import app.zerno.designsystem.theme.SjTheme
import app.zerno.feature.plans.api.PlanRunStatus
import app.zerno.feature.plans.domain.fraction
import app.zerno.feature.plans.resources.Res
import app.zerno.feature.plans.resources.plans_intro
import app.zerno.feature.plans.resources.plans_title
import com.royaal.arch.mvi.EffectHandler
import com.royaal.navigation.LocalNavigator
import org.jetbrains.compose.resources.stringResource

/** The Plans tab: every plan as a table of contents, the current one first. */
@Composable
fun PlansScreen(viewModel: PlansViewModel) {
    val navigator = LocalNavigator.current
    EffectHandler(viewModel) { effect ->
        when (effect) {
            is PlansEffect.OpenPlan -> navigator.navigate(PlanDetailRoute(effect.planId))
        }
    }
    val state by viewModel.state.collectAsState()
    val colors = SjTheme.colors
    val language = Locale.current.language
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.paper)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SjTheme.spacing.gutter, vertical = SjTheme.spacing.space6),
        verticalArrangement = Arrangement.spacedBy(SjTheme.spacing.space6),
    ) {
        BasicText(
            stringResource(Res.string.plans_title),
            Modifier.semantics { heading() },
            style = SjTheme.type.headline.copy(color = colors.ink),
        )
        BasicText(stringResource(Res.string.plans_intro), style = SjTheme.type.body.copy(color = colors.inkMuted))
        state.plans?.let { plans ->
            SjPlanList {
                plans.forEach { overview ->
                    val finished = overview.status == PlanRunStatus.Completed
                    SjPlanRow(
                        name = overview.plan.title.get(language),
                        caption = overview.caption(language),
                        kind = overview.plan.type.kind,
                        progress = overview.fraction,
                        finished = if (finished) "${overview.plan.length}/${overview.plan.length}" else null,
                        onClick = { viewModel.sendEvent(PlansEvent.PlanClicked(overview.plan.id)) },
                    )
                }
            }
        }
    }
}
