package com.thazh.share.network

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

// Fixed app-private UUID identifying a Thazh Share BLE fast-discovery beacon.
private val SERVICE_UUID: UUID = UUID.fromString("6ac2c2c0-2f1a-4a3f-9c7f-9e2b6a2d9b41")
private val SERVICE_PARCEL_UUID: ParcelUuid = ParcelUuid(SERVICE_UUID)

private const val FLOW_EMIT_THROTTLE_MS = 260L
private const val BEACON_STALE_TIMEOUT_MS = 14_000L

data class BleDiscoveredBeacon(
    val deviceAddress: String,
    val displayName: String?,
    val rssiDbm: Int,
    val estimatedDistanceMeters: Int,
    val lastSeenElapsedMs: Long,
)

/**
 * High-speed Bluetooth Low Energy (BLE) discovery manager with C++20 1D Kalman RSSI
 * stabilization, stale beacon pruning, and throttled StateFlow emission to eliminate
 * downstream UI recomposition storms during low-latency scanning.
 */
class BleDiscoveryManager(private val context: Context) {

    private val liveBeacons = ConcurrentHashMap<String, BleDiscoveredBeacon>()

    private val _discoveredBeacons = MutableStateFlow<Map<String, BleDiscoveredBeacon>>(emptyMap())
    val discoveredBeacons: StateFlow<Map<String, BleDiscoveredBeacon>> = _discoveredBeacons.asStateFlow()

    private val _discoveredAddresses = MutableStateFlow<Set<String>>(emptySet())
    val discoveredAddresses: StateFlow<Set<String>> = _discoveredAddresses.asStateFlow()

    private val _isAdvertisingState = MutableStateFlow(false)
    val isAdvertisingState: StateFlow<Boolean> = _isAdvertisingState.asStateFlow()

    private val _isScanningState = MutableStateFlow(false)
    val isScanningState: StateFlow<Boolean> = _isScanningState.asStateFlow()

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    }

    private val fallbackSessionMacBytes: ByteArray by lazy {
        ByteArray(6).also { bytes ->
            SecureRandom().nextBytes(bytes)
            // Locally administered unicast MAC bit pattern
            bytes[0] = ((bytes[0].toInt() and 0xFE) or 0x02).toByte()
        }
    }

    private var advertiser: BluetoothLeAdvertiser? = null
    private var scanner: BluetoothLeScanner? = null
    private var isAdvertising = false
    private var isScanning = false
    private var lastAdvertisedMac: String? = null
    private var lastAdvertisedName: String? = null
    @Volatile
    private var lastFlowEmitElapsedMs: Long = 0L

    fun isSupported(): Boolean {
        return runCatching {
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) && bluetoothAdapter != null
        }.getOrDefault(false)
    }

    fun isBluetoothEnabled(): Boolean {
        return runCatching { bluetoothAdapter?.isEnabled == true }.getOrDefault(false)
    }

    private fun hasPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    fun start(localMacAddress: String?, localDisplayName: String? = null) {
        runCatching {
            if (!isSupported() || !hasPermissions()) return
            val adapter = bluetoothAdapter ?: return
            if (!adapter.isEnabled) return
            val safeName = localDisplayName?.trim()?.takeIf { it.isNotEmpty() }
            if (isAdvertising && (localMacAddress != lastAdvertisedMac || safeName != lastAdvertisedName)) {
                stopAdvertising()
            }
            startAdvertising(adapter, localMacAddress, safeName)
            startScanning(adapter)
        }.onFailure {
            Log.w(TAG, "BLE start skipped: ${it.message}")
        }
    }

    fun stop() {
        runCatching {
            stopAdvertising()
            stopScanning()
            liveBeacons.clear()
            NativeCoreEngine.clearKalmanHistory()
            _discoveredBeacons.value = emptyMap()
            _discoveredAddresses.value = emptySet()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startAdvertising(
        adapter: BluetoothAdapter,
        localMacAddress: String?,
        localDisplayName: String?,
    ) {
        if (isAdvertising) return
        val macBytes = macStringToBytes(localMacAddress) ?: fallbackSessionMacBytes
        val nameBytes = (localDisplayName ?: Build.MODEL ?: "Thazh")
            .toByteArray(StandardCharsets.UTF_8)
            .let { if (it.size > 11) it.copyOf(11) else it }
        val payload = ByteArray(macBytes.size + nameBytes.size)
        System.arraycopy(macBytes, 0, payload, 0, macBytes.size)
        System.arraycopy(nameBytes, 0, payload, macBytes.size, nameBytes.size)

        val adv = adapter.bluetoothLeAdvertiser ?: return

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceUuid(SERVICE_PARCEL_UUID)
            .addServiceData(SERVICE_PARCEL_UUID, payload)
            .build()

        try {
            adv.startAdvertising(settings, data, advertiseCallback)
            advertiser = adv
            isAdvertising = true
            lastAdvertisedMac = localMacAddress
            lastAdvertisedName = localDisplayName
            _isAdvertisingState.value = true
        } catch (e: Exception) {
            Log.w(TAG, "startAdvertising failed: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopAdvertising() {
        if (!isAdvertising) return
        runCatching { advertiser?.stopAdvertising(advertiseCallback) }
        isAdvertising = false
        _isAdvertisingState.value = false
        advertiser = null
    }

    @SuppressLint("MissingPermission")
    private fun startScanning(adapter: BluetoothAdapter) {
        if (isScanning) return
        val sc = adapter.bluetoothLeScanner ?: return

        val filter = ScanFilter.Builder()
            .setServiceUuid(SERVICE_PARCEL_UUID)
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0L)
            .build()

        try {
            sc.startScan(listOf(filter), settings, scanCallback)
            scanner = sc
            isScanning = true
            _isScanningState.value = true
        } catch (e: Exception) {
            Log.w(TAG, "startScan failed: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopScanning() {
        if (!isScanning) return
        runCatching { scanner?.stopScan(scanCallback) }
        isScanning = false
        _isScanningState.value = false
        scanner = null
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int) {
            Log.w(TAG, "BLE advertise start failed: $errorCode")
            isAdvertising = false
            _isAdvertisingState.value = false
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val data = result.scanRecord?.getServiceData(SERVICE_PARCEL_UUID) ?: return
            if (data.size < 6) return
            if (isMatchingFallbackMac(data)) return
            val mac = macBytesToString(data, offset = 0) ?: return
            if (mac.equals(lastAdvertisedMac, ignoreCase = true)) return

            val parsedName = if (data.size > 6) {
                runCatching {
                    String(data, 6, data.size - 6, StandardCharsets.UTF_8).trim()
                }.getOrNull()?.takeIf { it.isNotBlank() }
            } else {
                null
            }

            val (filteredRssi, filteredMeters) = DistanceEstimator.estimateFiltered(
                deviceKey = "BLE_$mac",
                rawRssiDbm = result.rssi,
            )
            val rssi = filteredRssi ?: result.rssi
            val distance = filteredMeters ?: 2
            val now = SystemClock.elapsedRealtime()

            val previous = liveBeacons[mac]
            val resolvedName = parsedName ?: previous?.displayName
            val beacon = BleDiscoveredBeacon(
                deviceAddress = mac,
                displayName = resolvedName,
                rssiDbm = rssi,
                estimatedDistanceMeters = distance,
                lastSeenElapsedMs = now,
            )
            liveBeacons[mac] = beacon

            // Prune stale beacons that have disappeared
            var prunedAny = false
            val iterator = liveBeacons.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (now - entry.value.lastSeenElapsedMs > BEACON_STALE_TIMEOUT_MS) {
                    NativeCoreEngine.clearKalmanHistory("BLE_${entry.key}")
                    iterator.remove()
                    prunedAny = true
                }
            }

            val isImmediateStructuralChange = previous == null ||
                previous.displayName != resolvedName ||
                previous.estimatedDistanceMeters != distance ||
                prunedAny

            if (isImmediateStructuralChange || (now - lastFlowEmitElapsedMs) >= FLOW_EMIT_THROTTLE_MS) {
                lastFlowEmitElapsedMs = now
                val snapshot = HashMap(liveBeacons)
                _discoveredBeacons.value = snapshot
                _discoveredAddresses.value = snapshot.keys.toSet()
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.w(TAG, "BLE scan failed: $errorCode")
            isScanning = false
            _isScanningState.value = false
        }
    }

    private fun isMatchingFallbackMac(data: ByteArray): Boolean {
        if (data.size < 6) return false
        val fb = fallbackSessionMacBytes
        for (i in 0 until 6) {
            if (data[i] != fb[i]) return false
        }
        return true
    }

    private fun macStringToBytes(mac: String?): ByteArray? {
        if (mac.isNullOrBlank()) return null
        val normalized = mac.trim().uppercase()
        if (normalized == "02:00:00:00:00:00" || normalized == "00:00:00:00:00:00") return null
        val parts = normalized.split(":")
        if (parts.size != 6) return null
        return try {
            ByteArray(6) { i -> parts[i].toInt(16).toByte() }
        } catch (e: NumberFormatException) {
            null
        }
    }

    private fun macBytesToString(bytes: ByteArray, offset: Int = 0): String? {
        if (bytes.size - offset < 6) return null
        val chars = CharArray(17)
        for (i in 0 until 6) {
            val v = bytes[offset + i].toInt() and 0xFF
            val pos = i * 3
            chars[pos] = HEX_CHARS[v ushr 4]
            chars[pos + 1] = HEX_CHARS[v and 0x0F]
            if (i < 5) chars[pos + 2] = ':'
        }
        return String(chars)
    }

    companion object {
        private const val TAG = "BleDiscoveryManager"
        private val HEX_CHARS = "0123456789ABCDEF".toCharArray()
    }
}
