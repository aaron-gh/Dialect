package com.dialect.launcher

import android.content.Context
import com.dialect.launcher.appindex.AppIndexRepository
import com.dialect.launcher.appindex.IconLoader
import com.dialect.launcher.appindex.LauncherAppsQuerier
import com.dialect.launcher.contacts.CommunicationServiceResolver
import com.dialect.launcher.contacts.ContactIndexRepository
import com.dialect.launcher.contacts.ContactServicePreferenceRepository
import com.dialect.launcher.launch.AppLauncher
import com.dialect.launcher.settings.SettingsRepository
import com.dialect.launcher.usage.UsageStatsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Hand-rolled dependency container on the Application subclass. No DI framework: the app is small
 * enough that this is simpler than adding Hilt's KSP processor on top of Room's.
 */
class AppContainer(context: Context) {
    private val containerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val appIndexRepository = AppIndexRepository(context, LauncherAppsQuerier(context), IconLoader())
    val usageStatsRepository = UsageStatsRepository(context)
    val appLauncher = AppLauncher(context)
    val settingsRepository = SettingsRepository(context, containerScope)

    val contactIndexRepository = ContactIndexRepository(context)
    val communicationServiceResolver = CommunicationServiceResolver(context)
    val contactServicePreferenceRepository = ContactServicePreferenceRepository(context)

    init {
        // Contact indexing only ever runs while the user has explicitly opted in via Settings.
        containerScope.launch {
            settingsRepository.settings
                .map { it.contactDialingEnabled }
                .distinctUntilChanged()
                .collect { enabled ->
                    if (enabled) contactIndexRepository.start() else contactIndexRepository.stop()
                }
        }
    }
}
