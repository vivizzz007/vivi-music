package com.music.vivi.desktop

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Single source of truth for the update download state, shared by the update
 * notification banner and the Settings → Updates screen. Both surfaces read and
 * write the same state, so downloading/opening an installer from one is
 * immediately reflected in the other (and vice versa).
 */
object UpdateState {
    private val _progress = MutableStateFlow<DownloadProgress?>(null)
    val progress: StateFlow<DownloadProgress?> = _progress.asStateFlow()

    private val _downloadedFile = MutableStateFlow<File?>(null)
    val downloadedFile: StateFlow<File?> = _downloadedFile.asStateFlow()

    private val _installerCount = MutableStateFlow(UpdateDownloader.downloadedInstallers().size)
    val installerCount: StateFlow<Int> = _installerCount.asStateFlow()

    /**
     * Re-derives the on-disk installer for the currently available version (or
     * clears it) and refreshes the installer count. Call whenever the update
     * status changes so a stale installer for an older version is never offered.
     */
    fun syncWithStatus(status: UpdateStatus?) {
        val asset = (status as? UpdateStatus.Available)?.asset
        _downloadedFile.value = asset?.let { UpdateDownloader.downloadedInstaller(it.fileName) }
        _installerCount.value = UpdateDownloader.downloadedInstallers().size
    }

    /** Downloads [asset], reporting progress through [progress]; null on error. */
    suspend fun download(asset: UpdateAsset): File? {
        _progress.value = DownloadProgress(0, asset.sizeBytes, 0)
        return try {
            val file = UpdateDownloader.download(asset.downloadUrl, asset.fileName) { p -> _progress.value = p }
            _downloadedFile.value = file
            _installerCount.value = UpdateDownloader.downloadedInstallers().size
            file
        } catch (_: Exception) {
            null
        } finally {
            _progress.value = null
        }
    }

    fun deleteAllInstallers() {
        UpdateDownloader.deleteAll()
        _downloadedFile.value = null
        _installerCount.value = 0
    }
}
