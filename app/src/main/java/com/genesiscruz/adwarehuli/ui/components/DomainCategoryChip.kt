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
import com.genesiscruz.adwarehuli.domain.model.DomainCategory
import com.genesiscruz.adwarehuli.ui.theme.RiskRed
import com.genesiscruz.adwarehuli.ui.theme.RiskYellow

fun DomainCategory.labelRes(): Int = when (this) {
    DomainCategory.GAMBLING -> R.string.domain_category_gambling
    DomainCategory.AD_NETWORK -> R.string.domain_category_ad_network
    DomainCategory.MALVERTISING -> R.string.domain_category_malvertising
    DomainCategory.UNCATEGORIZED -> R.string.domain_category_uncategorized
}

@Composable
fun DomainCategoryChip(category: DomainCategory, isFlagged: Boolean, modifier: Modifier = Modifier) {
    val color = if (isFlagged) {
        when (category) {
            DomainCategory.GAMBLING, DomainCategory.MALVERTISING -> RiskRed
            DomainCategory.AD_NETWORK -> RiskYellow
            DomainCategory.UNCATEGORIZED -> RiskYellow
        }
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (isFlagged) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        text = stringResource(category.labelRes()),
        color = textColor,
        style = MaterialTheme.typography.labelLarge,
        modifier = modifier
            .background(color, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}
