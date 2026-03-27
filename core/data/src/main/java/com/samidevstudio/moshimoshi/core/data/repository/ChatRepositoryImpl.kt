package com.samidevstudio.moshimoshi.core.data.repository

import android.content.Context
import androidx.core.content.edit
import com.samidevstudio.moshimoshi.core.ai.GeminiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatRepositoryImpl(
    private val context: Context,
    private val geminiService: GeminiService
) : ChatRepository {

    private val prefs = context.getSharedPreferences("model_limits", Context.MODE_PRIVATE)

    override val availableModels = listOf(
        ModelOption("Gemini 1.5 Flash Lite", "gemini-1.5-flash-lite-preview", 500),
        ModelOption("Gemini 1.5 Flash", "gemini-1.5-flash-preview", 20),
        ModelOption("Gemini 1.0 Pro", "gemini-1.0-pro", 20)
    )

    private val _currentModel = MutableStateFlow(availableModels[0])
    override val currentModel: Flow<ModelOption> = _currentModel.asStateFlow()

    private val _disabledModels = MutableStateFlow<Set<String>>(emptySet())
    override val disabledModels: Flow<Set<String>> = _disabledModels.asStateFlow()

    init {
        checkResetDisabledModels()
    }

    private fun getCurrentDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun checkResetDisabledModels() {
        val lastResetDate = prefs.getString("last_reset_date", "")
        val currentDate = getCurrentDateString()

        if (lastResetDate != currentDate) {
            prefs.edit {
                clear()
                putString("last_reset_date", currentDate)
            }
            _disabledModels.value = emptySet()
        } else {
            val disabled = prefs.getStringSet("disabled_models", emptySet()) ?: emptySet()
            _disabledModels.value = disabled
        }
    }

    override suspend fun selectModel(modelId: String) {
        val model = availableModels.find { it.id == modelId } ?: return
        if (isModelDisabled(modelId)) return
        
        _currentModel.value = model
        geminiService.updateModel(modelId)
    }

    override suspend fun processAudio(file: File): String? {
        return geminiService.processAudio(file)
    }

    override suspend fun resetConversation() {
        geminiService.resetConversation()
    }

    override fun isModelDisabled(modelId: String): Boolean {
        return _disabledModels.value.contains(modelId)
    }

    override suspend fun markModelAsLimited(modelId: String) {
        val disabled = prefs.getStringSet("disabled_models", emptySet())?.toMutableSet() ?: mutableSetOf()
        disabled.add(modelId)
        prefs.edit { putStringSet("disabled_models", disabled) }
        _disabledModels.value = disabled
        
        // Auto-switch if current is disabled
        if (_currentModel.value.id == modelId) {
            val nextModel = availableModels.find { !disabled.contains(it.id) }
            if (nextModel != null) {
                selectModel(nextModel.id)
            }
        }
    }
}
