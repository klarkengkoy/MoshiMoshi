package com.samidevstudio.moshimoshi.core.data.repository

import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import com.samidevstudio.moshimoshi.core.data.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AuthRepositoryImpl(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) : AuthRepository {

    override val currentUser: Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.toUser())
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override suspend fun signInAnonymously() {
        firebaseAuth.signInAnonymously().await()
    }

    override suspend fun signInWithCredential(credential: Any): User? {
        if (credential is AuthCredential) {
            return firebaseAuth.signInWithCredential(credential).await().user?.toUser()
        }
        return null
    }

    override suspend fun updateDisplayName(name: String) {
        val profileUpdates = userProfileChangeRequest {
            displayName = name
        }
        firebaseAuth.currentUser?.updateProfile(profileUpdates)?.await()
    }

    override suspend fun signOut() {
        firebaseAuth.signOut()
    }

    override suspend fun deleteAccount() {
        firebaseAuth.currentUser?.delete()?.await()
    }
}

private fun FirebaseUser.toUser(): User = User(
    uid = uid,
    email = email,
    displayName = displayName,
    photoUrl = photoUrl?.toString(),
    isAnonymous = isAnonymous
)
