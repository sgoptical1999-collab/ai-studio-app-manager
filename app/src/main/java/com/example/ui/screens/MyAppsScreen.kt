package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.ui.AppFilter
import com.example.ui.components.AppIconAvatar
import com.example.ui.components.BuildStatusPill
import com.example.ui.components.DemoDataBadge
import com.example.ui.components.EmptyStateCard
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.NavyBorder
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceElevated
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.SkyCyan
import com.example.ui.theme.StatusError
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun MyAppsScreen(
    apps: List<AppProjectEntity>,
    totalAppCount: Int,
    searchQuery: String,
    selectedFilter: AppFilter,
    onSearchQueryChange: (String) -> Unit,
    onFilterSelected: (AppFilter) -> Unit,
    onAddNewApp: () -> Unit,
    onOpenAppDetail: (Long) -> Unit,
    onEditApp: (Long) -> Unit,
    onDeleteApp: (AppProjectEntity) -> Unit,
    onDownloadLatestApk: (Long) -> Unit,
    onImportSourceZipForApp: (Long) -> Unit,
    onLoadDemoFolders: () -> Unit
) {
    var appPendingDelete by remember { mutableStateOf<AppProjectEntity?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBackground)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "MY APPS",
                        style = MaterialTheme.typography.headlineLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$totalAppCount dedicated project folder(s) in AI Studio App Manager/My Apps/",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            "Search app, repo, package, version, or APK...",
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = "Search apps", tint = ElectricBlue)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear search", tint = TextMuted)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = NavyBorder,
                        focusedContainerColor = NavySurface,
                        unfocusedContainerColor = NavySurface,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("my_apps_search_input")
                )
            }

            // Filter Chips
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AppFilter.entries) { filter ->
                        val selected = filter == selectedFilter
                        FilterChip(
                            selected = selected,
                            onClick = { onFilterSelected(filter) },
                            label = {
                                Text(
                                    text = filter.label,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = NavySurface,
                                labelColor = TextSecondary,
                                selectedContainerColor = ElectricBlue.copy(alpha = 0.22f),
                                selectedLabelColor = ElectricBlue
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = NavyBorder,
                                selectedBorderColor = ElectricBlue
                            ),
                            modifier = Modifier.testTag("filter_chip_${filter.name.lowercase()}")
                        )
                    }
                }
            }

            if (apps.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Rounded.FolderCopy,
                        title = if (totalAppCount == 0) "No App Folders Yet" else "No Matching Folders",
                        message = if (totalAppCount == 0) {
                            "Create your first app folder to organize Google AI Studio links, exported ZIPs, GitHub builds, and downloaded APKs."
                        } else {
                            "Try clearing your search query or switching to the 'All Apps' filter."
                        },
                        primaryActionLabel = if (totalAppCount == 0) "ADD NEW APP" else "SHOW ALL APPS",
                        onPrimaryAction = {
                            if (totalAppCount == 0) onAddNewApp()
                            else {
                                onSearchQueryChange("")
                                onFilterSelected(AppFilter.ALL)
                            }
                        },
                        secondaryActionLabel = if (totalAppCount == 0) "Load Sample Folders (DEMO DATA)" else null,
                        onSecondaryAction = if (totalAppCount == 0) onLoadDemoFolders else null
                    )
                }
            } else {
                items(apps, key = { it.id }) { app ->
                    AppFolderCard(
                        app = app,
                        onOpenFolder = { onOpenAppDetail(app.id) },
                        onDownloadLatestApk = { onDownloadLatestApk(app.id) },
                        onImportZip = { onImportSourceZipForApp(app.id) },
                        onEdit = { onEditApp(app.id) },
                        onRequestDelete = { appPendingDelete = app }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = onAddNewApp,
            containerColor = ElectricBlue,
            contentColor = TextPrimary,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("my_apps_add_fab")
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Add new app folder")
        }
    }

    appPendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { appPendingDelete = null },
            containerColor = NavySurfaceElevated,
            title = {
                Text("Delete App Folder?", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "This will permanently delete '${target.name}' along with its local Source/, APK/, Build Artifacts/, and History/ folder contents from this device.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteApp(target)
                        appPendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError),
                    modifier = Modifier.testTag("confirm_delete_app_button")
                ) {
                    Text("Delete Folder")
                }
            },
            dismissButton = {
                TextButton(onClick = { appPendingDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun AppFolderCard(
    app: AppProjectEntity,
    onOpenFolder: () -> Unit,
    onDownloadLatestApk: () -> Unit,
    onImportZip: () -> Unit,
    onEdit: () -> Unit,
    onRequestDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("app_folder_card_${app.id}")
            .clickable(onClick = onOpenFolder),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        border = BorderStroke(1.dp, NavyBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppIconAvatar(
                    iconUri = app.iconUri,
                    colorHex = app.iconColorHex,
                    symbolKey = app.iconSymbolKey,
                    contentDescription = "${app.name} icon",
                    size = 52.dp
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = app.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (app.isDemoData) {
                            DemoDataBadge()
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = app.projectType.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = SkyCyan
                        )
                        Text("•", color = TextMuted)
                        Text(
                            text = "v${app.latestApkVersion.ifBlank { app.versionName }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
                BuildStatusPill(status = app.latestBuildStatus)
            }

            if (app.description.isNotBlank()) {
                Text(
                    text = app.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Linked metadata chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (app.githubOwner.isNotBlank() && app.githubRepoName.isNotBlank()) {
                    Surface(
                        color = NavySurfaceElevated,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, NavyBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Rounded.Code, contentDescription = null, tint = RoyalPurple, modifier = Modifier.size(13.dp))
                            Text(
                                text = "${app.githubOwner}/${app.githubRepoName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                if (app.aiStudioUrl.isNotBlank()) {
                    Surface(
                        color = NavySurfaceElevated,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, NavyBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Rounded.Link, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(13.dp))
                            Text(
                                text = "AI Studio Link",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricBlue
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = NavyBorder.copy(alpha = 0.6f))

            // Folder Action Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onDownloadLatestApk,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("card_download_apk_${app.id}")
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("APK", style = MaterialTheme.typography.labelLarge, maxLines = 1)
                }

                OutlinedButton(
                    onClick = onImportZip,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, NavyBorder),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("card_source_zip_${app.id}")
                ) {
                    Icon(Icons.Rounded.FolderZip, contentDescription = null, tint = SkyCyan, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("ZIP", style = MaterialTheme.typography.labelLarge, color = TextPrimary, maxLines = 1)
                }

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("card_edit_app_${app.id}")
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Edit ${app.name}", tint = TextSecondary)
                }

                IconButton(
                    onClick = onRequestDelete,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("card_delete_app_${app.id}")
                ) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Delete ${app.name}", tint = StatusError)
                }
            }
        }
    }
}
