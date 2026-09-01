package com.dialect.launcher.home

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dialect.launcher.homerole.HomeRoleManager

@Composable
fun HomeScreen(viewModel: HomeViewModel, onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val announcement by viewModel.liveRegionAnnouncement.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current

    // T-3: Back is a no-op while the launcher itself is foregrounded (standard launcher convention).
    BackHandler(enabled = true) {}

    var isDefaultHome by remember { mutableStateOf(HomeRoleManager.isDefaultHome(context)) }
    val roleRequestLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { isDefaultHome = HomeRoleManager.isDefaultHome(context) }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event -> handlePhysicalKey(event, viewModel) }
            // Edge-to-edge is mandatory from Android 15+ (targetSdk 36): without this, the bottom
            // dialpad row renders behind the gesture nav bar.
            .safeDrawingPadding()
            .padding(16.dp),
    ) {
        // FR-12: the typed buffer shown above the match list, for sighted visual confirmation.
        // A11Y-3: reads before the keypad and match list regardless of where it sits visually.
        // The " " placeholder (kept for layout height stability when empty) must not itself become
        // the announced content — clearAndSetSemantics only sets contentDescription when there's an
        // actual buffer, so an empty buffer has nothing for TalkBack to announce here at all.
        Text(
            text = state.buffer.ifEmpty { " " },
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.clearAndSetSemantics {
                traversalIndex = -2f
                if (state.buffer.isNotEmpty()) {
                    contentDescription = state.buffer
                }
            },
        )

        // A11Y-4: polite live region, debounced so rapid typing isn't interrupted by a re-announcement
        // on every keystroke. Announced automatically on change; not focus-stealing.
        Text(
            text = announcement,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.semantics {
                traversalIndex = -1f
                liveRegion = LiveRegionMode.Polite
            },
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .semantics { traversalIndex = -0.5f },
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // T-7: standard system role-picker UI, so TalkBack users get an accessible picker too.
            if (!isDefaultHome) {
                Text(
                    text = "Set Dialect as default launcher",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .clickable { roleRequestLauncher.launch(HomeRoleManager.createRequestRoleIntent(context)) },
                )
            }
            Text(
                text = "Settings",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.clickable(onClick = onOpenSettings),
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .semantics { isTraversalGroup = true; traversalIndex = 1f },
        ) {
            when {
                state.buffer.isEmpty() -> {
                    // FR-9: shown silently — no live region, no forced focus — spoken only if the
                    // user explicitly navigates into it (A11Y-4's "no spam" principle).
                    if (state.emptyStateApps.isNotEmpty()) {
                        LazyColumn {
                            items(state.emptyStateApps) { entry ->
                                MatchListItem(
                                    entry = entry,
                                    isTopMatch = false,
                                    onClick = { viewModel.onLaunchMatch(entry) },
                                    onLongClick = { viewModel.onLongPressMatch(entry) },
                                )
                            }
                        }
                    }
                }
                state.matches.isEmpty() -> Text("No matches", style = MaterialTheme.typography.bodyLarge)
                else -> LazyColumn {
                    itemsIndexed(state.matches) { index, match ->
                        MatchListItem(
                            entry = match.entry,
                            isTopMatch = index == 0,
                            onClick = { viewModel.onLaunchMatch(match.entry) },
                            onLongClick = { viewModel.onLongPressMatch(match.entry) },
                        )
                    }
                }
            }
        }

        DialpadGrid(
            enterEnabled = state.matches.isNotEmpty(),
            enterContentDescription = state.enterContentDescription,
            onDigit = viewModel::onDigit,
            onBackspace = viewModel::onBackspace,
            onBackspaceLongPress = viewModel::onClearBuffer,
            onEnter = viewModel::onEnter,
        )
    }
}

// FR-15: physical keyboard number row/numpad mapped identically to the on-screen buttons.
private val KEY_TO_DIGIT: Map<Key, Char> = mapOf(
    Key.Zero to '0', Key.NumPad0 to '0',
    Key.One to '1', Key.NumPad1 to '1',
    Key.Two to '2', Key.NumPad2 to '2',
    Key.Three to '3', Key.NumPad3 to '3',
    Key.Four to '4', Key.NumPad4 to '4',
    Key.Five to '5', Key.NumPad5 to '5',
    Key.Six to '6', Key.NumPad6 to '6',
    Key.Seven to '7', Key.NumPad7 to '7',
    Key.Eight to '8', Key.NumPad8 to '8',
    Key.Nine to '9', Key.NumPad9 to '9',
)

private fun handlePhysicalKey(event: KeyEvent, viewModel: HomeViewModel): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    KEY_TO_DIGIT[event.key]?.let {
        viewModel.onDigit(it)
        return true
    }
    return when (event.key) {
        Key.Backspace, Key.Delete -> {
            viewModel.onBackspace()
            true
        }
        Key.Enter, Key.NumPadEnter -> {
            viewModel.onEnter()
            true
        }
        else -> false
    }
}
