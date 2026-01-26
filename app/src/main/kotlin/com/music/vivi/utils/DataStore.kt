package com.music.vivi.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.music.vivi.extensions.toEnum
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.properties.ReadOnlyProperty

/**
 * Extension property to access the "settings" DataStore instance.
 * Uses the `preferencesDataStore` delegate.
 */
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

operator fun <T> DataStore<Preferences>.get(key: Preferences.Key<T>): T? = runBlocking(Dispatchers.IO) {
    data.first()[key]
}

/**
 * Blocking access to a DataStore preference (safe for synchronous access in non-suspend scopes).
 * WARNING: This uses [runBlocking] and performs IO on the calling thread. Use with caution.
 *
 * @param key The preference key.
 * @param defaultValue Value to return if key is missing.
 * @return The stored value or [defaultValue].
 */
fun <T> DataStore<Preferences>.get(key: Preferences.Key<T>, defaultValue: T): T = runBlocking(Dispatchers.IO) {
    data.first()[key] ?: defaultValue
}

fun <T> preference(context: Context, key: Preferences.Key<T>, defaultValue: T) =
    ReadOnlyProperty<Any?, T> { _, _ -> context.dataStore[key] ?: defaultValue }

inline fun <reified T : Enum<T>> enumPreference(context: Context, key: Preferences.Key<String>, defaultValue: T) =
    ReadOnlyProperty<Any?, T> { _, _ -> context.dataStore[key].toEnum(defaultValue) }

/**
 * A Composable helper that remembers a preference state and persists changes to DataStore.
 *
 * This provides a standard Compose `MutableState` interface to a persistent preference.
 * Changes to the state are asynchronously written to disk.
 *
 * ## Usage
 * ```kotlin
 * var showLyrics by rememberPreference(ShowLyricsKey, true)
 * Checkbox(checked = showLyrics, onCheckedChange = { showLyrics = it })
 * ```
 *
 * @param key The DataStore preference key.
 * @param defaultValue The initial value if the key doesn't exist.
 * @return A [MutableState] backed by DataStore.
 */
@Composable
fun <T> rememberPreference(key: Preferences.Key<T>, defaultValue: T): MutableState<T> {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val state = remember { mutableStateOf(defaultValue) }
    // ... implementation ...
    LaunchedEffect(key) {
        context.dataStore.data
            .map { it[key] ?: defaultValue }
            .distinctUntilChanged()
            .collect { state.value = it }
    }

    return remember {
        object : MutableState<T> {
            override var value: T
                get() = state.value
                set(value) {
                    state.value = value
                    coroutineScope.launch {
                        context.dataStore.edit {
                            it[key] = value
                        }
                    }
                }

            override fun component1() = value

            override fun component2(): (T) -> Unit = { value = it }
        }
    }
}

@Composable
inline fun <reified T : Enum<T>> rememberEnumPreference(
    key: Preferences.Key<String>,
    defaultValue: T,
): MutableState<T> {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val state = remember { mutableStateOf(defaultValue) }

    LaunchedEffect(key) {
        context.dataStore.data
            .map { it[key].toEnum(defaultValue = defaultValue) }
            .distinctUntilChanged()
            .collect { state.value = it }
    }

    return remember {
        object : MutableState<T> {
            override var value: T
                get() = state.value
                set(value) {
                    state.value = value
                    coroutineScope.launch {
                        context.dataStore.edit {
                            it[key] = value.name
                        }
                    }
                }

            override fun component1() = value

            override fun component2(): (T) -> Unit = { value = it }
        }
    }
}
