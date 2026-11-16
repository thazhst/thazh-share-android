package com.thazh.share.data

import android.content.Context
import android.util.Log
import com.thazh.share.util.FileNameUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

enum class RenameResult { SUCCESS, INVALID_NAME, NAME_TAKEN, NOT_FOUND, FAILED }

/**
 * Owns the folder where files received through the "Truyền tệp" tab are
 * stored, plus a small JSON index describing them (who sent each file, when,
 * whether the person has seen it yet).
 *
 * Layout inside the app's private storage:
 *
 *  - `files/received_files/`            finished files (what the manager lists)
 *  - `files/received_files/.incoming/`  partial files of transfers still in progress
 *  - `files/received_files_index.json`  the index
 *
 * Incoming data is written to `.incoming/` first and only moved into the
 * main folder once the last chunk arrived. That way an interrupted transfer
 * can never show up as a (corrupt) finished file, and [loadAndReconcile] can
 * safely purge `.incoming/` on start-up.
 *
 * The index is the source of metadata, the folder is the source of truth for
 * existence: entries whose file vanished are dropped, and finished files that
 * somehow have no entry (e.g. the process died between the move and the index
 * write) are adopted back instead of being orphaned.
 */
class ReceivedFilesRepository(context: Context) {

    private val appContext = context.applicationContext

    /** Folder holding finished received files. */
    val directory: File = File(appContext.filesDir, DIRECTORY_NAME).apply { mkdirs() }

    /** Folder for partial files; see the class comment. */
    val stagingDirectory: File = File(directory, STAGING_DIRECTORY_NAME).apply { mkdirs() }

    private val indexFile = File(appContext.filesDir, INDEX_FILE_NAME)
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _files = MutableStateFlow<List<ReceivedFile>>(emptyList())

    /** All received files, newest first. */
    val files: StateFlow<List<ReceivedFile>> = _files.asStateFlow()

    init {
        scope.launch { loadAndReconcile() }
    }

    /**
     * Records a file that just finished arriving (already moved into [directory]).
     * If another entry in [_files] already has the same display name, ensures the file on disk
     * and in the index has a unique suffixed name (e.g. `name (1).ext`).
     */
    suspend fun add(file: ReceivedFile) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val others = _files.value.filterNot { it.id == file.id }
            val existingNames = others.mapTo(HashSet()) { it.fileName }
            val diskFile = File(file.filePath)

            val finalEntry = if (existingNames.any { it.equals(file.fileName, ignoreCase = true) }) {
                val reserved = existingNames - setOf(diskFile.name)
                val target = if (diskFile.parentFile == directory &&
                    existingNames.none { it.equals(diskFile.name, ignoreCase = true) }
                ) {
                    diskFile
                } else {
                    val uniqueTarget = FileNameUtils.uniqueFile(directory, file.fileName, existingNames)
                    if (diskFile.isFile && diskFile.absolutePath != uniqueTarget.absolutePath) {
                        if (!diskFile.renameTo(uniqueTarget)) {
                            runCatching {
                                diskFile.copyTo(uniqueTarget, overwrite = false)
                                diskFile.delete()
                            }
                        }
                    }
                    uniqueTarget
                }
                file.copy(
                    fileName = target.name,
                    filePath = target.absolutePath
                )
            } else {
                file
            }

            val updated = (listOf(finalEntry) + others)
                .sortedByDescending { it.receivedAtEpochMs }
            _files.value = updated
            writeIndex(updated)
        }
    }

    /** Clears the "new" state of every file; called when the person leaves the manager screen. */
    suspend fun markAllSeen() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (_files.value.none { !it.seen }) return@withLock
            val updated = _files.value.map { if (it.seen) it else it.copy(seen = true) }
            _files.value = updated
            writeIndex(updated)
        }
    }

    /** Deletes the given files from disk and from the index. Returns how many were actually removed. */
    suspend fun delete(ids: Set<String>): Int = withContext(Dispatchers.IO) {
        mutex.withLock {
            val removedIds = HashSet<String>()
            for (entry in _files.value) {
                if (entry.id !in ids) continue
                val file = File(entry.filePath)
                when {
                    !file.exists() -> removedIds += entry.id // already gone
                    !isManaged(file) -> removedIds += entry.id // not ours to delete (tampered index): just forget it
                    file.delete() -> removedIds += entry.id
                    // else: deletion failed — keep the entry so the person can try again
                }
            }
            val remaining = _files.value.filterNot { it.id in removedIds }
            _files.value = remaining
            writeIndex(remaining)
            removedIds.size
        }
    }

    suspend fun rename(id: String, newName: String): RenameResult = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = _files.value.firstOrNull { it.id == id } ?: return@withLock RenameResult.NOT_FOUND
            val cleaned = FileNameUtils.sanitize(newName, fallback = "")
            if (cleaned.isBlank()) return@withLock RenameResult.INVALID_NAME

            val source = File(current.filePath)
            if (!source.isFile || !isManaged(source)) return@withLock RenameResult.NOT_FOUND
            if (cleaned == source.name) return@withLock RenameResult.SUCCESS

            val target = File(source.parentFile, cleaned)
            if (target.exists()) return@withLock RenameResult.NAME_TAKEN
            if (!source.renameTo(target)) return@withLock RenameResult.FAILED

            val sameExtension = FileNameUtils.extensionOf(cleaned) == FileNameUtils.extensionOf(current.fileName)
            val updated = _files.value.map {
                if (it.id == id) {
                    it.copy(
                        fileName = cleaned,
                        filePath = target.absolutePath,
                        mimeType = if (sameExtension) it.mimeType else FileNameUtils.guessMimeType(cleaned)
                    )
                } else it
            }
            _files.value = updated
            writeIndex(updated)
            RenameResult.SUCCESS
        }
    }

    // ---- Internals ----

    private fun isManaged(file: File): Boolean {
        return runCatching {
            file.canonicalPath.startsWith(directory.canonicalPath + File.separator)
        }.getOrDefault(false)
    }

    private suspend fun loadAndReconcile() = withContext(Dispatchers.IO) {
        mutex.withLock {
            val seenPaths = HashSet<String>()
            val kept = ArrayList<ReceivedFile>()

            for (entry in readIndex()) {
                val file = File(entry.filePath)
                if (file.isFile && seenPaths.add(file.absolutePath)) {
                    kept += entry.copy(sizeBytes = file.length())
                }
            }

            // Adopt finished files that have no index entry.
            directory.listFiles()?.forEach { file ->
                if (file.isFile && !file.name.startsWith(".") && file.absolutePath !in seenPaths) {
                    kept += ReceivedFile(
                        id = UUID.randomUUID().toString(),
                        fileName = file.name,
                        sizeBytes = file.length(),
                        mimeType = FileNameUtils.guessMimeType(file.name),
                        filePath = file.absolutePath,
                        receivedAtEpochMs = file.lastModified().takeIf { it > 0L } ?: System.currentTimeMillis(),
                        seen = true
                    )
                }
            }

            // Anything still in the staging folder belongs to a transfer that never finished.
            stagingDirectory.listFiles()?.forEach { runCatching { it.delete() } }

            // A file added before this initial load finished is already in _files — keep it.
            val inMemory = _files.value.filter { current -> kept.none { it.id == current.id || it.filePath == current.filePath } }
            val merged = (kept + inMemory).sortedByDescending { it.receivedAtEpochMs }
            _files.value = merged
            writeIndex(merged)
        }
    }

    private fun readIndex(): List<ReceivedFile> {
        if (!indexFile.exists()) return emptyList()
        return runCatching {
            json.decodeFromString<List<ReceivedFile>>(indexFile.readText())
        }.getOrElse {
            Log.w(TAG, "Could not read received-files index, rebuilding from the folder: ${it.message}")
            emptyList<ReceivedFile>()
        }
    }

    private fun writeIndex(list: List<ReceivedFile>) {
        runCatching {
            val text = json.encodeToString(list)
            val temp = File(indexFile.parentFile, indexFile.name + ".tmp")
            temp.writeText(text)
            if (!temp.renameTo(indexFile)) {
                indexFile.writeText(text)
                temp.delete()
            }
        }.onFailure {
            Log.w(TAG, "Could not write received-files index: ${it.message}")
        }
    }

    companion object {
        private const val TAG = "ReceivedFilesRepo"
        private const val DIRECTORY_NAME = "received_files"
        private const val STAGING_DIRECTORY_NAME = ".incoming"
        private const val INDEX_FILE_NAME = "received_files_index.json"
    }
}
