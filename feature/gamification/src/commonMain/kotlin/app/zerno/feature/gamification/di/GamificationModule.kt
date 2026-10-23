package app.zerno.feature.gamification.di

import app.zerno.data.db.user.UserDb
import app.zerno.feature.gamification.api.AchievementTexts
import app.zerno.feature.gamification.api.AchievementsRoute
import app.zerno.feature.gamification.api.GamificationEvents
import app.zerno.feature.gamification.api.GamificationState
import app.zerno.feature.gamification.data.BundledAchievementCatalog
import app.zerno.feature.gamification.data.GamificationRepository
import app.zerno.feature.gamification.data.GamificationService
import app.zerno.feature.gamification.data.SeenSealsStore
import app.zerno.feature.gamification.ui.AchievementsScreen
import app.zerno.feature.gamification.ui.AchievementsViewModel
import app.zerno.feature.gamification.ui.ResourceAchievementTexts
import com.royaal.navigation.screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.binds
import org.koin.dsl.module

/** The feature's Koin module, loaded by `initKoin()` in `:composeApp`. Needs `UserDb` from the platform data module. */
val gamificationModule = module {
    single { BundledAchievementCatalog() }
    single { GamificationRepository(get<UserDb>().gamificationDao()) }
    single {
        val catalog = get<BundledAchievementCatalog>()
        GamificationService(
            repository = get(),
            loadCatalog = catalog::load,
            clock = get(),
            zones = get(),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
        )
    } binds arrayOf(GamificationEvents::class, GamificationState::class)
    single<AchievementTexts> { ResourceAchievementTexts() }

    single { SeenSealsStore(get<UserDb>().appMetaDao()) }
    viewModelOf(::AchievementsViewModel)

    screen<AchievementsRoute>(showBottomBar = false) { AchievementsScreen(koinViewModel()) }
}
