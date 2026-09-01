package com.dialect.launcher.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dialect.launcher.AppContainer
import com.dialect.launcher.appindex.AppIndexEntry
import com.dialect.launcher.appindex.AppIndexRepository
import com.dialect.launcher.contacts.CommunicationService
import com.dialect.launcher.contacts.CommunicationServiceResolver
import com.dialect.launcher.contacts.ContactActionType
import com.dialect.launcher.contacts.ContactIndexRepository
import com.dialect.launcher.contacts.ContactPreference
import com.dialect.launcher.contacts.ContactServicePreferenceRepository
import com.dialect.launcher.contacts.resolveDefaultService
import com.dialect.launcher.launch.AppLauncher
import com.dialect.launcher.matching.MatchEngine
import com.dialect.launcher.settings.Settings
import com.dialect.launcher.usage.UsageStat
import com.dialect.launcher.usage.UsageStatsRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** A buffer starting with this (M=6, 0=separator) searches contacts-only, action = message instead of call. */
private const val MESSAGE_MODE_PREFIX = "60"

/**
 * Combines the buffer, live app + contact indexes, usage stats, and settings into a ranked match
 * list. Because ranking is re-derived from all sources (not just keystrokes), an app being
 * uninstalled while it's the top match re-ranks instantly (§10 edge case).
 */
class HomeViewModel(
    appIndexRepository: AppIndexRepository,
    contactIndexRepository: ContactIndexRepository,
    usageStatsRepository: UsageStatsRepository,
    private val settings: StateFlow<Settings>,
    private val appLauncher: AppLauncher,
    private val communicationServiceResolver: CommunicationServiceResolver,
    private val contactServicePreferenceRepository: ContactServicePreferenceRepository,
) : ViewModel() {
    private val buffer = MutableStateFlow("")

    val uiState: StateFlow<HomeUiState> = combine(
        buffer,
        appIndexRepository.index,
        contactIndexRepository.contacts,
        usageStatsRepository.stats,
        settings,
    ) { buf, apps, contacts, stats, settingsValue ->
        val isMessageMode = buf.startsWith(MESSAGE_MODE_PREFIX)
        val searchBuffer = if (isMessageMode) buf.removePrefix(MESSAGE_MODE_PREFIX) else buf
        val contactActionType = if (isMessageMode) ContactActionType.MESSAGE else ContactActionType.CALL
        val contactTargets = contacts.map { MatchTarget.ContactTarget(it, contactActionType) }
        val targets: List<MatchTarget> = if (isMessageMode) {
            contactTargets
        } else {
            apps.map { MatchTarget.AppTarget(it) } + contactTargets
        }

        HomeUiState(
            buffer = buf,
            matches = MatchEngine.filterAndRank(searchBuffer, targets, stats, settingsValue.wordInitialModeEnabled),
            emptyStateApps = if (buf.isEmpty()) emptyStateApps(apps, stats, settingsValue) else emptyList(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    // Transient, user-action-driven — kept separate from uiState so it isn't wiped out every time
    // uiState recomputes from the combine() above (which has no knowledge of this at all).
    private val _servicePickerRequest = MutableStateFlow<ServicePickerRequest?>(null)
    val servicePickerRequest: StateFlow<ServicePickerRequest?> = _servicePickerRequest

    private val recordLaunch: (String) -> Unit = usageStatsRepository::recordLaunch

    /**
     * A11Y-4: a separate, debounced announcement for the polite live region. Visual state (uiState)
     * updates immediately on every keystroke; this only updates ~300ms after typing pauses, so a
     * fast typist isn't interrupted by a full re-announcement on every digit ("TalkBack spam").
     */
    @OptIn(FlowPreview::class)
    val liveRegionAnnouncement: StateFlow<String> = uiState
        .debounce(300L)
        .map(::describeForAnnouncement)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun onDigit(digit: Char) {
        buffer.value += digit
    }

    fun onBackspace() {
        buffer.value = buffer.value.dropLast(1)
    }

    fun onClearBuffer() {
        buffer.value = ""
    }

    /** FR-16: Enter activates the current top match; no-op if there isn't one. */
    fun onEnter() {
        uiState.value.topMatch?.let(::activate)
    }

    /** FR-17: direct list-item activation, independent of Enter/top-match logic. */
    fun onLaunchMatch(target: MatchTarget) {
        activate(target)
    }

    /** FR-18 for apps: opens the standard app-info bottom sheet. For contacts: always opens the service picker. */
    fun onLongPressMatch(target: MatchTarget) {
        when (target) {
            is MatchTarget.AppTarget -> appLauncher.openAppInfo(target.entry)
            is MatchTarget.ContactTarget -> {
                val available = communicationServiceResolver.availableServicesFor(target.actionType)
                _servicePickerRequest.value = ServicePickerRequest(
                    contactId = target.entry.contactId,
                    contactName = target.entry.displayName,
                    phoneNumber = target.entry.phoneNumber,
                    actionType = target.actionType,
                    availableServices = available,
                    allowSetAsDefault = true,
                )
            }
        }
    }

    /** Resolves the chosen service from a picker, optionally saving it as that contact's override, then executes. */
    fun onServicePicked(service: CommunicationService, setAsDefault: Boolean) {
        val request = _servicePickerRequest.value ?: return
        _servicePickerRequest.value = null
        if (setAsDefault) {
            contactServicePreferenceRepository.setServicePreference(request.contactId, request.actionType, service)
        }
        executeContactAction(request.contactId, request.phoneNumber, request.actionType, service)
    }

    /** Sets this contact to always ask, overriding the global default for them specifically. Nothing to execute yet. */
    fun onAskEveryTimePicked() {
        val request = _servicePickerRequest.value ?: return
        _servicePickerRequest.value = null
        contactServicePreferenceRepository.setAskEveryTime(request.contactId, request.actionType)
    }

    fun onServicePickerDismissed() {
        _servicePickerRequest.value = null
    }

    private fun activate(target: MatchTarget) {
        when (target) {
            is MatchTarget.AppTarget -> launchApp(target.entry)
            is MatchTarget.ContactTarget -> activateContact(target)
        }
    }

    private fun launchApp(entry: AppIndexEntry) {
        if (appLauncher.launch(entry)) {
            recordLaunch(entry.componentKey)
        }
    }

    private fun activateContact(target: MatchTarget.ContactTarget) {
        val entry = target.entry
        val available = communicationServiceResolver.availableServicesFor(target.actionType)
        val contactPreference = contactServicePreferenceRepository.getPreference(entry.contactId, target.actionType)

        // A contact's own preference always wins when set, including AskEveryTime overriding a
        // global default. No contact-level preference at all falls through to the global default.
        val resolved: CommunicationService? = when (contactPreference) {
            is ContactPreference.UseService -> contactPreference.service.takeIf { available.contains(it) }
            ContactPreference.AskEveryTime -> null
            null -> resolveDefaultService(available, globalDefaultServiceFor(target.actionType))
        }

        if (resolved != null) {
            executeContactAction(entry.contactId, entry.phoneNumber, target.actionType, resolved)
        } else {
            _servicePickerRequest.value = ServicePickerRequest(
                contactId = entry.contactId,
                contactName = entry.displayName,
                phoneNumber = entry.phoneNumber,
                actionType = target.actionType,
                availableServices = available,
                allowSetAsDefault = false,
            )
        }
    }

    private fun globalDefaultServiceFor(actionType: ContactActionType): CommunicationService? {
        return when (actionType) {
            ContactActionType.CALL -> settings.value.defaultCallService
            ContactActionType.MESSAGE -> settings.value.defaultMessageService
        }
    }

    private fun executeContactAction(
        contactId: Long,
        phoneNumber: String,
        actionType: ContactActionType,
        service: CommunicationService,
    ) {
        if (communicationServiceResolver.launch(service, actionType, phoneNumber)) {
            recordLaunch("contact:$contactId:${actionType.name}")
        }
    }

    // FR-9: only apps with actual usage qualify as "most-used/most-recent"; a fresh install has none.
    private fun emptyStateApps(
        apps: List<AppIndexEntry>,
        stats: Map<String, UsageStat>,
        settingsValue: Settings,
    ): List<MatchTarget.AppTarget> {
        if (!settingsValue.emptyStateEnabled) return emptyList()
        return apps
            .filter { stats.containsKey(it.componentKey) }
            .sortedWith(
                compareByDescending<AppIndexEntry> { stats.getValue(it.componentKey).launchCount }
                    .thenByDescending { stats.getValue(it.componentKey).lastLaunchedAtMillis }
                    .thenBy { it.displayName.lowercase() },
            )
            .take(settingsValue.emptyStateItemCount)
            .map { MatchTarget.AppTarget(it) }
    }

    private fun describeForAnnouncement(state: HomeUiState): String = when {
        state.buffer.isEmpty() -> ""
        state.matches.isEmpty() -> "No matches"
        else -> "${state.matches.size} matches, top: ${state.matches.first().entry.displayName}"
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(
                container.appIndexRepository,
                container.contactIndexRepository,
                container.usageStatsRepository,
                container.settingsRepository.settings,
                container.appLauncher,
                container.communicationServiceResolver,
                container.contactServicePreferenceRepository,
            ) as T
        }
    }
}
