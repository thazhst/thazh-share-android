package com.thazh.share.data

import android.content.Context
import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.Query
import com.thazh.share.R
import com.thazh.share.network.CatboxUploadResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

@Keep
@IgnoreExtraProperties
data class SharedLinkItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fileName: String = "",
    val fileSizeBytes: Long = 0L,
    val mimeType: String = "application/octet-stream",
    val url: String = "",
    val expiryLabel: String = "PERMANENT",
    val expiresAtEpochMs: Long = 0L,
    val note: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
) {
    val isExpired: Boolean
        get() = expiresAtEpochMs > 0L && System.currentTimeMillis() > expiresAtEpochMs
}

class SharedLinkRepository(private val db: FirebaseFirestore) {

    fun observeSharedLinks(
        userId: String,
        onError: (Throwable) -> Unit = {},
    ): Flow<List<SharedLinkItem>> = callbackFlow {
        val path = "users/$userId/shared_links"
        val subscription = db.collection("users")
            .document(userId)
            .collection("shared_links")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    onError(error)
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val items = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(
                            SharedLinkItem::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    }
                    trySend(items)
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun saveUploadedLink(
        userId: String,
        uploadResult: CatboxUploadResult,
    ): String {
        val linkId = UUID.randomUUID().toString()
        val path = "users/$userId/shared_links/$linkId"
        val docRef = db.collection("users")
            .document(userId)
            .collection("shared_links")
            .document(linkId)

        val data = hashMapOf(
            "userId" to userId,
            "fileName" to uploadResult.fileName.take(255).ifBlank { "file" },
            "fileSizeBytes" to uploadResult.fileSizeBytes.coerceAtLeast(0L),
            "mimeType" to uploadResult.mimeType.take(128).ifBlank { "application/octet-stream" },
            "url" to uploadResult.url,
            "expiryLabel" to uploadResult.expiry.code,
            "expiresAtEpochMs" to uploadResult.expiresAtEpochMs.coerceAtLeast(0L),
            "note" to uploadResult.note.take(280),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        try {
            docRef.set(data).await()
            return linkId
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            throw e
        }
    }

    suspend fun updateLinkNote(
        userId: String,
        linkId: String,
        newNote: String,
    ) {
        val path = "users/$userId/shared_links/$linkId"
        val updates = mapOf(
            "note" to newNote.trim().take(280),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        try {
            db.collection("users")
                .document(userId)
                .collection("shared_links")
                .document(linkId)
                .update(updates)
                .await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            throw e
        }
    }

    suspend fun deleteSharedLink(
        userId: String,
        linkId: String,
    ) {
        val path = "users/$userId/shared_links/$linkId"
        try {
            db.collection("users")
                .document(userId)
                .collection("shared_links")
                .document(linkId)
                .delete()
                .await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            throw e
        }
    }

    fun observeSyncedDeviceName(userId: String): Flow<String?> = callbackFlow {
        val path = "users/$userId"
        val subscription = db.collection("users")
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.GET, path)
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val syncedName = if (snapshot.exists()) {
                        snapshot.getString("deviceName")?.trim()?.takeIf { it.isNotEmpty() }
                    } else {
                        null
                    }
                    trySend(syncedName)
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun saveSyncedDeviceName(
        userId: String,
        deviceName: String,
        email: String,
    ): Result<Unit> {
        val trimmedName = deviceName.trim().take(100)
        if (userId.isBlank() || trimmedName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Invalid userId or deviceName"))
        }
        val path = "users/$userId"
        val payload = hashMapOf(
            "userId" to userId,
            "deviceName" to trimmedName,
            "email" to email.trim().take(255),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        return try {
            db.collection("users")
                .document(userId)
                .set(payload)
                .await()
            Result.success(Unit)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    companion object {
        fun create(context: Context): SharedLinkRepository {
            val databaseId = context.getString(R.string.firestore_database_id)
            val firestore = FirebaseFirestore.getInstance(databaseId)
            return SharedLinkRepository(firestore)
        }
    }
}
