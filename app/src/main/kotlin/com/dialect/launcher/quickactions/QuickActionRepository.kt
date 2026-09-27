package com.dialect.launcher.quickactions

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** In-memory-first, same pattern as ContactServicePreferenceRepository: a set updates the map synchronously, Room write is fire-and-forget. */
class QuickActionRepository(context: Context) {
    private val db = Room.databaseBuilder(
        context.applicationContext,
        QuickActionDatabase::class.java,
        "quick_actions.db",
    )
        // Pre-release, device-local, non-critical data (rebuildable by re-assigning a digit).
        .fallbackToDestructiveMigration(true)
        .build()
    private val dao = db.quickActionDao()
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _actions = MutableStateFlow<Map<Char, QuickAction>>(emptyMap())
    val actions: StateFlow<Map<Char, QuickAction>> = _actions.asStateFlow()

    init {
        repoScope.launch {
            _actions.value = dao.getAll().mapNotNull { entity ->
                val action = entityToAction(entity) ?: return@mapNotNull null
                digitToChar(entity.digit) to action
            }.toMap()
        }
    }

    fun assign(digit: Char, action: QuickAction) {
        _actions.value = _actions.value + (digit to action)
        repoScope.launch {
            dao.upsert(actionToEntity(digit, action))
        }
    }

    fun remove(digit: Char) {
        _actions.value = _actions.value - digit
        repoScope.launch {
            dao.deleteByDigit(digit.digitToIntOrNull() ?: return@launch)
        }
    }

    private fun digitToChar(digit: Int): Char = ('0' + digit)

    private fun entityToAction(entity: QuickActionEntity): QuickAction? {
        return when (entity.kind) {
            QuickActionEntity.KIND_CALL_CONTACT -> entity.contactId?.let { QuickAction.CallContact(it, entity.label) }
            QuickActionEntity.KIND_MESSAGE_CONTACT -> entity.contactId?.let { QuickAction.MessageContact(it, entity.label) }
            QuickActionEntity.KIND_OPEN_APP -> entity.componentKey?.let { QuickAction.OpenApp(it, entity.label) }
            else -> null
        }
    }

    private fun actionToEntity(digit: Char, action: QuickAction): QuickActionEntity {
        val digitInt = digit.digitToIntOrNull() ?: error("Quick actions can only be assigned to digits 0-9")
        return when (action) {
            is QuickAction.CallContact -> QuickActionEntity(
                digitInt, QuickActionEntity.KIND_CALL_CONTACT, action.contactId, null, action.label,
            )
            is QuickAction.MessageContact -> QuickActionEntity(
                digitInt, QuickActionEntity.KIND_MESSAGE_CONTACT, action.contactId, null, action.label,
            )
            is QuickAction.OpenApp -> QuickActionEntity(
                digitInt, QuickActionEntity.KIND_OPEN_APP, null, action.componentKey, action.label,
            )
        }
    }
}
