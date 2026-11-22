package com.example.data.installer

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.FileProvider
import com.example.security.SafeZipAndApkValidator
import com.example.security.UrlValidator
import java.io.File

sealed interface ApkInstallCheckResult {
    data object ReadyToInstall : ApkInstallCheckResult
    data object NeedsInstallPermission : ApkInstallCheckResult
    data class InvalidApk(val reason: String) : ApkInstallCheckResult
}

/**
 * Handles official Android Package Installer launching via FileProvider content URIs,
 * unknown-app install permission checks, secure APK sharing, and official browser navigation.
 * Never attempts silent installation or security bypasses.
 */
class ApkInstallerManager(private val context: Context) {

    fun canRequestPackageInstalls(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun checkApkReadyForInstall(apkFile: File): ApkInstallCheckResult {
        val validation = SafeZipAndApkValidator.validateApkFile(apkFile, context)
        if (!validation.isValidApk) {
            return ApkInstallCheckResult.InvalidApk(
                validation.errorMessage ?: "APK file could not be verified."
            )
        }
        return if (canRequestPackageInstalls()) {
            ApkInstallCheckResult.ReadyToInstall
        } else {
            ApkInstallCheckResult.NeedsInstallPermission
        }
    }

    fun openInstallUnknownAppsSettings(): Result<Unit> = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    /**
     * Launches Android's official Package Installer with a FileProvider content URI.
     * Caller MUST show a user confirmation dialog displaying the APK filename first.
     */
    fun launchOfficialPackageInstaller(apkFile: File): Result<Unit> = runCatching {
        val validation = SafeZipAndApkValidator.validateApkFile(apkFile, context)
        if (!validation.isValidApk) {
            error(validation.errorMessage ?: "Cannot install invalid APK.")
        }
        val contentUri = getSecureContentUri(apkFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, MIME_APK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun shareFile(file: File, mimeType: String, chooserTitle: String): Result<Unit> = runCatching {
        if (!file.exists()) error("File not found on device.")
        val contentUri = getSecureContentUri(file)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(sendIntent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun openFileWithSystemChooser(file: File, mimeType: String): Result<Unit> = runCatching {
        if (!file.exists()) error("File not found on device.")
        val contentUri = getSecureContentUri(file)
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(viewIntent)
    }

    /**
     * Opens an official HTTPS URL in Chrome Custom Tabs (or system browser fallback).
     */
    fun openHttpsUrlInBrowser(url: String): Result<Unit> = runCatching {
        if (!UrlValidator.isValidHttpsUrl(url)) {
            error("Blocked non-HTTPS or invalid URL.")
        }
        val uri = Uri.parse(url.trim())
        try {
            val customTabs = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
            customTabs.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            customTabs.launchUrl(context, uri)
        } catch (_: ActivityNotFoundException) {
            val fallback = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(fallback)
        }
    }

    private fun getSecureContentUri(file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    companion object {
        const val MIME_APK = "application/vnd.android.package-archive"
        const val MIME_ZIP = "application/zip"
    }
}
