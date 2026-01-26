package com.music.vivi.extensions

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Operator overload to easily create child file path.
 * Usage: `val logs = cacheDir / "logs"`
 */
operator fun File.div(child: String): File = File(this, child)

/**
 * Wraps this input stream into a [ZipInputStream].
 */
fun InputStream.zipInputStream(): ZipInputStream = ZipInputStream(this)

/**
 * Wraps this output stream into a [ZipOutputStream].
 */
fun OutputStream.zipOutputStream(): ZipOutputStream = ZipOutputStream(this)
