package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.AppProjectEntity
import com.example.data.local.ProjectType
import com.example.ui.components.AppIconAvatar
import com.example.ui.components.AppSymbolOptions
import com.example.ui.components.GradientPrimaryButton
import com.example.ui.components.iconForSymbolKey
import com.example.ui.components.parseHexColor
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.FolderBadgeColors
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.NavyBorder
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceElevated
import com.example.ui.theme.SkyCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAppScreen(
    existingApp: AppProjectEntity?,
    onBack: () -> Unit,
    onSave: (
        name: String,
        description: String,
        aiStudioUrl: String,
        githubRepoUrl: String,
        projectType: ProjectType,
        versionName: String,
        notes: String,
        iconUri: String?,
        iconColorHex: String,
        iconSymbolKey: String
    ) -> Unit,
    onConnectGitHub: () -> Unit,
    onSaveAndImportZip: (
        name: String,
        description: String,
        aiStudioUrl: String,
        githubRepoUrl: String,
        projectType: ProjectType,
        versionName: String,
        notes: String,
        iconUri: String?,
        iconColorHex: String,
        iconSymbolKey: String
    ) -> Unit
) {
    val context = LocalContext.current
    var name by remember(existingApp?.id) { mutableStateOf(existingApp?.name ?: "") }
    var description by remember(existingApp?.id) { mutableStateOf(existingApp?.description ?: "") }
    var aiStudioUrl by remember(existingApp?.id) { mutableStateOf(existingApp?.aiStudioUrl ?: "") }
    var githubRepoUrl by remember(existingApp?.id) { mutableStateOf(existingApp?.githubRepoUrl ?: "") }
    var projectType by remember(existingApp?.id) {
        mutableStateOf(existingApp?.projectType ?: ProjectType.NATIVE_ANDROID)
    }
    var versionName by remember(existingApp?.id) { mutableStateOf(existingApp?.versionName ?: "1.0.0") }
    var notes by remember(existingApp?.id) { mutableStateOf(existingApp?.notes ?: "") }
    var iconUri by remember(existingApp?.id) { mutableStateOf(existingApp?.iconUri) }
    var iconColorHex by remember(existingApp?.id) { mutableStateOf(existingApp?.iconColorHex ?: "#3B82F6") }
    var iconSymbolKey by remember(existingApp?.id) { mutableStateOf(existingApp?.iconSymbolKey ?: "auto_awesome") }

    // Zero-permission Android Photo Picker for custom app icons
    val iconPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            iconUri = uri.toString()
        }
    }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = ElectricBlue,
        unfocusedBorderColor = NavyBorder,
        focusedContainerColor = NavySurface,
        unfocusedContainerColor = NavySurface,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary,
        focusedLabelColor = ElectricBlue,
        unfocusedLabelColor = TextMuted
    )

    Scaffold(
        containerColor = NavyBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (existingApp == null) "ADD NEW APP" else "EDIT APP FOLDER",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("add_app_back_button")) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Icon Selector
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Application Icon",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextSecondary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        AppIconAvatar(
                            iconUri = iconUri,
                            colorHex = iconColorHex,
                            symbolKey = iconSymbolKey,
                            contentDescription = "Selected app icon",
                            size = 64.dp
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    iconPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, NavyBorder),
                                modifier = Modifier.testTag("pick_app_icon_button")
                            ) {
                                Icon(Icons.Rounded.Image, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.size(8.dp))
                                Text("Choose Photo Icon", color = TextPrimary)
                            }
                            if (!iconUri.isNullOrBlank()) {
                                Text(
                                    text = "Use Vector Badge Instead",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SkyCyan,
                                    modifier = Modifier.clickable { iconUri = null }
                                )
                            }
                        }
                    }

                    // Color Swatches
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(FolderBadgeColors) { hex ->
                            val selected = iconColorHex == hex
                            val color = parseHexColor(hex)
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (selected) 3.dp else 1.dp,
                                        color = if (selected) TextPrimary else NavyBorder,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        iconColorHex = hex
                                        iconUri = null
                                    }
                            )
                        }
                    }

                    // Symbol Picker
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(AppSymbolOptions) { (key, label) ->
                            val selected = iconSymbolKey == key && iconUri == null
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    iconSymbolKey = key
                                    iconUri = null
                                },
                                leadingIcon = {
                                    Icon(
                                        iconForSymbolKey(key),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = NavySurface,
                                    selectedContainerColor = ElectricBlue.copy(alpha = 0.22f),
                                    selectedLabelColor = ElectricBlue,
                                    selectedLeadingIconColor = ElectricBlue
                                )
                            )
                        }
                    }
                }
            }

            // Form Inputs
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Application Name *") },
                        placeholder = { Text("e.g., VIRAL REEL AI", color = TextMuted) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = textFieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("app_name_input")
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Project Description") },
                        placeholder = { Text("What does this AI Studio app do?", color = TextMuted) },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(14.dp),
                        colors = textFieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("app_description_input")
                    )

                    // Project Type Selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Project Type",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextSecondary
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(ProjectType.entries) { type ->
                                val selected = type == projectType
                                FilterChip(
                                    selected = selected,
                                    onClick = { projectType = type },
                                    label = { Text(type.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = NavySurface,
                                        labelColor = TextSecondary,
                                        selectedContainerColor = ElectricBlue.copy(alpha = 0.22f),
                                        selectedLabelColor = ElectricBlue
                                    ),
                                    modifier = Modifier.testTag("project_type_${type.name.lowercase()}")
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = aiStudioUrl,
                        onValueChange = { aiStudioUrl = it },
                        label = { Text("Google AI Studio Project URL") },
                        placeholder = { Text("https://aistudio.google.com/...", color = TextMuted) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = textFieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_studio_url_input")
                    )

                    OutlinedTextField(
                        value = githubRepoUrl,
                        onValueChange = { githubRepoUrl = it },
                        label = { Text("GitHub Repository URL") },
                        placeholder = { Text("https://github.com/owner/repo", color = TextMuted) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = textFieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("github_repo_url_input")
                    )

                    OutlinedTextField(
                        value = versionName,
                        onValueChange = { versionName = it },
                        label = { Text("Version") },
                        placeholder = { Text("1.0.0", color = TextMuted) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = textFieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("app_version_input")
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Project Notes") },
                        placeholder = { Text("Signing notes, build instructions, changelog...", color = TextMuted) },
                        minLines = 3,
                        maxLines = 6,
                        shape = RoundedCornerShape(14.dp),
                        colors = textFieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("app_notes_input")
                    )
                }
            }

            // Action Buttons: SAVE APP, IMPORT SOURCE ZIP, CONNECT GITHUB REPOSITORY
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    GradientPrimaryButton(
                        text = if (existingApp == null) "SAVE APP" else "SAVE CHANGES",
                        icon = Icons.Rounded.Save,
                        enabled = name.isNotBlank(),
                        testTag = "save_app_button",
                        onClick = {
                            onSave(
                                name,
                                description,
                                aiStudioUrl,
                                githubRepoUrl,
                                projectType,
                                versionName,
                                notes,
                                iconUri,
                                iconColorHex,
                                iconSymbolKey
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedButton(
                        onClick = {
                            onSaveAndImportZip(
                                name,
                                description,
                                aiStudioUrl,
                                githubRepoUrl,
                                projectType,
                                versionName,
                                notes,
                                iconUri,
                                iconColorHex,
                                iconSymbolKey
                            )
                        },
                        enabled = name.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, NavyBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_and_import_zip_button")
                    ) {
                        Icon(Icons.Rounded.FolderZip, contentDescription = null, tint = SkyCyan, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text("IMPORT SOURCE ZIP", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = onConnectGitHub,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, NavyBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("add_app_connect_github_button")
                    ) {
                        Icon(Icons.Rounded.Code, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text("CONNECT GITHUB REPOSITORY", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}
