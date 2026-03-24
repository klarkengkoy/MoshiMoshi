package com.samidevstudio.moshimoshi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.google.firebase.auth.FirebaseAuth
import com.samidevstudio.moshimoshi.core.audio.AndroidAudioRecorder
import com.samidevstudio.moshimoshi.core.audio.TtsManager
import com.samidevstudio.moshimoshi.core.ai.GeminiService
import com.samidevstudio.moshimoshi.core.ui.theme.MoshiMoshiTheme
import com.samidevstudio.moshimoshi.feature.auth.LoginScreen
import com.samidevstudio.moshimoshi.feature.conversation.ConversationScreen
import com.samidevstudio.moshimoshi.feature.conversation.SettingsScreen
import kotlinx.serialization.Serializable

@Serializable
sealed class Destination : NavKey {
    @Serializable data object Login : Destination()
    @Serializable data object Practice : Destination()
    @Serializable data object Live : Destination()
    @Serializable data object Settings : Destination()
}

class MainActivity : ComponentActivity() {

    private val recorder by lazy {
        AndroidAudioRecorder(applicationContext)
    }

    private val geminiService = GeminiService(apiKey = BuildConfig.GEMINI_KEY)
    
    private var ttsManager: TtsManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        ttsManager = TtsManager(this)

        setContent {
            MoshiMoshiTheme {
                val auth = remember { FirebaseAuth.getInstance() }
                var currentUser by remember { mutableStateOf(auth.currentUser) }
                
                val backStack = rememberNavBackStack(
                    if (currentUser != null) Destination.Practice else Destination.Login
                )

                LaunchedEffect(Unit) {
                    auth.addAuthStateListener { firebaseAuth ->
                        val newUser = firebaseAuth.currentUser
                        if (newUser != currentUser) {
                            currentUser = newUser
                            // Reset backstack whenever the auth state changes
                            while (backStack.isNotEmpty()) {
                                backStack.removeLastOrNull()
                            }
                            if (newUser == null) {
                                backStack.add(Destination.Login)
                            } else {
                                backStack.add(Destination.Practice)
                            }
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (currentUser != null && backStack.last() !is Destination.Login) {
                            NavigationBar {
                                val currentDestination = backStack.last()
                                NavigationBarItem(
                                    selected = currentDestination is Destination.Practice,
                                    onClick = { 
                                        if (currentDestination !is Destination.Practice) {
                                            while (backStack.isNotEmpty()) backStack.removeLastOrNull()
                                            backStack.add(Destination.Practice)
                                        } 
                                    },
                                    icon = { Icon(Icons.Default.Mic, contentDescription = "Practice") },
                                    label = { Text("Practice") }
                                )
                                NavigationBarItem(
                                    selected = currentDestination is Destination.Live,
                                    onClick = { 
                                        if (currentDestination !is Destination.Live) {
                                            while (backStack.isNotEmpty()) backStack.removeLastOrNull()
                                            backStack.add(Destination.Live)
                                        } 
                                    },
                                    icon = { Icon(Icons.Default.SmartToy, contentDescription = "Live") },
                                    label = { Text("Live") }
                                )
                                NavigationBarItem(
                                    selected = currentDestination is Destination.Settings,
                                    onClick = { 
                                        if (currentDestination !is Destination.Settings) {
                                            while (backStack.isNotEmpty()) backStack.removeLastOrNull()
                                            backStack.add(Destination.Settings)
                                        } 
                                    },
                                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                    label = { Text("Settings") }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavDisplay(
                        backStack = backStack,
                        modifier = Modifier.padding(innerPadding),
                        onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
                        entryProvider = { key ->
                            when (key) {
                                is Destination.Login -> NavEntry(key as NavKey) {
                                    LoginScreen(
                                        webClientId = BuildConfig.WEB_CLIENT_ID,
                                        versionName = BuildConfig.VERSION_NAME,
                                        onLoginSuccess = {
                                            // Handled by AuthStateListener
                                        }
                                    )
                                }
                                is Destination.Practice -> NavEntry(key as NavKey) {
                                    ConversationScreen(
                                        recorder = recorder,
                                        geminiService = geminiService,
                                        ttsManager = ttsManager
                                    )
                                }
                                is Destination.Live -> NavEntry(key as NavKey) { PlaceholderScreen("Live") }
                                is Destination.Settings -> NavEntry(key as NavKey) { 
                                    SettingsScreen(versionName = BuildConfig.VERSION_NAME)
                                }
                                else -> error("Unknown route: $key")
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsManager?.shutDown()
    }
}

@Composable
fun PlaceholderScreen(name: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "$name Coming Soon", style = MaterialTheme.typography.headlineMedium)
    }
}
