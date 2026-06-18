@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.genesiscruz.adwarehuli.ui.scanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.genesiscruz.adwarehuli.R
import com.genesiscruz.adwarehuli.domain.model.AppRiskInfo
import com.genesiscruz.adwarehuli.ui.components.AppIcon
import com.genesiscruz.adwarehuli.ui.components.RiskBandChip
import com.genesiscruz.adwarehuli.ui.components.labelRes
import com.genesiscruz.adwarehuli.ui.rememberAppContainer

@Composable
fun ScannerScreen(onOpenAppDetail: (String) -> Unit) {
    val container = rememberAppContainer()
    val viewModel: ScannerViewModel = viewModel(
        factory = viewModelFactory { initializer { ScannerViewModel(container.appRiskRepository) } }
    )
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.scanner_title)) }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Button(
                    onClick = { viewModel.runScan() },
                    enabled = !state.isScanning,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.isScanning) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp).size(18.dp), strokeWidth = 2.dp)
                        Text(stringResource(R.string.scanner_scanning))
                    } else {
                        Text(stringResource(R.string.scanner_scan_button))
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.scanner_show_system))
                    Switch(checked = state.showSystemApps, onCheckedChange = { viewModel.setShowSystemApps(it) })
                }
            }

            if (state.visibleApps.isEmpty()) {
                Text(
                    stringResource(R.string.scanner_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                    items(state.visibleApps, key = { it.packageName }) { app ->
                        RiskAppRow(app, onClick = { onOpenAppDetail(app.packageName) })
                    }
                }
            }
        }
    }
}

@Composable
private fun RiskAppRow(app: AppRiskInfo, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), onClick = onClick) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AppIcon(app.packageName)
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(app.label, style = MaterialTheme.typography.bodyLarge)
                Text(app.packageName, style = MaterialTheme.typography.labelLarge)
                app.reasons.take(2).forEach { reason ->
                    Text("• " + stringResource(reason.labelRes()), style = MaterialTheme.typography.labelLarge)
                }
            }
            RiskBandChip(app.band)
        }
    }
}
