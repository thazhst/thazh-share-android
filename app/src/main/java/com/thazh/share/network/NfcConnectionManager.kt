package com.thazh.share.network

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.nio.charset.StandardCharsets
import java.util.UUID

data class NfcPeerPayload(
    val displayName: String,
    val wifiP2pName: String,
    val deviceAddress: String,
    val nonce: String
)

data class NfcPeerExchange(
    val peer: NfcPeerPayload,
    val initiateConnect: Boolean
)

/**
 * Coordinates NFC Tap-to-Connect payloads and bidirectional APDU exchanges
 * between [com.thazh.share.service.ShareNfcHceService] (PICC / card emulation)
 * and foreground [NfcAdapter.enableReaderMode] (PCD / reader).
 *
 * Android Beam was removed in API 34, so HCE + IsoDep ReaderMode provides modern,
 * bi-directional Wi-Fi Direct credential exchange on a single tap across Android 8+.
 */
object NfcConnectionCoordinator {
    private const val TAG = "NfcCoordinator"
    private const val EXCHANGE_DEBOUNCE_MS = 2_500L
    private const val MAGIC = "THAZH1"

    // Custom proprietary AID "THAZHS" (F0 54 48 41 5A 48 53)
    val AID_BYTES: ByteArray = byteArrayOf(
        0xF0.toByte(), 0x54, 0x48, 0x41, 0x5A, 0x48, 0x53
    )

    val SELECT_APDU: ByteArray = byteArrayOf(
        0x00, 0xA4.toByte(), 0x04, 0x00,
        AID_BYTES.size.toByte(),
        *AID_BYTES,
        0x00
    )

    const val CLA_THAZH: Byte = 0x80.toByte()
    const val INS_EXCHANGE: Byte = 0x10

    val SW_OK: ByteArray = byteArrayOf(0x90.toByte(), 0x00)
    val SW_UNKNOWN: ByteArray = byteArrayOf(0x6F, 0x00)
    val SW_INS_NOT_SUPPORTED: ByteArray = byteArrayOf(0x6D, 0x00)

    private val sessionNonce: String = UUID.randomUUID().toString().take(8)

    @Volatile
    private var localDisplayName: String = Build.MODEL?.takeIf { it.isNotBlank() } ?: "Thazh Share"

    @Volatile
    private var localWifiP2pName: String = ""

    @Volatile
    private var localDeviceAddress: String = ""

    @Volatile
    private var lastExchangeNonce: String? = null

    @Volatile
    private var lastExchangeElapsedMs: Long = 0L

    private val _exchanges = MutableSharedFlow<NfcPeerExchange>(extraBufferCapacity = 4)
    val exchanges: SharedFlow<NfcPeerExchange> = _exchanges.asSharedFlow()

    fun updateLocalIdentity(
        displayName: String? = null,
        wifiP2pName: String? = null,
        deviceAddress: String? = null
    ) {
        if (!displayName.isNullOrBlank()) {
            localDisplayName = sanitizeField(displayName, maxLen = 48)
        }
        if (!wifiP2pName.isNullOrBlank()) {
            localWifiP2pName = sanitizeField(wifiP2pName, maxLen = 48)
        }
        if (!deviceAddress.isNullOrBlank()) {
            localDeviceAddress = sanitizeField(deviceAddress, maxLen = 32)
        }
    }

    fun currentPayloadBytes(): ByteArray {
        val raw = listOf(
            MAGIC,
            sanitizeField(localDeviceAddress, 32),
            sanitizeField(localWifiP2pName, 48),
            sessionNonce,
            sanitizeField(localDisplayName, 48)
        ).joinToString("|")
        return raw.toByteArray(StandardCharsets.UTF_8)
    }

    fun buildExchangeCommandApdu(): ByteArray {
        val payload = currentPayloadBytes()
        val lc = payload.size.coerceAtMost(240)
        return ByteArray(5 + lc + 1).apply {
            this[0] = CLA_THAZH
            this[1] = INS_EXCHANGE
            this[2] = 0x00
            this[3] = 0x00
            this[4] = lc.toByte()
            System.arraycopy(payload, 0, this, 5, lc)
            this[5 + lc] = 0x00
        }
    }

    fun parsePayload(bytes: ByteArray): NfcPeerPayload? {
        if (bytes.isEmpty()) return null
        val text = runCatching { String(bytes, StandardCharsets.UTF_8) }.getOrNull() ?: return null
        val parts = text.split('|', limit = 5)
        if (parts.size < 5 || parts[0] != MAGIC) return null
        val address = parts[1].trim()
        val wifiName = parts[2].trim()
        val nonce = parts[3].trim()
        val display = parts[4].trim().ifBlank { wifiName.ifBlank { "Thazh Share" } }
        if (nonce == sessionNonce) return null
        return NfcPeerPayload(
            displayName = display,
            wifiP2pName = wifiName,
            deviceAddress = address,
            nonce = nonce
        )
    }

    fun onPeerExchanged(payload: NfcPeerPayload, initiateConnect: Boolean) {
        val now = SystemClock.elapsedRealtime()
        if (payload.nonce.isNotBlank() &&
            payload.nonce == lastExchangeNonce &&
            now - lastExchangeElapsedMs < EXCHANGE_DEBOUNCE_MS
        ) {
            return
        }
        lastExchangeNonce = payload.nonce
        lastExchangeElapsedMs = now
        Log.i(
            TAG,
            "NFC exchanged peer='${payload.displayName}' p2p='${payload.wifiP2pName}' mac='${payload.deviceAddress}' initiate=$initiateConnect"
        )
        _exchanges.tryEmit(NfcPeerExchange(peer = payload, initiateConnect = initiateConnect))
    }

    fun isValidMacAddress(mac: String?): Boolean {
        if (mac.isNullOrBlank()) return false
        val normalized = mac.trim().uppercase()
        if (normalized == "02:00:00:00:00:00" || normalized == "00:00:00:00:00:00") return false
        return MAC_REGEX.matches(normalized)
    }

    private fun sanitizeField(value: String, maxLen: Int): String {
        return value.replace('|', ' ').replace('\n', ' ').trim().take(maxLen)
    }

    private val MAC_REGEX = Regex("^([0-9A-F]{2}:){5}[0-9A-F]{2}$")
}

class NfcConnectionManager(private val context: Context) {

    private val nfcAdapter: NfcAdapter?
        get() = runCatching { NfcAdapter.getDefaultAdapter(context) }.getOrNull()

    fun isSupported(): Boolean {
        return runCatching {
            val pm = context.packageManager
            val hasNfc = pm.hasSystemFeature(PackageManager.FEATURE_NFC)
            hasNfc && nfcAdapter != null
        }.getOrDefault(false)
    }

    fun isEnabled(): Boolean {
        return runCatching { nfcAdapter?.isEnabled == true }.getOrDefault(false)
    }

    fun enableForegroundReader(activity: Activity) {
        val adapter = nfcAdapter ?: return
        if (!runCatching { adapter.isEnabled }.getOrDefault(false)) return
        runCatching {
            adapter.enableReaderMode(
                activity,
                readerCallback,
                NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                null
            )
        }.onFailure {
            Log.w(TAG, "enableReaderMode skipped: ${it.message}")
        }
    }

    fun disableForegroundReader(activity: Activity) {
        val adapter = nfcAdapter ?: return
        runCatching {
            adapter.disableReaderMode(activity)
        }.onFailure {
            Log.w(TAG, "disableReaderMode skipped: ${it.message}")
        }
    }

    fun openNfcSettings() {
        runCatching {
            val intent = Intent(Settings.ACTION_NFC_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    private val readerCallback = object : NfcAdapter.ReaderCallback {
        override fun onTagDiscovered(tag: Tag?) {
            if (tag != null) {
                handleDiscoveredTag(tag)
            }
        }
    }

    private fun handleDiscoveredTag(tag: Tag) {
        val isoDep = IsoDep.get(tag) ?: return
        try {
            isoDep.timeout = 2_500
            isoDep.connect()

            val selectResp = isoDep.transceive(NfcConnectionCoordinator.SELECT_APDU)
            if (isSuccessSw(selectResp)) {
                val exchangeResp = isoDep.transceive(NfcConnectionCoordinator.buildExchangeCommandApdu())
                if (isSuccessSw(exchangeResp) && exchangeResp.size > 2) {
                    val body = exchangeResp.copyOf(exchangeResp.size - 2)
                    val peer = NfcConnectionCoordinator.parsePayload(body)
                    if (peer != null) {
                        NfcConnectionCoordinator.onPeerExchanged(peer, initiateConnect = true)
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "NFC reader exchange ended: ${e.message}")
        } finally {
            runCatching { isoDep.close() }
        }
    }

    private fun isSuccessSw(response: ByteArray): Boolean {
        if (response.size < 2) return false
        return response[response.size - 2] == 0x90.toByte() &&
            response[response.size - 1] == 0x00.toByte()
    }

    companion object {
        private const val TAG = "NfcConnectionManager"
    }
}
