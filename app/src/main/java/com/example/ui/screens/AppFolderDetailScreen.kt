package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.BuildCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.InstallMobile
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.example.data.installer.ApkInstallerManager
import com.example.data.local.ApkVersionEntity
import com.example.data.local.AppProjectEntity
import com.example.data.local.BuildRunEntity
import com.example.data.local.DownloadRecordEntity
import com.example.data.local.ExtractionStatus
import com.example.data.local.SourceZipEntity
import com.example.data.repository.AppManagerRepository
import com.example.ui.components.AppIconAvatar
import com.example.ui.components.BuildStatusPill
import com.example.ui.components.DemoDataBadge
import com.example.ui.components.DownloadStatusPill
import com.example.ui.components.GradientPrimaryButton
import com.example.ui.components.SectionHeader
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
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppFolderDetailScreen(
    app: AppProjectEntity?,
    sourceZips: List<SourceZipEntity>,
    builds: List<BuildRunEntity>,
    apks: List<ApkVersionEntity>,
    downloads: List<DownloadRecordEntity>,
    onBack: () -> Unit,
    onEditApp: () -> Unit,
    onDeleteApp: () -> Unit,
    onDownloadLatestApk: () -> Unit,
    onRefreshBuilds: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onConnectGitHub: () -> Unit,
    onOpenBuildsTab: () -> Unit,
    onImportSourceZip: (Uri, String) -> Unit,
    onExtractSourceZip: (SourceZipEntity) -> Unit,
    onImportLocalApk: (Uri) -> Unit,
    onInstallApk: (ApkVersionEntity) -> Unit,
    onShareFile: (File, String, String) -> Unit,
    onOpenFile: (File, String) -> Unit,
    onDeleteApk: (ApkVersionEntity) -> Unit
) {
    if (app == null) {
        Scaffold(containerColor = NavyBackground) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("App folder not found.", color = TextSecondary)
            }
        }
        return
    }

    var showDeleteAppConfirm by remember { mutableStateOf(false) }
    var apkPendingDelete by remember { mutableStateOf<ApkVersionEntity?>(null) }
    var pendingZipUri by remember { mutableStateOf<Uri?>(null) }
    var zipVersionInput by remember { mutableStateOf(app.versionName.ifBlank { "1.0.0" }) }

    val zipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            zipVersionInput = app.versionName.ifBlank { "1.0.0" }
            pendingZipUri = uri
        }
    }

    val apkPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onImportLocalApk(uri)
        }
    }

    val latestApk = apks.firstOrNull { it.isLatest } ?: apks.firstOrNull()
    val previousApks = apks.filter { it.id != latestApk?.id }

    Scaffold(
        containerColor = NavyBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = app.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "My Apps/${app.folderName}/",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("app_detail_back_button")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = onEditApp, modifier = Modifier.testTag("app_detail_edit_button")) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit app", tint = TextPrimary)
                    }
                    IconButton(
                        onClick = { showDeleteAppConfirm = true },
                        modifier = Modifier.testTag("app_detail_delete_button")
                    ) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Delete app folder", tint = StatusError)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyBackground)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Project Information & One-Click Download Latest APK
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = NavySurface),
                    border = BorderStroke(1.dp, NavyBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            AppIconAvatar(
                                iconUri = app.iconUri,
                                colorHex = app.iconColorHex,
                                symbolKey = app.iconSymbolKey,
                                contentDescription = "${app.name} icon",
                                size = 60.dp
                            )
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = app.name,
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (app.isDemoData) DemoDataBadge()
                                }
                                Text(
                                    text = "${app.projectType.label} • v${app.versionName}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SkyCyan
                                )
                                if (app.packageName.isNotBlank()) {
                                    Text(
                                        text = app.packageName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                }
                            }
                        }

                        if (app.description.isNotBlank()) {
                            Text(
                                text = app.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }

                        // Folder Structure Badge
                        Surface(
                            color = NavySurfaceElevated,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, NavyBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Rounded.Folder, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(18.dp))
                                Column {
                                    Text(
                                        text = "AI Studio App Manager/My Apps/${app.folderName}/",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Subfolders: Source/ • APK/ • Build Artifacts/ • History/",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                }
                            }
                        }

                        // ONE-CLICK DOWNLOAD LATEST APK
                        GradientPrimaryButton(
                            text = "DOWNLOAD LATEST APK",
                            icon = Icons.Rounded.Download,
                            onClick = onDownloadLatestApk,
                            testTag = "download_latest_apk_button",
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Quick external links
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (app.aiStudioUrl.isNotBlank()) {
                                OutlinedButton(
                                    onClick = { onOpenUrl(app.aiStudioUrl) },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, NavyBorder),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.Link, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.size(6.dp))
                                    Text("AI Studio", color = TextPrimary, maxLines = 1)
                                }
                            }
                            if (app.githubRepoUrl.isNotBlank()) {
                                OutlinedButton(
                                    onClick = { onOpenUrl(app.githubRepoUrl) },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, NavyBorder),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, tint = RoyalPurple, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.size(6.dp))
                                    Text("GitHub", color = TextPrimary, maxLines = 1)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = onConnectGitHub,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, NavyBorder),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.Code, contentDescription = null, tint = RoyalPurple, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.size(6.dp))
                                    Text("Link GitHub Repo", color = TextPrimary, maxLines = 1)
                                }
                            }
                        }

                        if (app.notes.isNotBlank()) {
                            HorizontalDivider(color = NavyBorder.copy(alpha = 0.5f))
                            Text(
                                text = "Notes: ${app.notes}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }
                }
            }

            // 2. APK Files & Version Management
            item {
                SectionHeader(
                    title = "APK Files & Versions",
                    subtitle = "${apks.size} verified APK file(s) stored in APK/",
                    actionLabel = "+ Add APK",
                    onActionClick = {
                        apkPickerLauncher.launch(
                            arrayOf("application/vnd.android.package-archive", "application/octet-stream", "*/*")
                        )
                    }
                )
            }

            if (latestApk == null) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = NavySurface),
                        border = BorderStroke(1.dp, NavyBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No APK downloaded yet. Tap 'DOWNLOAD LATEST APK' after completing a GitHub Actions Android build, or tap '+ Add APK' to import a local APK.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                item {
                    ApkVersionCard(
                        apk = latestApk,
                        badgeLabel = "LATEST VERSION",
                        onInstall = { onInstallApk(latestApk) },
                        onDownloadAgain = onDownloadLatestApk,
                        onShare = {
                            onShareFile(
                                File(latestApk.localFilePath),
                                ApkInstallerManager.MIME_APK,
                                "Share ${latestApk.fileName}"
                            )
                        },
                        onDelete = { apkPendingDelete = latestApk }
                    )
                }
                if (previousApks.isNotEmpty()) {
                    item {
                        Text(
                            text = "PREVIOUS VERSIONS",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextMuted,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(previousApks, key = { it.id }) { oldApk ->
                        ApkVersionCard(
                            apk = oldApk,
                            badgeLabel = "PREVIOUS",
                            onInstall = { onInstallApk(oldApk) },
                            onDownloadAgain = onDownloadLatestApk,
                            onShare = {
                                onShareFile(
                                    File(oldApk.localFilePath),
                                    ApkInstallerManager.MIME_APK,
                                    "Share ${oldApk.fileName}"
                                )
                            },
                            onDelete = { apkPendingDelete = oldApk }
                        )
                    }
                }
            }

            // 3. Source ZIPs Section
            item {
                SectionHeader(
                    title = "Source ZIPs",
                    subtitle = "${sourceZips.size} archive(s) in Source/ • Originals preserved",
                    actionLabel = "+ Import ZIP",
                    onActionClick = {
                        zipPickerLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "*/*"))
                    }
                )
            }

            if (sourceZips.isEmpty()) {
                item {
                    OutlinedButton(
                        onClick = {
                            zipPickerLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "*/*"))
                        },
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, NavyBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("detail_import_source_zip_button")
                    ) {
                        Icon(Icons.Rounded.UploadFile, contentDescription = null, tint = SkyCyan)
                        Spacer(Modifier.size(8.dp))
                        Text("Import Exported Google AI Studio ZIP", color = TextPrimary)
                    }
                }
            } else {
                items(sourceZips, key = { it.id }) { zip ->
                    SourceZipCard(
                        zip = zip,
                        onExtract = { onExtractSourceZip(zip) },
                        onOpen = { onOpenFile(File(zip.localFilePath), ApkInstallerManager.MIME_ZIP) },
                        onShare = {
                            onShareFile(
                                File(zip.localFilePath),
                                ApkInstallerManager.MIME_ZIP,
                                "Share ${zip.fileName}"
                            )
                        }
                    )
                }
            }

            // 4. GitHub Repository & Build History
            item {
                SectionHeader(
                    title = "GitHub Repository & Builds",
                    subtitle = if (app.githubOwner.isNotBlank()) "${app.githubOwner}/${app.githubRepoName}" else "Not connected yet",
                    actionLabel = if (app.githubOwner.isNotBlank()) "Refresh" else "Connect",
                    onActionClick = {
                        if (app.githubOwner.isNotBlank()) onRefreshBuilds() else onConnectGitHub()
                    }
                )
            }

            if (builds.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = NavySurface),
                        border = BorderStroke(1.dp, NavyBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (app.githubOwner.isBlank()) {
                                    "Connect a GitHub repository to view live Actions builds and APK artifacts."
                                } else {
                                    "No GitHub Actions builds loaded yet. Tap Refresh to sync."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                modifier = Modifier.weight(1f)
                            )
                            if (app.githubOwner.isNotBlank()) {
                                IconButton(onClick = onRefreshBuilds) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = "Refresh builds", tint = ElectricBlue)
                                }
                            }
                        }
                    }
                }
            } else {
                items(builds.take(5), key = { it.id }) { run ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = NavySurface),
                        border = BorderStroke(1.dp, NavyBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = run.workflowName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Run #${run.githubRunId} • ${run.branch} • ${run.commitSha}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                }
                                BuildStatusPill(status = run.status)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${formatTimestamp(run.createdAt)} • ${formatDuration(run.durationSeconds)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                                TextButton(onClick = onOpenBuildsTab) {
                                    Text("View Artifacts", color = ElectricBlue)
                                }
                            }
                        }
                    }
                }
            }

            // 5. Folder Download History
            if (downloads.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Folder Download History",
                        subtitle = "${downloads.size} record(s)"
                    )
                }
                items(downloads.take(5), key = { it.id }) { rec ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = NavySurface),
                        border = BorderStroke(1.dp, NavyBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = rec.fileName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${rec.fileType.label} • ${AppManagerRepository.formatBytes(rec.fileSizeBytes)} • ${formatTimestamp(rec.createdAt)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                            DownloadStatusPill(status = rec.status)
                        }
                    }
                }
            }
        }
    }

    // ZIP version label prompt after picking a ZIP from SAF
    pendingZipUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingZipUri = null },
            containerColor = NavySurfaceElevated,
            title = { Text("Import Source ZIP", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Assign a version label for this ZIP archive. The original ZIP will be safely copied and preserved in Source/.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = zipVersionInput,
                        onValueChange = { zipVersionInput = it },
                        label = { Text("Source ZIP Version") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onImportSourceZip(uri, zipVersionInput)
                    pendingZipUri = null
                }) {
                    Text("Import ZIP")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingZipUri = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Delete App Folder Confirmation
    if (showDeleteAppConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteAppConfirm = false },
            containerColor = NavySurfaceElevated,
            title = { Text("Delete '${app.name}'?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "All locally saved ZIPs, APKs, artifacts, and records for '${app.name}' will be permanently deleted.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAppConfirm = false
                        onDeleteApp()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAppConfirm = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Delete APK Version Confirmation
    apkPendingDelete?.let { apk ->
        AlertDialog(
            onDismissRequest = { apkPendingDelete = null },
            containerColor = NavySurfaceElevated,
            title = { Text("Delete APK File?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Permanently delete '${apk.fileName}' (${AppManagerRepository.formatBytes(apk.fileSizeBytes)}) from this device?",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteApk(apk)
                        apkPendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text("Delete APK")
                }
            },
            dismissButton = {
                TextButton(onClick = { apkPendingDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun ApkVersionCard(
    apk: ApkVersionEntity,
    badgeLabel: String,
    onInstall: () -> Unit,
    onDownloadAgain: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        border = BorderStroke(1.dp, if (apk.isLatest) StatusSuccess.copy(alpha = 0.55f) else NavyBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Android, contentDescription = null, tint = StatusSuccess)
                    Text(
                        text = apk.fileName,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    color = if (apk.isLatest) StatusSuccess.copy(alpha = 0.16f) else NavySurfaceElevated,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = badgeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (apk.isLatest) StatusSuccess else TextMuted,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Text(
                text = buildString {
                    append("Version: v${apk.versionName.ifBlank { "1.0" }}")
                    if (apk.versionCode > 0) append(" (${apk.versionCode})")
                    append(" • ${AppManagerRepository.formatBytes(apk.fileSizeBytes)}")
                    if (apk.gitCommitSha.isNotBlank()) append(" • Commit ${apk.gitCommitSha}")
                },
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            if (apk.packageName.isNotBlank() || apk.buildNumber.isNotBlank()) {
                Text(
                    text = listOfNotNull(
                        apk.packageName.takeIf { it.isNotBlank() },
                        apk.buildNumber.takeIf { it.isNotBlank() }?.let { "Build #$it" },
                        apk.sourceZipVersion.takeIf { it.isNotBlank() }?.let { "ZIP $it" }
                    ).joinToString(" • "),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }

            Text(
                text = "Downloaded: ${formatTimestamp(apk.downloadedAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onInstall,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("install_apk_button_${apk.id}")
                ) {
                    Icon(Icons.Rounded.InstallMobile, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("INSTALL APK", style = MaterialTheme.typography.labelLarge)
                }
                IconButton(onClick = onDownloadAgain) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Download again", tint = ElectricBlue)
                }
                IconButton(onClick = onShare) {
                    Icon(Icons.Rounded.Share, contentDescription = "Share APK", tint = TextSecondary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Delete old version", tint = StatusError)
                }
            }
        }
    }
}

@Composable
private fun SourceZipCard(
    zip: SourceZipEntity,
    onExtract: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        border = BorderStroke(1.dp, NavyBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.FolderZip, contentDescription = null, tint = SkyCyan)
                    Column {
                        Text(
                            text = zip.fileName,
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Version ${zip.versionLabel} • ${AppManagerRepository.formatBytes(zip.fileSizeBytes)} • ${formatTimestamp(zip.uploadedAt)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = NavySurfaceElevated,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = zip.extractionStatus.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (zip.extractionStatus == ExtractionStatus.EXTRACTED) StatusSuccess else TextSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                OutlinedButton(
                    onClick = onExtract,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, NavyBorder),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Rounded.Unarchive, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("Extract", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                }
                IconButton(onClick = onOpen) {
                    Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = "Open ZIP", tint = TextSecondary)
                }
                IconButton(onClick = onShare) {
                    Icon(Icons.Rounded.Share, contentDescription = "Share ZIP", tint = TextSecondary)
                }
            }
        }
    }
}
