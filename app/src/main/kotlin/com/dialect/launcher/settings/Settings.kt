package com.dialect.launcher.settings

/** User-configurable behavior (FR-19/20), backed by DataStore in [SettingsRepository]. */
data class Settings(
    val wordInitialModeEnabled: Boolean = false,
    val emptyStateEnabled: Boolean = true,
    val emptyStateItemCount: Int = 4,
)
