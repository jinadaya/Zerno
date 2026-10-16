package app.zerno

import androidx.compose.ui.window.ComposeUIViewController
import app.zerno.di.initKoin
import org.koin.mp.KoinPlatformTools
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    // SwiftUI can rebuild the controller; Koin must start only once per process.
    if (KoinPlatformTools.defaultContext().getOrNull() == null) initKoin()
    return ComposeUIViewController { App() }
}
