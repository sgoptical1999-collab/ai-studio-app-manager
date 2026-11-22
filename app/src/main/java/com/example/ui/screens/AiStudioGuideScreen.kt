package com.example.ui.screens

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.preferences.AppSettings
import com.example.security.GoogleAccountSession
import com.example.ui.components.GradientPrimaryButton
import com.example.ui.components.InfoNoticeBanner
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.NavyBorder
import com.example.ui.theme.NavySurface
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.SkyCyan
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiStudioGuideScreen(
    settings: AppSettings,
    googleAccount: GoogleAccountSession?,
    onBack: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onSignInWithGoogle: () -> Unit,
    onAddNewApp: () -> Unit,
    onImportZip: () -> Unit
) {
    val steps = listOf(
        "Open Google AI Studio" to "Launch https://aistudio.google.com/ in your official browser and sign in securely with your Google account.",
        "Open My Apps" to "Navigate to Build / My Apps inside Google AI Studio to view your created projects.",
        "Select the application you created" to "Open the specific AI Studio project you want to organize or build.",
        "Export or download the project ZIP" to "Export the project source ZIP (or push it to GitHub directly from Google AI Studio).",
        "Return to AI STUDIO APP MANAGER" to "Switch back to this app—your vault keeps every app organized in its own folder.",
        "Import the ZIP or add its project link" to "Save the Google AI Studio project URL, import the exported ZIP into Source/, or connect its GitHub repo for APK builds."
    )

    Scaffold(
        containerColor = NavyBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "GOOGLE AI STUDIO",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("ai_studio_back_button")) {
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
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = NavySurface),
                    border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Official Browser Shortcuts",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Sign in directly on Google's official website. AI Studio App Manager never asks for or stores your Google password.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        GradientPrimaryButton(
                            text = "OPEN GOOGLE AI STUDIO",
                            icon = Icons.AutoMirrored.Rounded.OpenInNew,
                            onClick = { onOpenUrl(settings.aiStudioUrl) },
                            testTag = "open_google_ai_studio_button",
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedButton(
                            onClick = { onOpenUrl(AppSettings.DEFAULT_AI_STUDIO_APPS_URL) },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, NavyBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("open_ai_studio_my_apps_shortcut")
                        ) {
                            Icon(Icons.Rounded.Apps, contentDescription = null, tint = RoyalPurple, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text("OPEN BUILD / MY APPS SHORTCUT", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = onSignInWithGoogle,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, if (googleAccount != null) StatusSuccess else NavyBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("google_account_sign_in_button")
                        ) {
                            Icon(
                                Icons.Rounded.AccountCircle,
                                contentDescription = null,
                                tint = if (googleAccount != null) StatusSuccess else SkyCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.size(8.dp))
                            Text(
                                text = googleAccount?.let { "Google Account: ${it.email}" } ?: "Connect Google Account",
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            item {
                InfoNoticeBanner(
                    title = "Supported Official Workflow",
                    body = stringResource(R.string.ai_studio_disclaimer),
                    accent = StatusInfo,
                    icon = Icons.Rounded.Info
                )
            }

            item {
                Text(
                    text = "6-STEP EXPORT & REGISTER WORKFLOW",
                    style = MaterialTheme.typography.titleSmall,
                    color = ElectricBlue,
                    fontWeight = FontWeight.Bold
                )
            }

            itemsIndexed(steps) { index, (title, desc) ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = NavySurface),
                    border = BorderStroke(1.dp, NavyBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(ElectricBlue.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.titleSmall,
                                color = ElectricBlue,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = "STEP ${index + 1}: $title",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onAddNewApp,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, NavyBorder),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Rounded.AddCircle, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Save URL", color = TextPrimary)
                    }
                    OutlinedButton(
                        onClick = onImportZip,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, NavyBorder),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Rounded.UploadFile, contentDescription = null, tint = SkyCyan, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Import ZIP", color = TextPrimary)
                    }
                }
            }
        }
    }
}
