package com.example.dayquest.data.cloud

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

sealed class AuthState {
    data object SignedOut : AuthState()
    data object Loading : AuthState()
    data class Authenticated(val user: FirebaseUser, val email: String, val displayName: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

class FirebaseAuthManager(private val context: Context) {

    private val _authState = MutableStateFlow<AuthState>(AuthState.SignedOut)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val credentialManager by lazy { CredentialManager.create(context) }

    val isFirebaseInitialized: Boolean
        get() = try {
            FirebaseApp.getApps(context).isNotEmpty()
        } catch (e: Exception) {
            false
        }

    init {
        checkCurrentAuth()
    }

    fun checkCurrentAuth() {
        if (!isFirebaseInitialized) {
            _authState.value = AuthState.SignedOut
            return
        }
        try {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                _authState.value = AuthState.Authenticated(
                    user = user,
                    email = user.email ?: "",
                    displayName = user.displayName ?: "Adventurer"
                )
            } else {
                _authState.value = AuthState.SignedOut
            }
        } catch (e: Exception) {
            _authState.value = AuthState.SignedOut
        }
    }

    suspend fun signInWithGoogle(webClientId: String? = null): Result<FirebaseUser> {
        if (!isFirebaseInitialized) {
            val msg = "Firebase is not configured yet. Add google-services.json to connect cloud sync."
            _authState.value = AuthState.Error(msg)
            return Result.failure(IllegalStateException(msg))
        }

        _authState.value = AuthState.Loading
        return try {
            val actualClientId = webClientId?.ifBlank { null }
                ?: "default-dayquest-client-id.apps.googleusercontent.com"

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(actualClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(context = context, request = request)
            val credential = result.credential

            if (credential is androidx.credentials.CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = FirebaseAuth.getInstance().signInWithCredential(authCredential).await()
                val user = authResult.user ?: throw IllegalStateException("FirebaseUser is null after sign in")

                _authState.value = AuthState.Authenticated(
                    user = user,
                    email = user.email ?: "",
                    displayName = user.displayName ?: "Adventurer"
                )
                Result.success(user)
            } else {
                val errorMsg = "Unexpected credential format returned"
                _authState.value = AuthState.Error(errorMsg)
                Result.failure(IllegalStateException(errorMsg))
            }
        } catch (e: GetCredentialCancellationException) {
            _authState.value = AuthState.SignedOut
            Result.failure(e)
        } catch (e: GetCredentialException) {
            val msg = "Google Sign-In canceled or Play Services unavailable: ${e.message}"
            _authState.value = AuthState.Error(msg)
            Result.failure(e)
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: "Sign in failed"
            _authState.value = AuthState.Error(msg)
            Result.failure(e)
        }
    }

    fun signOut() {
        if (isFirebaseInitialized) {
            try {
                FirebaseAuth.getInstance().signOut()
            } catch (ignored: Exception) {}
        }
        _authState.value = AuthState.SignedOut
    }
}
