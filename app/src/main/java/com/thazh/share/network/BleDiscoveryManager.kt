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
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

// A fixed, app-private UUID identifying "this is a Direct Chat BLE beacon"
// so scanning can filter to just our own traffic. Doesn't need SIG
// registration — any well-formed random UUID is fine for a private service
// like this.
private val SERVICE_UUID: UUID = UUID.fromString("6ac2c2c0-2f1a-4a3f-9c7f-9e2b6a2d9b41")

/**
 * Speeds up finding a nearby peer by advertising/scanning this device's own
 * WiFi Direct MAC address over BLE, *alongside* WiFi Direct's own peer
 * discovery rather than replacing it. BLE advertise/scan cycles are much
 * faster and lower-power than WiFi Direct's native discovery, so a peer
 * running this too typically appears in the list noticeably sooner — see
 * [com.thazh.share.service.WifiDirectService] for where the two
 * results get merged. Connecting still goes through the normal WiFi Direct
 * connect() flow either way; BLE only helps the two devices learn each
 * other's address faster.
 *
 * A pure enhancement: [isSupported] gates every entry point so this stays
 * entirely inert — and never touches a Bluetooth permission — on hardware
 * without BLE, or when the person has turned it off in Settings.
 */
class BleDiscoveryManager(private val context: Context) {

    private val _discoveredAddresses = MutableStateFlow<Set<String>>(emptySet())
    val discoveredAddresses: StateFlow<Set<String>> = _discoveredAddresses.asStateFlow()

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    }

    private var advertiser: BluetoothLeAdvertiser? = null
    private var scanner: BluetoothLeScanner? = null
    private var isAdvertising = false
    private var isScanning = false

    fun isSupported(): Boolean {
        return runCatching {
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) && bluetoothAdapter != null
        }.getOrDefault(false)
    }

    private fun hasPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    fun start(localMacAddress: String?) {
        runCatching {
            if (!isSupported() || !hasPermissions()) return
            val adapter = bluetoothAdapter ?: return
            if (!adapter.isEnabled) return
            if (localMacAddress != null) startAdvertising(adapter, localMacAddress)
            startScanning(adapter)
        }.onFailure {
            Log.w(TAG, "BLE start skipped: ${it.message}")
        }
    }

    fun stop() {
        runCatching {
            stopAdvertising()
            stopScanning()
            _discoveredAddresses.value = emptySet()
        }
    }

    @SuppressLint("MissingPermission") // checked in hasPermissions(), called from start() before this
    private fun startAdvertising(adapter: BluetoothAdapter, localMacAddress: String) {
        if (isAdvertising) return
        val macBytes = macStringToBytes(localMacAddress) ?: return
        val adv = adapter.bluetoothLeAdvertiser ?: return

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .setConnectable(false)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceUuid(ParcelUuid(SERVICE_UUID))
            .addServiceData(ParcelUuid(SERVICE_UUID), macBytes)
            .build()

        try {
            adv.startAdvertising(settings, data, advertiseCallback)
            advertiser = adv
            isAdvertising = true
        } catch (e: Exception) {
            Log.w(TAG, "startAdvertising failed: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopAdvertising() {
        if (!isAdvertising) return
        runCatching { advertiser?.stopAdvertising(advertiseCallback) }
        isAdvertising = false
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
        } catch (e: Exception) {
            Log.w(TAG, "startScan failed: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopScanning() {
        if (!isScanning) return
        runCatching { scanner?.stopScan(scanCallback) }
        isScanning = false
        scanner = null
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int) {
            Log.w(TAG, "BLE advertise start failed: $errorCode")
            isAdvertising = false
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val data = result.scanRecord?.getServiceData(ParcelUuid(SERVICE_UUID)) ?: return
            val mac = macBytesToString(data) ?: return
            val current = _discoveredAddresses.value
            if (mac !in current) {
                _discoveredAddresses.value = current + mac
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.w(TAG, "BLE scan failed: $errorCode")
            isScanning = false
        }
    }

    private fun macStringToBytes(mac: String): ByteArray? {
        val parts = mac.split(":")
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
