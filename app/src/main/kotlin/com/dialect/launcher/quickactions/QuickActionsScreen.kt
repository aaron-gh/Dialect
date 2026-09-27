package com.dialect.launcher.quickactions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dialect.launcher.R
import com.dialect.launcher.appindex.AppIndexEntry
import com.dialect.launcher.contacts.ContactIndexEntry
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages already-assigned digits only (reassign or remove); new assignment always starts from a
 * long-press on the dialpad. Reuses the Home long-press dialogs, driven by local Compose state.
 */
@Composable
fun QuickActionsScreen(
    quickActionRepository: QuickActionRepository,
    appsFlow: StateFlow<List<AppIndexEntry>>,
    contactsFlow: StateFlow<List<ContactIndexEntry>>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val actions by quickActionRepository.actions.collectAsStateWithLifecycle()
    val contacts by contactsFlow.collectAsStateWithLifecycle()
    val apps by appsFlow.collectAsStateWithLifecycle()

    var editingDigit by remember { mutableStateOf<Char?>(null) }
    var flowState by remember { mutableStateOf<QuickActionFlowState?>(null) }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp)) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.settings_back)) }
            Text(stringResource(R.string.quick_actions_title), style = MaterialTheme.typography.headlineMedium)

            if (actions.isEmpty()) {
                Text(
                    stringResource(R.string.quick_actions_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 16.dp),
                )
            } else {
                for (digit in actions.keys.sorted()) {
                    val action = actions.getValue(digit)
                    TextButton(onClick = { editingDigit = digit }) {
                        Text(stringResource(R.string.quick_action_digit_row, digit.toString(), action.resolveLabel()))
                    }
                }
            }
        }
    }

    editingDigit?.let { digit ->
        AlertDialog(
            onDismissRequest = { editingDigit = null },
            title = { Text(digit.toString()) },
            text = {
                Column {
                    TextButton(onClick = {
                        editingDigit = null
                        flowState = QuickActionFlowState.ChoosingType(digit)
                    }) { Text(stringResource(R.string.quick_action_change)) }
                    TextButton(onClick = {
                        quickActionRepository.remove(digit)
                        editingDigit = null
                    }) { Text(stringResource(R.string.quick_action_remove)) }
                }
            },
            confirmButton = {
                TextButton(onClick = { editingDigit = null }) { Text(stringResource(R.string.dialog_cancel)) }
            },
        )
    }

    QuickActionFlowDialogs(
        flow = flowState,
        contacts = contacts,
        apps = apps,
        onFlowChange = { flowState = it },
        onAssign = { digit, action ->
            flowState = null
            quickActionRepository.assign(digit, action)
        },
    )
}
