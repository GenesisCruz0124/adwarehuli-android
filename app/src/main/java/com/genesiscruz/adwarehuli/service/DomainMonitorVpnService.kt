package com.genesiscruz.adwarehuli.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.genesiscruz.adwarehuli.AdwareHuliApp
import com.genesiscruz.adwarehuli.MainActivity
import com.genesiscruz.adwarehuli.R
import com.genesiscruz.adwarehuli.data.net.BlocklistMatcher
import com.genesiscruz.adwarehuli.data.net.ConnectionAttributor
import com.genesiscruz.adwarehuli.data.net.DnsMessage
import com.genesiscruz.adwarehuli.data.net.IpV4PacketView
import com.genesiscruz.adwarehuli.data.net.UdpReplyBuilder
import com.genesiscruz.adwarehuli.domain.Constants
import com.genesiscruz.adwarehuli.domain.model.DomainCategory
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetSocketAddress

/**
 * A local, loopback-only VpnService that captures the device's DNS traffic
 * (and only DNS traffic — see the scoped route in [buildVpnInterface]),
 * attributes each query to the app that issued it, checks it against the
 * bundled blocklist, and forwards it untouched to a real upstream resolver.
 *
 * No traffic ever leaves the device through anything but the normal
 * network stack; this process only inspects and re-injects DNS packets.
 *
 * TCP/443 SNI sniffing is intentionally out of scope for this build: since
 * we only route the DNS sentinel address through the tun (not a 0.0.0.0/0
 * default route), HTTPS traffic never reaches this process at all. Doing
 * SNI sniffing properly would require capturing all egress traffic and
 * building a full per-flow TCP proxy, which the spec explicitly rules out.
 */
class DomainMonitorVpnService : VpnService() {

    // A SupervisorJob alone does not stop an uncaught exception from one child
    // coroutine (e.g. a single malformed DNS packet) from crashing the whole
    // app process — it only stops siblings from being cancelled. This handler
    // is what actually keeps a per-packet failure from taking down AdwareHuli.
    private val exceptionHandler = CoroutineExceptionHandler { _, e ->
        Log.e(TAG, "Unhandled exception in Domain Monitor coroutine", e)
    }
    private val scope = CoroutineScope(SupervisorJob() + exceptionHandler)
    private var captureJob: Job? = null
    private var tunFd: ParcelFileDescriptor? = null

    private lateinit var notificationManager: NotificationManager
    private lateinit var blocklistMatcher: BlocklistMatcher
    private lateinit var connectionAttributor: ConnectionAttributor

    private val container by lazy { (application as AdwareHuliApp).container }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        blocklistMatcher = BlocklistMatcher(this)
        connectionAttributor = ConnectionAttributor(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopVpn()
                return START_NOT_STICKY
            }
            else -> startVpn()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopVpn()
        scope.cancel()
        super.onDestroy()
    }

    override fun onRevoke() {
        // The system VPN settings UI lets the user revoke consent at any time.
        stopVpn()
        super.onRevoke()
    }

    private fun startVpn() {
        if (captureJob?.isActive == true) return

        // The system requires startForeground() to be called very shortly after
        // startForegroundService() — if we return early below without ever calling
        // it, Android kills the whole app with "did not then call
        // Service.startForeground()". So this must run first, before anything
        // that can fail or return.
        startForeground(NOTIFICATION_ID, buildNotification())

        if (isAnotherVpnActive()) {
            _lastError.value = ErrorReason.ANOTHER_VPN_ACTIVE
        }

        val fd = try {
            buildVpnInterface()
        } catch (e: SecurityException) {
            _lastError.value = ErrorReason.CONSENT_DENIED
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        } catch (e: Exception) {
            _lastError.value = ErrorReason.ESTABLISH_FAILED
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }
        if (fd == null) {
            _lastError.value = ErrorReason.ESTABLISH_FAILED
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }
        tunFd = fd
        _lastError.value = null
        _isRunning.value = true
        _hitsThisSession.value = 0

        captureJob = scope.launch {
            try {
                blocklistMatcher.load()
                runCaptureLoop(fd)
            } catch (e: Exception) {
                Log.e(TAG, "Domain Monitor capture loop failed", e)
                _lastError.value = ErrorReason.ESTABLISH_FAILED
                stopVpn()
            }
        }
    }

    private fun stopVpn() {
        captureJob?.cancel()
        captureJob = null
        runCatching { tunFd?.close() }
        tunFd = null
        _isRunning.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun isAnotherVpnActive(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
    }

    private fun buildVpnInterface(): ParcelFileDescriptor? {
        return Builder()
            .setSession(getString(R.string.domain_monitor_session_name))
            .addAddress(Constants.VPN_ADDRESS, Constants.VPN_PREFIX_LENGTH)
            .addDnsServer(Constants.VPN_DNS_SENTINEL)
            // Scoped route: only traffic addressed to our DNS sentinel goes through the
            // tun. Everything else (including all HTTPS traffic) bypasses this process
            // entirely, by design — see the class doc.
            .addRoute(Constants.VPN_DNS_SENTINEL, 32)
            .setMtu(Constants.VPN_MTU)
            .setBlocking(true)
            .establish()
    }

    private suspend fun runCaptureLoop(fd: ParcelFileDescriptor) {
        val input = FileInputStream(fd.fileDescriptor)
        val output = FileOutputStream(fd.fileDescriptor)
        val buffer = ByteArray(32_767)

        while (true) {
            val length = try {
                withContext(Dispatchers.IO) { input.read(buffer) }
            } catch (e: Exception) {
                break
            }
            if (length <= 0) continue

            val packet = buffer.copyOf(length)
            scope.launch {
                try {
                    handlePacket(packet, output)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to handle a captured DNS packet", e)
                }
            }
        }
    }

    private suspend fun handlePacket(packet: ByteArray, output: FileOutputStream) {
        val ip = IpV4PacketView(packet, packet.size)
        if (!ip.isValid) return
        val udp = ip.udpView() ?: return
        if (udp.destinationPort != Constants.DNS_PORT) return

        val queryPayload = udp.payload()
        val hostname = DnsMessage.parseQuestionHostname(queryPayload) ?: return

        val attribution = connectionAttributor.attribute(
            IpV4PacketView.PROTOCOL_UDP,
            InetSocketAddress(ip.sourceAddress, udp.sourcePort),
            InetSocketAddress(ip.destinationAddress, udp.destinationPort)
        )
        val packageName = attribution?.packageName ?: ConnectionAttributor.UNKNOWN_PACKAGE
        val uid = attribution?.uid ?: -1

        val category = blocklistMatcher.classify(hostname)
        val isFlagged = category != null
        val timestamp = System.currentTimeMillis()

        recordHit(packageName, uid, hostname, category ?: DomainCategory.UNCATEGORIZED, isFlagged, timestamp)

        val blockingEnabled = DomainMonitorPrefs.isBlockingEnabled(this)
        val replyPayload = if (isFlagged && blockingEnabled) {
            DnsMessage.buildNxDomainResponse(queryPayload)
        } else {
            forwardToUpstream(queryPayload) ?: return
        }

        val replyPacket = UdpReplyBuilder.build(
            sourceAddress = ip.destinationAddress,
            sourcePort = udp.destinationPort,
            destinationAddress = ip.sourceAddress,
            destinationPort = udp.sourcePort,
            payload = replyPayload
        )
        try {
            withContext(Dispatchers.IO) { output.write(replyPacket) }
        } catch (e: Exception) {
            // tun closed mid-flight (service stopping) — safe to drop.
        }
    }

    private suspend fun forwardToUpstream(query: ByteArray): ByteArray? = withContext(Dispatchers.IO) {
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket()
            protect(socket)
            socket.soTimeout = UPSTREAM_TIMEOUT_MS
            val upstream = Inet4Address.getByName(Constants.UPSTREAM_DNS_PRIMARY)
            socket.send(DatagramPacket(query, query.size, upstream, Constants.DNS_PORT))

            val responseBuffer = ByteArray(MAX_DNS_RESPONSE_SIZE)
            val responsePacket = DatagramPacket(responseBuffer, responseBuffer.size)
            socket.receive(responsePacket)
            responseBuffer.copyOf(responsePacket.length)
        } catch (e: Exception) {
            null
        } finally {
            socket?.close()
        }
    }

    private suspend fun recordHit(
        packageName: String,
        uid: Int,
        domain: String,
        category: DomainCategory,
        isFlagged: Boolean,
        timestamp: Long
    ) {
        container.domainHitRepository.record(
            packageName = packageName,
            uid = uid,
            domain = domain,
            category = category,
            isFlagged = isFlagged,
            protocol = "UDP",
            timestamp = timestamp
        )
        _hitsThisSession.value += 1
    }

    private fun buildNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.domain_monitor_notification_title))
            .setContentText(getString(R.string.domain_monitor_notification_text))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openAppIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.domain_monitor_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

    enum class ErrorReason { CONSENT_DENIED, ESTABLISH_FAILED, ANOTHER_VPN_ACTIVE }

    companion object {
        const val ACTION_START = "com.genesiscruz.adwarehuli.action.START_DOMAIN_MONITOR"
        const val ACTION_STOP = "com.genesiscruz.adwarehuli.action.STOP_DOMAIN_MONITOR"

        private const val TAG = "DomainMonitorVpnService"
        private const val CHANNEL_ID = "domain_monitor_status"
        private const val NOTIFICATION_ID = 2001
        private const val UPSTREAM_TIMEOUT_MS = 5000
        private const val MAX_DNS_RESPONSE_SIZE = 4096

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _hitsThisSession = MutableStateFlow(0)
        val hitsThisSession: StateFlow<Int> = _hitsThisSession.asStateFlow()

        private val _lastError = MutableStateFlow<ErrorReason?>(null)
        val lastError: StateFlow<ErrorReason?> = _lastError.asStateFlow()

        /** Returns a consent Intent to launch for a result if the system needs to show the VPN dialog, else null. */
        fun prepareIntent(context: Context): Intent? = VpnService.prepare(context)

        fun start(context: Context) {
            val intent = Intent(context, DomainMonitorVpnService::class.java).setAction(ACTION_START)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, DomainMonitorVpnService::class.java).setAction(ACTION_STOP)
            context.startService(intent)
        }
    }
}
