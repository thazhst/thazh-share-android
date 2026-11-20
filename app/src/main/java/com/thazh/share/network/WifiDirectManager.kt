package com.thazh.share.network

import android.annotation.SuppressLint
import android.content.Context
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.thazh.share.data.DiscoveredPeer
import com.thazh.share.data.PeerStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class P2pConnectionState {
    data object Idle : P2pConnectionState()
    data object Connecting : P2pConnectionState()
    data class Connected(val isGroupOwner: Boolean, val groupOwnerAddress: String?) : P2pConnectionState()
    data object Disconnected : P2pConnectionState()
    data class Failed(val reason: String) : P2pConnectionState()
}

class WifiDirectManager(
    private val context: Context
) : WifiDirectBroadcastReceiver.Listener {

    private val wifiP2pManager: WifiP2pManager? =
        runCatching { context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager }.getOrNull()

    private var channel: WifiP2pManager.Channel? = null
    private var receiver: WifiDirectBroadcastReceiver? = null
    private var isReceiverRegistered = false

    val isSupported: Boolean
        get() = wifiP2pManager != null

    private val _isWifiP2pEnabled = MutableStateFlow(false)
    val isWifiP2pEnabled: StateFlow<Boolean> = _isWifiP2pEnabled.asStateFlow()

    private val _peers = MutableStateFlow<List<DiscoveredPeer>>(emptyList())
    val peers: StateFlow<List<DiscoveredPeer>> = _peers.asStateFlow()

    private val _connectionState = MutableStateFlow<P2pConnectionState>(P2pConnectionState.Idle)
    val connectionState: StateFlow<P2pConnectionState> = _connectionState.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private val _thisDevice = MutableStateFlow<WifiP2pDevice?>(null)
    val thisDevice: StateFlow<WifiP2pDevice?> = _thisDevice.asStateFlow()

    fun initialize() {
        val manager = wifiP2pManager ?: run {
            Log.w(TAG, "WifiP2pManager system service unavailable on this device")
            return
        }
        if (channel != null) return
        channel = runCatching {
            manager.initialize(context, context.mainLooper) {
                Log.w(TAG, "WifiP2pManager channel disconnected by system; re-initializing on next call")
                channel = null
            }
        }.onFailure {
            Log.w(TAG, "WifiP2pManager.initialize failed: ${it.message}")
        }.getOrNull()
        registerReceiver()
    }

    private fun registerReceiver() {
        val manager = wifiP2pManager ?: return
        val ch = channel ?: return
        if (isReceiverRegistered) return
        val rec = WifiDirectBroadcastReceiver(manager, ch, this)
        runCatching {
            ContextCompat.registerReceiver(
                context,
                rec,
                WifiDirectBroadcastReceiver.intentFilter(),
                ContextCompat.RECEIVER_EXPORTED
            )
            receiver = rec
            isReceiverRegistered = true
        }.onFailure {
            Log.w(TAG, "registerReceiver failed: ${it.message}")
        }
    }

    fun teardown() {
        if (isReceiverRegistered) {
            runCatching { context.unregisterReceiver(receiver) }
            isReceiverRegistered = false
        }
        receiver = null
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery(onResult: (success: Boolean, error: String?) -> Unit) {
        val manager = wifiP2pManager ?: return onResult(false, "Thiết bị không hỗ trợ WiFi Direct")
        if (channel == null) initialize()
        val ch = channel ?: return onResult(false, "Thiết bị không hỗ trợ WiFi Direct")
        val profile = com.thazh.share.util.DeviceOptimizationManager.getProfile(context)
        try {
            manager.discoverPeers(ch, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Log.d(TAG, "discoverPeers: onSuccess")
                    onResult(true, null)
                }

                override fun onFailure(reasonCode: Int) {
                    val reason = reasonToString(reasonCode)
                    Log.w(TAG, "discoverPeers: onFailure $reason (tier=${profile.tier})")
                    if (reasonCode == WifiP2pManager.BUSY) {
                        android.os.Handler(context.mainLooper).postDelayed(
                            {
                                val retryCh = channel ?: run {
                                    initialize()
                                    channel
                                } ?: return@postDelayed
                                runCatching {
                                    manager.discoverPeers(retryCh, object : WifiP2pManager.ActionListener {
                                        override fun onSuccess() {
                                            Log.d(TAG, "discoverPeers BUSY retry: onSuccess")
                                        }

                                        override fun onFailure(retryReason: Int) {
                                            Log.w(TAG, "discoverPeers BUSY retry failed: ${reasonToString(retryReason)}")
                                        }
                                    })
                                }
                            },
                            profile.p2pDiscoveryRetryDelayMs,
                        )
                    }
                    onResult(false, reason)
                }
            })
        } catch (e: Exception) {
            Log.w(TAG, "discoverPeers threw: ${e.message}")
            onResult(false, e.message ?: "Không thể khởi chạy tìm kiếm WiFi Direct")
        }
    }

    fun stopDiscovery(onResult: ((Boolean) -> Unit)? = null) {
        val manager = wifiP2pManager ?: return
        val ch = channel ?: return
        runCatching {
            manager.stopPeerDiscovery(ch, object : WifiP2pManager.ActionListener {
                override fun onSuccess() { onResult?.invoke(true) }
                override fun onFailure(reasonCode: Int) { onResult?.invoke(false) }
            })
        }.onFailure {
            onResult?.invoke(false)
        }
    }

    @SuppressLint("MissingPermission")
    fun connectTo(deviceAddress: String, onResult: (success: Boolean, error: String?) -> Unit) {
        val manager = wifiP2pManager ?: return onResult(false, "Thiết bị không hỗ trợ WiFi Direct")
        if (channel == null) initialize()
        val ch = channel ?: return onResult(false, "Thiết bị không hỗ trợ WiFi Direct")
        val config = WifiP2pConfig().apply {
            this.deviceAddress = deviceAddress
            wps.setup = android.net.wifi.WpsInfo.PBC
        }
        _connectionState.value = P2pConnectionState.Connecting

        fun attemptConnect(allowBusyRetry: Boolean) {
            try {
                manager.connect(ch, config, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        Log.d(TAG, "connect: onSuccess (negotiation started)")
                        onResult(true, null)
                    }

                    override fun onFailure(reasonCode: Int) {
                        if (allowBusyRetry && reasonCode == WifiP2pManager.BUSY) {
                            Log.d(TAG, "connect busy; stopping peer discovery and retrying immediately")
                            stopDiscovery {
                                attemptConnect(allowBusyRetry = false)
                            }
                            return
                        }
                        val reason = reasonToString(reasonCode)
                        Log.w(TAG, "connect: onFailure $reason")
                        _connectionState.value = P2pConnectionState.Failed(reason)
                        onResult(false, reason)
                    }
                })
            } catch (e: Exception) {
                val msg = e.message ?: "Không thể kết nối WiFi Direct"
                _connectionState.value = P2pConnectionState.Failed(msg)
                onResult(false, msg)
            }
        }

        attemptConnect(allowBusyRetry = true)
    }

    fun disconnect(onResult: ((Boolean) -> Unit)? = null) {
        val manager = wifiP2pManager ?: return
        val ch = channel ?: return
        runCatching {
            manager.removeGroup(ch, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    _connectionState.value = P2pConnectionState.Disconnected
                    onResult?.invoke(true)
                }
                override fun onFailure(reasonCode: Int) {
                    onResult?.invoke(false)
                }
            })
        }.onFailure {
            onResult?.invoke(false)
        }
    }

    @SuppressLint("MissingPermission")
    fun setDeviceName(name: String, onResult: (Boolean) -> Unit) {
        val manager = wifiP2pManager ?: return onResult(false)
        val ch = channel ?: return onResult(false)
        try {
            val method = WifiP2pManager::class.java.getMethod(
                "setDeviceName",
                WifiP2pManager.Channel::class.java,
                String::class.java,
                WifiP2pManager.ActionListener::class.java
            )
            method.invoke(
                manager,
                ch,
                name,
                object : WifiP2pManager.ActionListener {
                    override fun onSuccess() = onResult(true)
                    override fun onFailure(reasonCode: Int) = onResult(false)
                }
            )
        } catch (e: Exception) {
            Log.i(TAG, "setDeviceName reflective call unavailable on this OS build: ${e.message}")
            onResult(false)
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestPeersInternal() {
        val manager = wifiP2pManager ?: return
        val ch = channel ?: return
        runCatching {
            manager.requestPeers(ch) { deviceList: WifiP2pDeviceList ->
                val mapped = deviceList.deviceList.map { d ->
                    DiscoveredPeer(
                        deviceName = d.deviceName ?: d.deviceAddress,
                        deviceAddress = d.deviceAddress,
                        status = PeerStatus.fromWifiP2pStatus(d.status),
                        isGroupOwner = d.isGroupOwner
                    )
                }
                if (_peers.value != mapped) {
                    _peers.value = mapped
                }
            }
        }.onFailure {
            Log.w(TAG, "requestPeers failed: ${it.message}")
        }
    }

    override fun onWifiP2pStateChanged(isEnabled: Boolean) {
        _isWifiP2pEnabled.value = isEnabled
    }

    override fun onPeersChanged() {
        requestPeersInternal()
    }

    override fun onConnectionChanged(networkInfo: NetworkInfo?, connectionInfo: WifiP2pInfo?) {
        when {
            networkInfo?.isConnected == true && connectionInfo != null && connectionInfo.groupFormed -> {
                _connectionState.value = P2pConnectionState.Connected(
                    isGroupOwner = connectionInfo.isGroupOwner,
                    groupOwnerAddress = connectionInfo.groupOwnerAddress?.hostAddress
                )
            }
            else -> {
                if (_connectionState.value is P2pConnectionState.Connected) {
                    _connectionState.value = P2pConnectionState.Disconnected
                }
            }
        }
    }

    override fun onThisDeviceChanged(device: WifiP2pDevice) {
        _thisDevice.value = device
    }

    override fun onDiscoveryStateChanged(isDiscovering: Boolean) {
        _isDiscovering.value = isDiscovering
    }

    companion object {
        private const val TAG = "WifiDirectManager"

        fun reasonToString(reasonCode: Int): String = when (reasonCode) {
            WifiP2pManager.ERROR -> "Lỗi nội bộ hệ thống WiFi Direct"
            WifiP2pManager.P2P_UNSUPPORTED -> "Thiết bị không hỗ trợ WiFi Direct"
            WifiP2pManager.BUSY -> "Hệ thống đang bận, vui lòng thử lại"
            WifiP2pManager.NO_SERVICE_REQUESTS -> "Không có yêu cầu dịch vụ nào"
            else -> "Lỗi không xác định ($reasonCode)"
        }
    }
}
