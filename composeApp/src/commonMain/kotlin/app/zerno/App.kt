package app.zerno

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import app.zerno.data.prefs.AppPreferences
import app.zerno.debug.DebugMenuRoute
import app.zerno.debug.isDebugBuild
import app.zerno.designsystem.component.SjBottomNav
import app.zerno.designsystem.component.SjNavItem
import app.zerno.designsystem.component.SjTag
import app.zerno.designsystem.foundation.sjMinTapTarget
import app.zerno.designsystem.theme.SjTheme
import app.zerno.feature.onboarding.api.OnboardingRoute
import app.zerno.feature.settings.api.AppLanguage
import app.zerno.locale.LocalAppLocale
import com.royaal.navigation.LocalNavigator
import com.royaal.navigation.NavHost
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/**
 * Root composable shared by Android and iOS: the app shell. Each of [AppTabs] keeps its own back stack while another
 * is shown, and `SjBottomNav` sits under the screens unless the top one hides it (the reader does).
 * The UI language follows the device unless settings override it, and the first run picks the translation from
 * the device language. Debug builds float a small "debug" tag in the bottom corner that opens the debug menu.
 */
@Composable
fun App() {
    AppLanguageScope {
        SjTheme {
            NavHost(
                tabs = AppTabs.map { it.route },
                scaffold = { showBottomBar, content -> AppShell(showBottomBar, content) },
            )
        }
    }
}

@Composable
private fun AppLanguageScope(content: @Composable () -> Unit) {
    val appLanguage = koinInject<AppLanguage>()
    // Read before any override, so it is the device's own language.
    val deviceLanguage = Locale.current.language
    remember(appLanguage) { appLanguage.chooseFirstRunTranslation(deviceLanguage) }
    val uiLanguage by appLanguage.uiLanguage.collectAsState(appLanguage.currentUiLanguage)
    CompositionLocalProvider(LocalAppLocale provides uiLanguage) {
        key(uiLanguage) { content() }
    }
}

@Composable
private fun AppShell(showBottomBar: Boolean, content: @Composable () -> Unit) {
    OnboardingGate()
    Column(Modifier.fillMaxSize().background(SjTheme.colors.paper)) {
        // The bar pads itself above the system navigation bar, so screens above it don't pad for that again.
        val insets = if (showBottomBar) Modifier.consumeWindowInsets(WindowInsets.navigationBars) else Modifier
        Box(Modifier.weight(1f).fillMaxWidth().then(insets)) {
            content()
            if (isDebugBuild) DebugMenuButton(Modifier.align(Alignment.BottomEnd))
        }
        if (showBottomBar) AppBottomNav()
    }
}

/**
 * Opens onboarding over Today until it is done, once per app start: backing out of it lands on Today, and it comes
 * back on the next start.
 */
@Composable
private fun OnboardingGate() {
    val navigator = LocalNavigator.current
    val prefs = koinInject<AppPreferences>()
    var opened by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!opened && !prefs.prefs.value.onboardingDone) navigator.navigate(OnboardingRoute)
        opened = true
    }
}

@Composable
private fun AppBottomNav() {
    val navigator = LocalNavigator.current
    val items = AppTabs.map { SjNavItem(stringResource(it.label), it.icon) }
    SjBottomNav(
        items = items,
        selected = AppTabs.indexOfFirst { it.route == navigator.currentTab },
        onSelect = { navigator.selectTab(AppTabs[it].route) },
    )
}

@Composable
private fun DebugMenuButton(modifier: Modifier) {
    val navigator = LocalNavigator.current
    SjTag(
        "debug",
        modifier
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(SjTheme.spacing.space2)
            .sjMinTapTarget()
            .clickable { navigator.navigate(DebugMenuRoute) },
    )
}
