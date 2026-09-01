package com.dialect.launcher.launch

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import com.dialect.launcher.appindex.AppIndexEntry

/**
 * Launches apps via LauncherApps (not a plain Intent) so cross-profile targets (work apps) launch
 * correctly (FR-18, T-5), and opens the standard app-info bottom sheet on long-press (FR-18).
 */
class AppLauncher(context: Context) {
    private val launcherApps = context.applicationContext
        .getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private var lastLaunchAtMillis = 0L

    /** Returns false without launching if called again within the debounce window (§10: rapid double-tap Enter). */
    fun launch(entry: AppIndexEntry): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastLaunchAtMillis < DEBOUNCE_MILLIS) return false
        lastLaunchAtMillis = now
        return try {
            launcherApps.startMainActivity(
                ComponentName(entry.packageName, entry.className),
                entry.userHandle,
                null,
                null,
            )
            true
        } catch (e: SecurityException) {
            false
        } catch (e: IllegalStateException) {
            false
        }
    }

    fun openAppInfo(entry: AppIndexEntry) {
        try {
            launcherApps.startAppDetailsActivity(
                ComponentName(entry.packageName, entry.className),
                entry.userHandle,
                null,
                null,
            )
        } catch (e: SecurityException) {
            // No-op: app-info is a convenience action, not launch-critical.
        }
    }

    private companion object {
        const val DEBOUNCE_MILLIS = 500L
    }
}
