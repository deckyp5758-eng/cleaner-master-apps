package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.providers.UpdateViewModel
import com.example.screens.HomeScreen
import com.example.screens.LargeFilesScreen
import com.example.screens.ResultScreen
import com.example.screens.ScanResultScreen
import com.example.screens.SettingsScreen
import com.example.screens.SocialMediaCleanerScreen
import com.example.ui.theme.CleanCacheTheme
import com.example.viewmodel.CleanerViewModel
import com.example.viewmodel.ScanState
import com.example.widgets.UpdateDialog

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CleanCacheApp()
        }
    }
}

@Composable
fun CleanCacheApp(
    viewModel: CleanerViewModel = viewModel(),
    updateViewModel: UpdateViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val updateUiState by updateViewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(updateUiState.snackbarMessage) {
        updateUiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            updateViewModel.clearSnackbarMessage()
        }
    }

    // Navigasi otomatis berdasarkan status pemindaian
    LaunchedEffect(uiState.scanState) {
        when (uiState.scanState) {
            ScanState.SCANNED -> {
                navController.navigate("scan_result") {
                    launchSingleTop = true
                }
            }
            ScanState.CLEANED -> {
                navController.navigate("result") {
                    popUpTo("home") { inclusive = false }
                    launchSingleTop = true
                }
            }
            ScanState.IDLE -> {
                if (navController.currentDestination?.route != "home" &&
                    navController.currentDestination?.route != "large_files" &&
                    navController.currentDestination?.route != "social_cleaner" &&
                    navController.currentDestination?.route != "settings"
                ) {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            }
            else -> {}
        }
    }

    CleanCacheTheme(darkTheme = uiState.isDarkMode) {
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.fillMaxSize()
        ) {
            composable("home") {
                HomeScreen(
                    uiState = uiState,
                    onStartScan = { viewModel.startScan() },
                    onOpenLargeFiles = { navController.navigate("large_files") },
                    onOpenSocialCleaner = { navController.navigate("social_cleaner") },
                    onToggleDarkMode = { viewModel.toggleDarkMode(it) },
                    onOpenSettings = { navController.navigate("settings") },
                    onRefreshStorage = { viewModel.refreshStorageInfo() },
                    onErrorDismissed = { viewModel.clearError() }
                )
            }

            composable("scan_result") {
                ScanResultScreen(
                    uiState = uiState,
                    onToggleAppSelection = { viewModel.toggleAppSelection(it) },
                    onToggleSelectAllApps = { viewModel.toggleSelectAllApps(it) },
                    onToggleJunkCategorySelection = { viewModel.toggleJunkCategorySelection(it) },
                    onStartCleaning = { viewModel.startCleaning() },
                    onBackToHome = {
                        viewModel.resetToHome()
                        navController.popBackStack()
                    }
                )
            }

            composable("result") {
                ResultScreen(
                    uiState = uiState,
                    onDone = {
                        viewModel.resetToHome()
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                )
            }

            composable("large_files") {
                LargeFilesScreen(
                    uiState = uiState,
                    onScan = { viewModel.scanLargeAndDuplicateFiles() },
                    onToggleSelect = { viewModel.toggleLargeFileSelection(it) },
                    onToggleSelectAll = { viewModel.toggleAllLargeFiles(it) },
                    onDeleteSelected = {
                        viewModel.deleteSelectedLargeFiles { freedBytes ->
                            navController.navigate("result")
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable("social_cleaner") {
                SocialMediaCleanerScreen(
                    uiState = uiState,
                    onScan = { viewModel.scanSocialMediaJunk() },
                    onToggleSelect = { viewModel.toggleSocialMediaJunkSelection(it) },
                    onCleanSelected = {
                        viewModel.cleanSelectedSocialMediaJunk { freedBytes ->
                            navController.navigate("result")
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable("settings") {
                SettingsScreen(
                    uiState = uiState,
                    isCheckingUpdate = updateUiState.isCheckingUpdate,
                    onCheckUpdateManual = { updateViewModel.checkForUpdates(isManual = true) },
                    onToggleDarkMode = { viewModel.toggleDarkMode(it) },
                    onBack = { navController.popBackStack() }
                )
            }
        }

        // Dialog Pembaruan Aplikasi dari GitHub Releases
        if (updateUiState.showUpdateDialog) {
            UpdateDialog(
                updateInfo = updateUiState.updateInfo,
                updateStatus = updateUiState.updateStatus,
                downloadPercent = updateUiState.downloadPercent,
                downloadedBytes = updateUiState.downloadedBytes,
                totalBytes = updateUiState.totalBytes,
                errorMessage = updateUiState.errorMessage,
                onStartDownload = { updateViewModel.startDownload() },
                onInstallNow = { updateViewModel.installApk() },
                onDismiss = { updateViewModel.dismissDialog() }
            )
        }
    }
}
