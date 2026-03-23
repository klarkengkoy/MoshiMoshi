package com.samidevstudio.moshimoshi.feature.conversation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.samidevstudio.moshimoshi.core.ai.GeminiService
import com.samidevstudio.moshimoshi.core.audio.AndroidAudioRecorder
import com.samidevstudio.moshimoshi.core.audio.TtsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ModelOption(
    val name: String,
    val id: String,
    val rpd: Int
)

val availableModels = listOf(
    ModelOption("Gemini 3.1 Flash Lite", "gemini-3.1-flash-lite-preview", 500),
    ModelOption("Gemini 3 Flash", "gemini-3-flash-preview", 20),
    ModelOption("Gemini 2.5 Flash", "gemini-2.5-flash", 20),
    ModelOption("Gemini 2.5 Flash Lite", "gemini-2.5-flash-lite", 20)
)

data class ConversationUiState(
    val isRecording: Boolean = false,
    val isProcessing: Boolean = false,
    val responseText: String = "",
    val normalText: String = "",
    val basicText: String = "",
    val englishText: String = "",
    val showBasic: Boolean = false,
    val showEnglish: Boolean = false,
    val currentModel: ModelOption = availableModels[0], // Set Gemini 3.1 Flash Lite as default
    val isModelMenuExpanded: Boolean = false,
    val disabledModels: Set<String> = emptySet()
)

class ConversationViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(
        ConversationUiState(
            responseText = savedStateHandle["response_text"] ?: ""
        )
    )
    val uiState: StateFlow<ConversationUiState> = _uiState.asStateFlow()

    init {
        val savedText = savedStateHandle.get<String>("response_text")
        if (!savedText.isNullOrEmpty()) {
            parseAndSetResponse(savedText)
        }
    }

    private fun getCurrentDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun checkResetDisabledModels(context: Context) {
        val prefs = context.getSharedPreferences("model_limits", Context.MODE_PRIVATE)
        val lastResetDate = prefs.getString("last_reset_date", "")
        val currentDate = getCurrentDateString()

        if (lastResetDate != currentDate) {
            prefs.edit {
                clear()
                putString("last_reset_date", currentDate)
            }
            _uiState.update { it.copy(disabledModels = emptySet()) }
        } else {
            val disabled = prefs.getStringSet("disabled_models", emptySet()) ?: emptySet()
            _uiState.update { it.copy(disabledModels = disabled) }
        }
    }

    private fun disableModel(modelId: String, context: Context) {
        val prefs = context.getSharedPreferences("model_limits", Context.MODE_PRIVATE)
        val disabled = prefs.getStringSet("disabled_models", emptySet())?.toMutableSet() ?: mutableSetOf()
        disabled.add(modelId)
        prefs.edit { putStringSet("disabled_models", disabled) }
        _uiState.update { it.copy(disabledModels = disabled) }
    }

    fun setRecording(isRecording: Boolean) {
        _uiState.update { it.copy(isRecording = isRecording) }
    }

    fun setModelMenuExpanded(expanded: Boolean, context: Context) {
        if (expanded) checkResetDisabledModels(context)
        _uiState.update { it.copy(isModelMenuExpanded = expanded) }
    }

    fun selectModel(model: ModelOption, geminiService: GeminiService) {
        if (_uiState.value.disabledModels.contains(model.id)) return
        _uiState.update { it.copy(currentModel = model, isModelMenuExpanded = false) }
        geminiService.updateModel(model.id)
    }

    fun parseAndSetResponse(rawText: String) {
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

    fun processAudioResult(file: File, geminiService: GeminiService, ttsManager: TtsManager?, context: Context) {
        _uiState.update { it.copy(isProcessing = true) }
        val currentModelId = _uiState.value.currentModel.id
        
        viewModelScope.launch {
            try {
                val result = geminiService.processAudio(file)
                if (result != null) {
                    parseAndSetResponse(result)
                    val ttsText = result.substringAfter("START_NORMAL", "").substringBefore("END_NORMAL", "").trim()
                    ttsManager?.speak(ttsText.ifEmpty { result })
                }
            } catch (e: Exception) {
                val errorMsg = e.localizedMessage ?: ""
                if (errorMsg.contains("429") || errorMsg.contains("limit", ignoreCase = true)) {
                    disableModel(currentModelId, context)
                    val nextModel = availableModels.find { !uiState.value.disabledModels.contains(it.id) }
                    if (nextModel != null) {
                        _uiState.update { it.copy(currentModel = nextModel) }
                        geminiService.updateModel(nextModel.id)
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

    fun reset(geminiService: GeminiService) {
        geminiService.resetConversation()
        _uiState.update { 
            ConversationUiState(currentModel = it.currentModel, disabledModels = it.disabledModels)
        }
        savedStateHandle["response_text"] = ""
    }
}

@Composable
fun ConversationScreen(
    recorder: AndroidAudioRecorder,
    geminiService: GeminiService,
    ttsManager: TtsManager?,
    modifier: Modifier = Modifier,
    viewModel: ConversationViewModel = viewModel()
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    var audioFile by remember { mutableStateOf<File?>(null) }

    val scale by animateFloatAsState(
        targetValue = if (uiState.isRecording) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500)
        ),
        label = "MicScale"
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasPermission = isGranted
            if (isGranted) {
                val file = File(context.cacheDir, "audio.mp4")
                audioFile = file
                recorder.start(file)
                viewModel.setRecording(true)
                viewModel.parseAndSetResponse("")
            }
        }
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hello, ${currentUser?.displayName ?: "Friend"}! ✨",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Let's practice Japanese! 🌸",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                                .clickable { viewModel.setModelMenuExpanded(true, context) }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = uiState.currentModel.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "RPD: ${uiState.currentModel.rpd}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        
                        DropdownMenu(
                            expanded = uiState.isModelMenuExpanded,
                            onDismissRequest = { viewModel.setModelMenuExpanded(false, context) }
                        ) {
                            availableModels.forEach { model ->
                                val isDisabled = uiState.disabledModels.contains(model.id)
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = if (isDisabled) "${model.name} (Limit Reached)" else model.name,
                                                color = if (isDisabled) Color.Gray else Color.Unspecified
                                            )
                                            Text(
                                                "Daily Limit (RPD): ${model.rpd}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.Gray
                                            )
                                        }
                                    },
                                    enabled = !isDisabled,
                                    onClick = { viewModel.selectModel(model, geminiService) }
                                )
                            }
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.isProcessing) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(strokeWidth = 6.dp, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("${uiState.currentModel.name} is thinking... ✨", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                if (uiState.normalText.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        shape = RoundedCornerShape(32.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Moshi-chan ✨",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                Row {
                                    IconButton(onClick = { viewModel.toggleBasic() }) {
                                        Icon(
                                            imageVector = Icons.Outlined.Subtitles,
                                            contentDescription = "Show Basic",
                                            tint = if (uiState.showBasic) MaterialTheme.colorScheme.primary else Color.LightGray
                                        )
                                    }
                                    IconButton(onClick = { viewModel.toggleEnglish() }) {
                                        Icon(
                                            imageVector = Icons.Default.Translate,
                                            contentDescription = "Show English",
                                            tint = if (uiState.showEnglish) MaterialTheme.colorScheme.primary else Color.LightGray
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = uiState.normalText,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    lineHeight = 28.sp,
                                    fontSize = 20.sp
                                ),
                                color = Color.DarkGray
                            )

                            AnimatedVisibility(visible = uiState.showBasic && uiState.basicText.isNotEmpty()) {
                                Column {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = uiState.basicText,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            lineHeight = 24.sp,
                                            fontSize = 16.sp
                                        ),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            AnimatedVisibility(visible = uiState.showEnglish && uiState.englishText.isNotEmpty()) {
                                Column {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = uiState.englishText,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                        ),
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
                
                if (uiState.normalText.isEmpty() && !uiState.isRecording) {
                    Text(
                        text = "Tap the mic and say 'Ohayou'! 🌸",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(32.dp)
                    )
                }
            }
        }

        // Bottom Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            IconButton(
                onClick = {
                    if (uiState.isRecording) {
                        recorder.stop()
                        viewModel.setRecording(false)
                        audioFile?.let { file ->
                            viewModel.processAudioResult(file, geminiService, ttsManager, context)
                        }
                    } else {
                        if (hasPermission) {
                            val file = File(context.cacheDir, "audio.mp4")
                            audioFile = file
                            recorder.start(file)
                            viewModel.setRecording(true)
                            viewModel.parseAndSetResponse("")
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                },
                modifier = Modifier
                    .size(120.dp)
                    .scale(if (uiState.isRecording) scale else 1f)
                    .clip(CircleShape)
                    .background(
                        if (uiState.isRecording) Color(0xFFFF5252) 
                        else MaterialTheme.colorScheme.primary
                    )
                    .border(8.dp, Color.White.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(
                    imageVector = if (uiState.isRecording) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = "Toggle Recording",
                    tint = Color.White,
                    modifier = Modifier.size(56.dp)
                )
            }

            if (!uiState.isRecording) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(onClick = { viewModel.reset(geminiService) }) {
                        Text("Reset Conversation", color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.width(24.dp))
                    TextButton(onClick = { auth.signOut() }) {
                        Text("Sign Out", color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}
