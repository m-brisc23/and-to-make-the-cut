package com.tomakethecut.core.ui.state

import com.tomakethecut.core.domain.DataException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transformLatest

/** The three states every async section of a screen can be in. */
sealed interface LoadState<out T> {
    data object Loading : LoadState<Nothing>
    data class Error(val kind: ErrorKind) : LoadState<Nothing>
    data class Success<T>(val data: T) : LoadState<T>
}

/**
 * UI-facing error categories. The UI maps these to string resources; it never shows
 * `exception.message`, which is unlocalised and may leak internals.
 */
enum class ErrorKind { NETWORK, NOT_FOUND, SERVER, UNKNOWN }

fun Throwable.toErrorKind(): ErrorKind = when (this) {
    is DataException.Network -> ErrorKind.NETWORK
    is DataException.NotFound -> ErrorKind.NOT_FOUND
    is DataException.Server, is DataException.Parse -> ErrorKind.SERVER
    else -> ErrorKind.UNKNOWN
}

fun <T> Result<T>.toLoadState(): LoadState<T> =
    fold(onSuccess = { LoadState.Success(it) }, onFailure = { LoadState.Error(it.toErrorKind()) })

/**
 * Emits Loading then the result of [load]; re-runs whenever [retries] emits. `transformLatest`
 * cancels an in-flight load if the user hammers "retry".
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
fun <T> loadWithRetry(retries: Flow<Unit>, load: suspend () -> Result<T>): Flow<LoadState<T>> =
    retries
        .onStart { emit(Unit) }
        .transformLatest {
            emit(LoadState.Loading)
            emit(load().toLoadState())
        }
