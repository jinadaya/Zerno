package app.zerno.feature.plans.di

import app.zerno.data.db.user.UserDb
import app.zerno.feature.plans.api.Plans
import app.zerno.feature.plans.api.PlansRoute
import app.zerno.feature.plans.data.BundledPlans
import app.zerno.feature.plans.data.PlanCatalog
import app.zerno.feature.plans.data.PlanJourneyStore
import app.zerno.feature.plans.data.PlansService
import app.zerno.feature.plans.data.RemotePlans
import app.zerno.feature.plans.ui.PlanDetailRoute
import app.zerno.feature.plans.ui.PlanDetailScreen
import app.zerno.feature.plans.ui.PlanDetailViewModel
import app.zerno.feature.plans.ui.PlansScreen
import app.zerno.feature.plans.ui.PlansViewModel
import com.royaal.navigation.screen
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

/**
 * The feature's Koin module, loaded by `initKoin()` in `:composeApp`. Needs `UserDb`, the backend `HttpClient`,
 * `Clock` and `TimeZoneProvider`.
 */
val plansModule = module {
    single { BundledPlans() }
    single { RemotePlans(get()) }
    single { PlanCatalog(get(), get(), get<UserDb>().appMetaDao()) }
    single { PlanJourneyStore(get<UserDb>().appMetaDao()) }
    single<Plans> { PlansService(get(), get(), get(), get()) }
    viewModelOf(::PlansViewModel)
    viewModelOf(::PlanDetailViewModel)

    // The Plans tab's root, so it has no back link.
    screen<PlansRoute> { PlansScreen(koinViewModel()) }
    screen<PlanDetailRoute>(showBottomBar = false) { route ->
        PlanDetailScreen(koinViewModel(key = route.toString()) { parametersOf(route) })
    }
}
