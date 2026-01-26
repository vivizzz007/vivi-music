package com.music.vivi.extensions

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Launches a coroutine in the given [scope] to collect the flow.
 *
 * @param scope The CoroutineScope to launch the collection in.
 * @param action The action to perform for each emitted value.
 */
fun <T> Flow<T>.collect(scope: CoroutineScope, action: suspend (value: T) -> Unit) {
    scope.launch {
        collect(action)
    }
}

/**
 * Launches a coroutine in the given [scope] to collect the latest values of the flow.
 * Previous uncompleted actions are cancelled when a new value arrives.
 *
 * @param scope The CoroutineScope to launch the collection in.
 * @param action The action to perform for each emitted value.
 */
fun <T> Flow<T>.collectLatest(scope: CoroutineScope, action: suspend (value: T) -> Unit) {
    scope.launch {
        collectLatest(action)
    }
}

/**
 * A CoroutineExceptionHandler that silently ignores all exceptions.
 * Use with caution.
 */
val SilentHandler = CoroutineExceptionHandler { _, _ -> }
