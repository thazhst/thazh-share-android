package com.thazh.share.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.net.Uri
import android.os.BatteryManager
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.thazh.share.ShareApplication
import com.thazh.share.data.AppLanguage
import com.thazh.share.data.AppThemeMode
import com.thazh.share.data.BackgroundPromptMode
import com.thazh.share.data.ConnectionQuality
import com.thazh.share.data.DeviceVisibilityMode
import com.thazh.share.data.DiscoveredPeer
import com.thazh.share.data.IncomingConnectionRequest
import com.thazh.share.data.PickedFile
import com.thazh.share.data.ReceivedFile
import com.thazh.share.data.RenameResult
import com.thazh.share.data.SavedDevice
import com.thazh.share.data.SharedLinkItem
import com.thazh.share.data.TransferItem
import com.thazh.share.network.CatboxExpiry
import com.thazh.share.network.CatboxUploadProgress
import com.thazh.share.network.CatboxUploadResult
import com.thazh.share.network.CatboxUploader
import com.thazh.share.network.NfcConnectionManager
import com.thazh.share.network.P2pConnectionState
import com.thazh.share.service.WifiDirectService
import com.thazh.share.util.FileActions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed class ConnectScreenState {
    data object Discovering : ConnectScreenState()
    data object Connecting : ConnectScreenState()
    data class Connected(val peerName: String) : ConnectScreenState()
    data class Error(val message: String) : ConnectScreenState()
}

class ShareViewModel(application: Application) : AndroidViewModel(application) {

    private val app: ShareApplication get() = getApplication()

    private var service: WifiDirectService? = null
    private var isServiceBound = false
    private var latestUiInForeground = false
    private val bridgeJobs = mutableListOf<kotlinx.coroutines.Job>()

    private val _isServiceReady = MutableStateFlow(false)
    val isServiceReady: StateFlow<Boolean> = _isServiceReady.asStateFlow()

    private val _screenState = MutableStateFlow<ConnectScreenState>(ConnectScreenState.Discovering)
    val screenState: StateFlow<ConnectScreenState> = _screenState.asStateFlow()

    val displayName: StateFlow<String> = app.preferencesRepository.displayName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val savedDevices: StateFlow<List<SavedDevice>> = app.preferencesRepository.savedDevices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bleFastDiscoveryEnabled: StateFlow<Boolean> = app.preferencesRepository.bleFastDiscoveryEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val reduceTransparency: StateFlow<Boolean> = app.preferencesRepository.reduceTransparency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val liquidGlassEnabled: StateFlow<Boolean> = app.preferencesRepository.liquidGlassEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val defaultVisibilityMode: StateFlow<DeviceVisibilityMode> = app.preferencesRepository.defaultVisibilityMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DeviceVisibilityMode.TEMPORARY_10_MIN)

    val confirmIncomingConnection: StateFlow<Boolean> = app.preferencesRepository.confirmIncomingConnection
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val backgroundConnectionDialog: StateFlow<Boolean> = app.preferencesRepository.backgroundConnectionDialog
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val backgroundPromptMode: StateFlow<BackgroundPromptMode> = app.preferencesRepository.backgroundPromptMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BackgroundPromptMode.NOTIFICATION)

    val appLanguage: StateFlow<AppLanguage> = app.preferencesRepository.appLanguage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppLanguage.VIETNAMESE)

    val autoSaveToDownloads: StateFlow<Boolean> = app.preferencesRepository.autoSaveToDownloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val soundVibrationEnabled: StateFlow<Boolean> = app.preferencesRepository.soundVibrationEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val themeMode: StateFlow<AppThemeMode> = app.preferencesRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppThemeMode.SYSTEM)

    val syncDeviceNameWithAccount: StateFlow<Boolean> = app.preferencesRepository.syncDeviceNameWithAccount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private var accountNameSyncJob: kotlinx.coroutines.Job? = null
    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        restartAccountDeviceNameSync(user)
    }

    private val _localBatteryPercent = MutableStateFlow<Int?>(null)
    val localBatteryPercent: StateFlow<Int?> = _localBatteryPercent.asStateFlow()

    private val _localBatteryCharging = MutableStateFlow(false)
    val localBatteryCharging: StateFlow<Boolean> = _localBatteryCharging.asStateFlow()

    private val _connectedPeerBatteryPercent = MutableStateFlow<Int?>(null)
    val connectedPeerBatteryPercent: StateFlow<Int?> = _connectedPeerBatteryPercent.asStateFlow()

    private val _connectedPeerCharging = MutableStateFlow(false)
    val connectedPeerCharging: StateFlow<Boolean> = _connectedPeerCharging.asStateFlow()

    private val _peerBatteryLevels = MutableStateFlow<Map<String, Pair<Int, Boolean>>>(emptyMap())
    val peerBatteryLevels: StateFlow<Map<String, Pair<Int, Boolean>>> = _peerBatteryLevels.asStateFlow()

    private val _pendingConnectPeer = MutableStateFlow<DiscoveredPeer?>(null)
    val pendingConnectPeer: StateFlow<DiscoveredPeer?> = _pendingConnectPeer.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            sampleInitialLocalBattery()
        }
        viewModelScope.launch {
            runCatching {
                FirebaseAuth.getInstance().addAuthStateListener(authStateListener)
            }
        }
    }

    private fun sampleInitialLocalBattery() {
        runCatching {
            val context = getApplication<Application>()
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val cap = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.takeIf { it in 0..100 }
            if (cap != null) {
                _localBatteryPercent.value = cap
            }
            val sticky = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            if (sticky != null) {
                val level = sticky.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = sticky.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level >= 0 && scale > 0) {
                    _localBatteryPercent.value = ((level * 100f) / scale.toFloat()).toInt().coerceIn(0, 100)
                }
                val status = sticky.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                _localBatteryCharging.value = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
            }
        }
    }

    private val _visibilityMode = MutableStateFlow(DeviceVisibilityMode.TEMPORARY_10_MIN)
    val visibilityMode: StateFlow<DeviceVisibilityMode> = _visibilityMode.asStateFlow()

    private val _visibilityRemainingSeconds = MutableStateFlow<Int?>(600)
    val visibilityRemainingSeconds: StateFlow<Int?> = _visibilityRemainingSeconds.asStateFlow()

    private val _incomingConnectionRequest = MutableStateFlow<IncomingConnectionRequest?>(null)
    val incomingConnectionRequest: StateFlow<IncomingConnectionRequest?> = _incomingConnectionRequest.asStateFlow()

    private val _peers = MutableStateFlow<List<DiscoveredPeer>>(emptyList())
    val peers: StateFlow<List<DiscoveredPeer>> = _peers.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private val _connectedPeerName = MutableStateFlow<String?>(null)
    val connectedPeerName: StateFlow<String?> = _connectedPeerName.asStateFlow()

    private val _isSocketConnected = MutableStateFlow(false)
    val isSocketConnected: StateFlow<Boolean> = _isSocketConnected.asStateFlow()

    private val _connectionQuality = MutableStateFlow(ConnectionQuality.DISCONNECTED)
    val connectionQuality: StateFlow<ConnectionQuality> = _connectionQuality.asStateFlow()

    private val _estimatedDistanceMeters = MutableStateFlow<Int?>(null)
    val estimatedDistanceMeters: StateFlow<Int?> = _estimatedDistanceMeters.asStateFlow()

    private val _transfers = MutableStateFlow<List<TransferItem>>(emptyList())
    val transfers: StateFlow<List<TransferItem>> = _transfers.asStateFlow()

    val receivedFiles: StateFlow<List<ReceivedFile>> get() = app.receivedFilesRepository.files

    val unseenReceivedCount: StateFlow<Int> = app.receivedFilesRepository.files
        .map { files -> files.count { !it.seen } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _receivedFileEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val receivedFileEvents: SharedFlow<String> = _receivedFileEvents.asSharedFlow()

    private val nfcConnectionManager by lazy { NfcConnectionManager(application) }

    val isNfcSupported: Boolean
        get() = nfcConnectionManager.isSupported()

    private val _isNfcEnabled = MutableStateFlow(nfcConnectionManager.isEnabled())
    val isNfcEnabled: StateFlow<Boolean> = _isNfcEnabled.asStateFlow()

    private val _nfcPairingPeerName = MutableStateFlow<String?>(null)
    val nfcPairingPeerName: StateFlow<String?> = _nfcPairingPeerName.asStateFlow()

    private val _openReceivedFilesRequested = MutableStateFlow(false)
    val openReceivedFilesRequested: StateFlow<Boolean> = _openReceivedFilesRequested.asStateFlow()

    private val _pendingWifiDirectFiles = MutableStateFlow<List<PickedFile>>(emptyList())
    val pendingWifiDirectFilesCount: StateFlow<Int> = _pendingWifiDirectFiles
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Catbox.moe Link Share state
    private val _selectedCatboxExpiry = MutableStateFlow(CatboxExpiry.PERMANENT)
    val selectedCatboxExpiry: StateFlow<CatboxExpiry> = _selectedCatboxExpiry.asStateFlow()

    private val _isUploadingCatbox = MutableStateFlow(false)
    val isUploadingCatbox: StateFlow<Boolean> = _isUploadingCatbox.asStateFlow()

    private val _catboxUploadProgress = MutableStateFlow<CatboxUploadProgress?>(null)
    val catboxUploadProgress: StateFlow<CatboxUploadProgress?> = _catboxUploadProgress.asStateFlow()

    private val _catboxUploadError = MutableStateFlow<String?>(null)
    val catboxUploadError: StateFlow<String?> = _catboxUploadError.asStateFlow()

    private val _sessionCatboxLinks = MutableStateFlow<List<CatboxUploadResult>>(emptyList())
    val sessionCatboxLinks: StateFlow<List<CatboxUploadResult>> = _sessionCatboxLinks.asStateFlow()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as? WifiDirectService.LocalBinder ?: return
            val bound = localBinder.getService()
            service = bound
            bound.setUiInForeground(latestUiInForeground)
            _isServiceReady.value = true
            bridgeServiceFlows()
            viewModelScope.launch {
                val display = displayName.value.ifBlank { app.preferencesRepository.displayName.first() }
                bound.setLocalDisplayName(display)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            _isServiceReady.value = false
        }
    }

    fun bindService(context: Context) {
        if (isServiceBound) return
        WifiDirectService.bind(context, serviceConnection)
        isServiceBound = true
    }

    fun unbindService(context: Context) {
        if (!isServiceBound) return
        runCatching { context.unbindService(serviceConnection) }
        isServiceBound = false
    }

    fun setUiInForeground(inForeground: Boolean) {
        latestUiInForeground = inForeground
        service?.setUiInForeground(inForeground)
    }

    private fun bridgeServiceFlows() {
        bridgeJobs.forEach { it.cancel() }
        bridgeJobs.clear()
        val s = service ?: return
        bridgeJobs += viewModelScope.launch { s.peers.collect { _peers.value = it } }
        bridgeJobs += viewModelScope.launch { s.wifiDirectManager.isDiscovering.collect { _isDiscovering.value = it } }
        bridgeJobs += viewModelScope.launch { s.visibilityMode.collect { _visibilityMode.value = it } }
        bridgeJobs += viewModelScope.launch { s.visibilityRemainingSeconds.collect { _visibilityRemainingSeconds.value = it } }
        bridgeJobs += viewModelScope.launch { s.incomingConnectionRequest.collect { _incomingConnectionRequest.value = it } }
        bridgeJobs += viewModelScope.launch { s.connectedPeerName.collect { _connectedPeerName.value = it } }
        bridgeJobs += viewModelScope.launch {
            s.isSocketConnected.collect { connected ->
                _isSocketConnected.value = connected
                if (connected) {
                    val pending = _pendingWifiDirectFiles.value
                    if (pending.isNotEmpty()) {
                        _pendingWifiDirectFiles.value = emptyList()
                        s.sendTransferFiles(pending)
                    }
                }
            }
        }
        bridgeJobs += viewModelScope.launch { s.connectionQuality.collect { _connectionQuality.value = it } }
        bridgeJobs += viewModelScope.launch { s.estimatedDistanceMeters.collect { _estimatedDistanceMeters.value = it } }
        bridgeJobs += viewModelScope.launch { s.localBatteryPercent.collect { if (it != null) _localBatteryPercent.value = it } }
        bridgeJobs += viewModelScope.launch { s.localBatteryCharging.collect { _localBatteryCharging.value = it } }
        bridgeJobs += viewModelScope.launch { s.connectedPeerBatteryPercent.collect { _connectedPeerBatteryPercent.value = it } }
        bridgeJobs += viewModelScope.launch { s.connectedPeerCharging.collect { _connectedPeerCharging.value = it } }
        bridgeJobs += viewModelScope.launch { s.peerBatteryLevels.collect { _peerBatteryLevels.value = it } }
        bridgeJobs += viewModelScope.launch { s.transfers.collect { _transfers.value = it } }
        bridgeJobs += viewModelScope.launch { s.receivedFileEvents.collect { _receivedFileEvents.emit(it) } }
        bridgeJobs += viewModelScope.launch { s.nfcPairingPeerName.collect { _nfcPairingPeerName.value = it } }
        bridgeJobs += viewModelScope.launch {
            s.wifiDirectManager.connectionState.collect { state ->
                if (state !is P2pConnectionState.Connecting) {
                    _pendingConnectPeer.value = null
                }
                _screenState.value = when (state) {
                    is P2pConnectionState.Connecting -> ConnectScreenState.Connecting
                    is P2pConnectionState.Connected -> ConnectScreenState.Connected(_connectedPeerName.value ?: "Thiết bị")
                    is P2pConnectionState.Failed -> ConnectScreenState.Error(state.reason)
                    else -> ConnectScreenState.Discovering
                }
            }
        }
    }

    fun setVisibilityMode(mode: DeviceVisibilityMode) {
        service?.setVisibilityMode(mode)
    }

    fun setDefaultVisibilityMode(mode: DeviceVisibilityMode) {
        viewModelScope.launch {
            app.preferencesRepository.setDefaultVisibilityMode(mode)
        }
    }

    fun setConfirmIncomingConnection(enabled: Boolean) {
        viewModelScope.launch {
            app.preferencesRepository.setConfirmIncomingConnection(enabled)
        }
    }

    fun setBackgroundConnectionDialog(enabled: Boolean) {
        viewModelScope.launch {
            app.preferencesRepository.setBackgroundConnectionDialog(enabled)
        }
    }

    fun setBackgroundPromptMode(mode: BackgroundPromptMode) {
        viewModelScope.launch {
            app.preferencesRepository.setBackgroundPromptMode(mode)
        }
    }

    fun setAppLanguage(language: AppLanguage) {
        viewModelScope.launch {
            app.preferencesRepository.setAppLanguage(language)
        }
    }

    fun setAutoSaveToDownloads(enabled: Boolean) {
        viewModelScope.launch {
            app.preferencesRepository.setAutoSaveToDownloads(enabled)
        }
    }

    fun setSoundVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            app.preferencesRepository.setSoundVibrationEnabled(enabled)
        }
    }

    fun acceptIncomingConnection() {
        service?.acceptIncomingConnection()
    }

    fun declineIncomingConnection() {
        service?.declineIncomingConnection()
    }

    fun refreshNfcState() {
        _isNfcEnabled.value = nfcConnectionManager.isEnabled()
    }

    fun openNfcSettings() {
        nfcConnectionManager.openNfcSettings()
    }

    fun startDiscovery() {
        service?.startDiscovery { _, error ->
            if (error != null) _screenState.value = ConnectScreenState.Error(error)
        }
    }

    fun stopDiscovery() {
        service?.stopDiscovery()
    }

    fun connectToPeer(peer: DiscoveredPeer) {
        _pendingConnectPeer.value = peer
        _screenState.value = ConnectScreenState.Connecting
        service?.connectToPeer(peer.deviceAddress) { _, error ->
            if (error != null) {
                _pendingConnectPeer.value = null
                _screenState.value = ConnectScreenState.Error(error)
            }
        }
    }

    fun connectToSavedDevice(device: SavedDevice) {
        _pendingConnectPeer.value = DiscoveredPeer(
            deviceName = device.effectiveName,
            deviceAddress = device.deviceAddress,
            status = com.thazh.share.data.PeerStatus.INVITED,
        )
        _screenState.value = ConnectScreenState.Connecting
        service?.connectToPeer(device.deviceAddress) { _, error ->
            if (error != null) {
                _pendingConnectPeer.value = null
                _screenState.value = ConnectScreenState.Error(error)
            }
        }
    }

    fun cancelPendingConnection() {
        _pendingConnectPeer.value = null
        service?.disconnect()
        _screenState.value = ConnectScreenState.Discovering
        startDiscovery()
    }

    fun retryPendingConnection() {
        val target = _pendingConnectPeer.value
            ?: _peers.value.firstOrNull { it.status == com.thazh.share.data.PeerStatus.INVITED }
        service?.disconnect()
        if (target != null) {
            connectToPeer(target)
        } else {
            _screenState.value = ConnectScreenState.Discovering
            startDiscovery()
        }
    }

    fun disconnect() {
        _pendingConnectPeer.value = null
        service?.disconnect()
        _connectedPeerName.value = null
        _connectedPeerBatteryPercent.value = null
        _connectedPeerCharging.value = false
        _estimatedDistanceMeters.value = null
        _transfers.value = emptyList()
        _screenState.value = ConnectScreenState.Discovering
    }

    fun sendFilesForTransfer(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val context = getApplication<Application>()
        viewModelScope.launch {
            val picked = withContext(Dispatchers.IO) {
                uris.map { FileActions.queryPickedFile(context, it) }
            }
            service?.sendTransferFiles(picked)
        }
    }

    fun sendOrQueueFilesForWifiDirect(
        uris: List<Uri>,
        onResult: (sentImmediately: Boolean, count: Int) -> Unit = { _, _ -> },
    ) {
        if (uris.isEmpty()) return
        val context = getApplication<Application>()
        viewModelScope.launch {
            val picked = withContext(Dispatchers.IO) {
                uris.map { FileActions.queryPickedFile(context, it) }
            }
            val currentService = service
            if (_isSocketConnected.value && currentService != null) {
                currentService.sendTransferFiles(picked)
                onResult(true, picked.size)
            } else {
                _pendingWifiDirectFiles.value = _pendingWifiDirectFiles.value + picked
                startDiscovery()
                onResult(false, picked.size)
            }
        }
    }

    fun shareReceivedFilesViaWifiDirect(
        files: List<ReceivedFile>,
        onResult: (sentImmediately: Boolean, count: Int) -> Unit = { _, _ -> },
    ) {
        if (files.isEmpty()) return
        viewModelScope.launch {
            val picked = withContext(Dispatchers.IO) {
                files.mapNotNull { rf ->
                    val diskFile = File(rf.filePath)
                    if (!diskFile.isFile) return@mapNotNull null
                    PickedFile(
                        uri = Uri.fromFile(diskFile),
                        name = rf.fileName,
                        sizeBytes = diskFile.length().coerceAtLeast(rf.sizeBytes),
                        mimeType = rf.mimeType,
                    )
                }
            }
            if (picked.isEmpty()) {
                onResult(false, 0)
                return@launch
            }
            val currentService = service
            if (_isSocketConnected.value && currentService != null) {
                currentService.sendTransferFiles(picked)
                onResult(true, picked.size)
            } else {
                _pendingWifiDirectFiles.value = picked
                startDiscovery()
                onResult(false, picked.size)
            }
        }
    }

    fun clearPendingWifiDirectFiles() {
        _pendingWifiDirectFiles.value = emptyList()
    }

    // ---- Catbox.moe Link Share & Cloud History ----

    fun setCatboxExpiry(expiry: CatboxExpiry) {
        _selectedCatboxExpiry.value = expiry
    }

    fun clearSessionCatboxLinks() {
        _sessionCatboxLinks.value = emptyList()
    }

    fun uploadFilesToCatbox(
        uris: List<Uri>,
        expiry: CatboxExpiry,
        note: String = "",
    ) {
        if (uris.isEmpty() || _isUploadingCatbox.value) return
        val context = getApplication<Application>()
        viewModelScope.launch {
            _isUploadingCatbox.value = true
            _catboxUploadError.value = null
            try {
                val pickedFiles = withContext(Dispatchers.IO) {
                    uris.map { FileActions.queryPickedFile(context, it) }
                }
                for (picked in pickedFiles) {
                    val result = CatboxUploader.uploadFile(
                        context = context,
                        file = picked,
                        expiry = expiry,
                        note = note,
                        onProgress = { progress ->
                            _catboxUploadProgress.value = progress
                        },
                    )
                    result.onSuccess { uploaded ->
                        _sessionCatboxLinks.value = listOf(uploaded) + _sessionCatboxLinks.value
                        val currentUser = FirebaseAuth.getInstance().currentUser
                        if (currentUser != null) {
                            runCatching {
                                app.sharedLinkRepository.saveUploadedLink(currentUser.uid, uploaded)
                            }
                        }
                    }.onFailure { err ->
                        _catboxUploadError.value = err.localizedMessage ?: "Không thể tải tệp lên đám mây"
                    }
                }
            } finally {
                _isUploadingCatbox.value = false
                _catboxUploadProgress.value = null
            }
        }
    }

    fun observeCloudSharedLinks(userId: String): Flow<List<SharedLinkItem>> {
        return app.sharedLinkRepository.observeSharedLinks(userId) { err ->
            _catboxUploadError.value = err.localizedMessage
        }
    }

    fun updateCloudSharedLinkNote(userId: String, linkId: String, newNote: String) {
        viewModelScope.launch {
            runCatching {
                app.sharedLinkRepository.updateLinkNote(userId, linkId, newNote)
            }.onFailure { err ->
                _catboxUploadError.value = err.localizedMessage
            }
        }
    }

    fun deleteCloudSharedLink(userId: String, linkId: String) {
        viewModelScope.launch {
            runCatching {
                app.sharedLinkRepository.deleteSharedLink(userId, linkId)
            }.onFailure { err ->
                _catboxUploadError.value = err.localizedMessage
            }
        }
    }

    // ---- File Transfer Actions ----

    fun cancelTransfer(transferId: String) {
        service?.cancelTransfer(transferId)
    }

    fun retryTransfer(transferId: String) {
        service?.retryTransfer(transferId)
    }

    fun dismissTransfer(transferId: String) {
        service?.dismissTransfer(transferId)
    }

    fun clearFinishedTransfers() {
        service?.clearFinishedTransfers()
    }

    fun markReceivedFilesSeen() {
        viewModelScope.launch { app.receivedFilesRepository.markAllSeen() }
    }

    fun deleteReceivedFiles(files: List<ReceivedFile>, onDone: (removed: Int) -> Unit = {}) {
        if (files.isEmpty()) return
        viewModelScope.launch {
            val removed = app.receivedFilesRepository.delete(files.map { it.id }.toSet())
            onDone(removed)
        }
    }

    fun renameReceivedFile(file: ReceivedFile, newName: String, onDone: (RenameResult) -> Unit = {}) {
        viewModelScope.launch {
            onDone(app.receivedFilesRepository.rename(file.id, newName))
        }
    }

    fun saveReceivedFilesToDownloads(files: List<ReceivedFile>, onDone: (saved: Int, failed: Int) -> Unit) {
        if (files.isEmpty()) return
        val context = getApplication<Application>()
        viewModelScope.launch {
            var saved = 0
            for (file in files) {
                if (FileActions.exportToDownloads(context, File(file.filePath), file.fileName, file.mimeType)) saved++
            }
            onDone(saved, files.size - saved)
        }
    }

    fun requestOpenReceivedFiles() {
        _openReceivedFilesRequested.value = true
    }

    fun consumeOpenReceivedFilesRequest() {
        _openReceivedFilesRequested.value = false
    }

    fun updateDisplayName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            app.preferencesRepository.setDisplayName(trimmed)
            service?.setLocalDisplayName(trimmed)
            if (app.preferencesRepository.syncDeviceNameWithAccount.first()) {
                val user = runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull()
                if (user != null && user.uid.isNotBlank()) {
                    app.sharedLinkRepository.saveSyncedDeviceName(
                        userId = user.uid,
                        deviceName = trimmed,
                        email = user.email.orEmpty(),
                    )
                }
            }
        }
    }

    fun setSyncDeviceNameWithAccount(enabled: Boolean) {
        viewModelScope.launch {
            app.preferencesRepository.setSyncDeviceNameWithAccount(enabled)
            val user = runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull()
            restartAccountDeviceNameSync(user, forceEnabled = enabled)
        }
    }

    fun applyAccountNameAsDeviceName(accountDisplayName: String) {
        val trimmed = accountDisplayName.trim()
        if (trimmed.isEmpty()) return
        updateDisplayName(trimmed)
    }

    private fun restartAccountDeviceNameSync(
        user: com.google.firebase.auth.FirebaseUser?,
        forceEnabled: Boolean? = null,
    ) {
        accountNameSyncJob?.cancel()
        if (user == null || user.uid.isBlank()) return
        accountNameSyncJob = viewModelScope.launch {
            val syncEnabled = forceEnabled ?: app.preferencesRepository.syncDeviceNameWithAccount.first()
            if (!syncEnabled) return@launch

            runCatching {
                app.sharedLinkRepository.observeSyncedDeviceName(user.uid).collect { cloudName ->
                    val currentLocal = app.preferencesRepository.displayName.first().trim()
                    if (!cloudName.isNullOrBlank()) {
                        if (cloudName != currentLocal) {
                            app.preferencesRepository.setDisplayName(cloudName)
                            service?.setLocalDisplayName(cloudName)
                        }
                    } else {
                        val defaultModel = android.os.Build.MODEL?.trim().orEmpty()
                        val accountName = user.displayName?.trim().orEmpty()
                        val nameToSync = if (
                            (currentLocal.isEmpty() || currentLocal.equals(defaultModel, ignoreCase = true) || currentLocal == "Thiết bị") &&
                            accountName.isNotEmpty()
                        ) {
                            app.preferencesRepository.setDisplayName(accountName)
                            service?.setLocalDisplayName(accountName)
                            accountName
                        } else {
                            currentLocal.ifBlank { accountName.ifBlank { "Thiết bị" } }
                        }
                        app.sharedLinkRepository.saveSyncedDeviceName(
                            userId = user.uid,
                            deviceName = nameToSync,
                            email = user.email.orEmpty(),
                        )
                    }
                }
            }
        }
    }

    fun renameSavedDevice(deviceAddress: String, nickname: String) {
        viewModelScope.launch {
            app.preferencesRepository.renameSavedDevice(deviceAddress, nickname)
        }
    }

    fun removeSavedDevice(deviceAddress: String) {
        viewModelScope.launch {
            app.preferencesRepository.removeSavedDevice(deviceAddress)
        }
    }

    fun clearSavedDevices(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            app.preferencesRepository.clearSavedDevices()
            onDone()
        }
    }

    fun clearTempStagingCache(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                app.receivedFilesRepository.stagingDirectory.listFiles()?.forEach { file ->
                    runCatching { file.deleteRecursively() }
                }
            }
            onDone()
        }
    }

    fun setBleFastDiscoveryEnabled(enabled: Boolean) {
        viewModelScope.launch {
            app.preferencesRepository.setBleFastDiscoveryEnabled(enabled)
        }
    }

    fun setReduceTransparency(enabled: Boolean) {
        viewModelScope.launch {
            app.preferencesRepository.setReduceTransparency(enabled)
        }
    }

    fun setLiquidGlassEnabled(enabled: Boolean) {
        viewModelScope.launch {
            app.preferencesRepository.setLiquidGlassEnabled(enabled)
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            app.preferencesRepository.setThemeMode(mode)
        }
    }

    fun isBleFastDiscoverySupported(): Boolean = service?.isBleFastDiscoverySupported() ?: true

    fun retryBleDiscovery() {
        service?.retryBleDiscovery()
    }

    override fun onCleared() {
        super.onCleared()
        accountNameSyncJob?.cancel()
        runCatching {
            FirebaseAuth.getInstance().removeAuthStateListener(authStateListener)
        }
        service?.setUiInForeground(false)
    }
}
