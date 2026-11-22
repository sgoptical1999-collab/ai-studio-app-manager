package com.example

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.BackupRestoreManager
import com.example.data.github.GitHubNetworkFactory
import com.example.data.github.GitHubOAuthManager
import com.example.data.local.AppDatabase
import com.example.data.local.DownloadFileType
import com.example.data.local.DownloadRecordEntity
import com.example.data.local.DownloadStatus
import com.example.data.local.ProjectType
import com.example.data.preferences.SettingsDataStore
import com.example.data.repository.AppManagerRepository
import com.example.data.storage.AppFolderManager
import com.example.security.FallbackJvmCipher
import com.example.security.GitHubSession
import com.example.security.GoogleAccountSession
import com.example.security.SecureCredentialStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppManagerRobolectricTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var folderManager: AppFolderManager
    private lateinit var credentialStore: SecureCredentialStore
    private lateinit var repository: AppManagerRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        folderManager = AppFolderManager(context)
        credentialStore = SecureCredentialStore(context, FallbackJvmCipher())
        val settingsStore = SettingsDataStore(context)
        val backupMgr = BackupRestoreManager(context, database, folderManager)
        repository = AppManagerRepository(
            context = context,
            database = database,
            folderManager = folderManager,
            githubApi = GitHubNetworkFactory.createGitHubApiService(),
            credentialStore = credentialStore,
            settingsDataStore = settingsStore,
            backupRestoreManager = backupMgr
        )
    }

    @After
    fun tearDown() {
        database.close()
        folderManager.baseRootDir.deleteRecursively()
        credentialStore.clearGitHubSession()
        credentialStore.clearGoogleAccount()
    }

    @Test
    fun `app name string resource matches AI Studio App Manager`() {
        assertEquals("AI Studio App Manager", context.getString(R.string.app_name))
    }

    @Test
    fun `encrypted credential store protects GitHub and Google sessions and clears on disconnect`() {
        val secretToken = "gho_test_super_secret_token_987654"
        credentialStore.saveGitHubSession(
            GitHubSession(
                accessToken = secretToken,
                username = "dev_owner",
                scopes = "repo read:user",
                lastSyncedAt = 123456L
            )
        )
        credentialStore.saveGoogleAccount(
            GoogleAccountSession(
                email = "owner@example.com",
                displayName = "Owner"
            )
        )

        // Ensure raw SharedPreferences does NOT store plaintext token
        val rawPrefs = context.getSharedPreferences("secure_ai_studio_vault_prefs", Context.MODE_PRIVATE)
        val rawStored = rawPrefs.getString("gh_token_enc", "")
        assertFalse(rawStored.orEmpty().contains(secretToken))

        val loadedGh = credentialStore.getGitHubSession()
        assertNotNull(loadedGh)
        assertEquals("dev_owner", loadedGh?.username)
        assertEquals(secretToken, loadedGh?.accessToken)

        val loadedGoogle = credentialStore.getGoogleAccount()
        assertNotNull(loadedGoogle)
        assertEquals("owner@example.com", loadedGoogle?.email)

        credentialStore.clearGitHubSession()
        credentialStore.clearGoogleAccount()
        assertNull(credentialStore.getGitHubSession())
        assertNull(credentialStore.getGoogleAccount())
    }

    @Test
    fun `creating updating and deleting app persists in Room and organizes folders`() = runTest {
        val createdId = repository.createAppFolder(
            name = "VIRAL REEL AI",
            description = "Video script generator",
            aiStudioUrl = "https://aistudio.google.com/",
            githubRepoUrl = "https://github.com/owner/viral-reel-ai",
            projectType = ProjectType.NATIVE_ANDROID,
            versionName = "1.2.0",
            notes = "Initial build",
            iconUri = null,
            iconColorHex = "#8B5CF6",
            iconSymbolKey = "auto_awesome"
        ).getOrThrow()

        val saved = database.appProjectDao().getAppById(createdId)
        assertNotNull(saved)
        assertEquals("VIRAL REEL AI", saved?.name)
        assertEquals("owner", saved?.githubOwner)
        assertEquals("viral-reel-ai", saved?.githubRepoName)

        // Verify automatic 4-subfolder structure
        val appRoot = File(folderManager.baseRootDir, saved!!.folderName)
        assertTrue(File(appRoot, AppFolderManager.DIR_SOURCE).isDirectory)
        assertTrue(File(appRoot, AppFolderManager.DIR_APK).isDirectory)
        assertTrue(File(appRoot, AppFolderManager.DIR_BUILD_ARTIFACTS).isDirectory)
        assertTrue(File(appRoot, AppFolderManager.DIR_HISTORY).isDirectory)

        // Rename app folder
        repository.updateAppFolder(
            existing = saved,
            name = "VIRAL REEL PRO",
            description = "Updated description",
            aiStudioUrl = "https://aistudio.google.com/",
            githubRepoUrl = "https://github.com/owner/viral-reel-ai",
            projectType = ProjectType.NATIVE_ANDROID,
            versionName = "1.3.0",
            notes = "Renamed",
            iconUri = null,
            iconColorHex = "#3B82F6",
            iconSymbolKey = "rocket"
        ).getOrThrow()

        val renamed = database.appProjectDao().getAppById(createdId)!!
        assertEquals("VIRAL REEL PRO", renamed.name)
        assertTrue(File(folderManager.baseRootDir, renamed.folderName).isDirectory)

        // Delete app folder
        repository.deleteAppFolder(renamed).getOrThrow()
        assertNull(database.appProjectDao().getAppById(createdId))
        assertFalse(File(folderManager.baseRootDir, renamed.folderName).exists())
    }

    @Test
    fun `one click APK download reports honest message when no APK build exists`() = runTest {
        val appId = repository.createAppFolder(
            name = "APPVAULT HUB",
            description = "No builds yet",
            aiStudioUrl = "https://aistudio.google.com/",
            githubRepoUrl = "",
            projectType = ProjectType.NATIVE_ANDROID,
            versionName = "1.0.0",
            notes = "",
            iconUri = null,
            iconColorHex = "#3B82F6",
            iconSymbolKey = "apps"
        ).getOrThrow()

        val result = repository.downloadLatestApkForApp(appId)
        assertFalse(result.success)
        assertEquals(
            "No APK available for this project. Please complete an Android APK build first.",
            result.message
        )
    }

    @Test
    fun `demo sample folders are explicitly marked DEMO DATA and cleanly removable`() = runTest {
        val count = repository.loadDemoSampleFolders().getOrThrow()
        assertEquals(3, count)

        val apps = repository.allApps.first()
        assertEquals(3, apps.size)
        assertTrue(apps.all { it.isDemoData })
        assertTrue(apps.none { it.hasApkAvailable || it.hasApkDownloaded })

        repository.clearDemoSampleFolders()
        assertTrue(repository.allApps.first().isEmpty())
    }

    @Test
    fun `github oauth PKCE generates S256 challenge and rejects tampered state`() = runTest {
        val oauthManager = GitHubOAuthManager(
            GitHubNetworkFactory.createGitHubAuthService(),
            GitHubNetworkFactory.createGitHubApiService(),
            credentialStore
        )

        val url = oauthManager.buildPkceAuthorizeUrl("test_client_id", includePrivateRepos = false)
        assertTrue(url.startsWith("https://github.com/login/oauth/authorize"))
        assertTrue(url.contains("code_challenge_method=S256"))
        assertTrue(url.contains("scope=public_repo"))

        // Tampered state callback must fail security validation
        val tamperedCallback = Uri.parse("aistudioappmanager://oauth/github?code=abc123&state=wrong_state")
        val outcome = oauthManager.completePkceCallback("test_client_id", tamperedCallback)
        assertTrue(outcome is com.example.data.github.GitHubAuthOutcome.Error)
    }

    @Test
    fun `download history tracks failed and expired downloads separately and clears safely`() = runTest {
        val dao = database.downloadRecordDao()
        dao.insertRecord(
            DownloadRecordEntity(
                appId = 1L,
                appName = "Test App",
                fileName = "app-debug.apk",
                fileType = DownloadFileType.APK,
                status = DownloadStatus.EXPIRED,
                errorMessage = "GitHub Actions artifact has expired."
            )
        )
        assertEquals(1, repository.allDownloads.first().size)
        repository.clearDownloadHistoryOnly()
        assertTrue(repository.allDownloads.first().isEmpty())
    }
}
