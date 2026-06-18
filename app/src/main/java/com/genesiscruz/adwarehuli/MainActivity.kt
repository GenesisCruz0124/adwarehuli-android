package com.genesiscruz.adwarehuli

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.genesiscruz.adwarehuli.ui.navigation.MainScaffold
import com.genesiscruz.adwarehuli.ui.theme.AdwareHuliTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as AdwareHuliApp).container

        setContent {
            AdwareHuliTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScaffold(permissionChecker = container.permissionChecker)
                }
            }
        }
    }
}
