package com.example.data.github

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.IOException
import java.util.concurrent.TimeUnit

interface GitHubApiService {
    @Headers("Accept: application/vnd.github+json", "X-GitHub-Api-Version: 2022-11-28")
    @GET("user")
    suspend fun getAuthenticatedUser(
        @Header("Authorization") authorization: String
    ): retrofit2.Response<GitHubUserDto>

    @Headers("Accept: application/vnd.github+json", "X-GitHub-Api-Version: 2022-11-28")
    @GET("user/repos")
    suspend fun getUserRepositories(
        @Header("Authorization") authorization: String,
        @Query("sort") sort: String = "updated",
        @Query("per_page") perPage: Int = 100,
        @Query("affiliation") affiliation: String = "owner,collaborator,organization_member"
    ): retrofit2.Response<List<GitHubRepoDto>>

    @Headers("Accept: application/vnd.github+json", "X-GitHub-Api-Version: 2022-11-28")
    @GET("repos/{owner}/{repo}")
    suspend fun getRepository(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Header("Authorization") authorization: String? = null
    ): retrofit2.Response<GitHubRepoDto>

    @Headers("Accept: application/vnd.github+json", "X-GitHub-Api-Version: 2022-11-28")
    @GET("repos/{owner}/{repo}/actions/runs")
    suspend fun getWorkflowRuns(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Header("Authorization") authorization: String? = null,
        @Query("per_page") perPage: Int = 20
    ): retrofit2.Response<WorkflowRunsResponseDto>

    @Headers("Accept: application/vnd.github+json", "X-GitHub-Api-Version: 2022-11-28")
    @GET("repos/{owner}/{repo}/actions/runs/{run_id}/artifacts")
    suspend fun getRunArtifacts(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("run_id") runId: Long,
        @Header("Authorization") authorization: String? = null
    ): retrofit2.Response<ArtifactsResponseDto>
}

interface GitHubAuthApiService {
    @Headers("Accept: application/json")
    @FormUrlEncoded
    @POST("login/device/code")
    suspend fun requestDeviceCode(
        @Field("client_id") clientId: String,
        @Field("scope") scope: String
    ): retrofit2.Response<GitHubDeviceCodeResponseDto>

    @Headers("Accept: application/json")
    @FormUrlEncoded
    @POST("login/oauth/access_token")
    suspend fun pollDeviceAccessToken(
        @Field("client_id") clientId: String,
        @Field("device_code") deviceCode: String,
        @Field("grant_type") grantType: String = "urn:ietf:params:oauth:grant-type:device_code"
    ): retrofit2.Response<GitHubOAuthTokenResponseDto>

    @Headers("Accept: application/json")
    @FormUrlEncoded
    @POST("login/oauth/access_token")
    suspend fun exchangePkceCodeForToken(
        @Field("client_id") clientId: String,
        @Field("code") code: String,
        @Field("redirect_uri") redirectUri: String,
        @Field("code_verifier") codeVerifier: String
    ): retrofit2.Response<GitHubOAuthTokenResponseDto>
}

object GitHubNetworkFactory {

    /**
     * Enforces HTTPS-only requests across all API & artifact download traffic.
     * Never logs Authorization headers or access tokens.
     */
    private val httpsOnlyInterceptor = Interceptor { chain ->
        val request = chain.request()
        if (!request.url.isHttps) {
            throw IOException("Blocked insecure non-HTTPS request to ${request.url.host}")
        }
        val cleanRequest = request.newBuilder()
            .header("User-Agent", "AIStudioAppManager-Android/1.0")
            .build()
        chain.proceed(cleanRequest)
    }

    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val apiOkHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(httpsOnlyInterceptor)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Dedicated OkHttp client for GitHub Actions artifact ZIP downloads.
     * Disables automatic redirects so that when GitHub responds with a 302 to Azure Blob Storage,
     * the Authorization Bearer token is strictly stripped before following the signed HTTPS redirect.
     */
    val artifactDownloadClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(httpsOnlyInterceptor)
        .followRedirects(false)
        .followSslRedirects(false)
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    fun createGitHubApiService(): GitHubApiService {
        return Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(apiOkHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GitHubApiService::class.java)
    }

    fun createGitHubAuthService(): GitHubAuthApiService {
        return Retrofit.Builder()
            .baseUrl("https://github.com/")
            .client(apiOkHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GitHubAuthApiService::class.java)
    }

    /**
     * Executes an authenticated GitHub artifact download, safely stripping Authorization headers
     * when following cross-origin 302 redirects to temporary signed artifact storage.
     */
    @Throws(IOException::class)
    fun executeAuthorizedArtifactRequest(
        archiveDownloadUrl: String,
        bearerToken: String
    ): Response {
        val initialRequest = Request.Builder()
            .url(archiveDownloadUrl)
            .header("Authorization", "Bearer $bearerToken")
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .get()
            .build()

        val initialResponse = artifactDownloadClient.newCall(initialRequest).execute()
        if (initialResponse.code in listOf(301, 302, 303, 307, 308)) {
            val location = initialResponse.header("Location")
            initialResponse.close()
            if (location.isNullOrBlank() || !location.startsWith("https://", ignoreCase = true)) {
                throw IOException("GitHub returned an invalid or non-HTTPS artifact redirect.")
            }
            // Strip Authorization header on redirect to signed blob storage
            val redirectRequest = Request.Builder()
                .url(location)
                .get()
                .build()
            return artifactDownloadClient.newCall(redirectRequest).execute()
        }
        return initialResponse
    }
}
