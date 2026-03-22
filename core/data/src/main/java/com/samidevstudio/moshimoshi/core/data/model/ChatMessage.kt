package com.samidevstudio.moshimoshi.core.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey
    val id: String = "",
    val userId: String = "",
    val text: String = "",
    val sender: String = "", // "user" or "ai"
    val timestamp: Long = System.currentTimeMillis()
)
