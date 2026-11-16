package com.thazh.share.data

import kotlinx.serialization.Serializable

/**
 * A WiFi Direct peer as currently seen by the OS discovery API.
 * MAC address ([deviceAddress]) is the stable key Android gives us for a
 * peer for the lifetime of a discovery session.
 */
data class DiscoveredPeer(
    val deviceName: String,
    val deviceAddress: String,
    val status: PeerStatus,
    val isGroupOwner: Boolean = false
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
 * A device the user has previously chatted with and chosen to remember.
 * Persisted to DataStore as JSON so the "saved devices" list survives app
 * restarts and lets the user reconnect / rename without a fresh scan.
 *
 * [deviceAddress] (MAC) is the unique key. WiFi Direct MACs are stable for
 * a physical device across sessions in practice, though Android may
 * randomize the *local* device's own advertised MAC per docs; this affects
 * only self-identification, not our ability to recognize a remembered peer
 * we've successfully connected to before, since we key off the address we
 * observed during that prior connection.
 */
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
