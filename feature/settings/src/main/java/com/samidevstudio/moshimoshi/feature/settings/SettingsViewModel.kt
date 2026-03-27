package com.samidevstudio.moshimoshi.feature.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.samidevstudio.moshimoshi.core.data.model.User
import com.samidevstudio.moshimoshi.core.data.repository.AuthRepository
import com.samidevstudio.moshimoshi.core.data.repository.AuthRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val currentUser: User? = null,
    val showEditNameDialog: Boolean = false,
    val showLogoutWarning: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val newName: String = ""
)

class SettingsViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        authRepository.currentUser
            .onEach { user ->
                _uiState.update { it.copy(currentUser = user) }
            }
            .launchIn(viewModelScope)
    }

    fun onEditNameClick() {
        val currentName = uiState.value.currentUser?.displayName ?: ""
        _uiState.update { 
            it.copy(
                showEditNameDialog = true, 
                newName = currentName
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
        val nameToSave = _uiState.value.newName
        viewModelScope.launch {
            try {
                authRepository.updateDisplayName(nameToSave)
                _uiState.update { it.copy(showEditNameDialog = false) }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun onSignOutClick() {
        val user = uiState.value.currentUser
        if (user?.isAnonymous == true) {
            _uiState.update { it.copy(showLogoutWarning = true) }
        } else {
            confirmSignOut()
        }
    }

    fun onDeleteAccountClick() {
        _uiState.update { it.copy(showDeleteDialog = true) }
    }

    fun confirmSignOut() {
        viewModelScope.launch {
            authRepository.signOut()
            dismissDialogs()
        }
    }

    fun confirmDeleteAccount() {
        viewModelScope.launch {
            try {
                authRepository.deleteAccount()
                dismissDialogs()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    companion object {
        fun provideFactory(authRepository: AuthRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val savedStateHandle = createSavedStateHandle()
                SettingsViewModel(savedStateHandle, authRepository)
            }
        }
    }
}
