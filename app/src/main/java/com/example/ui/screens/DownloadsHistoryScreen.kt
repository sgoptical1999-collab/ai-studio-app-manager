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
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.InstallMobile
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.data.installer.ApkInstallerManager
import com.example.data.local.DownloadFileType
import com.example.data.local.DownloadRecordEntity
import com.example.data.local.DownloadStatus
import com.example.data.repository.AppManagerRepository
import com.example.ui.components.DownloadStatusPill
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.NavyBorder
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceElevated
import com.example.ui.theme.SkyCyan
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.File

private enum class DownloadFilterTab(val label: String) {
    ALL("All"),
    COMPLETED("Completed"),
    FAILED("Failed & Expired")
}

@Composable
fun DownloadsHistoryScreen(
    downloads: List<DownloadRecordEntity>,
    onInstallApk: (DownloadRecordEntity) -> Unit,
    onOpenFile: (File, String) -> Unit,
    onShareFile: (File, String, String) -> Unit,
    onDeleteRecord: (DownloadRecordEntity, Boolean) -> Unit,
    onRetryDownload: (Long) -> Unit,
    onGoToBuilds: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(DownloadFilterTab.ALL) }
    var recordPendingDelete by remember { mutableStateOf<DownloadRecordEntity?>(null) }

    val failedCount = remember(downloads) {
        downloads.count { it.status == DownloadStatus.FAILED || it.status == DownloadStatus.EXPIRED }
    }

    val filtered = remember(downloads, selectedTab) {
        when (selectedTab) {
            DownloadFilterTab.ALL -> downloads
            DownloadFilterTab.COMPLETED -> downloads.filter { it.status == DownloadStatus.COMPLETED }
            DownloadFilterTab.FAILED -> downloads.filter {
                it.status == DownloadStatus.FAILED || it.status == DownloadStatus.EXPIRED
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBackground)
            .statusBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "DOWNLOAD HISTORY",
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${downloads.size} total record(s) • $failedCount failed or expired",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(DownloadFilterTab.entries) { tab ->
                    val label = if (tab == DownloadFilterTab.FAILED && failedCount > 0) {
                        "${tab.label} ($failedCount)"
                    } else tab.label
                    FilterChip(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = NavySurface,
                            selectedContainerColor = if (tab == DownloadFilterTab.FAILED) {
                                StatusError.copy(alpha = 0.2f)
                            } else {
                                ElectricBlue.copy(alpha = 0.22f)
                            },
                            selectedLabelColor = if (tab == DownloadFilterTab.FAILED) StatusError else ElectricBlue
                        ),
                        modifier = Modifier.testTag("downloads_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.DownloadForOffline,
                    title = if (selectedTab == DownloadFilterTab.FAILED) "No Failed Downloads" else "No Downloads Yet",
                    message = if (selectedTab == DownloadFilterTab.FAILED) {
                        "All your downloads and artifact imports completed cleanly."
                    } else {
                        "Downloaded APKs, GitHub Actions artifact ZIPs, and imported project source ZIPs appear here with instant install, share, and retry options."
                    },
                    primaryActionLabel = if (selectedTab == DownloadFilterTab.FAILED) null else "OPEN APK BUILD MANAGER",
                    onPrimaryAction = if (selectedTab == DownloadFilterTab.FAILED) null else onGoToBuilds
                )
            }
        } else {
            items(filtered, key = { it.id }) { record ->
                DownloadHistoryCard(
                    record = record,
                    onInstall = { onInstallApk(record) },
                    onOpen = {
                        val mime = if (record.fileType == DownloadFileType.APK) {
                            ApkInstallerManager.MIME_APK
                        } else {
                            ApkInstallerManager.MIME_ZIP
                        }
                        onOpenFile(File(record.localFilePath), mime)
                    },
                    onShare = {
                        val mime = if (record.fileType == DownloadFileType.APK) {
                            ApkInstallerManager.MIME_APK
                        } else {
                            ApkInstallerManager.MIME_ZIP
                        }
                        onShareFile(File(record.localFilePath), mime, "Share ${record.fileName}")
                    },
                    onRetry = { onRetryDownload(record.appId) },
                    onDelete = { recordPendingDelete = record }
                )
            }
        }
    }

    recordPendingDelete?.let { rec ->
        val fileExists = rec.localFilePath.isNotBlank() && File(rec.localFilePath).exists()
        AlertDialog(
            onDismissRequest = { recordPendingDelete = null },
            containerColor = NavySurfaceElevated,
            title = { Text("Delete Download Entry?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    if (fileExists) {
                        "Choose whether to remove only the history record or also delete '${rec.fileName}' from device storage."
                    } else {
                        "Remove '${rec.fileName}' from your download history?"
                    },
                    color = TextSecondary
                )
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (fileExists) {
                        OutlinedButton(onClick = {
                            onDeleteRecord(rec, false)
                            recordPendingDelete = null
                        }) {
                            Text("History Only")
                        }
                    }
                    Button(
                        onClick = {
                            onDeleteRecord(rec, fileExists)
                            recordPendingDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                    ) {
                        Text(if (fileExists) "Delete File" else "Remove")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { recordPendingDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun DownloadHistoryCard(
    record: DownloadRecordEntity,
    onInstall: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit
) {
    val fileExists = record.localFilePath.isNotBlank() && File(record.localFilePath).exists()
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        border = BorderStroke(
            1.dp,
            if (record.status == DownloadStatus.FAILED || record.status == DownloadStatus.EXPIRED) {
                StatusError.copy(alpha = 0.45f)
            } else NavyBorder
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
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
                    Icon(
                        imageVector = if (record.fileType == DownloadFileType.APK) Icons.Rounded.Android else Icons.Rounded.FolderZip,
                        contentDescription = null,
                        tint = if (record.fileType == DownloadFileType.APK) StatusSuccess else SkyCyan
                    )
                    Column {
                        Text(
                            text = record.appName,
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricBlue,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = record.fileName,
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                DownloadStatusPill(status = record.status)
            }

            Text(
                text = "${record.fileType.label} • v${record.versionName.ifBlank { "1.0" }} • ${AppManagerRepository.formatBytes(record.fileSizeBytes)}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Text(
                text = "Date: ${formatTimestamp(record.createdAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )

            if (record.localFilePath.isNotBlank()) {
                val relativeDisplay = record.localFilePath.substringAfter("AI Studio App Manager/", record.localFilePath)
                Text(
                    text = "Location: AI Studio App Manager/$relativeDisplay",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!record.errorMessage.isNullOrBlank()) {
                Text(
                    text = record.errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = StatusError
                )
            }

            HorizontalDivider(color = NavyBorder.copy(alpha = 0.55f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (record.fileType == DownloadFileType.APK && record.status == DownloadStatus.COMPLETED && fileExists) {
                    Button(
                        onClick = onInstall,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Rounded.InstallMobile, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("Install", style = MaterialTheme.typography.labelMedium)
                    }
                }
                if (fileExists) {
                    OutlinedButton(
                        onClick = onOpen,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, NavyBorder),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("Open", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                    }
                }
                Spacer(Modifier.weight(1f))
                if (fileExists) {
                    IconButton(onClick = onShare) {
                        Icon(Icons.Rounded.Share, contentDescription = "Share file", tint = TextSecondary)
                    }
                }
                IconButton(onClick = onRetry) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Download again", tint = ElectricBlue)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Delete download", tint = StatusError)
                }
            }
        }
    }
}
