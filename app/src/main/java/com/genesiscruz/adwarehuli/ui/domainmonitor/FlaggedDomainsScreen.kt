@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.genesiscruz.adwarehuli.ui.domainmonitor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.genesiscruz.adwarehuli.R
import com.genesiscruz.adwarehuli.ui.components.AppIcon
import com.genesiscruz.adwarehuli.ui.components.DomainCategoryChip
import com.genesiscruz.adwarehuli.ui.rememberAppContainer

@Composable
fun FlaggedDomainsScreen(onOpenAppDetail: (String) -> Unit, onBack: () -> Unit) {
    val container = rememberAppContainer()
    val viewModel: FlaggedDomainsViewModel = viewModel(
        factory = viewModelFactory { initializer { FlaggedDomainsViewModel(container.domainHitRepository) } }
    )
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.flagged_domains_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
            if (state.groups.isEmpty()) {
                item { Text(stringResource(R.string.flagged_domains_empty), style = MaterialTheme.typography.bodyLarge) }
            } else {
                items(state.groups) { group -> FlaggedAppGroupCard(group, onClick = { onOpenAppDetail(group.packageName) }) }
            }
        }
    }
}

@Composable
private fun FlaggedAppGroupCard(group: FlaggedAppGroup, onClick: () -> Unit) {
    val container = rememberAppContainer()
    val label by produceState(initialValue = group.packageName, group.packageName) {
        value = container.packageInfoProvider.getLabel(group.packageName)
    }
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), onClick = onClick) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(group.packageName)
                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(label, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.flagged_domains_count, group.hits.size),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            group.hits.take(5).forEach { hit ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(hit.domain, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    DomainCategoryChip(hit.category, hit.isFlagged)
                }
            }
        }
    }
}
