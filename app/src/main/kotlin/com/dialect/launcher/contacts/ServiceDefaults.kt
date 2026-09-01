package com.dialect.launcher.contacts

/**
 * Shared resolution rule, used by both the Settings screen (to decide what's shown as selected)
 * and the runtime action-resolution path (to decide whether to execute directly or ask): when
 * exactly one service is available there's nothing to meaningfully choose between, so it's used
 * automatically regardless of what's stored. With two or more, the stored choice applies (null
 * meaning "ask every time", which covers both "never chosen" and "explicitly chose to be asked").
 */
fun resolveDefaultService(available: List<CommunicationService>, stored: CommunicationService?): CommunicationService? {
    if (available.size == 1) return available.first()
    return stored?.takeIf { available.contains(it) }
}
