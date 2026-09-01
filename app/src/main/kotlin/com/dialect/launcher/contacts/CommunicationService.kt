package com.dialect.launcher.contacts

enum class ContactActionType { CALL, MESSAGE }

/**
 * Services Dialect knows how to route a call/message through. PHONE and SMS have no fixed
 * [packageName] since they're native platform capabilities, not a specific app to detect.
 */
enum class CommunicationService(val id: String, val label: String, val packageName: String?) {
    PHONE("phone", "Phone", null),
    WHATSAPP("whatsapp", "WhatsApp", "com.whatsapp"),
    TELEGRAM("telegram", "Telegram", "org.telegram.messenger"),
    SMS("sms", "Messages", null),
}
