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
import com.genesiscruz.adwarehuli.domain.model.RiskBand
import com.genesiscruz.adwarehuli.ui.theme.RiskGreen
import com.genesiscruz.adwarehuli.ui.theme.RiskRed
import com.genesiscruz.adwarehuli.ui.theme.RiskYellow

@Composable
fun RiskBandChip(band: RiskBand, modifier: Modifier = Modifier) {
    val (color, labelRes) = when (band) {
        RiskBand.RED -> RiskRed to R.string.band_red
        RiskBand.YELLOW -> RiskYellow to R.string.band_yellow
        RiskBand.GREEN -> RiskGreen to R.string.band_green
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
