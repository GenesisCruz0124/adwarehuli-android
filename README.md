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

### 3. Domain Monitor (Phase 2)

An optional, local-only `VpnService` that captures the device's DNS lookups,
attributes each one to the app that made it, and flags lookups against a
bundled list of gambling/casino, ad-network, and malvertising domains. It
never inspects HTTPS content, never installs a certificate, and never sends
anything off the device — see "How the Domain Monitor works" below for the
full technical picture and its limitations.

### Combined verdict

An app is marked a **Confirmed Culprit** when EITHER:

- it has a meaningful number of observed redirects *and* a high risk score, or
- the Domain Monitor observed a flagged domain lookup shortly before one of
  its redirects — direct evidence that's treated as confirming on its own.

The Dashboard surfaces these with a plain-language explanation, e.g.
*"Triggered the browser 5x and can draw over other apps — likely the source
of the ads."* or, when a domain correlation exists, *"Looked up
bet888casino.xyz (gambling) 0.4s before the browser opened — confirmed
source of the ads."*

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

### Granting VPN consent for the Domain Monitor

The Domain Monitor needs Android's standard VPN consent dialog the first
time you start it (and again if you ever revoke it from
`Settings > Network > VPN`):

1. Open **Domain Monitor** from the Dashboard and tap **Start**.
2. Android shows a system dialog: *"AdwareHuli wants to set up a VPN
   connection that allows it to monitor network traffic."* Tap **OK**.
3. A persistent notification appears while the monitor runs — this is
   required by Android for any foreground VPN service and cannot be hidden.
4. If you tap **Cancel** instead, the screen shows a message explaining
   consent was denied and the monitor stays off; the app does not crash or
   retry automatically.
5. If another VPN app is already connected, Android only allows one VPN at
   a time — the screen tells you to disconnect it first.

## How the Domain Monitor works

- It runs a `VpnService` whose virtual network interface is scoped to route
  **only** the on-device DNS sentinel address through it (a `/32` route),
  not all device traffic. Every other packet — including all HTTPS
  traffic — bypasses this process entirely at the OS routing layer.
- It reads raw UDP/53 (DNS) packets from that interface, parses the queried
  hostname, looks up which app/UID issued the request
  (`ConnectivityManager.getConnectionOwnerUid`, Android 10+ only), checks
  the hostname against a bundled categorized blocklist, records the result,
  and forwards the query untouched to a real upstream resolver (Cloudflare
  `1.1.1.1` by default) using a `protect()`-ed socket so the forwarding
  traffic itself can't loop back into the VPN.
- The response is written back into the tun interface so your internet
  connection keeps working normally throughout.
- **Blocking is an explicit, separate toggle, off by default.** When off,
  flagged lookups are still forwarded normally — only recorded. When turned
  on, flagged lookups get an NXDOMAIN response instead.
- The bundled blocklists live under `app/src/main/assets/blocklist/` as
  plain-text files (one domain per line, `#` for comments), split into
  `gambling.txt`, `ad_networks.txt`, and `malvertising.txt`. Matching is
  exact-domain or subdomain-suffix (e.g. blocking `bet888casino.xyz` also
  matches `www.bet888casino.xyz`). There is no auto-update mechanism by
  design — lists are bundled with the app or replaced manually.

### How to read the correlation verdict

When a `DomainHit` for an app's flagged domain occurs within 3 seconds
before one of that app's Phase 1 redirect events, the two are joined into a
**Domain Monitor Correlation** — shown on both the Dashboard's Confirmed
Culprit cards and the App Detail screen's Network Activity section. This is
treated as direct evidence (the app looked up a known gambling/ad domain
right before the browser opened) and is enough on its own to mark the app a
Confirmed Culprit, even without a high risk score.

### Domain Monitor limitations

- **Domain names only.** It reads DNS queries and nothing else — no page
  content, no HTTPS payload, no credentials. There is no certificate
  installed and no man-in-the-middle interception of any kind.
- **One VPN at a time.** Android only allows a single active VPN
  connection; if another VPN app is running, you must disconnect it first.
- **App attribution needs Android 10 (API 29) or newer.** On older versions
  every lookup is attributed to "unknown" instead of crashing.
- **TCP/443 (SNI) sniffing is out of scope for this build.** Because the
  VPN only routes the DNS sentinel address (not a full `0.0.0.0/0` route),
  HTTPS connection attempts never reach this process — by design, to avoid
  building a full transparent TCP/IP proxy.
- **IPv6 is best-effort only;** the packet parsing in this build targets
  IPv4.

## How to read the verdict

- **Dashboard** — quick status (monitoring on/off, last scan time) and the
  list of Confirmed Culprits, if any.
- **Monitor tab** — start/stop the live redirect monitor, see the
  leaderboard of repeat offenders, and a chronological history of redirect
  events.
- **Scanner tab** — run a risk scan of all installed apps, ranked by risk
  band, with a toggle to include/exclude system apps.
- **Domain Monitor** — start/stop the local VPN, see connection status and
  any consent/another-VPN errors, a live feed of captured DNS lookups
  (flagged ones highlighted red), the blocking toggle, and a link to the
  **Flagged Domains** view (flagged lookups grouped by app).
- **App Detail** (tap any app) — full picture: label, icon, package,
  version, install source/date, the complete permission list (risky ones
  highlighted), redirect history, risk band with reasons, this app's
  Network Activity (its domain lookups and any Domain Monitor
  correlations), and one-tap Uninstall / Open App Info actions.

A RED band plus repeated redirects is the strongest signal — that's a
Confirmed Culprit. A YELLOW band with no observed redirects just means
"worth a second look," not "guilty."

## Limitations (by design, for this version)

- No HTTPS content inspection, MITM, or certificate installation — the
  Domain Monitor only ever sees domain names.
- No live AccessibilityService-based interception.
- No cloud sync, accounts, or backend of any kind.
- No remote/auto-updating blocklists — domain lists are bundled with the
  app or replaced manually from local storage.
- No share/export of reports (planned for a future update).

## Building

```
./gradlew :app:assembleDebug
```

Requires the Android SDK with platform 35 and build-tools 35.0.0 installed,
and `local.properties` pointing `sdk.dir` at it. minSdk 24, targetSdk 35.
