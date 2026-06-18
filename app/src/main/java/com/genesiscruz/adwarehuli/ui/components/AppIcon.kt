package com.genesiscruz.adwarehuli.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.genesiscruz.adwarehuli.AdwareHuliApp
import com.genesiscruz.adwarehuli.R

@Composable
fun AppIcon(packageName: String, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val app = LocalContext.current.applicationContext as AdwareHuliApp
    val bitmap by produceState<ImageBitmap?>(initialValue = null, packageName) {
        value = app.container.packageInfoProvider.getIcon(packageName)
            ?.toBitmap(width = 96, height = 96)
            ?.asImageBitmap()
    }

    Box(
        modifier = modifier
            .size(size)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = null,
                modifier = Modifier.size(size),
                contentScale = ContentScale.Fit
            )
        } else {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(size)
            )
        }
    }
}
