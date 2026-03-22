package com.samidevstudio.moshimoshi.feature.conversation

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

data class ConversationUiState(
    val isRecording: Boolean = false,
    val isProcessing: Boolean = false,
    val responseText: String = ""
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

    fun setRecording(isRecording: Boolean) {
        _uiState.update { it.copy(isRecording = isRecording) }
    }

    fun setProcessing(isProcessing: Boolean) {
        _uiState.update { it.copy(isProcessing = isProcessing) }
    }

    fun setResponseText(text: String) {
        _uiState.update { it.copy(responseText = text) }
        savedStateHandle["response_text"] = text
    }

    fun reset(geminiService: GeminiService) {
        geminiService.resetConversation()
        setResponseText("")
        setProcessing(false)
        setRecording(false)
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
    val scope = rememberCoroutineScope()
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

    fun startRecordingAction() {
        File(context.cacheDir, "audio.mp4").also {
            recorder.start(it)
            audioFile = it
        }
        viewModel.setRecording(true)
        viewModel.setResponseText("")
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasPermission = isGranted
            if (isGranted) {
                startRecordingAction()
            }
        }
    )

    Column(modifier = modifier.fillMaxSize()) {
        // User Info Header
        currentUser?.let { user ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Hello, ${user.displayName ?: user.email ?: "User"}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.75f)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.isProcessing) {
                CircularProgressIndicator()
            } else if (uiState.responseText.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = uiState.responseText,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.25f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            IconButton(
                onClick = {
                    if (uiState.isRecording) {
                        recorder.stop()
                        viewModel.setRecording(false)

                        audioFile?.let { file ->
                            viewModel.setProcessing(true)
                            scope.launch {
                                val result = try {
                                    geminiService.processAudio(file)
                                } catch (e: Exception) {
                                    "Error: ${e.localizedMessage}"
                                }
                                viewModel.setResponseText(result ?: "No response from Gemini")
                                viewModel.setProcessing(false)
                                result?.let { ttsManager?.speak(it) }
                            }
                        }
                    } else {
                        if (hasPermission) {
                            startRecordingAction()
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                },
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(if (uiState.isRecording) Color.Red else MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = if (uiState.isRecording) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = "Toggle Recording",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }

            if (!uiState.isRecording) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(onClick = { viewModel.reset(geminiService) }) {
                        Text("Reset Conversation")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    TextButton(onClick = { 
                        auth.signOut()
                        // Note: Navigation would ideally be handled by a hoisted event
                    }) {
                        Text("Sign Out", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
