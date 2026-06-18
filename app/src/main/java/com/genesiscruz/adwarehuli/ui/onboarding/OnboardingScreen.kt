@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.genesiscruz.adwarehuli.ui.onboarding

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.genesiscruz.adwarehuli.R
import com.genesiscruz.adwarehuli.ui.rememberAppContainer

private data class ChecklistItem(
    val title: String,
    val description: String,
    val granted: Boolean,
    val actionable: Boolean = true,
    val onAction: () -> Unit
)

@Composable
fun OnboardingScreen(onContinue: () -> Unit) {
    val context = LocalContext.current
    val container = rememberAppContainer()
    val viewModel: OnboardingViewModel = viewModel(
        factory = viewModelFactory {
            initializer { OnboardingViewModel(container.permissionChecker, context.applicationContext) }
        }
    )
    val state by viewModel.state.collectAsState()

    // Re-check permission status whenever the user returns from Settings.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) { viewModel.refresh() }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.refresh() }

    val items = listOf(
        ChecklistItem(
            title = stringResource(R.string.onboarding_usage_access_title),
            description = stringResource(R.string.onboarding_usage_access_desc),
            granted = state.hasUsageAccess,
            onAction = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }) }
        ),
        ChecklistItem(
            title = stringResource(R.string.onboarding_query_packages_title),
            description = stringResource(R.string.onboarding_query_packages_desc),
            granted = state.hasQueryAllPackages,
            actionable = false,
            onAction = {}
        ),
        ChecklistItem(
            title = stringResource(R.string.onboarding_notifications_title),
            description = stringResource(R.string.onboarding_notifications_desc),
            granted = state.hasNotifications,
            onAction = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        )
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.onboarding_title), style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(R.string.onboarding_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
                items(items) { item -> ChecklistRow(item) }

                item {
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.onboarding_boot_title), style = MaterialTheme.typography.bodyLarge)
                                Text(stringResource(R.string.onboarding_boot_desc), style = MaterialTheme.typography.labelLarge)
                            }
                            Switch(checked = state.resumeOnBoot, onCheckedChange = { viewModel.setResumeOnBoot(it) })
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                if (!state.canProceed) {
                    Text(
                        stringResource(R.string.onboarding_blocked_notice),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                Button(
                    onClick = onContinue,
                    enabled = state.canProceed,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.action_continue))
                }
            }
        }
    }
}

@Composable
private fun ChecklistRow(item: ChecklistItem) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(item.title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = stringResource(if (item.granted) R.string.status_granted else R.string.status_not_granted),
                    color = if (item.granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Text(item.description, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
            if (!item.granted && item.actionable) {
                Button(onClick = item.onAction) { Text(stringResource(R.string.action_grant)) }
            }
        }
    }
}
