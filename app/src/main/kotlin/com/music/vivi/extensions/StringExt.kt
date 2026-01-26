package com.music.vivi.extensions

import androidx.sqlite.db.SimpleSQLiteQuery
import java.net.InetSocketAddress
import java.net.InetSocketAddress.createUnresolved

/**
 * NULL-safe extension to convert a String to an Enum value.
 *
 * @param defaultValue Returned if the string is null or not a valid enum constant.
 */
inline fun <reified T : Enum<T>> String?.toEnum(defaultValue: T): T = if (this == null) {
    defaultValue
} else {
    try {
        enumValueOf(this)
    } catch (e: IllegalArgumentException) {
        defaultValue
    }
}

/**
 * Wraps the string in a [SimpleSQLiteQuery].
 * Use this for simple raw queries.
 */
fun String.toSQLiteQuery(): SimpleSQLiteQuery = SimpleSQLiteQuery(this)

/**
 * Parses a "host:port" string into an [InetSocketAddress].
 * Only creates an unresolved address (does not trigger DNS).
 */
fun String.toInetSocketAddress(): InetSocketAddress {
    val (host, port) = split(":")
    return createUnresolved(host, port.toInt())
}
