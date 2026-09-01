package com.dialect.launcher.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.dp

private data class Key(val primary: String, val letters: String?, val digit: Char)

// FR-11: standard 4x3 phone-dialpad grid, Backspace/Enter in the classic */# positions.
private val NUMBER_ROWS = listOf(
    listOf(Key("1", null, '1'), Key("2", "ABC", '2'), Key("3", "DEF", '3')),
    listOf(Key("4", "GHI", '4'), Key("5", "JKL", '5'), Key("6", "MNO", '6')),
    listOf(Key("7", "PQRS", '7'), Key("8", "TUV", '8'), Key("9", "WXYZ", '9')),
)

// A11Y-2: digit keys announce like a real phone dialpad ("2, A, B, C"), not a raw glyph concatenation.
private fun Key.contentDescription(): String {
    if (letters == null) return primary
    return "$primary, " + letters.toCharArray().joinToString(", ")
}

@Composable
fun DialpadGrid(
    enterEnabled: Boolean,
    enterContentDescription: String,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onBackspaceLongPress: () -> Unit,
    onEnter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // A11Y-3: keypad reads before the match list regardless of visual stacking (set by the caller).
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { isTraversalGroup = true; traversalIndex = 0f },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (row in NUMBER_ROWS) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (key in row) {
                    KeyButton(
                        primaryLabel = key.primary,
                        secondaryLabel = key.letters,
                        contentDescription = key.contentDescription(),
                        onClick = { onDigit(key.digit) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KeyButton(
                primaryLabel = "⌫",
                contentDescription = "Backspace",
                onClick = onBackspace,
                onLongClick = onBackspaceLongPress,
                // A11Y-7: exposed as a distinct, discoverable custom accessibility action, not just a raw gesture.
                onLongClickLabel = "Clear all",
                modifier = Modifier.weight(1f),
            )
            KeyButton(
                primaryLabel = "0",
                contentDescription = "0",
                onClick = { onDigit('0') },
                modifier = Modifier.weight(1f),
            )
            KeyButton(
                primaryLabel = "⏎",
                // A11Y-5/6: label dynamically names the target, or explains why Enter is disabled.
                contentDescription = enterContentDescription,
                enabled = enterEnabled,
                onClick = onEnter,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
