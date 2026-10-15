package com.royaal.arch.mvi.model.intent.scope

import com.royaal.arch.mvi.InnerEvent
import com.royaal.arch.mvi.model.intent.IntentExecutor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DefaultIntentScope<Event : InnerEvent>(
    key: Any?,
    executor: IntentExecutor<Event>,
    private val scope: CoroutineScope,
    private val generationProvider: () -> Long,
    // Dispatchers.IO is JVM/Android-only (internal on Kotlin/Native); Android callers that
    // want a dedicated IO pool can still pass Dispatchers.IO explicitly here.
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : IntentScope<Event>(key, executor) {

    override suspend fun emit(
        event: Event,
    ) {
        executor.emit(
            IntentExecutor.ScopedEvent(key, event, false, generationProvider())
        )
    }

    override fun launch(block: suspend IntentScope<Event>.() -> Unit): Job {
        return scope.launch {
            this@DefaultIntentScope.block()
        }
    }

    override suspend fun <T> io(block: suspend IntentScope<Event>.() -> T): T {
        return withContext(ioDispatcher) {
            this@DefaultIntentScope.block()
        }
    }
}