package com.dialect.launcher.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.dialect.launcher.contacts.CommunicationService
import com.dialect.launcher.contacts.ContactActionType

/**
 * Shown when a contact action has no resolved service to jump straight to: either a tap with no
 * override/default set (one-off choice), or any long-press (always shown, offers to save the
 * choice as that contact's override). "Ask every time" only appears for long-press with 2+
 * services — picking it just saves that preference and closes; there's nothing to execute yet.
 */
@Composable
fun ServicePickerDialog(
    request: ServicePickerRequest,
    onServicePicked: (CommunicationService, setAsDefault: Boolean) -> Unit,
    onAskEveryTimePicked: () -> Unit,
    onDismiss: () -> Unit,
) {
    var setAsDefault by remember(request) { mutableStateOf(request.allowSetAsDefault) }
    val verb = if (request.actionType == ContactActionType.CALL) "Call" else "Message"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$verb ${request.contactName}") },
        text = {
            Column {
                if (request.availableServices.isEmpty()) {
                    Text("No service is available for this.")
                } else {
                    for (service in request.availableServices) {
                        TextButton(onClick = { onServicePicked(service, setAsDefault) }) {
                            Text(service.label)
                        }
                    }
                }
                if (request.allowSetAsDefault) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = setAsDefault,
                                role = Role.Checkbox,
                                onValueChange = { setAsDefault = it },
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = setAsDefault, onCheckedChange = null)
                        Text(
                            "Always use this for ${request.contactName}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (request.availableServices.size > 1) {
                        TextButton(onClick = onAskEveryTimePicked) { Text("Ask every time") }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
