package com.thazh.share.util

import android.webkit.MimeTypeMap
import java.io.File
import java.util.Locale

/**
 * Helpers for turning an untrusted, peer-supplied file name into something
 * that is safe to create on disk, plus automatic suffixing when receiving
 * a file whose name collides with an existing file in 'Tệp đã nhận'.
 */
object FileNameUtils {

    private const val MAX_NAME_LENGTH = 120

    private val illegalChars = Regex("[\\\\/:*?\"<>|\\p{Cntrl}\\u202A-\\u202E\\u2066-\\u2069]")
    private val trailingCopySuffixRegex = Regex("^(.*?)\\s*\\((\\d+)\\)$")
    private val compoundExtensions = listOf(
        ".tar.gz", ".tar.bz2", ".tar.xz", ".tar.zst", ".tar.lz"
    )

    /** Returns a file-system-safe display name; never blank (falls back to [fallback]). */
    fun sanitize(rawName: String?, fallback: String = "file"): String {
        var name = (rawName ?: "")
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .replace(illegalChars, "_")
            .trim()
            .trimStart('.')
            .trim()

        if (name.isEmpty()) name = fallback

        if (name.length > MAX_NAME_LENGTH) {
            val (_, extWithDot) = splitBaseAndExtension(name)
            val keepExt = if (extWithDot.length <= 12) extWithDot else ""
            name = name.take(MAX_NAME_LENGTH - keepExt.length).trimEnd() + keepExt
        }
        return name
    }

    /** Keeps only characters that are harmless inside a file name; used for peer-supplied transfer ids. */
    fun sanitizeId(rawId: String): String {
        val cleaned = rawId
            .filter { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it == '-' || it == '_' }
            .take(64)
        return cleaned.ifEmpty { "transfer" }
    }

    /** Lower-case extension without the dot, or "" if the name has none (a leading dot doesn't count). */
    fun extensionOf(name: String): String {
        val dot = name.lastIndexOf('.')
        return if (dot in 1 until name.length - 1) name.substring(dot + 1).lowercase(Locale.ROOT) else ""
    }

    /**
     *Splits a filename into `(baseName, extensionWithLeadingDot)`.
     * Preserves compound archive extensions like `.tar.gz` and handles files without extensions.
     */
    fun splitBaseAndExtension(name: String): Pair<String, String> {
        val lower = name.lowercase(Locale.ROOT)
        for (compound in compoundExtensions) {
            if (lower.endsWith(compound) && name.length > compound.length) {
                val splitIdx = name.length - compound.length
                return name.substring(0, splitIdx) to name.substring(splitIdx)
            }
        }
        val dot = name.lastIndexOf('.')
        return if (dot > 0 && dot < name.length - 1) {
            name.substring(0, dot) to name.substring(dot)
        } else {
            name to ""
        }
    }

    /**
     * Resolves a unique file name inside [dir] by appending a numeric suffix ` (1)`, ` (2)`, …
     * before the extension whenever [name] already exists in [dir] (case-insensitively)
     * or is present in [reservedNames] (e.g. other in-flight incoming transfers).
     */
    fun uniqueFileName(
        dir: File,
        name: String,
        reservedNames: Set<String> = emptySet()
    ): String {
        if (!dir.exists()) dir.mkdirs()
        val sanitized = sanitize(name)

        val existingLower = HashSet<String>().apply {
            dir.listFiles()?.forEach { file ->
                if (!file.name.startsWith(".")) {
                    add(file.name.lowercase(Locale.ROOT))
                }
            }
            reservedNames.forEach { reserved ->
                if (reserved.isNotBlank()) {
                    add(reserved.lowercase(Locale.ROOT))
                }
            }
        }

        if (!isNameTaken(dir, sanitized, existingLower)) return sanitized

        val (rawBase, ext) = splitBaseAndExtension(sanitized)
        val match = trailingCopySuffixRegex.matchEntire(rawBase)
        val cleanBase = match?.groupValues?.get(1)?.takeIf { it.isNotBlank() } ?: rawBase

        var counter = 1
        while (counter <= 10_000) {
            val suffix = " ($counter)"
            val maxBaseLen = (MAX_NAME_LENGTH - suffix.length - ext.length).coerceAtLeast(1)
            val truncatedBase = if (cleanBase.length > maxBaseLen) {
                cleanBase.take(maxBaseLen).trimEnd()
            } else {
                cleanBase
            }
            val candidateName = "$truncatedBase$suffix$ext"
            if (!isNameTaken(dir, candidateName, existingLower)) {
                return candidateName
            }
            counter++
        }
        return "${cleanBase.take(40)}_${System.currentTimeMillis()}$ext"
    }

    private fun isNameTaken(
        dir: File,
        candidateName: String,
        existingLower: Set<String>
    ): Boolean {
        return candidateName.lowercase(Locale.ROOT) in existingLower ||
            File(dir, candidateName).exists()
    }

    /**
     * Returns `dir/name`, or `dir/name (1).ext`, `dir/name (2).ext`, … if that name is already
     * taken on disk or in [reservedNames].
     */
    fun uniqueFile(
        dir: File,
        name: String,
        reservedNames: Set<String> = emptySet()
    ): File {
        val resolvedName = uniqueFileName(dir, name, reservedNames)
        return File(dir, resolvedName)
    }

    fun guessMimeType(name: String): String {
        val ext = extensionOf(name)
        if (ext.isEmpty()) return DEFAULT_MIME_TYPE
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: DEFAULT_MIME_TYPE
    }

    const val DEFAULT_MIME_TYPE = "application/octet-stream"
}
