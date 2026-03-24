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
import kotlinx.serialization.Serializable

@Serializable
sealed class Destination : NavKey {
    @Serializable data object Login : Destination()
    @Serializable data object ModelDesu : Destination()
    @Serializable data object Model2 : Destination()
    @Serializable data object Model3 : Destination()
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
                    if (currentUser != null) Destination.ModelDesu else Destination.Login
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
                                backStack.add(Destination.ModelDesu)
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
                                    selected = currentDestination is Destination.ModelDesu,
                                    onClick = { 
                                        if (currentDestination !is Destination.ModelDesu) {
                                            while (backStack.isNotEmpty()) backStack.removeLastOrNull()
                                            backStack.add(Destination.ModelDesu)
                                        } 
                                    },
                                    icon = { Icon(Icons.Default.Mic, contentDescription = "Desu") },
                                    label = { Text("Desu") }
                                )
                                NavigationBarItem(
                                    selected = currentDestination is Destination.Model2,
                                    onClick = { 
                                        if (currentDestination !is Destination.Model2) {
                                            while (backStack.isNotEmpty()) backStack.removeLastOrNull()
                                            backStack.add(Destination.Model2)
                                        } 
                                    },
                                    icon = { Icon(Icons.Default.SmartToy, contentDescription = "Model 2") },
                                    label = { Text("Model 2") }
                                )
                                NavigationBarItem(
                                    selected = currentDestination is Destination.Model3,
                                    onClick = { 
                                        if (currentDestination !is Destination.Model3) {
                                            while (backStack.isNotEmpty()) backStack.removeLastOrNull()
                                            backStack.add(Destination.Model3)
                                        } 
                                    },
                                    icon = { Icon(Icons.Default.Settings, contentDescription = "Model 3") },
                                    label = { Text("Model 3") }
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
                                is Destination.ModelDesu -> NavEntry(key as NavKey) {
                                    ConversationScreen(
                                        recorder = recorder,
                                        geminiService = geminiService,
                                        ttsManager = ttsManager
                                    )
                                }
                                is Destination.Model2 -> NavEntry(key as NavKey) { PlaceholderScreen("Model 2") }
                                is Destination.Model3 -> NavEntry(key as NavKey) { PlaceholderScreen("Model 3") }
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
