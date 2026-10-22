package app.zerno.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable

/**
 * The two motions from the design system README. Components ask for a spec here instead of writing their own, so
 * reduced motion is one switch: with [reduced] every spec is [snap] and the change lands at once.
 *
 * Nothing loops, and progress never runs backwards; that is up to the component, not the spec.
 */
@Immutable
class SjMotion private constructor(
    /** True when the system asks for reduced motion. Skip one-off flourishes, such as the seal drop-in, entirely. */
    val reduced: Boolean,
) {
    /** A state change (switch knob, selection, a sticker landing): 250 ms with a small overshoot. */
    fun <T> spring(): FiniteAnimationSpec<T> =
        if (reduced) snap() else tween(durationMillis = SpringMillis, easing = SpringEasing)

    /** A progress line or ring growing: 600 ms, easing out. */
    fun <T> progress(): FiniteAnimationSpec<T> =
        if (reduced) snap() else tween(durationMillis = ProgressMillis, easing = ProgressEasing)

    override fun equals(other: Any?): Boolean = other is SjMotion && other.reduced == reduced

    override fun hashCode(): Int = reduced.hashCode()

    override fun toString(): String = "SjMotion(reduced=$reduced)"

    companion object {
        val Default: SjMotion = SjMotion(reduced = false)
        val Reduced: SjMotion = SjMotion(reduced = true)

        private const val SpringMillis = 250
        private const val ProgressMillis = 600

        /** `cubic-bezier(.3, 1.4, .5, 1)` from `.sj-switch` in bundle.css: overshoots by about 10%, then settles. */
        private val SpringEasing = CubicBezierEasing(0.3f, 1.4f, 0.5f, 1f)
        private val ProgressEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    }
}
