package com.samidevstudio.moshimoshi.feature.conversation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.samidevstudio.moshimoshi.core.audio.TtsManager
import com.samidevstudio.moshimoshi.core.data.repository.ChatRepository
import com.samidevstudio.moshimoshi.core.data.repository.ModelOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class ConversationUiState(
    val isRecording: Boolean = false,
    val isProcessing: Boolean = false,
    val responseText: String = "",
    val normalText: String = "",
    val basicText: String = "",
    val englishText: String = "",
    val showBasic: Boolean = false,
    val showEnglish: Boolean = false,
    val currentModel: ModelOption? = null,
    val isModelMenuExpanded: Boolean = false,
    val disabledModels: Set<String> = emptySet(),
    val availableModels: List<ModelOption> = emptyList()
)

class ConversationViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val chatRepository: ChatRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(
        ConversationUiState(
            responseText = savedStateHandle["response_text"] ?: "",
            availableModels = chatRepository.availableModels
        )
    )
    val uiState: StateFlow<ConversationUiState> = _uiState.asStateFlow()

    init {
        val savedText = savedStateHandle.get<String>("response_text")
        if (!savedText.isNullOrEmpty()) {
            parseAndSetResponse(savedText)
        }

        chatRepository.currentModel
            .onEach { model -> _uiState.update { it.copy(currentModel = model) } }
            .launchIn(viewModelScope)

        chatRepository.disabledModels
            .onEach { disabled -> _uiState.update { it.copy(disabledModels = disabled) } }
            .launchIn(viewModelScope)
    }

    fun setRecording(isRecording: Boolean) {
        _uiState.update { it.copy(isRecording = isRecording) }
    }

    fun setModelMenuExpanded(expanded: Boolean) {
        _uiState.update { it.copy(isModelMenuExpanded = expanded) }
    }

    fun selectModel(modelId: String) {
        viewModelScope.launch {
            chatRepository.selectModel(modelId)
            _uiState.update { it.copy(isModelMenuExpanded = false) }
        }
    }

    private fun parseAndSetResponse(rawText: String) {
        val normal = rawText.substringAfter("START_NORMAL", "").substringBefore("END_NORMAL", "").trim()
        val basic = rawText.substringAfter("START_BASIC", "").substringBefore("END_BASIC", "").trim()
        val english = rawText.substringAfter("START_ENGLISH", "").substringBefore("END_ENGLISH", "").trim()
        
        _uiState.update { 
            it.copy(
                responseText = rawText,
                normalText = normal.ifEmpty { rawText },
                basicText = basic,
                englishText = english
            ) 
        }
        savedStateHandle["response_text"] = rawText
    }

    fun processAudioResult(file: File, ttsManager: TtsManager?) {
        _uiState.update { it.copy(isProcessing = true) }
        val currentModelId = _uiState.value.currentModel?.id ?: ""
        
        viewModelScope.launch {
            try {
                val result = chatRepository.processAudio(file)
                if (result != null) {
                    parseAndSetResponse(result)
                    val ttsText = result.substringAfter("START_NORMAL", "").substringBefore("END_NORMAL", "").trim()
                    ttsManager?.speak(ttsText.ifEmpty { result })
                }
            } catch (e: Exception) {
                val errorMsg = e.localizedMessage ?: ""
                if (errorMsg.contains("429") || errorMsg.contains("limit", ignoreCase = true)) {
                    chatRepository.markModelAsLimited(currentModelId)
                    val nextModel = uiState.value.availableModels.find { !chatRepository.isModelDisabled(it.id) }
                    if (nextModel != null) {
                        parseAndSetResponse("START_NORMAL\nLimit reached. I've switched to ${nextModel.name} for you!\nEND_NORMAL")
                    } else {
                        parseAndSetResponse("START_NORMAL\nAll models have reached their daily limits. Please try again tomorrow! 🌸\nEND_NORMAL")
                    }
                } else {
                    parseAndSetResponse("Error: $errorMsg")
                }
            } finally {
                _uiState.update { it.copy(isProcessing = false) }
            }
        }
    }

    fun toggleBasic() {
        _uiState.update { it.copy(showBasic = !it.showBasic) }
    }

    fun toggleEnglish() {
        _uiState.update { it.copy(showEnglish = !it.showEnglish) }
    }

    fun reset() {
        viewModelScope.launch {
            chatRepository.resetConversation()
            _uiState.update { 
                ConversationUiState(
                    currentModel = it.currentModel, 
                    disabledModels = it.disabledModels,
                    availableModels = it.availableModels
                )
            }
            savedStateHandle["response_text"] = ""
        }
    }

    companion object {
        fun provideFactory(chatRepository: ChatRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val savedStateHandle = createSavedStateHandle()
                ConversationViewModel(savedStateHandle, chatRepository)
            }
        }
    }
}
