package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.models.JunkType
import com.example.models.LargeFileInfo
import com.example.models.ScanResult
import com.example.models.SocialMediaJunk
import com.example.services.AdvancedCleanerService
import com.example.services.CacheService
import com.example.utils.FileUtils
import com.example.utils.PermissionUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ScanState {
    IDLE,
    SCANNING,
    SCANNED,
    CLEANING,
    CLEANED
}

data class CleanerUiState(
    val scanState: ScanState = ScanState.IDLE,
    val scanProgressPercent: Int = 0,
    val scanProgressMessage: String = "",
    val scanResult: ScanResult = ScanResult(),
    val selectedAppIds: Set<String> = emptySet(),
    val selectedJunkTypes: Set<JunkType> = JunkType.values().toSet(),
    val lastFreedSizeBytes: Long = 0L,
    val totalStorageBytes: Long = 0L,
    val availableStorageBytes: Long = 0L,
    val isDarkMode: Boolean = true,
    val errorMessage: String? = null,
    val hasStoragePermission: Boolean = true,

    // State Pembersih File Besar & Duplikat
    val largeFiles: List<LargeFileInfo> = emptyList(),
    val isScanningLargeFiles: Boolean = false,
    val isCleaningLargeFiles: Boolean = false,

    // State Pembersih Media Sosial (WhatsApp, TikTok, dll)
    val socialJunkItems: List<SocialMediaJunk> = emptyList(),
    val isScanningSocialJunk: Boolean = false,
    val isCleaningSocialJunk: Boolean = false
) {
    val usedStorageBytes: Long
        get() = (totalStorageBytes - availableStorageBytes).coerceAtLeast(0L)

    val usedStoragePercentage: Float
        get() = if (totalStorageBytes > 0) {
            (usedStorageBytes.toFloat() / totalStorageBytes.toFloat())
        } else 0.72f

    val selectedAppsCacheSizeBytes: Long
        get() = scanResult.appCacheList
            .filter { selectedAppIds.contains(it.id) }
            .sumOf { it.cacheSizeBytes }

    val selectedJunkCategoriesSizeBytes: Long
        get() = scanResult.categories
            .filter { selectedJunkTypes.contains(it.type) && it.type != JunkType.APP_CACHE }
            .sumOf { it.totalSizeBytes }

    val totalSelectedCleanSizeBytes: Long
        get() = selectedAppsCacheSizeBytes + selectedJunkCategoriesSizeBytes

    val selectedLargeFilesSizeBytes: Long
        get() = largeFiles.filter { it.isSelected }.sumOf { it.sizeBytes }

    val selectedSocialJunkSizeBytes: Long
        get() = socialJunkItems.filter { it.isSelected }.sumOf { it.sizeBytes }
}

class CleanerViewModel(application: Application) : AndroidViewModel(application) {

    private val cacheService = CacheService(application.applicationContext)
    private val advancedCleanerService = AdvancedCleanerService(application.applicationContext)
    private val prefs = application.getSharedPreferences("clean_cache_pro_settings", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(CleanerUiState())
    val uiState: StateFlow<CleanerUiState> = _uiState.asStateFlow()

    init {
        val savedDarkMode = prefs.getBoolean("is_dark_mode", true)
        refreshStorageInfo()
        checkStoragePermission(application.applicationContext)
        _uiState.update { it.copy(isDarkMode = savedDarkMode) }
    }

    /**
     * Memeriksa dan memperbarui status izin penyimpanan.
     */
    fun checkStoragePermission(context: Context) {
        val hasPermission = PermissionUtils.hasStoragePermission(context)
        _uiState.update { it.copy(hasStoragePermission = hasPermission) }
    }

    /**
     * Memperbarui data ukuran memori internal HP.
     */
    fun refreshStorageInfo() {
        try {
            val total = FileUtils.getTotalInternalStorageSize()
            val available = FileUtils.getAvailableInternalStorageSize()
            _uiState.update {
                it.copy(
                    totalStorageBytes = total,
                    availableStorageBytes = available
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(errorMessage = "Gagal membaca info penyimpanan internal")
            }
        }
    }

    /**
     * Menjalankan proses pemindaian (Scan) cache dan junk files.
     */
    fun startScan() {
        if (_uiState.value.scanState == ScanState.SCANNING) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    scanState = ScanState.SCANNING,
                    scanProgressPercent = 0,
                    scanProgressMessage = "Menyiapkan pemindaian...",
                    errorMessage = null
                )
            }

            try {
                val result = cacheService.performScan { percent, message ->
                    _uiState.update { state ->
                        state.copy(
                            scanProgressPercent = percent,
                            scanProgressMessage = message
                        )
                    }
                }

                val allAppIds = result.appCacheList.map { it.id }.toSet()
                val allCategoryTypes = result.categories.map { it.type }.toSet()

                _uiState.update {
                    it.copy(
                        scanState = ScanState.SCANNED,
                        scanResult = result,
                        selectedAppIds = allAppIds,
                        selectedJunkTypes = allCategoryTypes,
                        scanProgressPercent = 100
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        scanState = ScanState.IDLE,
                        errorMessage = "Terjadi kesalahan saat memindai: ${e.localizedMessage ?: "Gagal mengakses storage"}"
                    )
                }
            }
        }
    }

    /**
     * Mengubah opsi centang pada aplikasi tertentu.
     */
    fun toggleAppSelection(appId: String) {
        _uiState.update { state ->
            val currentSelected = state.selectedAppIds.toMutableSet()
            if (currentSelected.contains(appId)) {
                currentSelected.remove(appId)
            } else {
                currentSelected.add(appId)
            }
            state.copy(selectedAppIds = currentSelected)
        }
    }

    /**
     * Mengubah opsi centang untuk semua aplikasi sekaligus.
     */
    fun toggleSelectAllApps(selectAll: Boolean) {
        _uiState.update { state ->
            val newSelection = if (selectAll) {
                state.scanResult.appCacheList.map { it.id }.toSet()
            } else {
                emptySet()
            }
            state.copy(selectedAppIds = newSelection)
        }
    }

    /**
     * Mengubah opsi centang pada kategori junk tertentu.
     */
    fun toggleJunkCategorySelection(type: JunkType) {
        _uiState.update { state ->
            val currentSelected = state.selectedJunkTypes.toMutableSet()
            if (currentSelected.contains(type)) {
                currentSelected.remove(type)
            } else {
                currentSelected.add(type)
            }
            state.copy(selectedJunkTypes = currentSelected)
        }
    }

    /**
     * Menjalankan proses pembersihan cache dan sampah terpilih.
     */
    fun startCleaning() {
        val currentState = _uiState.value
        if (currentState.scanState == ScanState.CLEANING) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    scanState = ScanState.CLEANING,
                    scanProgressPercent = 0,
                    scanProgressMessage = "Menyiapkan pembersihan...",
                    errorMessage = null
                )
            }

            try {
                val freedBytes = cacheService.performClean(
                    selectedAppIds = currentState.selectedAppIds,
                    selectedJunkTypes = currentState.selectedJunkTypes,
                    scanResult = currentState.scanResult
                ) { percent, message ->
                    _uiState.update { state ->
                        state.copy(
                            scanProgressPercent = percent,
                            scanProgressMessage = message
                        )
                    }
                }

                val newAvailable = (currentState.availableStorageBytes + freedBytes)
                    .coerceAtMost(currentState.totalStorageBytes)

                _uiState.update {
                    it.copy(
                        scanState = ScanState.CLEANED,
                        lastFreedSizeBytes = freedBytes,
                        availableStorageBytes = newAvailable,
                        scanProgressPercent = 100
                    )
                }
                refreshStorageInfo()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        scanState = ScanState.SCANNED,
                        errorMessage = "Gagal membersihkan: ${e.localizedMessage ?: "Terjadi kendala sistem"}"
                    )
                }
            }
        }
    }

    // ==========================================
    // MODULE 1: FITUR FILE BESAR & DUPLIKAT
    // ==========================================

    fun scanLargeAndDuplicateFiles() {
        if (_uiState.value.isScanningLargeFiles) return

        viewModelScope.launch {
            _uiState.update { it.copy(isScanningLargeFiles = true) }

            val files = advancedCleanerService.scanLargeAndDuplicateFiles { _, _ -> }

            _uiState.update {
                it.copy(
                    largeFiles = files,
                    isScanningLargeFiles = false
                )
            }
        }
    }

    fun toggleLargeFileSelection(fileId: String) {
        _uiState.update { state ->
            val updatedList = state.largeFiles.map { item ->
                if (item.id == fileId) item.copy(isSelected = !item.isSelected) else item
            }
            state.copy(largeFiles = updatedList)
        }
    }

    fun toggleAllLargeFiles(selectAll: Boolean) {
        _uiState.update { state ->
            val updatedList = state.largeFiles.map { it.copy(isSelected = selectAll) }
            state.copy(largeFiles = updatedList)
        }
    }

    fun deleteSelectedLargeFiles(onComplete: (Long) -> Unit) {
        if (_uiState.value.isCleaningLargeFiles) return

        viewModelScope.launch {
            _uiState.update { it.copy(isCleaningLargeFiles = true) }

            val selectedFiles = _uiState.value.largeFiles.filter { it.isSelected }
            val freedBytes = advancedCleanerService.deleteSelectedFiles(selectedFiles)

            val remainingFiles = _uiState.value.largeFiles.filter { !it.isSelected }
            val newAvailable = (_uiState.value.availableStorageBytes + freedBytes)
                .coerceAtMost(_uiState.value.totalStorageBytes)

            _uiState.update {
                it.copy(
                    largeFiles = remainingFiles,
                    isCleaningLargeFiles = false,
                    lastFreedSizeBytes = freedBytes,
                    availableStorageBytes = newAvailable
                )
            }
            refreshStorageInfo()
            onComplete(freedBytes)
        }
    }

    // ==========================================
    // MODULE 2: FITUR PEMBERSIH MEDIA SOSIAL
    // ==========================================

    fun scanSocialMediaJunk() {
        if (_uiState.value.isScanningSocialJunk) return

        viewModelScope.launch {
            _uiState.update { it.copy(isScanningSocialJunk = true) }

            val junkList = advancedCleanerService.scanSocialMediaJunk()

            _uiState.update {
                it.copy(
                    socialJunkItems = junkList,
                    isScanningSocialJunk = false
                )
            }
        }
    }

    fun toggleSocialMediaJunkSelection(junkId: String) {
        _uiState.update { state ->
            val updatedList = state.socialJunkItems.map { item ->
                if (item.id == junkId) item.copy(isSelected = !item.isSelected) else item
            }
            state.copy(socialJunkItems = updatedList)
        }
    }

    fun cleanSelectedSocialMediaJunk(onComplete: (Long) -> Unit) {
        if (_uiState.value.isCleaningSocialJunk) return

        viewModelScope.launch {
            _uiState.update { it.copy(isCleaningSocialJunk = true) }

            val selectedItems = _uiState.value.socialJunkItems.filter { it.isSelected }
            val freedBytes = advancedCleanerService.cleanSocialMediaJunk(selectedItems)

            val remainingItems = _uiState.value.socialJunkItems.filter { !it.isSelected }
            val newAvailable = (_uiState.value.availableStorageBytes + freedBytes)
                .coerceAtMost(_uiState.value.totalStorageBytes)

            _uiState.update {
                it.copy(
                    socialJunkItems = remainingItems,
                    isCleaningSocialJunk = false,
                    lastFreedSizeBytes = freedBytes,
                    availableStorageBytes = newAvailable
                )
            }
            refreshStorageInfo()
            onComplete(freedBytes)
        }
    }

    /**
     * Mengubah mode gelap / terang dan menyimpannya di SharedPreferences.
     */
    fun toggleDarkMode(isDark: Boolean) {
        prefs.edit().putBoolean("is_dark_mode", isDark).apply()
        _uiState.update { it.copy(isDarkMode = isDark) }
    }

    /**
     * Kembali ke halaman utama (reset status scan ke IDLE).
     */
    fun resetToHome() {
        _uiState.update {
            it.copy(
                scanState = ScanState.IDLE,
                scanProgressPercent = 0,
                scanProgressMessage = ""
            )
        }
    }

    /**
     * Mengosongkan pesan error.
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
