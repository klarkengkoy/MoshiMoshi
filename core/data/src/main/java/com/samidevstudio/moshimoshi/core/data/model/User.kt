package com.samidevstudio.moshimoshi.core.data.model

data class User(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
    val isAnonymous: Boolean
)
