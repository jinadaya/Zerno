package com.royaal.arch.mvi.model.intent.ext

import com.royaal.arch.mvi.InnerEvent
import com.royaal.arch.mvi.log.Logger
import com.royaal.arch.mvi.model.intent.scope.IntentScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration

/*                          --- POLICY ---
    The policy is used to set up environment around block of executed code.

    It uses PolicyConfig to configure the environment.
     * retries - amount of times code will try to retry the execution after error was thrown.
     * retryPredicate - a condition when we should stop retrying or not. Invoked before next try after first failure.
     * beforeRetry - invoked before each retry.
     * backoff - amount of time to wait before next retry. Can be configured with max backoff and backoff factor.
     * onError - invoked on error when no retry needed. For logging or reacting on errors. Is executed in IntentScope.
     * timeoutDuration - timeout before execution.
     * onEach - invoked on each retry
 */

data class PolicyConfig<Event : InnerEvent>(
    val retries: Int = 0,
    val retryPredicate: (error: Throwable) -> Boolean = { true },
    val beforeRetry: IntentScope<Event>.(error: Throwable) -> Unit = {
        Logger.warn("IntentScope Policy", "Retry caused by $it")
    },
    val backoff: Duration = Duration.ZERO,
    val backoffFactor: Double = 1.0,
    val backoffMax: Duration = backoff,
    val onError: (suspend IntentScope<Event>.(Throwable) -> Unit)? = null,
    val timeoutDuration: Duration? = null,
    val onEach: (() -> Unit)? = null,
)

private class PolicyBuilder<Event : InnerEvent> : PolicyScope<Event> {
    var config = PolicyConfig<Event>()
        private set

    override fun retry(
        amount: Int,
        retryPredicate: ((error: Throwable) -> Boolean)?,
        beforeRetry: (IntentScope<Event>.(error: Throwable) -> Unit)?
    ) {
        config = config.copy(
            retries = amount,
            retryPredicate = retryPredicate ?: config.retryPredicate,
            beforeRetry = beforeRetry ?: config.beforeRetry,
        )
    }

    override fun backoff(
        timeout: Duration,
        factor: Double,
        max: Duration
    ) {
        config = config.copy(
            backoff = timeout,
            backoffMax = max,
            backoffFactor = factor,
        )
    }

    override fun catch(onError: IntentScope<Event>.(Throwable) -> Unit) {
        config = config.copy(
            onError = onError,
        )
    }

    override fun timeout(duration: Duration) {
        config = config.copy(
            timeoutDuration = duration,
        )
    }

    override fun each(onEach: () -> Unit) {
        config = config.copy(
            onEach = onEach,
        )
    }
}

interface PolicyScope<Event : InnerEvent> {
    fun retry(
        amount: Int,
        retryPredicate: ((error: Throwable) -> Boolean)? = null,
        beforeRetry: (IntentScope<Event>.(error: Throwable) -> Unit)? = null
    )

    fun each(onEach: () -> Unit)
    fun backoff(timeout: Duration, factor: Double = 1.0, max: Duration = timeout)
    fun catch(onError: IntentScope<Event>.(Throwable) -> Unit)
    fun timeout(duration: Duration)
}

suspend fun <Event : InnerEvent> IntentScope<Event>.policy(
    config: PolicyScope<Event>.() -> Unit,
    block: suspend IntentScope<Event>.() -> Unit,
) {
    val cfg = PolicyBuilder<Event>().apply(config).config
    var attempt = 0
    var currentBackoff = cfg.backoff

    while (true) {
        try {
            if (cfg.timeoutDuration != null) {
                withTimeout(cfg.timeoutDuration) { block() }
            } else {
                block()
            }
            return
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            val shouldRetry = attempt < cfg.retries && cfg.retryPredicate(e)
            if (!shouldRetry) {
                cfg.onError?.invoke(this, e)
                return
            }
            cfg.beforeRetry(this, e)
            attempt++
            if (currentBackoff > Duration.ZERO) {
                delay(currentBackoff)
                currentBackoff = minOf(cfg.backoffMax, currentBackoff * cfg.backoffFactor)
            }
        } finally {
            cfg.onEach?.invoke()
        }
    }
}

fun <Event : InnerEvent> PolicyScope<Event>.retryForever(
    retryPredicate: (Throwable) -> Boolean = { true },
) = retry(Int.MAX_VALUE, retryPredicate)