package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppProjectDao {
    @Query("SELECT * FROM app_projects ORDER BY updatedAt DESC")
    fun observeAllApps(): Flow<List<AppProjectEntity>>

    @Query("SELECT * FROM app_projects ORDER BY updatedAt DESC")
    suspend fun getAllAppsSnapshot(): List<AppProjectEntity>

    @Query("SELECT * FROM app_projects WHERE id = :appId")
    fun observeAppById(appId: Long): Flow<AppProjectEntity?>

    @Query("SELECT * FROM app_projects WHERE id = :appId")
    suspend fun getAppById(appId: Long): AppProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: AppProjectEntity): Long

    @Update
    suspend fun updateApp(app: AppProjectEntity)

    @Delete
    suspend fun deleteApp(app: AppProjectEntity)

    @Query("DELETE FROM app_projects WHERE id = :appId")
    suspend fun deleteAppById(appId: Long)

    @Query("DELETE FROM app_projects WHERE isDemoData = 1")
    suspend fun deleteAllDemoApps()
}

@Dao
interface SourceZipDao {
    @Query("SELECT * FROM source_zips ORDER BY uploadedAt DESC")
    fun observeAllZips(): Flow<List<SourceZipEntity>>

    @Query("SELECT * FROM source_zips WHERE appId = :appId ORDER BY uploadedAt DESC")
    fun observeZipsForApp(appId: Long): Flow<List<SourceZipEntity>>

    @Query("SELECT * FROM source_zips WHERE appId = :appId ORDER BY uploadedAt DESC")
    suspend fun getZipsForApp(appId: Long): List<SourceZipEntity>

    @Query("SELECT COUNT(*) FROM source_zips")
    fun observeTotalZipCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertZip(zip: SourceZipEntity): Long

    @Update
    suspend fun updateZip(zip: SourceZipEntity)

    @Query("DELETE FROM source_zips WHERE id = :zipId")
    suspend fun deleteZipById(zipId: Long)
}

@Dao
interface BuildRunDao {
    @Query("SELECT * FROM build_runs ORDER BY createdAt DESC")
    fun observeAllBuilds(): Flow<List<BuildRunEntity>>

    @Query("SELECT * FROM build_runs WHERE appId = :appId ORDER BY createdAt DESC")
    fun observeBuildsForApp(appId: Long): Flow<List<BuildRunEntity>>

    @Query("SELECT * FROM build_runs WHERE appId = :appId ORDER BY createdAt DESC")
    suspend fun getBuildsForApp(appId: Long): List<BuildRunEntity>

    @Query("SELECT * FROM build_runs WHERE id = :buildId")
    suspend fun getBuildById(buildId: Long): BuildRunEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBuilds(builds: List<BuildRunEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBuild(build: BuildRunEntity): Long

    @Query("DELETE FROM build_runs WHERE appId = :appId")
    suspend fun clearBuildsForApp(appId: Long)
}

@Dao
interface ArtifactDao {
    @Query("SELECT * FROM build_artifacts WHERE appId = :appId ORDER BY createdAt DESC")
    fun observeArtifactsForApp(appId: Long): Flow<List<ArtifactEntity>>

    @Query("SELECT * FROM build_artifacts WHERE buildRunId = :buildRunId ORDER BY createdAt DESC")
    fun observeArtifactsForBuild(buildRunId: Long): Flow<List<ArtifactEntity>>

    @Query("SELECT * FROM build_artifacts WHERE buildRunId = :buildRunId ORDER BY createdAt DESC")
    suspend fun getArtifactsForBuild(buildRunId: Long): List<ArtifactEntity>

    @Query("SELECT * FROM build_artifacts WHERE id = :artifactId")
    suspend fun getArtifactById(artifactId: Long): ArtifactEntity?

    @Query("SELECT * FROM build_artifacts ORDER BY createdAt DESC")
    fun observeAllArtifacts(): Flow<List<ArtifactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertArtifacts(artifacts: List<ArtifactEntity>)

    @Update
    suspend fun updateArtifact(artifact: ArtifactEntity)
}

@Dao
interface ApkVersionDao {
    @Query("SELECT * FROM apk_versions ORDER BY downloadedAt DESC")
    fun observeAllApks(): Flow<List<ApkVersionEntity>>

    @Query("SELECT * FROM apk_versions WHERE appId = :appId ORDER BY downloadedAt DESC")
    fun observeApksForApp(appId: Long): Flow<List<ApkVersionEntity>>

    @Query("SELECT * FROM apk_versions WHERE appId = :appId ORDER BY downloadedAt DESC")
    suspend fun getApksForApp(appId: Long): List<ApkVersionEntity>

    @Query("UPDATE apk_versions SET isLatest = 0 WHERE appId = :appId")
    suspend fun clearLatestFlag(appId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApk(apk: ApkVersionEntity): Long

    @Query("DELETE FROM apk_versions WHERE id = :apkId")
    suspend fun deleteApkById(apkId: Long)
}

@Dao
interface DownloadRecordDao {
    @Query("SELECT * FROM download_records ORDER BY createdAt DESC")
    fun observeAllDownloads(): Flow<List<DownloadRecordEntity>>

    @Query("SELECT * FROM download_records WHERE appId = :appId ORDER BY createdAt DESC")
    fun observeDownloadsForApp(appId: Long): Flow<List<DownloadRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: DownloadRecordEntity): Long

    @Update
    suspend fun updateRecord(record: DownloadRecordEntity)

    @Query("DELETE FROM download_records WHERE id = :recordId")
    suspend fun deleteRecordById(recordId: Long)

    @Query("DELETE FROM download_records")
    suspend fun clearAllHistory()
}

@Dao
interface ActivityLogDao {
    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC LIMIT 25")
    fun observeRecentActivity(): Flow<List<ActivityLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ActivityLogEntity): Long

    @Query("DELETE FROM activity_logs")
    suspend fun clearLogs()
}
