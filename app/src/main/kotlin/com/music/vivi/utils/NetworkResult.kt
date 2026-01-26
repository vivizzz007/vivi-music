package com.music.vivi.utils

/**
 * A sealed class representing the state of a network operation.
 * Used to propagate data and error states from ViewModels to UI.
 *
 * @param T The type of data being returned.
 */
sealed class NetworkResult<T>(val data: T? = null, val message: String? = null) {
    class Success<T>(data: T) : NetworkResult<T>(data)
    class Error<T>(message: String, data: T? = null) : NetworkResult<T>(data, message)
    class Loading<T> : NetworkResult<T>()
}
