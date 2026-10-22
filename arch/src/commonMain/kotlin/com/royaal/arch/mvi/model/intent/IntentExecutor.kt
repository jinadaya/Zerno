package com.royaal.arch.mvi.model.intent

import com.royaal.arch.mvi.InnerEvent
import com.royaal.arch.mvi.ScreenState
import com.royaal.arch.mvi.model.intent.scope.IntentScope
import com.royaal.arch.mvi.model.intent.scope.SubscriptionScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 *  This class is responsible for managing state of our intents.
 */
interface IntentExecutor<Event : InnerEvent> {

    /**
     *  All unhandled errors that may rise.
     */
    val unhandledErrors: SharedFlow<Throwable>

    /**
     *  Job's keys that are in use.
     */
    val activeKeys: StateFlow<Set<Any>>

    /**
     *  Current state of screen this intent is tied to.
     */
    val screenState: StateFlow<ScreenState>

    /**
     *  Builder function that creates an [IntentScope] tied to [key].
     *  If one started with same [key] - others will be canceled.
     */
    fun intent(key: Any, block: suspend IntentScope<Event>.() -> Unit)

    /**
     *  Builder function that creates a single [IntentScope] tied to the owning view model.
     *  If one started, previous will be canceled. Latest wins.
     */
    fun intentLatest(block: suspend IntentScope<Event>.() -> Unit)

    /**
     *  Builder function that creates an infinite subscription.
     */
    fun subscription(key: Any, block: suspend SubscriptionScope<Event>.() -> Unit)

    suspend fun emit(event: ScopedEvent<Event>)

    /**
     *  Class that is used for consistency in events.
     *
     *  @param key decides how to differ events
     *  @param event an actual event to emit to reducer
     *  @param isSubscription if set to true will always reach reducer
     *  @param generation to differ events from different intents
     */
    class ScopedEvent<Event : InnerEvent>(
        val key: Any?,
        val event: Event,
        val isSubscription: Boolean,
        val generation: Long,
    )
}
