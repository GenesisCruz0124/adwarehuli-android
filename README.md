# AdwareHuli

AdwareHuli is a native Android diagnostic tool for phone technicians and power
users. It helps answer one question: **"Which app on this phone is causing
the browser redirects / casino pop-ups / spammy ads?"**

It does this entirely on-device — there is no backend, no analytics, no
network calls, and no third-party tracking SDK anywhere in the app. It is
built for sideloading in a repair shop, not for Play Store distribution.

## What it does

AdwareHuli combines two independent engines and cross-references their
output into a single verdict per app:

### 1. Culprit Monitor

A foreground service polls Android's `UsageStatsManager` every 1–2 seconds
to see which app was in the foreground immediately before the device's
browser or launcher came to the front. If a non-browser app is followed by
a browser launch within a short window (default 2.5s), that's logged as a
**redirect event** tied to the suspect app. Over time this builds a
leaderboard of apps that keep triggering browser opens.

### 2. Risk Scanner

A one-tap scan enumerates every installed app and scores it using signals
such as:

- Draws over other apps (`SYSTEM_ALERT_WINDOW`)
- Uses Accessibility services
- Hidden from the launcher (no visible icon — a common adware trick)
- Installed from outside the Play Store (sideloaded)
- Recently installed
- Auto-starts on boot
- Holds broad ad/network permissions

Each app is scored and bucketed into a **GREEN / YELLOW / RED** risk band.
All weights and thresholds live in a single `Constants.kt` file so they can
be tuned without touching the scoring logic.

### 3. Battery Usage

A one-tap scan ranks installed apps by estimated battery impact. Real
per-app battery consumption (mAh) requires the privileged
`android.permission.BATTERY_STATS`, which Android reserves for
system/signature apps — third-party apps like this one can't read it. So
this is a heuristic estimate built from data the app *is* allowed to read:

- Foreground time over the last 24h (`UsageStatsManager`)
- Whether the app is exempt from battery optimization (`PowerManager`)
- Auto-starts on boot

Each app gets a **HIGH / MEDIUM / LOW** battery-impact band. Tapping an app
offers shortcuts into the system's App Settings or Battery Settings screens
— Android does not let one third-party app force-stop another, so
"optimizing" an app means handing the user to the right system screen
rather than claiming to kill its process outright.

### Combined verdict

An app is marked a **Confirmed Culprit** when it has a meaningful number of
observed redirects *and* a high risk score.

The Dashboard surfaces these with a plain-language explanation, e.g.
*"Triggered the browser 5x and can draw over other apps — likely the source
of the ads."*

## How to grant permissions

On first launch, AdwareHuli walks you through an onboarding checklist.
Scanning and monitoring stay disabled until the required items are granted:

1. **Usage Access** (required) — tap "Grant" to open
   `Settings > Apps > Special app access > Usage access`, then enable
   AdwareHuli. This is what lets the Monitor see which app was in the
   foreground.
2. **Query All Packages** (automatic) — granted automatically via a manifest
   permission; nothing to do here, it's shown for transparency.
3. **Notifications** (recommended on Android 13+) — needed so the
   foreground-service notification can be shown while monitoring is active.
4. **Resume monitoring on boot** (optional, off by default) — a toggle that
   lets the Culprit Monitor service restart automatically after the device
   reboots.

If Usage Access is revoked while the app is running (e.g. from device
Settings), AdwareHuli detects this and stops monitoring gracefully instead
of crashing.

## How to read the verdict

- **Dashboard** — quick status (monitoring on/off, last scan time) and the
  list of Confirmed Culprits, if any.
- **Monitor tab** — start/stop the live redirect monitor, see the
  leaderboard of repeat offenders, and a chronological history of redirect
  events.
- **Scanner tab** — run a risk scan of all installed apps, ranked by risk
  band, with a toggle to include/exclude system apps.
- **Battery tab** — run a battery-impact scan, ranked by estimated impact
  band, with shortcuts to each app's system settings.
- **App Detail** (tap any app) — full picture: label, icon, package,
  version, install source/date, the complete permission list (risky ones
  highlighted), redirect history, risk band with reasons, and one-tap
  Uninstall / Open App Info actions.

A RED band plus repeated redirects is the strongest signal — that's a
Confirmed Culprit. A YELLOW band with no observed redirects just means
"worth a second look," not "guilty."

## Limitations (by design, for this version)

- No HTTPS content inspection, MITM, or certificate installation.
- No live AccessibilityService-based interception.
- No cloud sync, accounts, or backend of any kind.
- No share/export of reports (planned for a future update).

## Building

```
./gradlew :app:assembleDebug
```

Requires the Android SDK with platform 35 and build-tools 35.0.0 installed,
and `local.properties` pointing `sdk.dir` at it. minSdk 24, targetSdk 35.
