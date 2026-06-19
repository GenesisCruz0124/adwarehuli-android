package com.genesiscruz.adwarehuli.ui.navigation

object Routes {
    const val ONBOARDING = "onboarding"
    const val DASHBOARD = "dashboard"
    const val MONITOR = "monitor"
    const val SCANNER = "scanner"
    const val DOMAIN_MONITOR = "domain_monitor"
    const val FLAGGED_DOMAINS = "flagged_domains"
    const val APP_DETAIL = "app_detail/{packageName}"

    fun appDetail(packageName: String) = "app_detail/$packageName"
}
