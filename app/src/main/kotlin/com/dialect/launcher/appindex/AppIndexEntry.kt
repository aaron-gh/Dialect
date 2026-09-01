package com.dialect.launcher.appindex

import android.os.UserHandle
import androidx.compose.ui.graphics.ImageBitmap
import com.dialect.launcher.matching.T9Nameable

/**
 * One launchable activity in the index (an app may expose more than one, e.g. aliases — FR-8).
 * [componentKey] uniquely identifies this entry across profiles and is the join key for usage stats.
 */
data class AppIndexEntry(
    override val componentKey: String,
    val packageName: String,
    val className: String,
    val userHandle: UserHandle,
    override val displayName: String,
    override val fullPrefixDigits: String,
    override val wordInitialDigits: String,
    val icon: ImageBitmap?,
) : T9Nameable
