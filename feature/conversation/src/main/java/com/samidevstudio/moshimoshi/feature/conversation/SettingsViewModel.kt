package com.samidevstudio.moshimoshi.feature.conversation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class SettingsUiState(
    val currentUser: FirebaseUser? = null,
    val showEditNameDialog: Boolean = false,
    val showLogoutWarning: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val newName: String = ""
)

class SettingsViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val _uiState = MutableStateFlow(SettingsUiState(currentUser = auth.currentUser))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun onEditNameClick() {
        _uiState.update { 
            it.copy(
                showEditNameDialog = true, 
                newName = it.currentUser?.displayName ?: ""
            ) 
        }
    }

    fun onNameChange(name: String) {
        _uiState.update { it.copy(newName = name) }
    }

    fun dismissDialogs() {
        _uiState.update { 
            it.copy(
                showEditNameDialog = false, 
                showLogoutWarning = false, 
                showDeleteDialog = false 
            ) 
        }
    }

    fun saveName() {
        val user = auth.currentUser
        val nameToSave = _uiState.value.newName
        viewModelScope.launch {
            try {
                val profileUpdates = userProfileChangeRequest {
                    displayName = nameToSave
                }
                user?.updateProfile(profileUpdates)?.await()
                _uiState.update { it.copy(currentUser = auth.currentUser, showEditNameDialog = false) }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun onSignOutClick() {
        val user = auth.currentUser
        if (user?.isAnonymous == true) {
            _uiState.update { it.copy(showLogoutWarning = true) }
        } else {
            auth.signOut()
        }
    }

    fun onDeleteAccountClick() {
        _uiState.update { it.copy(showDeleteDialog = true) }
    }

    fun confirmSignOut() {
        auth.signOut()
        dismissDialogs()
    }

    fun confirmDeleteAccount() {
        viewModelScope.launch {
            try {
                auth.currentUser?.delete()?.await()
                auth.signOut()
                dismissDialogs()
            } catch (e: Exception) {
                // Handle error (e.g. requires recent login)
            }
        }
    }
}
