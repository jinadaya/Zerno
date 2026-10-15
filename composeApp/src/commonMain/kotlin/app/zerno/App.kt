package app.zerno

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/** Root composable shared by Android and iOS. Blank until the design system and navigation land. */
@Composable
fun App() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
    )
}
