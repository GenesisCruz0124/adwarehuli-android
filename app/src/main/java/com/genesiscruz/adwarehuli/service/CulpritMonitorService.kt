package com.genesiscruz.adwarehuli.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.genesiscruz.adwarehuli.AdwareHuliApp
import com.genesiscruz.adwarehuli.MainActivity
import com.genesiscruz.adwarehuli.R
import com.genesiscruz.adwarehuli.domain.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Foreground service that polls [UsageStatsManager] for foreground-app
 * transitions and flags a "redirect" whenever a browser package comes to
 * foreground shortly after a non-browser, non-launcher app.
 */
class CulpritMonitorService : Service() {

    private val scope = CoroutineScope(SupervisorJob())
    private var pollJob: Job? = null

    private lateinit var usageStatsManager: UsageStatsManager
    private lateinit var notificationManager: NotificationManager

    private val container by lazy { (application as AdwareHuliApp).container }

    private var lastNonSpecialPackage: String? = null
    private var lastNonSpecialTimestamp: Long = 0L
    private var lastProcessedEventTime: Long = System.currentTimeMillis() - Constants.POLL_OVERLAP_MS

    override fun onCreate() {
        super.onCreate()
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopMonitoring()
                return START_NOT_STICKY
            }
            else -> startMonitoring()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        pollJob?.cancel()
        scope.cancel()
        _isRunning.value = false
        super.onDestroy()
    }

    private fun startMonitoring() {
        if (pollJob?.isActive == true) return

        startForeground(NOTIFICATION_ID, buildMonitorNotification())
        _isRunning.value = true
        _eventsThisSession.value = 0
        lastProcessedEventTime = System.currentTimeMillis() - Constants.POLL_OVERLAP_MS

        pollJob = scope.launch {
            while (true) {
                pollOnce()
                delay(Constants.POLL_INTERVAL_MS)
            }
        }
    }

    private fun stopMonitoring() {
        pollJob?.cancel()
        _isRunning.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private suspend fun pollOnce() {
        val now = System.currentTimeMillis()
        val queryStart = (now - Constants.POLL_OVERLAP_MS).coerceAtMost(lastProcessedEventTime)
        val events = usageStatsManager.queryEvents(queryStart, now)
        val event = UsageEvents.Event()
        var newestEventTime = lastProcessedEventTime

        val foregroundEvents = mutableListOf<Pair<String, Long>>()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.timeStamp <= lastProcessedEventTime) continue
            val isForegroundEvent = event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND)
            if (isForegroundEvent) {
                foregroundEvents += event.packageName to event.timeStamp
            }
            if (event.timeStamp > newestEventTime) newestEventTime = event.timeStamp
        }
        lastProcessedEventTime = newestEventTime

        foregroundEvents.sortBy { it.second }
        for ((packageName, timestamp) in foregroundEvents) {
            handleForegroundTransition(packageName, timestamp)
        }
    }

    private suspend fun handleForegroundTransition(packageName: String, timestamp: Long) {
        if (isExcludedFromTracking(packageName)) return

        val isBrowser = container.browserDetector.isBrowser(packageName)
        if (isBrowser) {
            val suspect = lastNonSpecialPackage
            if (suspect != null && (timestamp - lastNonSpecialTimestamp) <= Constants.REDIRECT_WINDOW_MS) {
                recordRedirect(suspect, packageName, timestamp)
            }
        } else if (!container.launcherDetector.isLauncher(packageName)) {
            lastNonSpecialPackage = packageName
            lastNonSpecialTimestamp = timestamp
        }
    }

    private fun isExcludedFromTracking(packageName: String): Boolean {
        return packageName == this.packageName || packageName == SYSTEM_UI_PACKAGE
    }

    private suspend fun recordRedirect(suspectPackage: String, browserPackage: String, timestamp: Long) {
        container.redirectEventRepository.recordRedirect(suspectPackage, browserPackage, timestamp)
        _eventsThisSession.value += 1
        showRedirectAlert(suspectPackage)
    }

    private suspend fun showRedirectAlert(suspectPackage: String) {
        val label = container.packageInfoProvider.getLabel(suspectPackage)
        val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.monitor_alert_title))
            .setContentText(getString(R.string.monitor_alert_text, label))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        notificationManager.notify(ALERT_NOTIFICATION_ID, notification)
    }

    private fun buildMonitorNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, MONITOR_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.monitor_notification_title))
            .setContentText(getString(R.string.monitor_notification_text))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openAppIntent)
            .build()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val monitorChannel = NotificationChannel(
            MONITOR_CHANNEL_ID,
            getString(R.string.monitor_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        val alertChannel = NotificationChannel(
            ALERT_CHANNEL_ID,
            getString(R.string.monitor_alert_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(monitorChannel)
        notificationManager.createNotificationChannel(alertChannel)
    }

    companion object {
        const val ACTION_START = "com.genesiscruz.adwarehuli.action.START_MONITOR"
        const val ACTION_STOP = "com.genesiscruz.adwarehuli.action.STOP_MONITOR"

        private const val MONITOR_CHANNEL_ID = "monitor_status"
        private const val ALERT_CHANNEL_ID = "monitor_alerts"
        private const val NOTIFICATION_ID = 1001
        private const val ALERT_NOTIFICATION_ID = 1002
        private const val SYSTEM_UI_PACKAGE = "com.android.systemui"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _eventsThisSession = MutableStateFlow(0)
        val eventsThisSession: StateFlow<Int> = _eventsThisSession.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, CulpritMonitorService::class.java).setAction(ACTION_START)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, CulpritMonitorService::class.java).setAction(ACTION_STOP)
            context.startService(intent)
        }
    }
}
