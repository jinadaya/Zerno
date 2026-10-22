package app.zerno.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled
import platform.UIKit.UIAccessibilityReduceMotionStatusDidChangeNotification

/** Settings › Accessibility › Motion › Reduce Motion. */
@Composable
internal actual fun systemReducedMotion(): Boolean {
    var reduced by remember { mutableStateOf(UIAccessibilityIsReduceMotionEnabled()) }
    DisposableEffect(Unit) {
        val center = NSNotificationCenter.defaultCenter
        val observer = center.addObserverForName(
            name = UIAccessibilityReduceMotionStatusDidChangeNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
        ) { _ -> reduced = UIAccessibilityIsReduceMotionEnabled() }
        onDispose { center.removeObserver(observer) }
    }
    return reduced
}
