package com.dialect.launcher.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.dialect.launcher.R
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.dp

private data class Key(val primary: String, val letters: String?, val digit: Char)

// 1.2.0: which script's letters the dialpad legend shows/announces, since a Cyrillic or Greek
// keypad user expects to see their own alphabet on the keys, not English ones. T9 matching
// itself is unaffected by this choice (T9Mapper matches every supported script at once).
private val LATIN_LETTERS = mapOf('2' to "ABC", '3' to "DEF", '4' to "GHI", '5' to "JKL", '6' to "MNO", '7' to "PQRS", '8' to "TUV", '9' to "WXYZ")
private val CYRILLIC_LETTERS = mapOf('2' to "АБВГ", '3' to "ДЕЁЖЗ", '4' to "ИЙКЛ", '5' to "МНОП", '6' to "РСТУ", '7' to "ФХЦЧ", '8' to "ШЩЪЫ", '9' to "ЬЭЮЯ")
private val GREEK_LETTERS = mapOf('2' to "ΑΒΓ", '3' to "ΔΕΖ", '4' to "ΗΘΙ", '5' to "ΚΛΜ", '6' to "ΝΞΟ", '7' to "ΠΡΣ", '8' to "ΤΥΦ", '9' to "ΧΨΩ")

private fun keypadLettersFor(language: String): Map<Char, String> = when (language) {
    "ru" -> CYRILLIC_LETTERS
    "el" -> GREEK_LETTERS
    else -> LATIN_LETTERS
}

// FR-11: standard 4x3 phone-dialpad grid, Backspace/Enter in the classic */# positions.
private fun numberRows(letters: Map<Char, String>) = listOf(
    listOf(Key("1", null, '1'), Key("2", letters.getValue('2'), '2'), Key("3", letters.getValue('3'), '3')),
    listOf(Key("4", letters.getValue('4'), '4'), Key("5", letters.getValue('5'), '5'), Key("6", letters.getValue('6'), '6')),
    listOf(Key("7", letters.getValue('7'), '7'), Key("8", letters.getValue('8'), '8'), Key("9", letters.getValue('9'), '9')),
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
    onDigitLongPress: (Char) -> Unit,
    digitLongPressLabel: (Char) -> String?,
    onBackspace: () -> Unit,
    onBackspaceLongPress: () -> Unit,
    onEnter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 1.2.0: the key legend follows the current language (Cyrillic/Greek get their own alphabet).
    val language = LocalConfiguration.current.locales[0].language
    val rows = numberRows(keypadLettersFor(language))

    // A11Y-3: keypad reads before the match list regardless of visual stacking (set by the caller).
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { isTraversalGroup = true; traversalIndex = 0f },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (row in rows) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (key in row) {
                    KeyButton(
                        primaryLabel = key.primary,
                        secondaryLabel = key.letters,
                        contentDescription = key.contentDescription(),
                        onClick = { onDigit(key.digit) },
                        onLongClick = { onDigitLongPress(key.digit) },
                        onLongClickLabel = digitLongPressLabel(key.digit),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KeyButton(
                primaryLabel = "⌫",
                contentDescription = stringResource(R.string.dialpad_backspace),
                onClick = onBackspace,
                onLongClick = onBackspaceLongPress,
                // A11Y-7: exposed as a distinct, discoverable custom accessibility action, not just a raw gesture.
                onLongClickLabel = stringResource(R.string.dialpad_clear_all),
                modifier = Modifier.weight(1f),
            )
            KeyButton(
                primaryLabel = "0",
                contentDescription = "0",
                onClick = { onDigit('0') },
                onLongClick = { onDigitLongPress('0') },
                onLongClickLabel = digitLongPressLabel('0'),
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
