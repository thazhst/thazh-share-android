package com.thazh.share.network

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.SecureRandom

/**
 * Explicit NFC role based on the active tab:
 * - [SENDER_READER]: Active when the user is on the **Send tab** (`ShareTab.SEND`).
 *   The device acts as an NFC PCD (Reader Mode via [NfcAdapter.enableReaderMode]) to read
 *   the Receiver's HCE card, send its own peer info, and initiate the Wi-Fi Direct connection.
 * - [RECEIVER_CARD_EMULATOR]: Active when the user is on the **Receive tab** (`ShareTab.RECEIVE`)
 *   or other tabs. Reader mode is disabled so Android routes the NFC field to [com.thazh.share.service.ShareNfcHceService]
 *   (Host Card Emulation), allowing the Sender to read this device and automatically pair.
 */
enum class NfcRole {
    SENDER_READER,
    RECEIVER_CARD_EMULATOR,
}

/**
 * Data payload exchanged over NFC ISO-DEP (APDU) between two devices:
 * - Sender (Send tab) operates in Reader Mode (PCD).
 * - Receiver (Receive tab) operates in Host Card Emulation Mode (HCE).
 */
data class NfcPeerPayload(
    val displayName: String,
    val wifiP2pName: String,
    val deviceAddress: String,
    val nonce: Long,
    val role: String = "ANY",
) {
    fun toJsonBytes(): ByteArray {
        val obj = JSONObject()
            .put("v", 2)
            .put("name", displayName.take(48))
            .put("p2p", wifiP2pName.take(48))
            .put("mac", deviceAddress.take(32))
            .put("n", nonce)
            .put("role", role)
        return obj.toString().toByteArray(StandardCharsets.UTF_8)
    }

    companion object {
        fun fromJsonBytes(bytes: ByteArray): NfcPeerPayload? {
            if (bytes.isEmpty()) return null
            return try {
                val obj = JSONObject(String(bytes, StandardCharsets.UTF_8))
                val name = obj.optString("name", "").trim()
                val p2p = obj.optString("p2p", name).trim().ifEmpty { name }
                val mac = obj.optString("mac", "").trim()
                val nonce = obj.optLong("n", 0L)
                val role = obj.optString("role", "ANY").trim()
                if (name.isEmpty() && p2p.isEmpty() && mac.isEmpty()) return null
                NfcPeerPayload(
                    displayName = name.ifEmpty { p2p },
                    wifiP2pName = p2p.ifEmpty { name },
                    deviceAddress = mac,
                    nonce = nonce,
                    role = role,
                )
            } catch (e: Exception) {
                Log.w("NfcPeerPayload", "Invalid NFC JSON payload: ${e.message}")
                null
            }
        }
    }
}

/**
 * Event emitted when an NFC tap completes a peer payload exchange.
 * [initiateConnect] is `true` for the Sender device (on the Send tab in Reader Mode)
 * and `false` for the Receiver device (on the Receive tab in HCE Card Emulation Mode).
 */
data class NfcHandshakeEvent(
    val peer: NfcPeerPayload,
    val initiateConnect: Boolean,
)

/**
 * Process-wide singleton coordinator bridging [NfcConnectionManager] (Reader mode on Send tab),
 * [com.thazh.share.service.ShareNfcHceService] (HCE Card Emulation on Receive tab), and
 * [com.thazh.share.service.WifiDirectService].
 */
object NfcConnectionCoordinator {
    /** Custom proprietary ISO-7816-4 Application ID (AID): F0 54 48 41 5A 48 53 48 ("THAZHSH") */
    val AID_BYTES: ByteArray = byteArrayOf(
        0xF0.toByte(),
        0x54,
        0x48,
        0x41,
        0x5A,
        0x48,
        0x53,
        0x48,
    )

    val STATUS_OK: ByteArray = byteArrayOf(0x90.toByte(), 0x00.toByte())
    val STATUS_FAILED: ByteArray = byteArrayOf(0x6F.toByte(), 0x00.toByte())
    const val INS_EXCHANGE_PEER: Byte = 0xCA.toByte()

    private val localNonce: Long = kotlin.math.abs(SecureRandom().nextLong()).coerceAtLeast(1L)

    @Volatile
    private var localDisplayName: String = android.os.Build.MODEL ?: "Android"

    @Volatile
    private var localWifiP2pName: String = localDisplayName

    @Volatile
    private var localDeviceAddress: String = ""

    @Volatile
    private var lastHandshakePeerKey: String = ""

    @Volatile
    private var lastHandshakeTimestampMs: Long = 0L

    private val _currentRole = MutableStateFlow(NfcRole.SENDER_READER)
    val currentRole: StateFlow<NfcRole> = _currentRole.asStateFlow()

    private val _handshakeEvents = MutableSharedFlow<NfcHandshakeEvent>(extraBufferCapacity = 4)
    val handshakeEvents: SharedFlow<NfcHandshakeEvent> = _handshakeEvents.asSharedFlow()
    val exchanges: SharedFlow<NfcHandshakeEvent>
        get() = handshakeEvents

    fun isValidMacAddress(mac: String?): Boolean {
        if (mac.isNullOrBlank()) return false
        val normalized = mac.trim().uppercase()
        if (normalized == "02:00:00:00:00:00" || normalized == "00:00:00:00:00:00") return false
        return normalized.matches(Regex("^([0-9A-F]{2}:){5}[0-9A-F]{2}$"))
    }

    fun setRole(role: NfcRole) {
        _currentRole.value = role
    }

    fun updateLocalIdentity(
        displayName: String? = null,
        wifiP2pName: String? = null,
        deviceAddress: String? = null,
    ) {
        if (!displayName.isNullOrBlank()) {
            localDisplayName = displayName.trim()
        }
        if (!wifiP2pName.isNullOrBlank()) {
            localWifiP2pName = wifiP2pName.trim()
        }
        if (!deviceAddress.isNullOrBlank() && deviceAddress != "02:00:00:00:00:00") {
            localDeviceAddress = deviceAddress.trim()
        }
    }

    fun buildLocalPayload(role: NfcRole = _currentRole.value): NfcPeerPayload {
        val display = localDisplayName.ifBlank { android.os.Build.MODEL ?: "Android" }
        val p2p = localWifiP2pName.ifBlank { display }
        return NfcPeerPayload(
            displayName = display,
            wifiP2pName = p2p,
            deviceAddress = localDeviceAddress,
            nonce = localNonce,
            role = if (role == NfcRole.SENDER_READER) "SEND" else "RECEIVE",
        )
    }

    fun onPeerExchanged(peer: NfcPeerPayload, initiateConnect: Boolean) {
        val now = System.currentTimeMillis()
        val key = "${peer.deviceAddress}|${peer.wifiP2pName}|${peer.displayName}|$initiateConnect"
        if (key == lastHandshakePeerKey && now - lastHandshakeTimestampMs < 2_200L) {
            return
        }
        lastHandshakePeerKey = key
        lastHandshakeTimestampMs = now
        _handshakeEvents.tryEmit(NfcHandshakeEvent(peer = peer, initiateConnect = initiateConnect))
    }

    fun buildSelectAidApdu(): ByteArray {
        val header = byteArrayOf(
            0x00, // CLA
            0xA4.toByte(), // INS: SELECT
            0x04, // P1: Select by AID
            0x00, // P2
            AID_BYTES.size.toByte(), // Lc
        )
        return header + AID_BYTES + byteArrayOf(0x00) // Le
    }

    fun buildExchangeApdu(localPayload: NfcPeerPayload): ByteArray {
        val data = localPayload.toJsonBytes()
        val lc = data.size.coerceAtMost(240)
        val header = byteArrayOf(
            0x80.toByte(), // CLA
            INS_EXCHANGE_PEER, // INS
            0x00, // P1
            0x00, // P2
            lc.toByte(), // Lc
        )
        return header + data.copyOfRange(0, lc) + byteArrayOf(0x00) // Le
    }

    fun isStatusOk(apduResponse: ByteArray?): Boolean {
        if (apduResponse == null || apduResponse.size < 2) return false
        val sw1 = apduResponse[apduResponse.size - 2]
        val sw2 = apduResponse[apduResponse.size - 1]
        return sw1 == STATUS_OK[0] && sw2 == STATUS_OK[1]
    }

    fun extractPayloadFromResponse(apduResponse: ByteArray): ByteArray {
        if (apduResponse.size <= 2) return ByteArray(0)
        return apduResponse.copyOfRange(0, apduResponse.size - 2)
    }
}

/**
 * Manages NFC hardware status and dynamic switching between:
 * - **Sender Reader Mode (PCD)** when the user is on the **Send tab**: actively polls for the
 *   other phone's HCE service and initiates the Wi-Fi Direct connection.
 * - **Receiver HCE Card Mode** when the user is on the **Receive tab**: disables Reader Mode so
 *   the NFC controller routes incoming RF fields to [com.thazh.share.service.ShareNfcHceService].
 */
class NfcConnectionManager(context: Context) {
    private val appContext = context.applicationContext
    private val nfcAdapter: NfcAdapter? = try {
        NfcAdapter.getDefaultAdapter(appContext)
    } catch (_: Exception) {
        null
    }

    private val _isNfcEnabled = MutableStateFlow(nfcAdapter?.isEnabled == true)
    val isNfcEnabled: StateFlow<Boolean> = _isNfcEnabled.asStateFlow()

    @Volatile
    private var readerModeActive = false

    fun isSupported(): Boolean = nfcAdapter != null

    fun isEnabled(): Boolean {
        val enabled = nfcAdapter?.isEnabled == true
        _isNfcEnabled.value = enabled
        return enabled
    }

    fun refreshState() {
        _isNfcEnabled.value = nfcAdapter?.isEnabled == true
    }

    /**
     * Synchronizes the NFC controller mode with the currently active role:
     * - [NfcRole.SENDER_READER] (Send tab): enables Reader Mode (PCD) to read the Receive device's HCE card.
     * - [NfcRole.RECEIVER_CARD_EMULATOR] (Receive tab): disables Reader Mode so this device acts as an HCE Card.
     */
    fun applyRoleForActivity(activity: Activity, role: NfcRole) {
        NfcConnectionCoordinator.setRole(role)
        refreshState()
        when (role) {
            NfcRole.SENDER_READER -> enableForegroundReader(activity)
            NfcRole.RECEIVER_CARD_EMULATOR -> disableForegroundReader(activity)
        }
    }

    fun enableForegroundReader(activity: Activity) {
        val adapter = nfcAdapter ?: return
        refreshState()
        if (!adapter.isEnabled) return
        try {
            val extras = Bundle().apply {
                // Balanced presence check delay for Pixel, Oppo, and Xiaomi/Redmi NFC controllers
                putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 300)
            }
            adapter.enableReaderMode(
                activity,
                { tag -> handleDiscoveredTag(tag) },
                NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                extras,
            )
            readerModeActive = true
            Log.d(TAG, "NFC Reader Mode ENABLED (Send tab / PCD role)")
        } catch (e: Exception) {
            Log.w(TAG, "enableReaderMode failed: ${e.message}")
        }
    }

    fun disableForegroundReader(activity: Activity) {
        val adapter = nfcAdapter ?: return
        try {
            if (readerModeActive) {
                adapter.disableReaderMode(activity)
                readerModeActive = false
                Log.d(TAG, "NFC Reader Mode DISABLED -> HCE Card Emulation active (Receive tab)")
            } else {
                adapter.disableReaderMode(activity)
            }
        } catch (e: Exception) {
            Log.w(TAG, "disableReaderMode failed: ${e.message}")
        }
    }

    fun openNfcSettings() {
        val intent = Intent(Settings.ACTION_NFC_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            appContext.startActivity(intent)
        } catch (_: Exception) {
            val fallback = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { appContext.startActivity(fallback) }
        }
    }

    private fun handleDiscoveredTag(tag: Tag) {
        val isoDep = IsoDep.get(tag) ?: return
        try {
            isoDep.connect()
            isoDep.timeout = 2_500

            // Step 1: SELECT Thazh Share HCE AID on the Receiver device
            val selectResponse = isoDep.transceive(NfcConnectionCoordinator.buildSelectAidApdu())
            if (!NfcConnectionCoordinator.isStatusOk(selectResponse)) {
                Log.w(TAG, "SELECT AID rejected by peer HCE")
                return
            }

            // Step 2: Send our Sender payload and receive the Receiver's payload in one atomic APDU
            val localPayload = NfcConnectionCoordinator.buildLocalPayload(NfcRole.SENDER_READER)
            val exchangeResponse = isoDep.transceive(
                NfcConnectionCoordinator.buildExchangeApdu(localPayload),
            )
            if (!NfcConnectionCoordinator.isStatusOk(exchangeResponse)) {
                Log.w(TAG, "EXCHANGE_PEER failed on peer HCE")
                return
            }

            val peerBytes = NfcConnectionCoordinator.extractPayloadFromResponse(exchangeResponse)
            val peerPayload = NfcPeerPayload.fromJsonBytes(peerBytes)
                ?: NfcPeerPayload.fromJsonBytes(
                    NfcConnectionCoordinator.extractPayloadFromResponse(selectResponse),
                )
                ?: return

            // Because we are on the Send tab (Reader/PCD), we initiate the Wi-Fi Direct connection!
            Log.d(
                TAG,
                "NFC tap succeeded (Sender -> Receiver)! Connecting to ${peerPayload.displayName} (${peerPayload.wifiP2pName} / ${peerPayload.deviceAddress})",
            )
            NfcConnectionCoordinator.onPeerExchanged(
                peer = peerPayload,
                initiateConnect = true,
            )
        } catch (e: Exception) {
            Log.w(TAG, "ISO-DEP NFC handshake error: ${e.message}")
        } finally {
            runCatching { isoDep.close() }
        }
    }

    companion object {
        private const val TAG = "NfcConnectionManager"
    }
}
