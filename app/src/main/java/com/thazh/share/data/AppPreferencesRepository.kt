package com.thazh.share.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

private val Context.dataStore by preferencesDataStore(name = "thazh_share_prefs")

class AppPreferencesRepository(private val context: Context) {

    private object Keys {
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val LOCAL_DEVICE_ID = stringPreferencesKey("local_device_id")
        val SAVED_DEVICES_JSON = stringPreferencesKey("saved_devices_json")
        val BLE_FAST_DISCOVERY_ENABLED = booleanPreferencesKey("ble_fast_discovery_enabled")
    }

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    val displayName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.DISPLAY_NAME] ?: defaultDisplayName()
    }

    suspend fun setDisplayName(name: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DISPLAY_NAME] = name.trim().ifBlank { defaultDisplayName() }
        }
    }

    val localDeviceId: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.LOCAL_DEVICE_ID] ?: UUID.randomUUID().toString()
    }

    suspend fun ensureLocalDeviceId(): String {
        var id: String? = null
        context.dataStore.edit { prefs ->
            id = prefs[Keys.LOCAL_DEVICE_ID]
            if (id == null) {
                id = UUID.randomUUID().toString()
                prefs[Keys.LOCAL_DEVICE_ID] = id!!
            }
        }
        return id!!
    }

    val savedDevices: Flow<List<SavedDevice>> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.SAVED_DEVICES_JSON] ?: return@map emptyList()
        runCatching { json.decodeFromString<List<SavedDevice>>(raw) }.getOrDefault(emptyList())
    }

    suspend fun upsertSavedDevice(device: SavedDevice) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.SAVED_DEVICES_JSON]
                ?.let { runCatching { json.decodeFromString<List<SavedDevice>>(it) }.getOrDefault(emptyList()) }
                ?: emptyList()

            val updated = current
                .filterNot { it.deviceAddress == device.deviceAddress }
                .plus(device)
                .sortedByDescending { it.lastConnectedAtEpochMs }

            prefs[Keys.SAVED_DEVICES_JSON] = json.encodeToString(updated)
        }
    }

    suspend fun renameSavedDevice(deviceAddress: String, nickname: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.SAVED_DEVICES_JSON]
                ?.let { runCatching { json.decodeFromString<List<SavedDevice>>(it) }.getOrDefault(emptyList()) }
                ?: emptyList()

            val updated = current.map {
                if (it.deviceAddress == deviceAddress) it.copy(nickname = nickname.trim().ifBlank { null }) else it
            }
            prefs[Keys.SAVED_DEVICES_JSON] = json.encodeToString(updated)
        }
    }

    suspend fun removeSavedDevice(deviceAddress: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.SAVED_DEVICES_JSON]
                ?.let { runCatching { json.decodeFromString<List<SavedDevice>>(it) }.getOrDefault(emptyList()) }
                ?: emptyList()

            val updated = current.filterNot { it.deviceAddress == deviceAddress }
            prefs[Keys.SAVED_DEVICES_JSON] = json.encodeToString(updated)
        }
    }

    val bleFastDiscoveryEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.BLE_FAST_DISCOVERY_ENABLED] ?: true
    }

    suspend fun setBleFastDiscoveryEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BLE_FAST_DISCOVERY_ENABLED] = enabled
        }
    }

    private fun defaultDisplayName(): String {
        return android.os.Build.MODEL?.takeIf { it.isNotBlank() } ?: "Thiết bị"
    }
}
