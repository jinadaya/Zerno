package com.royaal.arch.mvi.model.intent.ext

import com.royaal.arch.mvi.InnerEvent
import com.royaal.arch.mvi.model.intent.scope.IntentScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 *  Launches a separate coroutine on IO Dispatcher.
 *
 *  Similar to calling:
 *  ```
 *  launch {
 *      io {
 *          ...
 *      }
 *  }
 *  ```
 */
inline fun <Event : InnerEvent> IntentScope<Event>.launchIo(crossinline block: suspend () -> Unit) = launch {
    io {
        block()
    }
}

/**
 *  Switches current dispatcher to [dispatcher] within [IntentScope]
 *
 *  Similar to calling:
 *  ```
 *  launch {
 *      withContext(dispatcher) {
 *          ...
 *      }
 *  }
 *  ```
 */
inline fun IntentScope<*>.switch(dispatcher: CoroutineDispatcher, crossinline block: suspend () -> Unit) = launch {
    withContext(dispatcher) {
        block()
    }
}

inline fun IntentScope<*>.onMainDispatcher(crossinline block: suspend () -> Unit) = switch(Dispatchers.Main, block)

inline fun IntentScope<*>.onDefaultDispatcher(crossinline block: suspend () -> Unit) =
    switch(Dispatchers.Default, block)
