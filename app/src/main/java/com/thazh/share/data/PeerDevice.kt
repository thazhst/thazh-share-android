package com.thazh.share.data

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

enum class PeerDiscoveryTransport {
    WIFI_DIRECT,
    BLE,
    WIFI_AND_BLE,
    NFC
}

/**
 * A WiFi Direct / BLE peer as currently seen by discovery APIs.
 * MAC address ([deviceAddress]) is the stable key Android gives us for a
 * peer for the lifetime of a discovery session.
 */
@Immutable
data class DiscoveredPeer(
    val deviceName: String,
    val deviceAddress: String,
    val status: PeerStatus,
    val isGroupOwner: Boolean = false,
    val discoveryTransport: PeerDiscoveryTransport = PeerDiscoveryTransport.WIFI_DIRECT,
    val rssiDbm: Int? = null,
    val estimatedDistanceMeters: Int? = null,
    val batteryPercent: Int? = null,
    val isCharging: Boolean = false,
)

enum class PeerStatus {
    CONNECTED, INVITED, FAILED, AVAILABLE, UNAVAILABLE;

    companion object {
        /** Maps android.net.wifi.p2p.WifiP2pDevice status ints. */
        fun fromWifiP2pStatus(status: Int): PeerStatus = when (status) {
            0 -> CONNECTED
            1 -> INVITED
            2 -> FAILED
            3 -> AVAILABLE
            4 -> UNAVAILABLE
            else -> UNAVAILABLE
        }
    }
}

/**
 * A device the user has previously connected with and chosen to remember.
 * Persisted to DataStore as JSON so the "saved devices" list survives app
 * restarts and lets the user reconnect / rename without a fresh scan.
 */
@Immutable
@Serializable
data class SavedDevice(
    val deviceAddress: String,
    val displayName: String,
    val nickname: String? = null,
    val lastConnectedAtEpochMs: Long,
    val firstSavedAtEpochMs: Long = lastConnectedAtEpochMs
) {
    /** Nickname the user set locally overrides the peer's broadcast name. */
    val effectiveName: String get() = nickname?.takeIf { it.isNotBlank() } ?: displayName
}
