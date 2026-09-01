# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Dialect is an Android home-screen launcher (HOME role) whose entire UI is a T9 phone dialpad: typing digits progressively filters/ranks installed apps by matching digit sequences against app names, and Enter launches the top match. Full requirements, worked examples, and functional/accessibility/platform requirements (FR-#, A11Y-#, T-#, NFR-#) live in [t9-dialpad-launcher-prd.md](t9-dialpad-launcher-prd.md) — read it before making product decisions; code comments reference these IDs (e.g. `// FR-9`, `// A11Y-5`).

Accessibility (TalkBack) is a first-class requirement, not a nice-to-have — see PRD §7. When touching UI code, check whether an A11Y-# requirement applies.

## Commands

```bash
./gradlew assembleDebug        # build debug APK
./gradlew test                 # run all JVM unit tests (matching engine — no device needed)
./gradlew test --tests "com.dialect.launcher.matching.MatchEngineTest"   # single test class
```

There is no emulator/AVD on this machine and no cmdline-tools/sdkmanager installed — all device testing is against a physical Android device over `adb`. Typical loop:

```bash
adb devices -l                                                  # confirm a device is connected
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.dialect.launcher/.MainActivity
adb logcat -d | grep -A 40 "FATAL EXCEPTION"                    # check for a crash after launching
adb shell uiautomator dump /sdcard/dump.xml && adb pull /sdcard/dump.xml   # inspect the live accessibility tree (content-desc, enabled, bounds) — far more reliable than estimating tap coordinates from a screenshot
adb shell cmd role add-role-holder android.app.role.HOME com.dialect.launcher   # set Dialect as the HOME role holder for testing, without the interactive picker
```

For multi-display devices (foldables), `adb shell screencap -p` needs an explicit `-d <displayId>` for the active display — find it via `adb shell dumpsys display | grep mDisplayId` (the one with `isActive=true`); the wrong display screenshots as solid black.

## Toolchain notes (non-obvious, verified against real build errors)

- AGP 9.2.0 with Gradle 9.4.1 (AGP 9.2 requires Gradle ≥9.4.1). AGP 9's built-in Kotlin support means **no `org.jetbrains.kotlin.android` plugin** is applied anywhere — only `com.android.application` + `org.jetbrains.kotlin.plugin.compose` (still required explicitly for the Compose compiler) + `com.google.devtools.ksp` (for Room).
- Compose BOM is pinned to `2026.06.00`, not the latest available — newer BOMs (`2026.08.00`+, resolving to `compose-ui 1.12.0`) require `compileSdk 37`, which isn't installed on this machine and conflicts with the PRD's fixed `compileSdk 36` (PRD T-6). Don't bump the BOM without checking this constraint.
- KSP's versioning scheme changed at Kotlin 2.3.x: no longer a combined `<kotlin>-<ksp>` tag, just a plain incrementing version (`2.3.11`, not `2.3.20-2.0.x`).
- `Modifier.weight()` inside `Row`/`Column` scopes resolves correctly *without* an explicit `import androidx.compose.foundation.layout.weight` in this Compose version — adding that import causes a "cannot access internal" compile error. Leave it unimported.
- Jetpack Compose does not expose `AccessibilityNodeInfo.isTextEntryKey` in any version up to `1.12.0` (verified by inspecting the compiled `ui-android` AAR directly) — this is why TalkBack's "Touch typing" preference isn't respected by the dialpad; see PRD A11Y-12.

## Architecture

Single `:app` module, MVVM, no DI framework — `AppContainer` (constructed in `DialectApplication.onCreate`) is a hand-rolled dependency container holding every repository; `HomeViewModel.Factory` reads from it. This is deliberate: the app is small enough that Hilt's KSP processor on top of Room's isn't worth the complexity.

**`matching/`** — the T9 engine is plain Kotlin with zero Android framework dependencies, specifically so it's unit-testable on the JVM without Robolectric. This is why `T9Nameable` (an interface exposing just `componentKey`/`displayName`/`fullPrefixDigits`/`wordInitialDigits`) exists separately from `appindex.AppIndexEntry` (which implements it but also carries `UserHandle`/`ImageBitmap` and other Android-only fields) — tests use a plain `TestApp` implementing `T9Nameable` instead of touching Android types. `MatchEngine.filterAndRank` is a pure, synchronous function; ranking logic (match-type → usage count → recency → alphabetical) lives in `RankingComparator`, generic over `T9Nameable`. Digit sequences are precomputed once per app name at index-build time (`T9Sequence`), never per keystroke.

**`appindex/`** — `AppIndexRepository` is the live source of truth (`StateFlow<List<AppIndexEntry>>`), built from `LauncherApps` (not raw `PackageManager.queryIntentActivities` — it's the API purpose-built for launchers: multiple launcher activities per package, profile-badged icons, cross-profile). Live updates come from `LauncherApps.registerCallback`; `PackageChangeReceiver` (manifest-declared) is a safety net for updates missed while the process was dead, not the primary update path. Package removal applies an immediate in-memory delta (not a full rescan) so an uninstalled top match re-ranks without waiting for a keystroke.

**`home/`** — `HomeViewModel.uiState` is a `combine()` of the digit buffer, the live app index, usage stats, and settings; anything changing (not just typing) re-derives the ranked list. `HomeUiState.enterContentDescription` and the debounced `liveRegionAnnouncement` (separate, ~300ms-debounced flow so visual state stays instant while the spoken summary doesn't spam on every keystroke) drive the accessibility-critical parts of `HomeScreen`.

**`crashsafety/`** — `SafeModeExceptionHandler` wraps `Thread.setDefaultUncaughtExceptionHandler`, recording crash count/timestamp to plain `SharedPreferences` (not Room/DataStore — must survive even if those are what's failing) using synchronous `commit()` (the process is about to die; `apply()` isn't safe here). `DialectApplication` checks this before deciding whether to construct the real `AppContainer` or leave it `null`, in which case `MainActivity` renders `SafeModeScreen` — a deliberately separate, minimal `PackageManager`-only screen with no dependency on the matching engine, Room, or DataStore.

**Accessibility semantics patterns used throughout `home/`**: `Modifier.clearAndSetSemantics { ... }` overrides a composable's default merged-children announcement (used for digit key labels like "2, A, B, C", and for the buffer display — critically, only set `contentDescription` when the buffer is non-empty, since the visible " " layout placeholder must not itself become the announced content). `Modifier.semantics { traversalIndex = ...; isTraversalGroup = true }` decouples TalkBack reading order from visual layout order. `combinedClickable`'s `onLongClickLabel` param (not manual semantics wiring) is what exposes Backspace's long-press as a discoverable custom accessibility action.
