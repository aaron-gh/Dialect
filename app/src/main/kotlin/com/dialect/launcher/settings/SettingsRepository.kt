package com.dialect.launcher.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dialect.launcher.contacts.CommunicationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private val Context.dataStore by preferencesDataStore(name = "dialect_settings")

private val WORD_INITIAL_MODE_ENABLED = booleanPreferencesKey("word_initial_mode_enabled")
private val EMPTY_STATE_ENABLED = booleanPreferencesKey("empty_state_enabled")
private val EMPTY_STATE_ITEM_COUNT = intPreferencesKey("empty_state_item_count")
private val CONTACT_DIALING_ENABLED = booleanPreferencesKey("contact_dialing_enabled")
private val DEFAULT_CALL_SERVICE = stringPreferencesKey("default_call_service")
private val DEFAULT_MESSAGE_SERVICE = stringPreferencesKey("default_message_service")

class SettingsRepository(context: Context, scope: CoroutineScope) {
    private val dataStore = context.applicationContext.dataStore

    val settings: StateFlow<Settings> = dataStore.data
        .map { prefs ->
            Settings(
                wordInitialModeEnabled = prefs[WORD_INITIAL_MODE_ENABLED] ?: false,
                emptyStateEnabled = prefs[EMPTY_STATE_ENABLED] ?: true,
                emptyStateItemCount = prefs[EMPTY_STATE_ITEM_COUNT] ?: 4,
                contactDialingEnabled = prefs[CONTACT_DIALING_ENABLED] ?: false,
                defaultCallService = serviceFromId(prefs[DEFAULT_CALL_SERVICE]),
                defaultMessageService = serviceFromId(prefs[DEFAULT_MESSAGE_SERVICE]),
            )
        }
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), Settings())

    suspend fun setWordInitialModeEnabled(enabled: Boolean) {
        dataStore.edit { it[WORD_INITIAL_MODE_ENABLED] = enabled }
    }

    suspend fun setEmptyStateEnabled(enabled: Boolean) {
        dataStore.edit { it[EMPTY_STATE_ENABLED] = enabled }
    }

    suspend fun setEmptyStateItemCount(count: Int) {
        dataStore.edit { it[EMPTY_STATE_ITEM_COUNT] = count }
    }

    suspend fun setContactDialingEnabled(enabled: Boolean) {
        dataStore.edit { it[CONTACT_DIALING_ENABLED] = enabled }
    }

    suspend fun setDefaultCallService(service: CommunicationService?) {
        dataStore.edit {
            if (service == null) it.remove(DEFAULT_CALL_SERVICE) else it[DEFAULT_CALL_SERVICE] = service.id
        }
    }

    suspend fun setDefaultMessageService(service: CommunicationService?) {
        dataStore.edit {
            if (service == null) it.remove(DEFAULT_MESSAGE_SERVICE) else it[DEFAULT_MESSAGE_SERVICE] = service.id
        }
    }

    private fun serviceFromId(id: String?): CommunicationService? {
        return CommunicationService.entries.find { it.id == id }
    }
}
