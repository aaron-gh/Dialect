package com.dialect.launcher.quickactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dialect.launcher.R
import com.dialect.launcher.appindex.AppIndexEntry
import com.dialect.launcher.contacts.ContactActionType
import com.dialect.launcher.contacts.ContactIndexEntry

/** Long-press-on-unassigned-digit entry point: the 3-way top-level choice. */
@Composable
fun QuickActionTypeDialog(
    digit: Char,
    onPickCall: () -> Unit,
    onPickMessage: () -> Unit,
    onPickApp: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.quick_action_type_title, digit.toString())) },
        text = {
            Column {
                TextButton(onClick = onPickCall) { Text(stringResource(R.string.quick_action_call_contact)) }
                TextButton(onClick = onPickMessage) { Text(stringResource(R.string.quick_action_message_contact)) }
                TextButton(onClick = onPickApp) { Text(stringResource(R.string.quick_action_open_app)) }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        },
    )
}

/**
 * Full-screen (not a cramped AlertDialog — a real device test showed apps/contacts squeezed into
 * a narrow, fixed-width column with an unlabeled search field) search-and-pick list, shared by the
 * contact and app pickers below since they're identical apart from item type/label extraction.
 */
@Composable
private fun <T> TargetPickerDialog(
    title: String,
    searchLabel: String,
    items: List<T>,
    itemLabel: (T) -> String,
    onPick: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(items, query) {
        items
            .filter { query.isBlank() || itemLabel(it).contains(query, ignoreCase = true) }
            .sortedBy { itemLabel(it).lowercase() }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(title, style = MaterialTheme.typography.headlineSmall)
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(searchLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                )
                if (filtered.isEmpty()) {
                    Text(
                        stringResource(R.string.quick_action_no_results),
                        modifier = Modifier.padding(top = 16.dp),
                    )
                } else {
                    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        items(filtered) { item ->
                            TextButton(onClick = { onPick(item) }, modifier = Modifier.fillMaxWidth()) {
                                Text(itemLabel(item), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionContactPickerDialog(
    contacts: List<ContactIndexEntry>,
    onPick: (ContactIndexEntry) -> Unit,
    onDismiss: () -> Unit,
) {
    val label = stringResource(R.string.quick_action_search_contacts)
    TargetPickerDialog(
        title = label,
        searchLabel = label,
        items = contacts,
        itemLabel = { it.displayName },
        onPick = onPick,
        onDismiss = onDismiss,
    )
}

@Composable
fun QuickActionAppPickerDialog(
    apps: List<AppIndexEntry>,
    onPick: (AppIndexEntry) -> Unit,
    onDismiss: () -> Unit,
) {
    val label = stringResource(R.string.quick_action_search_apps)
    TargetPickerDialog(
        title = label,
        searchLabel = label,
        items = apps,
        itemLabel = { it.displayName },
        onPick = onPick,
        onDismiss = onDismiss,
    )
}

/**
 * Renders whichever step of the assignment flow [flow] is on. Shared by the Home long-press path
 * (state held in HomeViewModel) and the Settings reassignment path (local Compose state).
 */
@Composable
fun QuickActionFlowDialogs(
    flow: QuickActionFlowState?,
    contacts: List<ContactIndexEntry>,
    apps: List<AppIndexEntry>,
    onFlowChange: (QuickActionFlowState?) -> Unit,
    onAssign: (Char, QuickAction) -> Unit,
) {
    when (flow) {
        is QuickActionFlowState.ChoosingType -> QuickActionTypeDialog(
            digit = flow.digit,
            onPickCall = { onFlowChange(QuickActionFlowState.ChoosingContact(flow.digit, ContactActionType.CALL)) },
            onPickMessage = { onFlowChange(QuickActionFlowState.ChoosingContact(flow.digit, ContactActionType.MESSAGE)) },
            onPickApp = { onFlowChange(QuickActionFlowState.ChoosingApp(flow.digit)) },
            onDismiss = { onFlowChange(null) },
        )
        is QuickActionFlowState.ChoosingContact -> QuickActionContactPickerDialog(
            contacts = contacts,
            onPick = { entry ->
                val action = when (flow.actionType) {
                    ContactActionType.CALL -> QuickAction.CallContact(entry.contactId, entry.displayName)
                    ContactActionType.MESSAGE -> QuickAction.MessageContact(entry.contactId, entry.displayName)
                }
                onAssign(flow.digit, action)
            },
            onDismiss = { onFlowChange(null) },
        )
        is QuickActionFlowState.ChoosingApp -> QuickActionAppPickerDialog(
            apps = apps,
            onPick = { entry -> onAssign(flow.digit, QuickAction.OpenApp(entry.componentKey, entry.displayName)) },
            onDismiss = { onFlowChange(null) },
        )
        null -> Unit
    }
}

/** Resolves to localized "Call {label}" / "Message {label}" / "Open {label}", same split-for-testability reasoning as EnterDescription.resolve() in HomeScreen.kt. */
@Composable
fun QuickAction.resolveLabel(): String = when (this) {
    is QuickAction.CallContact -> stringResource(R.string.quick_action_calls, label)
    is QuickAction.MessageContact -> stringResource(R.string.quick_action_messages, label)
    is QuickAction.OpenApp -> stringResource(R.string.quick_action_opens, label)
}
