package com.samidevstudio.moshimoshi.feature.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetPasswordOption
import androidx.credentials.PasswordCredential
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data object Success : AuthUiState
    data class Error(val message: String) : AuthUiState
}

class AuthViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(
        savedStateHandle.get<String>("error_message")?.let { AuthUiState.Error(it) } ?: AuthUiState.Idle
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun signIn(context: Context, webClientId: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            clearError()
            
            try {
                val credentialManager = CredentialManager.create(context)
                
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(true)
                    .build()

                val passwordOption = GetPasswordOption()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .addCredentialOption(passwordOption)
                    .build()

                val result = credentialManager.getCredential(
                    context = context,
                    request = request
                )
                
                val user = handleFirebaseSignIn(result)
                if (user != null) {
                    _uiState.value = AuthUiState.Success
                } else {
                    setError("Firebase Sign-in Failed")
                }
            } catch (e: GetCredentialCancellationException) {
                _uiState.value = AuthUiState.Idle
            } catch (e: GetCredentialException) {
                Log.e("Auth", "Credential Manager Error", e)
                setError(e.localizedMessage ?: "Authentication Failed")
            } catch (e: Exception) {
                Log.e("Auth", "Unexpected Error", e)
                setError("An unexpected error occurred")
            }
        }
    }

    private suspend fun handleFirebaseSignIn(result: GetCredentialResponse): FirebaseUser? {
        val auth = FirebaseAuth.getInstance()
        val credential = result.credential
        
        return try {
            val firebaseAuthCredential = when (credential) {
                is GoogleIdTokenCredential -> {
                    GoogleAuthProvider.getCredential(credential.idToken, null)
                }
                is PasswordCredential -> {
                    EmailAuthProvider.getCredential(credential.id, credential.password)
                }
                is CustomCredential -> {
                    if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                    } else {
                        Log.e("Auth", "Unexpected custom credential type: ${credential.type}")
                        null
                    }
                }
                else -> {
                    Log.e("Auth", "Unexpected credential type: ${credential.type}")
                    null
                }
            }
            
            firebaseAuthCredential?.let {
                val authResult = auth.signInWithCredential(it).await()
                authResult.user
            }
        } catch (e: Exception) {
            Log.e("Auth", "Firebase Auth Error: ${e.message}", e)
            null
        }
    }

    private fun setError(message: String) {
        _uiState.value = AuthUiState.Error(message)
        savedStateHandle["error_message"] = message
    }

    private fun clearError() {
        savedStateHandle.remove<String>("error_message")
    }
    
    fun resetState() {
        _uiState.value = AuthUiState.Idle
        clearError()
    }

    fun signOut() {
        FirebaseAuth.getInstance().signOut()
    }
}
