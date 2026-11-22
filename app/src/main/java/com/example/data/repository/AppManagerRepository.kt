package com.example.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import com.example.data.backup.BackupRestoreManager
import com.example.data.github.GitHubApiService
import com.example.data.github.GitHubArtifactDto
import com.example.data.github.GitHubNetworkFactory
import com.example.data.github.GitHubRepoDto
import com.example.data.github.WorkflowRunDto
import com.example.data.local.ActivityLogEntity
import com.example.data.local.ActivityType
import com.example.data.local.ApkVersionEntity
import com.example.data.local.AppDatabase
import com.example.data.local.AppProjectEntity
import com.example.data.local.ArtifactEntity
import com.example.data.local.BuildRunEntity
import com.example.data.local.BuildStatus
import com.example.data.local.DownloadFileType
import com.example.data.local.DownloadRecordEntity
import com.example.data.local.DownloadStatus
import com.example.data.local.ExtractionStatus
import com.example.data.local.ProjectType
import com.example.data.local.SourceZipEntity
import com.example.data.preferences.SettingsDataStore
import com.example.data.storage.AppFolderManager
import com.example.data.storage.StorageUsageSummary
import com.example.security.SafeZipAndApkValidator
import com.example.security.SecureCredentialStore
import com.example.security.UrlValidator
import com.example.worker.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class ArtifactExtractionOutcome(
    val artifactZipFile: File,
    val extractedApks: List<File>,
    val extractedAabs: List<File>,
    val validationErrors: List<String> = emptyList()
)

data class OneClickApkResult(
    val success: Boolean,
    val apkVersion: ApkVersionEntity? = null,
    val requiresOverwriteConfirmation: Boolean = false,
    val pendingApkSourceFile: File? = null,
    val pendingApkFileName: String? = null,
    val isAabOnly: Boolean = false,
    val message: String
)

class AppManagerRepository(
    private val context: Context,
    private val database: AppDatabase,
    val folderManager: AppFolderManager,
    private val githubApi: GitHubApiService,
    val credentialStore: SecureCredentialStore,
    val settingsDataStore: SettingsDataStore,
    val backupRestoreManager: BackupRestoreManager
) {
    private val appDao = database.appProjectDao()
    private val zipDao = database.sourceZipDao()
    private val buildDao = database.buildRunDao()
    private val artifactDao = database.artifactDao()
    private val apkDao = database.apkVersionDao()
    private val downloadDao = database.downloadRecordDao()
    private val activityDao = database.activityLogDao()

    val allApps: Flow<List<AppProjectEntity>> = appDao.observeAllApps()
    val allZips: Flow<List<SourceZipEntity>> = zipDao.observeAllZips()
    val totalZipCount: Flow<Int> = zipDao.observeTotalZipCount()
    val allBuilds: Flow<List<BuildRunEntity>> = buildDao.observeAllBuilds()
    val allArtifacts: Flow<List<ArtifactEntity>> = artifactDao.observeAllArtifacts()
    val allApks: Flow<List<ApkVersionEntity>> = apkDao.observeAllApks()
    val allDownloads: Flow<List<DownloadRecordEntity>> = downloadDao.observeAllDownloads()
    val recentActivity: Flow<List<ActivityLogEntity>> = activityDao.observeRecentActivity()

    fun observeApp(appId: Long): Flow<AppProjectEntity?> = appDao.observeAppById(appId)
    fun observeZipsForApp(appId: Long): Flow<List<SourceZipEntity>> = zipDao.observeZipsForApp(appId)
    fun observeBuildsForApp(appId: Long): Flow<List<BuildRunEntity>> = buildDao.observeBuildsForApp(appId)
    fun observeArtifactsForApp(appId: Long): Flow<List<ArtifactEntity>> = artifactDao.observeArtifactsForApp(appId)
    fun observeArtifactsForBuild(buildRunId: Long): Flow<List<ArtifactEntity>> = artifactDao.observeArtifactsForBuild(buildRunId)
    fun observeApksForApp(appId: Long): Flow<List<ApkVersionEntity>> = apkDao.observeApksForApp(appId)
    fun observeDownloadsForApp(appId: Long): Flow<List<DownloadRecordEntity>> = downloadDao.observeDownloadsForApp(appId)

    suspend fun createAppFolder(
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
        isPrivateRepo: Boolean = false,
        isDemoData: Boolean = false
    ): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            val trimmedName = name.trim()
            require(trimmedName.isNotEmpty()) { "Application name is required." }

            if (aiStudioUrl.isNotBlank() && !UrlValidator.isValidHttpsUrl(aiStudioUrl)) {
                error("Google AI Studio Project URL must be a valid HTTPS link.")
            }

            val parsedRepo = if (githubRepoUrl.isNotBlank()) {
                UrlValidator.parseGitHubRepo(githubRepoUrl)
                    ?: error("GitHub Repository URL must be a valid https://github.com/owner/repo link.")
            } else null

            val folderPaths = folderManager.ensureAppFolderStructure(trimmedName)
            val now = System.currentTimeMillis()
            val entity = AppProjectEntity(
                name = trimmedName,
                folderName = folderPaths.rootDir.name,
                iconUri = iconUri,
                iconColorHex = iconColorHex,
                iconSymbolKey = iconSymbolKey,
                description = description.trim(),
                aiStudioUrl = aiStudioUrl.trim(),
                githubRepoUrl = parsedRepo?.canonicalHttpsUrl ?: "",
                githubOwner = parsedRepo?.owner ?: "",
                githubRepoName = parsedRepo?.repo ?: "",
                isPrivateRepo = isPrivateRepo,
                projectType = projectType,
                versionName = versionName.trim().ifBlank { "1.0.0" },
                notes = notes.trim(),
                isDemoData = isDemoData,
                createdAt = now,
                updatedAt = now
            )
            val newId = appDao.insertApp(entity)
            logActivity(
                appId = newId,
                appName = trimmedName,
                type = ActivityType.APP_ADDED,
                title = if (isDemoData) "Demo folder created" else "Created app folder",
                subtitle = "$trimmedName (${projectType.label})"
            )
            newId
        }
    }

    suspend fun updateAppFolder(
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
        isPrivateRepo: Boolean = existing.isPrivateRepo
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val trimmedName = name.trim()
            require(trimmedName.isNotEmpty()) { "Application name is required." }
            if (aiStudioUrl.isNotBlank() && !UrlValidator.isValidHttpsUrl(aiStudioUrl)) {
                error("Google AI Studio URL must start with https://.")
            }
            val parsedRepo = if (githubRepoUrl.isNotBlank()) {
                UrlValidator.parseGitHubRepo(githubRepoUrl)
                    ?: error("Enter a valid GitHub repository URL (https://github.com/owner/repo).")
            } else null

            val finalFolderName = if (existing.name != trimmedName) {
                folderManager.renameAppFolder(existing.folderName, trimmedName)
            } else {
                folderManager.ensureAppFolderStructure(existing.folderName).rootDir.name
            }

            appDao.updateApp(
                existing.copy(
                    name = trimmedName,
                    folderName = finalFolderName,
                    iconUri = iconUri,
                    iconColorHex = iconColorHex,
                    iconSymbolKey = iconSymbolKey,
                    description = description.trim(),
                    aiStudioUrl = aiStudioUrl.trim(),
                    githubRepoUrl = parsedRepo?.canonicalHttpsUrl ?: "",
                    githubOwner = parsedRepo?.owner ?: "",
                    githubRepoName = parsedRepo?.repo ?: "",
                    isPrivateRepo = isPrivateRepo,
                    projectType = projectType,
                    versionName = versionName.trim().ifBlank { existing.versionName },
                    notes = notes.trim(),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun linkRepositoryToApp(appId: Long, repo: GitHubRepoDto): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val app = appDao.getAppById(appId) ?: error("App folder not found.")
                val owner = repo.owner?.login ?: repo.fullName.substringBefore('/')
                val repoName = repo.name.ifBlank { repo.fullName.substringAfter('/') }
                val updated = app.copy(
                    githubRepoUrl = repo.htmlUrl.ifBlank { "https://github.com/$owner/$repoName" },
                    githubOwner = owner,
                    githubRepoName = repoName,
                    isPrivateRepo = repo.isPrivate,
                    updatedAt = System.currentTimeMillis()
                )
                appDao.updateApp(updated)
                logActivity(
                    appId = app.id,
                    appName = app.name,
                    type = ActivityType.GITHUB_SYNCED,
                    title = "Linked GitHub repository",
                    subtitle = "$owner/$repoName (${if (repo.isPrivate) "Private" else "Public"})"
                )
            }
        }

    suspend fun deleteAppFolder(app: AppProjectEntity): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            folderManager.deleteAppFolder(app.folderName)
            appDao.deleteApp(app)
        }
    }

    suspend fun importSourceZip(
        appId: Long,
        uri: Uri,
        versionLabel: String
    ): Result<SourceZipEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val app = appDao.getAppById(appId) ?: error("Select a valid app folder first.")
            val settings = settingsDataStore.getSnapshot()
            val imported = folderManager.importSourceZipFromUri(
                folderName = app.folderName,
                sourceUri = uri,
                customTreeUri = settings.customStorageTreeUri
            ).getOrThrow()

            val zipEntity = SourceZipEntity(
                appId = app.id,
                versionLabel = versionLabel.trim().ifBlank { app.versionName.ifBlank { "v1.0" } },
                fileName = imported.fileName,
                localFilePath = imported.localFile.absolutePath,
                fileSizeBytes = imported.fileSizeBytes,
                extractionStatus = ExtractionStatus.NOT_EXTRACTED,
                uploadedAt = System.currentTimeMillis()
            )
            val insertedId = zipDao.insertZip(zipEntity)
            appDao.updateApp(
                app.copy(
                    hasSourceZip = true,
                    updatedAt = System.currentTimeMillis()
                )
            )
            downloadDao.insertRecord(
                DownloadRecordEntity(
                    appId = app.id,
                    appName = app.name,
                    fileName = imported.fileName,
                    fileType = DownloadFileType.SOURCE_ZIP,
                    versionName = zipEntity.versionLabel,
                    fileSizeBytes = imported.fileSizeBytes,
                    localFilePath = imported.localFile.absolutePath,
                    status = DownloadStatus.COMPLETED,
                    sourceUrl = app.aiStudioUrl
                )
            )
            logActivity(
                appId = app.id,
                appName = app.name,
                type = ActivityType.ZIP_IMPORTED,
                title = "Imported source ZIP",
                subtitle = "${imported.fileName} (${formatBytes(imported.fileSizeBytes)})"
            )
            zipEntity.copy(id = insertedId)
        }
    }

    suspend fun extractSourceZip(zip: SourceZipEntity): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            zipDao.updateZip(zip.copy(extractionStatus = ExtractionStatus.EXTRACTING))
            val file = File(zip.localFilePath)
            val result = folderManager.extractSourceZip(file)
            if (!result.success) {
                zipDao.updateZip(zip.copy(extractionStatus = ExtractionStatus.FAILED))
                error(result.errorMessage ?: "Extraction failed.")
            }
            val extractDir = File(file.parentFile, "extracted_${file.nameWithoutExtension}")
            zipDao.updateZip(
                zip.copy(
                    extractedDirPath = extractDir.absolutePath,
                    extractionStatus = ExtractionStatus.EXTRACTED
                )
            )
            "Safely extracted ${result.extractedFiles.size} file(s). Original ZIP preserved."
        }
    }

    suspend fun importLocalApk(appId: Long, uri: Uri): Result<ApkVersionEntity> =
        withContext(Dispatchers.IO) {
            runCatching {
                val app = appDao.getAppById(appId) ?: error("App folder not found.")
                val imported = folderManager.importApkFromUri(app.folderName, uri).getOrThrow()
                val validation = SafeZipAndApkValidator.validateApkFile(imported.localFile, context)
                val apkRecord = persistVerifiedApkRecord(
                    app = app,
                    savedFile = imported.localFile,
                    packageName = validation.packageName ?: app.packageName,
                    versionName = validation.versionName ?: app.versionName,
                    versionCode = validation.versionCode ?: 1L,
                    buildNumber = "Local Import",
                    gitCommitSha = "",
                    buildDate = System.currentTimeMillis()
                )
                apkRecord
            }
        }

    suspend fun fetchGitHubRepositories(): Result<List<GitHubRepoDto>> = withContext(Dispatchers.IO) {
        runCatching {
            val token = credentialStore.getGitHubAccessToken()
                ?: error("Connect your GitHub account first to browse your repositories.")
            val response = githubApi.getUserRepositories(authorization = "Bearer $token")
            if (!response.isSuccessful) {
                error(mapGitHubHttpError(response.code(), "Failed to load repositories"))
            }
            credentialStore.updateGitHubLastSync(System.currentTimeMillis())
            response.body().orEmpty()
        }
    }

    suspend fun refreshBuildsForApp(appId: Long): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val app = appDao.getAppById(appId) ?: error("App folder not found.")
            if (app.githubOwner.isBlank() || app.githubRepoName.isBlank()) {
                error("No GitHub repository linked to ${app.name}.")
            }

            val token = credentialStore.getGitHubAccessToken()
            val authHeader = token?.let { "Bearer $it" }
            if (app.isPrivateRepo && authHeader == null) {
                error("Connect GitHub to access private repository ${app.githubOwner}/${app.githubRepoName}.")
            }

            val runsResp = githubApi.getWorkflowRuns(
                owner = app.githubOwner,
                repo = app.githubRepoName,
                authorization = authHeader
            )
            if (!runsResp.isSuccessful) {
                error(mapGitHubHttpError(runsResp.code(), "Could not fetch GitHub Actions runs"))
            }

            val runDtos = runsResp.body()?.workflowRuns.orEmpty()
            var apkAvailableOnRepo = false
            var latestStatus = BuildStatus.NONE
            var latestWorkflow = ""
            var latestDate = 0L

            for ((index, dto) in runDtos.withIndex()) {
                val mappedStatus = mapGitHubStatus(dto.status, dto.conclusion)
                val createdMs = parseIso8601(dto.createdAt)
                val updatedMs = parseIso8601(dto.updatedAt)
                val startedMs = parseIso8601(dto.runStartedAt).takeIf { it > 0L } ?: createdMs
                val durationSec = if (updatedMs > startedMs) (updatedMs - startedMs) / 1000L else 0L

                if (index == 0) {
                    latestStatus = mappedStatus
                    latestWorkflow = dto.name ?: dto.displayTitle ?: "Workflow"
                    latestDate = createdMs
                }

                // Fetch artifacts for top 6 runs (respecting GitHub API rate limits)
                var artifactsForRun: List<GitHubArtifactDto> = emptyList()
                if (index < 6 && mappedStatus == BuildStatus.SUCCESS) {
                    val artResp = githubApi.getRunArtifacts(
                        owner = app.githubOwner,
                        repo = app.githubRepoName,
                        runId = dto.id,
                        authorization = authHeader
                    )
                    if (artResp.isSuccessful) {
                        artifactsForRun = artResp.body()?.artifacts.orEmpty()
                    }
                }

                val hasLikelyApk = artifactsForRun.any {
                    !it.expired && !it.name.contains("aab", ignoreCase = true)
                }
                val hasAabOnly = artifactsForRun.isNotEmpty() && artifactsForRun.all {
                    it.name.contains("aab", ignoreCase = true) || it.name.contains("bundle", ignoreCase = true)
                }
                if (hasLikelyApk) apkAvailableOnRepo = true

                val buildEntity = BuildRunEntity(
                    appId = app.id,
                    githubRunId = dto.id,
                    workflowName = dto.name ?: dto.displayTitle ?: "GitHub Workflow",
                    branch = dto.headBranch ?: "main",
                    commitSha = dto.headSha?.take(7) ?: "",
                    commitMessage = dto.headCommit?.message?.lineSequence()?.firstOrNull().orEmpty(),
                    status = mappedStatus,
                    conclusion = dto.conclusion ?: dto.status ?: "",
                    durationSeconds = durationSec,
                    htmlUrl = dto.htmlUrl ?: "${app.githubRepoUrl}/actions/runs/${dto.id}",
                    artifactCount = artifactsForRun.size,
                    hasApkArtifact = hasLikelyApk,
                    hasAabOnly = hasAabOnly,
                    createdAt = createdMs,
                    updatedAt = updatedMs
                )
                val runLocalId = buildDao.upsertBuild(buildEntity)

                if (artifactsForRun.isNotEmpty()) {
                    val artifactEntities = artifactsForRun.map { art ->
                        ArtifactEntity(
                            appId = app.id,
                            buildRunId = runLocalId,
                            githubArtifactId = art.id,
                            name = art.name,
                            sizeInBytes = art.sizeInBytes,
                            archiveDownloadUrl = art.archiveDownloadUrl,
                            expired = art.expired || isExpired(parseIso8601(art.expiresAt)),
                            createdAt = parseIso8601(art.createdAt),
                            expiresAt = parseIso8601(art.expiresAt),
                            isAabOnly = art.name.contains("aab", ignoreCase = true)
                        )
                    }
                    artifactDao.upsertArtifacts(artifactEntities)
                }
            }

            appDao.updateApp(
                app.copy(
                    latestBuildStatus = latestStatus,
                    latestBuildWorkflowName = latestWorkflow,
                    latestBuildDate = latestDate,
                    hasApkAvailable = apkAvailableOnRepo,
                    updatedAt = System.currentTimeMillis()
                )
            )
            logActivity(
                appId = app.id,
                appName = app.name,
                type = ActivityType.BUILD_UPDATED,
                title = "Refreshed Actions builds",
                subtitle = "${runDtos.size} run(s) • Latest: ${latestStatus.label}"
            )
            runDtos.size
        }
    }

    suspend fun refreshAllConnectedApps(): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val apps = appDao.getAllAppsSnapshot()
                .filter { it.githubOwner.isNotBlank() && it.githubRepoName.isNotBlank() }
            var refreshed = 0
            for (app in apps) {
                if (refreshBuildsForApp(app.id).isSuccess) {
                    refreshed++
                }
            }
            if (credentialStore.getGitHubAccessToken() != null) {
                credentialStore.updateGitHubLastSync(System.currentTimeMillis())
            }
            refreshed
        }
    }

    /**
     * Downloads a GitHub Actions artifact ZIP securely using authorized HTTPS access.
     */
    suspend fun downloadArtifactZip(artifact: ArtifactEntity): Result<File> =
        withContext(Dispatchers.IO) {
            runCatching {
                val app = appDao.getAppById(artifact.appId) ?: error("App folder not found.")
                checkWifiOnlyConstraint()

                if (artifact.expired || isExpired(artifact.expiresAt)) {
                    recordFailedDownload(
                        app = app,
                        fileName = "${artifact.name}.zip",
                        fileType = DownloadFileType.ARTIFACT_ZIP,
                        status = DownloadStatus.EXPIRED,
                        errorMessage = "GitHub Actions artifact has expired and is no longer downloadable.",
                        sourceUrl = artifact.archiveDownloadUrl
                    )
                    error("This GitHub Actions artifact has expired.")
                }

                val token = credentialStore.getGitHubAccessToken()
                    ?: error("GitHub sign-in is required by GitHub API to download Actions artifacts.")

                if (!UrlValidator.isValidHttpsUrl(artifact.archiveDownloadUrl)) {
                    error("Invalid artifact download URL.")
                }

                val paths = folderManager.ensureAppFolderStructure(app.folderName)
                val safeZipName = UrlValidator.sanitizeFileName(
                    "${artifact.name}_${artifact.githubArtifactId}.zip",
                    "artifact_${artifact.githubArtifactId}.zip"
                )
                val destZipFile = File(paths.buildArtifactsDir, safeZipName)

                val response = GitHubNetworkFactory.executeAuthorizedArtifactRequest(
                    archiveDownloadUrl = artifact.archiveDownloadUrl,
                    bearerToken = token
                )
                response.use { resp ->
                    if (!resp.isSuccessful) {
                        val msg = mapGitHubHttpError(resp.code, "Artifact download failed")
                        recordFailedDownload(
                            app = app,
                            fileName = safeZipName,
                            fileType = DownloadFileType.ARTIFACT_ZIP,
                            status = if (resp.code == 410) DownloadStatus.EXPIRED else DownloadStatus.FAILED,
                            errorMessage = msg,
                            sourceUrl = artifact.archiveDownloadUrl
                        )
                        error(msg)
                    }
                    val body = resp.body ?: error("Empty response from artifact download.")
                    body.byteStream().use { input ->
                        FileOutputStream(destZipFile).use { output ->
                            input.copyTo(output, bufferSize = 8192)
                        }
                    }
                }

                if (!SafeZipAndApkValidator.hasValidZipMagicHeader(destZipFile)) {
                    destZipFile.delete()
                    val msg = "Downloaded artifact is not a valid ZIP file."
                    recordFailedDownload(
                        app = app,
                        fileName = safeZipName,
                        fileType = DownloadFileType.ARTIFACT_ZIP,
                        status = DownloadStatus.FAILED,
                        errorMessage = msg,
                        sourceUrl = artifact.archiveDownloadUrl
                    )
                    error(msg)
                }

                artifactDao.updateArtifact(artifact.copy(localZipPath = destZipFile.absolutePath))
                downloadDao.insertRecord(
                    DownloadRecordEntity(
                        appId = app.id,
                        appName = app.name,
                        fileName = destZipFile.name,
                        fileType = DownloadFileType.ARTIFACT_ZIP,
                        versionName = app.versionName,
                        fileSizeBytes = destZipFile.length(),
                        localFilePath = destZipFile.absolutePath,
                        status = DownloadStatus.COMPLETED,
                        sourceUrl = artifact.archiveDownloadUrl
                    )
                )
                destZipFile
            }
        }

    /**
     * Safely extracts a downloaded artifact ZIP (with Zip Slip protection), locates .apk and .aab files,
     * and validates each discovered APK. Never renames .aab to .apk.
     */
    suspend fun extractAndInspectArtifact(artifact: ArtifactEntity): Result<ArtifactExtractionOutcome> =
        withContext(Dispatchers.IO) {
            runCatching {
                val app = appDao.getAppById(artifact.appId) ?: error("App folder not found.")
                val zipFile = artifact.localZipPath?.let { File(it) }
                    ?.takeIf { it.exists() }
                    ?: downloadArtifactZip(artifact).getOrThrow()

                val paths = folderManager.ensureAppFolderStructure(app.folderName)
                val extractDir = File(
                    paths.buildArtifactsDir,
                    "extracted_${artifact.githubArtifactId}"
                ).apply {
                    if (exists()) deleteRecursively()
                    mkdirs()
                }

                val extraction = SafeZipAndApkValidator.extractZipSafely(zipFile, extractDir)
                if (!extraction.success) {
                    error(extraction.errorMessage ?: "Artifact ZIP extraction failed.")
                }

                val validApks = mutableListOf<File>()
                val validationErrors = mutableListOf<String>()
                for (apkFile in extraction.apkFiles) {
                    val check = SafeZipAndApkValidator.validateApkFile(apkFile, context)
                    if (check.isValidApk) {
                        validApks.add(apkFile)
                    } else {
                        validationErrors.add("${apkFile.name}: ${check.errorMessage}")
                    }
                }

                val isAabOnly = validApks.isEmpty() && extraction.aabFiles.isNotEmpty()
                val validationSummary = when {
                    validApks.isNotEmpty() -> "Found ${validApks.size} verified APK file(s)."
                    isAabOnly -> "Contains Android App Bundle (.aab) only — not directly installable as an APK."
                    validationErrors.isNotEmpty() -> validationErrors.first()
                    else -> "No APK file found inside this artifact archive."
                }

                artifactDao.updateArtifact(
                    artifact.copy(
                        localZipPath = zipFile.absolutePath,
                        extractedDirPath = extractDir.absolutePath,
                        extractedApkPath = validApks.firstOrNull()?.absolutePath,
                        isAabOnly = isAabOnly,
                        validationMessage = validationSummary
                    )
                )

                ArtifactExtractionOutcome(
                    artifactZipFile = zipFile,
                    extractedApks = validApks,
                    extractedAabs = extraction.aabFiles,
                    validationErrors = validationErrors
                )
            }
        }

    /**
     * Saves an extracted, validated APK into the app's APK/ folder and records its version history.
     */
    suspend fun saveExtractedApkToAppFolder(
        appId: Long,
        extractedApkFile: File,
        buildRun: BuildRunEntity?,
        overwriteExisting: Boolean
    ): Result<ApkVersionEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val app = appDao.getAppById(appId) ?: error("App folder not found.")
            val validation = SafeZipAndApkValidator.validateApkFile(extractedApkFile, context)
            if (!validation.isValidApk) {
                error(validation.errorMessage ?: "APK verification failed.")
            }

            val settings = settingsDataStore.getSnapshot()
            val savedFile = folderManager.saveVerifiedApk(
                folderName = app.folderName,
                sourceApkFile = extractedApkFile,
                desiredFileName = extractedApkFile.name,
                overwrite = overwriteExisting,
                customTreeUri = settings.customStorageTreeUri
            )

            val apkVersion = persistVerifiedApkRecord(
                app = app,
                savedFile = savedFile,
                packageName = validation.packageName ?: app.packageName,
                versionName = validation.versionName ?: app.versionName,
                versionCode = validation.versionCode ?: 1L,
                buildNumber = buildRun?.githubRunId?.toString() ?: "",
                gitCommitSha = buildRun?.commitSha ?: "",
                buildDate = buildRun?.createdAt ?: System.currentTimeMillis()
            )

            if (settings.notificationsEnabled) {
                NotificationHelper.postBuildNotification(
                    context = context,
                    title = "APK Ready: ${app.name}",
                    message = "${savedFile.name} (${formatBytes(savedFile.length())}) verified & saved."
                )
            }
            apkVersion
        }
    }

    /**
     * One-Click "DOWNLOAD LATEST APK" pipeline:
     * Checks latest successful build -> locates APK artifact -> downloads ZIP -> safely extracts
     * -> validates APK signature/header -> saves into app's APK/ folder (asking confirmation on overwrite).
     */
    suspend fun downloadLatestApkForApp(
        appId: Long,
        overwriteIfExists: Boolean = false
    ): OneClickApkResult = withContext(Dispatchers.IO) {
        val app = appDao.getAppById(appId)
            ?: return@withContext OneClickApkResult(false, message = "App folder not found.")

        if (app.githubOwner.isBlank() || app.githubRepoName.isBlank()) {
            return@withContext OneClickApkResult(
                success = false,
                message = "No APK available for this project. Please complete an Android APK build first."
            )
        }

        // Refresh builds first if connected
        refreshBuildsForApp(app.id)

        val builds = buildDao.getBuildsForApp(app.id)
        val latestSuccess = builds.firstOrNull { it.status == BuildStatus.SUCCESS }
            ?: return@withContext OneClickApkResult(
                success = false,
                message = "No APK available for this project. Please complete an Android APK build first."
            )

        val artifacts = artifactDao.getArtifactsForBuild(latestSuccess.id)
            .filter { !it.expired && !isExpired(it.expiresAt) }

        if (artifacts.isEmpty()) {
            return@withContext OneClickApkResult(
                success = false,
                message = "No APK available for this project. Please complete an Android APK build first."
            )
        }

        val preferredArtifact = artifacts.firstOrNull {
            it.name.contains("apk", ignoreCase = true)
        } ?: artifacts.first()

        val extractionResult = extractAndInspectArtifact(preferredArtifact)
        if (extractionResult.isFailure) {
            return@withContext OneClickApkResult(
                success = false,
                message = extractionResult.exceptionOrNull()?.localizedMessage
                    ?: "Failed to download or extract APK artifact."
            )
        }

        val outcome = extractionResult.getOrThrow()
        if (outcome.extractedApks.isEmpty()) {
            if (outcome.extractedAabs.isNotEmpty()) {
                return@withContext OneClickApkResult(
                    success = false,
                    isAabOnly = true,
                    message = "Latest artifact contains only an Android App Bundle (.aab) and cannot be installed directly as an APK. Build an .apk artifact in GitHub Actions."
                )
            }
            return@withContext OneClickApkResult(
                success = false,
                message = "No APK available for this project. Please complete an Android APK build first."
            )
        }

        val apkFile = outcome.extractedApks.first()
        if (!overwriteIfExists && folderManager.apkAlreadyExists(app.folderName, apkFile.name)) {
            return@withContext OneClickApkResult(
                success = false,
                requiresOverwriteConfirmation = true,
                pendingApkSourceFile = apkFile,
                pendingApkFileName = apkFile.name,
                message = "An APK named '${apkFile.name}' already exists in this app folder. Confirm overwrite or save as a new copy?"
            )
        }

        val saved = saveExtractedApkToAppFolder(
            appId = app.id,
            extractedApkFile = apkFile,
            buildRun = latestSuccess,
            overwriteExisting = overwriteIfExists
        )

        return@withContext if (saved.isSuccess) {
            val version = saved.getOrThrow()
            OneClickApkResult(
                success = true,
                apkVersion = version,
                message = "Downloaded & verified ${version.fileName} (${formatBytes(version.fileSizeBytes)})."
            )
        } else {
            OneClickApkResult(
                success = false,
                message = saved.exceptionOrNull()?.localizedMessage ?: "Failed to save APK."
            )
        }
    }

    suspend fun deleteApkVersion(apk: ApkVersionEntity): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            folderManager.deleteFileSafely(apk.localFilePath)
            apkDao.deleteApkById(apk.id)
            val remaining = apkDao.getApksForApp(apk.appId)
            val app = appDao.getAppById(apk.appId)
            if (app != null) {
                appDao.updateApp(
                    app.copy(
                        hasApkDownloaded = remaining.isNotEmpty(),
                        latestApkVersion = remaining.firstOrNull()?.versionName ?: "",
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    suspend fun deleteDownloadRecord(record: DownloadRecordEntity, deleteLocalFile: Boolean): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (deleteLocalFile && record.localFilePath.isNotBlank()) {
                    folderManager.deleteFileSafely(record.localFilePath)
                }
                downloadDao.deleteRecordById(record.id)
            }
        }

    suspend fun clearDownloadHistoryOnly() = withContext(Dispatchers.IO) {
        downloadDao.clearAllHistory()
    }

    suspend fun recordApkInstalled(appId: Long, appName: String, fileName: String) {
        logActivity(
            appId = appId,
            appName = appName,
            type = ActivityType.APK_INSTALLED,
            title = "Opened Android Package Installer",
            subtitle = fileName
        )
    }

    suspend fun getStorageUsage(): StorageUsageSummary = folderManager.calculateStorageUsage()

    /**
     * Loads clearly labeled DEMO DATA sample app folders (no fake GitHub builds or fake APK downloads)
     * so the user can explore the folder structure immediately if desired.
     */
    suspend fun loadDemoSampleFolders(): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val existing = appDao.getAllAppsSnapshot()
            if (existing.any { it.isDemoData }) return@runCatching 0

            val samples = listOf(
                Triple(
                    "VIRAL REEL AI",
                    "AI short-form video script & hook planner created in Google AI Studio.",
                    ProjectType.NATIVE_ANDROID
                ),
                Triple(
                    "APPVAULT HUB",
                    "Personal app portfolio and release tracker built in Google AI Studio.",
                    ProjectType.NATIVE_ANDROID
                ),
                Triple(
                    "DAS OPTICS CUSTOMER BILL",
                    "Optical shop prescription & customer invoice organizer built in Google AI Studio.",
                    ProjectType.NATIVE_ANDROID
                )
            )

            samples.forEach { (title, desc, type) ->
                createAppFolder(
                    name = title,
                    description = desc,
                    aiStudioUrl = "https://aistudio.google.com/",
                    githubRepoUrl = "",
                    projectType = type,
                    versionName = "1.0.0",
                    notes = "DEMO DATA: Sample folder demonstrating My Apps organization. Connect a real GitHub repository or import a real project ZIP to manage builds.",
                    iconUri = null,
                    iconColorHex = when (title) {
                        "VIRAL REEL AI" -> "#8B5CF6"
                        "APPVAULT HUB" -> "#3B82F6"
                        else -> "#06B6D4"
                    },
                    iconSymbolKey = when (title) {
                        "VIRAL REEL AI" -> "auto_awesome"
                        "APPVAULT HUB" -> "apps"
                        else -> "receipt"
                    },
                    isDemoData = true
                )
            }
            samples.size
        }
    }

    suspend fun clearDemoSampleFolders() = withContext(Dispatchers.IO) {
        val demos = appDao.getAllAppsSnapshot().filter { it.isDemoData }
        demos.forEach { folderManager.deleteAppFolder(it.folderName) }
        appDao.deleteAllDemoApps()
    }

    private suspend fun persistVerifiedApkRecord(
        app: AppProjectEntity,
        savedFile: File,
        packageName: String,
        versionName: String,
        versionCode: Long,
        buildNumber: String,
        gitCommitSha: String,
        buildDate: Long
    ): ApkVersionEntity {
        val latestZipVersion = zipDao.getZipsForApp(app.id).firstOrNull()?.versionLabel ?: ""
        apkDao.clearLatestFlag(app.id)
        val record = ApkVersionEntity(
            appId = app.id,
            fileName = savedFile.name,
            localFilePath = savedFile.absolutePath,
            fileSizeBytes = savedFile.length(),
            packageName = packageName,
            versionName = versionName.ifBlank { app.versionName },
            versionCode = versionCode,
            buildNumber = buildNumber,
            gitCommitSha = gitCommitSha,
            sourceZipVersion = latestZipVersion,
            buildDate = buildDate,
            downloadedAt = System.currentTimeMillis(),
            isLatest = true,
            isValidApk = true
        )
        val id = apkDao.insertApk(record)
        appDao.updateApp(
            app.copy(
                packageName = packageName.ifBlank { app.packageName },
                latestApkVersion = record.versionName,
                hasApkDownloaded = true,
                updatedAt = System.currentTimeMillis()
            )
        )
        downloadDao.insertRecord(
            DownloadRecordEntity(
                appId = app.id,
                appName = app.name,
                fileName = savedFile.name,
                fileType = DownloadFileType.APK,
                versionName = record.versionName,
                fileSizeBytes = savedFile.length(),
                localFilePath = savedFile.absolutePath,
                status = DownloadStatus.COMPLETED,
                sourceUrl = app.githubRepoUrl
            )
        )
        logActivity(
            appId = app.id,
            appName = app.name,
            type = ActivityType.APK_DOWNLOADED,
            title = "Downloaded verified APK",
            subtitle = "${savedFile.name} • v${record.versionName} (${formatBytes(savedFile.length())})"
        )
        return record.copy(id = id)
    }

    private suspend fun recordFailedDownload(
        app: AppProjectEntity,
        fileName: String,
        fileType: DownloadFileType,
        status: DownloadStatus,
        errorMessage: String,
        sourceUrl: String
    ) {
        downloadDao.insertRecord(
            DownloadRecordEntity(
                appId = app.id,
                appName = app.name,
                fileName = fileName,
                fileType = fileType,
                versionName = app.versionName,
                fileSizeBytes = 0L,
                localFilePath = "",
                status = status,
                errorMessage = errorMessage,
                sourceUrl = sourceUrl
            )
        )
        logActivity(
            appId = app.id,
            appName = app.name,
            type = ActivityType.DOWNLOAD_FAILED,
            title = if (status == DownloadStatus.EXPIRED) "Artifact expired" else "Download failed",
            subtitle = "$fileName — $errorMessage"
        )
    }

    private suspend fun logActivity(
        appId: Long?,
        appName: String,
        type: ActivityType,
        title: String,
        subtitle: String
    ) {
        activityDao.insertLog(
            ActivityLogEntity(
                appId = appId,
                appName = appName,
                type = type,
                title = title,
                subtitle = subtitle,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    private suspend fun checkWifiOnlyConstraint() {
        val settings = settingsDataStore.getSnapshot()
        if (!settings.wifiOnlyDownloads) return
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        val network = cm.activeNetwork
        val caps = cm.getNetworkCapabilities(network)
        val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        if (!isWifi) {
            error("Wi-Fi-only downloads is enabled in Settings. Connect to Wi-Fi or turn off Wi-Fi-only mode.")
        }
    }

    private fun mapGitHubStatus(status: String?, conclusion: String?): BuildStatus {
        return when (status?.lowercase()) {
            "queued", "waiting", "pending", "requested" -> BuildStatus.QUEUED
            "in_progress" -> BuildStatus.IN_PROGRESS
            "completed" -> when (conclusion?.lowercase()) {
                "success" -> BuildStatus.SUCCESS
                "cancelled", "skipped" -> BuildStatus.CANCELLED
                else -> BuildStatus.FAILED
            }
            else -> BuildStatus.NONE
        }
    }

    private fun mapGitHubHttpError(code: Int, prefix: String): String {
        return when (code) {
            401 -> "$prefix: GitHub session expired or unauthorized. Please reconnect GitHub."
            403 -> "$prefix: Access forbidden or GitHub API rate limit reached. Try again later."
            404 -> "$prefix: Repository or build resource not found (check repository URL or private access)."
            410 -> "$prefix: GitHub Actions artifact has expired."
            else -> "$prefix (HTTP $code)."
        }
    }

    private fun parseIso8601(raw: String?): Long {
        if (raw.isNullOrBlank()) return 0L
        return runCatching {
            val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            fmt.parse(raw)?.time ?: 0L
        }.getOrDefault(0L)
    }

    private fun isExpired(expiresAtMs: Long): Boolean {
        return expiresAtMs > 0L && System.currentTimeMillis() > expiresAtMs
    }

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0L) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB")
            var value = bytes.toDouble()
            var idx = 0
            while (value >= 1024.0 && idx < units.lastIndex) {
                value /= 1024.0
                idx++
            }
            return if (idx == 0) "${bytes} B" else String.format(Locale.US, "%.1f %s", value, units[idx])
        }
    }
}
