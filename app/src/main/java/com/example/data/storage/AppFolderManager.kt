package com.example.data.storage

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import com.example.security.SafeZipAndApkValidator
import com.example.security.UrlValidator
import com.example.security.ZipExtractionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

data class StorageUsageSummary(
    val totalBytes: Long = 0L,
    val sourceBytes: Long = 0L,
    val apkBytes: Long = 0L,
    val artifactsBytes: Long = 0L,
    val historyBytes: Long = 0L,
    val appFolderCount: Int = 0,
    val rootPathDisplay: String = ""
)

data class AppFolderPaths(
    val rootDir: File,
    val sourceDir: File,
    val apkDir: File,
    val buildArtifactsDir: File,
    val historyDir: File
)

data class ImportedFileResult(
    val localFile: File,
    val fileName: String,
    val fileSizeBytes: Long
)

/**
 * Automatically organizes every application's files into a deterministic, isolated folder tree:
 *   AI Studio App Manager/My Apps/<APP_NAME>/
 *       Source/
 *       APK/
 *       Build Artifacts/
 *       History/
 *
 * Uses app-specific external/internal storage by default (zero broad storage permissions)
 * and supports optional Storage Access Framework (SAF) mirroring to a user-selected directory.
 */
class AppFolderManager(private val context: Context) {

    val baseRootDir: File
        get() {
            val base = context.getExternalFilesDir(null) ?: context.filesDir
            return File(base, "$ROOT_DIR_NAME/$MY_APPS_DIR_NAME").apply { mkdirs() }
        }

    fun ensureAppFolderStructure(folderName: String): AppFolderPaths {
        val safeName = UrlValidator.sanitizeFolderName(folderName)
        val appRoot = File(baseRootDir, safeName).apply { mkdirs() }
        val source = File(appRoot, DIR_SOURCE).apply { mkdirs() }
        val apk = File(appRoot, DIR_APK).apply { mkdirs() }
        val artifacts = File(appRoot, DIR_BUILD_ARTIFACTS).apply { mkdirs() }
        val history = File(appRoot, DIR_HISTORY).apply { mkdirs() }
        return AppFolderPaths(appRoot, source, apk, artifacts, history)
    }

    fun renameAppFolder(oldFolderName: String, newAppName: String): String {
        val safeOld = UrlValidator.sanitizeFolderName(oldFolderName)
        val safeNew = UrlValidator.sanitizeFolderName(newAppName)
        if (safeOld == safeNew) {
            ensureAppFolderStructure(safeNew)
            return safeNew
        }
        val oldDir = File(baseRootDir, safeOld)
        var targetName = safeNew
        var targetDir = File(baseRootDir, targetName)
        var suffix = 2
        while (targetDir.exists()) {
            targetName = "${safeNew}_$suffix"
            targetDir = File(baseRootDir, targetName)
            suffix++
        }
        if (oldDir.exists()) {
            val renamed = oldDir.renameTo(targetDir)
            if (!renamed) {
                oldDir.copyRecursively(targetDir, overwrite = true)
                oldDir.deleteRecursively()
            }
        }
        ensureAppFolderStructure(targetName)
        return targetName
    }

    fun deleteAppFolder(folderName: String): Boolean {
        val safeName = UrlValidator.sanitizeFolderName(folderName)
        val dir = File(baseRootDir, safeName)
        return if (dir.exists()) dir.deleteRecursively() else true
    }

    /**
     * Copies a user-selected file from an Android SAF [Uri] into the app's Source/ directory,
     * preserving the original archive untouched.
     */
    suspend fun importSourceZipFromUri(
        folderName: String,
        sourceUri: Uri,
        customTreeUri: String? = null
    ): Result<ImportedFileResult> = withContext(Dispatchers.IO) {
        runCatching {
            val paths = ensureAppFolderStructure(folderName)
            val originalName = queryDisplayName(sourceUri) ?: "source_${System.currentTimeMillis()}.zip"
            val sanitizedName = UrlValidator.sanitizeFileName(
                originalName,
                "source_${System.currentTimeMillis()}.zip"
            )
            val finalName = ensureUniqueFileName(paths.sourceDir, sanitizedName)
            val destFile = File(paths.sourceDir, finalName)

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output, bufferSize = 8192)
                }
            } ?: error("Unable to read selected file from storage.")

            if (!SafeZipAndApkValidator.hasValidZipMagicHeader(destFile)) {
                destFile.delete()
                error("Selected file is not a valid ZIP archive.")
            }

            if (!customTreeUri.isNullOrBlank()) {
                mirrorFileToSafTree(customTreeUri, folderName, DIR_SOURCE, destFile, "application/zip")
            }

            ImportedFileResult(
                localFile = destFile,
                fileName = destFile.name,
                fileSizeBytes = destFile.length()
            )
        }
    }

    /**
     * Copies a user-selected APK file from Android storage into the app's APK/ directory
     * after validating its APK structure.
     */
    suspend fun importApkFromUri(
        folderName: String,
        apkUri: Uri,
        allowOverwrite: Boolean = false
    ): Result<ImportedFileResult> = withContext(Dispatchers.IO) {
        runCatching {
            val paths = ensureAppFolderStructure(folderName)
            val originalName = queryDisplayName(apkUri) ?: "app_${System.currentTimeMillis()}.apk"
            val sanitizedName = UrlValidator.sanitizeFileName(originalName, "app.apk")
            val destName = if (allowOverwrite) sanitizedName else ensureUniqueFileName(paths.apkDir, sanitizedName)
            val tempFile = File(paths.buildArtifactsDir, "staging_${System.currentTimeMillis()}.apk")

            context.contentResolver.openInputStream(apkUri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output, bufferSize = 8192)
                }
            } ?: error("Unable to read selected APK file.")

            val validation = SafeZipAndApkValidator.validateApkFile(tempFile, context)
            if (!validation.isValidApk) {
                tempFile.delete()
                error(validation.errorMessage ?: "Invalid APK file.")
            }

            val finalFile = File(paths.apkDir, destName)
            tempFile.copyTo(finalFile, overwrite = true)
            tempFile.delete()

            ImportedFileResult(
                localFile = finalFile,
                fileName = finalFile.name,
                fileSizeBytes = finalFile.length()
            )
        }
    }

    /**
     * Extracts a source ZIP safely into a sibling folder inside Source/extracted_<name>/
     * while preserving the original ZIP file intact.
     */
    suspend fun extractSourceZip(sourceZipFile: File): ZipExtractionResult = withContext(Dispatchers.IO) {
        val extractFolderName = "extracted_${sourceZipFile.nameWithoutExtension}"
        val targetDir = File(sourceZipFile.parentFile ?: baseRootDir, extractFolderName)
        SafeZipAndApkValidator.extractZipSafely(sourceZipFile, targetDir)
    }

    /**
     * Checks if an APK with the same file name already exists in the app's APK/ directory.
     */
    fun apkAlreadyExists(folderName: String, apkFileName: String): Boolean {
        val paths = ensureAppFolderStructure(folderName)
        val safeFile = UrlValidator.sanitizeFileName(apkFileName, "app.apk")
        return File(paths.apkDir, safeFile).exists()
    }

    /**
     * Saves a verified APK file into the app's APK/ directory, optionally mirroring to a SAF custom folder.
     */
    suspend fun saveVerifiedApk(
        folderName: String,
        sourceApkFile: File,
        desiredFileName: String,
        overwrite: Boolean,
        customTreeUri: String? = null
    ): File = withContext(Dispatchers.IO) {
        val paths = ensureAppFolderStructure(folderName)
        val safeName = UrlValidator.sanitizeFileName(desiredFileName, "app.apk")
        val finalName = if (overwrite) safeName else ensureUniqueFileName(paths.apkDir, safeName)
        val destFile = File(paths.apkDir, finalName)
        sourceApkFile.copyTo(destFile, overwrite = true)

        if (!customTreeUri.isNullOrBlank()) {
            mirrorFileToSafTree(
                treeUriString = customTreeUri,
                appFolderName = folderName,
                subFolder = DIR_APK,
                file = destFile,
                mimeType = "application/vnd.android.package-archive"
            )
        }
        destFile
    }

    /**
     * Mirrors a file into a user-chosen Storage Access Framework (SAF) directory tree.
     */
    fun mirrorFileToSafTree(
        treeUriString: String,
        appFolderName: String,
        subFolder: String,
        file: File,
        mimeType: String
    ): Boolean {
        return runCatching {
            val rootDoc = DocumentFile.fromTreeUri(context, Uri.parse(treeUriString)) ?: return false
            val rootAppMgr = rootDoc.findFile(ROOT_DIR_NAME) ?: rootDoc.createDirectory(ROOT_DIR_NAME) ?: return false
            val myApps = rootAppMgr.findFile(MY_APPS_DIR_NAME) ?: rootAppMgr.createDirectory(MY_APPS_DIR_NAME) ?: return false
            val safeApp = UrlValidator.sanitizeFolderName(appFolderName)
            val appDir = myApps.findFile(safeApp) ?: myApps.createDirectory(safeApp) ?: return false
            val targetSub = appDir.findFile(subFolder) ?: appDir.createDirectory(subFolder) ?: return false

            targetSub.findFile(file.name)?.delete()
            val outDoc = targetSub.createFile(mimeType, file.name) ?: return false
            context.contentResolver.openOutputStream(outDoc.uri)?.use { out ->
                FileInputStream(file).use { input ->
                    input.copyTo(out)
                }
            }
            true
        }.getOrDefault(false)
    }

    /**
     * Enqueues a public HTTPS download via Android's system [DownloadManager] when appropriate.
     */
    fun enqueuePublicDownload(
        httpsUrl: String,
        fileName: String,
        title: String,
        wifiOnly: Boolean
    ): Long? {
        if (!UrlValidator.isValidHttpsUrl(httpsUrl)) return null
        return runCatching {
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager ?: return null
            val request = DownloadManager.Request(Uri.parse(httpsUrl))
                .setTitle(title)
                .setDescription("Downloading $fileName")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedOverMetered(!wifiOnly)
                .setDestinationInExternalFilesDir(
                    context,
                    "$ROOT_DIR_NAME/$MY_APPS_DIR_NAME",
                    UrlValidator.sanitizeFileName(fileName, "download.zip")
                )
            dm.enqueue(request)
        }.getOrNull()
    }

    suspend fun calculateStorageUsage(): StorageUsageSummary = withContext(Dispatchers.IO) {
        val root = baseRootDir
        var sourceBytes = 0L
        var apkBytes = 0L
        var artifactsBytes = 0L
        var historyBytes = 0L
        var folderCount = 0

        root.listFiles()?.filter { it.isDirectory }?.forEach { appDir ->
            folderCount++
            sourceBytes += folderSize(File(appDir, DIR_SOURCE))
            apkBytes += folderSize(File(appDir, DIR_APK))
            artifactsBytes += folderSize(File(appDir, DIR_BUILD_ARTIFACTS))
            historyBytes += folderSize(File(appDir, DIR_HISTORY))
        }

        StorageUsageSummary(
            totalBytes = sourceBytes + apkBytes + artifactsBytes + historyBytes,
            sourceBytes = sourceBytes,
            apkBytes = apkBytes,
            artifactsBytes = artifactsBytes,
            historyBytes = historyBytes,
            appFolderCount = folderCount,
            rootPathDisplay = "$ROOT_DIR_NAME/$MY_APPS_DIR_NAME/"
        )
    }

    fun deleteFileSafely(path: String): Boolean {
        if (path.isBlank()) return false
        return runCatching {
            val file = File(path).canonicalFile
            val rootCanonical = baseRootDir.canonicalFile
            if (!file.path.startsWith(rootCanonical.path + File.separator)) {
                return false
            }
            if (file.isDirectory) file.deleteRecursively() else file.delete()
        }.getOrDefault(false)
    }

    private fun folderSize(dir: File): Long {
        if (!dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    private fun ensureUniqueFileName(dir: File, fileName: String): String {
        var candidate = fileName
        val base = fileName.substringBeforeLast('.', fileName)
        val ext = fileName.substringAfterLast('.', "")
        var counter = 2
        while (File(dir, candidate).exists()) {
            candidate = if (ext.isNotEmpty()) "${base}_v$counter.$ext" else "${base}_v$counter"
            counter++
        }
        return candidate
    }

    private fun queryDisplayName(uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index >= 0) cursor.getString(index) else null
                    } else null
                }
        }.getOrNull()
    }

    companion object {
        const val ROOT_DIR_NAME = "AI Studio App Manager"
        const val MY_APPS_DIR_NAME = "My Apps"
        const val DIR_SOURCE = "Source"
        const val DIR_APK = "APK"
        const val DIR_BUILD_ARTIFACTS = "Build Artifacts"
        const val DIR_HISTORY = "History"
    }
}
