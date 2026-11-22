package com.example

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.AppManagerViewModel
import com.example.ui.components.ApkInstallConfirmationDialog
import com.example.ui.components.OverwriteApkConfirmationDialog
import com.example.ui.navigation.AppRoutes
import com.example.ui.navigation.PrimaryBottomTabs
import com.example.ui.screens.AddEditAppScreen
import com.example.ui.screens.AiStudioGuideScreen
import com.example.ui.screens.ApkBuildManagerScreen
import com.example.ui.screens.AppFolderDetailScreen
import com.example.ui.screens.DownloadsHistoryScreen
import com.example.ui.screens.GitHubConnectScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.MyAppsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AiStudioAppManagerTheme
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.NavyBorder
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import java.io.File

class MainActivity : ComponentActivity() {

    private val viewModel: AppManagerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        handleOAuthIntentIfPresent(intent)

        setContent {
            AiStudioAppManagerTheme {
                AiStudioAppManagerRoot(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOAuthIntentIfPresent(intent)
    }

    private fun handleOAuthIntentIfPresent(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "aistudioappmanager" && data.host == "oauth" && data.path == "/github") {
            viewModel.handleGitHubOAuthCallback(data)
        }
    }
}

@Composable
fun AiStudioAppManagerRoot(viewModel: AppManagerViewModel) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val opState by viewModel.opState.collectAsStateWithLifecycle()
    val metrics by viewModel.dashboardMetrics.collectAsStateWithLifecycle()
    val allApps by viewModel.allApps.collectAsStateWithLifecycle()
    val filteredApps by viewModel.filteredApps.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val allBuilds by viewModel.allBuilds.collectAsStateWithLifecycle()
    val allArtifacts by viewModel.allArtifacts.collectAsStateWithLifecycle()
    val allDownloads by viewModel.allDownloads.collectAsStateWithLifecycle()
    val recentActivity by viewModel.recentActivity.collectAsStateWithLifecycle()

    // Quick Import ZIP target selector (used when user taps IMPORT PROJECT ZIP from Dashboard or My Apps)
    var showPickAppForZipDialog by remember { mutableStateOf(false) }
    var targetAppIdForZip by remember { mutableStateOf<Long?>(null) }

    val quickZipPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        val appId = targetAppIdForZip
        if (uri != null && appId != null) {
            val app = allApps.firstOrNull { it.id == appId }
            viewModel.importSourceZip(appId, uri, app?.versionName ?: "1.0.0")
        }
        targetAppIdForZip = null
    }

    LaunchedEffect(opState.snackbarMessage) {
        val msg = opState.snackbarMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isTopLevelTab = currentRoute in PrimaryBottomTabs.map { it.route }

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBackground)
    ) {
        val useNavigationRail = maxWidth >= 720.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (useNavigationRail && isTopLevelTab) {
                NavigationRail(
                    containerColor = NavySurface,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    PrimaryBottomTabs.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigateTopLevel(tab.route) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = { Text(tab.label, maxLines = 1) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = ElectricBlue,
                                selectedTextColor = ElectricBlue,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted,
                                indicatorColor = ElectricBlue.copy(alpha = 0.18f)
                            ),
                            modifier = Modifier.testTag(tab.testTag)
                        )
                    }
                }
            }

            Scaffold(
                containerColor = NavyBackground,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = {
                    SnackbarHost(hostState = snackbarHostState) { data ->
                        Snackbar(
                            snackbarData = data,
                            containerColor = NavySurfaceElevated,
                            contentColor = TextPrimary,
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                },
                bottomBar = {
                    if (!useNavigationRail && isTopLevelTab) {
                        NavigationBar(
                            containerColor = NavySurface,
                            tonalElevation = 0.dp,
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            PrimaryBottomTabs.forEach { tab ->
                                val selected = currentRoute == tab.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTopLevel(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.label,
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = ElectricBlue,
                                        selectedTextColor = ElectricBlue,
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted,
                                        indicatorColor = ElectricBlue.copy(alpha = 0.18f)
                                    ),
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = AppRoutes.HOME,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    composable(AppRoutes.HOME) {
                        HomeDashboardScreen(
                            metrics = metrics,
                            recentActivity = recentActivity,
                            opState = opState,
                            onOpenAiStudioGuide = { navController.navigate(AppRoutes.AI_STUDIO_GUIDE) },
                            onOpenMyApps = { navigateTopLevel(AppRoutes.MY_APPS) },
                            onConnectGitHub = { navController.navigate(AppRoutes.GITHUB_CONNECT) },
                            onImportProjectZip = {
                                if (allApps.isEmpty()) {
                                    viewModel.showMessage("Create an app folder first, then import its source ZIP.")
                                    navController.navigate(AppRoutes.ADD_APP)
                                } else {
                                    showPickAppForZipDialog = true
                                }
                            },
                            onAddNewApp = { navController.navigate(AppRoutes.ADD_APP) },
                            onOpenApkBuilds = { navigateTopLevel(AppRoutes.APK_BUILDS) },
                            onOpenDownloadHistory = { navigateTopLevel(AppRoutes.DOWNLOADS) },
                            onOpenSettings = { navigateTopLevel(AppRoutes.SETTINGS) },
                            onRefreshAll = { viewModel.refreshAllConnectedRepos() },
                            onLoadDemoWorkspace = { viewModel.loadDemoFolders() },
                            onOpenAppDetail = { id -> navController.navigate(AppRoutes.appDetail(id)) }
                        )
                    }

                    composable(AppRoutes.MY_APPS) {
                        MyAppsScreen(
                            apps = filteredApps,
                            totalAppCount = allApps.size,
                            searchQuery = searchQuery,
                            selectedFilter = selectedFilter,
                            onSearchQueryChange = viewModel::setSearchQuery,
                            onFilterSelected = viewModel::setFilter,
                            onAddNewApp = { navController.navigate(AppRoutes.ADD_APP) },
                            onOpenAppDetail = { id -> navController.navigate(AppRoutes.appDetail(id)) },
                            onEditApp = { id -> navController.navigate(AppRoutes.editApp(id)) },
                            onDeleteApp = { viewModel.deleteApp(it) },
                            onDownloadLatestApk = { viewModel.downloadLatestApkOneClick(it) },
                            onImportSourceZipForApp = { appId ->
                                targetAppIdForZip = appId
                                quickZipPicker.launch(arrayOf("application/zip", "application/x-zip-compressed", "*/*"))
                            },
                            onLoadDemoFolders = { viewModel.loadDemoFolders() }
                        )
                    }

                    composable(AppRoutes.APK_BUILDS) {
                        ApkBuildManagerScreen(
                            apps = allApps,
                            builds = allBuilds,
                            artifacts = allArtifacts,
                            opState = opState,
                            onConnectGitHub = { navController.navigate(AppRoutes.GITHUB_CONNECT) },
                            onRefreshAll = { viewModel.refreshAllConnectedRepos() },
                            onRefreshAppBuilds = { viewModel.refreshBuildsForApp(it) },
                            onOpenUrl = { viewModel.openUrlInBrowser(it) },
                            onDownloadArtifactZip = { viewModel.downloadArtifactZipOnly(it) },
                            onExtractArtifact = { viewModel.extractArtifactForInspection(it) },
                            onSaveExtractedApk = { appId, file, run ->
                                viewModel.saveExtractedApk(appId, file, run)
                            },
                            onInstallApkFile = { appId, appName, file ->
                                viewModel.requestInstallApk(appId, appName, file)
                            },
                            onDownloadLatestApkForApp = { viewModel.downloadLatestApkOneClick(it) },
                            onDismissExtractionOutcome = { viewModel.clearExtractionOutcome() }
                        )
                    }

                    composable(AppRoutes.DOWNLOADS) {
                        DownloadsHistoryScreen(
                            downloads = allDownloads,
                            onInstallApk = { rec ->
                                viewModel.requestInstallApk(
                                    appId = rec.appId,
                                    appName = rec.appName,
                                    apkFile = File(rec.localFilePath),
                                    versionName = rec.versionName
                                )
                            },
                            onOpenFile = { file, mime ->
                                viewModel.installer.openFileWithSystemChooser(file, mime).onFailure {
                                    viewModel.showMessage(it.localizedMessage ?: "No app available to open this file.")
                                }
                            },
                            onShareFile = { file, mime, title ->
                                viewModel.installer.shareFile(file, mime, title).onFailure {
                                    viewModel.showMessage(it.localizedMessage ?: "Could not share file.")
                                }
                            },
                            onDeleteRecord = { rec, deleteFile ->
                                viewModel.deleteDownloadRecord(rec, deleteFile)
                            },
                            onRetryDownload = { appId -> viewModel.downloadLatestApkOneClick(appId) },
                            onGoToBuilds = { navigateTopLevel(AppRoutes.APK_BUILDS) }
                        )
                    }

                    composable(AppRoutes.SETTINGS) {
                        SettingsScreen(
                            settings = settings,
                            opState = opState,
                            hasDemoFolders = allApps.any { it.isDemoData },
                            onUpdateAiStudioUrl = viewModel::updateAiStudioUrl,
                            onConnectGitHub = { navController.navigate(AppRoutes.GITHUB_CONNECT) },
                            onDisconnectGitHub = viewModel::disconnectGitHub,
                            onSignInGoogle = { viewModel.signInWithGoogle(context) },
                            onDisconnectGoogle = viewModel::disconnectGoogleAccount,
                            onSelectCustomStorage = viewModel::updateCustomStorageLocation,
                            onSetWifiOnly = viewModel::setWifiOnlyDownloads,
                            onSetAutoRefresh = viewModel::setAutoRefresh,
                            onSetNotifications = viewModel::setNotificationsEnabled,
                            onClearDownloadHistory = viewModel::clearDownloadHistory,
                            onExportBackup = viewModel::exportBackup,
                            onRestoreBackup = viewModel::restoreBackup,
                            onRefreshStorage = viewModel::refreshStorageUsage,
                            onLoadDemoData = viewModel::loadDemoFolders,
                            onClearDemoData = viewModel::clearDemoFolders
                        )
                    }

                    composable(AppRoutes.ADD_APP) {
                        AddEditAppScreen(
                            existingApp = null,
                            onBack = { navController.popBackStack() },
                            onSave = { name, desc, aiUrl, ghUrl, type, version, notes, iconUri, colorHex, symbol ->
                                viewModel.createApp(
                                    name, desc, aiUrl, ghUrl, type, version, notes, iconUri, colorHex, symbol
                                ) { newId ->
                                    navController.navigate(AppRoutes.appDetail(newId)) {
                                        popUpTo(AppRoutes.ADD_APP) { inclusive = true }
                                    }
                                }
                            },
                            onConnectGitHub = { navController.navigate(AppRoutes.GITHUB_CONNECT) },
                            onSaveAndImportZip = { name, desc, aiUrl, ghUrl, type, version, notes, iconUri, colorHex, symbol ->
                                viewModel.createApp(
                                    name, desc, aiUrl, ghUrl, type, version, notes, iconUri, colorHex, symbol
                                ) { newId ->
                                    targetAppIdForZip = newId
                                    quickZipPicker.launch(arrayOf("application/zip", "application/x-zip-compressed", "*/*"))
                                    navController.navigate(AppRoutes.appDetail(newId)) {
                                        popUpTo(AppRoutes.ADD_APP) { inclusive = true }
                                    }
                                }
                            }
                        )
                    }

                    composable(
                        route = AppRoutes.EDIT_APP,
                        arguments = listOf(navArgument("appId") { type = NavType.LongType })
                    ) { backStack ->
                        val appId = backStack.arguments?.getLong("appId") ?: -1L
                        val appEntity = allApps.firstOrNull { it.id == appId }
                        AddEditAppScreen(
                            existingApp = appEntity,
                            onBack = { navController.popBackStack() },
                            onSave = { name, desc, aiUrl, ghUrl, type, version, notes, iconUri, colorHex, symbol ->
                                if (appEntity != null) {
                                    viewModel.updateApp(
                                        appEntity, name, desc, aiUrl, ghUrl, type, version, notes, iconUri, colorHex, symbol
                                    ) {
                                        navController.popBackStack()
                                    }
                                }
                            },
                            onConnectGitHub = { navController.navigate(AppRoutes.GITHUB_CONNECT) },
                            onSaveAndImportZip = { name, desc, aiUrl, ghUrl, type, version, notes, iconUri, colorHex, symbol ->
                                if (appEntity != null) {
                                    viewModel.updateApp(
                                        appEntity, name, desc, aiUrl, ghUrl, type, version, notes, iconUri, colorHex, symbol
                                    ) {
                                        targetAppIdForZip = appEntity.id
                                        quickZipPicker.launch(arrayOf("application/zip", "application/x-zip-compressed", "*/*"))
                                        navController.popBackStack()
                                    }
                                }
                            }
                        )
                    }

                    composable(
                        route = AppRoutes.APP_DETAIL,
                        arguments = listOf(navArgument("appId") { type = NavType.LongType })
                    ) { backStack ->
                        val appId = backStack.arguments?.getLong("appId") ?: -1L
                        val app by viewModel.repository.observeApp(appId).collectAsStateWithLifecycle(initialValue = null)
                        val zips by viewModel.repository.observeZipsForApp(appId).collectAsStateWithLifecycle(initialValue = emptyList())
                        val builds by viewModel.repository.observeBuildsForApp(appId).collectAsStateWithLifecycle(initialValue = emptyList())
                        val apks by viewModel.repository.observeApksForApp(appId).collectAsStateWithLifecycle(initialValue = emptyList())
                        val downloads by viewModel.repository.observeDownloadsForApp(appId).collectAsStateWithLifecycle(initialValue = emptyList())

                        AppFolderDetailScreen(
                            app = app,
                            sourceZips = zips,
                            builds = builds,
                            apks = apks,
                            downloads = downloads,
                            onBack = { navController.popBackStack() },
                            onEditApp = { navController.navigate(AppRoutes.editApp(appId)) },
                            onDeleteApp = {
                                app?.let { target ->
                                    viewModel.deleteApp(target) {
                                        navController.popBackStack()
                                    }
                                }
                            },
                            onDownloadLatestApk = { viewModel.downloadLatestApkOneClick(appId) },
                            onRefreshBuilds = { viewModel.refreshBuildsForApp(appId) },
                            onOpenUrl = { viewModel.openUrlInBrowser(it) },
                            onConnectGitHub = { navController.navigate(AppRoutes.GITHUB_CONNECT) },
                            onOpenBuildsTab = { navigateTopLevel(AppRoutes.APK_BUILDS) },
                            onImportSourceZip = { uri, ver -> viewModel.importSourceZip(appId, uri, ver) },
                            onExtractSourceZip = { viewModel.extractSourceZip(it) },
                            onImportLocalApk = { uri -> viewModel.importLocalApk(appId, uri) },
                            onInstallApk = { apk ->
                                viewModel.requestInstallApk(
                                    appId = appId,
                                    appName = app?.name ?: "App",
                                    apkFile = File(apk.localFilePath),
                                    versionName = apk.versionName,
                                    packageName = apk.packageName
                                )
                            },
                            onShareFile = { file, mime, title ->
                                viewModel.installer.shareFile(file, mime, title).onFailure {
                                    viewModel.showMessage(it.localizedMessage ?: "Could not share file.")
                                }
                            },
                            onOpenFile = { file, mime ->
                                viewModel.installer.openFileWithSystemChooser(file, mime).onFailure {
                                    viewModel.showMessage(it.localizedMessage ?: "Could not open file.")
                                }
                            },
                            onDeleteApk = { viewModel.deleteApkVersion(it) }
                        )
                    }

                    composable(AppRoutes.AI_STUDIO_GUIDE) {
                        AiStudioGuideScreen(
                            settings = settings,
                            googleAccount = opState.googleAccount,
                            onBack = { navController.popBackStack() },
                            onOpenUrl = { viewModel.openUrlInBrowser(it) },
                            onSignInWithGoogle = { viewModel.signInWithGoogle(context) },
                            onAddNewApp = { navController.navigate(AppRoutes.ADD_APP) },
                            onImportZip = {
                                if (allApps.isEmpty()) {
                                    navController.navigate(AppRoutes.ADD_APP)
                                } else {
                                    showPickAppForZipDialog = true
                                }
                            }
                        )
                    }

                    composable(AppRoutes.GITHUB_CONNECT) {
                        GitHubConnectScreen(
                            apps = allApps,
                            settings = settings,
                            opState = opState,
                            onBack = { navController.popBackStack() },
                            onStartDeviceFlow = viewModel::startGitHubDeviceFlow,
                            onCancelDeviceFlow = viewModel::cancelDeviceFlow,
                            onStartPkceFlow = viewModel::launchGitHubPkceBrowser,
                            onRefreshRepos = { viewModel.loadGitHubRepositories() },
                            onDisconnectGitHub = viewModel::disconnectGitHub,
                            onOpenUrl = { viewModel.openUrlInBrowser(it) },
                            onSaveGitHubClientId = viewModel::setCustomGitHubClientId,
                            onLinkRepoToApp = viewModel::linkRepoToApp,
                            onCreateAppFromRepo = { repo ->
                                viewModel.createAppFromGitHubRepo(repo) { newId ->
                                    navController.navigate(AppRoutes.appDetail(newId))
                                }
                            },
                            onAddManualRepoApp = { navController.navigate(AppRoutes.ADD_APP) }
                        )
                    }
                }
            }
        }

        // Global Busy Progress Overlay
        if (opState.isBusy) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NavyBackground.copy(alpha = 0.72f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = NavySurfaceElevated,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, NavyBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = ElectricBlue)
                        Text(
                            text = opState.busyMessage.ifBlank { "Working..." },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }

    // Quick App Picker for Dashboard "Import Project ZIP" button
    if (showPickAppForZipDialog) {
        AlertDialog(
            onDismissRequest = { showPickAppForZipDialog = false },
            containerColor = NavySurfaceElevated,
            title = { Text("Select Target App Folder", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(allApps, key = { it.id }) { appItem ->
                        OutlinedButton(
                            onClick = {
                                showPickAppForZipDialog = false
                                targetAppIdForZip = appItem.id
                                quickZipPicker.launch(arrayOf("application/zip", "application/x-zip-compressed", "*/*"))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(appItem.name, color = TextPrimary)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPickAppForZipDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }

    // Global APK Installation Confirmation Dialog
    opState.pendingInstall?.let { req ->
        ApkInstallConfirmationDialog(
            request = req,
            onConfirm = viewModel::confirmAndLaunchPackageInstaller,
            onDismiss = viewModel::dismissInstallDialog
        )
    }

    // Global Overwrite Existing APK Confirmation Dialog
    opState.pendingOverwrite?.let { req ->
        OverwriteApkConfirmationDialog(
            request = req,
            onOverwrite = { viewModel.resolveOverwriteConfirmation(overwriteExisting = true) },
            onKeepBoth = { viewModel.resolveOverwriteConfirmation(overwriteExisting = false) },
            onDismiss = viewModel::dismissOverwriteConfirmation
        )
    }
}
