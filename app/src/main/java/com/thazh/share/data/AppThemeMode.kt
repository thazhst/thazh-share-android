package com.thazh.share.data

enum class AppThemeMode(val storageKey: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromStorageKey(key: String?): AppThemeMode {
            return entries.firstOrNull { it.storageKey == key } ?: SYSTEM
        }
    }
}
