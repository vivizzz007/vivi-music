/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.utils

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.music.vivi.BuildConfig
import com.music.vivi.ui.screens.CrashActivity
import timber.log.Timber
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.LocalDateTime
import kotlin.system.exitProcess

class CrashHandler private constructor(
    private val applicationContext: Context
) : Thread.UncaughtExceptionHandler {

    private val defaultHandler: Thread.UncaughtExceptionHandler? =
        Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val crashLog = buildCrashLog(thread, throwable)
            Timber.e(throwable, "App crashed on thread: ${thread.name}")

            // Automatically register and save crash log to Download/vivimusic/repairtool/crashlog-<date>.txt
            val savedFile = saveCrashLogToFile(applicationContext, crashLog)

            // Launch crash activity
            val intent = Intent(applicationContext, CrashActivity::class.java).apply {
                putExtra(EXTRA_CRASH_LOG, crashLog)
                putExtra(EXTRA_CRASH_FILE, savedFile)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            applicationContext.startActivity(intent)

            // Kill the current process
            android.os.Process.killProcess(android.os.Process.myPid())
            exitProcess(1)
        } catch (e: Exception) {
            // If we fail to handle the crash, fall back to default handler
            Timber.e(e, "Error handling crash")
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun buildCrashLog(thread: Thread, throwable: Throwable): String {
        val stackTrace = StringWriter().apply {
            throwable.printStackTrace(PrintWriter(this))
        }.toString()

        return buildString {
            appendLine("==================================================")
            appendLine("             VIVIMUSIC CRASH REPORT               ")
            appendLine("==================================================")
            appendLine("Timestamp: ${LocalDateTime.now()}")
            appendLine("Thread: ${thread.name} (ID: ${thread.id})")
            appendLine("App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("Flavor: ${BuildConfig.FLAVOR}")
            appendLine()
            appendLine("----------------- DEVICE & OS -----------------")
            appendLine("Manufacturer: ${Build.MANUFACTURER}")
            appendLine("Brand: ${Build.BRAND}")
            appendLine("Model: ${Build.MODEL} (${Build.DEVICE})")
            appendLine("Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Supported ABIs: ${Build.SUPPORTED_ABIS.joinToString(", ")}")
            appendLine()
            appendLine("----------------- STACKTRACE -------------------")
            append(stackTrace)
            appendLine()
            appendLine("================== END OF REPORT =================")
        }
    }

    private fun saveCrashLogToFile(context: Context, crashLog: String): String? {
        return try {
            val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
            val timestamp = java.time.LocalDateTime.now().format(formatter)
            val fileName = "crashlog-$timestamp.txt"
            val savedPath = DiagnosticLogCollector.saveToVivimusicFolder(context, "crashlog", fileName, crashLog)
            if (savedPath != null) fileName else null
        } catch (e: Exception) {
            Timber.e(e, "Failed to save crash log to vivimusic folder")
            null
        }
    }

    companion object {
        const val EXTRA_CRASH_LOG = "crash_log"
        const val EXTRA_CRASH_FILE = "crash_file"

        fun install(context: Context) {
            val handler = CrashHandler(context.applicationContext)
            Thread.setDefaultUncaughtExceptionHandler(handler)
            Timber.d("CrashHandler installed")
        }
    }
}
