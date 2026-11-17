package com.thazh.share.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.thazh.share.R
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

fun Context.findActivity(): Activity {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    throw IllegalStateException("CredentialManager requires an Activity context")
}

object GoogleAuthHelper {
    private const val TAG = "GoogleAuthHelper"

    suspend fun attemptAutoSignIn(activityContext: Context): FirebaseUser? {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) return currentUser

        val credentialManager = CredentialManager.create(activityContext)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(activityContext.getString(R.string.default_web_client_id))
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                FirebaseAuth.getInstance().signInWithCredential(firebaseCredential).await().user
            } else {
                null
            }
        } catch (e: NoCredentialException) {
            null
        } catch (e: GetCredentialException) {
            Log.d(TAG, "Auto sign-in skipped: ${e.message}")
            null
        } catch (e: Exception) {
            Log.d(TAG, "Auto sign-in skipped: ${e.message}")
            null
        }
    }

    suspend fun onGoogleSignInClicked(
        activityContext: Context,
        onError: (String) -> Unit,
    ): FirebaseUser? {
        val credentialManager = CredentialManager.create(activityContext)
        val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(
            serverClientId = activityContext.getString(R.string.default_web_client_id)
        ).build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInWithGoogleOption)
            .build()

        return try {
            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                FirebaseAuth.getInstance().signInWithCredential(firebaseCredential).await().user
            } else {
                onError("Loại chứng thực tài khoản không được hỗ trợ.")
                null
            }
        } catch (e: GetCredentialCancellationException) {
            null
        } catch (e: NoCredentialException) {
            Log.e(TAG, "No Google accounts available on device", e)
            onError("Không tìm thấy tài khoản Google trên thiết bị. Vui lòng đăng nhập tài khoản Google trong Cài đặt hệ thống.")
            null
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Google sign-in failed", e)
            onError("Đăng nhập Google thất bại: ${e.localizedMessage ?: "Lỗi không xác định"}")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Firebase signInWithCredential failed", e)
            onError("Xác thực tài khoản thất bại: ${e.localizedMessage ?: "Lỗi không xác định"}")
            null
        }
    }

    suspend fun signOut(context: Context) {
        withContext(NonCancellable) {
            FirebaseAuth.getInstance().signOut()
            try {
                CredentialManager.create(context.applicationContext)
                    .clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clear credential state", e)
            }
        }
    }
}
