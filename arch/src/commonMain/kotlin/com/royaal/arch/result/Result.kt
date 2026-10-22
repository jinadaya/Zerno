package com.royaal.arch.result

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

sealed interface Result<out T> {
    data class Success<out T>(val value: T) : Result<T>
    data class Error<T>(val error: Throwable) : Result<T>
}

inline fun <T> Result<T>.onSuccess(block: (T) -> Unit) = apply {
    if (this is Result.Success) block(this.value)
}

inline fun <T> Result<T>.onFailure(block: (Throwable) -> Unit) = apply {
    if (this is Result.Error) block(this.error)
}

@OptIn(ExperimentalContracts::class)
val Result<*>.succeeded: Boolean
    get() {
        contract {
            returns(true) implies (this@succeeded is Result.Success)
        }
        return this is Result.Success
    }

@OptIn(ExperimentalContracts::class)
val Result<*>.failed: Boolean
    get() {
        contract {
            returns(true) implies (this@failed is Result.Error)
        }
        return this is Result.Error
    }

fun <T> T?.wrapAsResult(): Result<T> = if (this == null) {
    Result.Error(IllegalStateException())
} else {
    Result.Success(this)
}
