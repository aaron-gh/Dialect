package com.dialect.launcher.settings

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import com.dialect.launcher.contacts.CommunicationService
import com.dialect.launcher.contacts.CommunicationServiceResolver
import com.dialect.launcher.contacts.ContactActionType
import com.dialect.launcher.contacts.resolveDefaultService
import kotlinx.coroutines.launch

/** FR-19/20: matching-mode toggle and empty-state list on/off, plus contact search & dialing. */
@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    communicationServiceResolver: CommunicationServiceResolver,
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

    // All three requested together, only when this is turned on — never at first launch.
    // READ_PHONE_STATE isn't required for the toggle itself to enable (contacts + Phone calling +
    // SMS/WhatsApp messaging all work without it); it's only needed to detect self-managed calling
    // apps like WhatsApp as CALL options, so its absence degrades gracefully rather than blocking.
    val contactPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        val granted = results[Manifest.permission.READ_CONTACTS] == true &&
            results[Manifest.permission.CALL_PHONE] == true
        scope.launch { settingsRepository.setContactDialingEnabled(granted) }
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = settings.contactDialingEnabled,
                        role = Role.Switch,
                        onValueChange = { enabled ->
                            if (enabled) {
                                contactPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.READ_CONTACTS,
                                        Manifest.permission.CALL_PHONE,
                                        Manifest.permission.READ_PHONE_STATE,
                                    ),
                                )
                            } else {
                                scope.launch { settingsRepository.setContactDialingEnabled(false) }
                            }
                        },
                    )
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Contact search & dialing", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Search contacts by name (or type 60 then a name to message instead of call)",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(checked = settings.contactDialingEnabled, onCheckedChange = null)
            }

            if (settings.contactDialingEnabled) {
                ServiceSelector(
                    title = "Default call service",
                    services = communicationServiceResolver.availableServicesFor(ContactActionType.CALL),
                    selected = settings.defaultCallService,
                    onSelect = { service -> scope.launch { settingsRepository.setDefaultCallService(service) } },
                )
                ServiceSelector(
                    title = "Default message service",
                    services = communicationServiceResolver.availableServicesFor(ContactActionType.MESSAGE),
                    selected = settings.defaultMessageService,
                    onSelect = { service -> scope.launch { settingsRepository.setDefaultMessageService(service) } },
                )
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

/**
 * With exactly one service there's nothing to choose between, so it's shown selected and locked
 * rather than as an interactive choice. With two or more, each service is selectable alongside an
 * explicit "Ask every time" option — [onSelect] receives null for that choice.
 */
@Composable
private fun ServiceSelector(
    title: String,
    services: List<CommunicationService>,
    selected: CommunicationService?,
    onSelect: (CommunicationService?) -> Unit,
) {
    val effectiveSelection = resolveDefaultService(services, selected)

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        if (services.isEmpty()) {
            Text("No services available on this device", style = MaterialTheme.typography.bodySmall)
        } else {
            Column(modifier = Modifier.selectableGroup()) {
                for (service in services) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = effectiveSelection == service,
                                enabled = services.size > 1,
                                role = Role.RadioButton,
                                onClick = { onSelect(service) },
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = effectiveSelection == service, onClick = null)
                        Text(service.label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (services.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = effectiveSelection == null,
                                role = Role.RadioButton,
                                onClick = { onSelect(null) },
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = effectiveSelection == null, onClick = null)
                        Text("Ask every time", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
