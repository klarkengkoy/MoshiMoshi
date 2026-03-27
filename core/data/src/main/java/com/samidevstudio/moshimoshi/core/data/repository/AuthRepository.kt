package com.samidevstudio.moshimoshi.core.data.repository

import com.samidevstudio.moshimoshi.core.data.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<User?>
    suspend fun signInAnonymously()
    suspend fun signInWithCredential(credential: Any): User?
    suspend fun updateDisplayName(name: String)
    suspend fun signOut()
    suspend fun deleteAccount()
}
