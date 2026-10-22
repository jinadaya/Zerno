package com.royaal.arch.mvi.model.intent

import com.royaal.arch.mvi.InnerEvent
import com.royaal.arch.mvi.MviReducer
import com.royaal.arch.mvi.ScreenState
import com.royaal.arch.mvi.model.intent.scope.DefaultIntentScope
import com.royaal.arch.mvi.model.intent.scope.IntentScope
import com.royaal.arch.mvi.model.intent.scope.SubscriptionScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch

/**
 *  Works as follows:
 *
 *  - [intentLatest] - last one wins. All previous emits will not reach [reducer].
 *  - [intent] - same as [intentLatest], but separated by key.
 *  - [subscription] - may last for infinite amount of time in separate coroutine. Will not
 *  be affected by [intent] or [intentLatest].
 */
class DefaultIntentExecutor<State : ScreenState, Event : InnerEvent>(
    private val scope: CoroutineScope,
    private val reducer: MviReducer<State, Event>,
    private val state: MutableStateFlow<State>,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : IntentExecutor<Event> {

    // java.util.concurrent (AtomicLong/AtomicReference/ConcurrentHashMap) is JVM-only and
    // unavailable on iOS. MutableStateFlow.update/getAndUpdate/updateAndGet are lock-free CAS
    // loops too, and already used below for _activeKeys - same pattern, just applied everywhere.
    private val latestGeneration = MutableStateFlow(0L)
    private val latestJob = MutableStateFlow<Job?>(null)
    private val keyedGenerations = MutableStateFlow<Map<Any, Long>>(emptyMap())
    private val keyedJobs = MutableStateFlow<Map<Any, Job>>(emptyMap())
    private val subscriptions = MutableStateFlow<Map<Any, Job>>(emptyMap())

    private val queue = Channel<IntentExecutor.ScopedEvent<Event>>(16)

    private val _activeKeys = MutableStateFlow<Set<Any>>(emptySet())
    override val activeKeys: StateFlow<Set<Any>> = _activeKeys.asStateFlow()

    private val _unhandledErrors = MutableSharedFlow<Throwable>()
    override val unhandledErrors: SharedFlow<Throwable> = _unhandledErrors

    override val screenState: StateFlow<State>
        get() = state

    init {
        scope.launch {
            for (event in queue) {
                val isValid = (
                    if (event.key == null) {
                        event.generation == latestGeneration.value
                    } else {
                        event.generation == keyedGenerations.value[event.key]
                    }
                    ) || event.isSubscription
                if (isValid) {
                    try {
                        state.emit(reducer.reduce(state.value, event.event))
                    } catch (e: Throwable) {
                        _unhandledErrors.emit(e)
                    }
                }
            }
        }
    }

    override fun intent(key: Any, block: suspend IntentScope<Event>.() -> Unit) {
        val lastGen = keyedGenerations.updateAndGet { it + (key to (it[key] ?: 0L) + 1) }.getValue(key)

        val job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                DefaultIntentScope(
                    scope = this,
                    executor = this@DefaultIntentExecutor,
                    generationProvider = { lastGen },
                    key = key,
                    ioDispatcher = ioDispatcher,
                ).block()
            } catch (e: Throwable) {
                _unhandledErrors.emit(e)
            }
        }

        // Сначала кладём новый, потом отменяем старый — ключ не мерцает
        val old = keyedJobs.getAndUpdate { it + (key to job) }[key]
        _activeKeys.update { it + key }
        old?.cancel()

        job.invokeOnCompletion {
            // удаляем, только если это всё ещё наш Job
            val wasPresent = keyedJobs.getAndUpdate {
                if (it[key] === job) it - key else it
            }[key] === job
            if (wasPresent) {
                _activeKeys.update { it - key }
            }
        }
        job.start()
    }

    override fun intentLatest(block: suspend IntentScope<Event>.() -> Unit) {
        val lastGen = latestGeneration.updateAndGet { it + 1 }
        latestJob.value?.cancel()

        latestJob.value = scope.launch {
            try {
                DefaultIntentScope(
                    scope = this,
                    executor = this@DefaultIntentExecutor,
                    generationProvider = { lastGen },
                    key = null,
                    ioDispatcher = ioDispatcher,
                ).block()
            } catch (e: Throwable) {
                _unhandledErrors.emit(e)
            }
        }
    }

    override fun subscription(key: Any, block: suspend SubscriptionScope<Event>.() -> Unit) {
        subscriptions.getAndUpdate { it - key }[key]?.cancel()
        val job = scope.launch {
            try {
                SubscriptionScope(
                    scope = this,
                    executor = this@DefaultIntentExecutor,
                    key = key,
                    ioDispatcher = ioDispatcher,
                ).block()
            } catch (e: Throwable) {
                _unhandledErrors.emit(e)
            }
        }
        subscriptions.update { it + (key to job) }
        job.invokeOnCompletion {
            subscriptions.update { if (it[key] === job) it - key else it }
        }
    }

    override suspend fun emit(event: IntentExecutor.ScopedEvent<Event>) {
        val isSubscription = event.isSubscription
        val isKeyed = event.key != null
        val isValid = (
            if (!isKeyed) {
                event.generation == latestGeneration.value
            } else {
                event.generation == keyedGenerations.value[event.key]
            }
            ) || isSubscription
        if (isValid) queue.send(event)
    }
}
