package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "ai_studio_app_manager_settings"
)

data class AppSettings(
    val aiStudioUrl: String = DEFAULT_AI_STUDIO_URL,
    val customStorageTreeUri: String? = null,
    val customStorageDisplayLabel: String = "App-Managed Vault (Private Storage)",
    val wifiOnlyDownloads: Boolean = false,
    val autoRefreshEnabled: Boolean = false,
    val autoRefreshIntervalHours: Int = 6,
    val notificationsEnabled: Boolean = true,
    val customGitHubClientId: String = ""
) {
    val effectiveGitHubClientId: String
        get() {
            val fromSettings = customGitHubClientId.trim()
            if (fromSettings.isNotBlank()) return fromSettings
            val fromBuild = BuildConfig.GITHUB_CLIENT_ID.trim()
            return if (fromBuild.isNotBlank() && !fromBuild.startsWith("YOUR_")) fromBuild else ""
        }

    val effectiveGoogleWebClientId: String
        get() {
            val fromBuild = BuildConfig.GOOGLE_WEB_CLIENT_ID.trim()
            return if (fromBuild.isNotBlank() && !fromBuild.startsWith("YOUR_")) fromBuild else ""
        }

    companion object {
        const val DEFAULT_AI_STUDIO_URL = "https://aistudio.google.com/"
        const val DEFAULT_AI_STUDIO_APPS_URL = "https://aistudio.google.com/apps"
    }
}

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val AI_STUDIO_URL = stringPreferencesKey("ai_studio_url")
        val CUSTOM_STORAGE_URI = stringPreferencesKey("custom_storage_uri")
        val CUSTOM_STORAGE_LABEL = stringPreferencesKey("custom_storage_label")
        val WIFI_ONLY = booleanPreferencesKey("wifi_only_downloads")
        val AUTO_REFRESH = booleanPreferencesKey("auto_refresh_enabled")
        val AUTO_REFRESH_HOURS = intPreferencesKey("auto_refresh_hours")
        val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        val GITHUB_CLIENT_ID = stringPreferencesKey("custom_github_client_id")
    }

    val settingsFlow: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            aiStudioUrl = prefs[Keys.AI_STUDIO_URL] ?: AppSettings.DEFAULT_AI_STUDIO_URL,
            customStorageTreeUri = prefs[Keys.CUSTOM_STORAGE_URI],
            customStorageDisplayLabel = prefs[Keys.CUSTOM_STORAGE_LABEL]
                ?: "App-Managed Vault (AI Studio App Manager/My Apps)",
            wifiOnlyDownloads = prefs[Keys.WIFI_ONLY] ?: false,
            autoRefreshEnabled = prefs[Keys.AUTO_REFRESH] ?: false,
            autoRefreshIntervalHours = prefs[Keys.AUTO_REFRESH_HOURS] ?: 6,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS] ?: true,
            customGitHubClientId = prefs[Keys.GITHUB_CLIENT_ID] ?: ""
        )
    }

    suspend fun getSnapshot(): AppSettings = settingsFlow.first()

    suspend fun setAiStudioUrl(url: String) {
        context.settingsDataStore.edit { it[Keys.AI_STUDIO_URL] = url.trim() }
    }

    suspend fun setCustomStorageLocation(treeUri: String?, displayLabel: String) {
        context.settingsDataStore.edit { prefs ->
            if (treeUri == null) {
                prefs.remove(Keys.CUSTOM_STORAGE_URI)
            } else {
                prefs[Keys.CUSTOM_STORAGE_URI] = treeUri
            }
            prefs[Keys.CUSTOM_STORAGE_LABEL] = displayLabel
        }
    }

    suspend fun setWifiOnlyDownloads(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.WIFI_ONLY] = enabled }
    }

    suspend fun setAutoRefresh(enabled: Boolean, intervalHours: Int = 6) {
        context.settingsDataStore.edit {
            it[Keys.AUTO_REFRESH] = enabled
            it[Keys.AUTO_REFRESH_HOURS] = intervalHours.coerceIn(1, 24)
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.NOTIFICATIONS] = enabled }
    }

    suspend fun setCustomGitHubClientId(clientId: String) {
        context.settingsDataStore.edit { it[Keys.GITHUB_CLIENT_ID] = clientId.trim() }
    }
}
