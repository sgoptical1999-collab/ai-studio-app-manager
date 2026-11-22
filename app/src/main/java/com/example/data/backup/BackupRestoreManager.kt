package com.example.data.backup

import android.content.Context
import android.net.Uri
import com.example.data.github.GitHubNetworkFactory
import com.example.data.local.AppDatabase
import com.example.data.local.AppProjectEntity
import com.example.data.storage.AppFolderManager
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@JsonClass(generateAdapter = true)
data class AppRecordsBackupPayload(
    val schemaVersion: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val apps: List<AppProjectEntity> = emptyList()
)

/**
 * Exports and restores app project records as clean JSON using Android Storage Access Framework.
 * Credentials and tokens are NEVER included in backups.
 */
class BackupRestoreManager(
    private val context: Context,
    private val database: AppDatabase,
    private val folderManager: AppFolderManager
) {
    private val adapter = GitHubNetworkFactory.moshi.adapter(AppRecordsBackupPayload::class.java)

    suspend fun exportRecordsToUri(destinationUri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val apps = database.appProjectDao().getAllAppsSnapshot()
            val payload = AppRecordsBackupPayload(
                schemaVersion = 1,
                exportedAt = System.currentTimeMillis(),
                apps = apps
            )
            val json = adapter.indent("  ").toJson(payload)
            context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                out.write(json.toByteArray(Charsets.UTF_8))
            } ?: error("Could not open backup destination for writing.")
            apps.size
        }
    }

    suspend fun restoreRecordsFromUri(sourceUri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val json = context.contentResolver.openInputStream(sourceUri)?.use { input ->
                input.bufferedReader(Charsets.UTF_8).readText()
            } ?: error("Could not read selected backup file.")

            val payload = adapter.fromJson(json)
                ?: error("Invalid or unsupported backup file format.")

            var restored = 0
            for (app in payload.apps) {
                val safeFolder = folderManager.ensureAppFolderStructure(app.folderName).rootDir.name
                database.appProjectDao().insertApp(
                    app.copy(
                        id = 0L,
                        folderName = safeFolder,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                restored++
            }
            restored
        }
    }
}
