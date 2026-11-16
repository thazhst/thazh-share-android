package com.thazh.share.data

enum class DeviceVisibilityMode(val storageKey: String) {
    HIDDEN("hidden"),
    TEMPORARY_10_MIN("temporary_10_min"),
    ALWAYS_VISIBLE("always_visible");

    val isVisible: Boolean
        get() = this != HIDDEN

    companion object {
        fun fromStorageKey(key: String?): DeviceVisibilityMode =
            entries.firstOrNull { it.storageKey == key } ?: TEMPORARY_10_MIN
    }
}

data class IncomingConnectionRequest(
    val requestId: String,
    val peerName: String,
    val peerAddress: String,
    val requestedAtEpochMs: Long,
    val pendingFileCount: Int = 0,
    val pendingFileNames: List<String> = emptyList(),
)
