/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.extensions

import androidx.sqlite.db.SimpleSQLiteQuery

fun String.toSQLiteQuery(): SimpleSQLiteQuery = SimpleSQLiteQuery(this)

fun <T> List<T>.reversed(reversed: Boolean): List<T> = if (reversed) asReversed() else this
