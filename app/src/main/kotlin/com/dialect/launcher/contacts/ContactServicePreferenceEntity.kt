package com.dialect.launcher.contacts

import androidx.room.Entity

/** A contact's "always use this service" or "always ask" override, independent per action type. */
@Entity(tableName = "contact_service_preference", primaryKeys = ["contactId", "actionType"])
data class ContactServicePreferenceEntity(
    val contactId: Long,
    val actionType: String,
    val kind: String,
    val serviceId: String?,
) {
    companion object {
        const val KIND_SERVICE = "SERVICE"
        const val KIND_ASK_EVERY_TIME = "ASK_EVERY_TIME"
    }
}
