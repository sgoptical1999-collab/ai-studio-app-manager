package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.BuildCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.InstallMobile
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.AppProjectEntity
import com.example.data.local.ArtifactEntity
import com.example.data.local.BuildRunEntity
import com.example.data.local.BuildStatus
import com.example.data.repository.AppManagerRepository
import com.example.ui.UiOperationState
import com.example.ui.components.BuildStatusPill
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.InfoNoticeBanner
import com.example.ui.components.formatDuration
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.NavyBorder
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceElevated
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.SkyCyan
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.File

@Composable
fun ApkBuildManagerScreen(
    apps: List<AppProjectEntity>,
    builds: List<BuildRunEntity>,
    artifacts: List<ArtifactEntity>,
    opState: UiOperationState,
    onConnectGitHub: () -> Unit,
    onRefreshAll: () -> Unit,
    onRefreshAppBuilds: (Long) -> Unit,
    onOpenUrl: (String) -> Unit,
    onDownloadArtifactZip: (ArtifactEntity) -> Unit,
    onExtractArtifact: (ArtifactEntity) -> Unit,
    onSaveExtractedApk: (Long, File, BuildRunEntity?) -> Unit,
    onInstallApkFile: (Long, String, File) -> Unit,
    onDownloadLatestApkForApp: (Long) -> Unit,
    onDismissExtractionOutcome: () -> Unit
) {
    val connectedApps = remember(apps) {
        apps.filter { it.githubOwner.isNotBlank() && it.githubRepoName.isNotBlank() }
    }
    var selectedAppId by remember { mutableStateOf<Long?>(null) }
    var inspectedBuild by remember { mutableStateOf<BuildRunEntity?>(null) }

    val appsById = remember(apps) { apps.associateBy { it.id } }
    val filteredBuilds = remember(builds, selectedAppId) {
        if (selectedAppId == null) builds else builds.filter { it.appId == selectedAppId }
    }
    val artifactsByBuild = remember(artifacts) { artifacts.groupBy { it.buildRunId } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBackground)
            .statusBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "APK BUILD MANAGER",
                        style = MaterialTheme.typography.headlineLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "GitHub Actions workflows & APK artifact downloads",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
                OutlinedButton(
                    onClick = onRefreshAll,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, NavyBorder),
                    modifier = Modifier.testTag("builds_refresh_all_button")
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Refresh", color = TextPrimary)
                }
            }
        }

        // GitHub Connection Status Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = BorderStroke(1.dp, if (opState.githubSession != null) StatusSuccess.copy(alpha = 0.45f) else NavyBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = opState.githubSession?.let { "GitHub Connected (@${it.username})" }
                                ?: "GitHub Account Not Connected",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (opState.githubSession != null) {
                                "Authorized for private/public Actions artifacts • Last sync: ${formatTimestamp(opState.githubSession.lastSyncedAt)}"
                            } else {
                                "Connect via official GitHub OAuth to download Actions artifact ZIPs."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                    TextButton(
                        onClick = onConnectGitHub,
                        modifier = Modifier.testTag("builds_connect_github_button")
                    ) {
                        Text(
                            text = if (opState.githubSession != null) "Manage" else "Connect",
                            color = ElectricBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Filter by connected app
        if (connectedApps.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedAppId == null,
                            onClick = { selectedAppId = null },
                            label = { Text("All Connected (${connectedApps.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = NavySurface,
                                selectedContainerColor = ElectricBlue.copy(alpha = 0.22f),
                                selectedLabelColor = ElectricBlue
                            )
                        )
                    }
                    items(connectedApps, key = { it.id }) { appItem ->
                        FilterChip(
                            selected = selectedAppId == appItem.id,
                            onClick = { selectedAppId = appItem.id },
                            label = { Text(appItem.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = NavySurface,
                                selectedContainerColor = RoyalPurple.copy(alpha = 0.22f),
                                selectedLabelColor = RoyalPurple
                            )
                        )
                    }
                }
            }
        }

        // Builds List
        if (connectedApps.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.Code,
                    title = "No GitHub Repositories Linked",
                    message = "Connect your GitHub account or add a GitHub repository URL to any app folder in My Apps to inspect real GitHub Actions workflows and download APK artifacts.",
                    primaryActionLabel = "CONNECT GITHUB",
                    onPrimaryAction = onConnectGitHub
                )
            }
        } else if (filteredBuilds.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.BuildCircle,
                    title = "No Workflow Runs Synced Yet",
                    message = "Tap Refresh to fetch real GitHub Actions runs and APK artifacts from your linked repositories. No fake or simulated build results are ever displayed.",
                    primaryActionLabel = "REFRESH BUILD STATUS",
                    onPrimaryAction = onRefreshAll
                )
            }
        } else {
            items(filteredBuilds, key = { it.id }) { run ->
                val appEntity = appsById[run.appId]
                val runArtifacts = artifactsByBuild[run.id].orEmpty()
                BuildRunCard(
                    appName = appEntity?.name ?: "App #${run.appId}",
                    repoSlug = appEntity?.let { "${it.githubOwner}/${it.githubRepoName}" } ?: "",
                    run = run,
                    artifacts = runArtifacts,
                    onRefreshStatus = { onRefreshAppBuilds(run.appId) },
                    onViewDetails = { inspectedBuild = run },
                    onOpenLogs = { onOpenUrl(run.htmlUrl) },
                    onDownloadLatestApk = { onDownloadLatestApkForApp(run.appId) }
                )
            }
        }
    }

    // Build Details & Artifacts Modal Dialog
    inspectedBuild?.let { run ->
        val appEntity = appsById[run.appId]
        val runArtifacts = artifactsByBuild[run.id].orEmpty()
        AlertDialog(
            onDismissRequest = { inspectedBuild = null },
            containerColor = NavySurfaceElevated,
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = run.workflowName,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${appEntity?.name ?: ""} • Run #${run.githubRunId}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted
                    )
                }
            },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            BuildStatusPill(status = run.status)
                            Text("Branch: ${run.branch} • Commit: ${run.commitSha}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            if (run.commitMessage.isNotBlank()) {
                                Text(run.commitMessage, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                            }
                            Text(
                                "Date: ${formatTimestamp(run.createdAt)} • Duration: ${formatDuration(run.durationSeconds)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }

                    if (run.status == BuildStatus.FAILED) {
                        item {
                            InfoNoticeBanner(
                                title = "Build Failed",
                                body = "No installable APK artifact is available for failed builds. Review the GitHub Actions workflow logs to resolve the build error.",
                                accent = StatusError,
                                icon = Icons.Rounded.Warning
                            )
                        }
                    }

                    if (runArtifacts.isEmpty()) {
                        item {
                            Text(
                                text = "No build artifacts attached to this workflow run.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    } else {
                        items(runArtifacts, key = { it.id }) { artifact ->
                            ArtifactActionCard(
                                artifact = artifact,
                                onDownloadZip = { onDownloadArtifactZip(artifact) },
                                onExtractApk = { onExtractArtifact(artifact) },
                                onDownloadApk = {
                                    val extractedPath = artifact.extractedApkPath
                                    if (!extractedPath.isNullOrBlank() && File(extractedPath).exists()) {
                                        onSaveExtractedApk(run.appId, File(extractedPath), run)
                                    } else {
                                        onExtractArtifact(artifact)
                                    }
                                },
                                onInstallApk = {
                                    val extractedPath = artifact.extractedApkPath
                                    if (!extractedPath.isNullOrBlank() && File(extractedPath).exists()) {
                                        onInstallApkFile(run.appId, appEntity?.name ?: "App", File(extractedPath))
                                    } else {
                                        onExtractArtifact(artifact)
                                    }
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onOpenUrl(run.htmlUrl) }) {
                    Text("GitHub Logs", color = ElectricBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { inspectedBuild = null }) {
                    Text("Close", color = TextSecondary)
                }
            }
        )
    }

    // Extracted APK & AAB Selector Dialog
    val extraction = opState.latestExtractionOutcome
    val activeArtifact = opState.activeArtifactForExtraction
    if (extraction != null && activeArtifact != null) {
        val parentApp = appsById[activeArtifact.appId]
        val parentRun = builds.firstOrNull { it.id == activeArtifact.buildRunId }
        AlertDialog(
            onDismissRequest = onDismissExtractionOutcome,
            containerColor = NavySurfaceElevated,
            title = {
                Text("Extracted Artifact Contents", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Artifact: ${activeArtifact.name}",
                        style = MaterialTheme.typography.labelMedium,
                        color = SkyCyan
                    )

                    if (extraction.extractedApks.isNotEmpty()) {
                        Text(
                            text = "Verified APK files found (${extraction.extractedApks.size}):",
                            style = MaterialTheme.typography.titleSmall,
                            color = StatusSuccess
                        )
                        extraction.extractedApks.forEach { apkFile ->
                            Surface(
                                color = NavySurface,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, StatusSuccess.copy(alpha = 0.4f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = apkFile.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Size: ${AppManagerRepository.formatBytes(apkFile.length())} • Verified APK Header",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                onSaveExtractedApk(activeArtifact.appId, apkFile, parentRun)
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("DOWNLOAD APK")
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                onInstallApkFile(
                                                    activeArtifact.appId,
                                                    parentApp?.name ?: "App",
                                                    apkFile
                                                )
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("INSTALL APK", color = StatusSuccess)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (extraction.extractedAabs.isNotEmpty()) {
                        InfoNoticeBanner(
                            title = "Android App Bundle (.aab) Detected",
                            body = "Found ${extraction.extractedAabs.joinToString { it.name }}. An .aab bundle is for Google Play publishing and cannot be renamed to .apk or installed directly on a phone.",
                            accent = StatusWarning,
                            icon = Icons.Rounded.Warning
                        )
                    }

                    if (extraction.extractedApks.isEmpty() && extraction.extractedAabs.isEmpty()) {
                        Text(
                            text = "No .apk files were found inside this artifact ZIP archive.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismissExtractionOutcome) {
                    Text("Done", color = ElectricBlue)
                }
            }
        )
    }
}

@Composable
private fun BuildRunCard(
    appName: String,
    repoSlug: String,
    run: BuildRunEntity,
    artifacts: List<ArtifactEntity>,
    onRefreshStatus: () -> Unit,
    onViewDetails: () -> Unit,
    onOpenLogs: () -> Unit,
    onDownloadLatestApk: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        border = BorderStroke(1.dp, NavyBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "$appName • ${run.workflowName}",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$repoSlug • Run #${run.githubRunId}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SkyCyan
                    )
                }
                BuildStatusPill(status = run.status)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Branch: ${run.branch}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Text("Commit: ${run.commitSha.ifBlank { "—" }}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Text("Duration: ${formatDuration(run.durationSeconds)}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTimestamp(run.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                Surface(
                    color = if (run.hasApkArtifact) StatusSuccess.copy(alpha = 0.14f) else NavySurfaceElevated,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = when {
                            run.hasAabOnly -> "AAB Only (Not APK)"
                            artifacts.isNotEmpty() -> "${artifacts.size} Artifact(s)"
                            run.artifactCount > 0 -> "${run.artifactCount} Artifact(s)"
                            else -> "No Artifacts"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            run.hasAabOnly -> StatusWarning
                            run.hasApkArtifact -> StatusSuccess
                            else -> TextMuted
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            HorizontalDivider(color = NavyBorder.copy(alpha = 0.55f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onViewDetails,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, NavyBorder),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Inventory2, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("VIEW ARTIFACTS", style = MaterialTheme.typography.labelMedium, color = TextPrimary, maxLines = 1)
                }

                if (run.status == BuildStatus.FAILED) {
                    OutlinedButton(
                        onClick = onOpenLogs,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, StatusError.copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, tint = StatusError, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("VIEW LOGS", style = MaterialTheme.typography.labelMedium, color = StatusError, maxLines = 1)
                    }
                } else if (run.status == BuildStatus.SUCCESS && run.hasApkArtifact) {
                    Button(
                        onClick = onDownloadLatestApk,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("DOWNLOAD APK", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    }
                } else {
                    OutlinedButton(
                        onClick = onRefreshStatus,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, NavyBorder),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, tint = SkyCyan, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("REFRESH STATUS", style = MaterialTheme.typography.labelMedium, color = TextPrimary, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtifactActionCard(
    artifact: ArtifactEntity,
    onDownloadZip: () -> Unit,
    onExtractApk: () -> Unit,
    onDownloadApk: () -> Unit,
    onInstallApk: () -> Unit
) {
    Surface(
        color = NavySurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (artifact.expired) StatusWarning.copy(alpha = 0.5f) else NavyBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = artifact.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (artifact.expired) {
                    Text(
                        text = "EXPIRED",
                        style = MaterialTheme.typography.labelSmall,
                        color = StatusWarning,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = "Size: ${AppManagerRepository.formatBytes(artifact.sizeInBytes)} • Created: ${formatTimestamp(artifact.createdAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
            if (artifact.expiresAt > 0L) {
                Text(
                    text = "Expires: ${formatTimestamp(artifact.expiresAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (artifact.expired) StatusWarning else TextMuted
                )
            }
            if (!artifact.validationMessage.isNullOrBlank()) {
                Text(
                    text = artifact.validationMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (artifact.isAabOnly) StatusWarning else StatusSuccess
                )
            }

            if (!artifact.expired) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = onDownloadZip,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.FolderZip, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("ARTIFACT ZIP", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onExtractApk,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Unarchive, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("EXTRACT APK", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
                if (!artifact.isAabOnly) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = onDownloadApk,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.size(4.dp))
                            Text("DOWNLOAD APK", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                        Button(
                            onClick = onInstallApk,
                            colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.InstallMobile, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.size(4.dp))
                            Text("INSTALL APK", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}
