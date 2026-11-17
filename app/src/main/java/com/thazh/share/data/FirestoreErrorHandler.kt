package com.thazh.share.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(
    error: Throwable,
    operationType: OperationType,
    path: String?,
) {
    val currentUser = FirebaseAuth.getInstance().currentUser

    val providerInfoArray = JSONArray()
    currentUser?.providerData?.forEach { userInfo ->
        val providerObj = JSONObject().apply {
            put("providerId", userInfo.providerId)
            put("displayName", userInfo.displayName ?: JSONObject.NULL)
            put("email", userInfo.email ?: JSONObject.NULL)
            put("photoUrl", userInfo.photoUrl?.toString() ?: JSONObject.NULL)
        }
        providerInfoArray.put(providerObj)
    }

    val authInfoObj = JSONObject().apply {
        put("userId", currentUser?.uid ?: JSONObject.NULL)
        put("email", currentUser?.email ?: JSONObject.NULL)
        put("emailVerified", currentUser?.isEmailVerified ?: JSONObject.NULL)
        put("isAnonymous", currentUser?.isAnonymous ?: JSONObject.NULL)
        put("tenantId", currentUser?.tenantId ?: JSONObject.NULL)
        put("providerInfo", providerInfoArray)
    }

    val errInfoObj = JSONObject().apply {
        put("error", error.message ?: error.toString())
        put("operationType", operationType.value)
        put("path", path ?: JSONObject.NULL)
        put("authInfo", authInfoObj)
    }

    val jsonString = errInfoObj.toString()
    Log.e("FirestoreError", jsonString, error)
}
