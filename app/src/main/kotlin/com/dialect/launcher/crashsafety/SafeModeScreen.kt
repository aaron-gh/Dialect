package com.dialect.launcher.crashsafety

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private data class SafeModeApp(val label: String, val packageName: String)

/**
 * NFR-4 fallback: no matching engine, no index, no Room/DataStore — just PackageManager and touch,
 * each wrapped in its own try/catch so one bad entry can't crash the fallback itself.
 */
@Composable
fun SafeModeScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val apps = remember { loadAppsSafely(context) }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.safeDrawingPadding().padding(16.dp)) {
            Text(
                "Dialect ran into a problem and is showing a simplified app list.",
                style = MaterialTheme.typography.bodyMedium,
            )
            LazyColumn {
                items(apps) { app ->
                    Text(
                        text = app.label,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { launchSafely(context, app.packageName) }
                            .padding(vertical = 12.dp),
                    )
                }
            }
        }
    }
}

private fun launchSafely(context: Context, packageName: String) {
    try {
        context.packageManager.getLaunchIntentForPackage(packageName)?.let { context.startActivity(it) }
    } catch (e: Exception) {
        // No-op: a broken entry must not crash Safe Mode itself.
    }
}

private fun loadAppsSafely(context: Context): List<SafeModeApp> {
    return try {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            .mapNotNull { resolveInfo ->
                try {
                    SafeModeApp(resolveInfo.loadLabel(pm).toString(), resolveInfo.activityInfo.packageName)
                } catch (e: Exception) {
                    null
                }
            }
            .sortedBy { it.label.lowercase() }
    } catch (e: Exception) {
        emptyList()
    }
}
