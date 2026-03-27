package com.samidevstudio.moshimoshi.feature.conversation

import android.Manifest
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samidevstudio.moshimoshi.core.audio.AndroidAudioRecorder
import com.samidevstudio.moshimoshi.core.audio.TtsManager
import com.samidevstudio.moshimoshi.core.data.repository.ChatRepository
import java.io.File

@Composable
fun ConversationScreen(
    recorder: AndroidAudioRecorder,
    ttsManager: TtsManager?,
    chatRepository: ChatRepository,
    modifier: Modifier = Modifier,
    viewModel: ConversationViewModel = viewModel(
        factory = ConversationViewModel.provideFactory(chatRepository)
    )
) {
    val context = LocalContext.current
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
                            text = "MoshiMoshi Practice ✨",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Let's learn Japanese! 🌸",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                                .clickable { viewModel.setModelMenuExpanded(true) }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = uiState.currentModel?.name ?: "Loading...",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "RPD: ${uiState.currentModel?.rpd ?: 0}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        
                        DropdownMenu(
                            expanded = uiState.isModelMenuExpanded,
                            onDismissRequest = { viewModel.setModelMenuExpanded(false) }
                        ) {
                            uiState.availableModels.forEach { model ->
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
                                    onClick = { viewModel.selectModel(model.id) }
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
                    Text("${uiState.currentModel?.name} is thinking... ✨", style = MaterialTheme.typography.bodyMedium)
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
                                    text = "Moshi-chan",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                Row {
                                    IconButton(onClick = { viewModel.toggleBasic() }) {
                                        Icon(
                                            imageVector = Icons.Outlined.Subtitles,
                                            contentDescription = "Show Basic",
                                            tint = if (uiState.showBasic) Color.Gray else Color.LightGray
                                        )
                                    }
                                    IconButton(onClick = { viewModel.toggleEnglish() }) {
                                        Icon(
                                            imageVector = Icons.Default.Translate,
                                            contentDescription = "Show English",
                                            tint = if (uiState.showEnglish) Color.Gray else Color.LightGray
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
                                        color = Color.Gray
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
                            viewModel.processAudioResult(file, ttsManager)
                        }
                    } else {
                        if (hasPermission) {
                            val file = File(context.cacheDir, "audio.mp4")
                            audioFile = file
                            recorder.start(file)
                            viewModel.setRecording(true)
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
                    TextButton(onClick = { viewModel.reset() }) {
                        Text("Reset Conversation", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
