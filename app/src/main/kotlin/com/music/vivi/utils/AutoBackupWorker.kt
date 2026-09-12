/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.utils

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.music.vivi.constants.AutoBackupEnabledKey
import com.music.vivi.constants.AutoBackupWeeklyKey

class AutoBackupWorker(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val autoBackupEnabled = context.dataStore[AutoBackupEnabledKey] ?: true
        val weeklyBackupEnabled = context.dataStore[AutoBackupWeeklyKey] ?: false

        if (!autoBackupEnabled || !weeklyBackupEnabled) {
            return Result.success()
        }

        val success = AutoBackupHelper.performBackup(context, "weekly")
        return if (success) {
            Result.success()
        } else {
            Result.retry()
        }
    }
}
