<img src="docs/icon.png" alt="MicroAnki app icon: a buff cartoon guy holding up one finger next to a speech bubble with a star in it" width="128" align="right">

# MicroAnki — Timer Fork

*I do one flashcard every X seconds.*

> Fork of [FlorentRevest/microanki](https://github.com/FlorentRevest/microanki).
> Instead of showing a card when you open a chosen app, this fork shows a card
> **every X seconds** (interval set by you in settings) via a foreground timer
> service.

Practise vocabulary **on a steady rhythm**: every X seconds MicroAnki pops up
one flashcard from a deck you choose — drawn straight from your real
[AnkiDroid](https://github.com/ankidroid/Anki-Android) collection — no matter
what you are doing.

It uses the
[AnkiDroid API / database ContentProvider](https://github.com/ankidroid/apisample)
to fetch cards that are actually due and to report your answer back, so your
normal spaced-repetition scheduling keeps working.

## How it works

1. You pick a deck and set **seconds between cards** (minimum 10s, default
   300s / 5min), then press **Start timer**.
2. A foreground service (`CardTimerService`) waits for the interval, then shows
   a full-screen **flashcard** on top: question → *Show answer* → grade it
   (Again / Hard / Good / Easy). It uses a direct launch plus a full-screen
   intent notification so the card appears even from the background on
   Android 10+.
3. Grading is sent to AnkiDroid, which reschedules the card. The timer keeps
   running until you press **Stop timer** (ongoing notification also has a
   Stop action). It restarts after reboot if left enabled.

## Setup (in the app)

Open MicroAnki and work down the checklist:

1. **Permissions**
   - Install **AnkiDroid** (if you haven't).
   - Grant MicroAnki access to your AnkiDroid collection.
   - Allow **Display over other apps**.
   - Allow **Notifications** (used for timer cards).
   - Optionally **Ignore battery optimizations** so the timer isn't killed.
2. **Deck** — pick the deck you want to practise.
3. **Card interval** — set seconds between cards, use presets (30s / 1m / 5m /
   15m), then Start/Stop the timer.
4. **Options** — whether the Back button is blocked until you answer.

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

## Privacy

Everything runs on-device. MicroAnki never reads the content of your screen
and never sends anything off the device.

## Differences from upstream

- Removed: accessibility service (`AppMonitorService`), trigger-app picker,
  per-open cooldown.
- Added: `CardTimerService` foreground service with user-configurable
  `intervalSeconds`, full-screen intent cards, boot restart, Start/Stop UI.
