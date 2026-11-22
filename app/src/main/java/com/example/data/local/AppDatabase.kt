package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class AppTypeConverters {
    @TypeConverter
    fun fromProjectType(value: ProjectType): String = value.name

    @TypeConverter
    fun toProjectType(value: String): ProjectType =
        runCatching { ProjectType.valueOf(value) }.getOrDefault(ProjectType.OTHER)

    @TypeConverter
    fun fromBuildStatus(value: BuildStatus): String = value.name

    @TypeConverter
    fun toBuildStatus(value: String): BuildStatus =
        runCatching { BuildStatus.valueOf(value) }.getOrDefault(BuildStatus.NONE)

    @TypeConverter
    fun fromExtractionStatus(value: ExtractionStatus): String = value.name

    @TypeConverter
    fun toExtractionStatus(value: String): ExtractionStatus =
        runCatching { ExtractionStatus.valueOf(value) }.getOrDefault(ExtractionStatus.NOT_EXTRACTED)

    @TypeConverter
    fun fromDownloadStatus(value: DownloadStatus): String = value.name

    @TypeConverter
    fun toDownloadStatus(value: String): DownloadStatus =
        runCatching { DownloadStatus.valueOf(value) }.getOrDefault(DownloadStatus.FAILED)

    @TypeConverter
    fun fromDownloadFileType(value: DownloadFileType): String = value.name

    @TypeConverter
    fun toDownloadFileType(value: String): DownloadFileType =
        runCatching { DownloadFileType.valueOf(value) }.getOrDefault(DownloadFileType.APK)

    @TypeConverter
    fun fromActivityType(value: ActivityType): String = value.name

    @TypeConverter
    fun toActivityType(value: String): ActivityType =
        runCatching { ActivityType.valueOf(value) }.getOrDefault(ActivityType.APP_ADDED)
}

@Database(
    entities = [
        AppProjectEntity::class,
        SourceZipEntity::class,
        BuildRunEntity::class,
        ArtifactEntity::class,
        ApkVersionEntity::class,
        DownloadRecordEntity::class,
        ActivityLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(AppTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appProjectDao(): AppProjectDao
    abstract fun sourceZipDao(): SourceZipDao
    abstract fun buildRunDao(): BuildRunDao
    abstract fun artifactDao(): ArtifactDao
    abstract fun apkVersionDao(): ApkVersionDao
    abstract fun downloadRecordDao(): DownloadRecordDao
    abstract fun activityLogDao(): ActivityLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ai_studio_app_manager.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
