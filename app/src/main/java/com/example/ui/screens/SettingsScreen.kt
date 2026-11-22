package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.preferences.AppSettings
import com.example.data.repository.AppManagerRepository
import com.example.ui.UiOperationState
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

@Composable
fun SettingsScreen(
    settings: AppSettings,
    opState: UiOperationState,
    hasDemoFolders: Boolean,
    onUpdateAiStudioUrl: (String) -> Unit,
    onConnectGitHub: () -> Unit,
    onDisconnectGitHub: () -> Unit,
    onSignInGoogle: () -> Unit,
    onDisconnectGoogle: () -> Unit,
    onSelectCustomStorage: (Uri?) -> Unit,
    onSetWifiOnly: (Boolean) -> Unit,
    onSetAutoRefresh: (Boolean, Int) -> Unit,
    onSetNotifications: (Boolean) -> Unit,
    onClearDownloadHistory: () -> Unit,
    onExportBackup: (Uri) -> Unit,
    onRestoreBackup: (Uri) -> Unit,
    onRefreshStorage: () -> Unit,
    onLoadDemoData: () -> Unit,
    onClearDemoData: () -> Unit
) {
    val context = LocalContext.current
    var aiStudioUrlDraft by remember(settings.aiStudioUrl) { mutableStateOf(settings.aiStudioUrl) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    val customFolderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            onSelectCustomStorage(treeUri)
        }
    }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) onExportBackup(uri)
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onRestoreBackup(uri)
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onSetNotifications(granted)
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
                    text = "SETTINGS",
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Connections, storage vault, security, and backups",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }

        // Google AI Studio URL
        item {
            SettingsSectionCard(title = "Google AI Studio", icon = Icons.Rounded.Link) {
                OutlinedTextField(
                    value = aiStudioUrlDraft,
                    onValueChange = { aiStudioUrlDraft = it },
                    label = { Text("Google AI Studio URL (HTTPS)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = NavyBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_ai_studio_url_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onUpdateAiStudioUrl(aiStudioUrlDraft) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save URL")
                    }
                    OutlinedButton(
                        onClick = {
                            aiStudioUrlDraft = AppSettings.DEFAULT_AI_STUDIO_URL
                            onUpdateAiStudioUrl(AppSettings.DEFAULT_AI_STUDIO_URL)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reset Default")
                    }
                }
            }
        }

        // Connected Accounts (Google & GitHub)
        item {
            SettingsSectionCard(title = "Connected Accounts", icon = Icons.Rounded.Security) {
                // Google Account
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Rounded.AccountCircle,
                            contentDescription = null,
                            tint = if (opState.googleAccount != null) StatusSuccess else SkyCyan
                        )
                        Column {
                            Text("Google Account", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text(
                                text = opState.googleAccount?.email ?: "Not connected",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }
                    if (opState.googleAccount != null) {
                        TextButton(
                            onClick = onDisconnectGoogle,
                            modifier = Modifier.testTag("settings_disconnect_google_button")
                        ) {
                            Text("Disconnect", color = StatusError)
                        }
                    } else {
                        TextButton(onClick = onSignInGoogle) {
                            Text("Connect", color = ElectricBlue)
                        }
                    }
                }

                HorizontalDivider(color = NavyBorder.copy(alpha = 0.55f))

                // GitHub Account
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Rounded.Code,
                            contentDescription = null,
                            tint = if (opState.githubSession != null) StatusSuccess else RoyalPurple
                        )
                        Column {
                            Text("GitHub OAuth", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text(
                                text = opState.githubSession?.let { "@${it.username} (Keystore Encrypted)" }
                                    ?: "Not connected",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }
                    if (opState.githubSession != null) {
                        TextButton(
                            onClick = onDisconnectGitHub,
                            modifier = Modifier.testTag("settings_disconnect_github_button")
                        ) {
                            Text("Disconnect", color = StatusError)
                        }
                    } else {
                        TextButton(onClick = onConnectGitHub) {
                            Text("Connect", color = ElectricBlue)
                        }
                    }
                }
            }
        }

        // Storage & Download Location
        item {
            SettingsSectionCard(title = "Storage & Download Location", icon = Icons.Rounded.Storage) {
                val usage = opState.storageUsage
                Text(
                    text = "Active Location: ${settings.customStorageDisplayLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = "App Vault Root: ${usage.rootPathDisplay} (${usage.appFolderCount} folder(s))",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Used: ${AppManagerRepository.formatBytes(usage.totalBytes)}", style = MaterialTheme.typography.titleSmall, color = ElectricBlue)
                    TextButton(onClick = onRefreshStorage) {
                        Text("Recalculate", color = SkyCyan)
                    }
                }

                Text(
                    text = "Source ZIPs: ${AppManagerRepository.formatBytes(usage.sourceBytes)} • APKs: ${AppManagerRepository.formatBytes(usage.apkBytes)} • Artifacts: ${AppManagerRepository.formatBytes(usage.artifactsBytes)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { customFolderPicker.launch(null) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_pick_storage_folder_button")
                    ) {
                        Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Select SAF Folder")
                    }
                    if (settings.customStorageTreeUri != null) {
                        TextButton(onClick = { onSelectCustomStorage(null) }) {
                            Text("Reset", color = TextSecondary)
                        }
                    }
                }
            }
        }

        // Downloads, Refresh & Notifications
        item {
            SettingsSectionCard(title = "Downloads, Sync & Alerts", icon = Icons.Rounded.Sync) {
                SettingsToggleRow(
                    title = "Wi-Fi-Only Downloads",
                    subtitle = "Prevent downloading APKs and artifacts on cellular data",
                    checked = settings.wifiOnlyDownloads,
                    onCheckedChange = onSetWifiOnly,
                    testTag = "settings_wifi_only_switch"
                )
                HorizontalDivider(color = NavyBorder.copy(alpha = 0.5f))
                SettingsToggleRow(
                    title = "Automatic Background Refresh",
                    subtitle = "Periodically check GitHub Actions builds via WorkManager",
                    checked = settings.autoRefreshEnabled,
                    onCheckedChange = { enabled -> onSetAutoRefresh(enabled, settings.autoRefreshIntervalHours) },
                    testTag = "settings_auto_refresh_switch"
                )
                if (settings.autoRefreshEnabled) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(2, 6, 12, 24).forEach { hours ->
                            FilterChip(
                                selected = settings.autoRefreshIntervalHours == hours,
                                onClick = { onSetAutoRefresh(true, hours) },
                                label = { Text("${hours}h") }
                            )
                        }
                    }
                }
                HorizontalDivider(color = NavyBorder.copy(alpha = 0.5f))
                SettingsToggleRow(
                    title = "Build & APK Notifications",
                    subtitle = "Alert when background builds or APK downloads finish",
                    checked = settings.notificationsEnabled,
                    onCheckedChange = { wantEnabled ->
                        if (wantEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            onSetNotifications(wantEnabled)
                        }
                    },
                    testTag = "settings_notifications_switch"
                )
            }
        }

        // Backup, Restore & Maintenance
        item {
            SettingsSectionCard(title = "Backup, Restore & Maintenance", icon = Icons.Rounded.Backup) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { exportBackupLauncher.launch("ai_studio_app_manager_backup.json") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_backup_button")
                    ) {
                        Icon(Icons.Rounded.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Backup JSON")
                    }
                    OutlinedButton(
                        onClick = { restoreBackupLauncher.launch(arrayOf("application/json", "*/*")) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_restore_button")
                    ) {
                        Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Restore JSON")
                    }
                }

                OutlinedButton(
                    onClick = { showClearHistoryDialog = true },
                    border = BorderStroke(1.dp, NavyBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_clear_history_button")
                ) {
                    Icon(Icons.Rounded.DeleteSweep, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Clear Download History (Keeps APK Files)", color = TextPrimary)
                }

                OutlinedButton(
                    onClick = if (hasDemoFolders) onClearDemoData else onLoadDemoData,
                    border = BorderStroke(1.dp, NavyBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (hasDemoFolders) "Remove DEMO DATA Sample Folders" else "Load DEMO DATA Sample Folders",
                        color = SkyCyan
                    )
                }
            }
        }
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            containerColor = NavySurfaceElevated,
            title = { Text("Clear Download History?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This clears the download history log only. Your downloaded APK and ZIP files inside My Apps/ will NOT be deleted.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(onClick = {
                    onClearDownloadHistory()
                    showClearHistoryDialog = false
                }) {
                    Text("Clear History")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        border = BorderStroke(1.dp, NavyBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(20.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            content()
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}
