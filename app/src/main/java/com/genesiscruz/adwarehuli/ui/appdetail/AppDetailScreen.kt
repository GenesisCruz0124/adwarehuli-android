@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.genesiscruz.adwarehuli.ui.appdetail

import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.genesiscruz.adwarehuli.R
import com.genesiscruz.adwarehuli.domain.model.RedirectEvent
import com.genesiscruz.adwarehuli.ui.components.AppIcon
import com.genesiscruz.adwarehuli.ui.components.RiskBandChip
import com.genesiscruz.adwarehuli.ui.components.labelRes
import com.genesiscruz.adwarehuli.ui.rememberAppContainer
import java.text.DateFormat
import java.util.Date

private val RISKY_PERMISSIONS = setOf(
    android.Manifest.permission.SYSTEM_ALERT_WINDOW,
    android.Manifest.permission.RECEIVE_BOOT_COMPLETED,
    android.Manifest.permission.QUERY_ALL_PACKAGES,
    android.Manifest.permission.REQUEST_INSTALL_PACKAGES,
    android.Manifest.permission.PACKAGE_USAGE_STATS,
    android.Manifest.permission.BIND_ACCESSIBILITY_SERVICE
)

@Composable
fun AppDetailScreen(packageName: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val container = rememberAppContainer()
    val viewModel: AppDetailViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                AppDetailViewModel(
                    packageName,
                    context.packageManager,
                    container.appRiskRepository,
                    container.redirectEventRepository
                )
            }
        }
    )
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(packageName, size = 56.dp)
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(state.label, style = MaterialTheme.typography.titleLarge)
                        Text(stringResource(R.string.detail_package, state.packageName), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            item {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Text(stringResource(R.string.detail_version, state.versionName ?: stringResource(R.string.detail_unknown_source)))
                    Text(
                        stringResource(
                            R.string.detail_install_source,
                            state.riskInfo?.installSource ?: stringResource(R.string.detail_unknown_source)
                        )
                    )
                    val installDate = state.riskInfo?.firstInstallTime?.let {
                        DateFormat.getDateInstance().format(Date(it))
                    } ?: stringResource(R.string.detail_unknown_source)
                    Text(stringResource(R.string.detail_install_date, installDate))
                }
            }

            item {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text(stringResource(R.string.detail_uninstall)) }
                    }
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                .setData(Uri.parse("package:$packageName"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) { Text(stringResource(R.string.detail_open_app_info)) }
                }
            }

            item {
                Text(
                    stringResource(R.string.detail_risk_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
                )
            }
            item {
                val riskInfo = state.riskInfo
                if (riskInfo == null) {
                    Text(stringResource(R.string.detail_not_scanned), style = MaterialTheme.typography.bodyLarge)
                } else {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            RiskBandChip(riskInfo.band)
                            riskInfo.reasons.forEach { reason ->
                                Text("• " + stringResource(reason.labelRes()), modifier = Modifier.padding(top = 6.dp))
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    stringResource(R.string.detail_redirect_history_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
                )
                Text(stringResource(R.string.detail_redirect_count, state.redirectHistory.size))
            }
            items(state.redirectHistory) { event -> RedirectHistoryRow(event) }

            item {
                Text(
                    stringResource(R.string.detail_permissions_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
                )
            }
            items(state.requestedPermissions) { permission ->
                val isRisky = permission in RISKY_PERMISSIONS
                Text(
                    permission,
                    color = if (isRisky) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun RedirectHistoryRow(event: RedirectEvent) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            DateFormat.getDateTimeInstance().format(Date(event.timestamp)),
            modifier = Modifier.padding(12.dp)
        )
    }
}
