# iOS Alarm App — Design Spec

**Date:** 2026-09-20
**Status:** Approved by user (conversation, 2026-09-20)
**Target device:** User's iPhone (iOS 17.x), connected via USB. UDID `00008140001E08283AE3001C`.

## 1. Summary

A single-platform (iOS-only) alarm app that replicates the look and feel of the
native iOS Clock app's **Alarm** tab, written in **Kotlin** (Kotlin
Multiplatform + Compose Multiplatform), with two differentiating features:

1. **Hardware-button-proof alarms** — pressing power or volume buttons does
   not stop or snooze a ringing alarm. The alarm is dismissed or snoozed only
   via an on-screen interaction: a button on a lock-screen Live Activity card,
   a notification action, or an in-app full-screen swipe.
2. **Random tone by default** — each alarm ring picks a random tone from the
   bundled tone set, never repeating the immediately previous tone, unless the
   user has explicitly pinned a tone for that alarm.

## 2. Constraints and platform reality (accepted by user)

- iOS gives third-party apps **no way to intercept hardware buttons**. The
  button-proof behavior is achieved indirectly, Alarmy-style: the app keeps a
  background audio session; when the alarm fires, the app itself plays the
  looping alarm audio. A power press locks the screen but audio continues; a
  volume press is counteracted by programmatically resetting output volume.
- **Force-quitting the app disarms the audio engine.** Backup local
  notifications still fire (with sound, ~30 s each, chained), but the
  unstoppable behavior is lost. Accepted trade-off.
- Custom **swipe gestures are not possible on the lock screen**. Closest legal
  equivalents (both used): interactive **Live Activity** (iOS 17 App Intents
  buttons) with large Dismiss/Snooze buttons, and notification actions.
- Live Activity widgets **must be SwiftUI** (Apple requirement) → the project
  contains a thin Swift layer (~15%). Everything else is Kotlin.
- Silent switch / Focus modes: the app's own audio session
  (`AVAudioSessionCategoryPlayback`) plays through the silent switch. Backup
  notifications use critical-sounding defaults but are subject to Focus rules.
- Requires **Xcode** installed (currently only Command Line Tools on this
  Mac) and a free Apple developer profile for on-device deployment.
- No iOS 26 AlarmKit (device is iOS 17). Revisit if the device is upgraded.

## 3. Architecture

```
┌────────────────────────────────────────────────────────┐
│ iOS App (single Xcode workspace)                       │
│                                                        │
│  ┌──────────────────────────────┐  ┌────────────────┐  │
│  │ shared/ (Kotlin KMP)         │  │ iosApp/ (Swift)│  │
│  │  • Compose Multiplatform UI  │  │  • App entry   │  │
│  │  • Alarm domain model        │◄─┤  • AlarmEngine │  │
│  │  • Scheduling logic          │  │    (audio,     │  │
│  │  • Tone randomizer           │  │     notifs,    │  │
│  │  • Persistence (SQLDelight)  │  │     volume)    │  │
│  │  • AlarmEngine interface     │  │  • ActivityKit │  │
│  └──────────────────────────────┘  └────────────────┘  │
│  ┌──────────────────────────────────────────────────┐  │
│  │ AlarmWidget/ (Swift, WidgetKit extension)        │  │
│  │  • SwiftUI Live Activity card (Dismiss/Snooze    │  │
│  │    via App Intents)                              │  │
│  └──────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────┘
```

### 3.1 Module: `shared` (Kotlin)

- **UI (Compose Multiplatform)** — replica of the native Clock app Alarm tab:
  - Black background, white/orange typography matching iOS Clock.
  - Alarm list: large thin time text, AM/PM, label + repeat summary, orange
    iOS-style toggle per row. Swipe-to-delete. Edit mode.
  - "+" (top right) → Add Alarm sheet: wheel time picker, Repeat (weekday
    list), Label, Sound (list of bundled tones, default entry **"Random"**),
    Snooze toggle + snooze duration.
  - Full-screen in-app ringing view: current time, alarm label, tone name,
    **slide-to-dismiss** control (the in-app swipe), Snooze button.
- **Domain (Kotlin, pure, unit-tested)**
  - `Alarm` model: id, time (hour/minute), repeat days set, label, tone
    (`Random` or pinned tone id), snooze enabled + duration (default 9 min,
    configurable 1–30 min), enabled flag.
  - `NextFireCalculator`: given alarm + now → next fire instant (handles
    one-shot vs repeating, DST via `kotlinx-datetime`).
  - `ToneRandomizer`: picks uniformly from bundled tones excluding the last
    tone played for that alarm; persists last-played per alarm.
  - `AlarmScheduler`: orchestrates — on any change (create/edit/toggle/
    dismiss/snooze) recomputes and instructs the engine to (re)schedule.
- **Persistence**: SQLDelight, single `alarms` table + `settings` table
  (stores e.g. last random tone per alarm).
- **`AlarmEngine` interface (expect/actual boundary)** — implemented in Swift,
  exposed to Kotlin:
  - `schedule(alarmId, fireAt, toneId, label, snoozeConfig)`
  - `cancel(alarmId)`
  - `stopRinging(alarmId)` / `snooze(alarmId)`
  - callbacks: `onAlarmFired`, `onDismissed`, `onSnoozed` (from notification
    actions / Live Activity intents back into Kotlin).

### 3.2 Module: `iosApp` (Swift, thin)

- **AlarmEngine implementation:**
  - **Backup notifications**: for each scheduled alarm, a chain of up to 6
    `UNUserNotificationCenter` requests 30 s apart (each with the chosen tone
    trimmed ≤30 s), with `Dismiss` and `Snooze` actions — this covers the
    force-quit / audio-failure case.
  - **Foreground/background audio ringer**: maintains a playback
    `AVAudioSession`. A silent keep-alive strategy keeps the app schedulable;
    at fire time plays the tone in a loop at full volume. Observes
    `outputVolume` via KVO and resets volume (MPVolumeView slider technique)
    when the user presses volume-down while ringing.
  - **Live Activity control**: starts an `Activity` when ringing begins, ends
    it on dismiss/snooze.
- **App Intents** (`DismissAlarmIntent`, `SnoozeAlarmIntent`) — invoked by
  Live Activity buttons and notification actions; forward to Kotlin via the
  engine callbacks.

### 3.3 Module: `AlarmWidget` (Swift, WidgetKit extension)

- SwiftUI Live Activity: alarm label, time, large **Dismiss** button
  (destructive style, requires deliberate tap) and **Snooze** button, wired to
  the App Intents. Also renders Dynamic Island states.

## 4. Key flows

### 4.1 Ringing & dismissal
1. Engine fire time reached → audio ringer starts looping the selected/random
   tone; Live Activity appears on lock screen; backup notification chain fires.
2. User presses power/volume → screen may lock / system volume changes, but
   ringer continues (volume reset counteracts) — **alarm keeps ringing**.
3. User dismisses via: Live Activity **Dismiss** button, notification
   **Dismiss** action, or in-app **slide-to-dismiss**.
4. On dismiss: audio stops, Live Activity ends, remaining backup notifications
   cancelled, Kotlin recomputes next occurrence (repeating) or disables
   (one-shot), random tone history updated.

### 4.2 Snooze
Same surfaces offer **Snooze** → ringer stops, new fire scheduled at
now + snooze duration, Live Activity switches to "snoozed until HH:MM" state.
A snoozed ring re-randomizes the tone (still avoiding the previous one).

### 4.3 Random tone
At each schedule/fire, if alarm tone == `Random`: pick from bundled tones
(≥8 royalty-free tones bundled) excluding that alarm's last-played tone;
record choice. Pinned tone → always that tone.

## 5. Error handling

- **Notification permission denied** → prominent in-app banner; alarms still
  ring if app alive, but warn user the backup path is dead.
- **Audio session interrupted** (phone call etc.) → rely on notification
  chain; resume ringer after interruption if still undismissed.
- **App force-quit** → notification chain only; on next launch, reconcile: any
  alarm whose fire time passed while dead is marked missed (shown subtly) and
  rescheduled to next occurrence.
- **Device reboot** → notifications persist (system), audio engine does not;
  reconcile on next launch.
- **Live Activity unavailable/denied** → notification actions remain the
  lock-screen dismissal path.

## 6. Testing

- **Kotlin unit tests** (primary, TDD): `NextFireCalculator` (one-shot,
  repeat, DST, midnight edges), `ToneRandomizer` (no immediate repeat,
  pinned-tone bypass, single-tone degenerate case), `AlarmScheduler`
  (reschedule on toggle/edit/snooze/dismiss), persistence round-trips.
- **Swift engine**: minimal logic by design; notification-request builder unit
  tested (XCTest); audio/volume behavior verified manually on device.
- **On-device manual checklist** (required for each milestone): ring while
  locked, power press during ring, volume press during ring, dismiss from
  Live Activity, dismiss from notification, snooze, force-quit fallback,
  silent-switch behavior.

## 7. Milestones

1. **M0 — Toolchain**: Xcode installed, KMP/Compose scaffold builds, blank app
   runs on the physical iPhone.
2. **M1 — UI + domain**: Clock-style alarm list + add/edit sheet, persistence,
   full unit-tested scheduling/randomizer logic. Alarms fire as plain local
   notifications only.
3. **M2 — Engine**: Swift audio ringer with volume-reset, notification chain
   with actions, in-app ringing screen with slide-to-dismiss.
4. **M3 — Lock-screen UX**: Live Activity card with Dismiss/Snooze via App
   Intents, Dynamic Island states, snoozed state.
5. **M4 — Polish**: random-tone default UX, missed-alarm reconciliation,
   settings, visual fidelity pass against the native Clock app.

## 8. Out of scope

- Android target (KMP keeps the door open; not built now).
- App Store distribution (personal device install via free profile;
  note: free profiles expire after 7 days and need re-deploy).
- Bedtime/sleep tracking, world clock, stopwatch, timer tabs.
- iOS 26 AlarmKit integration (future enhancement if device upgrades).
