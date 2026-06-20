package com.example.api

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class AuthManager(private val context: Context) {
    private val TAG = "AuthManager"
    private val driveScope = "https://www.googleapis.com/auth/drive.appdata"

    private val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestProfile()
        .requestScopes(Scope(driveScope))
        .build()

    private val signInClient: GoogleSignInClient = GoogleSignIn.getClient(context, gso)

    data class UserProfile(
        val displayName: String,
        val email: String,
        val photoUrl: String?
    )

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    init {
        checkCurrentSession()
    }

    fun getSignInClient(): GoogleSignInClient = signInClient

    fun checkCurrentSession(onComplete: (() -> Unit)? = null) {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account != null) {
            _userProfile.value = UserProfile(
                displayName = account.displayName ?: "Japa Mitra",
                email = account.email ?: "local@sandhya.org",
                photoUrl = account.photoUrl?.toString()
            )
            onComplete?.invoke()
        } else {
            signInClient.silentSignIn().addOnCompleteListener { task ->
                try {
                    val silentAccount = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                    if (silentAccount != null) {
                        _userProfile.value = UserProfile(
                            displayName = silentAccount.displayName ?: "Japa Mitra",
                            email = silentAccount.email ?: "local@sandhya.org",
                            photoUrl = silentAccount.photoUrl?.toString()
                        )
                    } else {
                        _userProfile.value = null
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Silent sign in failed: ${e.message}")
                    _userProfile.value = null
                }
                onComplete?.invoke()
            }
        }
    }

    fun updateProfile(account: GoogleSignInAccount) {
        _userProfile.value = UserProfile(
            displayName = account.displayName ?: "Japa Mitra",
            email = account.email ?: "local@sandhya.org",
            photoUrl = account.photoUrl?.toString()
        )
    }

    suspend fun getAccessToken(): String? = withContext(Dispatchers.IO) {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context) ?: return@withContext null
            val scopeString = "oauth2:$driveScope"
            // Forces Play Services to retrieve a fresh Access Token for Drive API scoping
            GoogleAuthUtil.getToken(context, account.account ?: return@withContext null, scopeString)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch Access Token: ${e.message}", e)
            null
        }
    }

    suspend fun clearToken(): Unit = withContext(Dispatchers.IO) {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            if (account != null && account.account != null) {
                val scopeString = "oauth2:$driveScope"
                val token = GoogleAuthUtil.getToken(context, account.account!!, scopeString)
                if (token != null) {
                    GoogleAuthUtil.clearToken(context, token)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing cached auth tokens: ${e.message}")
        }
    }

    fun signOut(onComplete: () -> Unit) {
        signInClient.signOut().addOnCompleteListener {
            _userProfile.value = null
            onComplete()
        }
    }
}
