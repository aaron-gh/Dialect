package com.dialect.launcher.contacts

import androidx.annotation.StringRes
import com.dialect.launcher.R

enum class ContactActionType { CALL, MESSAGE }

/**
 * Services Dialect knows how to route a call/message through. PHONE and SMS have no fixed
 * [packageName] since they're native platform capabilities, not a specific app to detect.
 * [labelRes] rather than a literal label since this enum is plain Kotlin (no Compose context to
 * resolve a string at definition time) — resolve with stringResource(service.labelRes) at the
 * Compose call site. WhatsApp/Telegram are brand names and read identically in every locale.
 */
enum class CommunicationService(val id: String, @param:StringRes val labelRes: Int, val packageName: String?) {
    PHONE("phone", R.string.service_phone, null),
    WHATSAPP("whatsapp", R.string.service_whatsapp, "com.whatsapp"),
    TELEGRAM("telegram", R.string.service_telegram, "org.telegram.messenger"),
    SMS("sms", R.string.service_sms, null),
}
