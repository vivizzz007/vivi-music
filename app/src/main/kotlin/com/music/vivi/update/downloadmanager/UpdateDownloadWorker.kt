package com.music.vivi.update.downloadmanager

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

class UpdateDownloadWorker(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val apkUrl = inputData.getString("apk_url") ?: return@withContext Result.failure()
        val version = inputData.getString("version") ?: "unknown"
        val fileSize = inputData.getString("file_size") ?: ""

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

            val outputFile = File(downloadDir, "vivi.apk")
            val outputStream = FileOutputStream(outputFile)

            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalBytesRead: Long = 0

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                if (isStopped) {
                    outputStream.close()
                    inputStream.close()
                    connection.disconnect()
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

            DownloadNotificationManager.showDownloadComplete(version, outputFile.absolutePath)

            Result.success(workDataOf("file_path" to outputFile.absolutePath))
        } catch (e: Exception) {
            DownloadNotificationManager.showDownloadFailed(
                version,
                e.message ?: context.getString(R.string.download_failed)
            )
            Result.failure()
        }
    }
}
