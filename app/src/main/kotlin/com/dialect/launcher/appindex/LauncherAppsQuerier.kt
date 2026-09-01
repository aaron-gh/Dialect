package com.dialect.launcher.appindex

import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.os.UserHandle

/**
 * Wraps LauncherApps (not raw PackageManager.queryIntentActivities) — it natively handles apps
 * exposing multiple launcher activities/aliases, profile-badged icons, and cross-profile querying (FR-8, T-5).
 */
class LauncherAppsQuerier(context: Context) {
    private val appContext = context.applicationContext
    private val launcherApps = appContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

    /** All launchable activities across every active user profile. */
    fun queryAll(): List<LauncherActivityInfo> {
        return launcherApps.profiles.flatMap { profile -> launcherApps.getActivityList(null, profile) }
    }

    /** Launchable activities for a single package (used for incremental add/change updates). */
    fun queryForPackage(packageName: String, user: UserHandle): List<LauncherActivityInfo> {
        return launcherApps.getActivityList(packageName, user)
    }
}
