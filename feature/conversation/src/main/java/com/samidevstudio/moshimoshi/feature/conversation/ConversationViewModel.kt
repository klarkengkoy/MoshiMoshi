package com.samidevstudio.moshimoshi.feature.conversation

import android.util.Log
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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class SamiJsonResponse(
    val user_input: String? = null,
    val normal: String,
    val basic: String,
    val english: String,
    val suggestion: String
)

data class ChatMessageItem(
    val id: Long = System.currentTimeMillis(),
    val sender: String, // "user" or "sami"
    val userTranscription: String = "",
    val normalText: String = "",
    val basicText: String = "",
    val englishText: String = "",
    val suggestionText: String = "",
    val showBasic: Boolean = false,
    val showEnglish: Boolean = false
)

data class ConversationUiState(
    val isRecording: Boolean = false,
    val isProcessing: Boolean = false,
    val chatHistory: List<ChatMessageItem> = emptyList(),
    val currentModel: ModelOption? = null,
    val currentLevel: String = "N5",
    val isModelMenuExpanded: Boolean = false,
    val isLevelMenuExpanded: Boolean = false,
    val disabledModels: Set<String> = emptySet(),
    val availableModels: List<ModelOption> = emptyList(),
    val availableLevels: List<String> = listOf("N5", "N4", "N3", "N2", "N1"),
    val inputText: String = ""
)

class ConversationViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val chatRepository: ChatRepository
) : ViewModel() {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    private val _uiState = MutableStateFlow(
        ConversationUiState(
            availableModels = chatRepository.availableModels
        )
    )
    val uiState: StateFlow<ConversationUiState> = _uiState.asStateFlow()

    init {
        chatRepository.currentModel
            .onEach { model -> _uiState.update { it.copy(currentModel = model) } }
            .launchIn(viewModelScope)

        chatRepository.disabledModels
            .onEach { disabled -> _uiState.update { it.copy(disabledModels = disabled) } }
            .launchIn(viewModelScope)

        chatRepository.currentLevel
            .onEach { level -> _uiState.update { it.copy(currentLevel = level) } }
            .launchIn(viewModelScope)
    }

    fun setRecording(isRecording: Boolean) {
        _uiState.update { it.copy(isRecording = isRecording) }
    }

    fun setModelMenuExpanded(expanded: Boolean) {
        _uiState.update { it.copy(isModelMenuExpanded = expanded) }
    }

    fun setLevelMenuExpanded(expanded: Boolean) {
        _uiState.update { it.copy(isLevelMenuExpanded = expanded) }
    }

    fun selectModel(modelId: String) {
        viewModelScope.launch {
            chatRepository.selectModel(modelId)
            _uiState.update { it.copy(isModelMenuExpanded = false) }
        }
    }

    fun selectLevel(level: String) {
        viewModelScope.launch {
            chatRepository.selectLevel(level)
            _uiState.update { it.copy(isLevelMenuExpanded = false) }
        }
    }

    fun onInputTextChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendTextMessage(ttsManager: TtsManager?) {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty()) return

        val newUserMsg = ChatMessageItem(sender = "user", userTranscription = text)
        _uiState.update { it.copy(inputText = "", chatHistory = it.chatHistory + newUserMsg, isProcessing = true) }
        val currentModelId = _uiState.value.currentModel?.id ?: ""

        viewModelScope.launch {
            try {
                val result = chatRepository.processText(text)
                if (result != null) {
                    handleSamiJsonResponse(result, ttsManager)
                }
            } catch (e: Exception) {
                handleError(e, currentModelId)
            } finally {
                _uiState.update { it.copy(isProcessing = false) }
            }
        }
    }

    fun processAudioResult(file: File, ttsManager: TtsManager?) {
        _uiState.update { it.copy(isProcessing = true) }
        val currentModelId = _uiState.value.currentModel?.id ?: ""
        
        viewModelScope.launch {
            try {
                val result = chatRepository.processAudio(file)
                if (result != null) {
                    handleSamiJsonResponse(result, ttsManager)
                }
            } catch (e: Exception) {
                handleError(e, currentModelId)
            } finally {
                _uiState.update { it.copy(isProcessing = false) }
            }
        }
    }

    private fun handleSamiJsonResponse(response: String, ttsManager: TtsManager?) {
        try {
            // Robust JSON parsing using Kotlin Serialization
            val samiResponse = json.decodeFromString<SamiJsonResponse>(response)
            
            // Use a unique ID for each message to prevent LazyColumn crashes
            val userMsgId = System.currentTimeMillis()
            val samiMsgId = userMsgId + 1
            
            var currentHistory = uiState.value.chatHistory

            // If user_input is provided (from audio), update the latest user message or add a new one
            if (!samiResponse.user_input.isNullOrEmpty()) {
                val lastMsg = currentHistory.lastOrNull()
                if (lastMsg?.sender == "user" && lastMsg.userTranscription.isEmpty()) {
                    currentHistory = currentHistory.dropLast(1) + lastMsg.copy(userTranscription = samiResponse.user_input)
                } else if (lastMsg?.sender != "user") {
                    currentHistory = currentHistory + ChatMessageItem(
                        id = userMsgId,
                        sender = "user", 
                        userTranscription = samiResponse.user_input
                    )
                }
            }

            val newSamiMsg = ChatMessageItem(
                id = samiMsgId,
                sender = "sami",
                normalText = samiResponse.normal,
                basicText = samiResponse.basic,
                englishText = samiResponse.english,
                suggestionText = samiResponse.suggestion
            )

            _uiState.update { it.copy(chatHistory = currentHistory + newSamiMsg) }
            ttsManager?.speak(newSamiMsg.normalText)
        } catch (e: Exception) {
            Log.e("ConversationViewModel", "Failed to parse JSON response: $response", e)
            val errorMsg = ChatMessageItem(
                id = System.currentTimeMillis(),
                sender = "sami", 
                normalText = "Sami had trouble understanding. Please try again! 🌸"
            )
            _uiState.update { it.copy(chatHistory = it.chatHistory + errorMsg) }
        }
    }

    private suspend fun handleError(e: Exception, currentModelId: String) {
        val errorMsg = e.localizedMessage ?: ""
        if (errorMsg.contains("429") || errorMsg.contains("limit", ignoreCase = true)) {
            chatRepository.markModelAsLimited(currentModelId)
            val nextModel = uiState.value.availableModels.find { !chatRepository.isModelDisabled(it.id) }
            val message = if (nextModel != null) "Limit reached. Switched to ${nextModel.name}!" else "All limits reached. Try again tomorrow!"
            _uiState.update { it.copy(chatHistory = it.chatHistory + ChatMessageItem(sender = "sami", normalText = message)) }
        } else {
            _uiState.update { it.copy(chatHistory = it.chatHistory + ChatMessageItem(sender = "sami", normalText = "Error: $errorMsg")) }
        }
    }

    fun toggleBasic(messageId: Long) {
        _uiState.update { state ->
            state.copy(chatHistory = state.chatHistory.map { msg ->
                if (msg.id == messageId) msg.copy(showBasic = !msg.showBasic) else msg
            })
        }
    }

    fun toggleEnglish(messageId: Long) {
        _uiState.update { state ->
            state.copy(chatHistory = state.chatHistory.map { msg ->
                if (msg.id == messageId) msg.copy(showEnglish = !msg.showEnglish) else msg
            })
        }
    }

    fun useSuggestion() {
        val lastSamiMsg = uiState.value.chatHistory.lastOrNull { it.sender == "sami" }
        val suggestion = lastSamiMsg?.suggestionText ?: ""
        if (suggestion.isNotEmpty()) {
            onInputTextChange(suggestion)
        }
    }

    fun reset() {
        viewModelScope.launch {
            chatRepository.resetConversation()
            _uiState.update { 
                ConversationUiState(
                    currentModel = it.currentModel, 
                    disabledModels = it.disabledModels,
                    availableModels = it.availableModels,
                    currentLevel = it.currentLevel
                )
            }
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
