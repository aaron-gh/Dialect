package com.dialect.launcher.settings

import com.dialect.launcher.contacts.CommunicationService

/** User-configurable behavior (FR-19/20), backed by DataStore in [SettingsRepository]. */
data class Settings(
    val wordInitialModeEnabled: Boolean = false,
    val emptyStateEnabled: Boolean = true,
    val emptyStateItemCount: Int = 4,
    val contactDialingEnabled: Boolean = false,
    val defaultCallService: CommunicationService? = null,
    val defaultMessageService: CommunicationService? = null,
)
