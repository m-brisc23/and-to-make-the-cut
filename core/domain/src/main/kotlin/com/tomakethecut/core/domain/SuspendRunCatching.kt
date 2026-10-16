package com.tomakethecut.core.domain

import kotlin.coroutines.cancellation.CancellationException

/**
 * Like [runCatching] but re-throws [CancellationException].
 *
 * The standard library version swallows cancellation, which breaks structured
 * concurrency: a cancelled `viewModelScope` job would be reported as a "failure"
 * and keep running. This is one of the most common coroutine bugs in the wild.
 */
suspend inline fun <T> suspendRunCatching(crossinline block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
