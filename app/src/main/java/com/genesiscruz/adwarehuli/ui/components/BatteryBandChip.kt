package com.genesiscruz.adwarehuli.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.genesiscruz.adwarehuli.R
import com.genesiscruz.adwarehuli.domain.model.BatteryBand
import com.genesiscruz.adwarehuli.domain.model.BatteryReason
import com.genesiscruz.adwarehuli.ui.theme.RiskGreen
import com.genesiscruz.adwarehuli.ui.theme.RiskRed
import com.genesiscruz.adwarehuli.ui.theme.RiskYellow

@Composable
fun BatteryBandChip(band: BatteryBand, modifier: Modifier = Modifier) {
    val (color, labelRes) = when (band) {
        BatteryBand.HIGH -> RiskRed to R.string.battery_band_high
        BatteryBand.MEDIUM -> RiskYellow to R.string.battery_band_medium
        BatteryBand.LOW -> RiskGreen to R.string.battery_band_low
    }
    Text(
        text = stringResource(labelRes),
        color = Color.White,
        style = MaterialTheme.typography.labelLarge,
        modifier = modifier
            .background(color, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

fun BatteryReason.labelRes(): Int = when (this) {
    BatteryReason.HIGH_FOREGROUND_TIME -> R.string.battery_reason_foreground_time
    BatteryReason.NOT_OPTIMIZED -> R.string.battery_reason_not_optimized
    BatteryReason.BOOT_AUTOSTART -> R.string.battery_reason_autostart
}
