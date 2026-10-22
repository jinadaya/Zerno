package app.zerno.designsystem.theme

import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.TweenSpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SjMotionTest {
    @Test
    fun defaultMotionUsesTheReadmeDurations() {
        assertEquals(250, assertIs<TweenSpec<Float>>(SjMotion.Default.spring<Float>()).durationMillis)
        assertEquals(600, assertIs<TweenSpec<Float>>(SjMotion.Default.progress<Float>()).durationMillis)
    }

    @Test
    fun reducedMotionSnaps() {
        assertIs<SnapSpec<Float>>(SjMotion.Reduced.spring<Float>())
        assertIs<SnapSpec<Float>>(SjMotion.Reduced.progress<Float>())
    }
}
