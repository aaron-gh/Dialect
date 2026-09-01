package com.dialect.launcher.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

/** FR-19/20: matching-mode toggle and empty-state list on/off. */
@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings by settingsRepository.settings.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: Exception) {
            null
        }
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp)) {
            TextButton(onClick = onBack) { Text("Back") }
            Text("Settings", style = MaterialTheme.typography.headlineMedium)

            // The whole row is the toggle target (not just the Switch), and its label/description
            // Text children merge into one accessible name — a bare Switch has no discernible label
            // on its own (a real gap uiautomator flagged as NAF="true" during device verification).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = settings.wordInitialModeEnabled,
                        role = Role.Switch,
                        onValueChange = { enabled ->
                            scope.launch { settingsRepository.setWordInitialModeEnabled(enabled) }
                        },
                    )
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Word-initial matching", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Also match first letters of each word, e.g. 4-6 for \"Google Maps\"",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(checked = settings.wordInitialModeEnabled, onCheckedChange = null)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = settings.emptyStateEnabled,
                        role = Role.Switch,
                        onValueChange = { enabled ->
                            scope.launch { settingsRepository.setEmptyStateEnabled(enabled) }
                        },
                    )
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Show recent apps on empty buffer", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Shown silently; not announced by TalkBack until you navigate to it",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(checked = settings.emptyStateEnabled, onCheckedChange = null)
            }

            if (versionName != null) {
                Text(
                    "Version $versionName",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
        }
    }
}
