package com.example.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class GitHubSession(
    val accessToken: String,
    val username: String,
    val avatarUrl: String? = null,
    val displayName: String? = null,
    val scopes: String = "",
    val lastSyncedAt: Long = 0L
)

data class GoogleAccountSession(
    val email: String,
    val displayName: String,
    val photoUrl: String? = null,
    val signedInAt: Long = System.currentTimeMillis()
)

interface SecretCipher {
    fun encrypt(plainText: String): String
    fun decrypt(cipherText: String): String?
}

/**
 * Hardware/OS-backed Android Keystore AES-256-GCM cipher.
 * Keys never leave the Android Keystore and are never exposed in plaintext.
 */
class AndroidKeystoreCipher(
    private val keyAlias: String = "ai_studio_app_manager_aes_key"
) : SecretCipher {

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(keyAlias, null) as? SecretKey
        if (existing != null) return existing

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        val spec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    override fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(1 + iv.size + encrypted.size)
        combined[0] = iv.size.toByte()
        System.arraycopy(iv, 0, combined, 1, iv.size)
        System.arraycopy(encrypted, 0, combined, 1 + iv.size, encrypted.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    override fun decrypt(cipherText: String): String? {
        return try {
            val combined = Base64.decode(cipherText, Base64.NO_WRAP)
            if (combined.size < 14) return null
            val ivLength = combined[0].toInt() and 0xFF
            if (ivLength !in 12..16 || combined.size <= 1 + ivLength) return null
            val iv = combined.copyOfRange(1, 1 + ivLength)
            val encrypted = combined.copyOfRange(1 + ivLength, combined.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}

/**
 * Fallback AES-GCM cipher used only in non-Android-Keystore JVM unit tests (e.g., Robolectric).
 */
class FallbackJvmCipher : SecretCipher {
    private val key: SecretKey by lazy {
        KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    }

    override fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(1 + iv.size + encrypted.size)
        combined[0] = iv.size.toByte()
        System.arraycopy(iv, 0, combined, 1, iv.size)
        System.arraycopy(encrypted, 0, combined, 1 + iv.size, encrypted.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    override fun decrypt(cipherText: String): String? {
        return try {
            val combined = Base64.decode(cipherText, Base64.NO_WRAP)
            val ivLength = combined[0].toInt() and 0xFF
            val iv = combined.copyOfRange(1, 1 + ivLength)
            val encrypted = combined.copyOfRange(1 + ivLength, combined.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }
}

/**
 * Encrypted credential vault backed by Android Keystore.
 * Never logs tokens, never stores Google or GitHub passwords.
 */
class SecureCredentialStore(
    context: Context,
    private val cipher: SecretCipher = createBestAvailableCipher()
) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun putEncrypted(key: String, value: String?) {
        if (value == null) {
            prefs.edit().remove(key).apply()
        } else {
            val enc = cipher.encrypt(value)
            prefs.edit().putString(key, enc).apply()
        }
    }

    private fun getDecrypted(key: String): String? {
        val raw = prefs.getString(key, null) ?: return null
        return cipher.decrypt(raw)
    }

    fun saveGitHubSession(session: GitHubSession) {
        putEncrypted(KEY_GH_TOKEN, session.accessToken)
        putEncrypted(KEY_GH_USERNAME, session.username)
        putEncrypted(KEY_GH_AVATAR, session.avatarUrl)
        putEncrypted(KEY_GH_DISPLAY_NAME, session.displayName)
        putEncrypted(KEY_GH_SCOPES, session.scopes)
        prefs.edit().putLong(KEY_GH_LAST_SYNC, session.lastSyncedAt).apply()
    }

    fun updateGitHubLastSync(timestamp: Long) {
        prefs.edit().putLong(KEY_GH_LAST_SYNC, timestamp).apply()
    }

    fun getGitHubSession(): GitHubSession? {
        val token = getDecrypted(KEY_GH_TOKEN) ?: return null
        val username = getDecrypted(KEY_GH_USERNAME) ?: return null
        if (token.isBlank() || username.isBlank()) return null
        return GitHubSession(
            accessToken = token,
            username = username,
            avatarUrl = getDecrypted(KEY_GH_AVATAR),
            displayName = getDecrypted(KEY_GH_DISPLAY_NAME),
            scopes = getDecrypted(KEY_GH_SCOPES).orEmpty(),
            lastSyncedAt = prefs.getLong(KEY_GH_LAST_SYNC, 0L)
        )
    }

    fun getGitHubAccessToken(): String? = getDecrypted(KEY_GH_TOKEN)?.takeIf { it.isNotBlank() }

    fun clearGitHubSession() {
        prefs.edit()
            .remove(KEY_GH_TOKEN)
            .remove(KEY_GH_USERNAME)
            .remove(KEY_GH_AVATAR)
            .remove(KEY_GH_DISPLAY_NAME)
            .remove(KEY_GH_SCOPES)
            .remove(KEY_GH_LAST_SYNC)
            .remove(KEY_PKCE_VERIFIER)
            .remove(KEY_PKCE_STATE)
            .apply()
    }

    fun savePkceHandshake(codeVerifier: String, state: String) {
        putEncrypted(KEY_PKCE_VERIFIER, codeVerifier)
        putEncrypted(KEY_PKCE_STATE, state)
    }

    fun consumePkceHandshake(): Pair<String, String>? {
        val verifier = getDecrypted(KEY_PKCE_VERIFIER)
        val state = getDecrypted(KEY_PKCE_STATE)
        prefs.edit().remove(KEY_PKCE_VERIFIER).remove(KEY_PKCE_STATE).apply()
        if (verifier.isNullOrBlank() || state.isNullOrBlank()) return null
        return verifier to state
    }

    fun saveGoogleAccount(session: GoogleAccountSession) {
        putEncrypted(KEY_GOOGLE_EMAIL, session.email)
        putEncrypted(KEY_GOOGLE_NAME, session.displayName)
        putEncrypted(KEY_GOOGLE_PHOTO, session.photoUrl)
        prefs.edit().putLong(KEY_GOOGLE_SIGNED_IN_AT, session.signedInAt).apply()
    }

    fun getGoogleAccount(): GoogleAccountSession? {
        val email = getDecrypted(KEY_GOOGLE_EMAIL) ?: return null
        if (email.isBlank()) return null
        return GoogleAccountSession(
            email = email,
            displayName = getDecrypted(KEY_GOOGLE_NAME).orEmpty().ifBlank { email },
            photoUrl = getDecrypted(KEY_GOOGLE_PHOTO),
            signedInAt = prefs.getLong(KEY_GOOGLE_SIGNED_IN_AT, 0L)
        )
    }

    fun clearGoogleAccount() {
        prefs.edit()
            .remove(KEY_GOOGLE_EMAIL)
            .remove(KEY_GOOGLE_NAME)
            .remove(KEY_GOOGLE_PHOTO)
            .remove(KEY_GOOGLE_SIGNED_IN_AT)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "secure_ai_studio_vault_prefs"
        private const val KEY_GH_TOKEN = "gh_token_enc"
        private const val KEY_GH_USERNAME = "gh_username_enc"
        private const val KEY_GH_AVATAR = "gh_avatar_enc"
        private const val KEY_GH_DISPLAY_NAME = "gh_name_enc"
        private const val KEY_GH_SCOPES = "gh_scopes_enc"
        private const val KEY_GH_LAST_SYNC = "gh_last_sync"
        private const val KEY_PKCE_VERIFIER = "gh_pkce_verifier_enc"
        private const val KEY_PKCE_STATE = "gh_pkce_state_enc"
        private const val KEY_GOOGLE_EMAIL = "google_email_enc"
        private const val KEY_GOOGLE_NAME = "google_name_enc"
        private const val KEY_GOOGLE_PHOTO = "google_photo_enc"
        private const val KEY_GOOGLE_SIGNED_IN_AT = "google_signed_in_at"

        private fun createBestAvailableCipher(): SecretCipher {
            return try {
                val ks = KeyStore.getInstance("AndroidKeyStore")
                ks.load(null)
                AndroidKeystoreCipher()
            } catch (_: Exception) {
                FallbackJvmCipher()
            }
        }
    }
}
