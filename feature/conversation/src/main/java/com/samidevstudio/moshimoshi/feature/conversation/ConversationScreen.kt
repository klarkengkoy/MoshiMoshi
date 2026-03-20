package com.samidevstudio.moshimoshi.feature.conversation

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samidevstudio.moshimoshi.core.ai.GeminiService
import com.samidevstudio.moshimoshi.core.audio.AndroidAudioRecorder
import com.samidevstudio.moshimoshi.core.audio.TtsManager
import kotlinx.coroutines.launch
import java.io.File

class ConversationViewModel : ViewModel() {
    var isRecording by mutableStateOf(false)
    var isProcessing by mutableStateOf(false)
    var responseText by mutableStateOf("")

    fun reset(geminiService: GeminiService) {
        geminiService.resetConversation()
        responseText = ""
        isProcessing = false
        isRecording = false
    }
}

@Composable
fun ConversationScreen(
    viewModel: ConversationViewModel = viewModel(),
    recorder: AndroidAudioRecorder,
    geminiService: GeminiService,
    ttsManager: TtsManager?
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

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
        viewModel.isRecording = true
        viewModel.responseText = ""
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

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.75f)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (viewModel.isProcessing) {
                CircularProgressIndicator()
            } else if (viewModel.responseText.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = viewModel.responseText,
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
                    if (viewModel.isRecording) {
                        recorder.stop()
                        viewModel.isRecording = false

                        audioFile?.let { file ->
                            viewModel.isProcessing = true
                            scope.launch {
                                val result = try {
                                    geminiService.processAudio(file)
                                } catch (e: Exception) {
                                    "Error: ${e.localizedMessage}"
                                }
                                viewModel.responseText = result ?: "No response from Gemini"
                                viewModel.isProcessing = false
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
                    .background(if (viewModel.isRecording) Color.Red else MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = if (viewModel.isRecording) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = "Toggle Recording",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }

            if (!viewModel.isRecording) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { viewModel.reset(geminiService) }) {
                    Text("Reset Conversation")
                }
            }
        }
    }
}
