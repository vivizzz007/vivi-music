package com.music.vivi.update.experiment

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.music.vivi.CrashActivity
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CrashLogHandler(private val context: Context, private val defaultHandler: Thread.UncaughtExceptionHandler?) :
    Thread.UncaughtExceptionHandler {

    companion object {
        private const val TAG = "CrashLogHandler"
        private const val CRASH_LOG_DIR = "crash_logs"
        private const val MAX_LOG_FILES = 10 // Keep only last 10 crash logs

        fun initialize(context: Context) {
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            val crashHandler = CrashLogHandler(context, defaultHandler)
            Thread.setDefaultUncaughtExceptionHandler(crashHandler)
        }

        fun getCrashLogsDir(context: Context): File = File(context.getExternalFilesDir(null), CRASH_LOG_DIR).apply {
            if (!exists()) mkdirs()
        }

        fun getAllCrashLogs(context: Context): List<File> {
            val crashDir = getCrashLogsDir(context)
            return crashDir.listFiles()?.filter { it.name.endsWith(".txt") }
                ?.sortedByDescending { it.lastModified() } ?: emptyList()
        }

        fun deleteCrashLog(file: File): Boolean = try {
            file.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting crash log: ${e.message}")
            false
        }

        fun clearAllCrashLogs(context: Context): Boolean = try {
            val crashDir = getCrashLogsDir(context)
            crashDir.listFiles()?.forEach { it.delete() }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing crash logs: ${e.message}")
            false
        }
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            var crashLog = getCrashLog(thread, throwable)

            saveCrashLog(crashLog)

            // Save logcat
            saveLogcat()

            // Clean up old logs
            cleanupOldLogs()

            val intent = Intent(context, CrashActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            intent.putExtra("CrashData", crashLog)
            context.startActivity(intent)
            Thread.sleep(1000)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving crash log: ${e.message}", e)
        } finally {
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun saveCrashLog(crashLog: String) {
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
            val crashFile = File(getCrashLogsDir(context), "crash_$timestamp.txt")

            FileOutputStream(crashFile).use { fos ->
                PrintWriter(fos).use { writer ->
                    writer.print(crashLog)
                }
            }

            Log.i(TAG, "Crash log saved to: ${crashFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error writing crash log: ${e.message}", e)
        }
    }

    private fun getCrashLog(thread: Thread, throwable: Throwable): String {
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
            return StringWriter().use { stringwr ->
                PrintWriter(stringwr).use { writer ->
                    writer.println("=== CRASH REPORT ===")
                    writer.println("Timestamp: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
                    writer.println()

                    writer.println("=== DEVICE INFO ===")
                    writer.println("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
                    writer.println("Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                    writer.println("Brand: ${Build.BRAND}")
                    writer.println("Product: ${Build.PRODUCT}")
                    writer.println("CPU ABI: ${Build.SUPPORTED_ABIS.joinToString(", ")}")
                    writer.println()

                    writer.println("=== APP INFO ===")
                    writer.println("Package: ${context.packageName}")
                    try {
                        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                        val versionCode = androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(packageInfo)
                        writer.println("Version: ${packageInfo.versionName} ($versionCode)")
                    } catch (e: Exception) {
                        writer.println("Version: Unknown")
                    }
                    writer.println()

                    writer.println("=== THREAD INFO ===")
                    writer.println("Thread Name: ${thread.name}")
                    writer.println("Thread ID: ${thread.id}")
                    writer.println()

                    writer.println("=== EXCEPTION ===")
                    writer.println("Exception Type: ${throwable.javaClass.name}")
                    writer.println("Message: ${throwable.message ?: "No message"}")
                    writer.println()

                    writer.println("=== STACK TRACE ===")
                    throwable.printStackTrace(writer) // Simplified - no need for another StringWriter
                    writer.println()

                    var cause = throwable.cause
                    var causeLevel = 1
                    while (cause != null && causeLevel <= 10) { // Limit cause chain depth
                        writer.println()
                        writer.println("=== CAUSED BY ($causeLevel) ===")
                        writer.println("Exception Type: ${cause.javaClass.name}")
                        writer.println("Message: ${cause.message ?: "No message"}")
                        writer.println("Stack Trace:")
                        cause.printStackTrace(writer)
                        cause = cause.cause
                        causeLevel++
                    }
                }
                stringwr.toString()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating crash log: ${e.message}", e)
            return "Error creating crash log: ${e.message}"
        }
    }

    private fun saveLogcat() {
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
            val logcatFile = File(getCrashLogsDir(context), "logcat_$timestamp.txt")

            // Execute logcat command to get recent logs
            val process = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-v", "time", "*:V"))

            FileOutputStream(logcatFile).use { fos ->
                process.inputStream.copyTo(fos)
            }

            // Also save a filtered version with only app logs
            val appLogcatFile = File(getCrashLogsDir(context), "app_logcat_$timestamp.txt")
            val appProcess = Runtime.getRuntime().exec(
                arrayOf("logcat", "-d", "-v", "time", "--pid=${android.os.Process.myPid()}")
            )

            FileOutputStream(appLogcatFile).use { fos ->
                appProcess.inputStream.copyTo(fos)
            }

            Log.i(TAG, "Logcat saved to: ${logcatFile.absolutePath}")
            Log.i(TAG, "App logcat saved to: ${appLogcatFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving logcat: ${e.message}", e)
        }
    }

    private fun cleanupOldLogs() {
        try {
            val crashDir = getCrashLogsDir(context)
            val files = crashDir.listFiles()?.sortedByDescending { it.lastModified() } ?: return

            if (files.size > MAX_LOG_FILES * 3) { // 3 files per crash (crash, logcat, app_logcat)
                files.drop(MAX_LOG_FILES * 3).forEach { file ->
                    file.delete()
                    Log.d(TAG, "Deleted old crash log: ${file.name}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up old logs: ${e.message}", e)
        }
    }
}
