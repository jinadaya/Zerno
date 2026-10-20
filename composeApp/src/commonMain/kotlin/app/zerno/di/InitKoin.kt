package app.zerno.di

import app.zerno.feature.account.di.accountModule
import app.zerno.feature.gamification.di.gamificationModule
import app.zerno.feature.onboarding.di.onboardingModule
import app.zerno.feature.plans.di.plansModule
import app.zerno.feature.progress.di.progressModule
import app.zerno.feature.reader.di.readerModule
import app.zerno.feature.reflection.di.reflectionModule
import app.zerno.feature.settings.di.settingsModule
import app.zerno.feature.today.di.todayModule
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration

/** Every feature's Koin module. A new feature adds its `xModule` here. */
val featureModules: List<Module> = listOf(
    onboardingModule,
    todayModule,
    readerModule,
    plansModule,
    progressModule,
    gamificationModule,
    reflectionModule,
    accountModule,
    settingsModule,
)

/**
 * Starts Koin with all app modules. Called once per process: from `ZernoApplication` on Android
 * and from `MainViewController` on iOS. [platform] adds platform bindings such as the Android context.
 */
fun initKoin(platform: KoinAppDeclaration = {}): KoinApplication = startKoin {
    platform()
    modules(listOf(platformDataModule) + featureModules)
}
