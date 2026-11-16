package com.thazh.share.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.thazh.share.ShareApplication
import com.thazh.share.data.ConnectionQuality
import com.thazh.share.data.DiscoveredPeer
import com.thazh.share.data.PickedFile
import com.thazh.share.data.ReceivedFile
import com.thazh.share.data.RenameResult
import com.thazh.share.data.SavedDevice
import com.thazh.share.data.TransferItem
import com.thazh.share.network.NfcConnectionManager
import com.thazh.share.network.P2pConnectionState
import com.thazh.share.service.WifiDirectService
import com.thazh.share.util.FileActions
import kotlinx.coroutines.Dispatchers
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

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as? WifiDirectService.LocalBinder ?: return
            service = localBinder.getService()
            _isServiceReady.value = true
            bridgeServiceFlows()
            viewModelScope.launch {
                val display = displayName.value.ifBlank { app.preferencesRepository.displayName.first() }
                service?.setLocalDisplayName(display)
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

    private fun bridgeServiceFlows() {
        bridgeJobs.forEach { it.cancel() }
        bridgeJobs.clear()
        val s = service ?: return
        bridgeJobs += viewModelScope.launch { s.peers.collect { _peers.value = it } }
        bridgeJobs += viewModelScope.launch { s.wifiDirectManager.isDiscovering.collect { _isDiscovering.value = it } }
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
        bridgeJobs += viewModelScope.launch { s.transfers.collect { _transfers.value = it } }
        bridgeJobs += viewModelScope.launch { s.receivedFileEvents.collect { _receivedFileEvents.emit(it) } }
        bridgeJobs += viewModelScope.launch { s.nfcPairingPeerName.collect { _nfcPairingPeerName.value = it } }
        bridgeJobs += viewModelScope.launch {
            s.wifiDirectManager.connectionState.collect { state ->
                _screenState.value = when (state) {
                    is P2pConnectionState.Connecting -> ConnectScreenState.Connecting
                    is P2pConnectionState.Connected -> ConnectScreenState.Connected(_connectedPeerName.value ?: "Thiết bị")
                    is P2pConnectionState.Failed -> ConnectScreenState.Error(state.reason)
                    else -> ConnectScreenState.Discovering
                }
            }
        }
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
        _screenState.value = ConnectScreenState.Connecting
        service?.connectToPeer(peer.deviceAddress) { _, error ->
            if (error != null) _screenState.value = ConnectScreenState.Error(error)
        }
    }

    fun connectToSavedDevice(device: SavedDevice) {
        _screenState.value = ConnectScreenState.Connecting
        service?.connectToPeer(device.deviceAddress) { _, error ->
            if (error != null) _screenState.value = ConnectScreenState.Error(error)
        }
    }

    fun disconnect() {
        service?.disconnect()
        _connectedPeerName.value = null
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

    /**
     * Shares one or more [ReceivedFile]s over Wi-Fi Direct in a single operation.
     * - If a Wi-Fi Direct socket connection is already active, dispatches all files immediately.
     * - Otherwise queues them so they are automatically sent the moment a peer connects, and starts discovery.
     */
    fun shareReceivedFilesViaWifiDirect(
        files: List<ReceivedFile>,
        onResult: (sentImmediately: Boolean, count: Int) -> Unit = { _, _ -> }
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
                        mimeType = rf.mimeType
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
        viewModelScope.launch {
            app.preferencesRepository.setDisplayName(name)
            service?.setLocalDisplayName(name)
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

    fun setBleFastDiscoveryEnabled(enabled: Boolean) {
        viewModelScope.launch {
            app.preferencesRepository.setBleFastDiscoveryEnabled(enabled)
        }
    }

    fun isBleFastDiscoverySupported(): Boolean = service?.isBleFastDiscoverySupported() ?: false

    fun retryBleDiscovery() {
        service?.retryBleDiscovery()
    }

    override fun onCleared() {
        super.onCleared()
        stopDiscovery()
    }
}
