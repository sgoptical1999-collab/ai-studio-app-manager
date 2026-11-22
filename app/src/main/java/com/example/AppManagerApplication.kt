package com.example

import android.app.Application
import com.example.data.backup.BackupRestoreManager
import com.example.data.github.GitHubNetworkFactory
import com.example.data.github.GitHubOAuthManager
import com.example.data.installer.ApkInstallerManager
import com.example.data.local.AppDatabase
import com.example.data.preferences.SettingsDataStore
import com.example.data.repository.AppManagerRepository
import com.example.data.storage.AppFolderManager
import com.example.security.SecureCredentialStore
import com.example.worker.NotificationHelper

class AppManagerApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var folderManager: AppFolderManager
        private set

    lateinit var credentialStore: SecureCredentialStore
        private set

    lateinit var settingsDataStore: SettingsDataStore
        private set

    lateinit var githubOAuthManager: GitHubOAuthManager
        private set

    lateinit var installerManager: ApkInstallerManager
        private set

    lateinit var backupRestoreManager: BackupRestoreManager
        private set

    lateinit var repository: AppManagerRepository
        private set

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannel(this)

        database = AppDatabase.getInstance(this)
        folderManager = AppFolderManager(this)
        credentialStore = SecureCredentialStore(this)
        settingsDataStore = SettingsDataStore(this)
        installerManager = ApkInstallerManager(this)
        backupRestoreManager = BackupRestoreManager(this, database, folderManager)

        val githubApi = GitHubNetworkFactory.createGitHubApiService()
        val githubAuthApi = GitHubNetworkFactory.createGitHubAuthService()
        githubOAuthManager = GitHubOAuthManager(githubAuthApi, githubApi, credentialStore)

        repository = AppManagerRepository(
            context = this,
            database = database,
            folderManager = folderManager,
            githubApi = githubApi,
            credentialStore = credentialStore,
            settingsDataStore = settingsDataStore,
            backupRestoreManager = backupRestoreManager
        )
    }
}
