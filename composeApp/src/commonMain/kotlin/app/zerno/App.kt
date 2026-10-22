package app.zerno

import androidx.compose.runtime.Composable
import app.zerno.designsystem.theme.SjTheme
import app.zerno.feature.reader.ui.ReaderRoute
import com.royaal.navigation.NavHost

/** Root composable shared by Android and iOS. Opens on the reader until the app shell (PR-01) adds the tabs. */
@Composable
fun App() {
    SjTheme {
        NavHost(startRoute = ReaderRoute)
    }
}
