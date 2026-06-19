@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.genesiscruz.adwarehuli.ui.dashboard

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.genesiscruz.adwarehuli.R
import com.genesiscruz.adwarehuli.domain.model.CulpritVerdict
import com.genesiscruz.adwarehuli.service.CulpritMonitorService
import com.genesiscruz.adwarehuli.ui.components.AppIcon
import com.genesiscruz.adwarehuli.ui.rememberAppContainer

@Composable
fun DashboardScreen(
    onRunScan: () -> Unit,
    onOpenMonitor: () -> Unit,
    onOpenDomainMonitor: () -> Unit,
    onOpenAppDetail: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val container = rememberAppContainer()
    val viewModel: DashboardViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                DashboardViewModel(
                    container.redirectEventRepository,
                    container.appRiskRepository,
                    container.domainHitRepository,
                    container.computeVerdictsUseCase
                )
            }
        }
    )
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.dashboard_title)) }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                StatusCard(
                    isMonitoring = state.isMonitoring,
                    isDomainMonitoring = state.isDomainMonitoring,
                    lastScanTime = state.lastScanTime,
                    confirmedCount = state.confirmedCulprits.size
                )
            }
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            if (state.isMonitoring) CulpritMonitorService.stop(context) else CulpritMonitorService.start(context)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(if (state.isMonitoring) R.string.dashboard_stop_monitor else R.string.dashboard_start_monitor))
                    }
                    OutlinedButton(onClick = onRunScan, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.dashboard_run_scan))
                    }
                }
            }
            item {
                Button(onClick = onOpenMonitor, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Text(stringResource(R.string.nav_monitor))
                }
            }
            item {
                OutlinedButton(onClick = onOpenDomainMonitor, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Text(stringResource(R.string.nav_domain_monitor))
                }
            }
            item {
                Text(stringResource(R.string.dashboard_confirmed_section), style = MaterialTheme.typography.titleLarge)
            }
            if (state.confirmedCulprits.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.dashboard_no_confirmed),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            } else {
                items(state.confirmedCulprits) { verdict ->
                    ConfirmedCulpritCard(verdict, onClick = { onOpenAppDetail(verdict.packageName) })
                }
            }
        }
    }
}

@Composable
private fun StatusCard(isMonitoring: Boolean, isDomainMonitoring: Boolean, lastScanTime: Long?, confirmedCount: Int) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(if (isMonitoring) R.string.dashboard_monitor_on else R.string.dashboard_monitor_off),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                stringResource(
                    if (isDomainMonitoring) R.string.dashboard_domain_monitor_on else R.string.dashboard_domain_monitor_off
                ),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                if (lastScanTime != null) {
                    stringResource(R.string.dashboard_last_scan, DateUtils.getRelativeTimeSpanString(lastScanTime).toString())
                } else {
                    stringResource(R.string.dashboard_last_scan_never)
                },
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                stringResource(R.string.dashboard_confirmed_count, confirmedCount),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun ConfirmedCulpritCard(verdict: CulpritVerdict, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        onClick = onClick
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            AppIcon(verdict.packageName)
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(verdict.label, style = MaterialTheme.typography.bodyLarge)
                Text(verdict.explanation, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
