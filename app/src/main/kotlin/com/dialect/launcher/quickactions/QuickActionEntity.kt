package com.dialect.launcher.quickactions

import androidx.room.Entity

/** One digit's (0-9) assigned quick action. Only one of [contactId]/[componentKey] is set, per [kind]. */
@Entity(tableName = "quick_action", primaryKeys = ["digit"])
data class QuickActionEntity(
    val digit: Int,
    val kind: String,
    val contactId: Long?,
    val componentKey: String?,
    val label: String,
) {
    companion object {
        const val KIND_CALL_CONTACT = "CALL_CONTACT"
        const val KIND_MESSAGE_CONTACT = "MESSAGE_CONTACT"
        const val KIND_OPEN_APP = "OPEN_APP"
    }
}
