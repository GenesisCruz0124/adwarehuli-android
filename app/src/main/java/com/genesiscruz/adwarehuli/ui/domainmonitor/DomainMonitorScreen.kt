@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.genesiscruz.adwarehuli.ui.domainmonitor

import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.genesiscruz.adwarehuli.CrashLog
import com.genesiscruz.adwarehuli.R
import com.genesiscruz.adwarehuli.domain.model.DomainHit
import com.genesiscruz.adwarehuli.service.DomainMonitorPrefs
import com.genesiscruz.adwarehuli.service.DomainMonitorVpnService
import com.genesiscruz.adwarehuli.ui.components.AppIcon
import com.genesiscruz.adwarehuli.ui.components.DomainCategoryChip
import com.genesiscruz.adwarehuli.ui.rememberAppContainer

@Composable
fun DomainMonitorScreen(
    onOpenFlaggedDomains: () -> Unit,
    onOpenAppDetail: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val container = rememberAppContainer()
    val viewModel: DomainMonitorViewModel = viewModel(
        factory = viewModelFactory { initializer { DomainMonitorViewModel(container.domainHitRepository) } }
    )
    val state by viewModel.state.collectAsState()
    var blockingEnabled by remember { mutableStateOf(DomainMonitorPrefs.isBlockingEnabled(context)) }
    var lastCrash by remember { mutableStateOf(CrashLog.lastCrash(context)) }

    val consentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            DomainMonitorVpnService.start(context)
        }
    }

    fun startMonitor() {
        val consentIntent = DomainMonitorVpnService.prepareIntent(context)
        if (consentIntent != null) {
            consentLauncher.launch(consentIntent)
        } else {
            DomainMonitorVpnService.start(context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.domain_monitor_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
            lastCrash?.let { crash ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Last crash (screenshot this for support)", style = MaterialTheme.typography.titleSmall)
                            androidx.compose.foundation.text.selection.SelectionContainer {
                                Text(
                                    crash,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    CrashLog.clear(context)
                                    lastCrash = null
                                },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            ) { Text("Dismiss") }
                        }
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            stringResource(if (state.isRunning) R.string.domain_monitor_status_on else R.string.domain_monitor_status_off),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            stringResource(R.string.domain_monitor_hits_session, state.hitsThisSession),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        state.lastError?.let { error ->
                            Text(
                                stringResource(errorMessageRes(error)),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        Button(
                            onClick = {
                                if (state.isRunning) DomainMonitorVpnService.stop(context) else startMonitor()
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                        ) {
                            Text(stringResource(if (state.isRunning) R.string.domain_monitor_stop else R.string.domain_monitor_start))
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.domain_monitor_blocking_toggle), style = MaterialTheme.typography.bodyLarge)
                            Switch(
                                checked = blockingEnabled,
                                onCheckedChange = { enabled ->
                                    blockingEnabled = enabled
                                    DomainMonitorPrefs.setBlockingEnabled(context, enabled)
                                }
                            )
                        }
                        Text(
                            stringResource(R.string.domain_monitor_blocking_desc),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            item {
                OutlinedButton(onClick = onOpenFlaggedDomains, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                    Text(stringResource(R.string.domain_monitor_view_flagged))
                }
            }

            item {
                Text(
                    stringResource(R.string.domain_monitor_feed_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            if (state.recentHits.isEmpty()) {
                item { Text(stringResource(R.string.domain_monitor_feed_empty), style = MaterialTheme.typography.bodyLarge) }
            } else {
                items(state.recentHits) { hit -> DomainHitRow(hit, onClick = { onOpenAppDetail(hit.packageName) }) }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.domain_monitor_limitations_title), style = MaterialTheme.typography.titleSmall)
                        Text(
                            stringResource(R.string.domain_monitor_limitations_text),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun errorMessageRes(error: DomainMonitorVpnService.ErrorReason): Int = when (error) {
    DomainMonitorVpnService.ErrorReason.CONSENT_DENIED -> R.string.domain_monitor_error_consent_denied
    DomainMonitorVpnService.ErrorReason.ANOTHER_VPN_ACTIVE -> R.string.domain_monitor_error_another_vpn
    DomainMonitorVpnService.ErrorReason.ESTABLISH_FAILED -> R.string.domain_monitor_error_establish_failed
}

@Composable
fun DomainHitRow(hit: DomainHit, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = if (hit.isFlagged) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        } else {
            CardDefaults.cardColors()
        },
        onClick = onClick
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AppIcon(hit.packageName, size = 32.dp)
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(hit.domain, style = MaterialTheme.typography.bodyLarge)
                Text(
                    DateUtils.getRelativeTimeSpanString(hit.timestamp).toString(),
                    style = MaterialTheme.typography.labelLarge
                )
            }
            DomainCategoryChip(hit.category, hit.isFlagged)
        }
    }
}
