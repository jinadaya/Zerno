package com.royaal.arch.mvi.model

import com.royaal.arch.mvi.ScreenState
import com.royaal.arch.mvi.SideEffect
import com.royaal.arch.mvi.UserEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 *  This is a plain Kotlin interface that can be used on other platforms.
 */
interface MviModel<
    State : ScreenState,
    Event : UserEvent,
    Effect : SideEffect,
    > {

    val state: StateFlow<State>

    /**
     * One-off effects: navigation, snackbars, etc. Each one is delivered once, to a single
     * collector; effects sent while nobody collects wait until someone does. Collect it with
     * [com.royaal.arch.mvi.EffectHandler].
     */
    val effects: Flow<Effect>

    fun sendEvent(event: Event)
}
