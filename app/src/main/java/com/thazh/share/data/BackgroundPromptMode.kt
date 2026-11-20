package com.thazh.share.data

enum class BackgroundPromptMode(val storageKey: String) {
    NOTIFICATION("notification"),
    POPUP("popup"),
    OFF("off");

    companion object {
        fun fromStorageKey(key: String?): BackgroundPromptMode {
            return entries.firstOrNull { it.storageKey == key } ?: NOTIFICATION
        }
    }
}
