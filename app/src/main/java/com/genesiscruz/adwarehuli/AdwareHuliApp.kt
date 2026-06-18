package com.genesiscruz.adwarehuli

import android.app.Application

class AdwareHuliApp : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
