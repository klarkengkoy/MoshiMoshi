package com.samidevstudio.moshimoshi.core.data.repository

import com.samidevstudio.moshimoshi.core.ai.GeminiService
import kotlinx.coroutines.flow.Flow
import java.io.File

data class ModelOption(
    val name: String,
    val id: String,
    val rpd: Int
)

interface ChatRepository {
    val availableModels: List<ModelOption>
    val currentModel: Flow<ModelOption>
    val disabledModels: Flow<Set<String>>
    
    suspend fun selectModel(modelId: String)
    suspend fun processAudio(file: File): String?
    suspend fun resetConversation()
    fun isModelDisabled(modelId: String): Boolean
    suspend fun markModelAsLimited(modelId: String)
}
