package com.dialect.launcher.appindex

import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.os.UserHandle
import com.dialect.launcher.matching.T9Sequence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Owns the live, in-memory app index (FR-8). Rebuilds happen off the main thread via plain
 * coroutines (index/PackageManager calls are blocking IPC); package removal applies an immediate
 * in-memory delta so a top match that gets uninstalled re-ranks instantly, without waiting for a
 * keystroke (§10 edge case).
 */
class AppIndexRepository(
    context: Context,
    private val querier: LauncherAppsQuerier,
    private val iconLoader: IconLoader,
) {
    private val appContext = context.applicationContext
    private val launcherApps = appContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _index = MutableStateFlow<List<AppIndexEntry>>(emptyList())
    val index: StateFlow<List<AppIndexEntry>> = _index.asStateFlow()

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageAdded(packageName: String, user: UserHandle) = refreshPackage(packageName, user)
        override fun onPackageChanged(packageName: String, user: UserHandle) = refreshPackage(packageName, user)
        override fun onPackageRemoved(packageName: String, user: UserHandle) = removePackage(packageName, user)

        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
            rebuild()
        }

        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
            for (packageName in packageNames) removePackage(packageName, user)
        }
    }

    fun start() {
        launcherApps.registerCallback(callback)
        rebuild()
    }

    fun stop() {
        launcherApps.unregisterCallback(callback)
    }

    fun rebuild() {
        repoScope.launch {
            val entries = querier.queryAll().map(::toEntry)
            _index.value = entries
        }
    }

    private fun refreshPackage(packageName: String, user: UserHandle) {
        repoScope.launch {
            val updated = querier.queryForPackage(packageName, user).map(::toEntry)
            _index.update { current ->
                current.filterNot { it.packageName == packageName && it.userHandle == user } + updated
            }
        }
    }

    private fun removePackage(packageName: String, user: UserHandle) {
        _index.update { current -> current.filterNot { it.packageName == packageName && it.userHandle == user } }
    }

    private fun toEntry(info: LauncherActivityInfo): AppIndexEntry {
        val name = info.label.toString()
        return AppIndexEntry(
            componentKey = "${info.componentName.flattenToString()}#${info.user}",
            packageName = info.componentName.packageName,
            className = info.componentName.className,
            userHandle = info.user,
            displayName = name,
            fullPrefixDigits = T9Sequence.fullPrefixDigits(name),
            wordInitialDigits = T9Sequence.wordInitialDigits(name),
            icon = iconLoader.load(info),
        )
    }
}
