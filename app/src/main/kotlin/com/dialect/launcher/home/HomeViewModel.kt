package com.dialect.launcher.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dialect.launcher.AppContainer
import com.dialect.launcher.appindex.AppIndexEntry
import com.dialect.launcher.appindex.AppIndexRepository
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

/**
 * Combines the buffer, live app index, usage stats, and settings into a ranked match list.
 * Because ranking is re-derived from all four sources (not just keystrokes), an app being
 * uninstalled while it's the top match re-ranks instantly (§10 edge case).
 */
class HomeViewModel(
    appIndexRepository: AppIndexRepository,
    usageStatsRepository: UsageStatsRepository,
    settings: StateFlow<Settings>,
    private val appLauncher: AppLauncher,
) : ViewModel() {
    private val buffer = MutableStateFlow("")

    val uiState: StateFlow<HomeUiState> = combine(
        buffer,
        appIndexRepository.index,
        usageStatsRepository.stats,
        settings,
    ) { buf, index, stats, settingsValue ->
        HomeUiState(
            buffer = buf,
            matches = MatchEngine.filterAndRank(buf, index, stats, settingsValue.wordInitialModeEnabled),
            emptyStateApps = if (buf.isEmpty()) emptyStateApps(index, stats, settingsValue) else emptyList(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

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

    /** FR-16: Enter launches the current top match; no-op if there isn't one. */
    fun onEnter() {
        uiState.value.topMatch?.let(::launchEntry)
    }

    /** FR-17: direct list-item activation launches that entry regardless of Enter/top-match logic. */
    fun onLaunchMatch(entry: AppIndexEntry) {
        launchEntry(entry)
    }

    /** FR-18: long-press on a match opens the standard app-info bottom sheet. */
    fun onLongPressMatch(entry: AppIndexEntry) {
        appLauncher.openAppInfo(entry)
    }

    private fun launchEntry(entry: AppIndexEntry) {
        if (appLauncher.launch(entry)) {
            recordLaunch(entry.componentKey)
        }
    }

    // FR-9: only apps with actual usage qualify as "most-used/most-recent"; a fresh install has none.
    private fun emptyStateApps(
        index: List<AppIndexEntry>,
        stats: Map<String, UsageStat>,
        settingsValue: Settings,
    ): List<AppIndexEntry> {
        if (!settingsValue.emptyStateEnabled) return emptyList()
        return index
            .filter { stats.containsKey(it.componentKey) }
            .sortedWith(
                compareByDescending<AppIndexEntry> { stats.getValue(it.componentKey).launchCount }
                    .thenByDescending { stats.getValue(it.componentKey).lastLaunchedAtMillis }
                    .thenBy { it.displayName.lowercase() },
            )
            .take(settingsValue.emptyStateItemCount)
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
                container.usageStatsRepository,
                container.settingsRepository.settings,
                container.appLauncher,
            ) as T
        }
    }
}
