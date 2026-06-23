package com.music.vivi.vivimusic.updater.downloadmanager

import android.content.Context
import android.os.Environment
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.music.vivi.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream
import com.music.vivi.constants.AutoBackupEnabledKey
import com.music.vivi.constants.AutoBackupBeforeUpdateKey
import com.music.vivi.utils.dataStore
import com.music.vivi.utils.get
import com.music.vivi.utils.AutoBackupHelper
import timber.log.Timber

class UpdateDownloadWorker(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val apkUrl = inputData.getString("apk_url") ?: return@withContext Result.failure()
        val version = inputData.getString("version") ?: "unknown"
        val fileSize = inputData.getString("file_size") ?: ""

        try {
            val autoBackupEnabled = context.dataStore[AutoBackupEnabledKey] ?: true
            val backupBeforeUpdate = context.dataStore[AutoBackupBeforeUpdateKey] ?: true
            if (autoBackupEnabled && backupBeforeUpdate) {
                Timber.tag("UpdateDownloadWorker").d("Auto backup enabled. Creating backup before update.")
                AutoBackupHelper.performBackup(context, "before_update")
            }
        } catch (e: Exception) {
            Timber.tag("UpdateDownloadWorker").e(e, "Failed to perform auto backup before update")
        }

        DownloadNotificationManager.showDownloadStarting(version, fileSize)

        try {
            val url = URL(apkUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                DownloadNotificationManager.showDownloadFailed(
                    version,
                    context.getString(R.string.server_error, connection.responseCode)
                )
                return@withContext Result.failure()
            }

            val fileLength = connection.contentLength
            val inputStream = connection.inputStream

            val downloadDir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "vivi_updates"
            )
            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
            }

            val isZip = apkUrl.contains("nightly.link") || apkUrl.endsWith(".zip")
            val downloadFile = if (isZip) File(downloadDir, "vivi_temp.zip") else File(downloadDir, "vivi.apk")
            val outputStream = FileOutputStream(downloadFile)

            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalBytesRead: Long = 0

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                if (isStopped) {
                    outputStream.close()
                    inputStream.close()
                    connection.disconnect()
                    if (downloadFile.exists()) {
                        downloadFile.delete()
                    }
                    return@withContext Result.retry()
                }

                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead

                if (fileLength > 0) {
                    val progress = (totalBytesRead.toFloat() / fileLength.toFloat() * 100).toInt()
                    // Update notification
                    DownloadNotificationManager.updateDownloadProgress(progress, version)
                    // Update WorkManager progress for UI observation
                    setProgress(workDataOf("progress" to progress.toFloat() / 100f))
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()
            connection.disconnect()

            val finalFile = if (isZip) {
                val targetApkFile = File(downloadDir, "vivi.apk")
                var extracted = false
                try {
                    ZipInputStream(downloadFile.inputStream()).use { zis ->
                        var entry = zis.nextEntry
                        while (entry != null) {
                            if (!entry.isDirectory && entry.name.endsWith(".apk")) {
                                FileOutputStream(targetApkFile).use { fos ->
                                    zis.copyTo(fos)
                                }
                                extracted = true
                                break
                            }
                            entry = zis.nextEntry
                        }
                    }
                } catch (e: Exception) {
                    if (downloadFile.exists()) downloadFile.delete()
                    DownloadNotificationManager.showDownloadFailed(
                        version,
                        e.message ?: "Failed to extract zip file"
                    )
                    return@withContext Result.failure()
                } finally {
                    if (downloadFile.exists()) {
                        downloadFile.delete()
                    }
                }
                if (!extracted) {
                    DownloadNotificationManager.showDownloadFailed(
                        version,
                        "Could not find APK in zip"
                    )
                    return@withContext Result.failure()
                }
                targetApkFile
            } else {
                downloadFile
            }

            if (version.startsWith("nightly-r")) {
                val runNumberString = version.removePrefix("nightly-r")
                val runNumber = runNumberString.toIntOrNull()
                if (runNumber != null) {
                    val sharedPreferences = context.getSharedPreferences("update_settings", Context.MODE_PRIVATE)
                    sharedPreferences.edit().putInt("last_installed_nightly_run", runNumber).apply()
                }
            }

            DownloadNotificationManager.showDownloadComplete(version, finalFile.absolutePath)

            Result.success(workDataOf("file_path" to finalFile.absolutePath))
        } catch (e: Exception) {
            DownloadNotificationManager.showDownloadFailed(
                version,
                e.message ?: context.getString(R.string.download_failed)
            )
            Result.failure()
        }
    }
}
