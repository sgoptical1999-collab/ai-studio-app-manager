package com.example.ui.components

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.BuildStatus
import com.example.data.local.DownloadStatus
import com.example.data.repository.AppManagerRepository
import com.example.ui.PendingInstallRequest
import com.example.ui.PendingOverwriteRequest
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NavyBorder
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceElevated
import com.example.ui.theme.PrimaryBrandGradient
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusErrorContainer
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusInfoContainer
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessContainer
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.StatusWarningContainer
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun iconForSymbolKey(key: String): ImageVector = when (key) {
    "auto_awesome" -> Icons.Rounded.AutoAwesome
    "android" -> Icons.Rounded.Android
    "apps" -> Icons.Rounded.Apps
    "code" -> Icons.Rounded.Code
    "rocket" -> Icons.Rounded.RocketLaunch
    "receipt" -> Icons.Rounded.ReceiptLong
    "web" -> Icons.Rounded.Language
    "terminal" -> Icons.Rounded.Terminal
    "bolt" -> Icons.Rounded.Bolt
    else -> Icons.Rounded.AutoAwesome
}

val AppSymbolOptions = listOf(
    "auto_awesome" to "AI Spark",
    "android" to "Android",
    "apps" to "App Grid",
    "code" to "Code",
    "rocket" to "Rocket",
    "receipt" to "Invoice",
    "web" to "Web",
    "bolt" to "Fast"
)

fun parseHexColor(hex: String, fallback: Color = ElectricBlue): Color {
    return runCatching {
        Color(android.graphics.Color.parseColor(hex))
    }.getOrDefault(fallback)
}

fun formatTimestamp(epochMs: Long): String {
    if (epochMs <= 0L) return "Not yet"
    val fmt = SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.US)
    return fmt.format(Date(epochMs))
}

fun formatDuration(seconds: Long): String {
    if (seconds <= 0L) return "—"
    val mins = seconds / 60
    val rem = seconds % 60
    return if (mins > 0) "${mins}m ${rem}s" else "${rem}s"
}

@Composable
fun AppIconAvatar(
    iconUri: String?,
    colorHex: String,
    symbolKey: String,
    contentDescription: String,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    val accent = parseHexColor(colorHex)
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(accent.copy(alpha = 0.28f), RoyalPurple.copy(alpha = 0.24f))
                )
            )
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (!iconUri.isNullOrBlank()) {
            AsyncImage(
                model = Uri.parse(iconUri),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = iconForSymbolKey(symbolKey),
                contentDescription = contentDescription,
                tint = accent,
                modifier = Modifier.size(size * 0.52f)
            )
        }
    }
}

@Composable
fun BuildStatusPill(status: BuildStatus, modifier: Modifier = Modifier) {
    val (bg, fg, icon) = when (status) {
        BuildStatus.SUCCESS -> Triple(StatusSuccessContainer, StatusSuccess, Icons.Rounded.CheckCircle)
        BuildStatus.IN_PROGRESS -> Triple(StatusInfoContainer, StatusInfo, Icons.Rounded.HourglassTop)
        BuildStatus.QUEUED -> Triple(StatusWarningContainer, StatusWarning, Icons.Rounded.Schedule)
        BuildStatus.FAILED -> Triple(StatusErrorContainer, StatusError, Icons.Rounded.Error)
        BuildStatus.CANCELLED -> Triple(NavySurfaceElevated, TextMuted, Icons.Rounded.Cancel)
        BuildStatus.NONE -> Triple(NavySurfaceElevated, TextMuted, Icons.Rounded.Schedule)
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, fg.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(13.dp))
            Text(
                text = status.label,
                style = MaterialTheme.typography.labelMedium,
                color = fg,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
fun DownloadStatusPill(status: DownloadStatus, modifier: Modifier = Modifier) {
    val (bg, fg) = when (status) {
        DownloadStatus.COMPLETED -> StatusSuccessContainer to StatusSuccess
        DownloadStatus.DOWNLOADING -> StatusInfoContainer to StatusInfo
        DownloadStatus.EXPIRED -> StatusWarningContainer to StatusWarning
        DownloadStatus.FAILED -> StatusErrorContainer to StatusError
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, fg.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Text(
            text = status.label,
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun DemoDataBadge(modifier: Modifier = Modifier) {
    Surface(
        color = StatusWarningContainer,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, StatusWarning.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Text(
            text = "DEMO DATA",
            style = MaterialTheme.typography.labelSmall,
            color = StatusWarning,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun MetricSummaryCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        border = BorderStroke(1.dp, NavyBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun GradientPrimaryButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    testTag: String = ""
) {
    val brush = if (enabled) PrimaryBrandGradient else Brush.linearGradient(
        listOf(NavySurfaceElevated, NavySurfaceElevated)
    )
    Button(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(0.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        modifier = modifier
            .height(54.dp)
            .then(if (testTag.isNotBlank()) Modifier.testTag(testTag) else Modifier)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush, RoundedCornerShape(16.dp))
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) TextPrimary else TextMuted,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (enabled) TextPrimary else TextMuted,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }
        if (!actionLabel.isNullOrBlank() && onActionClick != null) {
            TextButton(onClick = onActionClick) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = ElectricBlue
                )
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    message: String,
    primaryActionLabel: String? = null,
    onPrimaryAction: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        border = BorderStroke(1.dp, NavyBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(ElectricBlue.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(28.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            if (!primaryActionLabel.isNullOrBlank() && onPrimaryAction != null) {
                Spacer(Modifier.height(4.dp))
                GradientPrimaryButton(
                    text = primaryActionLabel,
                    icon = Icons.Rounded.AutoAwesome,
                    onClick = onPrimaryAction,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (!secondaryActionLabel.isNullOrBlank() && onSecondaryAction != null) {
                OutlinedButton(
                    onClick = onSecondaryAction,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, NavyBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(secondaryActionLabel, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
fun ApkInstallConfirmationDialog(
    request: PendingInstallRequest,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NavySurfaceElevated,
        icon = {
            Icon(
                imageVector = if (request.needsPermissionSettings) Icons.Rounded.Security else Icons.Rounded.Android,
                contentDescription = null,
                tint = if (request.needsPermissionSettings) StatusWarning else StatusSuccess,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = if (request.needsPermissionSettings) {
                    "Allow App Installations"
                } else {
                    "Install Verified APK?"
                },
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (request.needsPermissionSettings) {
                    Text(
                        text = "Android requires your permission to install APK packages from AI Studio App Manager. Tap Continue to open Android Settings, enable 'Allow from this source', and return to install:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                } else {
                    Text(
                        text = "AI Studio App Manager verified this APK file header and manifest. Confirm below to hand it off to Android's official Package Installer:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                Surface(
                    color = NavySurface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, NavyBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = request.apkFile.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                        if (request.packageName.isNotBlank()) {
                            Text(
                                text = "Package: ${request.packageName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                        Text(
                            text = "Size: ${AppManagerRepository.formatBytes(request.apkFile.length())}" +
                                if (request.versionName.isNotBlank()) " • v${request.versionName}" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.testTag("confirm_install_apk_button")
            ) {
                Text(if (request.needsPermissionSettings) "Open Permission Settings" else "Open Package Installer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
fun OverwriteApkConfirmationDialog(
    request: PendingOverwriteRequest,
    onOverwrite: () -> Unit,
    onKeepBoth: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NavySurfaceElevated,
        icon = {
            Icon(
                imageVector = Icons.Rounded.Warning,
                contentDescription = null,
                tint = StatusWarning
            )
        },
        title = {
            Text(
                text = "Existing APK Found",
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "'${request.apkFileName}' already exists in this app's APK/ folder. Would you like to replace the existing file or keep both versions?",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onKeepBoth) {
                    Text("Keep Both")
                }
                Button(onClick = onOverwrite) {
                    Text("Overwrite")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
fun InfoNoticeBanner(
    title: String,
    body: String,
    accent: Color = StatusInfo,
    icon: ImageVector = Icons.Rounded.FolderZip,
    modifier: Modifier = Modifier
) {
    Surface(
        color = accent.copy(alpha = 0.10f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}
