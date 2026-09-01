package com.dialect.launcher.home

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.unit.dp

/**
 * One dialpad key. Minimum 48x48dp touch target (A11Y-8). [contentDescription] replaces the
 * default merged-children announcement so digit keys read like a real phone dialpad ("2, A, B, C")
 * instead of a raw glyph concatenation (A11Y-2).
 */
@Composable
fun KeyButton(
    primaryLabel: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    secondaryLabel: String? = null,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .sizeIn(minWidth = 64.dp, minHeight = 64.dp)
            .combinedClickable(
                enabled = enabled,
                onClick = onClick,
                onLongClick = onLongClick,
                onLongClickLabel = onLongClickLabel,
            )
            .clearAndSetSemantics {
                this.contentDescription = contentDescription
                if (!enabled) disabled()
            },
        shape = CircleShape,
        color = if (enabled) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    ) {
        Column(
            modifier = Modifier.sizeIn(minWidth = 64.dp, minHeight = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(primaryLabel, style = MaterialTheme.typography.headlineSmall)
            if (secondaryLabel != null) {
                Text(secondaryLabel, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
