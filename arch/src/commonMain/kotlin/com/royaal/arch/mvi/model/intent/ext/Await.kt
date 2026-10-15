package com.royaal.arch.mvi.model.intent.ext

import com.royaal.arch.mvi.InnerEvent
import com.royaal.arch.mvi.ScreenState
import com.royaal.arch.mvi.model.intent.scope.IntentScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/*         --- Await ---

    When one intent needs to await for another one (or meet state).

    * awaitState - suspends until precondition isn't met
    * awaitIdle - suspends until intent with given key

 */

/**
 *  Ждёт state типа [S], удовлетворяющий [predicate].
 *  Возвращается мгновенно, если текущий state уже подходит.
 *
 *  @throws IllegalStateException если фактический state не является [S] —
 *         это ошибка программиста (неверный тип у awaitState)
 */
suspend inline fun <reified S : ScreenState, E : InnerEvent> IntentScope<E>.awaitState(
    timeout: Duration = 30.seconds,
    crossinline predicate: (S) -> Boolean,
): S = withTimeout(timeout) {
    executor.screenState.first { state ->
        check(state is S) {
            "awaitState expected ${S::class.simpleName}, got ${state::class.simpleName}"
        }
        predicate(state)
    } as S
}

/**
 *  Ждёт, пока интент с ключом [key] (включая всю цепочку latest-wins перезапусков) не завершится.
 *  Возвращается сразу, если по ключу ничего не запущено.
 *
 *  @throws [kotlinx.coroutines.TimeoutCancellationException] по истечении [timeout]
 */
suspend fun <E : InnerEvent> IntentScope<E>.awaitIdle(
    key: Any,
    timeout: Duration = 30.seconds,
) {
    require(key != this.key) { "Intent '$key' cannot await itself" }
    withTimeout(timeout) {
        executor.activeKeys.first { key !in it }
    }
}