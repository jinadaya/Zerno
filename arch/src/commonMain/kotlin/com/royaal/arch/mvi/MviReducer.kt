package com.royaal.arch.mvi

fun interface MviReducer<State : ScreenState, Event : InnerEvent> {

    /**
     * This method is used to produce new state by event and original state.
     *
     * These events are made by the owning view model and are not user events.
     */
    fun reduce(oldState: State, event: Event): State
}
