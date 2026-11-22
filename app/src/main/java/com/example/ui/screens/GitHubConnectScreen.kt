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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.github.GitHubRepoDto
import com.example.data.local.AppProjectEntity
import com.example.data.preferences.AppSettings
import com.example.ui.UiOperationState
import com.example.ui.components.GradientPrimaryButton
import com.example.ui.components.InfoNoticeBanner
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitHubConnectScreen(
    apps: List<AppProjectEntity>,
    settings: AppSettings,
    opState: UiOperationState,
    onBack: () -> Unit,
    onStartDeviceFlow: (Boolean) -> Unit,
    onCancelDeviceFlow: () -> Unit,
    onStartPkceFlow: (Boolean) -> Unit,
    onRefreshRepos: () -> Unit,
    onDisconnectGitHub: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onSaveGitHubClientId: (String) -> Unit,
    onLinkRepoToApp: (Long, GitHubRepoDto) -> Unit,
    onCreateAppFromRepo: (GitHubRepoDto) -> Unit,
    onAddManualRepoApp: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var includePrivateRepos by remember { mutableStateOf(true) }
    var repoSearch by remember { mutableStateOf("") }
    var clientIdInput by remember(settings.customGitHubClientId) {
        mutableStateOf(settings.customGitHubClientId)
    }
    var repoPendingLink by remember { mutableStateOf<GitHubRepoDto?>(null) }

    val filteredRepos = remember(opState.githubRepos, repoSearch) {
        val q = repoSearch.trim().lowercase()
        if (q.isEmpty()) opState.githubRepos
        else opState.githubRepos.filter {
            it.name.lowercase().contains(q) || it.fullName.lowercase().contains(q)
        }
    }

    Scaffold(
        containerColor = NavyBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "GITHUB CONNECTION",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("github_back_button")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = TextPrimary)
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Connected Account Card OR OAuth Authorization Card
            item {
                val session = opState.githubSession
                if (session != null) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = NavySurface),
                        border = BorderStroke(1.dp, StatusSuccess.copy(alpha = 0.55f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = StatusSuccess)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "@${session.username}",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Connected via Keystore-Encrypted OAuth • Synced ${formatTimestamp(session.lastSyncedAt)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = onRefreshRepos,
                                    enabled = !opState.isLoadingRepos,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.size(6.dp))
                                    Text("Sync Repos")
                                }
                                OutlinedButton(
                                    onClick = onDisconnectGitHub,
                                    border = BorderStroke(1.dp, StatusError.copy(alpha = 0.6f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("disconnect_github_button")
                                ) {
                                    Icon(Icons.Rounded.Logout, contentDescription = null, tint = StatusError, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.size(6.dp))
                                    Text("Disconnect", color = StatusError)
                                }
                            }
                        }
                    }
                } else {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = NavySurface),
                        border = BorderStroke(1.dp, NavyBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Secure GitHub OAuth Authorization",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Authorize using GitHub's official OAuth Device Flow or OAuth PKCE browser flow. Minimum permissions requested: repository metadata, contents, Actions runs, and artifacts. Never enters passwords or Personal Access Tokens.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Include Private Repositories", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                    Text(
                                        text = if (includePrivateRepos) "Scope: repo read:user" else "Scope: public_repo read:user",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                }
                                Switch(
                                    checked = includePrivateRepos,
                                    onCheckedChange = { includePrivateRepos = it }
                                )
                            }

                            if (settings.effectiveGitHubClientId.isBlank()) {
                                InfoNoticeBanner(
                                    title = "GitHub OAuth App Client ID Required",
                                    body = "Set GITHUB_CLIENT_ID in the AI Studio Secrets panel (or enter your public GitHub OAuth App Client ID below) with Device Flow enabled on GitHub. No client secret is needed.",
                                    accent = StatusWarning,
                                    icon = Icons.Rounded.Security
                                )
                                OutlinedTextField(
                                    value = clientIdInput,
                                    onValueChange = { clientIdInput = it },
                                    label = { Text("GitHub OAuth App Client ID (Public)") },
                                    placeholder = { Text("Ov23li...", color = TextMuted) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ElectricBlue,
                                        unfocusedBorderColor = NavyBorder,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("github_client_id_input")
                                )
                                OutlinedButton(
                                    onClick = { onSaveGitHubClientId(clientIdInput) },
                                    enabled = clientIdInput.isNotBlank(),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Save Public Client ID", color = ElectricBlue)
                                }
                            }

                            GradientPrimaryButton(
                                text = "CONNECT GITHUB (DEVICE OAUTH)",
                                icon = Icons.Rounded.Code,
                                enabled = settings.effectiveGitHubClientId.isNotBlank() && !opState.isPollingDeviceFlow,
                                testTag = "connect_github_device_flow_button",
                                onClick = { onStartDeviceFlow(includePrivateRepos) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedButton(
                                onClick = { onStartPkceFlow(includePrivateRepos) },
                                enabled = settings.effectiveGitHubClientId.isNotBlank(),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, NavyBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("connect_github_pkce_button")
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, tint = SkyCyan, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.size(8.dp))
                                Text("Authorize via Browser (OAuth PKCE)", color = TextPrimary)
                            }
                        }
                    }
                }
            }

            // Active Device Flow Verification Code Card
            opState.deviceFlowChallenge?.let { challenge ->
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = NavySurfaceElevated),
                        border = BorderStroke(1.dp, ElectricBlue),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Enter This Code on GitHub",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color = NavyBackground,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, RoyalPurple)
                            ) {
                                Text(
                                    text = challenge.userCode,
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = SkyCyan,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(onClick = {
                                    clipboard.setText(AnnotatedString(challenge.userCode))
                                }) {
                                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.size(6.dp))
                                    Text("Copy Code")
                                }
                                Button(onClick = { onOpenUrl(challenge.verificationUri) }) {
                                    Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.size(6.dp))
                                    Text("Open GitHub")
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = ElectricBlue)
                                Text(
                                    text = "Waiting for authorization...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                            TextButton(onClick = onCancelDeviceFlow) {
                                Text("Cancel Authorization", color = StatusError)
                            }
                        }
                    }
                }
            }

            // Manual Fallback Card
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = NavySurface),
                    border = BorderStroke(1.dp, NavyBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Manual Repository URL Fallback",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "You can also paste any public GitHub URL when creating or editing an app folder.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                        TextButton(onClick = onAddManualRepoApp) {
                            Text("Add URL", color = ElectricBlue)
                        }
                    }
                }
            }

            // Connected Repositories List
            if (opState.githubSession != null) {
                item {
                    OutlinedTextField(
                        value = repoSearch,
                        onValueChange = { repoSearch = it },
                        placeholder = { Text("Filter your GitHub repositories...", color = TextMuted) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricBlue,
                            unfocusedBorderColor = NavyBorder,
                            focusedContainerColor = NavySurface,
                            unfocusedContainerColor = NavySurface,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (opState.isLoadingRepos) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = ElectricBlue)
                        }
                    }
                } else {
                    items(filteredRepos, key = { it.id }) { repo ->
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
                                    Text(
                                        text = repo.fullName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Surface(
                                        color = NavySurfaceElevated,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (repo.isPrivate) Icons.Rounded.Lock else Icons.Rounded.Public,
                                                contentDescription = null,
                                                tint = if (repo.isPrivate) StatusWarning else StatusSuccess,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = if (repo.isPrivate) "Private" else "Public",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                }
                                if (!repo.description.isNullOrBlank()) {
                                    Text(
                                        text = repo.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { repoPendingLink = repo },
                                        enabled = apps.isNotEmpty(),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Rounded.Link, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(Modifier.size(4.dp))
                                        Text("Link to App", style = MaterialTheme.typography.labelMedium)
                                    }
                                    Button(
                                        onClick = { onCreateAppFromRepo(repo) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(Modifier.size(4.dp))
                                        Text("New Folder", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Select App Folder to Link Dialog
    repoPendingLink?.let { repo ->
        AlertDialog(
            onDismissRequest = { repoPendingLink = null },
            containerColor = NavySurfaceElevated,
            title = {
                Text("Link ${repo.name} to App Folder", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(apps, key = { it.id }) { appItem ->
                        OutlinedButton(
                            onClick = {
                                onLinkRepoToApp(appItem.id, repo)
                                repoPendingLink = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(appItem.name, color = TextPrimary)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { repoPendingLink = null }) {
                    Text("Close", color = TextSecondary)
                }
            }
        )
    }
}
