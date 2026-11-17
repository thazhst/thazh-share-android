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

// Fixed app-private UUID identifying a Thazh Share BLE fast-discovery beacon.
private val SERVICE_UUID: UUID = UUID.fromString("6ac2c2c0-2f1a-4a3f-9c7f-9e2b6a2d9b41")

data class BleDiscoveredBeacon(
    val deviceAddress: String,
    val displayName: String?,
    val rssiDbm: Int,
    val estimatedDistanceMeters: Int,
    val lastSeenElapsedMs: Long,
)

/**
 * Real Bluetooth Low Energy (BLE) fast discovery manager:
 * - Advertises a compact service data payload containing the 6-byte Wi-Fi Direct MAC
 *   (or a session-stable synthetic 6-byte beacon ID when Android anonymizes local MACs)
 *   plus a UTF-8 snippet of the device's display name (up to 12 bytes) so nearby
 *   Thazh Share devices see both the peer's name and real-time RSSI/distance on the Radar!
 * - Scans in low-latency mode for nearby Thazh Share BLE beacons and exposes both
 *   [discoveredBeacons] (with RSSI dBm & estimated distance) and [discoveredAddresses].
 */
class BleDiscoveryManager(private val context: Context) {

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
            .addServiceUuid(ParcelUuid(SERVICE_UUID))
            .addServiceData(ParcelUuid(SERVICE_UUID), payload)
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
            .setServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
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
            val data = result.scanRecord?.getServiceData(ParcelUuid(SERVICE_UUID)) ?: return
            if (data.size < 6) return
            val macBytes = data.copyOfRange(0, 6)
            if (macBytes.contentEquals(fallbackSessionMacBytes)) return
            val mac = macBytesToString(macBytes) ?: return
            if (mac.equals(lastAdvertisedMac, ignoreCase = true)) return

            val parsedName = if (data.size > 6) {
                runCatching {
                    String(data, 6, data.size - 6, StandardCharsets.UTF_8).trim()
                }.getOrNull()?.takeIf { it.isNotBlank() }
            } else {
                null
            }

            val rssi = result.rssi
            val distance = DistanceEstimator.estimateMeters(rssi) ?: 2
            val now = SystemClock.elapsedRealtime()

            val beacon = BleDiscoveredBeacon(
                deviceAddress = mac,
                displayName = parsedName,
                rssiDbm = rssi,
                estimatedDistanceMeters = distance,
                lastSeenElapsedMs = now,
            )

            val updatedMap = _discoveredBeacons.value.toMutableMap()
            updatedMap[mac] = beacon
            _discoveredBeacons.value = updatedMap
            _discoveredAddresses.value = updatedMap.keys
        }

        override fun onScanFailed(errorCode: Int) {
            Log.w(TAG, "BLE scan failed: $errorCode")
            isScanning = false
            _isScanningState.value = false
        }
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

    private fun macBytesToString(bytes: ByteArray): String? {
        if (bytes.size != 6) return null
        return bytes.joinToString(":") { b -> String.format("%02X", b) }
    }

    companion object {
        private const val TAG = "BleDiscoveryManager"
    }
}
