<img src="header.png" alt="AnkiLoop header banner" width="100%">

# AnkiLoop

*I do one flashcard every X seconds.*

> Originally forked from [FlorentRevest/microanki](https://github.com/FlorentRevest/microanki)
> and since rebranded to **AnkiLoop** (`com.ankiloop`): instead of showing a
> card when you open a chosen app, AnkiLoop shows a card **every X seconds**
> (interval set by you in settings) via a foreground timer service.

Practise vocabulary **on a steady rhythm**: every X seconds AnkiLoop pops up
one flashcard from a deck you choose — drawn straight from your real
[AnkiDroid](https://github.com/ankidroid/Anki-Android) collection — whenever
your screen is on and unlocked.

It uses the
[AnkiDroid API / database ContentProvider](https://github.com/ankidroid/apisample)
to fetch cards that are actually due and to report your answer back, so your
normal spaced-repetition scheduling keeps working.

## How it works

1. You pick a deck and set **seconds between cards** (minimum 10s, default
   300s / 5min), then press **Start timer**.
2. A foreground service (`CardTimerService`) waits for the interval, then shows
   just the **flashcard** on top: question → *Show answer* → grade it
   (Again / Hard / Good / Easy) — but only if the screen is on and the
   device is unlocked. Ticks that fire while the screen is off or locked are
   skipped, so cards never wake the phone or pile up on the lock screen.
   No per-card push notifications — only the flashcard itself.
3. Grading is sent to AnkiDroid, which reschedules the card. The timer keeps
   running until you press **Stop timer** (ongoing notification also has a
   Stop action). It restarts after reboot if left enabled.

## Setup (in the app)

Open AnkiLoop and work down the checklist:

1. **Permissions**
   - Install **AnkiDroid** (if you haven't).
   - Grant AnkiLoop access to your AnkiDroid collection.
   - Allow **Display over other apps**.
   - Allow **Notifications** (only for the persistent timer status Android
     requires for foreground services — no per-card pushes).
   - Optionally **Ignore battery optimizations** so the timer isn't killed.
2. **Deck** — pick the deck you want to practise.
3. **Card interval** — set seconds between cards, use presets (30s / 1m / 5m /
   15m), then Start/Stop the timer.
4. **Options** — whether the Back button is blocked until you answer, and
   whether grade buttons show the next review time.
5. **Card theme** — pick from 12+ looks (Minimal, pastels, gradients, paper,
   forest…) with a live preview.

Tip: use *Show a card now* to test without waiting for the timer.

## Building

Requires the Android SDK and JDK 17+.

```bash
./gradlew assembleDebug      # build app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug       # build + install on a connected device
```

The AnkiDroid API dependency (`com.github.ankidroid:Anki-Android:api-v1.1.0`) is
pulled from JitPack — see `settings.gradle.kts`.

| | |
|---|---|
| Language | Kotlin + Jetpack Compose |
| minSdk / targetSdk | 26 / 35 |
| AnkiDroid API | `api-v1.1.0` |
| Trigger | Timer every X seconds (`CardTimerService`) |
| Package | `com.ankiloop` |

## Privacy

Everything runs on-device. AnkiLoop never reads the content of your screen
and never sends anything off the device.

## History

- Upstream: accessibility service showing a card on app-open.
- AnkiLoop: timer service with user-configurable `intervalSeconds`, 12+ card
  themes, hideable review times, boot restart, Start/Stop UI.
