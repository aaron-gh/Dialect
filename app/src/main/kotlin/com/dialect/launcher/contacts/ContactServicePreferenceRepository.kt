package com.dialect.launcher.contacts

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** In-memory-first, same pattern as UsageStatsRepository: a set updates the map synchronously, Room write is fire-and-forget. */
class ContactServicePreferenceRepository(context: Context) {
    private val db = Room.databaseBuilder(
        context.applicationContext,
        ContactPreferenceDatabase::class.java,
        "contact_service_preferences.db",
    )
        // Pre-release, device-local, non-critical data (rebuildable by re-picking a service) —
        // simplest way to absorb this schema's own recent addition of the ASK_EVERY_TIME kind.
        .fallbackToDestructiveMigration(true)
        .build()
    private val dao = db.contactServicePreferenceDao()
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _preferences = MutableStateFlow<Map<Pair<Long, ContactActionType>, ContactPreference>>(emptyMap())
    val preferences: StateFlow<Map<Pair<Long, ContactActionType>, ContactPreference>> = _preferences.asStateFlow()

    init {
        repoScope.launch {
            _preferences.value = dao.getAll().mapNotNull { entity ->
                val actionType = runCatching { ContactActionType.valueOf(entity.actionType) }.getOrNull()
                    ?: return@mapNotNull null
                val preference = entityToPreference(entity) ?: return@mapNotNull null
                (entity.contactId to actionType) to preference
            }.toMap()
        }
    }

    fun getPreference(contactId: Long, actionType: ContactActionType): ContactPreference? {
        return _preferences.value[contactId to actionType]
    }

    fun setServicePreference(contactId: Long, actionType: ContactActionType, service: CommunicationService) {
        setPreference(contactId, actionType, ContactPreference.UseService(service))
    }

    fun setAskEveryTime(contactId: Long, actionType: ContactActionType) {
        setPreference(contactId, actionType, ContactPreference.AskEveryTime)
    }

    private fun setPreference(contactId: Long, actionType: ContactActionType, preference: ContactPreference) {
        _preferences.value = _preferences.value + ((contactId to actionType) to preference)
        repoScope.launch {
            dao.upsert(preferenceToEntity(contactId, actionType, preference))
        }
    }

    private fun entityToPreference(entity: ContactServicePreferenceEntity): ContactPreference? {
        return when (entity.kind) {
            ContactServicePreferenceEntity.KIND_ASK_EVERY_TIME -> ContactPreference.AskEveryTime
            ContactServicePreferenceEntity.KIND_SERVICE -> {
                val service = CommunicationService.entries.find { it.id == entity.serviceId } ?: return null
                ContactPreference.UseService(service)
            }
            else -> null
        }
    }

    private fun preferenceToEntity(
        contactId: Long,
        actionType: ContactActionType,
        preference: ContactPreference,
    ): ContactServicePreferenceEntity {
        return when (preference) {
            is ContactPreference.UseService -> ContactServicePreferenceEntity(
                contactId, actionType.name, ContactServicePreferenceEntity.KIND_SERVICE, preference.service.id,
            )
            ContactPreference.AskEveryTime -> ContactServicePreferenceEntity(
                contactId, actionType.name, ContactServicePreferenceEntity.KIND_ASK_EVERY_TIME, null,
            )
        }
    }
}
