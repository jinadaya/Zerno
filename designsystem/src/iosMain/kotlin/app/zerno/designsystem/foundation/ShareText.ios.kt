package app.zerno.designsystem.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.popoverPresentationController

@Composable
actual fun rememberShareText(): (text: String) -> Unit = remember {
    { text ->
        val presenter = topViewController()
        if (presenter != null) {
            val sheet = UIActivityViewController(activityItems = listOf(text), applicationActivities = null)
            // iPad shows the sheet as a popover, which needs an anchor.
            sheet.popoverPresentationController?.sourceView = presenter.view
            presenter.presentViewController(sheet, animated = true, completion = null)
        }
    }
}

/** The controller on top of the key window, which can present a sheet. */
@Suppress("DEPRECATION")
private fun topViewController(): UIViewController? {
    var top = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return null
    while (true) {
        top = top.presentedViewController ?: return top
    }
}
