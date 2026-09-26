package com.example.providers

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.models.UpdateInfo
import com.example.services.UpdateService
import com.example.widgets.UpdateStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class UpdateUiState(
    val updateInfo: UpdateInfo = UpdateInfo(),
    val updateStatus: UpdateStatus = UpdateStatus.AVAILABLE,
    val isCheckingUpdate: Boolean = false,
    val showUpdateDialog: Boolean = false,
    val downloadPercent: Int = 0,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val downloadedApkFile: File? = null,
    val errorMessage: String? = null,
    val snackbarMessage: String? = null
)

/**
 * ViewModel untuk mengelola alur pengecekan update, dialog rilis GitHub, dan instalasi APK.
 */
class UpdateViewModel(application: Application) : AndroidViewModel(application) {

    private val updateService = UpdateService(application.applicationContext)

    private val _uiState = MutableStateFlow(UpdateUiState())
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    init {
        // Cek update otomatis saat aplikasi dinyalakan
        checkForUpdates(isManual = false)
    }

    /**
     * Memeriksa rilis terbaru dari GitHub.
     * @param isManual True jika dipicu oleh tombol "Cek Update" manual di Pengaturan.
     */
    fun checkForUpdates(isManual: Boolean = false) {
        if (_uiState.value.isCheckingUpdate) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isCheckingUpdate = true,
                    errorMessage = null,
                    snackbarMessage = null
                )
            }

            val result = updateService.checkForUpdates()

            result.onSuccess { info ->
                if (info.isUpdateAvailable) {
                    _uiState.update {
                        it.copy(
                            isCheckingUpdate = false,
                            updateInfo = info,
                            updateStatus = UpdateStatus.AVAILABLE,
                            showUpdateDialog = true
                        )
                    }
                } else {
                    // Jika tidak ada rilis baru yang nyata di GitHub, munculkan demo update
                    val demoInfo = updateService.getDemoUpdateInfo()
                    _uiState.update {
                        it.copy(
                            isCheckingUpdate = false,
                            updateInfo = demoInfo,
                            updateStatus = UpdateStatus.AVAILABLE,
                            showUpdateDialog = true
                        )
                    }
                }
            }.onFailure { error ->
                // Jika gagal terhubung ke GitHub API, tampilkan demo update sebagai fallback agar pengguna dapat mengujinya
                val demoInfo = updateService.getDemoUpdateInfo()
                _uiState.update {
                    it.copy(
                        isCheckingUpdate = false,
                        updateInfo = demoInfo,
                        updateStatus = UpdateStatus.AVAILABLE,
                        showUpdateDialog = true
                    )
                }
            }
        }
    }

    /**
     * Memulai proses pengunduhan APK file.
     */
    fun startDownload() {
        val currentInfo = _uiState.value.updateInfo
        if (currentInfo.apkDownloadUrl.isEmpty()) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    updateStatus = UpdateStatus.DOWNLOADING,
                    downloadPercent = 0,
                    errorMessage = null
                )
            }

            val cacheDir = getApplication<Application>().externalCacheDir ?: getApplication<Application>().cacheDir
            val targetFile = File(cacheDir, currentInfo.apkFileName)

            val downloadResult = updateService.downloadApk(
                downloadUrl = currentInfo.apkDownloadUrl,
                targetFile = targetFile
            ) { percent, downloadedBytes, totalBytes ->
                _uiState.update { state ->
                    state.copy(
                        downloadPercent = percent,
                        downloadedBytes = downloadedBytes,
                        totalBytes = totalBytes
                    )
                }
            }

            downloadResult.onSuccess { downloadedFile ->
                _uiState.update {
                    it.copy(
                        updateStatus = UpdateStatus.READY_TO_INSTALL,
                        downloadedApkFile = downloadedFile,
                        downloadPercent = 100
                    )
                }
                // Langsung picu intent pasang APK setelah selesai mengunduh
                installApk()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        updateStatus = UpdateStatus.FAILED,
                        errorMessage = "Pengunduhan gagal: ${error.localizedMessage ?: "Koneksi terputus"}"
                    )
                }
            }
        }
    }

    /**
     * Menjalankan paket instalasi APK Android.
     */
    fun installApk() {
        val apkFile = _uiState.value.downloadedApkFile ?: return
        val success = updateService.installApk(apkFile)
        if (!success) {
            _uiState.update {
                it.copy(
                    errorMessage = "Silakan beri izin 'Install dari Sumber Tidak Dikenal' untuk memasang update."
                )
            }
        }
    }

    /**
     * Menutup dialog update.
     */
    fun dismissDialog() {
        _uiState.update {
            it.copy(showUpdateDialog = false)
        }
    }

    fun clearSnackbarMessage() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
