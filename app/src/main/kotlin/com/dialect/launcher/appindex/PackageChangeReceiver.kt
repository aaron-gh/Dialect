package com.dialect.launcher.appindex

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dialect.launcher.DialectApplication

/**
 * Safety net for FR-8: LauncherApps.Callback (registered in AppIndexRepository.start) is the
 * primary live-update signal, but it only fires while the process is alive to receive it. This
 * manifest-declared receiver catches package changes that happened while the process was dead,
 * triggering a rebuild on next launch. This is the one broadcast receiver NFR-3 permits.
 */
class PackageChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as DialectApplication
        app.container?.appIndexRepository?.rebuild()
    }
}
