package com.royaal.arch.mvi.model.intent.scope

import com.royaal.arch.mvi.InnerEvent
import com.royaal.arch.mvi.model.intent.IntentExecutor
import kotlinx.coroutines.Job

/**
 * DSL scope for consistent work within [MviModel][com.royaal.arch.mvi.model.MviModel]
 */
abstract class IntentScope<Event : InnerEvent>(
    internal val key: Any?,
    /**
     *  Avoid using plain executor. Prefer using [MviModel][com.royaal.arch.mvi.model.MviModel] methods
     *
     *  @see [com.royaal.arch.mvi.model.MviModel]
     */
    val executor: IntentExecutor<Event>,
) {
    /**
     * Emit [event] into reducer.
     */
    abstract suspend fun emit(event: Event)

    /**
     * Start parallel work that is tied to this [IntentScope]
     */
    abstract fun launch(block: suspend IntentScope<Event>.() -> Unit): Job

    /**
     * Start parallel io work that is tied to this [IntentScope]
     */
    abstract suspend fun <T> io(block: suspend IntentScope<Event>.() -> T): T
}
