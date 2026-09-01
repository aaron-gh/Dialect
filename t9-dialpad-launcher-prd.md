# Product Requirements Document: Dialect

**Name:** Dialect
**Status:** Draft v1.0
**Author:** Aaron
**Last updated:** 2026-08-15

---

## 1. Overview

A minimal Android home screen replacement (launcher) whose primary — and for MVP, only — interaction surface is a phone-style dialpad. Instead of scrolling an app grid or typing into a text search box, the user presses number keys T9-style; each keypress narrows a ranked list of installed apps whose names match the digit sequence typed so far. Pressing **Enter** (bottom-right key) launches the current top match.

This is a search-first launcher: there is no app drawer to browse and no icon grid to scan. The dialpad *is* the home screen at all times.

### 1.1 Motivating example

- Typing `3-4-7` narrows toward apps like **Discord** (D=3, I=4, S=7...). Pressing **Enter** launches the top-ranked match.
- Typing `6`, then `6-2`, then `6-2-8`, then `6-2-8-2` should each independently return **Obtainium** as a live candidate, because each prefix (`O`, `OB`, `OBT`, `OBTA`) maps to that digit sequence under standard T9 letter mapping. The match list narrows progressively as digits are added; it never requires the full word.

### 1.2 Why this exists

Traditional launchers optimize for visual scanning (icon grids, folders, search bars requiring a keyboard). A dialpad-first launcher optimizes for **fast, muscle-memory, non-visual input** — closer to phone dialing than to typing — which is both a novel interaction model for sighted power users and, done correctly, a strong fit for blind/low-vision users who already rely on predictable spatial layouts (this is explicitly a hard requirement, not a nice-to-have — see §9).

---

## 2. Goals

| # | Goal |
|---|------|
| G1 | Launch any installed app in ≤4 keypresses + Enter for the overwhelming majority of a typical user's app set. |
| G2 | Be fully operable and fully *understandable* using TalkBack alone, with no sighted assistance required for setup, daily use, or recovery from mistakes. |
| G3 | Be installable and selectable as the Android default launcher (HOME role), and behave correctly as one (survives reboot, handles the Home button, back-stack behavior, app add/remove). |
| G4 | Do all matching on-device, instantly, with no network dependency and no telemetry by default. |

### 2.1 Non-goals (MVP)

- No app drawer / grid / folder browsing UI.
- No home screen widgets.
- No wallpaper personalization beyond a solid theme-appropriate background.
- No notification badges or dots.
- No gesture navigation customization (swipe-up-for-apps, etc.) — irrelevant since there's no drawer.
- No cloud sync, backup of usage stats, or account system.

These may become Phase 2+ items (see §14) but must not block MVP.

---

## 3. Target Users

1. **Primary: blind and low-vision users on TalkBack** who want a predictable, low-cognitive-load way to launch apps without scanning a grid or hunting for a search field with an on-screen keyboard.
2. **Secondary: sighted power users** who want faster app launch than swipe-and-scroll and like the phone-dialer muscle memory.
3. **Tertiary: minimalists** who want a distraction-free home screen with no visual clutter, feeds, or widgets.

---

## 4. Glossary

| Term | Meaning |
|---|---|
| **T9** | Predictive text input scheme originally for phone keypads, mapping letters to digits (see §6.1 table). |
| **Prefix match** | The typed digit sequence corresponds to the start of the app's display name. |
| **Word-initial match** | The typed digit sequence corresponds to the first letter of each word in a multi-word app name (e.g. "Google Maps" → `4-6` for G, M). |
| **HOME role** | The Android system role (`RoleManager.ROLE_HOME`) that designates the default launcher, handling `Intent.ACTION_MAIN` / `Intent.CATEGORY_HOME`. |
| **Launcher activity** | A specific activity a package exposes with `CATEGORY_LAUNCHER`; some apps expose more than one (e.g. work-profile duplicates, app aliases). |
| **Live region** | An Android accessibility concept where a view's content changes are automatically announced by TalkBack without requiring explicit focus. |

---

## 5. Core User Flows

### 5.1 Happy path: launch an app
1. User presses Home (or the launcher is already foregrounded).
2. Dialpad is shown, empty digit buffer, no matches shown (or optionally: most-used apps shown as defaults — see FR-9).
3. User presses digits corresponding to the app name.
4. After each digit, the match list re-filters and re-ranks; the top match is reflected both visually (first list item) and in the Enter key's accessible label (see A11Y-5).
5. User presses Enter. Top match launches.
6. Launcher returns to empty-buffer state next time it's foregrounded (see FR-10).

### 5.2 Correcting a mistake
1. User has typed digits producing an unwanted top match.
2. User presses Backspace (bottom-left key) to delete the last digit, or long-presses Backspace to clear the whole buffer.
3. Match list updates accordingly.

### 5.3 Choosing a non-top match
1. Several apps match the typed digits.
2. User navigates the match list directly (touch, TalkBack swipe, or D-pad/keyboard arrow) instead of pressing Enter, and activates a specific list item.
3. That app launches, bypassing the Enter/top-match shortcut.

### 5.4 No matches
1. Typed digit sequence matches no installed app.
2. List area shows/announces "No matches."
3. Enter key becomes disabled and its accessible label reflects that state (see A11Y-6).
4. **Decision: no fallback to web search or Play Store on zero matches.** The launcher stays fully offline and local-only in every state, consistent with NFR-2; this is out of scope permanently, not deferred.

---

## 6. Functional Requirements

Priority key: **P0** = required for MVP, **P1** = should have, **P2** = future/nice-to-have.

### 6.1 T9 Matching Engine

**FR-1 (P0): Standard letter-to-digit mapping.**

| Digit | Letters |
|---|---|
| 1 | *(no letters — reserved / punctuation bucket)* |
| 2 | A B C |
| 3 | D E F |
| 4 | G H I |
| 5 | J K L |
| 6 | M N O |
| 7 | P Q R S |
| 8 | T U V |
| 9 | W X Y Z |
| 0 | *(space — see FR-4)* |

**FR-2 (P0): Digit-to-digit literal matching.** If a character in the app name is itself a digit (e.g. "1Password", "7-Zip", "9GAG"), it must match that literal digit key, not a letter-mapped one.

**FR-3 (P0): Progressive prefix matching.** For every installed app, compute the T9 digit sequence of its display name. An app is a candidate whenever the user's typed buffer is a **prefix** of that sequence (per the Obtainium example in §1.1). Matching is case-insensitive and must strip diacritics (e.g. "Café" behaves like "Cafe").

**FR-4 (P1): Word-initial matching mode.** In addition to full-name prefix matching, also compute the digit sequence of each app's word-initials (splitting on spaces, hyphens, and underscores). "Google Maps" matches `4-6`. `0` may optionally be typeable as an explicit word-boundary hint (classic T9 space key) to disambiguate, e.g. `4-0-6` forces "word starting with G, then word starting with M." **Decision: ships in MVP but off by default, toggleable via FR-19.** Rationale: full-name-only prefix matching is the simpler, more predictable default (fewer surprise matches while learning the app), with word-initial available to enable once a user wants it.

**FR-5 (P2): Substring/"anywhere" matching mode.** Match digit sequences occurring anywhere in the name, not just at the start or a word boundary. Off by default (higher false-positive rate); exposed as a settings toggle.

**FR-6 (P0): Ranking of multiple candidates**, in order:
1. Match type priority: full-name prefix > word-initial > substring.
2. Usage frequency through this launcher (most-launched-by-this-input-pattern first).
3. Recency of last launch.
4. Alphabetical, as final tiebreaker.

**FR-7 (P0): Live, incremental filtering.** Filtering must feel instantaneous (target: sub-16ms per keystroke against a device's full app list) — see NFR-1.

### 6.2 App Index

**FR-8 (P0): Index build & refresh.** Build the searchable index from `PackageManager` queries for all activities exposing `CATEGORY_LAUNCHER`, including apps that expose multiple launcher activities (aliases/shortcuts) and work-profile apps (via `CrossProfileApps` / `LauncherApps`). Rebuild incrementally on `ACTION_PACKAGE_ADDED`, `ACTION_PACKAGE_REMOVED`, `ACTION_PACKAGE_REPLACED`, and profile-availability broadcasts. Index build/refresh must run off the main thread.

**FR-9 (P1): Empty-buffer default state.** When no digits are typed, optionally show a short list of most-used or most-recent apps rather than a fully blank screen, configurable in settings (default: on, 4 items). **Decision: the list is not auto-announced by TalkBack on launcher open** — it renders silently and is only spoken when the user explicitly navigates focus into it (consistent with the "no spam" principle in A11Y-4; a blind user landing on the home screen shouldn't get an unsolicited announcement before they've done anything).

**FR-10 (P0): Buffer reset policy.** The digit buffer clears whenever the launcher is re-entered via the Home button after having been backgrounded (configurable: "always clear on Home" vs. "keep buffer for N seconds" — default: always clear).

### 6.3 Input & UI Layout

**FR-11 (P0): Standard phone-dialpad grid**, 4 rows × 3 columns:

```
[ 1 ]  [ 2 ABC ]  [ 3 DEF ]
[ 4 GHI ] [ 5 JKL ] [ 6 MNO ]
[ 7 PQRS ] [ 8 TUV ] [ 9 WXYZ ]
[ ⌫ Backspace ] [ 0 ] [ ⏎ Enter ]
```

This satisfies "Enter in the bottom right" directly and reuses the classic phone dialer's `*`/`#` positions for Backspace and Enter respectively, which keeps the layout familiar to anyone who has used a phone dialer or a TalkBack-accessible dialer before.

**FR-12 (P0): Digit display.** The currently typed buffer is shown as text above the match list (e.g. "628" for Obtainium so far), for sighted users' visual confirmation.

**FR-13 (P0): Match list.** A scrollable list beneath/above the buffer display, each item showing app icon + display name, with the top item visually distinguished (e.g. highlighted row) since it's what Enter will launch.

**FR-14 (P0): Backspace.** Single press deletes last digit. Long-press clears the entire buffer. Both must have distinct, correctly announced TalkBack behavior (see A11Y-7).

**FR-15 (P1): Alternate input.** Support physical keyboard number row / numpad input (common for foldables/tablets with keyboards, and for some accessibility hardware) mapped identically to the on-screen buttons.

### 6.4 Launch Behavior

**FR-16 (P0): Enter launches current top match.** If the buffer is empty or has no matches, Enter is a no-op (and disabled — see A11Y-6).

**FR-17 (P0): Direct list-item activation.** Tapping (or TalkBack-activating) any match list item launches that app directly, independent of Enter/top-match logic (§5.3).

**FR-18 (P0): Long-press on a match item** opens the standard Android app-info / uninstall / "App info" bottom sheet, matching normal launcher long-press conventions.

### 6.5 Settings

**FR-19 (P1): Matching mode toggle** (prefix only [default] / + word-initial / + substring, per §6.1).
**FR-20 (P1): Empty-state list** on/off and item count (FR-9).
**FR-21 (P2): Theming** — light/dark/system, background color/wallpaper passthrough.
**FR-22 (P2): Haptic & audio feedback** toggle per keypress (default: haptic on, matching standard Android dialer conventions).

---

## 7. Accessibility Requirements (TalkBack) — P0 unless noted

This is a first-class requirement, not a post-hoc audit item. Every requirement below is MVP-blocking.

**A11Y-1: Full TalkBack operability.** Every interactive element (12 keypad buttons, each match list item, any settings control) must be reachable and operable via TalkBack's standard focus/activate model (swipe to move focus, double-tap or double-tap-and-hold to activate), with no functionality that depends on precise multi-finger or visually-guided gestures TalkBack can't mediate.

**A11Y-2: Correct, non-redundant content descriptions.**
- Digit buttons announce as a standard phone dialpad would ("2, A B C" rather than just "2"), matching the mental model of existing accessible dialers.
- Backspace announces as "Backspace" (not "star").
- Enter announces as "Enter" plus dynamic target info (A11Y-5), not "pound."
- Match list items announce app name only (icons are decorative — `importantForAccessibility="no"` on the icon `ImageView`).

**A11Y-3: Logical, stable focus order.** Reading order is left-to-right, top-to-bottom through the keypad grid, then into the match list, consistently, with no order changes as state updates (avoid TalkBack focus jumping unexpectedly when the list re-filters — see A11Y-4).

**A11Y-4: Non-disruptive live updates.** As the match list re-filters on every keystroke, do **not** force TalkBack focus onto the list or interrupt the user's current reading position. Use a **polite** live region (`accessibilityLiveRegion="polite"`, announced only when TalkBack is idle) for a brief summary announcement (e.g. "3 matches, top: Discord") rather than announcing full list contents on every keypress. Avoid "TalkBack spam" — this is a known failure mode in accessible search UIs where every keystroke triggers a full re-announcement, making rapid typing unusable. Consider debouncing the announcement (e.g. only announce after ~300ms of no new input) so a fast typist isn't interrupted mid-sequence.

**A11Y-5: Enter key reflects its target.** The Enter button's accessible name must dynamically update to name what it will do, giving TalkBack users the same information a sighted user gets by glancing at the highlighted top row — e.g. "Enter, opens Discord" rather than a static "Enter." This is the single most important accessibility-specific requirement in this document: it's the non-visual equivalent of "seeing" the top match before committing to it.

**A11Y-6: Disabled/empty states are announced, not silent.** When there are no matches, Enter must be marked disabled (`enabled=false`) *and* have a label explaining why ("Enter, no matches to open") rather than silently doing nothing when double-tapped — a silent no-op is a common source of confusion for screen reader users, who have no visual cue that nothing happened.

**A11Y-7: Backspace vs. long-press-backspace are distinguishable.** Both the single-press and long-press behaviors need to be discoverable via TalkBack — e.g. expose the long-press "clear all" behavior as a custom accessibility action, not just a raw touch gesture, so it's available to users who navigate primarily via TalkBack's local context menu / linear navigation instead of raw touch gestures.

**A11Y-8: Touch target size & spacing.** Minimum 48×48dp per key (Android accessibility baseline), with adequate spacing to avoid accidental adjacent-key activation for users with motor-control considerations alongside vision-related ones.

**A11Y-9: Respect system accessibility settings.** Honor system font scale, "bold text," and high-contrast/dark theme settings for the digit display and match list text (benefits low-vision users who are not using TalkBack at all, or use it alongside magnification).

**A11Y-10 (P1): Braille display compatibility.** Verify behavior with a braille display driving TalkBack (e.g. via BrailleBack-style input) — specifically that button activation and the live-region announcements in A11Y-4 behave sanely when output is routed to a display rather than speech (no assumption that "announced" implies "spoken").

**A11Y-11 (P1): Switch Access compatibility.** As a secondary check (not the primary target of this PRD, but low-cost to verify given the grid layout is naturally scan-friendly): confirm the dialpad grid scans in a sane order under Switch Access.

**A11Y-12 (known limitation, P2/investigate): TalkBack "Touch typing" preference is not respected.** TalkBack's Settings → Typing → Typing preference offers a "touch typing" mode where, on the system keyboard, lifting a finger on a key activates it directly (no double-tap needed). Dialect's dialpad always requires the standard double-tap, regardless of this preference. Investigated 2026-09-01: the underlying platform mechanism (`AccessibilityNodeInfo.isTextEntryKey`/`setTextEntryKey`) exists but (a) is not exposed by Jetpack Compose in any current version (checked 1.11.3 and 1.12.0 directly against the compiled library — not a version gap), and (b) its own documentation describes it as inserting text into "the input field that has accessibility focus," implying it may require an actual editable text field as the target, which Dialect's buffer (a plain label, not an `EditText`) doesn't have. Unconfirmed whether this behavior is achievable for any non-IME app at all. Logged as a known limitation rather than blocking MVP; revisit if Compose adds support or if a concrete counter-example (another non-keyboard app achieving this) surfaces.

---

## 8. Non-Functional Requirements

**NFR-1: Performance.** Cold app-index build must complete within ~1s on a mid-range device for a typical 150–250 app install. Per-keystroke filter/re-rank must not visibly lag (<16ms target, definitely <100ms worst case) — this is directly an accessibility requirement too, since live-region announcement timing (A11Y-4) depends on filtering being fast and stable.

**NFR-2: Privacy.** No network permissions requested. No analytics/telemetry by default. Usage-frequency data (FR-6, FR-9) stored locally only, never transmitted. If any opt-in telemetry is added later, it must be off by default and clearly disclosed.

**NFR-3: Battery/resource use.** No background services beyond the package-change broadcast receiver (FR-8); no polling.

**NFR-4: Reliability as HOME app.** Must never crash-loop into a state where the device has no usable launcher (e.g. a fatal error on the home screen should fail into a minimal safe state, not a boot loop — this is a much higher bar for a launcher than for an ordinary app, since a crash here can strand the user with no way to reach any other app).

---

## 9. Android Platform / Technical Requirements

**T-1: Manifest declarations.** Main launcher activity must declare:
```xml
<intent-filter>
    <action android:name="android.intent.action.MAIN" />
    <category android:name="android.intent.category.HOME" />
    <category android:name="android.intent.category.DEFAULT" />
</intent-filter>
```

**T-2: Package visibility (Android 11+).** Enumerating all installed apps via `PackageManager` normally requires either explicit `<queries>` declarations or the `QUERY_ALL_PACKAGES` permission. Apps holding the system HOME role are generally covered by Android's automatic visibility exemption for the default launcher, but this should be explicitly verified against current Play Store policy and the target API level at implementation time, since platform policy in this area has changed before and may change again.

**T-3: Back button / task-stack behavior.** As HOME, must not put itself on the back stack in a way that lets "Back" from another app return into a confusing intermediate launcher state; pressing Back while the launcher itself is foregrounded should generally be a no-op (standard launcher convention) rather than exiting to a blank screen.

**T-4: Home button re-entry.** Repeated presses of the physical/gesture Home button while already on the launcher should reset the buffer (FR-10) — mirroring the common launcher convention where double-tapping Home returns to a default state.

**T-5: Work profile support (P1).** Apps in a work profile should appear in the index (badged appropriately) if the device has one configured, via `LauncherApps`/`CrossProfileApps`.

**T-6: Minimum/target SDK.**
- `minSdkVersion = 31` — **Android 12**, per the product decision to support Android 12 and higher. This is the actual device-compatibility floor and should not need to move unless the supported device range changes.
- `targetSdkVersion` — this is a *separate* axis from minSdk: it's what Google Play requires the build to be compiled/tested against in order to publish, and it ratchets upward roughly annually regardless of minSdk. As of this writing (Aug 2026), Play Console requires new apps/updates to target **Android 16 (API 36)** to publish, with existing published apps needing at least Android 15 (API 35) to stay visible to users on newer OS versions. This value will need bumping again on Google's next annual cycle (historically each ~August) independent of any change to minSdk — treat "raise targetSdk" as a recurring maintenance task, not a one-time setting.
- Because `minSdkVersion` (31) sits well below `targetSdkVersion` (36), features gated behind newer behavior changes (scoped storage enforcement, permission flows, exact-alarm restrictions, etc.) need explicit version-gated handling (`Build.VERSION.SDK_INT` checks) rather than assuming the newest behavior applies universally — the app will run across five Android major versions' worth of platform differences.

**T-7: RoleManager flow for "set as default launcher."** Use `RoleManager.createRequestRoleIntent(ROLE_HOME)` on API levels that support it, falling back to the standard "Home app" system settings redirect on older versions, so users (including TalkBack users) get the standard, accessible system role-picker UI rather than a custom one.

---

## 10. Edge Cases

| Case | Required behavior |
|---|---|
| No apps match typed digits | Show/announce "No matches"; disable Enter (A11Y-6). |
| App uninstalled while its match is highlighted | Remove from index immediately (FR-8); if it was the top match, re-rank and update Enter's label (A11Y-5) without waiting for next keypress. |
| Two apps have identical digit sequences and identical usage stats | Fall back to alphabetical (FR-6) — must be deterministic, not random, so repeated identical input always produces the same top match. |
| App name starts with an emoji/symbol | Strip leading non-alphanumeric characters for indexing purposes; don't let such apps become permanently unreachable by digit input. |
| User double-taps Enter rapidly (double-launch) | Debounce Enter so the app isn't launched twice. |
| Digit buffer grows longer than any real app's mapped name | Naturally yields zero matches (handled by the No-matches case above); no special-casing needed. |
| Work profile unavailable/paused | Exclude its apps from the index until available again, without erroring. |

---

## 11. Success Metrics (post-launch)

Given NFR-2 (no default telemetry), these should be evaluated via manual testing / opt-in user feedback rather than analytics:
- Median keypresses-to-launch for a representative app set.
- TalkBack task-completion time for "launch app X from a blank buffer" vs. stock launcher's search.
- Zero crash-to-no-launcher incidents in testing (NFR-4).

---

## 12. Open Questions

All open questions from the initial draft have been resolved (see §5.4, FR-4, FR-9, T-6).

Reopened during device verification (2026-09-01): see A11Y-12 — whether Dialect can ever respect TalkBack's "Touch typing" preference is unresolved and logged as a known limitation, not blocking MVP.

---

## 13. Phase 2+ Ideas (explicitly out of MVP scope)

- Substring/"anywhere" matching as default rather than opt-in (FR-5).
- Minimal widget support (e.g. clock only) behind the dialpad.
- Contact dialing integration (leaning into the "actual dialpad" metaphor — type digits, optionally dial a real phone number if no app matches and the buffer looks number-like).
- Custom per-digit quick-launch shortcuts (long-press digit 1 → always open a pinned app).
- Notification badge counts on match list items.

---

## 14. Appendix: Worked Examples

| Typed | Matches (example device) | Why |
|---|---|---|
| `3` | Discord, Facebook, Email... | All start with D/E/F |
| `3-4` | Discord (D-I...) | 4 = GHI, "i" is Discord's 2nd letter |
| `6` | Obtainium, Maps, NVDA Remote... | All start with M/N/O |
| `6-2-8-2` | Obtainium | O-B-T-A, per §1.1 |
| `4-6` (word-initial mode) | Google Maps | G, M — first letters of each word |
