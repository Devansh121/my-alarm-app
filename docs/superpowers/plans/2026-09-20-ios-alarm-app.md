# iOS Alarm App Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** An iOS-only alarm app in Kotlin (KMP + Compose Multiplatform) that replicates the native Clock app's Alarm tab, with hardware-button-proof ringing (Alarmy-style audio engine + lock-screen Live Activity dismiss/snooze) and a random-tone-per-ring default.

**Architecture:** All domain logic, persistence, and UI live in the Kotlin `shared` module (Compose Multiplatform). A thin Swift layer (`iosApp`) implements the Kotlin `AlarmEngine` interface: backup notification chains, looping `AVAudioSession` ringer with volume-reset, and ActivityKit Live Activity control. A `AlarmWidget` WidgetKit extension renders the SwiftUI Live Activity card whose buttons fire App Intents back into the app process.

**Tech Stack:** Kotlin 2.2.x, Compose Multiplatform 1.9.x, kotlinx-datetime, kotlinx-coroutines, SQLDelight 2.x (native driver), XcodeGen for the Xcode project, Swift 5.10 (AVFoundation, UserNotifications, ActivityKit, AppIntents, WidgetKit).

**Spec:** `docs/superpowers/specs/2026-09-20-ios-alarm-app-design.md`

---

## Executor rules (read first, follow exactly)

1. **Work tasks strictly in order.** Do not start a task until the previous task's "Done when" criterion is met.
2. **Never skip the run-the-failing-test step.** TDD steps are: write test → run and SEE it fail → implement → run and SEE it pass. If a test passes before you implement, stop and investigate — the test is wrong.
3. **Branch is `master`.** After every task's commit, run `git push` so CI (`.github/workflows/ci.yml`, macOS runner) validates it. Check the run with `gh run watch --exit-status` before moving on; a red CI run means fix before proceeding.
4. **Commit messages:** use the exact messages given in each task. **NEVER add "Co-Authored-By", "Claude-Session", or any attribution trailers** — plain messages only (also enforced by the user's global CLAUDE.md).
5. **Manual steps are real.** Steps marked "manual user step" or "on-device manual test" require the human. Report back and wait; never claim them done or fake their output.
6. **If an API doesn't resolve,** read the "Known risks & watch-items" section at the bottom before improvising. Check generated headers/docs; do not silently substitute different libraries or downgrade versions.
7. **Expected test output formats** (Kotlin/Native via Gradle):
   - FAIL at compile stage looks like: `e: file:///…/NextFireCalculatorTest.kt:12:9 Unresolved reference 'NextFireCalculator'` followed by `BUILD FAILED`.
   - FAIL at assertion stage looks like: `kotlin.AssertionError: Expected <…>, actual <…>` with the failing test name, then `BUILD FAILED` and a `Tests failed` summary.
   - PASS looks like: `BUILD SUCCESSFUL` (Gradle only prints per-test lines on failure; use `--tests` filter or add `testLogging` if you need to confirm a specific test executed).

### Done-when criteria (one line per task)

| Task | Done when |
|------|-----------|
| 1 | `xcodebuild -version` prints ≥16.x AND `xcrun devicectl list devices` shows the iPhone; `xcodegen` on PATH |
| 2 | `./gradlew :shared:compileKotlinIosSimulatorArm64` → BUILD SUCCESSFUL; committed & pushed; CI guard removed from ci.yml in the same commit; CI green |
| 3 | Simulator xcodebuild → BUILD SUCCEEDED; app runs on physical iPhone showing "shared module builds"; committed & pushed |
| 4 | `:shared:iosSimulatorArm64Test` green incl. both AlarmTest cases; committed & pushed; CI green |
| 5 | All 6 NextFireCalculatorTest cases pass; committed & pushed; CI green |
| 6 | All 4 ToneRandomizerTest cases pass; committed & pushed; CI green |
| 7 | All 4 AlarmRepositoryTest cases pass; committed & pushed; CI green |
| 8 | All 7 AlarmSchedulerTest cases pass; committed & pushed; CI green |
| 9 | `:shared:compileKotlinIosSimulatorArm64` BUILD SUCCESSFUL with AppCore wiring + temporary Swift NoopEngine; committed & pushed |
| 10 | Simulator build SUCCEEDED; alarm list renders with rows/toggles; committed & pushed |
| 11 | Manual simulator check passes: add/edit/toggle/delete alarm, wheel snaps, tone list shows Random default; committed & pushed |
| 12 | 8 .caf files exist and `afplay` plays one; Xcode project regenerated; committed & pushed |
| 13 | Simulator build SUCCEEDED with real engine wired (no NoopEngine remains); committed & pushed |
| 14 | Full M2 on-device checklist (Task 14 Step 3) confirmed by the user; committed & pushed |
| 15 | App target builds with intents + attributes files; bridge wired in AppDelegate; committed & pushed |
| 16 | Full M3 on-device checklist (Task 16 Step 7) confirmed by the user; committed & pushed |
| 17 | rescheduleAll test passes; committed & pushed; CI green |
| 18 | Both builds succeed; banner shows when notifications denied (simulator check); committed & pushed |
| 19 | All Kotlin tests green, full device regression confirmed by user, fidelity pass done; final commit pushed; CI green |

---

## File Structure (final state)

```
my-alarm-app/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradle/libs.versions.toml
├── shared/
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/
│       │   ├── kotlin/com/devansh/alarm/
│       │   │   ├── domain/Alarm.kt            # model + ToneSelection
│       │   │   ├── domain/Tone.kt             # tone catalog
│       │   │   ├── domain/NextFireCalculator.kt
│       │   │   ├── domain/ToneRandomizer.kt
│       │   │   ├── domain/AlarmScheduler.kt   # orchestration
│       │   │   ├── engine/AlarmEngine.kt      # interface implemented in Swift
│       │   │   ├── data/AlarmRepository.kt    # SQLDelight wrapper
│       │   │   ├── data/DriverFactory.kt      # expect
│       │   │   ├── AppCore.kt                 # singleton wiring, Swift entry points
│       │   │   └── ui/ (Theme.kt, App.kt, AlarmListScreen.kt,
│       │   │        EditAlarmSheet.kt, WheelPicker.kt, RingingScreen.kt)
│       │   └── sqldelight/com/devansh/alarm/db/Alarm.sq
│       ├── commonTest/kotlin/com/devansh/alarm/domain/  # unit tests
│       ├── iosMain/kotlin/com/devansh/alarm/
│       │   ├── MainViewController.kt
│       │   └── data/DriverFactory.ios.kt      # actual
│       └── iosTest/kotlin/com/devansh/alarm/data/AlarmRepositoryTest.kt
├── iosApp/
│   ├── project.yml                            # XcodeGen definition
│   ├── iosApp/
│   │   ├── iOSApp.swift                       # @main, wiring
│   │   ├── Info.plist
│   │   ├── AlarmEngineImpl.swift              # implements Kotlin AlarmEngine
│   │   ├── AudioRinger.swift                  # AVAudioSession loop player
│   │   ├── VolumeGuard.swift                  # volume-reset trick
│   │   ├── NotificationScheduler.swift        # backup chain + actions
│   │   ├── LiveActivityController.swift       # ActivityKit start/stop
│   │   ├── AlarmActivityAttributes.swift      # shared with widget target
│   │   ├── AlarmIntents.swift                 # shared with widget target
│   │   └── Tones/                             # 8 bundled .caf tones
│   └── AlarmWidget/
│       ├── AlarmWidgetBundle.swift
│       ├── AlarmActivityWidget.swift          # SwiftUI Live Activity card
│       └── Info.plist
└── scripts/make_tones.py                      # synthesizes placeholder tones
```

---

## Milestone 0 — Toolchain & Scaffold

### Task 1: Install Xcode (manual user step — cannot be done by an agent)

**This is a human step.** Verify before continuing; every later iOS task depends on it.

- [ ] **Step 1: User installs Xcode** from the Mac App Store (or `xcodes` CLI). After install, run:

```bash
sudo xcode-select -s /Applications/Xcode.app/Contents/Developer
sudo xcodebuild -license accept
xcodebuild -downloadPlatform iOS
```

- [ ] **Step 2: Verify**

Run: `xcodebuild -version && xcrun devicectl list devices`
Expected: Xcode version ≥ 16.x printed; the connected iPhone appears in the device list. If the iPhone shows "unpaired", unlock it and tap **Trust**.

- [ ] **Step 3: Install XcodeGen and JDK**

```bash
brew install xcodegen temurin@17 2>/dev/null || brew install xcodegen
java -version   # any JDK 17+ is fine
```

### Task 2: Gradle/KMP scaffold

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, `shared/build.gradle.kts`
- Create: `shared/src/commonMain/kotlin/com/devansh/alarm/Placeholder.kt`

- [ ] **Step 1: Write `gradle/libs.versions.toml`**

```toml
[versions]
kotlin = "2.2.20"
compose = "1.9.0"
coroutines = "1.10.2"
datetime = "0.7.1"
sqldelight = "2.1.0"

[libraries]
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }
kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "datetime" }
sqldelight-runtime = { module = "app.cash.sqldelight:runtime", version.ref = "sqldelight" }
sqldelight-native = { module = "app.cash.sqldelight:native-driver", version.ref = "sqldelight" }

[plugins]
kotlinMultiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
composeMultiplatform = { id = "org.jetbrains.compose", version.ref = "compose" }
composeCompiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
sqldelight = { id = "app.cash.sqldelight", version.ref = "sqldelight" }
```

(If a newer stable version fails to resolve, pin to the nearest stable listed on the plugin portal — do not downgrade Kotlin below 2.1.)

- [ ] **Step 2: Write `settings.gradle.kts`**

```kotlin
pluginManagement {
    repositories { google(); gradlePluginPortal(); mavenCentral() }
}
dependencyResolutionManagement {
    repositories { google(); mavenCentral() }
}
rootProject.name = "my-alarm-app"
include(":shared")
```

- [ ] **Step 3: Write root `build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.sqldelight) apply false
}
```

- [ ] **Step 4: Write `gradle.properties`**

```properties
kotlin.native.cacheKind=none
org.gradle.jvmargs=-Xmx4g
org.gradle.caching=true
```

- [ ] **Step 5: Write `shared/build.gradle.kts`**

```kotlin
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.sqldelight)
}

kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target: KotlinNativeTarget ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.sqldelight.runtime)
        }
        commonTest.dependencies { implementation(kotlin("test")) }
        iosMain.dependencies { implementation(libs.sqldelight.native) }
    }
}

sqldelight {
    databases {
        create("AlarmDb") { packageName.set("com.devansh.alarm.db") }
    }
}
```

- [ ] **Step 6: Write placeholder source** `shared/src/commonMain/kotlin/com/devansh/alarm/Placeholder.kt`

```kotlin
package com.devansh.alarm

fun placeholder(): String = "shared module builds"
```

- [ ] **Step 7: Add Gradle wrapper and build**

```bash
gradle wrapper --gradle-version 8.14 2>/dev/null || brew install gradle && gradle wrapper --gradle-version 8.14
./gradlew :shared:compileKotlinIosSimulatorArm64
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Un-guard CI** — `.github/workflows/ci.yml` already exists with a scaffold guard. Now that `settings.gradle.kts` exists, replace the guarded "Run shared-module tests" step with the direct command so CI hard-fails on test failures:

```yaml
      - name: Run shared-module tests
        run: ./gradlew :shared:iosSimulatorArm64Test --no-daemon
```

- [ ] **Step 9: Commit, push, verify CI**

```bash
git add -A && git commit -m "chore: KMP scaffold, shared module builds for iOS"
git push && gh run watch --exit-status
```

Expected: CI run completes green on the macos-14 runner.

### Task 3: Xcode project via XcodeGen + blank app on device

**Files:**
- Create: `iosApp/project.yml`, `iosApp/iosApp/iOSApp.swift`, `iosApp/iosApp/Info.plist`
- Create: `shared/src/iosMain/kotlin/com/devansh/alarm/MainViewController.kt`

- [ ] **Step 1: Write `MainViewController.kt`** (temporary hello screen; real App comes in Task 9)

```kotlin
package com.devansh.alarm

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(placeholder())
    }
}
```

- [ ] **Step 2: Write `iosApp/project.yml`**

```yaml
name: iosApp
options:
  bundleIdPrefix: com.devansh
  deploymentTarget:
    iOS: "17.0"
settings:
  base:
    SWIFT_VERSION: "5.10"
targets:
  iosApp:
    type: application
    platform: iOS
    sources:
      - path: iosApp
    settings:
      base:
        PRODUCT_BUNDLE_IDENTIFIER: com.devansh.myalarm
        INFOPLIST_FILE: iosApp/Info.plist
        FRAMEWORK_SEARCH_PATHS: "$(SRCROOT)/../shared/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)"
        OTHER_LDFLAGS: "$(inherited) -framework Shared"
        ENABLE_USER_SCRIPT_SANDBOXING: NO
    preBuildScripts:
      - name: Build Kotlin framework
        script: |
          cd "$SRCROOT/.."
          ./gradlew :shared:embedAndSignAppleFrameworkForXcode
        basedOnDependencyAnalysis: false
```

- [ ] **Step 3: Write `iosApp/iosApp/iOSApp.swift`**

```swift
import SwiftUI
import Shared

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ComposeView().ignoresSafeArea(.all)
        }
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
```

- [ ] **Step 4: Write `iosApp/iosApp/Info.plist`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleDisplayName</key><string>My Alarm</string>
    <key>CFBundleIdentifier</key><string>$(PRODUCT_BUNDLE_IDENTIFIER)</string>
    <key>CFBundleShortVersionString</key><string>1.0</string>
    <key>CFBundleVersion</key><string>1</string>
    <key>UILaunchScreen</key><dict/>
    <key>UIBackgroundModes</key>
    <array><string>audio</string></array>
    <key>NSSupportsLiveActivities</key><true/>
</dict>
</plist>
```

- [ ] **Step 5: Generate project, build for simulator**

```bash
cd iosApp && xcodegen generate && cd ..
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 16' build
```

Expected: `BUILD SUCCEEDED`. (Simulator smoke test; device signing next step.)

- [ ] **Step 6: Device signing (manual user step)** — open `iosApp/iosApp.xcodeproj` in Xcode once, select the iosApp target → Signing & Capabilities → check *Automatically manage signing*, pick your personal team (add Apple ID under Xcode ▸ Settings ▸ Accounts if empty). Then:

```bash
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -destination 'platform=iOS,id=00008140-001E08283AE3001C' \
  -allowProvisioningUpdates build
```

Expected: `BUILD SUCCEEDED`. Run from Xcode once to install; on the phone enable Settings ▸ General ▸ VPN & Device Management ▸ trust your developer profile. App launches showing "shared module builds".

- [ ] **Step 7: Commit**

```bash
git add -A && git commit -m "feat: blank Compose app runs on iPhone via XcodeGen project"
```

---

## Milestone 1 — Domain logic (TDD) + persistence

### Task 4: Alarm model + Tone catalog

**Files:**
- Create: `shared/src/commonMain/kotlin/com/devansh/alarm/domain/Alarm.kt`
- Create: `shared/src/commonMain/kotlin/com/devansh/alarm/domain/Tone.kt`
- Test: `shared/src/commonTest/kotlin/com/devansh/alarm/domain/AlarmTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.devansh.alarm.domain

import kotlinx.datetime.DayOfWeek
import kotlin.test.Test
import kotlin.test.assertEquals

class AlarmTest {
    @Test fun defaultsMatchSpec() {
        val a = Alarm(id = "a1", hour = 7, minute = 30)
        assertEquals(emptySet<DayOfWeek>(), a.repeatDays)
        assertEquals("Alarm", a.label)
        assertEquals(ToneSelection.Random, a.tone)
        assertEquals(true, a.snoozeEnabled)
        assertEquals(9, a.snoozeMinutes)
        assertEquals(true, a.enabled)
    }
    @Test fun repeatDaysRoundTripsThroughCsv() {
        val days = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)
        assertEquals(days, repeatDaysFromCsv(repeatDaysToCsv(days)))
        assertEquals(emptySet<DayOfWeek>(), repeatDaysFromCsv(""))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :shared:iosSimulatorArm64Test --tests "com.devansh.alarm.domain.AlarmTest"`
Expected: FAIL (unresolved reference `Alarm`).

- [ ] **Step 3: Write `Alarm.kt`**

```kotlin
package com.devansh.alarm.domain

import kotlinx.datetime.DayOfWeek

sealed interface ToneSelection {
    data object Random : ToneSelection
    data class Pinned(val toneId: String) : ToneSelection
}

data class Alarm(
    val id: String,
    val hour: Int,                 // 0..23
    val minute: Int,               // 0..59
    val repeatDays: Set<DayOfWeek> = emptySet(),  // empty = one-shot
    val label: String = "Alarm",
    val tone: ToneSelection = ToneSelection.Random,
    val snoozeEnabled: Boolean = true,
    val snoozeMinutes: Int = 9,    // 1..30
    val enabled: Boolean = true,
) {
    init {
        require(hour in 0..23 && minute in 0..59)
        require(snoozeMinutes in 1..30)
    }
}

fun repeatDaysToCsv(days: Set<DayOfWeek>): String =
    days.sortedBy { it.isoDayNumber }.joinToString(",") { it.isoDayNumber.toString() }

fun repeatDaysFromCsv(csv: String): Set<DayOfWeek> =
    if (csv.isBlank()) emptySet()
    else csv.split(",").map { DayOfWeek(it.toInt()) }.toSet()
```

(`isoDayNumber`/`DayOfWeek(Int)` come from kotlinx-datetime.)

- [ ] **Step 4: Write `Tone.kt`**

```kotlin
package com.devansh.alarm.domain

data class Tone(val id: String, val displayName: String, val fileName: String)

// fileName must match a .caf bundled in the iOS app (Task 12).
val BUNDLED_TONES: List<Tone> = listOf(
    Tone("radial", "Radial", "tone_radial.caf"),
    Tone("ascent", "Ascent", "tone_ascent.caf"),
    Tone("pulse", "Pulse", "tone_pulse.caf"),
    Tone("chimes", "Chimes", "tone_chimes.caf"),
    Tone("cosmic", "Cosmic", "tone_cosmic.caf"),
    Tone("beacon", "Beacon", "tone_beacon.caf"),
    Tone("signal", "Signal", "tone_signal.caf"),
    Tone("waves", "Waves", "tone_waves.caf"),
)
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew :shared:iosSimulatorArm64Test`
Expected: PASS.

- [ ] **Step 6: Commit** — `git add -A && git commit -m "feat: Alarm domain model and tone catalog"`

### Task 5: NextFireCalculator

**Files:**
- Create: `shared/src/commonMain/kotlin/com/devansh/alarm/domain/NextFireCalculator.kt`
- Test: `shared/src/commonTest/kotlin/com/devansh/alarm/domain/NextFireCalculatorTest.kt`

- [ ] **Step 1: Write the failing tests**

```kotlin
package com.devansh.alarm.domain

import kotlinx.datetime.*
import kotlin.test.Test
import kotlin.test.assertEquals

class NextFireCalculatorTest {
    private val zone = TimeZone.of("Europe/Berlin")
    private val calc = NextFireCalculator(zone)
    private fun at(y: Int, mo: Int, d: Int, h: Int, mi: Int) =
        LocalDateTime(y, mo, d, h, mi).toInstant(zone)

    @Test fun oneShotLaterToday() {
        val now = at(2026, 9, 21, 6, 0) // Monday 06:00
        val a = Alarm("a", 7, 30)
        assertEquals(at(2026, 9, 21, 7, 30), calc.nextFire(a, now))
    }
    @Test fun oneShotTimePassedGoesTomorrow() {
        val now = at(2026, 9, 21, 8, 0)
        val a = Alarm("a", 7, 30)
        assertEquals(at(2026, 9, 22, 7, 30), calc.nextFire(a, now))
    }
    @Test fun exactlyNowGoesToNextOccurrence() {
        val now = at(2026, 9, 21, 7, 30)
        val a = Alarm("a", 7, 30)
        assertEquals(at(2026, 9, 22, 7, 30), calc.nextFire(a, now))
    }
    @Test fun repeatingSkipsToChosenDay() {
        val now = at(2026, 9, 21, 8, 0) // Monday, past 07:30
        val a = Alarm("a", 7, 30, repeatDays = setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY))
        assertEquals(at(2026, 9, 24, 7, 30), calc.nextFire(a, now)) // Thursday
    }
    @Test fun repeatingWrapsToNextWeek() {
        val now = at(2026, 9, 21, 8, 0) // Monday, past 07:30
        val a = Alarm("a", 7, 30, repeatDays = setOf(DayOfWeek.MONDAY))
        assertEquals(at(2026, 9, 28, 7, 30), calc.nextFire(a, now))
    }
    @Test fun springForwardGapResolvesForward() {
        // Berlin 2026: DST starts Mar 29, 02:00 -> 03:00. 02:30 does not exist.
        val now = at(2026, 3, 28, 12, 0)
        val a = Alarm("a", 2, 30)
        val fire = calc.nextFire(a, now).toLocalDateTime(zone)
        assertEquals(LocalDate(2026, 3, 29), fire.date)
        assertEquals(3, fire.hour) // kotlinx-datetime shifts gap times forward
    }
}
```

- [ ] **Step 2: Run to verify FAIL** — `./gradlew :shared:iosSimulatorArm64Test` → unresolved `NextFireCalculator`.

- [ ] **Step 3: Implement**

```kotlin
package com.devansh.alarm.domain

import kotlinx.datetime.*

class NextFireCalculator(private val zone: TimeZone) {
    /** First instant strictly after [now] matching the alarm's time (and days, if repeating). */
    fun nextFire(alarm: Alarm, now: Instant): Instant {
        var date = now.toLocalDateTime(zone).date
        repeat(8) {
            val dayOk = alarm.repeatDays.isEmpty() || date.dayOfWeek in alarm.repeatDays
            if (dayOk) {
                val candidate = LocalDateTime(date, LocalTime(alarm.hour, alarm.minute)).toInstant(zone)
                if (candidate > now) return candidate
            }
            date = date.plus(1, DateTimeUnit.DAY)
        }
        error("no fire date within 8 days — impossible for valid input")
    }
}
```

- [ ] **Step 4: Run to verify PASS** — `./gradlew :shared:iosSimulatorArm64Test`
- [ ] **Step 5: Commit** — `git commit -am "feat: NextFireCalculator with DST and repeat handling"`

### Task 6: ToneRandomizer

**Files:**
- Create: `shared/src/commonMain/kotlin/com/devansh/alarm/domain/ToneRandomizer.kt`
- Test: `shared/src/commonTest/kotlin/com/devansh/alarm/domain/ToneRandomizerTest.kt`

- [ ] **Step 1: Write the failing tests**

```kotlin
package com.devansh.alarm.domain

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ToneRandomizerTest {
    private val tones = BUNDLED_TONES

    @Test fun neverRepeatsPreviousTone() {
        val r = ToneRandomizer(Random(42))
        var last: String? = null
        repeat(200) {
            val pick = r.pick(tones, lastToneId = last)
            assertTrue(pick.id != last, "picked same tone twice in a row")
            last = pick.id
        }
    }
    @Test fun pinnedSelectionBypassesRandomizer() {
        val r = ToneRandomizer(Random(1))
        val pick = r.resolve(ToneSelection.Pinned("pulse"), tones, lastToneId = "pulse")
        assertEquals("pulse", pick.id) // pinned may repeat; no-repeat only applies to Random
    }
    @Test fun singleToneListDegeneratesGracefully() {
        val r = ToneRandomizer(Random(1))
        val only = listOf(tones.first())
        assertEquals(tones.first().id, r.pick(only, lastToneId = tones.first().id).id)
    }
    @Test fun unknownPinnedIdFallsBackToRandom() {
        val r = ToneRandomizer(Random(1))
        val pick = r.resolve(ToneSelection.Pinned("nope"), tones, lastToneId = null)
        assertTrue(tones.any { it.id == pick.id })
    }
}
```

- [ ] **Step 2: Run to verify FAIL.**

- [ ] **Step 3: Implement**

```kotlin
package com.devansh.alarm.domain

import kotlin.random.Random

class ToneRandomizer(private val random: Random = Random.Default) {
    fun pick(tones: List<Tone>, lastToneId: String?): Tone {
        require(tones.isNotEmpty())
        val candidates = tones.filter { it.id != lastToneId }.ifEmpty { tones }
        return candidates[random.nextInt(candidates.size)]
    }
    fun resolve(selection: ToneSelection, tones: List<Tone>, lastToneId: String?): Tone =
        when (selection) {
            is ToneSelection.Pinned -> tones.firstOrNull { it.id == selection.toneId }
                ?: pick(tones, lastToneId)
            ToneSelection.Random -> pick(tones, lastToneId)
        }
}
```

- [ ] **Step 4: Run to verify PASS.**
- [ ] **Step 5: Commit** — `git commit -am "feat: ToneRandomizer with no-immediate-repeat rule"`

### Task 7: SQLDelight persistence

**Files:**
- Create: `shared/src/commonMain/sqldelight/com/devansh/alarm/db/Alarm.sq`
- Create: `shared/src/commonMain/kotlin/com/devansh/alarm/data/AlarmRepository.kt`
- Create: `shared/src/commonMain/kotlin/com/devansh/alarm/data/DriverFactory.kt` (expect)
- Create: `shared/src/iosMain/kotlin/com/devansh/alarm/data/DriverFactory.ios.kt` (actual)
- Test: `shared/src/iosTest/kotlin/com/devansh/alarm/data/AlarmRepositoryTest.kt`

- [ ] **Step 1: Write `Alarm.sq`**

```sql
CREATE TABLE alarmRow (
  id TEXT NOT NULL PRIMARY KEY,
  hour INTEGER NOT NULL,
  minute INTEGER NOT NULL,
  repeatDays TEXT NOT NULL,
  label TEXT NOT NULL,
  toneId TEXT,
  snoozeEnabled INTEGER NOT NULL,
  snoozeMinutes INTEGER NOT NULL,
  enabled INTEGER NOT NULL,
  lastPlayedToneId TEXT
);

selectAll:
SELECT * FROM alarmRow ORDER BY hour, minute;

upsert:
INSERT OR REPLACE INTO alarmRow(id, hour, minute, repeatDays, label, toneId,
  snoozeEnabled, snoozeMinutes, enabled, lastPlayedToneId)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);

deleteById:
DELETE FROM alarmRow WHERE id = ?;

setEnabled:
UPDATE alarmRow SET enabled = ? WHERE id = ?;

setLastPlayedTone:
UPDATE alarmRow SET lastPlayedToneId = ? WHERE id = ?;
```

- [ ] **Step 2: Write `DriverFactory.kt` (expect) and iOS actual**

```kotlin
// commonMain .../data/DriverFactory.kt
package com.devansh.alarm.data

import app.cash.sqldelight.db.SqlDriver

expect class DriverFactory() {
    fun createDriver(name: String = "alarms.db"): SqlDriver
}
```

```kotlin
// iosMain .../data/DriverFactory.ios.kt
package com.devansh.alarm.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.devansh.alarm.db.AlarmDb

actual class DriverFactory actual constructor() {
    actual fun createDriver(name: String): SqlDriver =
        NativeSqliteDriver(AlarmDb.Schema, name)
}
```

- [ ] **Step 3: Write the failing repository test** (in `iosTest`, uses an in-memory-ish throwaway db name)

```kotlin
package com.devansh.alarm.data

import com.devansh.alarm.domain.*
import kotlinx.datetime.DayOfWeek
import kotlin.random.Random
import kotlin.test.*

class AlarmRepositoryTest {
    private fun freshRepo() =
        AlarmRepository(DriverFactory().createDriver("test-${Random.nextInt()}.db"))

    @Test fun saveAndLoadRoundTrip() {
        val repo = freshRepo()
        val alarm = Alarm("a1", 6, 45,
            repeatDays = setOf(DayOfWeek.SATURDAY),
            label = "Gym", tone = ToneSelection.Pinned("pulse"),
            snoozeEnabled = false, snoozeMinutes = 5, enabled = false)
        repo.save(alarm)
        assertEquals(listOf(alarm), repo.all())
    }
    @Test fun randomToneStoredAsNull() {
        val repo = freshRepo()
        repo.save(Alarm("a2", 7, 0))
        assertEquals(ToneSelection.Random, repo.all().single().tone)
    }
    @Test fun deleteAndToggle() {
        val repo = freshRepo()
        repo.save(Alarm("a3", 7, 0))
        repo.setEnabled("a3", false)
        assertFalse(repo.all().single().enabled)
        repo.delete("a3")
        assertTrue(repo.all().isEmpty())
    }
    @Test fun lastPlayedTonePersists() {
        val repo = freshRepo()
        repo.save(Alarm("a4", 7, 0))
        repo.setLastPlayedTone("a4", "waves")
        assertEquals("waves", repo.lastPlayedTone("a4"))
    }
}
```

- [ ] **Step 4: Run to verify FAIL** — `./gradlew :shared:iosSimulatorArm64Test`

- [ ] **Step 5: Implement `AlarmRepository.kt`**

```kotlin
package com.devansh.alarm.data

import app.cash.sqldelight.db.SqlDriver
import com.devansh.alarm.db.AlarmDb
import com.devansh.alarm.db.AlarmRow
import com.devansh.alarm.domain.*

class AlarmRepository(driver: SqlDriver) {
    private val db = AlarmDb(driver)
    private val q = db.alarmQueries

    fun all(): List<Alarm> = q.selectAll().executeAsList().map { it.toDomain() }

    fun save(alarm: Alarm) = q.upsert(
        id = alarm.id, hour = alarm.hour.toLong(), minute = alarm.minute.toLong(),
        repeatDays = repeatDaysToCsv(alarm.repeatDays), label = alarm.label,
        toneId = (alarm.tone as? ToneSelection.Pinned)?.toneId,
        snoozeEnabled = if (alarm.snoozeEnabled) 1 else 0,
        snoozeMinutes = alarm.snoozeMinutes.toLong(),
        enabled = if (alarm.enabled) 1 else 0,
        lastPlayedToneId = lastPlayedTone(alarm.id),
    )

    fun delete(id: String) = q.deleteById(id)
    fun setEnabled(id: String, enabled: Boolean) = q.setEnabled(if (enabled) 1L else 0L, id)
    fun setLastPlayedTone(id: String, toneId: String) = q.setLastPlayedTone(toneId, id)
    fun lastPlayedTone(id: String): String? =
        q.selectAll().executeAsList().firstOrNull { it.id == id }?.lastPlayedToneId

    private fun AlarmRow.toDomain() = Alarm(
        id = id, hour = hour.toInt(), minute = minute.toInt(),
        repeatDays = repeatDaysFromCsv(repeatDays), label = label,
        tone = toneId?.let { ToneSelection.Pinned(it) } ?: ToneSelection.Random,
        snoozeEnabled = snoozeEnabled == 1L, snoozeMinutes = snoozeMinutes.toInt(),
        enabled = enabled == 1L,
    )
}
```

Note: if the generated row class name differs (check `shared/build/generated/sqldelight`), adjust the import — SQLDelight names it after the table (`AlarmRow`).

- [ ] **Step 6: Run to verify PASS.**
- [ ] **Step 7: Commit** — `git commit -am "feat: SQLDelight persistence with repository round-trip tests"`

### Task 8: AlarmEngine interface + AlarmScheduler

**Files:**
- Create: `shared/src/commonMain/kotlin/com/devansh/alarm/engine/AlarmEngine.kt`
- Create: `shared/src/commonMain/kotlin/com/devansh/alarm/domain/AlarmScheduler.kt`
- Test: `shared/src/iosTest/kotlin/com/devansh/alarm/domain/AlarmSchedulerTest.kt` (iosTest, NOT commonTest — it needs the iOS `DriverFactory` actual)

- [ ] **Step 1: Write `AlarmEngine.kt`** (the Kotlin↔Swift boundary — exported as an ObjC protocol)

```kotlin
package com.devansh.alarm.engine

/** Implemented in Swift (AlarmEngineImpl). All times are epoch milliseconds UTC. */
interface AlarmEngine {
    fun schedule(request: ScheduleRequest)
    fun cancel(alarmId: String)
    fun stopRinging(alarmId: String)
}

data class ScheduleRequest(
    val alarmId: String,
    val fireAtEpochMillis: Long,
    val toneFileName: String,
    val toneDisplayName: String,
    val label: String,
    val snoozeMinutes: Int,     // 0 = snooze disabled
)
```

- [ ] **Step 2: Write the failing tests** (fake engine records calls)

```kotlin
package com.devansh.alarm.domain

import com.devansh.alarm.data.*
import com.devansh.alarm.engine.*
import kotlinx.datetime.*
import kotlin.random.Random
import kotlin.test.*

private class FakeEngine : AlarmEngine {
    val scheduled = mutableListOf<ScheduleRequest>()
    val cancelled = mutableListOf<String>()
    val stopped = mutableListOf<String>()
    override fun schedule(request: ScheduleRequest) { scheduled += request }
    override fun cancel(alarmId: String) { cancelled += alarmId }
    override fun stopRinging(alarmId: String) { stopped += alarmId }
}

private class FixedClock(var now: Instant) : Clock { override fun now() = now }

class AlarmSchedulerTest {
    private val zone = TimeZone.of("Europe/Berlin")
    private val engine = FakeEngine()
    private val repo = AlarmRepository(
        com.devansh.alarm.data.DriverFactory().createDriver("sched-${Random.nextInt()}.db"))
    private val clock = FixedClock(LocalDateTime(2026, 9, 21, 6, 0).toInstant(zone))
    private val scheduler = AlarmScheduler(repo, engine, NextFireCalculator(zone),
        ToneRandomizer(Random(7)), clock)

    @Test fun savingEnabledAlarmSchedulesIt() {
        scheduler.saveAlarm(Alarm("a", 7, 30))
        val req = engine.scheduled.single()
        assertEquals("a", req.alarmId)
        assertEquals(LocalDateTime(2026, 9, 21, 7, 30).toInstant(zone).toEpochMilliseconds(),
            req.fireAtEpochMillis)
        assertEquals(9, req.snoozeMinutes)
    }
    @Test fun savingDisabledAlarmCancelsIt() {
        scheduler.saveAlarm(Alarm("a", 7, 30, enabled = false))
        assertTrue(engine.scheduled.isEmpty())
        assertEquals(listOf("a"), engine.cancelled)
    }
    @Test fun randomToneChangesBetweenRings() {
        scheduler.saveAlarm(Alarm("a", 7, 30))
        val first = engine.scheduled.last().toneFileName
        scheduler.onDismissed("a")   // one-shot: dismiss disables; re-enable to ring again
        scheduler.saveAlarm(repo.all().single().copy(enabled = true))
        val second = engine.scheduled.last().toneFileName
        assertTrue(first != second, "consecutive rings must differ in tone")
    }
    @Test fun dismissOneShotDisablesIt() {
        scheduler.saveAlarm(Alarm("a", 7, 30))
        scheduler.onDismissed("a")
        assertFalse(repo.all().single().enabled)
        assertEquals(listOf("a"), engine.stopped)
    }
    @Test fun dismissRepeatingReschedules() {
        scheduler.saveAlarm(Alarm("a", 7, 30, repeatDays = setOf(DayOfWeek.MONDAY)))
        clock.now = LocalDateTime(2026, 9, 21, 7, 30).toInstant(zone)
        scheduler.onDismissed("a")
        assertTrue(repo.all().single().enabled)
        assertEquals(LocalDateTime(2026, 9, 28, 7, 30).toInstant(zone).toEpochMilliseconds(),
            engine.scheduled.last().fireAtEpochMillis)
    }
    @Test fun snoozeSchedulesNowPlusDuration() {
        scheduler.saveAlarm(Alarm("a", 7, 30, snoozeMinutes = 5))
        clock.now = LocalDateTime(2026, 9, 21, 7, 30).toInstant(zone)
        scheduler.onSnoozed("a")
        assertEquals(clock.now.plus(5, DateTimeUnit.MINUTE).toEpochMilliseconds(),
            engine.scheduled.last().fireAtEpochMillis)
    }
    @Test fun deleteCancels() {
        scheduler.saveAlarm(Alarm("a", 7, 30))
        scheduler.deleteAlarm("a")
        assertTrue(repo.all().isEmpty())
        assertTrue("a" in engine.cancelled)
    }
}
```

Note: this test file lives in `iosTest` (keeping package `com.devansh.alarm.domain` as declared in the code above) because it needs the iOS `DriverFactory` actual. Run with `:shared:iosSimulatorArm64Test`.

- [ ] **Step 3: Run to verify FAIL.**

- [ ] **Step 4: Implement `AlarmScheduler.kt`**

```kotlin
package com.devansh.alarm.domain

import com.devansh.alarm.data.AlarmRepository
import com.devansh.alarm.engine.*
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus

class AlarmScheduler(
    private val repo: AlarmRepository,
    private val engine: AlarmEngine,
    private val calc: NextFireCalculator,
    private val randomizer: ToneRandomizer,
    private val clock: Clock = Clock.System,
) {
    fun saveAlarm(alarm: Alarm) {
        repo.save(alarm)
        if (alarm.enabled) scheduleNext(alarm) else engine.cancel(alarm.id)
    }

    fun deleteAlarm(id: String) { repo.delete(id); engine.cancel(id) }

    fun setEnabled(id: String, enabled: Boolean) {
        repo.setEnabled(id, enabled)
        val alarm = repo.all().firstOrNull { it.id == id } ?: return
        if (enabled) scheduleNext(alarm) else engine.cancel(id)
    }

    fun onDismissed(id: String) {
        engine.stopRinging(id)
        val alarm = repo.all().firstOrNull { it.id == id } ?: return
        if (alarm.repeatDays.isEmpty()) {
            repo.setEnabled(id, false)
            engine.cancel(id)
        } else scheduleNext(alarm)
    }

    fun onSnoozed(id: String) {
        engine.stopRinging(id)
        val alarm = repo.all().firstOrNull { it.id == id } ?: return
        val fireAt = clock.now().plus(alarm.snoozeMinutes, DateTimeUnit.MINUTE)
        scheduleAt(alarm, fireAt.toEpochMilliseconds())
    }

    /** Call on app launch: re-arm everything (engine state dies with the process). */
    fun rescheduleAll() = repo.all().filter { it.enabled }.forEach { scheduleNext(it) }

    private fun scheduleNext(alarm: Alarm) =
        scheduleAt(alarm, calc.nextFire(alarm, clock.now()).toEpochMilliseconds())

    private fun scheduleAt(alarm: Alarm, fireAtEpochMillis: Long) {
        val tone = randomizer.resolve(alarm.tone, BUNDLED_TONES, repo.lastPlayedTone(alarm.id))
        repo.setLastPlayedTone(alarm.id, tone.id)
        engine.schedule(ScheduleRequest(
            alarmId = alarm.id, fireAtEpochMillis = fireAtEpochMillis,
            toneFileName = tone.fileName, toneDisplayName = tone.displayName,
            label = alarm.label,
            snoozeMinutes = if (alarm.snoozeEnabled) alarm.snoozeMinutes else 0,
        ))
    }
}
```

- [ ] **Step 5: Run to verify PASS** — `./gradlew :shared:iosSimulatorArm64Test`
- [ ] **Step 6: Commit** — `git commit -am "feat: AlarmScheduler orchestration with engine interface"`

---

## Milestone 1 (cont.) — Compose UI

### Task 9: Theme + App shell + AppCore wiring

**Files:**
- Create: `shared/src/commonMain/kotlin/com/devansh/alarm/ui/Theme.kt`
- Create: `shared/src/commonMain/kotlin/com/devansh/alarm/AppCore.kt`
- Create: `shared/src/commonMain/kotlin/com/devansh/alarm/ui/App.kt`
- Modify: `shared/src/iosMain/kotlin/com/devansh/alarm/MainViewController.kt`
- Delete: `shared/src/commonMain/kotlin/com/devansh/alarm/Placeholder.kt`

UI code is verified by building + running on simulator/device (no automated UI tests in this plan; domain logic is where the tests are).

- [ ] **Step 1: Write `Theme.kt`** — iOS Clock palette

```kotlin
package com.devansh.alarm.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ClockOrange = Color(0xFFFF9F0A)   // iOS system orange (dark)
val ClockBackground = Color(0xFF000000)
val ClockSurface = Color(0xFF1C1C1E)  // iOS secondarySystemBackground (dark)
val ClockSeparator = Color(0xFF38383A)
val ClockTextSecondary = Color(0xFF8E8E93)

@Composable
fun ClockTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = ClockOrange,
            background = ClockBackground,
            surface = ClockSurface,
            onBackground = Color.White,
            onSurface = Color.White,
        ),
        content = content,
    )
}
```

- [ ] **Step 2: Write `AppCore.kt`** — singleton wiring + Swift-facing entry points

```kotlin
package com.devansh.alarm

import com.devansh.alarm.data.AlarmRepository
import com.devansh.alarm.data.DriverFactory
import com.devansh.alarm.domain.*
import com.devansh.alarm.engine.AlarmEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.TimeZone

/** Process-wide state. Swift calls initialize() once at launch, then the
 *  dismiss/snooze entry points from notification actions and App Intents. */
object AppCore {
    lateinit var scheduler: AlarmScheduler
        private set
    lateinit var repo: AlarmRepository
        private set
    /** alarmId currently ringing (drives the in-app RingingScreen), null otherwise. */
    val ringingAlarmId = MutableStateFlow<String?>(null)

    fun initialize(engine: AlarmEngine) {
        repo = AlarmRepository(DriverFactory().createDriver())
        scheduler = AlarmScheduler(
            repo, engine,
            NextFireCalculator(TimeZone.currentSystemDefault()),
            ToneRandomizer(),
        )
        scheduler.rescheduleAll()
    }

    // Called from Swift:
    fun onAlarmFired(alarmId: String) { ringingAlarmId.value = alarmId }
    fun onDismissTapped(alarmId: String) {
        scheduler.onDismissed(alarmId); ringingAlarmId.value = null
    }
    fun onSnoozeTapped(alarmId: String) {
        scheduler.onSnoozed(alarmId); ringingAlarmId.value = null
    }
}
```

- [ ] **Step 3: Write `App.kt`** — root composable switching list / ringing

```kotlin
package com.devansh.alarm.ui

import androidx.compose.runtime.*
import com.devansh.alarm.AppCore

@Composable
fun App() {
    ClockTheme {
        val ringingId by AppCore.ringingAlarmId.collectAsState()
        val id = ringingId
        if (id != null) RingingScreen(alarmId = id)   // Task 15
        else AlarmListScreen()                        // Task 10
    }
}
```

(Until Tasks 10/15 exist, temporarily stub both as `Text("list")` / `Text("ringing")` so this compiles; replace in those tasks.)

- [ ] **Step 4: Rewrite `MainViewController.kt`**

```kotlin
package com.devansh.alarm

import androidx.compose.ui.window.ComposeUIViewController
import com.devansh.alarm.engine.AlarmEngine
import com.devansh.alarm.ui.App
import platform.UIKit.UIViewController

fun MainViewController(engine: AlarmEngine): UIViewController {
    AppCore.initialize(engine)
    return ComposeUIViewController { App() }
}
```

- [ ] **Step 5: Build** — `./gradlew :shared:compileKotlinIosSimulatorArm64` → BUILD SUCCESSFUL. (The iOS app target won't compile until Swift passes an engine — Task 13 updates `iOSApp.swift`; for now keep a temporary no-op Swift engine:)

```swift
// temporary, in iOSApp.swift — replaced in Task 13
class NoopEngine: AlarmEngine {
    func schedule(request: ScheduleRequest) {}
    func cancel(alarmId: String) {}
    func stopRinging(alarmId: String) {}
}
// and: MainViewControllerKt.MainViewController(engine: NoopEngine())
```

- [ ] **Step 6: Commit** — `git commit -am "feat: theme, AppCore wiring, app shell"`

### Task 10: Alarm list screen (Clock-app replica)

**Files:**
- Create: `shared/src/commonMain/kotlin/com/devansh/alarm/ui/AlarmListScreen.kt`
- Modify: `shared/src/commonMain/kotlin/com/devansh/alarm/ui/App.kt` (remove stub)

- [ ] **Step 1: Implement `AlarmListScreen.kt`**

```kotlin
package com.devansh.alarm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devansh.alarm.AppCore
import com.devansh.alarm.domain.Alarm
import kotlinx.datetime.DayOfWeek

@Composable
fun AlarmListScreen() {
    var alarms by remember { mutableStateOf(AppCore.repo.all()) }
    var editing by remember { mutableStateOf<Alarm?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    fun refresh() { alarms = AppCore.repo.all() }

    Column(Modifier.fillMaxSize().background(ClockBackground)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
                .padding(top = 44.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Alarm", fontSize = 34.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground)
            TextButton(onClick = { showAdd = true }) {
                Text("+", fontSize = 28.sp, color = ClockOrange)
            }
        }
        LazyColumn(Modifier.weight(1f)) {
            items(alarms, key = { it.id }) { alarm ->
                AlarmRow(
                    alarm = alarm,
                    onToggle = { enabled ->
                        AppCore.scheduler.setEnabled(alarm.id, enabled); refresh()
                    },
                    onClick = { editing = alarm },
                )
                HorizontalDivider(color = ClockSeparator, thickness = 0.5.dp,
                    modifier = Modifier.padding(start = 16.dp))
            }
        }
    }
    if (showAdd || editing != null) {
        EditAlarmSheet(  // Task 11
            initial = editing,
            onSave = { AppCore.scheduler.saveAlarm(it); showAdd = false; editing = null; refresh() },
            onDelete = { id -> AppCore.scheduler.deleteAlarm(id); editing = null; refresh() },
            onCancel = { showAdd = false; editing = null },
        )
    }
}

@Composable
private fun AlarmRow(alarm: Alarm, onToggle: (Boolean) -> Unit, onClick: () -> Unit) {
    val h12 = when { alarm.hour == 0 -> 12; alarm.hour > 12 -> alarm.hour - 12; else -> alarm.hour }
    val ampm = if (alarm.hour < 12) "AM" else "PM"
    val timeText = "$h12:${alarm.minute.toString().padStart(2, '0')}"
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(timeText, fontSize = 50.sp, fontWeight = FontWeight.Light,
                    color = if (alarm.enabled) MaterialTheme.colorScheme.onBackground
                            else ClockTextSecondary)
                Spacer(Modifier.width(4.dp))
                Text(ampm, fontSize = 24.sp, fontWeight = FontWeight.Light,
                    color = if (alarm.enabled) MaterialTheme.colorScheme.onBackground
                            else ClockTextSecondary,
                    modifier = Modifier.padding(bottom = 6.dp))
            }
            Text("${alarm.label}${repeatSummary(alarm.repeatDays)}",
                fontSize = 14.sp, color = ClockTextSecondary)
        }
        Switch(
            checked = alarm.enabled, onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedTrackColor = ClockOrange,
                checkedThumbColor = androidx.compose.ui.graphics.Color.White,
            ),
        )
    }
}

fun repeatSummary(days: Set<DayOfWeek>): String {
    if (days.isEmpty()) return ""
    val weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
    val weekend = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
    return ", " + when (days) {
        DayOfWeek.entries.toSet() -> "every day"
        weekdays -> "weekdays"
        weekend -> "weekends"
        else -> days.sortedBy { it.isoDayNumber }
            .joinToString(" ") { it.name.take(3).lowercase().replaceFirstChar(Char::uppercase) }
    }
}
```

- [ ] **Step 2: Build** — `./gradlew :shared:compileKotlinIosSimulatorArm64` → SUCCESS (EditAlarmSheet must exist; if doing this task before Task 11, stub it with the same signature returning nothing).
- [ ] **Step 3: Commit** — `git commit -am "feat: Clock-style alarm list screen"`

### Task 11: Wheel time picker + Edit Alarm sheet

**Files:**
- Create: `shared/src/commonMain/kotlin/com/devansh/alarm/ui/WheelPicker.kt`
- Create: `shared/src/commonMain/kotlin/com/devansh/alarm/ui/EditAlarmSheet.kt`

- [ ] **Step 1: Implement `WheelPicker.kt`** — snapping wheel column

```kotlin
package com.devansh.alarm.ui

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Snapping wheel. [items] rendered at [rowHeightDp] each; 2 padding rows top/bottom
 *  keep the selected item vertically centered in a 5-row viewport. */
@Composable
fun WheelPicker(
    items: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    rowHeightDp: Int = 36,
) {
    val state = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val fling = rememberSnapFlingBehavior(lazyListState = state)
    LaunchedEffect(state.isScrollInProgress) {
        if (!state.isScrollInProgress) onSelected(state.firstVisibleItemIndex)
    }
    LazyColumn(
        state = state, flingBehavior = fling,
        modifier = modifier.height((rowHeightDp * 5).dp),
        contentPadding = PaddingValues(vertical = (rowHeightDp * 2).dp),
    ) {
        items(items.size) { i ->
            val selected = i == state.firstVisibleItemIndex
            Box(Modifier.height(rowHeightDp.dp).fillMaxWidth(),
                contentAlignment = Alignment.Center) {
                Text(items[i], fontSize = if (selected) 24.sp else 20.sp,
                    color = if (selected) androidx.compose.ui.graphics.Color.White
                            else ClockTextSecondary)
            }
        }
    }
}
```

- [ ] **Step 2: Implement `EditAlarmSheet.kt`**

```kotlin
package com.devansh.alarm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devansh.alarm.domain.*
import kotlinx.datetime.DayOfWeek
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAlarmSheet(
    initial: Alarm?,
    onSave: (Alarm) -> Unit,
    onDelete: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var hour by remember { mutableStateOf(initial?.hour ?: 7) }
    var minute by remember { mutableStateOf(initial?.minute ?: 0) }
    var days by remember { mutableStateOf(initial?.repeatDays ?: emptySet()) }
    var label by remember { mutableStateOf(initial?.label ?: "Alarm") }
    var tone by remember { mutableStateOf(initial?.tone ?: ToneSelection.Random) }
    var snoozeOn by remember { mutableStateOf(initial?.snoozeEnabled ?: true) }
    var snoozeMin by remember { mutableStateOf(initial?.snoozeMinutes ?: 9) }

    ModalBottomSheet(onDismissRequest = onCancel, containerColor = ClockSurface) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onCancel) { Text("Cancel", color = ClockOrange) }
                Text(if (initial == null) "Add Alarm" else "Edit Alarm",
                    fontSize = 17.sp, color = Color.White,
                    modifier = Modifier.align(Alignment.CenterVertically))
                TextButton(onClick = {
                    onSave(Alarm(
                        id = initial?.id ?: "alarm-${Random.nextLong()}",
                        hour = hour, minute = minute, repeatDays = days, label = label,
                        tone = tone, snoozeEnabled = snoozeOn, snoozeMinutes = snoozeMin,
                        enabled = true,
                    ))
                }) { Text("Save", color = ClockOrange) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                WheelPicker((0..23).map { it.toString().padStart(2, '0') }, hour,
                    { hour = it }, Modifier.width(80.dp))
                Text(":", fontSize = 24.sp, color = Color.White,
                    modifier = Modifier.align(Alignment.CenterVertically))
                WheelPicker((0..59).map { it.toString().padStart(2, '0') }, minute,
                    { minute = it }, Modifier.width(80.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text("REPEAT", fontSize = 13.sp, color = ClockTextSecondary)
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween) {
                DayOfWeek.entries.forEach { d ->
                    val on = d in days
                    FilterChip(selected = on, onClick = { days = if (on) days - d else days + d },
                        label = { Text(d.name.take(1)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ClockOrange))
                }
            }
            OutlinedTextField(value = label, onValueChange = { label = it },
                label = { Text("Label") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            Text("SOUND", fontSize = 13.sp, color = ClockTextSecondary)
            ToneRowItem("Random (default)", tone == ToneSelection.Random) {
                tone = ToneSelection.Random
            }
            BUNDLED_TONES.forEach { t ->
                ToneRowItem(t.displayName, (tone as? ToneSelection.Pinned)?.toneId == t.id) {
                    tone = ToneSelection.Pinned(t.id)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("Snooze", color = Color.White)
                Switch(checked = snoozeOn, onCheckedChange = { snoozeOn = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = ClockOrange))
            }
            if (snoozeOn) {
                Text("Snooze duration: $snoozeMin min", color = ClockTextSecondary)
                Slider(value = snoozeMin.toFloat(), onValueChange = { snoozeMin = it.toInt() },
                    valueRange = 1f..30f, colors = SliderDefaults.colors(thumbColor = ClockOrange,
                        activeTrackColor = ClockOrange))
            }
            if (initial != null) {
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = { onDelete(initial.id) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Delete Alarm", color = Color(0xFFFF453A))
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ToneRowItem(name: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween) {
        TextButton(onClick = onClick) {
            Text(name, color = if (selected) ClockOrange else Color.White)
        }
        if (selected) Text("✓", color = ClockOrange,
            modifier = Modifier.align(Alignment.CenterVertically).padding(end = 12.dp))
    }
}
```

- [ ] **Step 3: Build + run on simulator; verify manually:** add an alarm (wheel scrolls & snaps), edit it, toggle it, repeat-day chips, tone list with Random default, snooze slider, delete.

```bash
./gradlew :shared:compileKotlinIosSimulatorArm64
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 16' build
```

- [ ] **Step 4: Commit** — `git commit -am "feat: edit alarm sheet with wheel picker, repeat, tone, snooze"`

---

## Milestone 2 — Swift AlarmEngine

### Task 12: Bundled tones

**Files:**
- Create: `scripts/make_tones.py`, `iosApp/iosApp/Tones/*.caf` (8 files)

- [ ] **Step 1: Write `scripts/make_tones.py`** (placeholder tones — distinct synthesized patterns, ≤29 s so they're valid notification sounds; replaceable with real audio later)

```python
#!/usr/bin/env python3
import math, struct, subprocess, wave, os

RATE = 44100
DUR = 28.0
SPECS = {  # name: (base_freq, beep_hz, second_freq)
    "tone_radial": (880, 2.0, 1108), "tone_ascent": (523, 1.0, 659),
    "tone_pulse": (740, 4.0, 740),   "tone_chimes": (1046, 0.5, 1318),
    "tone_cosmic": (440, 1.5, 554),  "tone_beacon": (988, 3.0, 784),
    "tone_signal": (660, 2.5, 880),  "tone_waves": (392, 0.8, 494),
}
os.makedirs("iosApp/iosApp/Tones", exist_ok=True)
for name, (f1, beep, f2) in SPECS.items():
    frames = []
    for i in range(int(RATE * DUR)):
        t = i / RATE
        gate = 1.0 if math.sin(2 * math.pi * beep * t) > 0 else 0.0
        f = f1 if int(t * beep * 2) % 2 == 0 else f2
        s = 0.6 * gate * math.sin(2 * math.pi * f * t)
        frames.append(struct.pack("<h", int(s * 32767)))
    wav = f"/tmp/{name}.wav"
    with wave.open(wav, "wb") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(RATE)
        w.writeframes(b"".join(frames))
    subprocess.run(["afconvert", "-f", "caff", "-d", "LEI16",
                    wav, f"iosApp/iosApp/Tones/{name}.caf"], check=True)
    print("made", name)
```

- [ ] **Step 2: Run it** — `python3 scripts/make_tones.py` → 8 `.caf` files exist. Verify one plays: `afplay iosApp/iosApp/Tones/tone_radial.caf` (Ctrl-C to stop).
- [ ] **Step 3: Regenerate Xcode project** so Tones/ is bundled: `cd iosApp && xcodegen generate && cd ..`
- [ ] **Step 4: Commit** — `git add -A && git commit -m "feat: eight synthesized placeholder alarm tones"`

### Task 13: Swift engine — notifications, audio ringer, volume guard

**Files:**
- Create: `iosApp/iosApp/NotificationScheduler.swift`, `iosApp/iosApp/AudioRinger.swift`, `iosApp/iosApp/VolumeGuard.swift`, `iosApp/iosApp/AlarmEngineImpl.swift`
- Modify: `iosApp/iosApp/iOSApp.swift` (replace NoopEngine)
- Modify: `iosApp/iosApp/Info.plist` (already has `audio` background mode)

- [ ] **Step 1: Write `NotificationScheduler.swift`** — chain of 6 notifications 30 s apart with Dismiss/Snooze actions

```swift
import Foundation
import UserNotifications

enum NotifCategory {
    static let alarm = "ALARM_CATEGORY"
    static let dismissAction = "DISMISS_ACTION"
    static let snoozeAction = "SNOOZE_ACTION"
}

final class NotificationScheduler {
    static let shared = NotificationScheduler()
    private let center = UNUserNotificationCenter.current()

    func registerCategoriesAndRequestPermission() {
        let dismiss = UNNotificationAction(identifier: NotifCategory.dismissAction,
            title: "Dismiss", options: [.destructive])
        let snooze = UNNotificationAction(identifier: NotifCategory.snoozeAction,
            title: "Snooze", options: [])
        let cat = UNNotificationCategory(identifier: NotifCategory.alarm,
            actions: [dismiss, snooze], intentIdentifiers: [],
            options: [.customDismissAction])
        center.setNotificationCategories([cat])
        center.requestAuthorization(options: [.alert, .sound, .badge]) { _, _ in }
    }

    /// Chain of 6 backup notifications, 30s apart, starting at fireAt.
    func scheduleChain(alarmId: String, fireAt: Date, toneFileName: String,
                       label: String, snoozeMinutes: Int32) {
        cancelChain(alarmId: alarmId)
        for i in 0..<6 {
            let content = UNMutableNotificationContent()
            content.title = label
            content.body = i == 0 ? "Alarm" : "Still ringing — open to dismiss"
            content.sound = UNNotificationSound(
                named: UNNotificationSoundName(toneFileName))
            content.categoryIdentifier = NotifCategory.alarm
            content.userInfo = ["alarmId": alarmId, "snoozeMinutes": Int(snoozeMinutes)]
            content.interruptionLevel = .timeSensitive
            let t = fireAt.addingTimeInterval(Double(i) * 30)
            let interval = max(t.timeIntervalSinceNow, 1)
            let trigger = UNTimeIntervalNotificationTrigger(
                timeInterval: interval, repeats: false)
            center.add(UNNotificationRequest(
                identifier: "\(alarmId)#\(i)", content: content, trigger: trigger))
        }
    }

    func cancelChain(alarmId: String) {
        let ids = (0..<6).map { "\(alarmId)#\($0)" }
        center.removePendingNotificationRequests(withIdentifiers: ids)
        center.removeDeliveredNotifications(withIdentifiers: ids)
    }
}
```

- [ ] **Step 2: Write `AudioRinger.swift`** — looping playback session + fire timer

```swift
import AVFoundation
import Foundation

/// Plays the alarm tone in a loop through a .playback session (ignores silent
/// switch). Keeps a near-silent looping player alive while alarms are armed so
/// the app stays runnable in the background and the timer can fire.
final class AudioRinger: NSObject {
    static let shared = AudioRinger()
    private var keepAlivePlayer: AVAudioPlayer?
    private var ringPlayer: AVAudioPlayer?
    private var fireTimers: [String: Timer] = [:]
    var onFire: ((String) -> Void)?   // set by AlarmEngineImpl

    func armSession() {
        let session = AVAudioSession.sharedInstance()
        try? session.setCategory(.playback, mode: .default, options: [.mixWithOthers])
        try? session.setActive(true)
        guard keepAlivePlayer == nil,
              let url = Bundle.main.url(forResource: "tone_radial", withExtension: "caf")
        else { return }
        keepAlivePlayer = try? AVAudioPlayer(contentsOf: url)
        keepAlivePlayer?.volume = 0.0   // inaudible keep-alive
        keepAlivePlayer?.numberOfLoops = -1
        keepAlivePlayer?.play()
    }

    func scheduleFire(alarmId: String, at date: Date, toneFileName: String) {
        fireTimers[alarmId]?.invalidate()
        let interval = max(date.timeIntervalSinceNow, 0.5)
        let timer = Timer(timeInterval: interval, repeats: false) { [weak self] _ in
            self?.startRinging(alarmId: alarmId, toneFileName: toneFileName)
        }
        RunLoop.main.add(timer, forMode: .common)
        fireTimers[alarmId] = timer
    }

    func cancelFire(alarmId: String) {
        fireTimers[alarmId]?.invalidate()
        fireTimers[alarmId] = nil
    }

    private func startRinging(alarmId: String, toneFileName: String) {
        let name = (toneFileName as NSString).deletingPathExtension
        let session = AVAudioSession.sharedInstance()
        try? session.setCategory(.playback, mode: .default, options: [])  // stop mixing: full volume
        try? session.setActive(true)
        if let url = Bundle.main.url(forResource: name, withExtension: "caf") {
            ringPlayer = try? AVAudioPlayer(contentsOf: url)
            ringPlayer?.numberOfLoops = -1
            ringPlayer?.volume = 1.0
            ringPlayer?.play()
        }
        VolumeGuard.shared.startGuarding()
        onFire?(alarmId)
    }

    func stopRinging() {
        VolumeGuard.shared.stopGuarding()
        ringPlayer?.stop()
        ringPlayer = nil
        let session = AVAudioSession.sharedInstance()
        try? session.setCategory(.playback, mode: .default, options: [.mixWithOthers])
    }
}
```

- [ ] **Step 3: Write `VolumeGuard.swift`** — reset volume when user presses volume-down during ring

```swift
import AVFoundation
import MediaPlayer
import UIKit

/// While ringing, observes system output volume and snaps it back up if the
/// user tries to silence the alarm with the volume buttons.
final class VolumeGuard: NSObject {
    static let shared = VolumeGuard()
    private var observation: NSKeyValueObservation?
    private let volumeView = MPVolumeView(frame: .zero)
    private let targetVolume: Float = 0.9

    func startGuarding() {
        volumeView.isHidden = true
        UIApplication.shared.connectedScenes
            .compactMap { ($0 as? UIWindowScene)?.keyWindow }
            .first?.addSubview(volumeView)
        setVolume(targetVolume)
        observation = AVAudioSession.sharedInstance().observe(
            \.outputVolume, options: [.new]
        ) { [weak self] _, change in
            guard let self, let v = change.newValue, v < self.targetVolume else { return }
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
                self.setVolume(self.targetVolume)
            }
        }
    }

    func stopGuarding() {
        observation?.invalidate(); observation = nil
        volumeView.removeFromSuperview()
    }

    private func setVolume(_ value: Float) {
        let slider = volumeView.subviews.compactMap { $0 as? UISlider }.first
        DispatchQueue.main.async { slider?.value = value }
    }
}
```

- [ ] **Step 4: Write `AlarmEngineImpl.swift`** — conforms to the Kotlin protocol, ties it together

```swift
import Foundation
import Shared

final class AlarmEngineImpl: NSObject, AlarmEngine {
    static let shared = AlarmEngineImpl()

    override init() {
        super.init()
        AudioRinger.shared.onFire = { alarmId in
            LiveActivityController.shared.start(alarmId: alarmId)   // Task 16 (no-op stub until then)
            AppCore.shared.onAlarmFired(alarmId: alarmId)
        }
    }

    func schedule(request: ScheduleRequest) {
        let fireAt = Date(timeIntervalSince1970:
            Double(request.fireAtEpochMillis) / 1000.0)
        NotificationScheduler.shared.scheduleChain(
            alarmId: request.alarmId, fireAt: fireAt,
            toneFileName: request.toneFileName, label: request.label,
            snoozeMinutes: request.snoozeMinutes)
        AudioRinger.shared.armSession()
        AudioRinger.shared.scheduleFire(alarmId: request.alarmId, at: fireAt,
            toneFileName: request.toneFileName)
    }

    func cancel(alarmId: String) {
        NotificationScheduler.shared.cancelChain(alarmId: alarmId)
        AudioRinger.shared.cancelFire(alarmId: alarmId)
    }

    func stopRinging(alarmId: String) {
        AudioRinger.shared.stopRinging()
        NotificationScheduler.shared.cancelChain(alarmId: alarmId)
        LiveActivityController.shared.end(alarmId: alarmId)
    }
}
```

Also add a temporary stub so this compiles before Task 16:

```swift
// LiveActivityController.swift — REPLACED in Task 16
import Foundation
final class LiveActivityController {
    static let shared = LiveActivityController()
    func start(alarmId: String) {}
    func end(alarmId: String) {}
}
```

- [ ] **Step 5: Update `iOSApp.swift`** — real engine, notification delegate, permission request

```swift
import SwiftUI
import UserNotifications
import Shared

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate
    var body: some Scene {
        WindowGroup { ComposeView().ignoresSafeArea(.all) }
    }
}

class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    func application(_ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions:
        [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        NotificationScheduler.shared.registerCategoriesAndRequestPermission()
        return true
    }

    // Notification action taps (lock screen / banner)
    func userNotificationCenter(_ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void) {
        let info = response.notification.request.content.userInfo
        guard let alarmId = info["alarmId"] as? String else { completionHandler(); return }
        switch response.actionIdentifier {
        case NotifCategory.snoozeAction:
            AppCore.shared.onSnoozeTapped(alarmId: alarmId)
        default: // Dismiss action, or tapping the notification body
            AppCore.shared.onDismissTapped(alarmId: alarmId)
        }
        completionHandler()
    }

    // Show banner+sound even when app is foregrounded
    func userNotificationCenter(_ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler:
        @escaping (UNNotificationPresentationOptions) -> Void) {
        completionHandler([.banner, .sound])
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(engine: AlarmEngineImpl.shared)
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
```

(Kotlin `object AppCore` is reached from Swift as `AppCore.shared`.)

- [ ] **Step 6: Build for simulator** — `cd iosApp && xcodegen generate && cd .. && xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 16' build` → SUCCEEDED.
- [ ] **Step 7: Commit** — `git commit -am "feat: Swift alarm engine — notification chain, audio ringer, volume guard"`

### Task 14: In-app ringing screen (slide to dismiss)

**Files:**
- Create: `shared/src/commonMain/kotlin/com/devansh/alarm/ui/RingingScreen.kt`
- Modify: `shared/src/commonMain/kotlin/com/devansh/alarm/ui/App.kt` (remove stub)

- [ ] **Step 1: Implement `RingingScreen.kt`**

```kotlin
package com.devansh.alarm.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devansh.alarm.AppCore
import kotlin.math.roundToInt

@Composable
fun RingingScreen(alarmId: String) {
    val alarm = remember(alarmId) { AppCore.repo.all().firstOrNull { it.id == alarmId } }
    Column(
        Modifier.fillMaxSize().background(ClockBackground).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Spacer(Modifier.height(80.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(alarm?.label ?: "Alarm", fontSize = 34.sp, color = Color.White)
            val h = alarm?.hour ?: 0; val m = alarm?.minute ?: 0
            Text("${if (h % 12 == 0) 12 else h % 12}:${m.toString().padStart(2, '0')} " +
                 if (h < 12) "AM" else "PM",
                fontSize = 22.sp, color = ClockTextSecondary)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (alarm?.snoozeEnabled == true) {
                TextButton(onClick = { AppCore.onSnoozeTapped(alarmId) }) {
                    Text("Snooze (${alarm.snoozeMinutes} min)",
                        fontSize = 20.sp, color = ClockOrange)
                }
                Spacer(Modifier.height(24.dp))
            }
            SlideToDismiss { AppCore.onDismissTapped(alarmId) }
            Spacer(Modifier.height(48.dp))
        }
    }
}

/** Track 300dp wide; dragging the 56dp knob past 80% triggers dismissal. */
@Composable
private fun SlideToDismiss(onDismissed: () -> Unit) {
    val trackWidth = 300.dp; val knob = 56.dp
    val maxPx = with(androidx.compose.ui.platform.LocalDensity.current) {
        (trackWidth - knob).toPx()
    }
    var offset by remember { mutableStateOf(0f) }
    val animated by animateFloatAsState(offset)
    Box(
        Modifier.width(trackWidth).height(knob).clip(CircleShape).background(ClockSurface),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text("slide to stop", color = ClockTextSecondary,
            modifier = Modifier.align(Alignment.Center))
        Box(
            Modifier.offset { IntOffset(animated.roundToInt(), 0) }
                .size(knob).clip(CircleShape).background(ClockOrange)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        offset = (offset + delta).coerceIn(0f, maxPx)
                    },
                    onDragStopped = {
                        if (offset > maxPx * 0.8f) onDismissed() else offset = 0f
                    },
                ),
            contentAlignment = Alignment.Center,
        ) { Text("⏰", fontSize = 24.sp) }
    }
}
```

- [ ] **Step 2: Build** — `./gradlew :shared:compileKotlinIosSimulatorArm64` → SUCCESS.

- [ ] **Step 3: On-device manual test (M2 checklist)** — deploy to iPhone, set an alarm 2 minutes out, then verify each:
  - App foreground: ringing screen appears, tone loops, slide-to-dismiss stops it, next occurrence scheduled (check alarm list).
  - App background (home screen, phone unlocked): tone plays via audio session; notification banner shows; tapping Dismiss action stops audio.
  - Phone locked: alarm fires — sound plays; **press power button: sound continues**; **press volume-down: volume snaps back up**; notification on lock screen → swipe/long-press → Dismiss stops it.
  - Snooze via notification action: rings again after the snooze duration with a different tone.
  - Force-quit the app, alarm due in 2 min: notification chain fires (6 × 30 s bursts), no continuous audio — expected degraded mode.

- [ ] **Step 4: Commit** — `git commit -am "feat: in-app ringing screen with slide-to-dismiss"`

---

## Milestone 3 — Lock-screen Live Activity

### Task 15: Activity attributes + App Intents (shared files)

**Files:**
- Create: `iosApp/iosApp/AlarmActivityAttributes.swift`, `iosApp/iosApp/AlarmIntents.swift`
- Modify: `iosApp/project.yml` (widget target; these two files compile into BOTH targets)

- [ ] **Step 1: Write `AlarmActivityAttributes.swift`**

```swift
import ActivityKit
import Foundation

struct AlarmActivityAttributes: ActivityAttributes {
    struct ContentState: Codable, Hashable {
        var state: String          // "ringing" | "snoozed"
        var snoozedUntil: Date?
    }
    var alarmId: String
    var label: String
    var timeText: String           // "7:30 AM"
}
```

- [ ] **Step 2: Write `AlarmIntents.swift`** — `LiveActivityIntent` runs in the app's process, so it can reach the Kotlin framework

```swift
import AppIntents
import Foundation

struct DismissAlarmIntent: LiveActivityIntent {
    static var title: LocalizedStringResource = "Dismiss Alarm"
    @Parameter(title: "Alarm ID") var alarmId: String
    init() {}
    init(alarmId: String) { self.alarmId = alarmId }
    func perform() async throws -> some IntentResult {
        await MainActor.run { AlarmIntentBridge.dismiss?(alarmId) }
        return .result()
    }
}

struct SnoozeAlarmIntent: LiveActivityIntent {
    static var title: LocalizedStringResource = "Snooze Alarm"
    @Parameter(title: "Alarm ID") var alarmId: String
    init() {}
    init(alarmId: String) { self.alarmId = alarmId }
    func perform() async throws -> some IntentResult {
        await MainActor.run { AlarmIntentBridge.snooze?(alarmId) }
        return .result()
    }
}

/// The widget target compiles this file too but must not link the Kotlin
/// framework; the app target sets these closures at launch.
enum AlarmIntentBridge {
    static var dismiss: ((String) -> Void)?
    static var snooze: ((String) -> Void)?
}
```

- [ ] **Step 3: Wire the bridge in `AppDelegate.didFinishLaunching`** (add these lines):

```swift
AlarmIntentBridge.dismiss = { AppCore.shared.onDismissTapped(alarmId: $0) }
AlarmIntentBridge.snooze = { AppCore.shared.onSnoozeTapped(alarmId: $0) }
```

- [ ] **Step 4: Commit** — `git commit -am "feat: Live Activity attributes and dismiss/snooze App Intents"`

### Task 16: Live Activity controller + widget extension

**Files:**
- Create: `iosApp/AlarmWidget/AlarmWidgetBundle.swift`, `iosApp/AlarmWidget/AlarmActivityWidget.swift`, `iosApp/AlarmWidget/Info.plist`
- Modify: `iosApp/iosApp/LiveActivityController.swift` (replace stub), `iosApp/project.yml`

- [ ] **Step 1: Replace `LiveActivityController.swift`**

```swift
import ActivityKit
import Foundation
import Shared

final class LiveActivityController {
    static let shared = LiveActivityController()
    private var activities: [String: Activity<AlarmActivityAttributes>] = [:]

    func start(alarmId: String) {
        guard ActivityAuthorizationInfo().areActivitiesEnabled else { return }
        let alarm = AppCore.shared.repo.all().first { $0.id == alarmId }
        let h = Int(alarm?.hour ?? 0), m = Int(alarm?.minute ?? 0)
        let timeText = "\(h % 12 == 0 ? 12 : h % 12):\(String(format: "%02d", m)) \(h < 12 ? "AM" : "PM")"
        let attrs = AlarmActivityAttributes(alarmId: alarmId,
            label: alarm?.label ?? "Alarm", timeText: timeText)
        let state = AlarmActivityAttributes.ContentState(state: "ringing", snoozedUntil: nil)
        activities[alarmId] = try? Activity.request(attributes: attrs,
            content: .init(state: state, staleDate: nil))
    }

    func showSnoozed(alarmId: String, until: Date) {
        guard let activity = activities[alarmId] else { return }
        Task {
            await activity.update(.init(
                state: .init(state: "snoozed", snoozedUntil: until), staleDate: nil))
        }
    }

    func end(alarmId: String) {
        guard let activity = activities[alarmId] else { return }
        activities[alarmId] = nil
        Task { await activity.end(nil, dismissalPolicy: .immediate) }
    }
}
```

- [ ] **Step 2: Write the widget** `AlarmWidgetBundle.swift` + `AlarmActivityWidget.swift`

```swift
// AlarmWidgetBundle.swift
import SwiftUI
import WidgetKit

@main
struct AlarmWidgetBundle: WidgetBundle {
    var body: some Widget { AlarmActivityWidget() }
}
```

```swift
// AlarmActivityWidget.swift
import ActivityKit
import SwiftUI
import WidgetKit

struct AlarmActivityWidget: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: AlarmActivityAttributes.self) { context in
            // Lock screen card
            VStack(spacing: 12) {
                HStack {
                    VStack(alignment: .leading) {
                        Text(context.attributes.label).font(.headline)
                        Text(context.attributes.timeText)
                            .font(.title2).fontWeight(.light)
                    }
                    Spacer()
                    Image(systemName: "alarm.fill").font(.title)
                        .foregroundStyle(.orange)
                }
                if context.state.state == "snoozed", let until = context.state.snoozedUntil {
                    Text("Snoozed until \(until, style: .time)")
                        .font(.subheadline).foregroundStyle(.secondary)
                } else {
                    HStack(spacing: 12) {
                        Button(intent: SnoozeAlarmIntent(alarmId: context.attributes.alarmId)) {
                            Text("Snooze").frame(maxWidth: .infinity)
                        }.tint(.gray)
                        Button(intent: DismissAlarmIntent(alarmId: context.attributes.alarmId)) {
                            Text("Dismiss").frame(maxWidth: .infinity)
                        }.tint(.orange)
                    }.buttonStyle(.borderedProminent)
                }
            }
            .padding()
            .activityBackgroundTint(Color.black.opacity(0.8))
        } dynamicIsland: { context in
            DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {
                    Text(context.attributes.label).font(.headline)
                }
                DynamicIslandExpandedRegion(.trailing) {
                    Text(context.attributes.timeText)
                }
                DynamicIslandExpandedRegion(.bottom) {
                    HStack {
                        Button(intent: SnoozeAlarmIntent(alarmId: context.attributes.alarmId)) {
                            Text("Snooze")
                        }
                        Button(intent: DismissAlarmIntent(alarmId: context.attributes.alarmId)) {
                            Text("Dismiss")
                        }.tint(.orange)
                    }
                }
            } compactLeading: {
                Image(systemName: "alarm.fill").foregroundStyle(.orange)
            } compactTrailing: {
                Text(context.attributes.timeText).font(.caption2)
            } minimal: {
                Image(systemName: "alarm.fill").foregroundStyle(.orange)
            }
        }
    }
}
```

- [ ] **Step 3: Write `iosApp/AlarmWidget/Info.plist`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleDisplayName</key><string>AlarmWidget</string>
    <key>NSExtension</key>
    <dict>
        <key>NSExtensionPointIdentifier</key>
        <string>com.apple.widgetkit-extension</string>
    </dict>
</dict>
</plist>
```

- [ ] **Step 4: Update `project.yml`** — add the widget target and embed it (append under `targets:`):

```yaml
  AlarmWidget:
    type: app-extension
    platform: iOS
    sources:
      - path: AlarmWidget
      - path: iosApp/AlarmActivityAttributes.swift
      - path: iosApp/AlarmIntents.swift
    settings:
      base:
        PRODUCT_BUNDLE_IDENTIFIER: com.devansh.myalarm.widget
        INFOPLIST_FILE: AlarmWidget/Info.plist
        SKIP_INSTALL: YES
```

And in the `iosApp` target add:

```yaml
    dependencies:
      - target: AlarmWidget
        embed: true
```

- [ ] **Step 5: Hook snooze state update** — in `AlarmEngineImpl.schedule`, when the request is a snooze re-schedule the Live Activity shows "snoozed"; simplest: in `AppCore.onSnoozeTapped` Kotlin already calls `engine.schedule(...)`; add to Swift `AlarmEngineImpl.schedule` before `armSession()`:

```swift
LiveActivityController.shared.showSnoozed(alarmId: request.alarmId, until: fireAt)
```

(This is a no-op unless an activity exists for that id — exactly the snooze case; a freshly armed alarm has no activity yet.)

- [ ] **Step 6: Regenerate + build** — `cd iosApp && xcodegen generate && cd .. && xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 16' build` → SUCCEEDED. Re-check signing for the new widget target in Xcode (same personal team).

- [ ] **Step 7: On-device manual test (M3 checklist)** — alarm 2 min out, lock the phone:
  - Live Activity card appears on the lock screen when the alarm fires.
  - **Dismiss** button stops the sound without unlocking; card disappears.
  - **Snooze** button stops the sound; card shows "Snoozed until …"; alarm re-fires at snooze time with a different tone; card returns to ringing state (new activity).
  - Dynamic Island (if applicable) shows compact alarm state.

- [ ] **Step 8: Commit** — `git commit -am "feat: lock-screen Live Activity with dismiss/snooze"`

---

## Milestone 4 — Polish & reconciliation

### Task 17: Missed-alarm reconciliation on launch

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/devansh/alarm/domain/AlarmScheduler.kt`
- Test: add to `shared/src/iosTest/kotlin/com/devansh/alarm/domain/AlarmSchedulerTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
@Test fun rescheduleAllSkipsPastAndArmsFuture() {
    // Alarm at 07:30, "app was dead" over the fire time; relaunch at 09:00.
    scheduler.saveAlarm(Alarm("a", 7, 30, repeatDays = setOf(DayOfWeek.MONDAY)))
    engine.scheduled.clear()
    clock.now = LocalDateTime(2026, 9, 21, 9, 0).toInstant(zone) // Monday 09:00
    scheduler.rescheduleAll()
    // Must arm NEXT Monday, not the past occurrence.
    assertEquals(LocalDateTime(2026, 9, 28, 7, 30).toInstant(zone).toEpochMilliseconds(),
        engine.scheduled.single().fireAtEpochMillis)
}
```

- [ ] **Step 2: Run** — this should already PASS given `nextFire` only returns future instants (the test pins the behavior). If it fails, fix `rescheduleAll`. Either way, keep the test.
- [ ] **Step 3: Commit** — `git commit -am "test: pin missed-alarm reconciliation behavior"`

### Task 18: Permission warning banner

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/devansh/alarm/AppCore.kt`, `shared/src/commonMain/kotlin/com/devansh/alarm/ui/AlarmListScreen.kt`, `iosApp/iosApp/iOSApp.swift`

- [ ] **Step 1: Add to `AppCore`:**

```kotlin
val notificationsDenied = MutableStateFlow(false)
fun setNotificationsDenied(denied: Boolean) { notificationsDenied.value = denied }
```

- [ ] **Step 2: In `AppDelegate.didFinishLaunching`** (after registerCategories):

```swift
UNUserNotificationCenter.current().getNotificationSettings { settings in
    let denied = settings.authorizationStatus == .denied
    DispatchQueue.main.async { AppCore.shared.setNotificationsDenied(denied: denied) }
}
```

- [ ] **Step 3: In `AlarmListScreen`**, above the LazyColumn:

```kotlin
val denied by AppCore.notificationsDenied.collectAsState()
if (denied) {
    Text(
        "Notifications are off — alarms won't ring if the app is closed. " +
        "Enable them in Settings ▸ My Alarm.",
        color = androidx.compose.ui.graphics.Color(0xFFFF453A),
        fontSize = 13.sp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
}
```

- [ ] **Step 4: Build both** (gradle compile + xcodebuild simulator) → SUCCESS. Commit — `git commit -am "feat: notification-permission warning banner"`

### Task 19: Full regression pass + visual fidelity

- [ ] **Step 1: Run all Kotlin tests** — `./gradlew :shared:iosSimulatorArm64Test` → all PASS.
- [ ] **Step 2: Full on-device checklist** (every item from Tasks 14 & 16 checklists, in one sitting) — record any failures as new bugs; use superpowers:systematic-debugging for each.
- [ ] **Step 3: Fidelity pass** — open the native Clock app side-by-side with ours; adjust font sizes/spacings in `AlarmListScreen`/`Theme` until rows visually match (time 50sp light, orange toggle, hairline separators, 34sp bold header).
- [ ] **Step 4: Silent-switch + Focus check** — flip the mute switch and enable a Focus; app-audio ring must still sound (playback session ignores mute); note in README that backup notifications respect Focus (expected iOS behavior).
- [ ] **Step 5: Final commit** — `git commit -am "chore: M4 polish — fidelity pass and regression checklist"`

---

## Known risks & watch-items (read before executing)

1. **Kotlin↔Swift naming:** the ObjC export may surface `ScheduleRequest` under a slightly different name/initializer. If Swift can't find it, check `shared/build/.../Shared.framework/Headers/Shared.h` for the exact exported names and adjust Swift call sites — not the Kotlin.
2. **Background timer reality:** iOS may still suspend the app despite the silent keep-alive player if the system reclaims resources (low power mode, long background time). The notification chain is the safety net — this is the accepted spec trade-off. Test realistic overnight scenarios before trusting it as your only alarm.
3. **`rememberSnapFlingBehavior` / Compose API drift:** Compose Multiplatform occasionally moves experimental APIs between versions. If an API in Tasks 10–14 doesn't resolve, check the versioned docs rather than downgrading.
4. **Free provisioning expiry:** personal-team installs expire after 7 days; re-run the Xcode deploy to refresh. A paid Apple Developer account removes this.
5. **Volume guard limits:** the MPVolumeView slider technique is unofficial-but-tolerated; if it stops working on a future iOS, the ring still plays at whatever volume — degraded, not broken.
