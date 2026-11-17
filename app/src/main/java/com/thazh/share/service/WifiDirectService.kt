package com.thazh.share.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.thazh.share.MainActivity
import com.thazh.share.R
import com.thazh.share.ShareApplication
import com.thazh.share.data.ConnectionQuality
import com.thazh.share.data.DeviceVisibilityMode
import com.thazh.share.data.DiscoveredPeer
import com.thazh.share.data.IncomingConnectionRequest
import com.thazh.share.data.PeerDiscoveryTransport
import com.thazh.share.data.PeerStatus
import com.thazh.share.data.PickedFile
import com.thazh.share.data.ReceivedFile
import com.thazh.share.data.ReceivedFilesRepository
import com.thazh.share.data.SavedDevice
import com.thazh.share.data.TransferDirection
import com.thazh.share.data.TransferItem
import com.thazh.share.data.TransferState
import com.thazh.share.network.BleDiscoveryManager
import com.thazh.share.network.ConnectionMonitor
import com.thazh.share.network.FileTransferManager
import com.thazh.share.network.Frame
import com.thazh.share.network.NfcConnectionCoordinator
import com.thazh.share.network.NfcPeerExchange
import com.thazh.share.network.P2pConnectionState
import com.thazh.share.network.PeerConnection
import com.thazh.share.network.SocketConnectionEvent
import com.thazh.share.network.TransferProgressEvent
import com.thazh.share.network.WifiDirectManager
import com.thazh.share.ui.IncomingConnectionActivity
import com.thazh.share.util.FileActions
import com.thazh.share.util.FileNameUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.UUID

class WifiDirectService : Service() {

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    lateinit var wifiDirectManager: WifiDirectManager
        private set
    private var peerConnection: PeerConnection? = null
    private var connectionMonitor: ConnectionMonitor? = null
    private var fileTransferManager: FileTransferManager? = null

    private val wifiManager: WifiManager? by lazy {
        applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    }

    private val bleDiscoveryManager: BleDiscoveryManager by lazy { BleDiscoveryManager(applicationContext) }
    private var bleFastDiscoveryEnabled = true
    private var confirmIncomingConnectionEnabled = true
    private var backgroundConnectionDialogEnabled = true
    private var autoSaveToDownloadsEnabled = false
    private var soundVibrationEnabled = true
    private var isDiscoveryActive = false

    private var socketEventJob: Job? = null
    private var progressEventJob: Job? = null
    private var qualityJob: Job? = null
    private var distanceJob: Job? = null
    private var p2pStateJob: Job? = null
    private var peersJob: Job? = null
    private var prefsJob: Job? = null
    private var nfcExchangeJob: Job? = null
    private var thisDeviceJob: Job? = null
    private var nfcTimeoutJob: Job? = null
    private var visibilityTimerJob: Job? = null
    private var incomingRequestTimeoutJob: Job? = null

    private val _connectedPeerName = MutableStateFlow<String?>(null)
    val connectedPeerName: StateFlow<String?> = _connectedPeerName.asStateFlow()

    private val _connectedPeerAddress = MutableStateFlow<String?>(null)
    val connectedPeerAddress: StateFlow<String?> = _connectedPeerAddress.asStateFlow()

    private val _isSocketConnected = MutableStateFlow(false)
    val isSocketConnected: StateFlow<Boolean> = _isSocketConnected.asStateFlow()

    private val _connectionQuality = MutableStateFlow(ConnectionQuality.DISCONNECTED)
    val connectionQuality: StateFlow<ConnectionQuality> = _connectionQuality.asStateFlow()

    private val _estimatedDistanceMeters = MutableStateFlow<Int?>(null)
    val estimatedDistanceMeters: StateFlow<Int?> = _estimatedDistanceMeters.asStateFlow()

    private val _transfers = MutableStateFlow<List<TransferItem>>(emptyList())
    val transfers: StateFlow<List<TransferItem>> = _transfers.asStateFlow()

    private val _receivedFileEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val receivedFileEvents: SharedFlow<String> = _receivedFileEvents.asSharedFlow()

    private val _peers = MutableStateFlow<List<DiscoveredPeer>>(emptyList())
    val peers: StateFlow<List<DiscoveredPeer>> = _peers.asStateFlow()

    private val _nfcPairingPeerName = MutableStateFlow<String?>(null)
    val nfcPairingPeerName: StateFlow<String?> = _nfcPairingPeerName.asStateFlow()

    private val _visibilityMode = MutableStateFlow(DeviceVisibilityMode.TEMPORARY_10_MIN)
    val visibilityMode: StateFlow<DeviceVisibilityMode> = _visibilityMode.asStateFlow()

    private val _visibilityRemainingSeconds = MutableStateFlow<Int?>(TEMPORARY_VISIBILITY_SECONDS)
    val visibilityRemainingSeconds: StateFlow<Int?> = _visibilityRemainingSeconds.asStateFlow()

    private val _incomingConnectionRequest = MutableStateFlow<IncomingConnectionRequest?>(null)
    val incomingConnectionRequest: StateFlow<IncomingConnectionRequest?> = _incomingConnectionRequest.asStateFlow()

    private data class PendingNfcMatch(
        val displayName: String,
        val wifiP2pName: String,
        val expiresAtElapsedMs: Long,
    )

    private enum class IncomingGateState {
        NONE,
        WAITING_DECISION,
        APPROVED,
        REJECTED,
    }

    private var pendingNfcMatch: PendingNfcMatch? = null
    private var localDisplayName: String = Build.MODEL?.takeIf { it.isNotBlank() } ?: "Thiết bị"
    private var currentGroupOwnerAddress: String? = null
    private var currentDeviceAddress: String? = null
    private var isUiBound = false
    private var isUiInForeground = false
    private var hasInitiatedOutgoingConnect = false
    private var incomingGateState = IncomingGateState.NONE
    private val queuedIncomingOffers = mutableListOf<Frame.FileOffer>()

    private val outputDir: File by lazy { File(filesDir, "share_files").apply { mkdirs() } }

    private val app: ShareApplication
        get() = application as ShareApplication

    private val receivedFilesRepository: ReceivedFilesRepository
        get() = app.receivedFilesRepository

    private val transferSendMutex = Mutex()
    private val transferSamples = HashMap<String, TransferSample>()

    private class TransferSample(
        var lastUiUpdateMs: Long,
        var lastSpeedMs: Long,
        var lastSpeedBytes: Long,
        var speedBytesPerSec: Double,
    )

    override fun onCreate() {
        super.onCreate()
        wifiDirectManager = WifiDirectManager(applicationContext)
        wifiDirectManager.initialize()
        observeP2pConnectionState()
        observeMergedPeers()
        observeThisDeviceForNfc()
        observeNfcExchanges()
        observePreferences()
        startForegroundWithNotification(getString(R.string.notif_scanning))

        serviceScope.launch {
            val initialMode = runCatching { app.preferencesRepository.defaultVisibilityMode.first() }
                .getOrDefault(DeviceVisibilityMode.TEMPORARY_10_MIN)
            setVisibilityMode(initialMode)
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        isUiBound = true
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        isUiBound = false
        return false
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_ACCEPT_INCOMING -> {
                val peerName = _incomingConnectionRequest.value?.peerName.orEmpty()
                acceptIncomingConnection()
                if (peerName.isNotBlank()) {
                    Toast.makeText(
                        applicationContext,
                        getString(R.string.incoming_conn_accepted_toast, peerName),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
            ACTION_DECLINE_INCOMING -> declineIncomingConnection()
            ACTION_SET_VISIBILITY_FROM_TILE -> {
                val modeKey = intent.getStringExtra(EXTRA_VISIBILITY_MODE)
                val targetMode = DeviceVisibilityMode.fromStorageKey(modeKey)
                setVisibilityMode(targetMode)
            }
        }
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }

    override fun onDestroy() {
        ShareQuickSettingsTileService.notifyVisibilityChanged(
            context = applicationContext,
            mode = _visibilityMode.value,
            running = false,
        )
        tearDownActiveConnection()
        visibilityTimerJob?.cancel()
        incomingRequestTimeoutJob?.cancel()
        cancelIncomingConnectionNotification()
        bleDiscoveryManager.stop()
        peersJob?.cancel()
        prefsJob?.cancel()
        nfcExchangeJob?.cancel()
        thisDeviceJob?.cancel()
        nfcTimeoutJob?.cancel()
        wifiDirectManager.teardown()
        super.onDestroy()
    }

    fun setUiInForeground(inForeground: Boolean) {
        isUiInForeground = inForeground
        if (inForeground) {
            cancelIncomingConnectionNotification()
        } else {
            val pending = _incomingConnectionRequest.value
            if (pending != null &&
                incomingGateState == IncomingGateState.WAITING_DECISION &&
                _visibilityMode.value.isVisible &&
                backgroundConnectionDialogEnabled
            ) {
                showIncomingConnectionNotification(pending, isInitialPrompt = true)
                launchIncomingConnectionActivity()
            }
        }
    }

    fun setLocalDisplayName(name: String) {
        localDisplayName = name.ifBlank { localDisplayName }
        NfcConnectionCoordinator.updateLocalIdentity(displayName = localDisplayName)
        if (isDiscoveryActive && bleFastDiscoveryEnabled) {
            bleDiscoveryManager.start(wifiDirectManager.thisDevice.value?.deviceAddress, localDisplayName)
        }
        serviceScope.launch {
            peerConnection?.takeIf { it.isConnected }?.send(Frame.NameChanged(localDisplayName))
        }
    }

    fun setVisibilityMode(mode: DeviceVisibilityMode) {
        visibilityTimerJob?.cancel()
        _visibilityMode.value = mode
        ShareQuickSettingsTileService.notifyVisibilityChanged(
            context = applicationContext,
            mode = mode,
            running = true,
        )

        when (mode) {
            DeviceVisibilityMode.HIDDEN -> {
                _visibilityRemainingSeconds.value = null
                stopDiscoveryInternal()
                if (incomingGateState == IncomingGateState.WAITING_DECISION) {
                    declineIncomingConnection()
                }
                if (!_isSocketConnected.value) {
                    updateNotification(getString(R.string.notif_hidden))
                }
            }

            DeviceVisibilityMode.TEMPORARY_10_MIN -> {
                _visibilityRemainingSeconds.value = TEMPORARY_VISIBILITY_SECONDS
                startDiscoveryInternal { _, _ -> }
                startVisibilityCountdown()
            }

            DeviceVisibilityMode.ALWAYS_VISIBLE -> {
                _visibilityRemainingSeconds.value = null
                startDiscoveryInternal { _, _ -> }
                if (!_isSocketConnected.value) {
                    updateNotification(getString(R.string.notif_visible_always))
                }
            }
        }
    }

    private fun startVisibilityCountdown() {
        visibilityTimerJob?.cancel()
        visibilityTimerJob = serviceScope.launch {
            var remaining = TEMPORARY_VISIBILITY_SECONDS
            while (remaining > 0 && _visibilityMode.value == DeviceVisibilityMode.TEMPORARY_10_MIN) {
                _visibilityRemainingSeconds.value = remaining
                if (!_isSocketConnected.value) {
                    updateNotification(getString(R.string.notif_visible_10min, formatCountdown(remaining)))
                }
                delay(1_000L)
                remaining--
            }
            if (_visibilityMode.value == DeviceVisibilityMode.TEMPORARY_10_MIN && remaining <= 0) {
                setVisibilityMode(DeviceVisibilityMode.HIDDEN)
            }
        }
    }

    fun startDiscovery(onResult: (Boolean, String?) -> Unit) {
        if (_visibilityMode.value == DeviceVisibilityMode.HIDDEN) {
            setVisibilityMode(DeviceVisibilityMode.TEMPORARY_10_MIN)
            onResult(true, null)
            return
        }
        startDiscoveryInternal(onResult)
    }

    fun stopDiscovery() {
        setVisibilityMode(DeviceVisibilityMode.HIDDEN)
    }

    private fun startDiscoveryInternal(onResult: (Boolean, String?) -> Unit) {
        isDiscoveryActive = true
        if (bleFastDiscoveryEnabled) {
            bleDiscoveryManager.start(wifiDirectManager.thisDevice.value?.deviceAddress, localDisplayName)
        }
        wifiDirectManager.startDiscovery(onResult)
    }

    private fun stopDiscoveryInternal() {
        isDiscoveryActive = false
        bleDiscoveryManager.stop()
        wifiDirectManager.stopDiscovery()
    }

    fun isBleFastDiscoverySupported(): Boolean = bleDiscoveryManager.isSupported()

    val isBleActive: StateFlow<Boolean> get() = bleDiscoveryManager.isScanningState

    fun retryBleDiscovery() {
        if (isDiscoveryActive && bleFastDiscoveryEnabled) {
            bleDiscoveryManager.start(wifiDirectManager.thisDevice.value?.deviceAddress, localDisplayName)
        }
    }

    fun acceptIncomingConnection() {
        if (incomingGateState != IncomingGateState.WAITING_DECISION) return
        incomingGateState = IncomingGateState.APPROVED
        incomingRequestTimeoutJob?.cancel()
        _incomingConnectionRequest.value = null
        cancelIncomingConnectionNotification()

        val queued = queuedIncomingOffers.toList()
        queuedIncomingOffers.clear()
        queued.forEach { offer -> onTransferOfferReceived(offer) }
    }

    fun declineIncomingConnection() {
        incomingRequestTimeoutJob?.cancel()
        incomingGateState = IncomingGateState.REJECTED
        _incomingConnectionRequest.value = null
        queuedIncomingOffers.clear()
        cancelIncomingConnectionNotification()
        disconnect()
    }

    private fun observePreferences() {
        prefsJob?.cancel()
        prefsJob = serviceScope.launch {
            launch {
                app.preferencesRepository.bleFastDiscoveryEnabled.collect { enabled ->
                    bleFastDiscoveryEnabled = enabled
                    if (isDiscoveryActive) {
                        if (enabled) bleDiscoveryManager.start(wifiDirectManager.thisDevice.value?.deviceAddress, localDisplayName)
                        else bleDiscoveryManager.stop()
                    }
                }
            }
            launch {
                app.preferencesRepository.confirmIncomingConnection.collect { enabled ->
                    confirmIncomingConnectionEnabled = enabled
                }
            }
            launch {
                app.preferencesRepository.backgroundConnectionDialog.collect { enabled ->
                    backgroundConnectionDialogEnabled = enabled
                }
            }
            launch {
                app.preferencesRepository.autoSaveToDownloads.collect { enabled ->
                    autoSaveToDownloadsEnabled = enabled
                }
            }
            launch {
                app.preferencesRepository.soundVibrationEnabled.collect { enabled ->
                    soundVibrationEnabled = enabled
                }
            }
        }
    }

    private fun observeThisDeviceForNfc() {
        thisDeviceJob?.cancel()
        thisDeviceJob = serviceScope.launch {
            wifiDirectManager.thisDevice.collect { device ->
                if (device != null) {
                    NfcConnectionCoordinator.updateLocalIdentity(
                        displayName = localDisplayName,
                        wifiP2pName = device.deviceName,
                        deviceAddress = device.deviceAddress,
                    )
                }
            }
        }
    }

    private fun observeNfcExchanges() {
        nfcExchangeJob?.cancel()
        nfcExchangeJob = serviceScope.launch {
            NfcConnectionCoordinator.exchanges.collect { exchange ->
                handleNfcPeerExchange(exchange)
            }
        }
    }

    private fun handleNfcPeerExchange(exchange: NfcPeerExchange) {
        val currentP2pState = wifiDirectManager.connectionState.value
        if (currentP2pState is P2pConnectionState.Connected || currentP2pState is P2pConnectionState.Connecting) {
            return
        }

        _nfcPairingPeerName.value = exchange.peer.displayName
        nfcTimeoutJob?.cancel()
        nfcTimeoutJob = serviceScope.launch {
            delay(15_000L)
            _nfcPairingPeerName.value = null
            pendingNfcMatch = null
        }

        startDiscovery { _, _ -> }

        if (!exchange.initiateConnect) {
            return
        }

        val peerAddress = exchange.peer.deviceAddress
        if (NfcConnectionCoordinator.isValidMacAddress(peerAddress)) {
            connectToPeer(peerAddress) { _, _ -> }
            return
        }

        val match = PendingNfcMatch(
            displayName = exchange.peer.displayName,
            wifiP2pName = exchange.peer.wifiP2pName,
            expiresAtElapsedMs = SystemClock.elapsedRealtime() + 15_000L,
        )
        pendingNfcMatch = match
        tryCompletePendingNfcMatch(wifiDirectManager.peers.value)
    }

    private fun tryCompletePendingNfcMatch(availablePeers: List<DiscoveredPeer>) {
        val match = pendingNfcMatch ?: return
        if (SystemClock.elapsedRealtime() > match.expiresAtElapsedMs) {
            pendingNfcMatch = null
            return
        }
        val candidates = listOf(match.wifiP2pName, match.displayName)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val found = availablePeers.firstOrNull { peer ->
            val pName = peer.deviceName.trim()
            candidates.any { candidate -> pName.equals(candidate, ignoreCase = true) }
        } ?: return

        pendingNfcMatch = null
        connectToPeer(found.deviceAddress) { _, _ -> }
    }

    private fun observeMergedPeers() {
        peersJob?.cancel()
        peersJob = serviceScope.launch {
            combine(wifiDirectManager.peers, bleDiscoveryManager.discoveredBeacons) { wifiPeers, bleBeacons ->
                val enrichedWifi = wifiPeers.map { peer ->
                    val matchingBeacon = bleBeacons[peer.deviceAddress.uppercase()]
                        ?: bleBeacons.values.firstOrNull { beacon ->
                            !beacon.displayName.isNullOrBlank() &&
                                peer.deviceName.contains(beacon.displayName, ignoreCase = true)
                        }
                    if (matchingBeacon != null) {
                        peer.copy(
                            discoveryTransport = PeerDiscoveryTransport.WIFI_AND_BLE,
                            rssiDbm = matchingBeacon.rssiDbm,
                            estimatedDistanceMeters = matchingBeacon.estimatedDistanceMeters,
                        )
                    } else {
                        peer
                    }
                }
                val alreadyKnownAddresses = wifiPeers.map { it.deviceAddress.uppercase() }.toSet()
                val alreadyKnownNames = wifiPeers.map { it.deviceName.lowercase() }.toSet()
                val bleOnly = bleBeacons.values.filter { beacon ->
                    beacon.deviceAddress.uppercase() !in alreadyKnownAddresses &&
                        (beacon.displayName == null || beacon.displayName.lowercase() !in alreadyKnownNames)
                }.map { beacon ->
                    val label = beacon.displayName?.takeIf { it.isNotBlank() }
                        ?: getString(R.string.ble_nearby_device)
                    DiscoveredPeer(
                        deviceName = label,
                        deviceAddress = beacon.deviceAddress,
                        status = PeerStatus.AVAILABLE,
                        discoveryTransport = PeerDiscoveryTransport.BLE,
                        rssiDbm = beacon.rssiDbm,
                        estimatedDistanceMeters = beacon.estimatedDistanceMeters,
                    )
                }
                enrichedWifi + bleOnly
            }.collect { merged ->
                _peers.value = merged
                tryCompletePendingNfcMatch(merged)
            }
        }
    }

    fun connectToPeer(deviceAddress: String, onResult: (Boolean, String?) -> Unit) {
        hasInitiatedOutgoingConnect = true
        currentDeviceAddress = deviceAddress
        wifiDirectManager.connectTo(deviceAddress) { ok, err ->
            if (!ok) {
                hasInitiatedOutgoingConnect = false
            }
            onResult(ok, err)
        }
    }

    fun disconnect() {
        hasInitiatedOutgoingConnect = false
        tearDownActiveConnection()
        _transfers.value = emptyList()
        wifiDirectManager.disconnect()
    }

    private fun observeP2pConnectionState() {
        p2pStateJob = serviceScope.launch {
            wifiDirectManager.connectionState.collect { state ->
                when (state) {
                    is P2pConnectionState.Connected -> {
                        pendingNfcMatch = null
                        _nfcPairingPeerName.value = null
                        nfcTimeoutJob?.cancel()
                        onGroupFormed(state)
                    }
                    is P2pConnectionState.Disconnected -> {
                        hasInitiatedOutgoingConnect = false
                        _nfcPairingPeerName.value = null
                        tearDownActiveConnection()
                    }
                    is P2pConnectionState.Failed -> {
                        hasInitiatedOutgoingConnect = false
                        pendingNfcMatch = null
                        _nfcPairingPeerName.value = null
                        tearDownActiveConnection()
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun onGroupFormed(state: P2pConnectionState.Connected) {
        if (peerConnection != null) {
            Log.d(TAG, "onGroupFormed: connection already active/in progress, ignoring duplicate")
            return
        }

        // Reject passive incoming connections if the user deliberately set visibility to Hidden.
        if (!hasInitiatedOutgoingConnect && _visibilityMode.value == DeviceVisibilityMode.HIDDEN) {
            Log.i(TAG, "Ignoring incoming P2P connection while device visibility is HIDDEN")
            wifiDirectManager.disconnect()
            return
        }

        val shouldGateIncoming = !hasInitiatedOutgoingConnect && (
            confirmIncomingConnectionEnabled ||
                (!isUiInForeground && _visibilityMode.value.isVisible && backgroundConnectionDialogEnabled)
            )
        incomingGateState = if (shouldGateIncoming) {
            IncomingGateState.WAITING_DECISION
        } else {
            IncomingGateState.APPROVED
        }
        queuedIncomingOffers.clear()

        val groupOwnerAddr = state.groupOwnerAddress
        currentGroupOwnerAddress = groupOwnerAddr
        updateNotification(getString(R.string.notif_connecting))

        val connection = PeerConnection(serviceScope)
        peerConnection = connection
        fileTransferManager = FileTransferManager(applicationContext, connection, outputDir)
        connectionMonitor = ConnectionMonitor(connection, serviceScope, wifiManager)

        observeSocketEvents(connection)
        observeTransferProgress()
        observeQuality()
        observeDistance()

        serviceScope.launch {
            if (state.isGroupOwner) {
                connection.startAsServer(WILDCARD_BIND_ADDRESS)
            } else {
                connection.connectAsClient(groupOwnerAddr ?: DEFAULT_GROUP_OWNER_IP)
            }
        }

        if (incomingGateState == IncomingGateState.WAITING_DECISION) {
            serviceScope.launch {
                delay(1_200L)
                if (peerConnection === connection &&
                    incomingGateState == IncomingGateState.WAITING_DECISION &&
                    _incomingConnectionRequest.value == null
                ) {
                    val fallbackName = _connectedPeerName.value?.takeIf { it.isNotBlank() }
                        ?: _peers.value.firstOrNull { it.deviceAddress == currentDeviceAddress }?.deviceName
                        ?: getString(R.string.transfer_unknown_peer)
                    val fallbackAddress = _connectedPeerAddress.value?.takeIf { it.isNotBlank() }
                        ?: currentDeviceAddress.orEmpty()
                    triggerIncomingConnectionPrompt(
                        peerName = fallbackName,
                        peerAddress = fallbackAddress,
                    )
                }
            }
        }
    }

    private fun observeSocketEvents(connection: PeerConnection) {
        socketEventJob?.cancel()
        socketEventJob = serviceScope.launch {
            connection.events.collect { event ->
                when (event) {
                    is SocketConnectionEvent.Connected -> {
                        _isSocketConnected.value = true
                        connectionMonitor?.start()
                        connectionMonitor?.onConnectionStateHint(true)
                        connection.send(Frame.Hello(localDisplayName, currentDeviceAddress.orEmpty()))
                        updateNotification(getString(R.string.notif_connected, _connectedPeerName.value ?: ""))
                    }

                    is SocketConnectionEvent.Disconnected -> {
                        _isSocketConnected.value = false
                        connectionMonitor?.onConnectionStateHint(false)
                        updateNotification(defaultVisibilityNotificationText())
                        if (peerConnection === connection) {
                            connectionMonitor?.stop()
                            failInFlightTransfers()
                            resetIncomingGate()
                            peerConnection = null
                            connectionMonitor = null
                            fileTransferManager = null
                        }
                    }

                    is SocketConnectionEvent.FrameReceived -> handleIncomingFrame(event.frame)

                    is SocketConnectionEvent.RawChunkReceived -> {
                        fileTransferManager?.onChunkBytesReceived(event.bytes)
                    }
                }
            }
        }
    }

    private fun handleIncomingFrame(frame: Frame) {
        when (frame) {
            is Frame.Hello -> {
                _connectedPeerName.value = frame.displayName
                _connectedPeerAddress.value = frame.deviceAddress.ifBlank { currentDeviceAddress }
                persistSavedDevice(frame.deviceAddress.ifBlank { currentDeviceAddress.orEmpty() }, frame.displayName)
                if (_isSocketConnected.value) {
                    updateNotification(getString(R.string.notif_connected, frame.displayName))
                }
                if (incomingGateState == IncomingGateState.WAITING_DECISION) {
                    triggerIncomingConnectionPrompt(
                        peerName = frame.displayName.ifBlank { getString(R.string.transfer_unknown_peer) },
                        peerAddress = frame.deviceAddress.ifBlank { currentDeviceAddress.orEmpty() },
                    )
                }
            }

            is Frame.NameChanged -> {
                _connectedPeerName.value = frame.newDisplayName
                _connectedPeerAddress.value?.let { persistSavedDevice(it, frame.newDisplayName) }
                val pending = _incomingConnectionRequest.value
                if (pending != null) {
                    _incomingConnectionRequest.value = pending.copy(peerName = frame.newDisplayName)
                    if (!isUiInForeground && backgroundConnectionDialogEnabled) {
                        showIncomingConnectionNotification(_incomingConnectionRequest.value!!)
                    }
                }
            }

            is Frame.FileOffer -> {
                when (incomingGateState) {
                    IncomingGateState.WAITING_DECISION -> {
                        queuedIncomingOffers += frame
                        val existing = _incomingConnectionRequest.value
                        val peerName = existing?.peerName
                            ?: _connectedPeerName.value?.takeIf { it.isNotBlank() }
                            ?: getString(R.string.transfer_unknown_peer)
                        val peerAddress = existing?.peerAddress ?: _connectedPeerAddress.value.orEmpty()
                        val updatedRequest = (existing ?: IncomingConnectionRequest(
                            requestId = UUID.randomUUID().toString(),
                            peerName = peerName,
                            peerAddress = peerAddress,
                            requestedAtEpochMs = System.currentTimeMillis(),
                        )).copy(
                            pendingFileCount = queuedIncomingOffers.size,
                            pendingFileNames = queuedIncomingOffers.map { it.fileName },
                        )
                        _incomingConnectionRequest.value = updatedRequest
                        if (existing == null) {
                            launchIncomingPromptUi(updatedRequest)
                        } else if (!isUiInForeground && backgroundConnectionDialogEnabled) {
                            showIncomingConnectionNotification(updatedRequest)
                        }
                    }

                    IncomingGateState.REJECTED -> Unit

                    IncomingGateState.NONE, IncomingGateState.APPROVED -> {
                        onTransferOfferReceived(frame)
                    }
                }
            }

            is Frame.FileChunkHeader -> fileTransferManager?.onChunkHeaderReceived(frame)

            is Frame.FileAck -> {
                val transfer = findTransfer(frame.messageId) ?: return
                onTransferAckReceived(transfer, frame)
            }

            is Frame.Ping -> serviceScope.launch {
                peerConnection?.send(Frame.Pong(frame.sequence, frame.sentAtEpochMs))
            }

            is Frame.Pong -> connectionMonitor?.onPongReceived(frame)
        }
    }

    private fun triggerIncomingConnectionPrompt(peerName: String, peerAddress: String) {
        val existing = _incomingConnectionRequest.value
        if (existing != null) {
            val updated = existing.copy(
                peerName = peerName.ifBlank { existing.peerName },
                peerAddress = peerAddress.ifBlank { existing.peerAddress },
                pendingFileCount = queuedIncomingOffers.size,
                pendingFileNames = queuedIncomingOffers.map { it.fileName },
            )
            _incomingConnectionRequest.value = updated
            if (!isUiInForeground && _visibilityMode.value.isVisible && backgroundConnectionDialogEnabled) {
                showIncomingConnectionNotification(updated, isInitialPrompt = false)
            }
            return
        }
        val request = IncomingConnectionRequest(
            requestId = UUID.randomUUID().toString(),
            peerName = peerName,
            peerAddress = peerAddress,
            requestedAtEpochMs = System.currentTimeMillis(),
            pendingFileCount = queuedIncomingOffers.size,
            pendingFileNames = queuedIncomingOffers.map { it.fileName },
        )
        _incomingConnectionRequest.value = request
        launchIncomingPromptUi(request)
    }

    private fun launchIncomingPromptUi(request: IncomingConnectionRequest) {
        incomingRequestTimeoutJob?.cancel()
        incomingRequestTimeoutJob = serviceScope.launch {
            delay(INCOMING_REQUEST_TIMEOUT_MS)
            if (_incomingConnectionRequest.value?.requestId == request.requestId &&
                incomingGateState == IncomingGateState.WAITING_DECISION
            ) {
                declineIncomingConnection()
            }
        }

        if (!isUiInForeground && _visibilityMode.value.isVisible && backgroundConnectionDialogEnabled) {
            showIncomingConnectionNotification(request, isInitialPrompt = true)
            launchIncomingConnectionActivity()
        }
    }

    private fun launchIncomingConnectionActivity() {
        runCatching {
            val dialogIntent = Intent(this, IncomingConnectionActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(dialogIntent)
        }.onFailure {
            Log.w(TAG, "launchIncomingConnectionActivity fallback to notification: ${it.message}")
        }
    }

    private fun resetIncomingGate() {
        incomingRequestTimeoutJob?.cancel()
        incomingGateState = IncomingGateState.NONE
        _incomingConnectionRequest.value = null
        queuedIncomingOffers.clear()
        cancelIncomingConnectionNotification()
    }

    private fun observeTransferProgress() {
        progressEventJob?.cancel()
        progressEventJob = serviceScope.launch {
            fileTransferManager?.progressEvents?.collect { event ->
                when (event) {
                    is TransferProgressEvent.Progress -> {
                        onTransferProgress(event.messageId, event.bytesTransferred, event.totalBytes)
                    }
                    is TransferProgressEvent.Completed -> {
                        val transfer = findTransfer(event.messageId) ?: return@collect
                        onTransferCompleted(transfer, event.file)
                    }
                    is TransferProgressEvent.Failed -> {
                        val transfer = findTransfer(event.messageId) ?: return@collect
                        onTransferFailed(transfer, event.reason)
                    }
                }
            }
        }
    }

    private fun observeQuality() {
        qualityJob?.cancel()
        qualityJob = serviceScope.launch {
            connectionMonitor?.quality?.collect { q ->
                _connectionQuality.value = q
            }
        }
    }

    private fun observeDistance() {
        distanceJob?.cancel()
        distanceJob = serviceScope.launch {
            connectionMonitor?.estimatedDistanceMeters?.collect { meters ->
                _estimatedDistanceMeters.value = meters
            }
        }
    }

    // ---- File transfer queue ----

    fun sendTransferFiles(files: List<PickedFile>) {
        if (files.isEmpty()) return
        val now = System.currentTimeMillis()
        val peerName = _connectedPeerName.value.orEmpty()
        val peerAddress = _connectedPeerAddress.value.orEmpty()
        val items = files.map { file ->
            TransferItem(
                direction = TransferDirection.SENT,
                fileName = file.name,
                fileSizeBytes = file.sizeBytes,
                mimeType = file.mimeType,
                state = TransferState.PENDING,
                createdAtEpochMs = now,
                peerName = peerName,
                peerDeviceAddress = peerAddress,
                sourceUri = file.uri.toString(),
            )
        }
        _transfers.value = _transfers.value + items
        items.forEach { dispatchTransferSend(it.id) }
    }

    fun retryTransfer(transferId: String) {
        val item = findTransfer(transferId) ?: return
        if (item.direction != TransferDirection.SENT || item.sourceUri == null) return
        if (item.state != TransferState.FAILED && item.state != TransferState.CANCELLED) return

        fileTransferManager?.clearOutgoingCancel(transferId)
        updateTransfer(transferId) {
            it.copy(
                state = TransferState.PENDING,
                bytesTransferred = 0L,
                speedBytesPerSec = 0L,
                errorMessage = null,
                createdAtEpochMs = System.currentTimeMillis(),
                peerName = _connectedPeerName.value.orEmpty(),
                peerDeviceAddress = _connectedPeerAddress.value.orEmpty(),
            )
        }
        dispatchTransferSend(transferId)
    }

    fun cancelTransfer(transferId: String) {
        val item = findTransfer(transferId)?.takeIf { it.isActive } ?: return
        transferSamples.remove(transferId)
        updateTransfer(transferId) { it.copy(state = TransferState.CANCELLED, speedBytesPerSec = 0L) }

        when (item.direction) {
            TransferDirection.SENT -> {
                if (item.state == TransferState.IN_PROGRESS) {
                    fileTransferManager?.cancelOutgoing(transferId)
                    serviceScope.launch {
                        peerConnection?.send(Frame.FileAck(transferId, success = false, cancelled = true))
                    }
                }
            }
            TransferDirection.RECEIVED -> {
                fileTransferManager?.cancelTransfer(transferId)
                serviceScope.launch {
                    peerConnection?.send(Frame.FileAck(transferId, success = false, cancelled = true))
                }
            }
        }
    }

    fun dismissTransfer(transferId: String) {
        _transfers.value = _transfers.value.filterNot { it.id == transferId && it.isFinished }
    }

    fun clearFinishedTransfers() {
        _transfers.value = _transfers.value.filter { it.isActive }
    }

    private fun dispatchTransferSend(transferId: String) {
        serviceScope.launch {
            transferSendMutex.withLock {
                val item = findTransfer(transferId)?.takeIf { it.state == TransferState.PENDING } ?: return@withLock

                val uri = item.sourceUri?.let { runCatching { Uri.parse(it) }.getOrNull() }
                val connection = peerConnection
                val manager = fileTransferManager
                if (uri == null) {
                    failTransfer(transferId, getString(R.string.transfer_error_cannot_open))
                    return@withLock
                }
                if (connection == null || manager == null) {
                    failTransfer(transferId, getString(R.string.transfer_error_not_connected))
                    return@withLock
                }

                updateTransfer(transferId) {
                    it.copy(state = TransferState.IN_PROGRESS, bytesTransferred = 0L, speedBytesPerSec = 0L, errorMessage = null)
                }
                val offerSent = connection.send(
                    Frame.FileOffer(
                        messageId = item.id,
                        fileName = item.fileName,
                        fileSizeBytes = item.fileSizeBytes,
                        mimeType = item.mimeType,
                        messageType = "FILE",
                        timestampEpochMs = System.currentTimeMillis(),
                        channel = Frame.CHANNEL_TRANSFER,
                    ),
                )
                if (!offerSent) {
                    failTransfer(transferId, getString(R.string.transfer_error_connection_lost))
                    return@withLock
                }

                val sent = manager.sendFile(item.id, uri, item.fileName, item.fileSizeBytes)
                if (!sent && findTransfer(transferId)?.state == TransferState.IN_PROGRESS) {
                    failTransfer(transferId, getString(R.string.transfer_error_connection_lost))
                }
            }
        }
    }

    private fun onTransferOfferReceived(offer: Frame.FileOffer) {
        val manager = fileTransferManager ?: return
        val repository = receivedFilesRepository
        val existingNames = repository.files.value.mapTo(HashSet()) { it.fileName }
        val initialSafeName = FileNameUtils.sanitize(offer.fileName)

        try {
            val resolvedName = manager.onFileOfferReceived(
                offer = offer,
                destinationDir = repository.directory,
                stagingDir = repository.stagingDirectory,
                existingNames = existingNames,
            )
            val item = TransferItem(
                id = offer.messageId,
                direction = TransferDirection.RECEIVED,
                fileName = resolvedName,
                fileSizeBytes = offer.fileSizeBytes.coerceAtLeast(0L),
                mimeType = offer.mimeType.ifBlank { FileNameUtils.DEFAULT_MIME_TYPE },
                state = TransferState.IN_PROGRESS,
                peerName = _connectedPeerName.value.orEmpty(),
                peerDeviceAddress = _connectedPeerAddress.value.orEmpty(),
            )
            _transfers.value = _transfers.value.filterNot { it.id == item.id } + item
            transferSamples.remove(item.id)
        } catch (e: Exception) {
            Log.w(TAG, "Could not accept incoming file ${offer.fileName}: ${e.message}")
            val failedItem = TransferItem(
                id = offer.messageId,
                direction = TransferDirection.RECEIVED,
                fileName = initialSafeName,
                fileSizeBytes = offer.fileSizeBytes.coerceAtLeast(0L),
                mimeType = offer.mimeType.ifBlank { FileNameUtils.DEFAULT_MIME_TYPE },
                state = TransferState.FAILED,
                peerName = _connectedPeerName.value.orEmpty(),
                peerDeviceAddress = _connectedPeerAddress.value.orEmpty(),
                errorMessage = e.message ?: getString(R.string.transfer_error_no_space),
            )
            _transfers.value = _transfers.value.filterNot { it.id == failedItem.id } + failedItem
            serviceScope.launch { peerConnection?.send(Frame.FileAck(failedItem.id, success = false)) }
        }
    }

    private fun onTransferAckReceived(transfer: TransferItem, ack: Frame.FileAck) {
        when (transfer.direction) {
            TransferDirection.SENT -> {
                if (ack.success) {
                    if (transfer.isActive) markTransferSent(transfer.id)
                } else {
                    fileTransferManager?.cancelOutgoing(transfer.id)
                    transferSamples.remove(transfer.id)
                    if (transfer.state != TransferState.CANCELLED) {
                        updateTransfer(transfer.id) {
                            it.copy(
                                state = if (ack.cancelled) TransferState.CANCELLED else TransferState.FAILED,
                                speedBytesPerSec = 0L,
                                errorMessage = if (ack.cancelled) null else getString(R.string.transfer_error_receiver_failed),
                            )
                        }
                    }
                }
            }
            TransferDirection.RECEIVED -> {
                if (!ack.success && transfer.isActive) {
                    fileTransferManager?.cancelTransfer(transfer.id)
                    transferSamples.remove(transfer.id)
                    updateTransfer(transfer.id) {
                        it.copy(
                            state = if (ack.cancelled) TransferState.CANCELLED else TransferState.FAILED,
                            speedBytesPerSec = 0L,
                            errorMessage = if (ack.cancelled) null else getString(R.string.transfer_error_sender_failed),
                        )
                    }
                }
            }
        }
    }

    private fun onTransferProgress(transferId: String, transferred: Long, total: Long) {
        val now = SystemClock.elapsedRealtime()
        val sample = transferSamples.getOrPut(transferId) {
            TransferSample(lastUiUpdateMs = 0L, lastSpeedMs = now, lastSpeedBytes = 0L, speedBytesPerSec = 0.0)
        }
        val finished = total > 0 && transferred >= total
        if (!finished && now - sample.lastUiUpdateMs < PROGRESS_UI_INTERVAL_MS) return

        val elapsed = now - sample.lastSpeedMs
        if (elapsed > 0) {
            val instantaneous = (transferred - sample.lastSpeedBytes) * 1000.0 / elapsed
            sample.speedBytesPerSec = if (sample.speedBytesPerSec <= 0.0) instantaneous else sample.speedBytesPerSec * 0.7 + instantaneous * 0.3
            sample.lastSpeedMs = now
            sample.lastSpeedBytes = transferred
        }
        sample.lastUiUpdateMs = now

        val speed = sample.speedBytesPerSec.toLong().coerceAtLeast(0L)
        updateTransfer(transferId) {
            if (it.isActive) {
                it.copy(state = TransferState.IN_PROGRESS, bytesTransferred = transferred, speedBytesPerSec = speed)
            } else {
                it
            }
        }
    }

    private fun onTransferCompleted(transfer: TransferItem, file: File) {
        transferSamples.remove(transfer.id)
        when (transfer.direction) {
            TransferDirection.SENT -> if (transfer.isActive) markTransferSent(transfer.id)
            TransferDirection.RECEIVED -> onTransferReceived(transfer, file)
        }
    }

    private fun markTransferSent(transferId: String) {
        updateTransfer(transferId) {
            it.copy(state = TransferState.COMPLETED, bytesTransferred = it.fileSizeBytes, speedBytesPerSec = 0L, errorMessage = null)
        }
    }

    private fun onTransferReceived(transfer: TransferItem, file: File) {
        if (transfer.state == TransferState.CANCELLED) {
            runCatching { file.delete() }
            return
        }

        val size = file.length()
        val record = ReceivedFile(
            id = transfer.id,
            fileName = file.name,
            sizeBytes = size,
            mimeType = transfer.mimeType,
            filePath = file.absolutePath,
            peerName = transfer.peerName,
            peerDeviceAddress = transfer.peerDeviceAddress,
            receivedAtEpochMs = System.currentTimeMillis(),
            seen = false,
        )
        serviceScope.launch {
            receivedFilesRepository.add(record)
            if (autoSaveToDownloadsEnabled) {
                FileActions.exportToDownloads(applicationContext, file, file.name, transfer.mimeType)
            }
        }

        updateTransfer(transfer.id) {
            it.copy(
                fileName = file.name,
                fileSizeBytes = size,
                bytesTransferred = size,
                state = TransferState.COMPLETED,
                speedBytesPerSec = 0L,
                localPath = file.absolutePath,
                errorMessage = null,
            )
        }
        serviceScope.launch { peerConnection?.send(Frame.FileAck(transfer.id, success = true)) }

        _receivedFileEvents.tryEmit(file.name)
        if (!isUiInForeground) showFileReceivedNotification(transfer.peerName, file.name)
    }

    private fun onTransferFailed(transfer: TransferItem, reason: String) {
        transferSamples.remove(transfer.id)
        if (!transfer.isActive) return
        updateTransfer(transfer.id) {
            it.copy(state = TransferState.FAILED, speedBytesPerSec = 0L, errorMessage = reason)
        }
        serviceScope.launch { peerConnection?.send(Frame.FileAck(transfer.id, success = false)) }
    }

    private fun failTransfer(transferId: String, reason: String) {
        transferSamples.remove(transferId)
        updateTransfer(transferId) {
            if (it.isActive) it.copy(state = TransferState.FAILED, speedBytesPerSec = 0L, errorMessage = reason) else it
        }
    }

    private fun findTransfer(transferId: String): TransferItem? = _transfers.value.firstOrNull { it.id == transferId }

    private inline fun updateTransfer(transferId: String, transform: (TransferItem) -> TransferItem) {
        _transfers.value = _transfers.value.map { if (it.id == transferId) transform(it) else it }
    }

    private fun failInFlightTransfers() {
        fileTransferManager?.abortAll()
        transferSamples.clear()

        if (_transfers.value.any { it.isActive }) {
            val reason = getString(R.string.transfer_error_connection_lost)
            _transfers.value = _transfers.value.map {
                if (it.isActive) it.copy(state = TransferState.FAILED, speedBytesPerSec = 0L, errorMessage = reason) else it
            }
        }
    }

    private fun persistSavedDevice(address: String, name: String) {
        if (address.isBlank()) return
        serviceScope.launch {
            val currentNickname = app.preferencesRepository.savedDevices
                .first()
                .firstOrNull { it.deviceAddress == address }
                ?.nickname

            app.preferencesRepository.upsertSavedDevice(
                SavedDevice(
                    deviceAddress = address,
                    displayName = name,
                    nickname = currentNickname,
                    lastConnectedAtEpochMs = System.currentTimeMillis(),
                ),
            )
        }
    }

    private fun tearDownActiveConnection() {
        connectionMonitor?.stop()
        failInFlightTransfers()
        resetIncomingGate()
        peerConnection?.close()
        socketEventJob?.cancel()
        progressEventJob?.cancel()
        qualityJob?.cancel()
        distanceJob?.cancel()
        peerConnection = null
        connectionMonitor = null
        fileTransferManager = null
        _isSocketConnected.value = false
        _connectionQuality.value = ConnectionQuality.DISCONNECTED
        _estimatedDistanceMeters.value = null
    }

    // ---- Notifications ----

    private fun defaultVisibilityNotificationText(): String = when (_visibilityMode.value) {
        DeviceVisibilityMode.HIDDEN -> getString(R.string.notif_hidden)
        DeviceVisibilityMode.TEMPORARY_10_MIN -> getString(
            R.string.notif_visible_10min,
            formatCountdown(_visibilityRemainingSeconds.value ?: TEMPORARY_VISIBILITY_SECONDS),
        )
        DeviceVisibilityMode.ALWAYS_VISIBLE -> getString(R.string.notif_visible_always)
    }

    private fun startForegroundWithNotification(contentText: String) {
        runCatching {
            val notification = buildNotification(contentText)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        }.onFailure {
            Log.w(TAG, "startForegroundWithNotification fallback: ${it.message}")
        }
    }

    private fun updateNotification(contentText: String) {
        runCatching {
            val manager = getSystemService(NotificationManager::class.java)
            manager?.notify(NOTIFICATION_ID, buildNotification(contentText))
        }
    }

    private fun buildNotification(contentText: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        return NotificationCompat.Builder(this, ShareApplication.CHANNEL_CONNECTION_STATUS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(contentText)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun openAppPendingIntent(requestCode: Int, openReceivedFiles: Boolean = false): PendingIntent =
        PendingIntent.getActivity(
            this,
            requestCode,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                if (openReceivedFiles) putExtra(MainActivity.EXTRA_OPEN_RECEIVED_FILES, true)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun showIncomingConnectionNotification(
        request: IncomingConnectionRequest,
        isInitialPrompt: Boolean = false,
    ) {
        runCatching {
            val openDialogIntent = PendingIntent.getActivity(
                this,
                10,
                Intent(this, IncomingConnectionActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val acceptIntent = PendingIntent.getService(
                this,
                11,
                Intent(this, WifiDirectService::class.java).setAction(ACTION_ACCEPT_INCOMING),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val declineIntent = PendingIntent.getService(
                this,
                12,
                Intent(this, WifiDirectService::class.java).setAction(ACTION_DECLINE_INCOMING),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

            val body = if (request.pendingFileCount > 0) {
                getString(R.string.incoming_conn_message_files, request.peerName, request.pendingFileCount)
            } else {
                getString(R.string.incoming_conn_notif_body)
            }

            val builder = NotificationCompat.Builder(this, ShareApplication.CHANNEL_INCOMING_CONNECTION)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getString(R.string.incoming_conn_notif_title, request.peerName))
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(openDialogIntent)
                .setFullScreenIntent(openDialogIntent, true)
                .addAction(0, getString(R.string.incoming_conn_accept), acceptIntent)
                .addAction(0, getString(R.string.incoming_conn_decline), declineIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(!isInitialPrompt)
                .setAutoCancel(false)

            if (!soundVibrationEnabled) {
                builder.setSilent(true)
            } else if (isInitialPrompt) {
                builder.setDefaults(NotificationCompat.DEFAULT_ALL)
            }

            getSystemService(NotificationManager::class.java)
                ?.notify(INCOMING_CONN_NOTIFICATION_ID, builder.build())
        }.onFailure {
            Log.w(TAG, "showIncomingConnectionNotification failed: ${it.message}")
        }
    }

    private fun cancelIncomingConnectionNotification() {
        runCatching {
            getSystemService(NotificationManager::class.java)?.cancel(INCOMING_CONN_NOTIFICATION_ID)
        }
    }

    private fun showFileReceivedNotification(senderName: String, fileName: String) {
        runCatching {
            val sender = senderName.ifBlank { getString(R.string.transfer_unknown_peer) }
            val builder = NotificationCompat.Builder(this, ShareApplication.CHANNEL_RECEIVED_FILES)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getString(R.string.notif_file_received_title))
                .setContentText(getString(R.string.notif_file_received_text, fileName, sender))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setContentIntent(openAppPendingIntent(5, openReceivedFiles = true))
                .setAutoCancel(true)

            if (!soundVibrationEnabled) {
                builder.setSilent(true)
            }

            getSystemService(NotificationManager::class.java)?.notify(NEW_FILE_NOTIFICATION_ID, builder.build())
        }.onFailure {
            Log.w(TAG, "showFileReceivedNotification failed: ${it.message}")
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): WifiDirectService = this@WifiDirectService
    }

    companion object {
        private const val TAG = "WifiDirectService"
        private const val NOTIFICATION_ID = 1001
        private const val INCOMING_CONN_NOTIFICATION_ID = 1003
        private const val NEW_FILE_NOTIFICATION_ID = 1004
        private const val PROGRESS_UI_INTERVAL_MS = 150L
        private const val TEMPORARY_VISIBILITY_SECONDS = 10 * 60
        private const val INCOMING_REQUEST_TIMEOUT_MS = 60_000L
        const val ACTION_ACCEPT_INCOMING = "com.thazh.share.action.ACCEPT_INCOMING"
        const val ACTION_DECLINE_INCOMING = "com.thazh.share.action.DECLINE_INCOMING"
        const val ACTION_SET_VISIBILITY_FROM_TILE = "com.thazh.share.action.SET_VISIBILITY_FROM_TILE"
        const val EXTRA_VISIBILITY_MODE = "com.thazh.share.extra.VISIBILITY_MODE"
        const val WILDCARD_BIND_ADDRESS = "0.0.0.0"
        const val DEFAULT_GROUP_OWNER_IP = "192.168.49.1"

        fun formatCountdown(seconds: Int): String {
            val safe = seconds.coerceAtLeast(0)
            val m = safe / 60
            val s = safe % 60
            return String.format(java.util.Locale.US, "%02d:%02d", m, s)
        }

        fun bind(context: Context, connection: android.content.ServiceConnection) {
            val intent = Intent(context, WifiDirectService::class.java)
            runCatching { context.startService(intent) }
            runCatching { context.bindService(intent, connection, Context.BIND_AUTO_CREATE) }
        }
    }
}
