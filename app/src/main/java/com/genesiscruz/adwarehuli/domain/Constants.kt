package com.genesiscruz.adwarehuli.domain

/**
 * Every tunable threshold and weight lives here so the detection heuristics
 * can be adjusted without hunting through the codebase.
 */
object Constants {

    // --- Culprit Monitor ---
    const val POLL_INTERVAL_MS = 1500L
    const val POLL_OVERLAP_MS = 2000L
    const val REDIRECT_WINDOW_MS = 2500L

    val KNOWN_BROWSER_PACKAGES = setOf(
        "com.android.chrome",
        "com.chrome.beta",
        "com.chrome.dev",
        "com.chrome.canary",
        "org.mozilla.firefox",
        "org.mozilla.firefox_beta",
        "org.mozilla.focus",
        "com.opera.browser",
        "com.opera.browser.beta",
        "com.opera.mini.native",
        "com.brave.browser",
        "com.microsoft.emmx",
        "com.duckduckgo.mobile.android",
        "com.sec.android.app.sbrowser",
        "com.android.browser",
        "com.UCMobile.intl",
        "mark.via.gp",
        "com.vivaldi.browser",
        "com.kiwibrowser.browser",
        "com.google.android.webview",
        "com.android.htmlviewer",
        // Custom Tabs / trusted web activity hosts commonly used to render redirects
        "com.google.android.gms"
    )

    // --- Risk Scanner ---
    const val RECENT_INSTALL_DAYS = 14

    object RiskWeights {
        const val OVERLAY = 35
        const val ACCESSIBILITY = 35
        const val HIDDEN_APP = 40
        const val SIDELOADED = 20
        const val RECENT_INSTALL = 15
        const val BOOT_AUTOSTART = 10
        const val AD_NETWORK_PERMS = 5
    }

    // Score thresholds map a 0..max total onto a band.
    const val RISK_BAND_RED_THRESHOLD = 55
    const val RISK_BAND_YELLOW_THRESHOLD = 25

    // --- Combined Verdict ---
    const val CONFIRMED_CULPRIT_MIN_REDIRECTS = 1
    // An app needs at least one of these "high" signals plus a redirect to be confirmed.
    val HIGH_RISK_SIGNAL_WEIGHTS = listOf(
        RiskWeights.OVERLAY,
        RiskWeights.ACCESSIBILITY,
        RiskWeights.HIDDEN_APP
    )
    const val CONFIRMED_CULPRIT_MIN_SCORE = RISK_BAND_YELLOW_THRESHOLD
}
