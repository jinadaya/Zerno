package com.royaal.arch.mvi.model.intent.scope

import com.royaal.arch.mvi.InnerEvent
import com.royaal.arch.mvi.model.intent.IntentExecutor
import com.royaal.arch.mvi.model.intent.ext.PolicyScope
import com.royaal.arch.mvi.model.intent.ext.policy
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SubscriptionScope<Event : InnerEvent>(
    key: Any?,
    executor: IntentExecutor<Event>,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : IntentScope<Event>(key, executor) {

    /**
     *  Binds flow to emitting data into reducer with given mapping.
     *
     *  If [policy] is provided, failures thrown by [flow] (or its collection)
     *  are handled per [PolicyScope] semantics, and a retry re-subscribes to [flow]
     *  from scratch. Without [policy], an unhandled exception cancels
     *  this subscription silently, same as before.
     */
    fun <T> bind(flow: Flow<T>, policy: (PolicyScope<Event>.() -> Unit)? = null, mapper: (T) -> Event) = launch {
        val collect: suspend IntentScope<Event>.() -> Unit = {
            flow.collect { data -> emit(mapper(data)) }
        }
        if (policy != null) {
            policy(policy, collect)
        } else {
            collect()
        }
    }

    override suspend fun emit(event: Event) {
        executor.emit(
            IntentExecutor.ScopedEvent(key, event, true, Long.MAX_VALUE),
        )
    }

    override fun launch(block: suspend IntentScope<Event>.() -> Unit): Job = scope.launch {
        this@SubscriptionScope.block()
    }

    override suspend fun <T> io(block: suspend IntentScope<Event>.() -> T): T = withContext(ioDispatcher) {
        this@SubscriptionScope.block()
    }
}
