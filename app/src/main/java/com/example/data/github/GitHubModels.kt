package com.example.data.github

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GitHubUserDto(
    val id: Long = 0L,
    val login: String = "",
    val name: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    @Json(name = "html_url") val htmlUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class GitHubRepoDto(
    val id: Long = 0L,
    val name: String = "",
    @Json(name = "full_name") val fullName: String = "",
    @Json(name = "private") val isPrivate: Boolean = false,
    @Json(name = "html_url") val htmlUrl: String = "",
    val description: String? = null,
    @Json(name = "default_branch") val defaultBranch: String = "main",
    @Json(name = "updated_at") val updatedAt: String? = null,
    val owner: GitHubUserDto? = null
)

@JsonClass(generateAdapter = true)
data class WorkflowRunsResponseDto(
    @Json(name = "total_count") val totalCount: Int = 0,
    @Json(name = "workflow_runs") val workflowRuns: List<WorkflowRunDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class HeadCommitDto(
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class WorkflowRunDto(
    val id: Long = 0L,
    val name: String? = null,
    @Json(name = "display_title") val displayTitle: String? = null,
    @Json(name = "head_branch") val headBranch: String? = null,
    @Json(name = "head_sha") val headSha: String? = null,
    val status: String? = null,
    val conclusion: String? = null,
    @Json(name = "html_url") val htmlUrl: String? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null,
    @Json(name = "run_started_at") val runStartedAt: String? = null,
    @Json(name = "head_commit") val headCommit: HeadCommitDto? = null
)

@JsonClass(generateAdapter = true)
data class ArtifactsResponseDto(
    @Json(name = "total_count") val totalCount: Int = 0,
    val artifacts: List<GitHubArtifactDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GitHubArtifactDto(
    val id: Long = 0L,
    val name: String = "",
    @Json(name = "size_in_bytes") val sizeInBytes: Long = 0L,
    @Json(name = "archive_download_url") val archiveDownloadUrl: String = "",
    val expired: Boolean = false,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "expires_at") val expiresAt: String? = null
)

@JsonClass(generateAdapter = true)
data class GitHubDeviceCodeResponseDto(
    @Json(name = "device_code") val deviceCode: String? = null,
    @Json(name = "user_code") val userCode: String? = null,
    @Json(name = "verification_uri") val verificationUri: String? = null,
    @Json(name = "expires_in") val expiresIn: Int = 900,
    val interval: Int = 5,
    val error: String? = null,
    @Json(name = "error_description") val errorDescription: String? = null
)

@JsonClass(generateAdapter = true)
data class GitHubOAuthTokenResponseDto(
    @Json(name = "access_token") val accessToken: String? = null,
    @Json(name = "token_type") val tokenType: String? = null,
    val scope: String? = null,
    val error: String? = null,
    @Json(name = "error_description") val errorDescription: String? = null
)
