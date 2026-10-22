package com.royaal.arch.mvi.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.royaal.arch.mvi.InnerEvent
import com.royaal.arch.mvi.MviReducer
import com.royaal.arch.mvi.ScreenSnapshot
import com.royaal.arch.mvi.ScreenState
import com.royaal.arch.mvi.SideEffect
import com.royaal.arch.mvi.SnapshotHandler
import com.royaal.arch.mvi.UserEvent
import com.royaal.arch.mvi.model.MviModel
import com.royaal.arch.mvi.model.intent.DefaultIntentExecutor
import com.royaal.arch.mvi.model.intent.scope.IntentScope
import com.royaal.arch.mvi.model.intent.scope.SubscriptionScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

abstract class MviViewModel<
    State : ScreenState,
    Event : UserEvent,
    InnEvent : InnerEvent,
    Effect : SideEffect,
    Snapshot : ScreenSnapshot,
    >(
    private val savedStateHandle: SavedStateHandle,
    initialState: State,
    private val reducer: MviReducer<State, InnEvent>,
    private val snapshotHandler: SnapshotHandler<State, Snapshot>,
    ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel(),
    MviModel<State, Event, Effect> {

    private val _state = MutableStateFlow(
        (savedStateHandle.get<Any>(SAVE_KEY) as? Snapshot)
            ?.runCatching { snapshotHandler.restore(this) }
            ?.getOrNull() ?: initialState,
    )
    override val state = _state.asStateFlow()

    private val intentExecutor = DefaultIntentExecutor(
        scope = viewModelScope,
        reducer = reducer,
        state = _state,
        ioDispatcher = ioDispatcher,
    )

    // A channel, not a SharedFlow: a SharedFlow drops what's emitted while nobody collects (the
    // screen is stopped, being recreated, or in a hidden tab), a channel keeps it until collected.
    private val _effects = Channel<Effect>(
        capacity = EFFECTS_BUFFER_SIZE,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val effects: Flow<Effect> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            _state.collect { state ->

                // only persist stable snapshots
                if (!state.shouldSaveSnapshot()) return@collect

                snapshotHandler.snapshot(state)?.let { snapshot ->
                    savedStateHandle[SAVE_KEY] = snapshot
                }
            }
        }
    }

    protected abstract fun onEvent(event: Event)

    /**
     *  Send event to inner events flow.
     *
     *  IMPORTANT: this is synchronous dispatch into onEvent.
     *  If async behavior is needed, use intent/launch inside onEvent.
     */
    override fun sendEvent(event: Event) = onEvent(event)

    /** Never suspends: if [EFFECTS_BUFFER_SIZE] effects are already waiting, the oldest is dropped. */
    protected suspend fun postEffect(effect: Effect) = _effects.send(effect)

    /**
     * Start an [IntentScope]. All methods within will be tied to this intent.
     *
     * @param key key that represent what event this intent is tied to. You may want to use it
     * as follows:
     * ```
     *  onEvent(event) {
     *      when (event) {
     *          // Will be cancelled on next event of same type.
     *          is SearchQuery -> intent(event::class) {
     *              ...
     *          }
     *
     *          // Will be cancelled on next event with same sectionId.
     *          is ShowSection -> intent(event) {
     *              getSection(event.sectionId)
     *              ...
     *          }
     *      }
     *  }
     * ```
     */
    protected fun intent(key: Any, block: suspend IntentScope<InnEvent>.() -> Unit) =
        intentExecutor.intent(key, block = block)

    /**
     *  Start an [IntentScope]. All methods within will be tied to this intent.
     *  When next intentLatest comes in, the previous one is canceled.
     */
    protected fun intentLatest(block: suspend IntentScope<InnEvent>.() -> Unit) = intentExecutor.intentLatest(block)

    /**
     *  An infinite subscription to something.
     *
     *  @param key key of subscription. When another enters, previous one is cancelled.
     */
    protected fun subscription(key: Any, block: suspend SubscriptionScope<InnEvent>.() -> Unit) =
        intentExecutor.subscription(key, block)

    companion object {
        private const val SAVE_KEY = "mvi_state"
        private const val EFFECTS_BUFFER_SIZE = 16
    }

    // STATE SAVE

    protected open fun State.shouldSaveSnapshot(): Boolean = true
}
