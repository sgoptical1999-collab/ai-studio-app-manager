package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

enum class ProjectType(val label: String) {
    NATIVE_ANDROID("Native Android App"),
    WEB_APP("Web App"),
    REACT_APP("React App"),
    OTHER("Other")
}

enum class BuildStatus(val label: String) {
    NONE("No Builds"),
    QUEUED("Queued"),
    IN_PROGRESS("In Progress"),
    SUCCESS("Success"),
    FAILED("Failed"),
    CANCELLED("Cancelled")
}

enum class ExtractionStatus(val label: String) {
    NOT_EXTRACTED("Archived ZIP"),
    EXTRACTING("Extracting..."),
    EXTRACTED("Extracted"),
    FAILED("Extraction Failed")
}

enum class DownloadStatus(val label: String) {
    DOWNLOADING("Downloading"),
    COMPLETED("Completed"),
    FAILED("Failed"),
    EXPIRED("Expired")
}

enum class DownloadFileType(val label: String) {
    APK("APK"),
    ARTIFACT_ZIP("Artifact ZIP"),
    SOURCE_ZIP("Source ZIP"),
    AAB("Android App Bundle (.aab)")
}

enum class ActivityType {
    APP_ADDED,
    ZIP_IMPORTED,
    BUILD_UPDATED,
    APK_DOWNLOADED,
    DOWNLOAD_FAILED,
    APK_INSTALLED,
    GITHUB_SYNCED
}

@JsonClass(generateAdapter = true)
@Entity(tableName = "app_projects")
data class AppProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val folderName: String,
    val iconUri: String? = null,
    val iconColorHex: String = "#3B82F6",
    val iconSymbolKey: String = "spark",
    val description: String = "",
    val aiStudioUrl: String = "",
    val githubRepoUrl: String = "",
    val githubOwner: String = "",
    val githubRepoName: String = "",
    val isPrivateRepo: Boolean = false,
    val projectType: ProjectType = ProjectType.NATIVE_ANDROID,
    val versionName: String = "1.0.0",
    val packageName: String = "",
    val notes: String = "",
    val latestBuildStatus: BuildStatus = BuildStatus.NONE,
    val latestBuildWorkflowName: String = "",
    val latestBuildDate: Long = 0L,
    val latestApkVersion: String = "",
    val hasApkAvailable: Boolean = false,
    val hasApkDownloaded: Boolean = false,
    val hasSourceZip: Boolean = false,
    val isDemoData: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
@Entity(
    tableName = "source_zips",
    foreignKeys = [
        ForeignKey(
            entity = AppProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["appId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["appId"])]
)
data class SourceZipEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val appId: Long,
    val versionLabel: String,
    val fileName: String,
    val localFilePath: String,
    val fileSizeBytes: Long,
    val extractedDirPath: String? = null,
    val extractionStatus: ExtractionStatus = ExtractionStatus.NOT_EXTRACTED,
    val uploadedAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
@Entity(
    tableName = "build_runs",
    foreignKeys = [
        ForeignKey(
            entity = AppProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["appId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["appId"]), Index(value = ["appId", "githubRunId"], unique = true)]
)
data class BuildRunEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val appId: Long,
    val githubRunId: Long,
    val workflowName: String,
    val branch: String,
    val commitSha: String,
    val commitMessage: String = "",
    val status: BuildStatus,
    val conclusion: String = "",
    val durationSeconds: Long = 0L,
    val htmlUrl: String = "",
    val artifactCount: Int = 0,
    val hasApkArtifact: Boolean = false,
    val hasAabOnly: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
@Entity(
    tableName = "build_artifacts",
    foreignKeys = [
        ForeignKey(
            entity = AppProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["appId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["appId"]), Index(value = ["buildRunId"]), Index(value = ["appId", "githubArtifactId"], unique = true)]
)
data class ArtifactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val appId: Long,
    val buildRunId: Long,
    val githubArtifactId: Long,
    val name: String,
    val sizeInBytes: Long,
    val archiveDownloadUrl: String,
    val expired: Boolean = false,
    val createdAt: Long = 0L,
    val expiresAt: Long = 0L,
    val localZipPath: String? = null,
    val extractedDirPath: String? = null,
    val extractedApkPath: String? = null,
    val isAabOnly: Boolean = false,
    val validationMessage: String? = null
)

@JsonClass(generateAdapter = true)
@Entity(
    tableName = "apk_versions",
    foreignKeys = [
        ForeignKey(
            entity = AppProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["appId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["appId"])]
)
data class ApkVersionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val appId: Long,
    val fileName: String,
    val localFilePath: String,
    val fileSizeBytes: Long,
    val packageName: String = "",
    val versionName: String = "",
    val versionCode: Long = 0L,
    val buildNumber: String = "",
    val gitCommitSha: String = "",
    val sourceZipVersion: String = "",
    val buildDate: Long = 0L,
    val downloadedAt: Long = System.currentTimeMillis(),
    val isLatest: Boolean = true,
    val isValidApk: Boolean = true
)

@JsonClass(generateAdapter = true)
@Entity(
    tableName = "download_records",
    indices = [Index(value = ["appId"])]
)
data class DownloadRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val appId: Long,
    val appName: String,
    val fileName: String,
    val fileType: DownloadFileType,
    val versionName: String = "",
    val fileSizeBytes: Long = 0L,
    val localFilePath: String = "",
    val status: DownloadStatus,
    val errorMessage: String? = null,
    val sourceUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "activity_logs")
data class ActivityLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val appId: Long? = null,
    val appName: String = "",
    val type: ActivityType,
    val title: String,
    val subtitle: String,
    val timestamp: Long = System.currentTimeMillis()
)
