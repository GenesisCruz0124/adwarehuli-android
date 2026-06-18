@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.genesiscruz.adwarehuli.ui.monitor

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.genesiscruz.adwarehuli.R
import com.genesiscruz.adwarehuli.domain.model.CulpritTally
import com.genesiscruz.adwarehuli.domain.model.RedirectEvent
import com.genesiscruz.adwarehuli.service.CulpritMonitorService
import com.genesiscruz.adwarehuli.ui.components.AppIcon
import com.genesiscruz.adwarehuli.ui.rememberAppContainer

@Composable
fun MonitorScreen(onOpenAppDetail: (String) -> Unit) {
    val context = LocalContext.current
    val container = rememberAppContainer()
    val viewModel: MonitorViewModel = viewModel(
        factory = viewModelFactory { initializer { MonitorViewModel(container.redirectEventRepository) } }
    )
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.monitor_title)) }) }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            stringResource(if (state.isMonitoring) R.string.monitor_status_on else R.string.monitor_status_off),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            stringResource(R.string.monitor_events_session, state.eventsThisSession),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Button(
                            onClick = {
                                if (state.isMonitoring) CulpritMonitorService.stop(context) else CulpritMonitorService.start(context)
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                        ) {
                            Text(stringResource(if (state.isMonitoring) R.string.monitor_stop else R.string.monitor_start))
                        }
                    }
                }
            }

            item {
                Text(
                    stringResource(R.string.monitor_leaderboard_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
                )
            }
            if (state.leaderboard.isEmpty()) {
                item { Text(stringResource(R.string.monitor_leaderboard_empty), style = MaterialTheme.typography.bodyLarge) }
            } else {
                items(state.leaderboard) { tally -> LeaderboardRow(tally, onClick = { onOpenAppDetail(tally.suspectPackage) }) }
            }

            item {
                Text(
                    stringResource(R.string.monitor_history_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
                )
            }
            if (state.history.isEmpty()) {
                item { Text(stringResource(R.string.monitor_history_empty), style = MaterialTheme.typography.bodyLarge) }
            } else {
                items(state.history) { event -> HistoryRow(event) }
            }
        }
    }
}

@Composable
private fun LeaderboardRow(tally: CulpritTally, onClick: () -> Unit) {
    val container = rememberAppContainer()
    val label by produceState(initialValue = tally.suspectPackage, tally.suspectPackage) {
        value = container.packageInfoProvider.getLabel(tally.suspectPackage)
    }
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), onClick = onClick) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AppIcon(tally.suspectPackage)
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(label, style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.monitor_last_seen, DateUtils.getRelativeTimeSpanString(tally.lastSeen).toString()),
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Text(stringResource(R.string.monitor_redirect_count, tally.redirectCount), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun HistoryRow(event: RedirectEvent) {
    val context = LocalContext.current
    val container = rememberAppContainer()
    val suspectLabel by produceState(initialValue = event.suspectPackage, event.suspectPackage) {
        value = container.packageInfoProvider.getLabel(event.suspectPackage)
    }
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(suspectLabel, style = MaterialTheme.typography.bodyLarge)
                Text(
                    DateUtils.formatDateTime(
                        context,
                        event.timestamp,
                        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME
                    ),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
