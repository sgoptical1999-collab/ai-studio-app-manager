package com.example.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

data class ZipExtractionResult(
    val success: Boolean,
    val extractedFiles: List<File> = emptyList(),
    val apkFiles: List<File> = emptyList(),
    val aabFiles: List<File> = emptyList(),
    val totalExtractedBytes: Long = 0L,
    val errorMessage: String? = null
)

data class ApkValidationResult(
    val isValidApk: Boolean,
    val isAabBundle: Boolean = false,
    val packageName: String? = null,
    val versionName: String? = null,
    val versionCode: Long? = null,
    val fileSizeBytes: Long = 0L,
    val errorMessage: String? = null
)

/**
 * Provides hardened ZIP extraction with Zip Slip path-traversal protection,
 * zip-bomb limits, APK structural validation, and AAB detection.
 * Never executes code contained in imported or downloaded archives.
 */
object SafeZipAndApkValidator {

    private const val MAX_EXTRACTED_BYTES: Long = 512L * 1024L * 1024L // 512 MB guard
    private const val MAX_ENTRY_COUNT: Int = 5_000
    private const val BUFFER_SIZE: Int = 8_192

    /**
     * Verifies a file starts with the standard ZIP/APK magic header (PK\x03\x04).
     */
    fun hasValidZipMagicHeader(file: File): Boolean {
        if (!file.exists() || !file.isFile || file.length() < 4L) return false
        return try {
            FileInputStream(file).use { input ->
                val header = ByteArray(4)
                val read = input.read(header)
                read == 4 &&
                    header[0] == 0x50.toByte() &&
                    header[1] == 0x4B.toByte() &&
                    header[2] == 0x03.toByte() &&
                    header[3] == 0x04.toByte()
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Resolves a [ZipEntry] path against [destinationDir] and enforces strict canonical confinement
     * to prevent Zip Slip path traversal attacks (e.g., ../../etc/passwd or absolute paths).
     */
    @Throws(SecurityException::class)
    fun resolveSafeEntryFile(destinationDir: File, entryName: String): File {
        if (entryName.isBlank() || entryName.startsWith("/") || entryName.startsWith("\\")) {
            throw SecurityException("Blocked absolute path in ZIP entry: $entryName")
        }
        val normalizedName = entryName.replace('\\', '/')
        if (normalizedName.split('/').any { it == ".." }) {
            throw SecurityException("Blocked path traversal sequence in ZIP entry: $entryName")
        }
        val canonicalDestDir = destinationDir.canonicalFile
        val canonicalDestPrefix = canonicalDestDir.path + File.separator
        val targetFile = File(canonicalDestDir, normalizedName).canonicalFile
        if (targetFile != canonicalDestDir && !targetFile.path.startsWith(canonicalDestPrefix)) {
            throw SecurityException("Blocked Zip Slip escape outside target folder: $entryName")
        }
        return targetFile
    }

    /**
     * Safely extracts [zipFile] into [destinationDir], checking every entry for path traversal
     * and zip bomb thresholds. Discovers any .apk and .aab files inside the archive.
     */
    fun extractZipSafely(
        zipFile: File,
        destinationDir: File,
        maxTotalBytes: Long = MAX_EXTRACTED_BYTES,
        maxEntries: Int = MAX_ENTRY_COUNT
    ): ZipExtractionResult {
        if (!zipFile.exists() || !zipFile.isFile) {
            return ZipExtractionResult(success = false, errorMessage = "ZIP archive not found.")
        }
        if (!hasValidZipMagicHeader(zipFile)) {
            return ZipExtractionResult(success = false, errorMessage = "Invalid ZIP file header.")
        }

        if (!destinationDir.exists() && !destinationDir.mkdirs()) {
            return ZipExtractionResult(
                success = false,
                errorMessage = "Could not create extraction directory."
            )
        }

        val extractedFiles = mutableListOf<File>()
        val apkFiles = mutableListOf<File>()
        val aabFiles = mutableListOf<File>()
        var totalBytes = 0L
        var entryCount = 0

        return try {
            ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
                val buffer = ByteArray(BUFFER_SIZE)
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    entryCount++
                    if (entryCount > maxEntries) {
                        throw SecurityException("ZIP archive exceeds maximum allowed entry count ($maxEntries).")
                    }
                    val targetFile = resolveSafeEntryFile(destinationDir, entry.name)
                    if (entry.isDirectory) {
                        targetFile.mkdirs()
                    } else {
                        targetFile.parentFile?.mkdirs()
                        FileOutputStream(targetFile).use { fos ->
                            var len: Int
                            while (zis.read(buffer).also { len = it } > 0) {
                                totalBytes += len
                                if (totalBytes > maxTotalBytes) {
                                    throw SecurityException("ZIP archive exceeds safe uncompressed size limit.")
                                }
                                fos.write(buffer, 0, len)
                            }
                        }
                        extractedFiles.add(targetFile)
                        when {
                            targetFile.name.endsWith(".apk", ignoreCase = true) -> apkFiles.add(targetFile)
                            targetFile.name.endsWith(".aab", ignoreCase = true) -> aabFiles.add(targetFile)
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            ZipExtractionResult(
                success = true,
                extractedFiles = extractedFiles,
                apkFiles = apkFiles,
                aabFiles = aabFiles,
                totalExtractedBytes = totalBytes
            )
        } catch (se: SecurityException) {
            ZipExtractionResult(
                success = false,
                errorMessage = se.message ?: "Blocked unsafe ZIP archive content."
            )
        } catch (e: Exception) {
            ZipExtractionResult(
                success = false,
                errorMessage = "Failed to extract ZIP: ${e.localizedMessage ?: "Corrupted archive"}"
            )
        }
    }

    /**
     * Validates an APK file before marking a download successful or offering installation.
     * Detects Android App Bundles (.aab) explicitly and never treats them as installable APKs.
     */
    fun validateApkFile(file: File, context: Context? = null): ApkValidationResult {
        if (!file.exists() || !file.isFile) {
            return ApkValidationResult(isValidApk = false, errorMessage = "APK file does not exist.")
        }
        val size = file.length()
        if (size < 64L) {
            return ApkValidationResult(
                isValidApk = false,
                fileSizeBytes = size,
                errorMessage = "File is too small to be a valid APK."
            )
        }
        if (file.name.endsWith(".aab", ignoreCase = true)) {
            return ApkValidationResult(
                isValidApk = false,
                isAabBundle = true,
                fileSizeBytes = size,
                errorMessage = "This file is an Android App Bundle (.aab) and cannot be installed directly as an APK."
            )
        }
        if (!hasValidZipMagicHeader(file)) {
            return ApkValidationResult(
                isValidApk = false,
                fileSizeBytes = size,
                errorMessage = "Invalid APK binary header (missing PK signature)."
            )
        }

        return try {
            var hasAndroidManifest = false
            var hasDexOrResources = false
            var hasBundleConfig = false

            ZipFile(file).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val name = entries.nextElement().name
                    when {
                        name == "AndroidManifest.xml" -> hasAndroidManifest = true
                        name == "BundleConfig.pb" -> hasBundleConfig = true
                        name == "classes.dex" ||
                            name == "resources.arsc" ||
                            name.matches(Regex("^classes\\d+\\.dex$")) -> hasDexOrResources = true
                    }
                }
            }

            if (hasBundleConfig && !hasAndroidManifest) {
                return ApkValidationResult(
                    isValidApk = false,
                    isAabBundle = true,
                    fileSizeBytes = size,
                    errorMessage = "Archive is an Android App Bundle (.aab), not an installable APK."
                )
            }

            if (!hasAndroidManifest || !hasDexOrResources) {
                return ApkValidationResult(
                    isValidApk = false,
                    fileSizeBytes = size,
                    errorMessage = "APK is missing required Android package entries (AndroidManifest.xml / classes.dex)."
                )
            }

            // Inspect real package metadata via Android PackageManager when available
            var packageName: String? = null
            var versionName: String? = null
            var versionCode: Long? = null

            if (context != null) {
                runCatching {
                    val pm = context.packageManager
                    val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        pm.getPackageArchiveInfo(
                            file.absolutePath,
                            PackageManager.PackageInfoFlags.of(0L)
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        pm.getPackageArchiveInfo(file.absolutePath, 0)
                    }
                    if (info != null) {
                        packageName = info.packageName
                        versionName = info.versionName
                        versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            info.longVersionCode
                        } else {
                            @Suppress("DEPRECATION")
                            info.versionCode.toLong()
                        }
                    }
                }
            }

            ApkValidationResult(
                isValidApk = true,
                isAabBundle = false,
                packageName = packageName,
                versionName = versionName,
                versionCode = versionCode,
                fileSizeBytes = size
            )
        } catch (e: Exception) {
            ApkValidationResult(
                isValidApk = false,
                fileSizeBytes = size,
                errorMessage = "Corrupted APK archive: ${e.localizedMessage ?: "Unreadable package"}"
            )
        }
    }
}
