package com.genesiscruz.adwarehuli.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.genesiscruz.adwarehuli.AdwareHuliApp
import com.genesiscruz.adwarehuli.AppContainer

@Composable
fun rememberAppContainer(): AppContainer {
    val context = LocalContext.current.applicationContext as AdwareHuliApp
    return context.container
}
