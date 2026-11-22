package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AppManagerApplication
import com.example.data.github.DeviceFlowChallenge
import com.example.data.github.GitHubAuthOutcome
import com.example.data.github.GitHubRepoDto
import com.example.data.installer.ApkInstallCheckResult
import com.example.data.local.ActivityLogEntity
import com.example.data.local.ApkVersionEntity
import com.example.data.local.AppProjectEntity
import com.example.data.local.ArtifactEntity
import com.example.data.local.BuildRunEntity
import com.example.data.local.BuildStatus
import com.example.data.local.DownloadRecordEntity
import com.example.data.local.ProjectType
import com.example.data.local.SourceZipEntity
import com.example.data.preferences.AppSettings
import com.example.data.repository.ArtifactExtractionOutcome
import com.example.data.storage.StorageUsageSummary
import com.example.security.GitHubSession
import com.example.security.GoogleAccountSession
import com.example.worker.BuildRefreshWorker
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

enum class AppFilter(val label: String) {
    ALL("All Apps"),
    APK_AVAILABLE("APK Available"),
    APK_DOWNLOADED("APK Downloaded"),
    BUILD_FAILED("Build Failed"),
    BUILD_RUNNING("Build Running"),
    SOURCE_ZIP("Source ZIP")
}

data class DashboardMetrics(
    val totalApps: Int = 0,
    val apkAvailableCount: Int = 0,
    val apkDownloadedCount: Int = 0,
    val pendingBuildsCount: Int = 0,
    val totalProjectZips: Int = 0
)

data class PendingInstallRequest(
    val appId: Long,
    val appName: String,
    val apkFile: File,
    val versionName: String,
    val packageName: String,
    val needsPermissionSettings: Boolean
)

data class PendingOverwriteRequest(
    val appId: Long,
    val apkSourceFile: File,
    val apkFileName: String,
    val buildRun: BuildRunEntity? = null
)

data class UiOperationState(
    val isRefreshingAll: Boolean = false,
    val isBusy: Boolean = false,
    val busyMessage: String = "",
    val snackbarMessage: String? = null,
    val githubSession: GitHubSession? = null,
    val googleAccount: GoogleAccountSession? = null,
    val githubRepos: List<GitHubRepoDto> = emptyList(),
    val isLoadingRepos: Boolean = false,
    val deviceFlowChallenge: DeviceFlowChallenge? = null,
    val isPollingDeviceFlow: Boolean = false,
    val storageUsage: StorageUsageSummary = StorageUsageSummary(),
    val pendingInstall: PendingInstallRequest? = null,
    val pendingOverwrite: PendingOverwriteRequest? = null,
    val latestExtractionOutcome: ArtifactExtractionOutcome? = null,
    val activeArtifactForExtraction: ArtifactEntity? = null
)

class AppManagerViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AppManagerApplication
    val repository = app.repository
    val installer = app.installerManager
    private val oauthManager = app.githubOAuthManager
    private val settingsStore = app.settingsDataStore

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(AppFilter.ALL)
    val selectedFilter: StateFlow<AppFilter> = _selectedFilter.asStateFlow()

    private val _opState = MutableStateFlow(
        UiOperationState(
            githubSession = repository.credentialStore.getGitHubSession(),
            googleAccount = repository.credentialStore.getGoogleAccount()
        )
    )
    val opState: StateFlow<UiOperationState> = _opState.asStateFlow()

    private var deviceFlowJob: Job? = null

    val settings: StateFlow<AppSettings> = settingsStore.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettings()
    )

    val allApps: StateFlow<List<AppProjectEntity>> = repository.allApps.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val allBuilds: StateFlow<List<BuildRunEntity>> = repository.allBuilds.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val allArtifacts: StateFlow<List<ArtifactEntity>> = repository.allArtifacts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val allApks: StateFlow<List<ApkVersionEntity>> = repository.allApks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val allZips: StateFlow<List<SourceZipEntity>> = repository.allZips.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val allDownloads: StateFlow<List<DownloadRecordEntity>> = repository.allDownloads.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val recentActivity: StateFlow<List<ActivityLogEntity>> = repository.recentActivity.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val dashboardMetrics: StateFlow<DashboardMetrics> = combine(
        allApps,
        allBuilds,
        repository.totalZipCount
    ) { apps, builds, zipCount ->
        val pendingBuilds = builds.count {
            it.status == BuildStatus.QUEUED || it.status == BuildStatus.IN_PROGRESS
        }
        DashboardMetrics(
            totalApps = apps.size,
            apkAvailableCount = apps.count { it.hasApkAvailable },
            apkDownloadedCount = apps.count { it.hasApkDownloaded },
            pendingBuildsCount = pendingBuilds,
            totalProjectZips = zipCount
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardMetrics()
    )

    val filteredApps: StateFlow<List<AppProjectEntity>> = combine(
        allApps,
        allApks,
        _searchQuery,
        _selectedFilter
    ) { apps, apks, query, filter ->
        val trimmed = query.trim().lowercase()
        val apksByApp = apks.groupBy { it.appId }

        apps.filter { appItem ->
            val matchesFilter = when (filter) {
                AppFilter.ALL -> true
                AppFilter.APK_AVAILABLE -> appItem.hasApkAvailable
                AppFilter.APK_DOWNLOADED -> appItem.hasApkDownloaded
                AppFilter.BUILD_FAILED -> appItem.latestBuildStatus == BuildStatus.FAILED
                AppFilter.BUILD_RUNNING -> appItem.latestBuildStatus == BuildStatus.IN_PROGRESS ||
                    appItem.latestBuildStatus == BuildStatus.QUEUED
                AppFilter.SOURCE_ZIP -> appItem.hasSourceZip
            }
            if (!matchesFilter) return@filter false
            if (trimmed.isEmpty()) return@filter true

            val apkFilenames = apksByApp[appItem.id].orEmpty().map { it.fileName.lowercase() }
            appItem.name.lowercase().contains(trimmed) ||
                appItem.githubRepoName.lowercase().contains(trimmed) ||
                appItem.githubOwner.lowercase().contains(trimmed) ||
                appItem.packageName.lowercase().contains(trimmed) ||
                appItem.versionName.lowercase().contains(trimmed) ||
                appItem.latestApkVersion.lowercase().contains(trimmed) ||
                apkFilenames.any { it.contains(trimmed) }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    init {
        refreshStorageUsage()
        if (_opState.value.githubSession != null) {
            loadGitHubRepositories(silent = true)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: AppFilter) {
        _selectedFilter.value = filter
    }

    fun clearSnackbar() {
        _opState.update { it.copy(snackbarMessage = null) }
    }

    fun showMessage(message: String) {
        _opState.update { it.copy(snackbarMessage = message) }
    }

    fun refreshStorageUsage() {
        viewModelScope.launch {
            val summary = repository.getStorageUsage()
            _opState.update { it.copy(storageUsage = summary) }
        }
    }

    // --- App Folder CRUD ---

    fun createApp(
        name: String,
        description: String,
        aiStudioUrl: String,
        githubRepoUrl: String,
        projectType: ProjectType,
        versionName: String,
        notes: String,
        iconUri: String?,
        iconColorHex: String,
        iconSymbolKey: String,
        onCreated: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.createAppFolder(
                name = name,
                description = description,
                aiStudioUrl = aiStudioUrl,
                githubRepoUrl = githubRepoUrl,
                projectType = projectType,
                versionName = versionName,
                notes = notes,
                iconUri = iconUri,
                iconColorHex = iconColorHex,
                iconSymbolKey = iconSymbolKey
            )
            result.onSuccess { newId ->
                refreshStorageUsage()
                showMessage("Created app folder for $name")
                onCreated(newId)
            }.onFailure { err ->
                showMessage(err.localizedMessage ?: "Failed to create app folder.")
            }
        }
    }

    fun updateApp(
        existing: AppProjectEntity,
        name: String,
        description: String,
        aiStudioUrl: String,
        githubRepoUrl: String,
        projectType: ProjectType,
        versionName: String,
        notes: String,
        iconUri: String?,
        iconColorHex: String,
        iconSymbolKey: String,
        onUpdated: () -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.updateAppFolder(
                existing = existing,
                name = name,
                description = description,
                aiStudioUrl = aiStudioUrl,
                githubRepoUrl = githubRepoUrl,
                projectType = projectType,
                versionName = versionName,
                notes = notes,
                iconUri = iconUri,
                iconColorHex = iconColorHex,
                iconSymbolKey = iconSymbolKey
            )
            result.onSuccess {
                refreshStorageUsage()
                showMessage("Updated $name")
                onUpdated()
            }.onFailure { err ->
                showMessage(err.localizedMessage ?: "Failed to update app folder.")
            }
        }
    }

    fun deleteApp(appProject: AppProjectEntity, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteAppFolder(appProject).onSuccess {
                refreshStorageUsage()
                showMessage("Deleted folder '${appProject.name}'")
                onDeleted()
            }.onFailure {
                showMessage(it.localizedMessage ?: "Failed to delete folder.")
            }
        }
    }

    fun loadDemoFolders() {
        viewModelScope.launch {
            repository.loadDemoSampleFolders().onSuccess { count ->
                refreshStorageUsage()
                if (count > 0) {
                    showMessage("Loaded $count DEMO DATA sample folders.")
                } else {
                    showMessage("DEMO DATA folders are already present.")
                }
            }
        }
    }

    fun clearDemoFolders() {
        viewModelScope.launch {
            repository.clearDemoSampleFolders()
            refreshStorageUsage()
            showMessage("Removed all DEMO DATA sample folders.")
        }
    }

    // --- Source ZIP & Local APK Import ---

    fun importSourceZip(appId: Long, uri: Uri, versionLabel: String) {
        viewModelScope.launch {
            setBusy(true, "Importing & verifying ZIP archive...")
            repository.importSourceZip(appId, uri, versionLabel).onSuccess { zip ->
                refreshStorageUsage()
                showMessage("Saved ${zip.fileName} into Source/ folder.")
            }.onFailure { err ->
                showMessage(err.localizedMessage ?: "Failed to import ZIP file.")
            }
            setBusy(false)
        }
    }

    fun extractSourceZip(zip: SourceZipEntity) {
        viewModelScope.launch {
            setBusy(true, "Safely extracting ${zip.fileName}...")
            repository.extractSourceZip(zip).onSuccess { msg ->
                refreshStorageUsage()
                showMessage(msg)
            }.onFailure { err ->
                showMessage(err.localizedMessage ?: "Extraction failed.")
            }
            setBusy(false)
        }
    }

    fun importLocalApk(appId: Long, uri: Uri) {
        viewModelScope.launch {
            setBusy(true, "Validating APK file signature...")
            repository.importLocalApk(appId, uri).onSuccess { apk ->
                refreshStorageUsage()
                showMessage("Verified & saved ${apk.fileName}")
            }.onFailure { err ->
                showMessage(err.localizedMessage ?: "Invalid APK file.")
            }
            setBusy(false)
        }
    }

    // --- GitHub OAuth & Repository Integration ---

    fun startGitHubDeviceFlow(includePrivateRepos: Boolean) {
        val clientId = settings.value.effectiveGitHubClientId
        if (clientId.isBlank()) {
            showMessage("Configure your GitHub OAuth App Client ID in Settings or AI Studio Secrets first.")
            return
        }
        deviceFlowJob?.cancel()
        viewModelScope.launch {
            setBusy(true, "Requesting GitHub Device Flow code...")
            oauthManager.startDeviceFlow(clientId, includePrivateRepos).onSuccess { challenge ->
                _opState.update {
                    it.copy(
                        deviceFlowChallenge = challenge,
                        isPollingDeviceFlow = true
                    )
                }
                setBusy(false)
                deviceFlowJob = viewModelScope.launch {
                    when (val outcome = oauthManager.pollDeviceFlowUntilAuthorized(clientId, challenge)) {
                        is GitHubAuthOutcome.Success -> {
                            _opState.update {
                                it.copy(
                                    githubSession = outcome.session,
                                    deviceFlowChallenge = null,
                                    isPollingDeviceFlow = false,
                                    snackbarMessage = "Connected GitHub as @${outcome.session.username}"
                                )
                            }
                            loadGitHubRepositories()
                        }
                        is GitHubAuthOutcome.Error -> {
                            _opState.update {
                                it.copy(
                                    deviceFlowChallenge = null,
                                    isPollingDeviceFlow = false,
                                    snackbarMessage = outcome.message
                                )
                            }
                        }
                    }
                }
            }.onFailure { err ->
                setBusy(false)
                showMessage(err.localizedMessage ?: "Failed to start GitHub OAuth.")
            }
        }
    }

    fun cancelDeviceFlow() {
        deviceFlowJob?.cancel()
        _opState.update { it.copy(deviceFlowChallenge = null, isPollingDeviceFlow = false) }
    }

    fun launchGitHubPkceBrowser(includePrivateRepos: Boolean) {
        val clientId = settings.value.effectiveGitHubClientId
        if (clientId.isBlank()) {
            showMessage("Configure your GitHub OAuth App Client ID in Settings or AI Studio Secrets first.")
            return
        }
        val url = oauthManager.buildPkceAuthorizeUrl(clientId, includePrivateRepos)
        installer.openHttpsUrlInBrowser(url).onFailure {
            showMessage("Could not open browser for GitHub authorization.")
        }
    }

    fun handleGitHubOAuthCallback(callbackUri: Uri) {
        val clientId = settings.value.effectiveGitHubClientId
        if (clientId.isBlank()) {
            showMessage("Missing GitHub Client ID to complete OAuth callback.")
            return
        }
        viewModelScope.launch {
            setBusy(true, "Completing GitHub OAuth PKCE verification...")
            when (val outcome = oauthManager.completePkceCallback(clientId, callbackUri)) {
                is GitHubAuthOutcome.Success -> {
                    _opState.update {
                        it.copy(
                            githubSession = outcome.session,
                            snackbarMessage = "Connected GitHub as @${outcome.session.username}"
                        )
                    }
                    loadGitHubRepositories()
                }
                is GitHubAuthOutcome.Error -> {
                    showMessage(outcome.message)
                }
            }
            setBusy(false)
        }
    }

    fun loadGitHubRepositories(silent: Boolean = false) {
        viewModelScope.launch {
            _opState.update { it.copy(isLoadingRepos = true) }
            repository.fetchGitHubRepositories().onSuccess { repos ->
                _opState.update {
                    it.copy(
                        githubRepos = repos,
                        githubSession = repository.credentialStore.getGitHubSession(),
                        isLoadingRepos = false
                    )
                }
                if (!silent) showMessage("Synced ${repos.size} GitHub repositories.")
            }.onFailure { err ->
                _opState.update { it.copy(isLoadingRepos = false) }
                if (!silent) showMessage(err.localizedMessage ?: "Could not load repositories.")
            }
        }
    }

    fun linkRepoToApp(appId: Long, repo: GitHubRepoDto) {
        viewModelScope.launch {
            repository.linkRepositoryToApp(appId, repo).onSuccess {
                showMessage("Linked ${repo.fullName}. Refreshing GitHub Actions...")
                refreshBuildsForApp(appId)
            }.onFailure {
                showMessage(it.localizedMessage ?: "Failed to link repository.")
            }
        }
    }

    fun createAppFromGitHubRepo(repo: GitHubRepoDto, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val result = repository.createAppFolder(
                name = repo.name.replace('-', ' ').replace('_', ' ')
                    .replaceFirstChar { it.uppercase() },
                description = repo.description ?: "Connected GitHub repository ${repo.fullName}",
                aiStudioUrl = "",
                githubRepoUrl = repo.htmlUrl,
                projectType = ProjectType.NATIVE_ANDROID,
                versionName = "1.0.0",
                notes = "Created directly from GitHub repository ${repo.fullName}.",
                iconUri = null,
                iconColorHex = "#3B82F6",
                iconSymbolKey = "code",
                isPrivateRepo = repo.isPrivate
            )
            result.onSuccess { newId ->
                refreshStorageUsage()
                showMessage("Created app folder for ${repo.name}")
                refreshBuildsForApp(newId)
                onCreated(newId)
            }.onFailure {
                showMessage(it.localizedMessage ?: "Could not create app folder from repo.")
            }
        }
    }

    fun disconnectGitHub() {
        deviceFlowJob?.cancel()
        repository.credentialStore.clearGitHubSession()
        _opState.update {
            it.copy(
                githubSession = null,
                githubRepos = emptyList(),
                deviceFlowChallenge = null,
                isPollingDeviceFlow = false,
                snackbarMessage = "Disconnected GitHub account and revoked local encrypted token."
            )
        }
    }

    // --- Google Account Sign-In (Credential Manager) ---

    fun signInWithGoogle(activityContext: Context) {
        val webClientId = settings.value.effectiveGoogleWebClientId
        if (webClientId.isBlank()) {
            showMessage("Set GOOGLE_WEB_CLIENT_ID in AI Studio Secrets for native Google Sign-In, or sign in via Google AI Studio in your browser.")
            return
        }
        viewModelScope.launch {
            try {
                val credentialManager = CredentialManager.create(activityContext)
                val googleOption = GetSignInWithGoogleOption.Builder(webClientId).build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleOption)
                    .build()
                val result = credentialManager.getCredential(activityContext, request)
                val googleCred = GoogleIdTokenCredential.createFrom(result.credential.data)
                val session = GoogleAccountSession(
                    email = googleCred.id,
                    displayName = googleCred.displayName ?: googleCred.id,
                    photoUrl = googleCred.profilePictureUri?.toString()
                )
                repository.credentialStore.saveGoogleAccount(session)
                _opState.update {
                    it.copy(
                        googleAccount = session,
                        snackbarMessage = "Connected Google account: ${session.email}"
                    )
                }
            } catch (e: Exception) {
                showMessage("Google Sign-In not completed: ${e.localizedMessage ?: "Cancelled"}")
            }
        }
    }

    fun disconnectGoogleAccount() {
        repository.credentialStore.clearGoogleAccount()
        _opState.update {
            it.copy(
                googleAccount = null,
                snackbarMessage = "Disconnected Google account from AI Studio App Manager."
            )
        }
    }

    // --- GitHub Actions Refresh & Artifact / APK Pipelines ---

    fun refreshBuildsForApp(appId: Long) {
        viewModelScope.launch {
            setBusy(true, "Fetching GitHub Actions builds & artifacts...")
            repository.refreshBuildsForApp(appId).onSuccess { count ->
                showMessage("Synced $count GitHub Actions workflow run(s).")
            }.onFailure { err ->
                showMessage(err.localizedMessage ?: "Failed to refresh builds.")
            }
            setBusy(false)
        }
    }

    fun refreshAllConnectedRepos() {
        viewModelScope.launch {
            _opState.update { it.copy(isRefreshingAll = true) }
            repository.refreshAllConnectedApps().onSuccess { count ->
                if (_opState.value.githubSession != null) {
                    loadGitHubRepositories(silent = true)
                }
                showMessage(
                    if (count > 0) "Refreshed GitHub builds for $count connected app(s)."
                    else "No GitHub-linked apps found. Link a repository to sync builds."
                )
            }.onFailure { err ->
                showMessage(err.localizedMessage ?: "Refresh failed.")
            }
            _opState.update { it.copy(isRefreshingAll = false) }
        }
    }

    fun downloadArtifactZipOnly(artifact: ArtifactEntity) {
        viewModelScope.launch {
            setBusy(true, "Downloading artifact ZIP '${artifact.name}'...")
            repository.downloadArtifactZip(artifact).onSuccess { zipFile ->
                refreshStorageUsage()
                showMessage("Saved artifact ZIP: ${zipFile.name}")
            }.onFailure { err ->
                showMessage(err.localizedMessage ?: "Artifact ZIP download failed.")
            }
            setBusy(false)
        }
    }

    fun extractArtifactForInspection(artifact: ArtifactEntity) {
        viewModelScope.launch {
            setBusy(true, "Downloading & safely extracting '${artifact.name}'...")
            repository.extractAndInspectArtifact(artifact).onSuccess { outcome ->
                refreshStorageUsage()
                _opState.update {
                    it.copy(
                        latestExtractionOutcome = outcome,
                        activeArtifactForExtraction = artifact
                    )
                }
                val msg = when {
                    outcome.extractedApks.isNotEmpty() ->
                        "Found ${outcome.extractedApks.size} verified APK file(s) inside archive."
                    outcome.extractedAabs.isNotEmpty() ->
                        "Artifact contains an Android App Bundle (.aab), not a directly installable APK."
                    else -> "No APK files found inside '${artifact.name}'."
                }
                showMessage(msg)
            }.onFailure { err ->
                showMessage(err.localizedMessage ?: "Could not extract artifact.")
            }
            setBusy(false)
        }
    }

    fun clearExtractionOutcome() {
        _opState.update {
            it.copy(latestExtractionOutcome = null, activeArtifactForExtraction = null)
        }
    }

    fun saveExtractedApk(appId: Long, apkFile: File, buildRun: BuildRunEntity?, overwrite: Boolean = false) {
        viewModelScope.launch {
            val appEntity = allApps.value.firstOrNull { it.id == appId }
            if (appEntity != null && !overwrite && repository.folderManager.apkAlreadyExists(appEntity.folderName, apkFile.name)) {
                _opState.update {
                    it.copy(
                        pendingOverwrite = PendingOverwriteRequest(
                            appId = appId,
                            apkSourceFile = apkFile,
                            apkFileName = apkFile.name,
                            buildRun = buildRun
                        )
                    )
                }
                return@launch
            }
            setBusy(true, "Validating & saving ${apkFile.name}...")
            repository.saveExtractedApkToAppFolder(appId, apkFile, buildRun, overwrite).onSuccess { saved ->
                refreshStorageUsage()
                showMessage("Saved verified APK: ${saved.fileName}")
            }.onFailure { err ->
                showMessage(err.localizedMessage ?: "Could not save APK.")
            }
            setBusy(false)
        }
    }

    fun downloadLatestApkOneClick(appId: Long) {
        viewModelScope.launch {
            setBusy(true, "Checking latest build & downloading APK...")
            val result = repository.downloadLatestApkForApp(appId, overwriteIfExists = false)
            setBusy(false)
            refreshStorageUsage()
            if (result.requiresOverwriteConfirmation && result.pendingApkSourceFile != null && result.pendingApkFileName != null) {
                _opState.update {
                    it.copy(
                        pendingOverwrite = PendingOverwriteRequest(
                            appId = appId,
                            apkSourceFile = result.pendingApkSourceFile,
                            apkFileName = result.pendingApkFileName
                        )
                    )
                }
            } else {
                showMessage(result.message)
            }
        }
    }

    fun resolveOverwriteConfirmation(overwriteExisting: Boolean) {
        val pending = _opState.value.pendingOverwrite ?: return
        _opState.update { it.copy(pendingOverwrite = null) }
        viewModelScope.launch {
            setBusy(true, "Saving ${pending.apkFileName}...")
            repository.saveExtractedApkToAppFolder(
                appId = pending.appId,
                extractedApkFile = pending.apkSourceFile,
                buildRun = pending.buildRun,
                overwriteExisting = overwriteExisting
            ).onSuccess { saved ->
                refreshStorageUsage()
                showMessage("Saved verified APK: ${saved.fileName}")
            }.onFailure { err ->
                showMessage(err.localizedMessage ?: "Failed to save APK.")
            }
            setBusy(false)
        }
    }

    fun dismissOverwriteConfirmation() {
        _opState.update { it.copy(pendingOverwrite = null) }
    }

    // --- APK Installation (Confirmation + Permission Check) ---

    fun requestInstallApk(
        appId: Long,
        appName: String,
        apkFile: File,
        versionName: String = "",
        packageName: String = ""
    ) {
        when (val check = installer.checkApkReadyForInstall(apkFile)) {
            is ApkInstallCheckResult.ReadyToInstall -> {
                _opState.update {
                    it.copy(
                        pendingInstall = PendingInstallRequest(
                            appId = appId,
                            appName = appName,
                            apkFile = apkFile,
                            versionName = versionName,
                            packageName = packageName,
                            needsPermissionSettings = false
                        )
                    )
                }
            }
            is ApkInstallCheckResult.NeedsInstallPermission -> {
                _opState.update {
                    it.copy(
                        pendingInstall = PendingInstallRequest(
                            appId = appId,
                            appName = appName,
                            apkFile = apkFile,
                            versionName = versionName,
                            packageName = packageName,
                            needsPermissionSettings = true
                        )
                    )
                }
            }
            is ApkInstallCheckResult.InvalidApk -> {
                showMessage(check.reason)
            }
        }
    }

    fun confirmAndLaunchPackageInstaller() {
        val pending = _opState.value.pendingInstall ?: return
        _opState.update { it.copy(pendingInstall = null) }
        if (pending.needsPermissionSettings) {
            installer.openInstallUnknownAppsSettings().onFailure {
                showMessage("Open Android Settings to allow installing unknown apps for AI Studio App Manager.")
            }
            return
        }
        installer.launchOfficialPackageInstaller(pending.apkFile).onSuccess {
            viewModelScope.launch {
                repository.recordApkInstalled(pending.appId, pending.appName, pending.apkFile.name)
            }
        }.onFailure { err ->
            showMessage(err.localizedMessage ?: "Could not launch Android Package Installer.")
        }
    }

    fun dismissInstallDialog() {
        _opState.update { it.copy(pendingInstall = null) }
    }

    fun deleteApkVersion(apk: ApkVersionEntity) {
        viewModelScope.launch {
            repository.deleteApkVersion(apk).onSuccess {
                refreshStorageUsage()
                showMessage("Deleted ${apk.fileName}")
            }.onFailure {
                showMessage(it.localizedMessage ?: "Failed to delete APK.")
            }
        }
    }

    fun deleteDownloadRecord(record: DownloadRecordEntity, deleteFile: Boolean) {
        viewModelScope.launch {
            repository.deleteDownloadRecord(record, deleteFile).onSuccess {
                refreshStorageUsage()
                showMessage(if (deleteFile) "Deleted ${record.fileName}" else "Removed history entry")
            }
        }
    }

    fun clearDownloadHistory() {
        viewModelScope.launch {
            repository.clearDownloadHistoryOnly()
            showMessage("Cleared download history records. Downloaded APK files remain safe.")
        }
    }

    // --- Settings & Backup/Restore ---

    fun updateAiStudioUrl(url: String) {
        viewModelScope.launch {
            if (!com.example.security.UrlValidator.isValidHttpsUrl(url)) {
                showMessage("Google AI Studio URL must be a valid https:// address.")
                return@launch
            }
            settingsStore.setAiStudioUrl(url)
            showMessage("Updated Google AI Studio URL.")
        }
    }

    fun updateCustomStorageLocation(treeUri: Uri?) {
        viewModelScope.launch {
            if (treeUri == null) {
                settingsStore.setCustomStorageLocation(
                    null,
                    "App-Managed Vault (AI Studio App Manager/My Apps)"
                )
                showMessage("Reset to default app-managed storage vault.")
            } else {
                val label = treeUri.lastPathSegment?.substringAfter(':') ?: treeUri.toString()
                settingsStore.setCustomStorageLocation(treeUri.toString(), "Custom Folder: $label")
                showMessage("Configured SAF mirror folder: $label")
            }
        }
    }

    fun setWifiOnlyDownloads(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setWifiOnlyDownloads(enabled)
            val current = settings.value
            BuildRefreshWorker.scheduleOrCancel(
                app,
                current.autoRefreshEnabled,
                current.autoRefreshIntervalHours,
                enabled
            )
        }
    }

    fun setAutoRefresh(enabled: Boolean, intervalHours: Int = settings.value.autoRefreshIntervalHours) {
        viewModelScope.launch {
            settingsStore.setAutoRefresh(enabled, intervalHours)
            BuildRefreshWorker.scheduleOrCancel(
                app,
                enabled,
                intervalHours,
                settings.value.wifiOnlyDownloads
            )
            showMessage(
                if (enabled) "Automatic refresh scheduled every ${intervalHours}h."
                else "Automatic background refresh disabled."
            )
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setNotificationsEnabled(enabled)
        }
    }

    fun setCustomGitHubClientId(clientId: String) {
        viewModelScope.launch {
            settingsStore.setCustomGitHubClientId(clientId)
            showMessage("Saved GitHub OAuth Client ID.")
        }
    }

    fun exportBackup(destinationUri: Uri) {
        viewModelScope.launch {
            setBusy(true, "Exporting app records backup...")
            repository.backupRestoreManager.exportRecordsToUri(destinationUri).onSuccess { count ->
                showMessage("Backed up $count app record(s) (no credentials included).")
            }.onFailure { err ->
                showMessage(err.localizedMessage ?: "Backup export failed.")
            }
            setBusy(false)
        }
    }

    fun restoreBackup(sourceUri: Uri) {
        viewModelScope.launch {
            setBusy(true, "Restoring app records...")
            repository.backupRestoreManager.restoreRecordsFromUri(sourceUri).onSuccess { count ->
                refreshStorageUsage()
                showMessage("Restored $count app folder record(s).")
            }.onFailure { err ->
                showMessage(err.localizedMessage ?: "Backup restore failed.")
            }
            setBusy(false)
        }
    }

    fun openUrlInBrowser(url: String) {
        installer.openHttpsUrlInBrowser(url).onFailure {
            showMessage(it.localizedMessage ?: "Could not open URL.")
        }
    }

    private fun setBusy(busy: Boolean, message: String = "") {
        _opState.update { it.copy(isBusy = busy, busyMessage = message) }
    }
}
