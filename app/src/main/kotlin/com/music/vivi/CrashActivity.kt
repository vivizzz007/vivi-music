package com.music.vivi

import android.os.Bundle
import android.os.Environment
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.activity.ComponentActivity
import com.music.vivi.ui.crash.CrashPage
import java.io.File

class CrashActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CrashPage(getLatestLog())
        }
    }

    fun getLatestLog(): String {
        return try {
            val logDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "vivi/crash_log"
            )

            if (!logDir.exists() || !logDir.isDirectory) {
                return "Looks like there aren't any logs! Maybe try restarting the app."
            }
            val crashFiles = logDir.listFiles { file ->
                file.isFile && file.name.startsWith("crash_") && file.name.endsWith(".txt")
            }
            val newestFile = crashFiles?.maxByOrNull { it.lastModified() }
            if (newestFile != null && newestFile.exists()) {
                newestFile.readText()
            } else {
                "We've hit a snag reading the crash, you can always check ~/Download/vivi/crash_log"
            }
        } catch (e: Exception) {
            "We've hit a snag reading the crash, you can always check ~/Download/vivi/crash_log"
        }
    }
}