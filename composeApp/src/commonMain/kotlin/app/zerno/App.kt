package app.zerno

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.zerno.debug.DebugMenuRoute
import app.zerno.debug.isDebugBuild
import app.zerno.designsystem.component.SjTag
import app.zerno.designsystem.foundation.sjMinTapTarget
import app.zerno.designsystem.theme.SjTheme
import app.zerno.feature.reader.api.ReaderRoute
import com.royaal.navigation.LocalNavigator
import com.royaal.navigation.NavHost

/**
 * Root composable shared by Android and iOS. Opens on the reader until the app shell (PR-01) adds the tabs.
 * Debug builds float a small "debug" tag in the bottom corner that opens the debug menu.
 */
@Composable
fun App() {
    SjTheme {
        NavHost(
            tabs = listOf(ReaderRoute),
            scaffold = { _, content ->
                Box(Modifier.fillMaxSize()) {
                    content()
                    if (isDebugBuild) DebugMenuButton(Modifier.align(Alignment.BottomEnd))
                }
            },
        )
    }
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
