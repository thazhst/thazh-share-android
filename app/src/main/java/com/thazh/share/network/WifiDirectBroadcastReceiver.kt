package com.thazh.share.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.util.Log

/**
 * Listens for the system WiFi P2P broadcasts and forwards them to
 * [WifiDirectManager] as typed callbacks. Registered/unregistered in
 * lockstep with the hosting Activity/Service lifecycle.
 */
class WifiDirectBroadcastReceiver(
    private val manager: WifiP2pManager,
    private val channel: WifiP2pManager.Channel,
    private val listener: Listener
) : BroadcastReceiver() {

    interface Listener {
        fun onWifiP2pStateChanged(isEnabled: Boolean)
        fun onPeersChanged()
        fun onConnectionChanged(networkInfo: android.net.NetworkInfo?, connectionInfo: android.net.wifi.p2p.WifiP2pInfo?)
        fun onThisDeviceChanged(device: WifiP2pDevice)
        fun onDiscoveryStateChanged(isDiscovering: Boolean)
    }

    override fun onReceive(context: Context, intent: Intent) {
        runCatching {
            when (intent.action) {
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                    val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                    val enabled = state == WifiP2pManager.WIFI_P2P_STATE_ENABLED
                    Log.d(TAG, "WIFI_P2P_STATE_CHANGED enabled=$enabled")
                    listener.onWifiP2pStateChanged(enabled)
                }

                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    Log.d(TAG, "WIFI_P2P_PEERS_CHANGED")
                    listener.onPeersChanged()
                }

                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    val networkInfo: android.net.NetworkInfo? = if (Build.VERSION.SDK_INT >= 33) {
                        intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO, android.net.NetworkInfo::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO)
                    }
                    Log.d(TAG, "WIFI_P2P_CONNECTION_CHANGED connected=${networkInfo?.isConnected}")

                    manager.requestConnectionInfo(channel) { info ->
                        listener.onConnectionChanged(networkInfo, info)
                    }
                }

                WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                    val device: WifiP2pDevice? = if (Build.VERSION.SDK_INT >= 33) {
                        intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE, WifiP2pDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)
                    }
                    device?.let { listener.onThisDeviceChanged(it) }
                }

                WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION -> {
                    val state = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, -1)
                    listener.onDiscoveryStateChanged(state == WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED)
                }
            }
        }.onFailure {
            Log.w(TAG, "BroadcastReceiver handling skipped: ${it.message}")
        }
    }

    companion object {
        private const val TAG = "WifiDirectBroadcast"

        fun intentFilter(): IntentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION)
        }
    }
}

private typealias IntentFilter = android.content.IntentFilter
