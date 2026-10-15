package com.royaal.arch.mvi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.royaal.arch.mvi.model.MviModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Handles [viewModel]'s one-off effects on the screen it's called from:
 * ```
 * EffectHandler(viewModel) { effect ->
 *     when (effect) {
 *         is MainSideEffect.NavigateTo -> navigator.navigateTo(effect.destination)
 *     }
 * }
 * ```
 * - Only while the screen is at least [minActiveState]. Effects posted while it's in the
 *   background or in a hidden tab wait in the model and are handled once it's back, instead of
 *   navigating from an invisible screen.
 * - On the main thread, as soon as they arrive (`Main.immediate`), so nothing is lost between an
 *   effect being taken from the model and handled, e.g. when the screen stops right then.
 * - Always with the latest [onEffect], so it can use values from the current composition.
 *
 * [onEffect] runs on the main thread and holds back the next effects until it returns; for
 * long-running work (e.g. a snackbar waiting to be dismissed) launch a coroutine from it.
 */
@Composable
fun <Ef : SideEffect> EffectHandler(
    viewModel: MviModel<*, *, Ef>,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    onEffect: (Ef) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnEffect by rememberUpdatedState(onEffect)
    LaunchedEffect(viewModel, lifecycleOwner, minActiveState) {
        lifecycleOwner.repeatOnLifecycle(minActiveState) {
            withContext(Dispatchers.Main.immediate) {
                viewModel.effects.collect { effect -> currentOnEffect(effect) }
            }
        }
    }
}
