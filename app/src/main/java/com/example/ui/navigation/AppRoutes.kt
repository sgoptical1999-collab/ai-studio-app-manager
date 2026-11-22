package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BuildCircle
import androidx.compose.material.icons.outlined.DownloadForOffline
import androidx.compose.material.icons.outlined.FolderCopy
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.BuildCircle
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

object AppRoutes {
    const val HOME = "home"
    const val MY_APPS = "my_apps"
    const val APK_BUILDS = "apk_builds"
    const val DOWNLOADS = "downloads"
    const val SETTINGS = "settings"

    const val ADD_APP = "add_app"
    const val EDIT_APP = "edit_app/{appId}"
    const val APP_DETAIL = "app_detail/{appId}"
    const val AI_STUDIO_GUIDE = "ai_studio_guide"
    const val GITHUB_CONNECT = "github_connect"

    fun appDetail(appId: Long): String = "app_detail/$appId"
    fun editApp(appId: Long): String = "edit_app/$appId"
}

data class BottomNavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

val PrimaryBottomTabs = listOf(
    BottomNavTab(
        route = AppRoutes.HOME,
        label = "Home",
        selectedIcon = Icons.Rounded.Home,
        unselectedIcon = Icons.Outlined.Home,
        testTag = "nav_tab_home"
    ),
    BottomNavTab(
        route = AppRoutes.MY_APPS,
        label = "My Apps",
        selectedIcon = Icons.Rounded.FolderCopy,
        unselectedIcon = Icons.Outlined.FolderCopy,
        testTag = "nav_tab_my_apps"
    ),
    BottomNavTab(
        route = AppRoutes.APK_BUILDS,
        label = "APK Builds",
        selectedIcon = Icons.Rounded.BuildCircle,
        unselectedIcon = Icons.Outlined.BuildCircle,
        testTag = "nav_tab_apk_builds"
    ),
    BottomNavTab(
        route = AppRoutes.DOWNLOADS,
        label = "Downloads",
        selectedIcon = Icons.Rounded.DownloadForOffline,
        unselectedIcon = Icons.Outlined.DownloadForOffline,
        testTag = "nav_tab_downloads"
    ),
    BottomNavTab(
        route = AppRoutes.SETTINGS,
        label = "Settings",
        selectedIcon = Icons.Rounded.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        testTag = "nav_tab_settings"
    )
)
