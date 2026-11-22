package com.example.data.github

import android.net.Uri
import android.util.Base64
import com.example.security.GitHubSession
import com.example.security.SecureCredentialStore
import kotlinx.coroutines.delay
import java.security.MessageDigest
import java.security.SecureRandom

data class DeviceFlowChallenge(
    val deviceCode: String,
    val userCode: String,
    val verificationUri: String,
    val expiresInSeconds: Int,
    val intervalSeconds: Int
)

sealed interface GitHubAuthOutcome {
    data class Success(val session: GitHubSession) : GitHubAuthOutcome
    data class Error(val message: String) : GitHubAuthOutcome
}

/**
 * Implements official GitHub OAuth 2.0 flows:
 * 1. Device Authorization Grant (RFC 8628) — official GitHub mobile/native flow, requires no client secret.
 * 2. Authorization Code Flow with PKCE (S256) — opens official GitHub authorize URL in browser with state validation.
 *
 * Never requests or stores GitHub passwords or asks users for PATs in plain text fields.
 * Requests minimum scopes needed for repository metadata, contents, Actions workflow runs, and artifacts.
 */
class GitHubOAuthManager(
    private val authApi: GitHubAuthApiService,
    private val restApi: GitHubApiService,
    private val credentialStore: SecureCredentialStore
) {

    /**
     * Generates a cryptographically strong PKCE code verifier, S256 code challenge,
     * and anti-CSRF state, returning the official GitHub HTTPS authorize URL.
     */
    fun buildPkceAuthorizeUrl(clientId: String, includePrivateRepos: Boolean): String {
        val codeVerifier = generateSecureRandomUrlSafe(64)
        val state = generateSecureRandomUrlSafe(24)
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(codeVerifier.toByteArray(Charsets.US_ASCII))
        val codeChallenge = Base64.encodeToString(
            digest,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
        credentialStore.savePkceHandshake(codeVerifier, state)

        val scope = if (includePrivateRepos) MIN_PRIVATE_SCOPES else MIN_PUBLIC_SCOPES
        return Uri.parse("https://github.com/login/oauth/authorize").buildUpon()
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("scope", scope)
            .appendQueryParameter("state", state)
            .appendQueryParameter("code_challenge", codeChallenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .build()
            .toString()
    }

    /**
     * Completes an OAuth PKCE redirect callback after validating the CSRF state parameter.
     */
    suspend fun completePkceCallback(clientId: String, callbackUri: Uri): GitHubAuthOutcome {
        val error = callbackUri.getQueryParameter("error")
        if (!error.isNullOrBlank()) {
            val desc = callbackUri.getQueryParameter("error_description") ?: error
            return GitHubAuthOutcome.Error("GitHub authorization cancelled: $desc")
        }
        val code = callbackUri.getQueryParameter("code")
        val returnedState = callbackUri.getQueryParameter("state")
        val handshake = credentialStore.consumePkceHandshake()
            ?: return GitHubAuthOutcome.Error("Authorization session expired. Please try connecting again.")

        val (verifier, expectedState) = handshake
        if (code.isNullOrBlank() || returnedState != expectedState) {
            return GitHubAuthOutcome.Error("Security state verification failed. Please retry GitHub sign-in.")
        }

        return try {
            val tokenResp = authApi.exchangePkceCodeForToken(
                clientId = clientId,
                code = code,
                redirectUri = REDIRECT_URI,
                codeVerifier = verifier
            )
            val body = tokenResp.body()
            if (!tokenResp.isSuccessful || body?.accessToken.isNullOrBlank()) {
                val msg = body?.errorDescription ?: body?.error ?: "Token exchange failed (HTTP ${tokenResp.code()})."
                return GitHubAuthOutcome.Error(msg)
            }
            fetchAndPersistUserSession(body!!.accessToken!!, body.scope.orEmpty())
        } catch (e: Exception) {
            GitHubAuthOutcome.Error("Network error completing GitHub OAuth: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    /**
     * Initiates GitHub's official OAuth 2.0 Device Flow.
     */
    suspend fun startDeviceFlow(
        clientId: String,
        includePrivateRepos: Boolean
    ): Result<DeviceFlowChallenge> {
        return runCatching {
            val scope = if (includePrivateRepos) MIN_PRIVATE_SCOPES else MIN_PUBLIC_SCOPES
            val resp = authApi.requestDeviceCode(clientId = clientId, scope = scope)
            val body = resp.body()
            if (!resp.isSuccessful || body == null || body.deviceCode.isNullOrBlank() || body.userCode.isNullOrBlank()) {
                val reason = body?.errorDescription ?: body?.error ?: "HTTP ${resp.code()}"
                error("Could not start GitHub Device Flow: $reason. Make sure Device Flow is enabled in your GitHub OAuth App settings.")
            }
            DeviceFlowChallenge(
                deviceCode = body.deviceCode,
                userCode = body.userCode,
                verificationUri = body.verificationUri ?: "https://github.com/login/device",
                expiresInSeconds = body.expiresIn,
                intervalSeconds = body.interval.coerceAtLeast(5)
            )
        }
    }

    /**
     * Polls GitHub's token endpoint responsibly, honoring interval and slow_down backoff.
     */
    suspend fun pollDeviceFlowUntilAuthorized(
        clientId: String,
        challenge: DeviceFlowChallenge
    ): GitHubAuthOutcome {
        var pollIntervalMs = challenge.intervalSeconds * 1000L
        val deadline = System.currentTimeMillis() + (challenge.expiresInSeconds * 1000L)

        while (System.currentTimeMillis() < deadline) {
            delay(pollIntervalMs)
            try {
                val resp = authApi.pollDeviceAccessToken(
                    clientId = clientId,
                    deviceCode = challenge.deviceCode
                )
                val body = resp.body()
                if (body?.accessToken != null && body.accessToken.isNotBlank()) {
                    return fetchAndPersistUserSession(body.accessToken, body.scope.orEmpty())
                }
                when (body?.error) {
                    "authorization_pending" -> continue
                    "slow_down" -> {
                        pollIntervalMs += 5_000L
                        continue
                    }
                    "expired_token" -> return GitHubAuthOutcome.Error("Device code expired. Start authorization again.")
                    "access_denied" -> return GitHubAuthOutcome.Error("GitHub authorization was denied.")
                    else -> {
                        val err = body?.errorDescription ?: body?.error ?: "Authorization error."
                        return GitHubAuthOutcome.Error(err)
                    }
                }
            } catch (e: Exception) {
                return GitHubAuthOutcome.Error("Connection interrupted while checking GitHub authorization: ${e.localizedMessage}")
            }
        }
        return GitHubAuthOutcome.Error("GitHub device authorization timed out.")
    }

    private suspend fun fetchAndPersistUserSession(
        accessToken: String,
        scopes: String
    ): GitHubAuthOutcome {
        val userResp = restApi.getAuthenticatedUser("Bearer $accessToken")
        val user = userResp.body()
        if (!userResp.isSuccessful || user == null || user.login.isBlank()) {
            return GitHubAuthOutcome.Error("Authorized with GitHub, but failed to load account profile.")
        }
        val session = GitHubSession(
            accessToken = accessToken,
            username = user.login,
            avatarUrl = user.avatarUrl,
            displayName = user.name,
            scopes = scopes,
            lastSyncedAt = System.currentTimeMillis()
        )
        credentialStore.saveGitHubSession(session)
        return GitHubAuthOutcome.Success(session)
    }

    private fun generateSecureRandomUrlSafe(byteLength: Int): String {
        val bytes = ByteArray(byteLength)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    companion object {
        const val REDIRECT_URI = "aistudioappmanager://oauth/github"
        // Minimum scopes for reading repo metadata, contents, Actions workflow runs, and artifacts
        const val MIN_PUBLIC_SCOPES = "public_repo read:user"
        const val MIN_PRIVATE_SCOPES = "repo read:user"
    }
}
