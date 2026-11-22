package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BuildCircle
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.InstallMobile
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.ActivityLogEntity
import com.example.data.local.ActivityType
import com.example.ui.DashboardMetrics
import com.example.ui.UiOperationState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MetricSummaryCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.AccentCardGradient
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.HeroOverlayGradient
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.NavyBorder
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceElevated
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.SkyCyan
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun HomeDashboardScreen(
    metrics: DashboardMetrics,
    recentActivity: List<ActivityLogEntity>,
    opState: UiOperationState,
    onOpenAiStudioGuide: () -> Unit,
    onOpenMyApps: () -> Unit,
    onConnectGitHub: () -> Unit,
    onImportProjectZip: () -> Unit,
    onAddNewApp: () -> Unit,
    onOpenApkBuilds: () -> Unit,
    onOpenDownloadHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onRefreshAll: () -> Unit,
    onLoadDemoWorkspace: () -> Unit,
    onOpenAppDetail: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBackground),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Hero Header Banner
        item {
            DashboardHeroHeader(
                opState = opState,
                onRefreshAll = onRefreshAll
            )
        }

        // 5 Summary Metric Cards (2 + 3 balanced rows)
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SectionHeader(
                    title = "Workspace Overview",
                    subtitle = "All your AI Studio apps in one place"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricSummaryCard(
                        title = "Total Apps",
                        value = metrics.totalApps.toString(),
                        icon = Icons.Rounded.FolderCopy,
                        accentColor = ElectricBlue,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("metric_total_apps"),
                        onClick = onOpenMyApps
                    )
                    MetricSummaryCard(
                        title = "APK Available",
                        value = metrics.apkAvailableCount.toString(),
                        icon = Icons.Rounded.Android,
                        accentColor = RoyalPurple,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("metric_apk_available"),
                        onClick = onOpenApkBuilds
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricSummaryCard(
                        title = "APK Downloaded",
                        value = metrics.apkDownloadedCount.toString(),
                        icon = Icons.Rounded.DownloadForOffline,
                        accentColor = StatusSuccess,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenDownloadHistory
                    )
                    MetricSummaryCard(
                        title = "Pending Builds",
                        value = metrics.pendingBuildsCount.toString(),
                        icon = Icons.Rounded.HourglassTop,
                        accentColor = StatusWarning,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenApkBuilds
                    )
                    MetricSummaryCard(
                        title = "Project ZIPs",
                        value = metrics.totalProjectZips.toString(),
                        icon = Icons.Rounded.FolderZip,
                        accentColor = SkyCyan,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenMyApps
                    )
                }
            }
        }

        // 8 Main Action Buttons (2-column large mobile touch grid)
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SectionHeader(
                    title = "Quick Actions",
                    subtitle = "Jump straight into your workflow"
                )

                val actions = listOf(
                    DashboardActionItem(
                        title = "OPEN GOOGLE AI STUDIO",
                        subtitle = "Browser & 6-step export",
                        icon = Icons.AutoMirrored.Rounded.OpenInNew,
                        accent = ElectricBlue,
                        highlighted = true,
                        testTag = "action_open_ai_studio",
                        onClick = onOpenAiStudioGuide
                    ),
                    DashboardActionItem(
                        title = "MY APPS",
                        subtitle = "${metrics.totalApps} project folder(s)",
                        icon = Icons.Rounded.FolderCopy,
                        accent = RoyalPurple,
                        highlighted = true,
                        testTag = "action_my_apps",
                        onClick = onOpenMyApps
                    ),
                    DashboardActionItem(
                        title = "CONNECT GITHUB",
                        subtitle = opState.githubSession?.let { "@${it.username}" } ?: "OAuth Device / PKCE",
                        icon = Icons.Rounded.Code,
                        accent = SkyCyan,
                        testTag = "action_connect_github",
                        onClick = onConnectGitHub
                    ),
                    DashboardActionItem(
                        title = "IMPORT PROJECT ZIP",
                        subtitle = "Save to app Source/",
                        icon = Icons.Rounded.UploadFile,
                        accent = StatusSuccess,
                        testTag = "action_import_zip",
                        onClick = onImportProjectZip
                    ),
                    DashboardActionItem(
                        title = "ADD NEW APP",
                        subtitle = "Create app folder",
                        icon = Icons.Rounded.AddCircle,
                        accent = ElectricBlue,
                        testTag = "action_add_new_app",
                        onClick = onAddNewApp
                    ),
                    DashboardActionItem(
                        title = "APK DOWNLOADS",
                        subtitle = "Actions builds & APKs",
                        icon = Icons.Rounded.BuildCircle,
                        accent = RoyalPurple,
                        testTag = "action_apk_downloads",
                        onClick = onOpenApkBuilds
                    ),
                    DashboardActionItem(
                        title = "DOWNLOAD HISTORY",
                        subtitle = "Install, share, retry",
                        icon = Icons.Rounded.History,
                        accent = StatusWarning,
                        testTag = "action_download_history",
                        onClick = onOpenDownloadHistory
                    ),
                    DashboardActionItem(
                        title = "SETTINGS",
                        subtitle = "Vault, Wi-Fi, backup",
                        icon = Icons.Rounded.Settings,
                        accent = TextSecondary,
                        testTag = "action_settings",
                        onClick = onOpenSettings
                    )
                )

                actions.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        pair.forEach { item ->
                            DashboardActionTile(
                                item = item,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // First-run sample data helper (explicitly labeled DEMO DATA, never fake builds)
        if (metrics.totalApps == 0) {
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    EmptyStateCard(
                        icon = Icons.Rounded.Apps,
                        title = "Your App Vault Is Ready",
                        message = "Register your first Google AI Studio app to create its dedicated Source/, APK/, Build Artifacts/, and History/ folders—or load clearly labeled DEMO DATA folders to preview the layout.",
                        primaryActionLabel = "ADD YOUR FIRST APP",
                        onPrimaryAction = onAddNewApp,
                        secondaryActionLabel = "Load Sample Folders (DEMO DATA)",
                        onSecondaryAction = onLoadDemoWorkspace
                    )
                }
            }
        }

        // Recent Activity Feed
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                SectionHeader(
                    title = "Recent Activity",
                    subtitle = "Apps added, builds synced, APK downloads & failures"
                )
            }
        }

        if (recentActivity.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = NavySurface),
                    border = BorderStroke(1.dp, NavyBorder)
                ) {
                    Text(
                        text = "No activity recorded yet. Add an app folder, import a project ZIP, or connect GitHub to see live updates here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }
        } else {
            items(recentActivity.take(8), key = { it.id }) { log ->
                ActivityLogRow(
                    log = log,
                    onClick = { log.appId?.let(onOpenAppDetail) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun DashboardHeroHeader(
    opState: UiOperationState,
    onRefreshAll: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(216.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_dashboard_hero),
            contentDescription = "AI Studio App Manager dashboard banner",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(HeroOverlayGradient)
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_app_icon_fg),
                        contentDescription = "AI Studio App Manager Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Surface(
                        color = NavySurfaceElevated.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (opState.githubSession != null) Icons.Rounded.CloudDone else Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = if (opState.githubSession != null) StatusSuccess else ElectricBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = opState.githubSession?.let { "GitHub: @${it.username}" } ?: "Personal App Vault",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextPrimary
                            )
                        }
                    }
                }

                Surface(
                    color = NavySurfaceElevated.copy(alpha = 0.85f),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, NavyBorder)
                ) {
                    IconButton(
                        onClick = onRefreshAll,
                        enabled = !opState.isRefreshingAll,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("dashboard_refresh_all_button")
                    ) {
                        if (opState.isRefreshingAll) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = ElectricBlue
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Refresh all connected GitHub repositories",
                                tint = TextPrimary
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "AI STUDIO APP MANAGER",
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Manage, Organize & Download Your AI Apps.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

private data class DashboardActionItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: Color,
    val highlighted: Boolean = false,
    val testTag: String,
    val onClick: () -> Unit
)

@Composable
private fun DashboardActionTile(
    item: DashboardActionItem,
    modifier: Modifier = Modifier
) {
    val borderBrush = if (item.highlighted) {
        Brush.linearGradient(listOf(ElectricBlue.copy(alpha = 0.65f), RoyalPurple.copy(alpha = 0.65f)))
    } else {
        Brush.linearGradient(listOf(NavyBorder, NavyBorder))
    }

    Card(
        modifier = modifier
            .height(98.dp)
            .testTag(item.testTag)
            .clickable(onClick = item.onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        border = BorderStroke(1.dp, borderBrush)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (item.highlighted) Modifier.background(AccentCardGradient) else Modifier)
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(item.accent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(item.icon, contentDescription = null, tint = item.accent, modifier = Modifier.size(19.dp))
                }
                Column {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityLogRow(
    log: ActivityLogEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, tint) = when (log.type) {
        ActivityType.APP_ADDED -> Icons.Rounded.FolderCopy to ElectricBlue
        ActivityType.ZIP_IMPORTED -> Icons.Rounded.FolderZip to SkyCyan
        ActivityType.BUILD_UPDATED -> Icons.Rounded.BuildCircle to RoyalPurple
        ActivityType.APK_DOWNLOADED -> Icons.Rounded.CheckCircle to StatusSuccess
        ActivityType.DOWNLOAD_FAILED -> Icons.Rounded.Error to StatusError
        ActivityType.APK_INSTALLED -> Icons.Rounded.InstallMobile to StatusSuccess
        ActivityType.GITHUB_SYNCED -> Icons.Rounded.Code to StatusInfo
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = log.appId != null, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        border = BorderStroke(1.dp, NavyBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tint.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = log.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = log.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatTimestamp(log.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
        }
    }
}
