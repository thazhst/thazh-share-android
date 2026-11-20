package com.thazh.share.data

enum class AppLanguage(val code: String) {
    VIETNAMESE("vi"),
    ENGLISH("en");

    companion object {
        fun fromCode(code: String?): AppLanguage {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: VIETNAMESE
        }
    }
}
